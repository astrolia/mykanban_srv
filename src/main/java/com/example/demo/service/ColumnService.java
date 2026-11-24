package com.example.demo.service;

import com.example.demo.dto.ColumnRequestDTO;
import com.example.demo.dto.ColumnResponseDTO;
import com.example.demo.exception.ApiException;
import com.example.demo.model.CardModel;
import com.example.demo.model.CardRepository;
import com.example.demo.model.ColumnModel;
import com.example.demo.model.ColumnRepository;
import com.example.demo.model.EnvironmentModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ColumnService {

    private final ColumnRepository columnRepository;
    private final CardRepository cardRepository;
    private final EnvironmentService environmentService;

    public ColumnService(ColumnRepository columnRepository,
                         CardRepository cardRepository,
                         EnvironmentService environmentService) {
        this.columnRepository = columnRepository;
        this.cardRepository = cardRepository;
        this.environmentService = environmentService;
    }

    @Transactional(readOnly = true)
    public List<ColumnResponseDTO> listByEnvironment(Integer envId, String userEmail) {
        environmentService.requireMember(envId, userEmail);
        return columnRepository.findByEnvironmentIdOrderByPositionAscIdAsc(envId)
                .stream()
                .map(ColumnResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ColumnResponseDTO getById(Integer columnId, String userEmail) {
        ColumnModel column = requireColumnWithAccess(columnId, userEmail);
        return ColumnResponseDTO.from(column);
    }

    @Transactional
    public ColumnResponseDTO create(Integer envId, ColumnRequestDTO req, String userEmail) {
        EnvironmentModel env = environmentService.requireMember(envId, userEmail);
        String label = req != null ? req.resolvedLabel() : null;
        if (label == null || label.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Nome da coluna é obrigatório");
        }

        if (columnRepository.existsByEnvironmentIdAndNameIgnoreCase(envId, label)) {
            throw new ApiException(HttpStatus.CONFLICT, "Já existe uma coluna com esse nome");
        }

        List<ColumnModel> existing = columnRepository.findByEnvironmentIdOrderByPositionAscIdAsc(envId);
        int pos = req.position() != null ? req.position() : existing.size();

        ColumnModel column = new ColumnModel(label, pos, env);
        ColumnModel saved = columnRepository.save(column);
        return ColumnResponseDTO.from(saved);
    }

    @Transactional
    public ColumnResponseDTO update(Integer columnId, ColumnRequestDTO req, String userEmail) {
        ColumnModel column = requireColumnWithAccess(columnId, userEmail);
        Integer envId = column.getEnvironment().getId();

        String newLabel = req != null ? req.resolvedLabel() : null;
        if (newLabel != null) {
            if (columnRepository.existsByEnvironmentIdAndNameIgnoreCaseAndIdNot(envId, newLabel, columnId)) {
                throw new ApiException(HttpStatus.CONFLICT, "Já existe uma coluna com esse nome");
            }
            column.setName(newLabel);
        }

        if (req != null && req.position() != null) {
            column.setPosition(req.position());
        }

        return ColumnResponseDTO.from(columnRepository.save(column));
    }

    @Transactional
    public List<ColumnResponseDTO> delete(Integer columnId, String userEmail) {
        ColumnModel column = requireColumnWithAccess(columnId, userEmail);
        Integer envId = column.getEnvironment().getId();

        if (cardRepository.existsByColumnIdAndArchivedFalse(columnId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Essa coluna ainda tem cards ativos no Kanban");
        }

        // Desassocia cards arquivados vinculados a esta coluna antes de deletá-la
        List<CardModel> archivedCards = cardRepository.findByColumnId(columnId);
        if (!archivedCards.isEmpty()) {
            for (CardModel c : archivedCards) {
                c.setColumn(null);
            }
            cardRepository.saveAll(archivedCards);
        }

        EnvironmentModel env = column.getEnvironment();
        env.getColumns().removeIf(c -> c.getId().equals(columnId));
        columnRepository.delete(column);
        columnRepository.flush();

        List<ColumnModel> remaining = columnRepository.findByEnvironmentIdOrderByPositionAscIdAsc(envId);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setPosition(i);
        }
        columnRepository.saveAll(remaining);

        return remaining.stream().map(ColumnResponseDTO::from).toList();
    }

    private ColumnModel requireColumnWithAccess(Integer columnId, String userEmail) {
        ColumnModel column = columnRepository.findById(columnId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Coluna não encontrada"));
        environmentService.requireMember(column.getEnvironment().getId(), userEmail);
        return column;
    }
}

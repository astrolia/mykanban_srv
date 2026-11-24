package com.example.demo.service;

import com.example.demo.dto.ColumnRequestDTO;
import com.example.demo.dto.ColumnResponseDTO;
import com.example.demo.exception.ApiException;
import com.example.demo.model.CardModel;
import com.example.demo.model.CardRepository;
import com.example.demo.model.ColumnModel;
import com.example.demo.model.ColumnRepository;
import com.example.demo.model.EnvironmentModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class ColumnService {

    private final ColumnRepository columnRepository;
    private final CardRepository cardRepository;
    private final EnvironmentService environmentService;

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
        String label = req.resolvedLabel();

        if (columnRepository.existsByEnvironmentIdAndNameIgnoreCase(envId, label)) {
            throw new ApiException(HttpStatus.CONFLICT, "Já existe uma coluna com esse nome");
        }

        List<ColumnModel> existing = new ArrayList<>(
                columnRepository.findByEnvironmentIdOrderByPositionAscIdAsc(envId)
        );
        int targetPos = req.position() != null
                ? Math.min(req.position(), existing.size())
                : existing.size();

        ColumnModel column = new ColumnModel(label, targetPos, env);
        ColumnModel saved = columnRepository.save(column);

        if (targetPos < existing.size()) {
            existing.add(targetPos, saved);
            for (int i = 0; i < existing.size(); i++) {
                existing.get(i).setPosition(i);
            }
            columnRepository.saveAll(existing);
        }

        log.info("Coluna criada id={} no ambiente id={}", saved.getId(), envId);
        return ColumnResponseDTO.from(saved);
    }

    @Transactional
    public ColumnResponseDTO update(Integer columnId, ColumnRequestDTO req, String userEmail) {
        ColumnModel column = requireColumnWithAccess(columnId, userEmail);
        Integer envId = column.getEnvironment().getId();

        String newLabel = req.resolvedLabel();
        if (newLabel != null) {
            if (columnRepository.existsByEnvironmentIdAndNameIgnoreCaseAndIdNot(envId, newLabel, columnId)) {
                throw new ApiException(HttpStatus.CONFLICT, "Já existe uma coluna com esse nome");
            }
            column.setName(newLabel);
        }

        if (req.position() != null) {
            List<ColumnModel> columns = new ArrayList<>(
                    columnRepository.findByEnvironmentIdOrderByPositionAscIdAsc(envId)
            );
            columns.removeIf(c -> c.getId().equals(columnId));
            int targetPos = Math.min(req.position(), columns.size());
            columns.add(targetPos, column);
            for (int i = 0; i < columns.size(); i++) {
                columns.get(i).setPosition(i);
            }
            columnRepository.saveAll(columns);
        }

        ColumnModel saved = columnRepository.save(column);
        log.info("Coluna atualizada id={}", saved.getId());
        return ColumnResponseDTO.from(saved);
    }

    @Transactional
    public List<ColumnResponseDTO> delete(Integer columnId, String userEmail) {
        ColumnModel column = requireColumnWithAccess(columnId, userEmail);
        Integer envId = column.getEnvironment().getId();

        if (cardRepository.existsByColumnIdAndArchivedFalse(columnId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Essa coluna ainda tem cards");
        }

        List<CardModel> archivedCards = cardRepository.findByColumnIdAndArchivedTrue(columnId);
        if (!archivedCards.isEmpty()) {
            for (CardModel card : archivedCards) {
                card.setColumn(null);
            }
            cardRepository.saveAll(archivedCards);
            cardRepository.flush();
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

        log.info("Coluna excluída id={} do ambiente id={}", columnId, envId);
        return remaining.stream().map(ColumnResponseDTO::from).toList();
    }

    private ColumnModel requireColumnWithAccess(Integer columnId, String userEmail) {
        ColumnModel column = columnRepository.findById(columnId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Coluna não encontrada"));
        environmentService.requireMember(column.getEnvironment().getId(), userEmail);
        return column;
    }
}

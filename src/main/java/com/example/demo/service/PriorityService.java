package com.example.demo.service;

import com.example.demo.dto.PriorityRequestDTO;
import com.example.demo.dto.PriorityResponseDTO;
import com.example.demo.exception.ApiException;
import com.example.demo.model.CardModel;
import com.example.demo.model.CardRepository;
import com.example.demo.model.EnvironmentModel;
import com.example.demo.model.PriorityModel;
import com.example.demo.model.PriorityRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PriorityService {

    private final PriorityRepository priorityRepository;
    private final CardRepository cardRepository;
    private final EnvironmentService environmentService;

    public PriorityService(PriorityRepository priorityRepository,
                           CardRepository cardRepository,
                           EnvironmentService environmentService) {
        this.priorityRepository = priorityRepository;
        this.cardRepository = cardRepository;
        this.environmentService = environmentService;
    }

    @Transactional(readOnly = true)
    public List<PriorityResponseDTO> listByEnvironment(Integer envId, String userEmail) {
        environmentService.requireMember(envId, userEmail);
        return priorityRepository.findByEnvironmentIdOrderByIdAsc(envId)
                .stream()
                .map(PriorityResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PriorityResponseDTO getById(Integer priorityId, String userEmail) {
        PriorityModel priority = requirePriorityWithAccess(priorityId, userEmail);
        return PriorityResponseDTO.from(priority);
    }

    @Transactional
    public PriorityResponseDTO create(Integer envId, PriorityRequestDTO req, String userEmail) {
        EnvironmentModel env = environmentService.requireMember(envId, userEmail);
        String label = req != null ? req.resolvedLabel() : null;
        String css = req != null ? req.resolvedCss() : null;

        if (label == null || label.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Nome da prioridade é obrigatório");
        }
        if (css == null || css.isBlank()) {
            css = "priority-grey";
        }
        if (priorityRepository.existsByEnvironmentIdAndNameIgnoreCase(envId, label)) {
            throw new ApiException(HttpStatus.CONFLICT, "Já existe uma prioridade com esse nome");
        }

        PriorityModel priority = new PriorityModel(label, css, env);
        return PriorityResponseDTO.from(priorityRepository.save(priority));
    }

    @Transactional
    public PriorityResponseDTO update(Integer priorityId, PriorityRequestDTO req, String userEmail) {
        PriorityModel priority = requirePriorityWithAccess(priorityId, userEmail);
        Integer envId = priority.getEnvironment().getId();

        String newLabel = req != null ? req.resolvedLabel() : null;
        if (newLabel != null) {
            if (priorityRepository.existsByEnvironmentIdAndNameIgnoreCaseAndIdNot(envId, newLabel, priorityId)) {
                throw new ApiException(HttpStatus.CONFLICT, "Já existe uma prioridade com esse nome");
            }
            priority.setName(newLabel);
        }

        String newCss = req != null ? req.resolvedCss() : null;
        if (newCss != null) {
            priority.setColor(newCss);
        }

        return PriorityResponseDTO.from(priorityRepository.save(priority));
    }

    @Transactional
    public void delete(Integer priorityId, String userEmail) {
        PriorityModel priority = requirePriorityWithAccess(priorityId, userEmail);

        List<CardModel> cardsUsing = cardRepository.findByPriorityId(priorityId);
        for (CardModel card : cardsUsing) {
            card.setPriority(null);
        }
        cardRepository.saveAll(cardsUsing);

        EnvironmentModel env = priority.getEnvironment();
        env.getPriorities().removeIf(p -> p.getId().equals(priorityId));
        priorityRepository.delete(priority);
    }

    private PriorityModel requirePriorityWithAccess(Integer priorityId, String userEmail) {
        PriorityModel priority = priorityRepository.findById(priorityId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Prioridade não encontrada"));
        environmentService.requireMember(priority.getEnvironment().getId(), userEmail);
        return priority;
    }
}

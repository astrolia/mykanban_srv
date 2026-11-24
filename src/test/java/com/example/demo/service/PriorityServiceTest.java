package com.example.demo.service;

import com.example.demo.dto.PriorityRequestDTO;
import com.example.demo.dto.PriorityResponseDTO;
import com.example.demo.exception.ApiException;
import com.example.demo.model.CardModel;
import com.example.demo.model.CardRepository;
import com.example.demo.model.EnvironmentModel;
import com.example.demo.model.PriorityModel;
import com.example.demo.model.PriorityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriorityServiceTest {

    @Mock
    private PriorityRepository priorityRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private EnvironmentService environmentService;

    @InjectMocks
    private PriorityService priorityService;

    private EnvironmentModel env;

    @BeforeEach
    void setUp() {
        env = new EnvironmentModel();
        env.setId(1);
    }

    @Test
    @DisplayName("Deve criar prioridade aplicando cor padrão quando css não for informado")
    void shouldCreatePriorityWithDefaultColor() {
        when(environmentService.requireMember(1, "user@example.com")).thenReturn(env);
        when(priorityRepository.existsByEnvironmentIdAndNameIgnoreCase(1, "Baixa")).thenReturn(false);
        when(priorityRepository.save(any(PriorityModel.class))).thenAnswer(inv -> {
            PriorityModel p = inv.getArgument(0);
            p.setId(5);
            return p;
        });

        PriorityResponseDTO created = priorityService.create(
                1, new PriorityRequestDTO("Baixa", null, null, null), "user@example.com"
        );

        assertThat(created.id()).isEqualTo(5);
        assertThat(created.label()).isEqualTo("Baixa");
        assertThat(created.css()).isEqualTo("priority-grey");
    }

    @Test
    @DisplayName("Deve lançar 409 CONFLICT quando já existir prioridade com o mesmo nome")
    void shouldRejectDuplicatePriorityName() {
        when(environmentService.requireMember(1, "user@example.com")).thenReturn(env);
        when(priorityRepository.existsByEnvironmentIdAndNameIgnoreCase(1, "Alta")).thenReturn(true);

        assertThatThrownBy(() -> priorityService.create(
                1, new PriorityRequestDTO("Alta", null, "priority-red", null), "user@example.com"
        ))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Deve atualizar nome e cor de prioridade existente")
    void shouldUpdatePriority() {
        PriorityModel priority = new PriorityModel("Alta", "priority-red", env);
        priority.setId(5);

        when(priorityRepository.findById(5)).thenReturn(Optional.of(priority));
        when(environmentService.requireMember(1, "user@example.com")).thenReturn(env);
        when(priorityRepository.existsByEnvironmentIdAndNameIgnoreCaseAndIdNot(1, "Crítica", 5)).thenReturn(false);
        when(priorityRepository.save(any(PriorityModel.class))).thenAnswer(inv -> inv.getArgument(0));

        PriorityResponseDTO updated = priorityService.update(
                5, new PriorityRequestDTO("Crítica", null, "priority-purple", null), "user@example.com"
        );

        assertThat(updated.label()).isEqualTo("Crítica");
        assertThat(updated.css()).isEqualTo("priority-purple");
    }

    @Test
    @DisplayName("Deve limpar prioridade dos cards vinculados antes de excluir")
    void shouldClearPriorityFromCardsOnDelete() {
        PriorityModel priority = new PriorityModel("Alta", "priority-red", env);
        priority.setId(5);
        env.getPriorities().add(priority);

        CardModel card = new CardModel();
        card.setId(10);
        card.setPriority(priority);

        when(priorityRepository.findById(5)).thenReturn(Optional.of(priority));
        when(environmentService.requireMember(1, "user@example.com")).thenReturn(env);
        when(cardRepository.findByPriorityId(5)).thenReturn(List.of(card));

        priorityService.delete(5, "user@example.com");

        assertThat(card.getPriority()).isNull();
        verify(cardRepository).saveAll(List.of(card));
        verify(priorityRepository).delete(priority);
    }
}

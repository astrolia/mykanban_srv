package com.example.demo.service;

import com.example.demo.dto.ColumnRequestDTO;
import com.example.demo.dto.ColumnResponseDTO;
import com.example.demo.exception.ApiException;
import com.example.demo.model.CardModel;
import com.example.demo.model.CardRepository;
import com.example.demo.model.ColumnModel;
import com.example.demo.model.ColumnRepository;
import com.example.demo.model.EnvironmentModel;
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
class ColumnServiceTest {

    @Mock
    private ColumnRepository columnRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private EnvironmentService environmentService;

    @InjectMocks
    private ColumnService columnService;

    private EnvironmentModel env;
    private ColumnModel col0;
    private ColumnModel col1;
    private ColumnModel col2;

    @BeforeEach
    void setUp() {
        env = new EnvironmentModel();
        env.setId(1);

        col0 = new ColumnModel("To Do", 0, env);
        col0.setId(10);
        col1 = new ColumnModel("In Progress", 1, env);
        col1.setId(11);
        col2 = new ColumnModel("Done", 2, env);
        col2.setId(12);
    }

    @Test
    @DisplayName("Deve criar coluna no fim quando posição não for informada")
    void shouldCreateColumnAtEndWhenPositionIsNull() {
        when(environmentService.requireMember(1, "user@example.com")).thenReturn(env);
        when(columnRepository.existsByEnvironmentIdAndNameIgnoreCase(1, "Revisão")).thenReturn(false);
        when(columnRepository.findByEnvironmentIdOrderByPositionAscIdAsc(1)).thenReturn(List.of(col0, col1, col2));
        when(columnRepository.save(any(ColumnModel.class))).thenAnswer(inv -> {
            ColumnModel c = inv.getArgument(0);
            c.setId(13);
            return c;
        });

        ColumnResponseDTO created = columnService.create(1, new ColumnRequestDTO("Revisão", null, null), "user@example.com");

        assertThat(created.id()).isEqualTo(13);
        assertThat(created.label()).isEqualTo("Revisão");
        assertThat(created.position()).isEqualTo(3);
    }

    @Test
    @DisplayName("Deve criar coluna em posição intermediária reordenando as demais")
    void shouldInsertColumnAtPositionAndRenumber() {
        when(environmentService.requireMember(1, "user@example.com")).thenReturn(env);
        when(columnRepository.existsByEnvironmentIdAndNameIgnoreCase(1, "Triagem")).thenReturn(false);
        when(columnRepository.findByEnvironmentIdOrderByPositionAscIdAsc(1)).thenReturn(List.of(col0, col1, col2));
        when(columnRepository.save(any(ColumnModel.class))).thenAnswer(inv -> {
            ColumnModel c = inv.getArgument(0);
            c.setId(13);
            return c;
        });

        ColumnResponseDTO created = columnService.create(1, new ColumnRequestDTO("Triagem", null, 1), "user@example.com");

        assertThat(created.position()).isEqualTo(1);
        assertThat(col0.getPosition()).isEqualTo(0);
        assertThat(col1.getPosition()).isEqualTo(2);
        assertThat(col2.getPosition()).isEqualTo(3);
        verify(columnRepository).saveAll(any());
    }

    @Test
    @DisplayName("Deve lançar 409 CONFLICT ao criar coluna com nome duplicado")
    void shouldRejectDuplicateColumnName() {
        when(environmentService.requireMember(1, "user@example.com")).thenReturn(env);
        when(columnRepository.existsByEnvironmentIdAndNameIgnoreCase(1, "To Do")).thenReturn(true);

        assertThatThrownBy(() -> columnService.create(1, new ColumnRequestDTO("To Do", null, null), "user@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Deve reordenar coluna ao atualizar posição")
    void shouldReorderColumnOnUpdate() {
        when(columnRepository.findById(12)).thenReturn(Optional.of(col2));
        when(environmentService.requireMember(1, "user@example.com")).thenReturn(env);
        when(columnRepository.findByEnvironmentIdOrderByPositionAscIdAsc(1)).thenReturn(List.of(col0, col1, col2));
        when(columnRepository.save(any(ColumnModel.class))).thenAnswer(inv -> inv.getArgument(0));

        ColumnResponseDTO updated = columnService.update(12, new ColumnRequestDTO(null, null, 0), "user@example.com");

        assertThat(updated.position()).isEqualTo(0);
        assertThat(col0.getPosition()).isEqualTo(1);
        assertThat(col1.getPosition()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve impedir exclusão de coluna que ainda possui cards ativos")
    void shouldForbidDeletingColumnWithActiveCards() {
        when(columnRepository.findById(10)).thenReturn(Optional.of(col0));
        when(environmentService.requireMember(1, "user@example.com")).thenReturn(env);
        when(cardRepository.existsByColumnIdAndArchivedFalse(10)).thenReturn(true);

        assertThatThrownBy(() -> columnService.delete(10, "user@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(columnRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve desvincular cards arquivados e renumerar colunas restantes ao excluir coluna")
    void shouldDetachArchivedCardsAndRenumberRemainingOnDelete() {
        CardModel archivedCard = new CardModel();
        archivedCard.setId(99);
        archivedCard.setColumn(col1);
        archivedCard.setArchived(true);

        when(columnRepository.findById(11)).thenReturn(Optional.of(col1));
        when(environmentService.requireMember(1, "user@example.com")).thenReturn(env);
        when(cardRepository.existsByColumnIdAndArchivedFalse(11)).thenReturn(false);
        when(cardRepository.findByColumnIdAndArchivedTrue(11)).thenReturn(List.of(archivedCard));
        when(columnRepository.findByEnvironmentIdOrderByPositionAscIdAsc(1)).thenReturn(List.of(col0, col2));

        List<ColumnResponseDTO> remaining = columnService.delete(11, "user@example.com");

        assertThat(archivedCard.getColumn()).isNull();
        assertThat(remaining).hasSize(2);
        assertThat(col0.getPosition()).isEqualTo(0);
        assertThat(col2.getPosition()).isEqualTo(1);
        verify(columnRepository).delete(col1);
    }
}

package com.example.demo.service;

import com.example.demo.dto.CardMoveRequestDTO;
import com.example.demo.dto.CardRequestDTO;
import com.example.demo.dto.CardResponseDTO;
import com.example.demo.exception.ApiException;
import com.example.demo.model.CardModel;
import com.example.demo.model.CardRepository;
import com.example.demo.model.ColumnModel;
import com.example.demo.model.ColumnRepository;
import com.example.demo.model.EnvironmentModel;
import com.example.demo.model.PriorityModel;
import com.example.demo.model.PriorityRepository;
import com.example.demo.model.UserModel;
import com.example.demo.model.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private ColumnRepository columnRepository;

    @Mock
    private PriorityRepository priorityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EnvironmentService environmentService;

    @InjectMocks
    private CardService cardService;

    private UserModel owner;
    private EnvironmentModel env;
    private ColumnModel col1;
    private ColumnModel col2;
    private PriorityModel priority;

    @BeforeEach
    void setUp() {
        owner = new UserModel("Dono", "dono@example.com", "hash");
        owner.setId(1);

        env = new EnvironmentModel();
        env.setId(10);
        env.setOwner(owner);
        env.addUser(owner);

        col1 = new ColumnModel("To Do", 0, env);
        col1.setId(100);
        col2 = new ColumnModel("Doing", 1, env);
        col2.setId(101);

        priority = new PriorityModel("Alta", "priority-red", env);
        priority.setId(200);
    }

    @Test
    @DisplayName("Deve criar card com coluna, prioridade, prazo e responsáveis")
    void shouldCreateCardWithAllAttributes() {
        when(environmentService.requireMember(10, "dono@example.com")).thenReturn(env);
        when(columnRepository.findById(100)).thenReturn(Optional.of(col1));
        when(priorityRepository.findById(200)).thenReturn(Optional.of(priority));
        when(userRepository.findById(1)).thenReturn(Optional.of(owner));
        when(cardRepository.findByColumnIdAndArchivedFalseOrderByPositionAscIdAsc(100)).thenReturn(List.of());
        when(cardRepository.save(any(CardModel.class))).thenAnswer(inv -> {
            CardModel c = inv.getArgument(0);
            c.setId(1);
            return c;
        });

        CardRequestDTO req = new CardRequestDTO(
                "Implementar testes", null, 100, "Unitários com Mockito",
                200, 50.0f, "2026-12-20", null, List.of(1), false, false, null
        );

        CardResponseDTO created = cardService.create(10, req, "dono@example.com");

        assertThat(created.id()).isEqualTo(1);
        assertThat(created.label()).isEqualTo("Implementar testes");
        assertThat(created.column()).isEqualTo(100);
        assertThat(created.priority()).isEqualTo(200);
        assertThat(created.progress()).isEqualTo(50.0f);
        assertThat(created.deadline()).isEqualTo(LocalDate.of(2026, 12, 20));
        assertThat(created.users()).containsExactly(1);
        assertThat(created.position()).isEqualTo(0);
    }

    @Test
    @DisplayName("Deve rejeitar criação quando coluna pertence a outro ambiente")
    void shouldRejectColumnFromDifferentEnvironment() {
        EnvironmentModel otherEnv = new EnvironmentModel();
        otherEnv.setId(99);
        ColumnModel foreignCol = new ColumnModel("Externa", 0, otherEnv);
        foreignCol.setId(555);

        when(environmentService.requireMember(10, "dono@example.com")).thenReturn(env);
        when(columnRepository.findById(555)).thenReturn(Optional.of(foreignCol));

        CardRequestDTO req = new CardRequestDTO("Card", null, 555, null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> cardService.create(10, req, "dono@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Deve rejeitar criação quando usuário atribuído não faz parte do ambiente")
    void shouldRejectUserOutsideEnvironment() {
        UserModel stranger = new UserModel("Estranho", "stranger@example.com", "hash");
        stranger.setId(99);

        when(environmentService.requireMember(10, "dono@example.com")).thenReturn(env);
        when(userRepository.findById(99)).thenReturn(Optional.of(stranger));

        CardRequestDTO req = new CardRequestDTO("Card", null, null, null, null, null, null, null, List.of(99), null, null, null);

        assertThatThrownBy(() -> cardService.create(10, req, "dono@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Deve rejeitar formato de data inválido no prazo do card")
    void shouldRejectInvalidDeadlineFormat() {
        when(environmentService.requireMember(10, "dono@example.com")).thenReturn(env);

        CardRequestDTO req = new CardRequestDTO("Card", null, null, null, null, null, "31/12/2026", null, null, null, null, null);

        assertThatThrownBy(() -> cardService.create(10, req, "dono@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Deve retornar CardResponseDTO em edição simples e lista de cards ao arquivar")
    void shouldReturnSingleCardOnEditAndListOnArchive() {
        CardModel card = buildCard(1, "Card 1", col1, 0, false);

        when(cardRepository.findById(1)).thenReturn(Optional.of(card));
        when(environmentService.requireMember(10, "dono@example.com")).thenReturn(env);
        when(columnRepository.findById(100)).thenReturn(Optional.of(col1));
        when(cardRepository.save(any(CardModel.class))).thenAnswer(inv -> inv.getArgument(0));

        CardRequestDTO editReq = new CardRequestDTO("Card Editado", null, 100, null, null, 80f, null, null, null, false, false, null);
        Object editResult = cardService.update(1, editReq, "dono@example.com");
        assertThat(editResult).isInstanceOf(CardResponseDTO.class);
        assertThat(((CardResponseDTO) editResult).label()).isEqualTo("Card Editado");

        when(cardRepository.findByColumnIdAndArchivedFalseOrderByPositionAscIdAsc(100)).thenReturn(List.of());
        when(cardRepository.findByEnvironmentIdOrderByPositionAscIdAsc(10)).thenReturn(List.of(card));

        CardRequestDTO archiveReq = new CardRequestDTO("Card Editado", null, 100, null, null, 80f, null, null, null, true, true, null);
        Object archiveResult = cardService.update(1, archiveReq, "dono@example.com");
        assertThat(archiveResult).isInstanceOf(List.class);
        assertThat(card.getArchived()).isTrue();
    }

    @Test
    @DisplayName("Deve reordenar cards na mesma coluna posicionando antes do card alvo")
    void shouldReorderCardsWithinSameColumn() {
        CardModel c1 = buildCard(1, "C1", col1, 0, false);
        CardModel c2 = buildCard(2, "C2", col1, 1, false);
        CardModel c3 = buildCard(3, "C3", col1, 2, false);

        when(cardRepository.findById(3)).thenReturn(Optional.of(c3));
        when(environmentService.requireMember(10, "dono@example.com")).thenReturn(env);
        when(cardRepository.findByColumnIdAndArchivedFalseOrderByPositionAscIdAsc(100)).thenReturn(List.of(c1, c2, c3));
        when(cardRepository.findByEnvironmentIdOrderByPositionAscIdAsc(10)).thenReturn(List.of(c3, c1, c2));

        List<CardResponseDTO> result = cardService.move(3, new CardMoveRequestDTO(null, 1), "dono@example.com");

        assertThat(c3.getPosition()).isEqualTo(0);
        assertThat(c1.getPosition()).isEqualTo(1);
        assertThat(c2.getPosition()).isEqualTo(2);
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("Deve mover card entre colunas renumerando origem e destino")
    void shouldMoveCardAcrossColumnsAndRenumberBoth() {
        CardModel c1 = buildCard(1, "C1", col1, 0, false);
        CardModel c2 = buildCard(2, "C2", col1, 1, false);
        CardModel targetCard = buildCard(3, "C3", col2, 0, false);

        when(cardRepository.findById(1)).thenReturn(Optional.of(c1));
        when(environmentService.requireMember(10, "dono@example.com")).thenReturn(env);
        when(columnRepository.findById(101)).thenReturn(Optional.of(col2));
        when(cardRepository.findByColumnIdAndArchivedFalseOrderByPositionAscIdAsc(100)).thenReturn(List.of(c1, c2));
        when(cardRepository.findByColumnIdAndArchivedFalseOrderByPositionAscIdAsc(101)).thenReturn(List.of(targetCard));
        when(cardRepository.findByEnvironmentIdOrderByPositionAscIdAsc(10)).thenReturn(List.of(c2, c1, targetCard));

        cardService.move(1, new CardMoveRequestDTO(101, 3), "dono@example.com");

        assertThat(c2.getPosition()).isEqualTo(0);
        assertThat(c1.getColumn()).isEqualTo(col2);
        assertThat(c1.getPosition()).isEqualTo(0);
        assertThat(targetCard.getPosition()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve excluir card e renumerar cards restantes da coluna")
    void shouldDeleteCardAndRenumberColumn() {
        CardModel c1 = buildCard(1, "C1", col1, 0, false);
        CardModel c2 = buildCard(2, "C2", col1, 1, false);
        env.getCards().add(c1);
        env.getCards().add(c2);

        when(cardRepository.findById(1)).thenReturn(Optional.of(c1));
        when(environmentService.requireMember(10, "dono@example.com")).thenReturn(env);
        when(cardRepository.findByColumnIdAndArchivedFalseOrderByPositionAscIdAsc(100)).thenReturn(List.of(c2));

        cardService.delete(1, "dono@example.com");

        assertThat(c2.getPosition()).isEqualTo(0);
        verify(cardRepository).delete(c1);
    }

    private CardModel buildCard(Integer id, String name, ColumnModel col, int pos, boolean archived) {
        CardModel c = new CardModel();
        c.setId(id);
        c.setName(name);
        c.setEnvironment(env);
        c.setColumn(col);
        c.setPosition(pos);
        c.setArchived(archived);
        c.setDone(false);
        c.setProgress(0f);
        return c;
    }
}

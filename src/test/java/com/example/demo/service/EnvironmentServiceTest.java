package com.example.demo.service;

import com.example.demo.dto.EnvironmentRequestDTO;
import com.example.demo.dto.EnvironmentResponseDTO;
import com.example.demo.dto.EnvironmentUpdateRequestDTO;
import com.example.demo.dto.UserEnvDTO;
import com.example.demo.dto.UserEnvironmentDTO;
import com.example.demo.exception.ApiException;
import com.example.demo.model.CardModel;
import com.example.demo.model.CardRepository;
import com.example.demo.model.EnvironmentModel;
import com.example.demo.model.EnvironmentRepository;
import com.example.demo.model.UserModel;
import com.example.demo.model.UserRepository;
import com.example.demo.model.enums.EnvironmentType;
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
class EnvironmentServiceTest {

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private EnvironmentService environmentService;

    private UserModel owner;
    private UserModel member;
    private UserModel outsider;
    private EnvironmentModel env;

    @BeforeEach
    void setUp() {
        owner = new UserModel("Dono", "dono@example.com", "hash");
        owner.setId(1);

        member = new UserModel("Membro", "membro@example.com", "hash");
        member.setId(2);

        outsider = new UserModel("Estranho", "estranho@example.com", "hash");
        outsider.setId(3);

        env = new EnvironmentModel();
        env.setId(10);
        env.setName("Projeto");
        env.setDescription("Desc");
        env.setArchived(false);
        env.setOwner(owner);
        env.addUser(owner);
        env.addUser(member);
    }

    @Test
    @DisplayName("Deve criar ambiente DEFAULT com 3 colunas padrão e vínculo bidirecional com o dono")
    void shouldCreateDefaultEnvironmentWithThreeColumns() {
        when(userService.requireByEmail("dono@example.com")).thenReturn(owner);
        when(environmentRepository.findDistinctByOwnerOrUsersContainingOrderByPositionAscIdAsc(owner, owner))
                .thenReturn(List.of());
        when(environmentRepository.save(any(EnvironmentModel.class))).thenAnswer(inv -> {
            EnvironmentModel e = inv.getArgument(0);
            e.setId(100);
            return e;
        });

        EnvironmentRequestDTO req = new EnvironmentRequestDTO("  Meu Quadro  ", "  Desc  ", null, null, null);
        EnvironmentResponseDTO dto = environmentService.createEnvironment(req, "dono@example.com");

        assertThat(dto.id()).isEqualTo(100);
        assertThat(dto.name()).isEqualTo("Meu Quadro");
        assertThat(dto.owner()).isEqualTo(1);
        assertThat(dto.archived()).isFalse();
        assertThat(dto.columns()).extracting("label").containsExactly("To Do", "In Progress", "Done");
        assertThat(owner.getEnvironments()).anyMatch(e -> Integer.valueOf(100).equals(e.getId()));
    }

    @Test
    @DisplayName("Deve criar ambiente CUSTOM sem colunas padrão")
    void shouldCreateCustomEnvironmentWithoutDefaultColumns() {
        when(userService.requireByEmail("dono@example.com")).thenReturn(owner);
        when(environmentRepository.findDistinctByOwnerOrUsersContainingOrderByPositionAscIdAsc(owner, owner))
                .thenReturn(List.of());
        when(environmentRepository.save(any(EnvironmentModel.class))).thenAnswer(inv -> inv.getArgument(0));

        EnvironmentRequestDTO req = new EnvironmentRequestDTO("Custom", "", 2, false, EnvironmentType.CUSTOM);
        EnvironmentResponseDTO dto = environmentService.createEnvironment(req, "dono@example.com");

        assertThat(dto.columns()).isEmpty();
        assertThat(dto.position()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve listar ambientes resumidos apenas para o próprio usuário autenticado")
    void shouldListSummariesOnlyForSelf() {
        when(userService.requireByEmail("dono@example.com")).thenReturn(owner);
        when(environmentRepository.findDistinctByOwnerOrUsersContainingOrderByPositionAscIdAsc(owner, owner))
                .thenReturn(List.of(env));

        List<UserEnvironmentDTO> result = environmentService.listSummariesForUser(1, "dono@example.com");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Projeto");

        assertThatThrownBy(() -> environmentService.listSummariesForUser(2, "dono@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Deve negar acesso a ambiente arquivado para membro comum e permitir para o dono")
    void shouldRestrictArchivedEnvironmentToOwner() {
        env.setArchived(true);
        when(environmentRepository.findById(10)).thenReturn(Optional.of(env));
        when(userService.requireByEmail("membro@example.com")).thenReturn(member);
        when(userService.requireByEmail("dono@example.com")).thenReturn(owner);

        assertThatThrownBy(() -> environmentService.getEnvironment(10, "membro@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);

        EnvironmentResponseDTO ownerView = environmentService.getEnvironment(10, "dono@example.com");
        assertThat(ownerView.archived()).isTrue();
    }

    @Test
    @DisplayName("Deve negar acesso a usuário que não pertence ao ambiente")
    void shouldDenyAccessToOutsider() {
        when(userService.requireByEmail("estranho@example.com")).thenReturn(outsider);
        when(environmentRepository.findById(10)).thenReturn(Optional.of(env));

        assertThatThrownBy(() -> environmentService.requireMember(10, "estranho@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Deve permitir apenas o dono arquivar ou restaurar o ambiente")
    void shouldAllowOnlyOwnerToChangeArchiveStatus() {
        when(environmentRepository.findById(10)).thenReturn(Optional.of(env));
        when(userService.requireByEmail("membro@example.com")).thenReturn(member);
        when(userService.requireByEmail("dono@example.com")).thenReturn(owner);
        when(environmentRepository.save(any(EnvironmentModel.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> environmentService.updateArchiveStatus(10, true, "membro@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);

        EnvironmentResponseDTO archived = environmentService.updateArchiveStatus(10, true, "dono@example.com");
        assertThat(archived.archived()).isTrue();

        EnvironmentResponseDTO restored = environmentService.updateArchiveStatus(10, false, "dono@example.com");
        assertThat(restored.archived()).isFalse();
    }

    @Test
    @DisplayName("Deve impedir membro comum de alterar flag archived no update parcial")
    void shouldPreventMemberFromArchivingViaUpdate() {
        when(environmentRepository.findById(10)).thenReturn(Optional.of(env));
        when(userService.requireByEmail("membro@example.com")).thenReturn(member);

        EnvironmentUpdateRequestDTO req = new EnvironmentUpdateRequestDTO(null, null, null, true, null);

        assertThatThrownBy(() -> environmentService.updateEnvironment(10, req, "membro@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Deve adicionar usuário no ambiente atualizando os dois lados da relação")
    void shouldAddUserBidirectionally() {
        when(environmentRepository.findById(10)).thenReturn(Optional.of(env));
        when(userService.requireByEmail("dono@example.com")).thenReturn(owner);
        when(userRepository.findById(3)).thenReturn(Optional.of(outsider));
        when(environmentRepository.save(any(EnvironmentModel.class))).thenAnswer(inv -> inv.getArgument(0));

        UserEnvDTO added = environmentService.addUser(10, 3, "dono@example.com");

        assertThat(added.id()).isEqualTo(3);
        assertThat(added.label()).isEqualTo("Estranho");
        assertThat(env.getUsers()).contains(outsider);
        assertThat(outsider.getEnvironments()).contains(env);
    }

    @Test
    @DisplayName("Deve rejeitar adição duplicada de usuário com 409 CONFLICT")
    void shouldRejectDuplicateMemberAddition() {
        when(environmentRepository.findById(10)).thenReturn(Optional.of(env));
        when(userService.requireByEmail("dono@example.com")).thenReturn(owner);
        when(userRepository.findById(2)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> environmentService.addUser(10, 2, "dono@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Deve impedir membro comum de adicionar ou remover usuários")
    void shouldForbidNonOwnerFromManagingMembers() {
        when(environmentRepository.findById(10)).thenReturn(Optional.of(env));
        when(userService.requireByEmail("membro@example.com")).thenReturn(member);

        assertThatThrownBy(() -> environmentService.addUser(10, 3, "membro@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);

        assertThatThrownBy(() -> environmentService.removeUser(10, 2, "membro@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Deve remover membro do ambiente, da lista do usuário e dos cards do ambiente")
    void shouldRemoveUserFromEnvironmentAndCards() {
        CardModel card = new CardModel();
        card.setId(50);
        card.getUsers().add(owner);
        card.getUsers().add(member);

        when(environmentRepository.findById(10)).thenReturn(Optional.of(env));
        when(userService.requireByEmail("dono@example.com")).thenReturn(owner);
        when(userRepository.findById(2)).thenReturn(Optional.of(member));
        when(cardRepository.findByEnvironmentIdAndUsersContaining(10, member)).thenReturn(List.of(card));

        environmentService.removeUser(10, 2, "dono@example.com");

        assertThat(env.getUsers()).doesNotContain(member);
        assertThat(member.getEnvironments()).doesNotContain(env);
        assertThat(card.getUsers()).containsExactly(owner);
        verify(cardRepository).saveAll(List.of(card));
        verify(environmentRepository).save(env);
    }

    @Test
    @DisplayName("Deve impedir remoção do próprio dono do ambiente")
    void shouldPreventRemovingOwner() {
        when(environmentRepository.findById(10)).thenReturn(Optional.of(env));
        when(userService.requireByEmail("dono@example.com")).thenReturn(owner);

        assertThatThrownBy(() -> environmentService.removeUser(10, 1, "dono@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Deve permitir exclusão do ambiente apenas pelo dono")
    void shouldAllowOnlyOwnerToDeleteEnvironment() {
        when(environmentRepository.findById(10)).thenReturn(Optional.of(env));
        when(userService.requireByEmail("membro@example.com")).thenReturn(member);
        when(userService.requireByEmail("dono@example.com")).thenReturn(owner);

        assertThatThrownBy(() -> environmentService.deleteEnvironment(10, "membro@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);

        environmentService.deleteEnvironment(10, "dono@example.com");

        assertThat(env.getUsers()).isEmpty();
        verify(environmentRepository).delete(env);
    }
}

package com.example.demo.service;

import com.example.demo.exception.ApiException;
import com.example.demo.model.CardModel;
import com.example.demo.model.CardRepository;
import com.example.demo.model.ColumnModel;
import com.example.demo.model.EnvironmentModel;
import com.example.demo.model.EnvironmentRepository;
import com.example.demo.model.PriorityModel;
import com.example.demo.model.UserModel;
import com.example.demo.model.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Deve normalizar dados, criptografar senha e salvar novo usuário")
    void shouldSaveUserWithNormalizedEmailAndEncodedPassword() {
        UserModel input = new UserModel("  Aline Silva  ", "  ALINE@Example.com  ", "senha123");
        when(userRepository.findByEmailIgnoreCase("aline@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$encoded");
        when(userRepository.save(any(UserModel.class))).thenAnswer(inv -> {
            UserModel u = inv.getArgument(0);
            u.setId(1);
            return u;
        });

        UserModel saved = userService.saveUser(input);

        assertThat(saved.getId()).isEqualTo(1);
        assertThat(saved.getName()).isEqualTo("Aline Silva");
        assertThat(saved.getEmail()).isEqualTo("aline@example.com");
        assertThat(saved.getPassword()).isEqualTo("$2a$encoded");
        verify(userRepository).save(input);
    }

    @Test
    @DisplayName("Deve lançar 409 CONFLICT quando email já estiver cadastrado")
    void shouldThrowConflictWhenEmailAlreadyExists() {
        UserModel input = new UserModel("Aline", "aline@example.com", "senha123");
        when(userRepository.findByEmailIgnoreCase("aline@example.com"))
                .thenReturn(Optional.of(new UserModel("Existente", "aline@example.com", "hash")));

        assertThatThrownBy(() -> userService.saveUser(input))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve retornar vazio ao buscar por email nulo sem consultar banco")
    void shouldReturnEmptyWhenEmailIsNull() {
        assertThat(userService.findByEmail(null)).isEmpty();
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Deve lançar 404 NOT_FOUND quando requireByEmail não encontrar usuário")
    void shouldThrowNotFoundWhenRequireByEmailFails() {
        when(userRepository.findByEmailIgnoreCase("inexistente@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.requireByEmail("inexistente@example.com"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Deve desvincular cards e ambientes antes de excluir o usuário")
    void shouldDetachAssociationsBeforeDeletingUser() {
        UserModel user = new UserModel("Aline", "aline@example.com", "hash");
        user.setId(1);

        UserModel otherOwner = new UserModel("Outro", "outro@example.com", "hash");
        otherOwner.setId(2);

        CardModel assignedCard = new CardModel();
        assignedCard.setId(10);
        assignedCard.getUsers().add(user);
        assignedCard.getUsers().add(otherOwner);

        EnvironmentModel memberEnv = new EnvironmentModel();
        memberEnv.setId(20);
        memberEnv.setOwner(otherOwner);
        memberEnv.addUser(otherOwner);
        memberEnv.addUser(user);

        EnvironmentModel ownedEnv = new EnvironmentModel();
        ownedEnv.setId(30);
        ownedEnv.setOwner(user);
        ownedEnv.addUser(user);
        CardModel ownedCard = new CardModel();
        ownedCard.setId(11);
        ownedCard.setColumn(new ColumnModel("To Do", 0, ownedEnv));
        ownedCard.setPriority(new PriorityModel("Alta", "priority-red", ownedEnv));
        ownedCard.getUsers().add(user);
        ownedEnv.getCards().add(ownedCard);
        user.getOwnedEnvironments().add(ownedEnv);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(cardRepository.findByUsersContaining(user)).thenReturn(List.of(assignedCard));
        when(environmentRepository.findByUsersContaining(user)).thenReturn(List.of(memberEnv, ownedEnv));

        userService.deleteById(1);

        assertThat(assignedCard.getUsers()).containsExactly(otherOwner);
        assertThat(memberEnv.getUsers()).containsExactly(otherOwner);
        assertThat(ownedCard.getColumn()).isNull();
        assertThat(ownedCard.getPriority()).isNull();
        assertThat(ownedCard.getUsers()).isEmpty();
        assertThat(ownedEnv.getUsers()).isEmpty();
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("Deve lançar 404 NOT_FOUND ao tentar excluir usuário inexistente")
    void shouldThrowNotFoundWhenDeletingMissingUser() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteById(99))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}

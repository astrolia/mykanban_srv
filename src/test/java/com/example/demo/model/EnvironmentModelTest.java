package com.example.demo.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentModelTest {

    @Test
    @DisplayName("Deve sincronizar os dois lados da relação ao adicionar usuário sem duplicar")
    void shouldSyncBothSidesOnAddUserWithoutDuplicates() {
        EnvironmentModel env = new EnvironmentModel();
        env.setId(1);
        env.setName("Quadro");

        UserModel user = new UserModel("Aline", "aline@example.com", "hash");
        user.setId(10);

        env.addUser(user);
        env.addUser(user);

        assertThat(env.getUsers()).containsExactly(user);
        assertThat(user.getEnvironments()).containsExactly(env);
    }

    @Test
    @DisplayName("Deve remover usuário dos dois lados da relação e indicar se houve remoção")
    void shouldSyncBothSidesOnRemoveUser() {
        EnvironmentModel env = new EnvironmentModel();
        env.setId(1);

        UserModel user = new UserModel("Aline", "aline@example.com", "hash");
        user.setId(10);

        env.addUser(user);

        assertThat(env.removeUser(user)).isTrue();
        assertThat(env.removeUser(user)).isFalse();
        assertThat(env.getUsers()).isEmpty();
        assertThat(user.getEnvironments()).isEmpty();
    }
}

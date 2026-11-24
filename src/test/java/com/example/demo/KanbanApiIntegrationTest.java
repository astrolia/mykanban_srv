package com.example.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class KanbanApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndLogin(String name, String email, String password) throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.email").value(email.toLowerCase()));

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return json.get("token").asString();
    }

    private int createDefaultEnvironment(String token, String name, String description) throws Exception {
        MvcResult result = mockMvc.perform(post("/environment/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "description", description
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.description").value(description))
                .andExpect(jsonPath("$.archived").value(false))
                .andExpect(jsonPath("$.columns", hasSize(3)))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asInt();
    }

    @Test
    @DisplayName("Autenticação e ciclo completo de usuário")
    void fullUserAuthAndProfileFlow() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());

        String token = registerAndLogin("Aline Silva", "aline@example.com", "senha123");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Outra",
                                "email", "ALINE@example.com",
                                "password", "123456"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "aline@example.com",
                                "password", "errada"
                        ))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "naoexiste@example.com",
                                "password", "senha123"
                        ))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Aline Silva"))
                .andExpect(jsonPath("$.email").value("aline@example.com"));

        MvcResult updateRes = mockMvc.perform(put("/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Aline Atualizada",
                                "email", "aline.nova@example.com"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value("Aline Atualizada"))
                .andExpect(jsonPath("$.user.email").value("aline.nova@example.com"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String newToken = objectMapper.readTree(updateRes.getResponse().getContentAsString()).get("token").asString();

        mockMvc.perform(get("/users/by-email")
                        .param("email", "aline.nova@example.com")
                        .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.label").value("Aline Atualizada"));

        mockMvc.perform(get("/users/by-email")
                        .param("email", "inexistente@example.com")
                        .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/users")
                        .param("email", "aline.nova@example.com")
                        .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("aline.nova@example.com"));

        mockMvc.perform(put("/users/me/password")
                        .header("Authorization", "Bearer " + newToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "errada",
                                "newPassword", "novaSenha123"
                        ))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/users/me/password")
                        .header("Authorization", "Bearer " + newToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "senha123",
                                "newPassword", "123"
                        ))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/users/me/password")
                        .header("Authorization", "Bearer " + newToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "senha123",
                                "newPassword", "novaSenha123"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "aline.nova@example.com",
                                "password", "novaSenha123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(delete("/users/me")
                        .header("Authorization", "Bearer " + newToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "errada"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/users/me")
                        .header("Authorization", "Bearer " + newToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "novaSenha123"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "aline.nova@example.com",
                                "password", "novaSenha123"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Ambientes, permissões de dono, arquivamento e vínculo bidirecional com usuários")
    void environmentAndMemberManagementFlow() throws Exception {
        String ownerToken = registerAndLogin("Dono", "dono@example.com", "senha123");
        String memberToken = registerAndLogin("Membro", "membro@example.com", "senha123");
        String outsiderToken = registerAndLogin("Estranho", "estranho@example.com", "senha123");

        int envId = createDefaultEnvironment(ownerToken, "Projeto TCC", "Kanban principal");

        mockMvc.perform(get("/environment")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(envId));

        mockMvc.perform(get("/environment/" + envId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());

        int memberId = objectMapper.readTree(
                mockMvc.perform(get("/users/by-email")
                                .param("email", "membro@example.com")
                                .header("Authorization", "Bearer " + ownerToken))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString()
        ).get("id").asInt();

        int outsiderId = objectMapper.readTree(
                mockMvc.perform(get("/users/by-email")
                                .param("email", "estranho@example.com")
                                .header("Authorization", "Bearer " + ownerToken))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString()
        ).get("id").asInt();

        mockMvc.perform(patch("/environment/" + envId + "/users")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", memberId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(memberId))
                .andExpect(jsonPath("$.label").value("Membro"));

        mockMvc.perform(patch("/environment/" + envId + "/users")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", memberId))))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/users/" + memberId + "/environments")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(envId))
                .andExpect(jsonPath("$[0].name").value("Projeto TCC"));

        mockMvc.perform(get("/users/" + memberId + "/environments")
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());

        MvcResult envLoaded = mockMvc.perform(get("/environment/" + envId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(envId))
                .andExpect(jsonPath("$.name").value("Projeto TCC"))
                .andExpect(jsonPath("$.users", hasSize(2)))
                .andReturn();

        JsonNode envJson = objectMapper.readTree(envLoaded.getResponse().getContentAsString());
        int ownerId = envJson.get("owner").asInt();
        int firstColId = envJson.get("columns").get(0).get("id").asInt();

        // Membro comum pode editar nome/descrição, mas não arquivar/excluir/gerenciar membros
        mockMvc.perform(patch("/environment/" + envId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("description", "Atualizado por membro"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Atualizado por membro"));

        mockMvc.perform(patch("/environment/" + envId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("archived", true))))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/environment/" + envId + "/users")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", outsiderId))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/environment/" + envId + "/users")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", memberId))))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/environment/" + envId + "/archive-status")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("archived", true))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/environment/" + envId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());

        // Dono arquiva e restaura o ambiente
        mockMvc.perform(patch("/environment/" + envId + "/archive-status")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("archived", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        mockMvc.perform(get("/environment/" + envId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/environment/" + envId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        mockMvc.perform(patch("/environment/" + envId + "/archive-status")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("archived", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(false));

        MvcResult cardRes = mockMvc.perform(post("/environments/" + envId + "/cards")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "label", "Card do Membro",
                                "column", firstColId,
                                "users", List.of(ownerId, memberId)
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.users", containsInAnyOrder(ownerId, memberId)))
                .andReturn();
        int cardId = objectMapper.readTree(cardRes.getResponse().getContentAsString()).get("id").asInt();

        mockMvc.perform(delete("/environment/" + envId + "/users")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", ownerId))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/environment/" + envId + "/users")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", memberId))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/" + memberId + "/environments")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/cards/" + cardId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", contains(ownerId)));

        mockMvc.perform(patch("/environment/" + envId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Projeto TCC Editado",
                                "description", "Nova descrição"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Projeto TCC Editado"))
                .andExpect(jsonPath("$.description").value("Nova descrição"));

        mockMvc.perform(delete("/environment/" + envId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/environment/" + envId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Colunas, prioridades, cards, arquivamento, restauração e ordenação")
    void fullKanbanBoardWorkflow() throws Exception {
        String token = registerAndLogin("Gabriel", "gabriel@example.com", "senha123");
        int envId = createDefaultEnvironment(token, "Board Completo", "Testando fluxo completo");

        MvcResult colRes = mockMvc.perform(post("/environments/" + envId + "/columns")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("label", "Revisão"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value("Revisão"))
                .andExpect(jsonPath("$.position").value(3))
                .andReturn();
        int reviewColId = objectMapper.readTree(colRes.getResponse().getContentAsString()).get("id").asInt();

        mockMvc.perform(get("/columns/" + reviewColId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Revisão"));

        mockMvc.perform(post("/environments/" + envId + "/columns")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("label", "revisão"))))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/columns/" + reviewColId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "label", "Code Review",
                                "position", 1
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Code Review"))
                .andExpect(jsonPath("$.position").value(1));

        MvcResult colsListRes = mockMvc.perform(get("/environments/" + envId + "/columns")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[1].id").value(reviewColId))
                .andReturn();
        JsonNode colsJson = objectMapper.readTree(colsListRes.getResponse().getContentAsString());
        int todoColId = colsJson.get(0).get("id").asInt();
        int inProgressColId = colsJson.get(2).get("id").asInt();

        MvcResult priRes = mockMvc.perform(post("/environments/" + envId + "/priority")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("label", "Alta", "css", "priority-red"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value("Alta"))
                .andExpect(jsonPath("$.css").value("priority-red"))
                .andReturn();
        int highPriId = objectMapper.readTree(priRes.getResponse().getContentAsString()).get("id").asInt();

        mockMvc.perform(get("/priority/" + highPriId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Alta"));

        mockMvc.perform(get("/environments/" + envId + "/priority")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(put("/priority/" + highPriId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "label", "Urgente",
                                "css", "priority-orange"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Urgente"))
                .andExpect(jsonPath("$.css").value("priority-orange"));

        MvcResult c1Res = mockMvc.perform(post("/environments/" + envId + "/cards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "label", "Card 1",
                                "description", "Descrição inicial",
                                "column", todoColId,
                                "priority", highPriId,
                                "progress", 30,
                                "deadline", "2026-12-15T03:00:00.000Z",
                                "archived", false
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value("Card 1"))
                .andExpect(jsonPath("$.priority").value(highPriId))
                .andExpect(jsonPath("$.progress").value(30.0))
                .andExpect(jsonPath("$.deadline").value("2026-12-15"))
                .andExpect(jsonPath("$.position").value(0))
                .andReturn();
        int card1Id = objectMapper.readTree(c1Res.getResponse().getContentAsString()).get("id").asInt();

        MvcResult c2Res = mockMvc.perform(post("/environments/" + envId + "/cards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("label", "Card 2", "column", todoColId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(1))
                .andReturn();
        int card2Id = objectMapper.readTree(c2Res.getResponse().getContentAsString()).get("id").asInt();

        MvcResult c3Res = mockMvc.perform(post("/environments/" + envId + "/cards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("label", "Card 3", "column", todoColId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(2))
                .andReturn();
        int card3Id = objectMapper.readTree(c3Res.getResponse().getContentAsString()).get("id").asInt();

        mockMvc.perform(delete("/columns/" + todoColId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());

        MvcResult archivedInReview = mockMvc.perform(post("/environments/" + envId + "/cards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "label", "Card Arquivado",
                                "column", reviewColId,
                                "archived", true
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        int archivedCardId = objectMapper.readTree(archivedInReview.getResponse().getContentAsString()).get("id").asInt();

        mockMvc.perform(delete("/columns/" + reviewColId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        mockMvc.perform(get("/cards/" + archivedCardId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.column").value(nullValue()));

        mockMvc.perform(get("/environments/" + envId + "/cards")
                        .param("archived", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(archivedCardId));

        mockMvc.perform(put("/cards/" + card1Id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "label", "Card 1 Editado",
                                "column", todoColId,
                                "priority", highPriId,
                                "progress", 75,
                                "archived", false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Card 1 Editado"))
                .andExpect(jsonPath("$.progress").value(75.0))
                .andExpect(jsonPath("$.description").value(nullValue()));

        MvcResult moveSameCol = mockMvc.perform(put("/cards/move/" + card3Id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("before", card1Id))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode afterMove1 = objectMapper.readTree(moveSameCol.getResponse().getContentAsString());
        assertThat(findPosition(afterMove1, card3Id)).isEqualTo(0);
        assertThat(findPosition(afterMove1, card1Id)).isEqualTo(1);
        assertThat(findPosition(afterMove1, card2Id)).isEqualTo(2);

        MvcResult moveOtherCol = mockMvc.perform(put("/cards/move/" + card2Id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("column", inProgressColId))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode afterMove2 = objectMapper.readTree(moveOtherCol.getResponse().getContentAsString());
        assertThat(findColumn(afterMove2, card2Id)).isEqualTo(inProgressColId);
        assertThat(findPosition(afterMove2, card2Id)).isEqualTo(0);

        MvcResult archiveRes = mockMvc.perform(put("/cards/" + card1Id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "label", "Card 1 Editado",
                                "column", todoColId,
                                "priority", highPriId,
                                "done", true,
                                "archived", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn();

        JsonNode afterArchive = objectMapper.readTree(archiveRes.getResponse().getContentAsString());
        assertThat(findField(afterArchive, card1Id, "archived").asBoolean()).isTrue();
        assertThat(findField(afterArchive, card1Id, "done").asBoolean()).isTrue();

        MvcResult restoreRes = mockMvc.perform(put("/cards/" + card1Id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "label", "Card 1 Editado",
                                "column", todoColId,
                                "priority", highPriId,
                                "archived", false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn();

        JsonNode afterRestore = objectMapper.readTree(restoreRes.getResponse().getContentAsString());
        assertThat(findField(afterRestore, card1Id, "archived").asBoolean()).isFalse();

        mockMvc.perform(delete("/priority/" + highPriId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/cards/" + card1Id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value(nullValue()));

        mockMvc.perform(delete("/cards/" + card1Id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/cards/" + card1Id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Exclusão em cascata segura de ambiente e conta com vínculos")
    void cascadeDeleteEnvironmentAndAccountSafety() throws Exception {
        String aliceToken = registerAndLogin("Alice", "alice@example.com", "senha123");
        String bobToken = registerAndLogin("Bob", "bob@example.com", "senha123");

        int aliceEnvId = createDefaultEnvironment(aliceToken, "Ambiente Alice", "Com dados");
        int bobEnvId = createDefaultEnvironment(bobToken, "Ambiente Bob", "Compartilhado");

        int aliceId = objectMapper.readTree(
                mockMvc.perform(get("/users/by-email").param("email", "alice@example.com")
                                .header("Authorization", "Bearer " + bobToken))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()
        ).get("id").asInt();

        int bobId = objectMapper.readTree(
                mockMvc.perform(get("/users/by-email").param("email", "bob@example.com")
                                .header("Authorization", "Bearer " + aliceToken))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()
        ).get("id").asInt();

        mockMvc.perform(patch("/environment/" + aliceEnvId + "/users")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", bobId))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/environment/" + bobEnvId + "/users")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", aliceId))))
                .andExpect(status().isOk());

        JsonNode aliceEnv = objectMapper.readTree(
                mockMvc.perform(get("/environment/" + aliceEnvId).header("Authorization", "Bearer " + aliceToken))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()
        );
        int aliceColId = aliceEnv.get("columns").get(0).get("id").asInt();

        int priId = objectMapper.readTree(
                mockMvc.perform(post("/environments/" + aliceEnvId + "/priority")
                                .header("Authorization", "Bearer " + aliceToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("label", "Crítica", "css", "priority-red"))))
                        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()
        ).get("id").asInt();

        mockMvc.perform(post("/environments/" + aliceEnvId + "/cards")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "label", "Card na Alice",
                                "column", aliceColId,
                                "priority", priId,
                                "users", List.of(aliceId, bobId)
                        ))))
                .andExpect(status().isCreated());

        JsonNode bobEnv = objectMapper.readTree(
                mockMvc.perform(get("/environment/" + bobEnvId).header("Authorization", "Bearer " + bobToken))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()
        );
        int bobColId = bobEnv.get("columns").get(0).get("id").asInt();

        int bobCardId = objectMapper.readTree(
                mockMvc.perform(post("/environments/" + bobEnvId + "/cards")
                                .header("Authorization", "Bearer " + bobToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "label", "Card no Bob",
                                        "column", bobColId,
                                        "users", List.of(aliceId, bobId)
                                ))))
                        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()
        ).get("id").asInt();

        mockMvc.perform(delete("/users/me")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "senha123"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/cards/" + bobCardId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", contains(bobId)));

        mockMvc.perform(delete("/environment/" + bobEnvId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Bean Validation nos endpoints e documentação SpringDoc OpenAPI")
    void beanValidationAndSpringDocEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("MyKanban API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists());

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Teste",
                                "email", "email-invalido",
                                "password", "123"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());

        String token = registerAndLogin("Carlos", "carlos@example.com", "senha123");

        mockMvc.perform(post("/environment/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "   "))))
                .andExpect(status().isBadRequest());

        int envId = createDefaultEnvironment(token, "Validacao", "Testando validacoes");

        mockMvc.perform(patch("/environment/" + envId + "/archive-status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/environments/" + envId + "/columns")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("label", "   "))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/environments/" + envId + "/priority")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("label", ""))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/environments/" + envId + "/cards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "label", "Progresso inválido",
                                "progress", 150
                        ))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/environments/" + envId + "/cards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "label", "Data inválida",
                                "deadline", "data-errada"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CORS aceita requisições de qualquer origem e porta")
    void corsAllowsAnyOriginAndMethods() throws Exception {
        mockMvc.perform(options("/environment/1/users")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("PATCH")));

        mockMvc.perform(options("/environment/1/archive-status")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("PATCH")));

        mockMvc.perform(options("/auth/login")
                        .header("Origin", "http://192.168.1.50:8081")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://192.168.1.50:8081"));
    }

    private int findPosition(JsonNode array, int cardId) {
        return findField(array, cardId, "position").asInt();
    }

    private int findColumn(JsonNode array, int cardId) {
        return findField(array, cardId, "column").asInt();
    }

    private JsonNode findField(JsonNode array, int cardId, String field) {
        for (JsonNode item : array) {
            if (item.get("id").asInt() == cardId) {
                return item.get(field);
            }
        }
        throw new AssertionError("Card " + cardId + " não encontrado no array: " + array);
    }
}

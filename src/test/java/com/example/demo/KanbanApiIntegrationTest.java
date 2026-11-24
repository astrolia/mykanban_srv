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
        MvcResult result = mockMvc.perform(post("/environments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "description", description
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.description").value(description))
                .andExpect(jsonPath("$.columns", hasSize(3)))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asInt();
    }

    @Test
    @DisplayName("Autenticação e ciclo completo de usuário (cadastro, login, perfil, senha, busca por email e exclusão)")
    void fullUserAuthAndProfileFlow() throws Exception {
        // Sem token deve retornar 401 (usado pelo interceptor do frontend)
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());

        String token = registerAndLogin("Aline Silva", "aline@example.com", "senha123");

        // Email duplicado -> 409
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Outra",
                                "email", "ALINE@example.com",
                                "password", "123456"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());

        // Login com senha errada -> 401
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "aline@example.com",
                                "password", "errada"
                        ))))
                .andExpect(status().isUnauthorized());

        // GET /users/me
        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Aline Silva"))
                .andExpect(jsonPath("$.email").value("aline@example.com"));

        // PUT /users/me -> atualiza nome e email e devolve novo token
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

        // GET /users/by-email -> retorna userEnv { id, label }
        mockMvc.perform(get("/users/by-email")
                        .param("email", "aline.nova@example.com")
                        .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.label").value("Aline Atualizada"));

        // PUT /users/me/password com senha atual incorreta -> 400
        mockMvc.perform(put("/users/me/password")
                        .header("Authorization", "Bearer " + newToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "errada",
                                "newPassword", "novaSenha123"
                        ))))
                .andExpect(status().isBadRequest());

        // PUT /users/me/password com senha nova curta -> 400
        mockMvc.perform(put("/users/me/password")
                        .header("Authorization", "Bearer " + newToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "senha123",
                                "newPassword", "123"
                        ))))
                .andExpect(status().isBadRequest());

        // PUT /users/me/password válido -> 200
        mockMvc.perform(put("/users/me/password")
                        .header("Authorization", "Bearer " + newToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currentPassword", "senha123",
                                "newPassword", "novaSenha123"
                        ))))
                .andExpect(status().isOk());

        // Login com a nova senha funciona
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "aline.nova@example.com",
                                "password", "novaSenha123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        // DELETE /users/me com senha incorreta -> 400
        mockMvc.perform(delete("/users/me")
                        .header("Authorization", "Bearer " + newToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "errada"))))
                .andExpect(status().isBadRequest());

        // DELETE /users/me com senha correta -> 204
        mockMvc.perform(delete("/users/me")
                        .header("Authorization", "Bearer " + newToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "novaSenha123"))))
                .andExpect(status().isNoContent());

        // Usuário removido não consegue mais logar
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "aline.nova@example.com",
                                "password", "novaSenha123"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Ambientes e gerenciamento de membros (CRUD, controle de acesso, adicionar/remover usuário e limpar dos cards)")
    void environmentAndMemberManagementFlow() throws Exception {
        String ownerToken = registerAndLogin("Dono", "dono@example.com", "senha123");
        String memberToken = registerAndLogin("Membro", "membro@example.com", "senha123");
        String outsiderToken = registerAndLogin("Estranho", "estranho@example.com", "senha123");

        int envId = createDefaultEnvironment(ownerToken, "Projeto TCC", "Kanban principal");

        // Estranho não pode acessar o ambiente -> 403
        mockMvc.perform(get("/environment/" + envId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());

        // Buscar membro por email (contrato UserService.findByEmail)
        MvcResult memberLookup = mockMvc.perform(get("/users/by-email")
                        .param("email", "membro@example.com")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        int memberId = objectMapper.readTree(memberLookup.getResponse().getContentAsString()).get("id").asInt();

        // Adicionar membro via PATCH /environment/{envId}/users
        mockMvc.perform(patch("/environment/" + envId + "/users")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", memberId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(memberId))
                .andExpect(jsonPath("$.label").value("Membro"));

        // Adicionar novamente -> 409
        mockMvc.perform(patch("/environment/" + envId + "/users")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", memberId))))
                .andExpect(status().isConflict());

        // Membro agora consegue carregar o ambiente via GET /environment/{envId}
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

        // Criar um card atribuído ao membro
        MvcResult cardRes = mockMvc.perform(post("/environments/" + envId + "/cards")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "data", Map.of(
                                        "label", "Card do Membro",
                                        "column", firstColId,
                                        "users", List.of(ownerId, memberId)
                                )
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.users", containsInAnyOrder(ownerId, memberId)))
                .andReturn();
        int cardId = objectMapper.readTree(cardRes.getResponse().getContentAsString()).get("id").asInt();

        // Não pode remover o dono -> 400
        mockMvc.perform(delete("/environment/" + envId + "/users")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", ownerId))))
                .andExpect(status().isBadRequest());

        // Membro não pode excluir o ambiente -> 403
        mockMvc.perform(delete("/environment/" + envId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());

        // Remover membro do ambiente -> também deve retirá-lo do card
        mockMvc.perform(delete("/environment/" + envId + "/users")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("userId", memberId))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/cards/" + cardId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", contains(ownerId)));

        // Atualizar ambiente via PATCH e alterar status de arquivamento (compatibilidade Frontend)
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

        // GET /users/{userId}/environments (chamado pela Home do frontend logo após o login)
        mockMvc.perform(get("/users/" + ownerId + "/environments")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Projeto TCC Editado"));

        // PATCH /environment/{envId}/archive-status
        mockMvc.perform(patch("/environment/" + envId + "/archive-status")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("archived", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        // Dono exclui o ambiente
        mockMvc.perform(delete("/environment/" + envId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/environment/" + envId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Colunas, Prioridades, Cards, Backlog, Arquivamento, Restauração e Drag-and-Drop (move)")
    void fullKanbanBoardWorkflow() throws Exception {
        String token = registerAndLogin("Gabriel", "gabriel@example.com", "senha123");
        int envId = createDefaultEnvironment(token, "Board Completo", "Testando tudo");

        // 1. Criar coluna usando payload encapsulado do frontend { column: { label } }
        MvcResult colRes = mockMvc.perform(post("/environments/" + envId + "/columns")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "column", Map.of("label", "Revisão")
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value("Revisão"))
                .andExpect(jsonPath("$.position").value(3))
                .andReturn();
        int reviewColId = objectMapper.readTree(colRes.getResponse().getContentAsString()).get("id").asInt();

        // Coluna duplicada (case-insensitive) -> 409
        mockMvc.perform(post("/environments/" + envId + "/columns")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "column", Map.of("label", "revisão")
                        ))))
                .andExpect(status().isConflict());

        // Editar coluna
        mockMvc.perform(put("/columns/" + reviewColId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "id", reviewColId,
                                "label", "Code Review"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Code Review"));

        // Listar colunas
        MvcResult colsListRes = mockMvc.perform(get("/environments/" + envId + "/columns")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andReturn();
        JsonNode colsJson = objectMapper.readTree(colsListRes.getResponse().getContentAsString());
        int todoColId = colsJson.get(0).get("id").asInt();
        int inProgressColId = colsJson.get(1).get("id").asInt();

        // 2. Prioridades: criar usando payload { data: { label, css } }, editar e depois deletar
        MvcResult priRes = mockMvc.perform(post("/environments/" + envId + "/priority")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "data", Map.of("label", "Alta", "css", "priority-red")
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value("Alta"))
                .andExpect(jsonPath("$.css").value("priority-red"))
                .andReturn();
        int highPriId = objectMapper.readTree(priRes.getResponse().getContentAsString()).get("id").asInt();

        mockMvc.perform(put("/priority/" + highPriId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "id", highPriId,
                                "label", "Urgente",
                                "css", "priority-orange"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Urgente"))
                .andExpect(jsonPath("$.css").value("priority-orange"));

        // 3. Criar 3 cards na coluna To Do (com payload { data: { ... } } e deadline ISO igual ao frontend)
        MvcResult c1Res = mockMvc.perform(post("/environments/" + envId + "/cards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "data", Map.of(
                                        "label", "Card 1",
                                        "description", "Descrição inicial",
                                        "column", todoColId,
                                        "priority", highPriId,
                                        "progress", 30,
                                        "deadline", "2026-12-15T03:00:00.000Z",
                                        "archived", false
                                )
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
                        .content(objectMapper.writeValueAsString(Map.of(
                                "data", Map.of("label", "Card 2", "column", todoColId)
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(1))
                .andReturn();
        int card2Id = objectMapper.readTree(c2Res.getResponse().getContentAsString()).get("id").asInt();

        MvcResult c3Res = mockMvc.perform(post("/environments/" + envId + "/cards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "data", Map.of("label", "Card 3", "column", todoColId)
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(2))
                .andReturn();
        int card3Id = objectMapper.readTree(c3Res.getResponse().getContentAsString()).get("id").asInt();

        // Criar card de Backlog (column = null)
        mockMvc.perform(post("/environments/" + envId + "/cards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "data", Map.of("label", "Item de Backlog")
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.column").value(nullValue()));

        mockMvc.perform(get("/environments/" + envId + "/cards")
                        .param("backlog", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].label").value("Item de Backlog"));

        // 4. Tentar deletar coluna com cards -> 409
        mockMvc.perform(delete("/columns/" + todoColId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());

        // Deletar coluna vazia -> 200 retornando as colunas restantes reordenadas
        mockMvc.perform(delete("/columns/" + reviewColId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        // 5. Editar card (sem alterar archived -> retorna CardEnv único)
        mockMvc.perform(put("/cards/" + card1Id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "id", card1Id,
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

        // 6. Mover Card 3 para antes de Card 1 dentro da mesma coluna (Card 3 -> pos 0, Card 1 -> pos 1, Card 2 -> pos 2)
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

        // 7. Mover Card 2 para outra coluna (In Progress) no fim
        MvcResult moveOtherCol = mockMvc.perform(put("/cards/move/" + card2Id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("column", inProgressColId))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode afterMove2 = objectMapper.readTree(moveOtherCol.getResponse().getContentAsString());
        assertThat(findColumn(afterMove2, card2Id)).isEqualTo(inProgressColId);
        assertThat(findPosition(afterMove2, card2Id)).isEqualTo(0);

        // 8. Arquivar Card 1 (PUT /cards/{id} mudando archived para true -> retorna CardEnv[])
        MvcResult archiveRes = mockMvc.perform(put("/cards/" + card1Id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "id", card1Id,
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

        // 9. Restaurar Card 1 (PUT /cards/{id} mudando archived para false -> retorna CardEnv[])
        MvcResult restoreRes = mockMvc.perform(put("/cards/" + card1Id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "id", card1Id,
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

        // 10. Deletar prioridade -> deve limpar a prioridade nos cards que a usavam
        mockMvc.perform(delete("/priority/" + highPriId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/cards/" + card1Id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value(nullValue()));

        // 11. Excluir card definitivamente via endpoint permanente
        mockMvc.perform(delete("/cards/" + card1Id + "/permanent")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/cards/" + card1Id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Exclusão em cascata segura: deletar ambiente com cards/prioridades e deletar conta com vínculos")
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

        // Bob entra no ambiente de Alice; Alice entra no ambiente de Bob
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

        // Criar prioridade e card no ambiente da Alice
        JsonNode aliceEnv = objectMapper.readTree(
                mockMvc.perform(get("/environment/" + aliceEnvId).header("Authorization", "Bearer " + aliceToken))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()
        );
        int aliceColId = aliceEnv.get("columns").get(0).get("id").asInt();

        int priId = objectMapper.readTree(
                mockMvc.perform(post("/environments/" + aliceEnvId + "/priority")
                                .header("Authorization", "Bearer " + aliceToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("data", Map.of("label", "Crítica", "css", "priority-red")))))
                        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()
        ).get("id").asInt();

        mockMvc.perform(post("/environments/" + aliceEnvId + "/cards")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("data", Map.of(
                                "label", "Card na Alice",
                                "column", aliceColId,
                                "priority", priId,
                                "users", List.of(aliceId, bobId)
                        )))))
                .andExpect(status().isCreated());

        // Criar card no ambiente do Bob atribuído à Alice
        JsonNode bobEnv = objectMapper.readTree(
                mockMvc.perform(get("/environment/" + bobEnvId).header("Authorization", "Bearer " + bobToken))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()
        );
        int bobColId = bobEnv.get("columns").get(0).get("id").asInt();

        int bobCardId = objectMapper.readTree(
                mockMvc.perform(post("/environments/" + bobEnvId + "/cards")
                                .header("Authorization", "Bearer " + bobToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("data", Map.of(
                                        "label", "Card no Bob",
                                        "column", bobColId,
                                        "users", List.of(aliceId, bobId)
                                )))))
                        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()
        ).get("id").asInt();

        // Alice exclui sua conta mesmo sendo dona de ambiente populado e membro/responsável no ambiente do Bob
        mockMvc.perform(delete("/users/me")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "senha123"))))
                .andExpect(status().isNoContent());

        // Card no ambiente do Bob continua íntegro, agora só com Bob
        mockMvc.perform(get("/cards/" + bobCardId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", contains(bobId)));

        // Bob consegue excluir seu ambiente mesmo com cards nele
        mockMvc.perform(delete("/environment/" + bobEnvId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Mover card entre colunas antes de outro card e mover card do backlog para coluna")
    void moveCardBeforeTargetAndFromBacklog() throws Exception {
        String token = registerAndLogin("Carlos", "carlos@example.com", "senha123");
        int envId = createDefaultEnvironment(token, "Kanban Move", "Teste drag-and-drop");

        JsonNode cols = objectMapper.readTree(
                mockMvc.perform(get("/environments/" + envId + "/columns")
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()
        );
        int col1 = cols.get(0).get("id").asInt();
        int col2 = cols.get(1).get("id").asInt();

        int cA = objectMapper.readTree(
                mockMvc.perform(post("/environments/" + envId + "/cards")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("data", Map.of("label", "A", "column", col2)))))
                        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()
        ).get("id").asInt();

        int cB = objectMapper.readTree(
                mockMvc.perform(post("/environments/" + envId + "/cards")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("data", Map.of("label", "B", "column", col1)))))
                        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()
        ).get("id").asInt();

        int cBacklog = objectMapper.readTree(
                mockMvc.perform(post("/environments/" + envId + "/cards")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("data", Map.of("label", "Backlog")))))
                        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()
        ).get("id").asInt();

        // Mover B (de col1) para col2 ANTES de A -> B fica pos 0, A fica pos 1
        JsonNode moved1 = objectMapper.readTree(
                mockMvc.perform(put("/cards/move/" + cB)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("column", col2, "before", cA))))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()
        );
        assertThat(findColumn(moved1, cB)).isEqualTo(col2);
        assertThat(findPosition(moved1, cB)).isEqualTo(0);
        assertThat(findPosition(moved1, cA)).isEqualTo(1);

        // Mover Backlog para col2 ANTES de A -> B=0, Backlog=1, A=2
        JsonNode moved2 = objectMapper.readTree(
                mockMvc.perform(put("/cards/move/" + cBacklog)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("column", col2, "before", cA))))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()
        );
        assertThat(findColumn(moved2, cBacklog)).isEqualTo(col2);
        assertThat(findPosition(moved2, cB)).isEqualTo(0);
        assertThat(findPosition(moved2, cBacklog)).isEqualTo(1);
        assertThat(findPosition(moved2, cA)).isEqualTo(2);
    }

    @Test
    @DisplayName("CORS preflight permite PATCH/PUT/DELETE vindos do frontend localhost:5173")
    void corsAllowsFrontendMethods() throws Exception {
        mockMvc.perform(options("/environment/1/users")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("PATCH")));
    }

    @Test
    @DisplayName("Ciclo completo de Arquivo: soft delete convencional, listagem de arquivados, restore, endpoint dedicado e delete definitivo")
    void archiveRestoreAndSoftDeleteWorkflow() throws Exception {
        String token = registerAndLogin("Marina", "marina@example.com", "senha123");
        int envId = createDefaultEnvironment(token, "Ambiente Arquivo", "Testando novas funcionalidades");

        JsonNode envJson = objectMapper.readTree(
                mockMvc.perform(get("/environment/" + envId)
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()
        );
        int col1Id = envJson.get("columns").get(0).get("id").asInt();
        int col2Id = envJson.get("columns").get(1).get("id").asInt();

        // 1. Criar card com novos atributos: prioridade, progresso e data
        MvcResult cardRes = mockMvc.perform(post("/environments/" + envId + "/cards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "data", Map.of(
                                        "label", "Tarefa de Teste",
                                        "description", "Detalhes da tarefa",
                                        "column", col1Id,
                                        "progress", 45.5,
                                        "deadline", "2026-11-30"
                                )
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value("Tarefa de Teste"))
                .andExpect(jsonPath("$.progress").value(45.5))
                .andExpect(jsonPath("$.deadline").value("2026-11-30"))
                .andExpect(jsonPath("$.archived").value(false))
                .andReturn();
        int cardId = objectMapper.readTree(cardRes.getResponse().getContentAsString()).get("id").asInt();

        // 2. Exclusão convencional via DELETE /cards/{id} -> deve realizar SOFT DELETE (não remove do banco)
        mockMvc.perform(delete("/cards/" + cardId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Card continua no banco com archived = true
        mockMvc.perform(get("/cards/" + cardId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId))
                .andExpect(jsonPath("$.archived").value(true));

        // 3. Listar arquivados via endpoint dedicado GET /environments/{envId}/cards/archived
        mockMvc.perform(get("/environments/" + envId + "/cards/archived")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(cardId))
                .andExpect(jsonPath("$[0].archived").value(true));

        // 4. Restaurar card via endpoint dedicado POST /cards/{id}/restore (indicando a coluna 2)
        mockMvc.perform(post("/cards/" + cardId + "/restore")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("column", col2Id))))
                .andExpect(status().isOk());

        // Card agora está ativo na coluna 2
        mockMvc.perform(get("/cards/" + cardId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(false))
                .andExpect(jsonPath("$.column").value(col2Id));

        // 5. Arquivar card via endpoint dedicado POST /cards/{id}/archive
        mockMvc.perform(post("/cards/" + cardId + "/archive")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("done", true))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/cards/" + cardId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true))
                .andExpect(jsonPath("$.done").value(true));

        // 6. Excluir coluna 2 com card arquivado nela: deve desassociar a coluna do card sem conflito
        mockMvc.perform(delete("/columns/" + col2Id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/cards/" + cardId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.column").value(nullValue()));

        // 7. Excluir definitivamente via DELETE /cards/{id}/permanent
        mockMvc.perform(delete("/cards/" + cardId + "/permanent")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Agora sim foi removido permanentemente do banco
        mockMvc.perform(get("/cards/" + cardId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
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

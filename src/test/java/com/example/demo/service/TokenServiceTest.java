package com.example.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    private static final String SECRET = "chave-secreta-para-testes-unitarios-jwt-com-tamanho-suficiente-256bits";
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(SECRET);
    }

    @Test
    @DisplayName("Deve gerar token JWT válido e recuperar o email do assunto")
    void shouldGenerateAndValidateToken() {
        String token = tokenService.generate("aline@example.com");

        assertThat(token).isNotBlank();
        assertThat(tokenService.validate(token)).isEqualTo("aline@example.com");
    }

    @Test
    @DisplayName("Deve rejeitar token adulterado")
    void shouldRejectTamperedToken() {
        String token = tokenService.generate("aline@example.com");
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThat(tokenService.validate(tampered)).isNull();
    }

    @Test
    @DisplayName("Deve rejeitar token assinado com outra chave")
    void shouldRejectTokenWithDifferentSecret() {
        TokenService otherService = new TokenService("outra-chave-completamente-diferente-para-teste-de-assinatura-jwt");
        String foreignToken = otherService.generate("aline@example.com");

        assertThat(tokenService.validate(foreignToken)).isNull();
    }

    @Test
    @DisplayName("Deve rejeitar token nulo, vazio ou malformado")
    void shouldRejectNullEmptyOrMalformedToken() {
        assertThat(tokenService.validate(null)).isNull();
        assertThat(tokenService.validate("")).isNull();
        assertThat(tokenService.validate("token.invalido")).isNull();
    }
}

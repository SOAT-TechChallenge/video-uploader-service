package com.videoUploaderService.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.videoUploaderService.service.TokenService.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class TokenServiceTest {

    private TokenService tokenService;
    
    private final String TEST_SECRET = "segredo-de-teste-123";

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", TEST_SECRET);
    }

    @Test
    @DisplayName("Deve decodificar token válido e retornar UserInfo corretamente")
    void decodeToken_ShouldReturnUserInfo_WhenTokenIsValid() {
        // Arrange
        String username = "usuario_teste";
        String email = "teste@email.com";
        
        String token = JWT.create()
                .withSubject(username)
                .withClaim("email", email)
                .sign(Algorithm.HMAC256(TEST_SECRET));

        String tokenHeader = "Bearer " + token;

        // Act
        UserInfo result = tokenService.decodeToken(tokenHeader);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.username());
        assertEquals(email, result.email());
    }

    @Test
    @DisplayName("Deve lançar exception quando a assinatura do token for inválida")
    void decodeToken_ShouldThrowException_WhenSignatureIsInvalid() {
        // Arrange
        // Gera token assinado com um segredo DIFERENTE
        String token = JWT.create()
                .withSubject("hacker")
                .sign(Algorithm.HMAC256("segredo-errado"));

        String tokenHeader = "Bearer " + token;

        // Act & Assert
        assertThrows(JWTVerificationException.class, () -> {
            tokenService.decodeToken(tokenHeader);
        });
    }

    @Test
    @DisplayName("Deve lançar exception quando o token estiver expirado")
    void decodeToken_ShouldThrowException_WhenTokenIsExpired() {
        // Arrange
        String token = JWT.create()
                .withSubject("user")
                .withExpiresAt(Date.from(Instant.now().minus(1, ChronoUnit.HOURS))) // Expirou há 1 hora
                .sign(Algorithm.HMAC256(TEST_SECRET));

        String tokenHeader = "Bearer " + token;

        // Act & Assert
        assertThrows(JWTVerificationException.class, () -> {
            tokenService.decodeToken(tokenHeader);
        });
    }

    @Test
    @DisplayName("Deve lançar exception quando o token estiver mal formatado")
    void decodeToken_ShouldThrowException_WhenTokenIsMalformed() {
        // Arrange
        String tokenHeader = "Bearer token-todo-errado-que-nao-é-jwt";

        // Act & Assert
        assertThrows(JWTVerificationException.class, () -> {
            tokenService.decodeToken(tokenHeader);
        });
    }
}
package com.videoUploaderService.controller;

import com.videoUploaderService.service.TokenService;
import com.videoUploaderService.service.TokenService.UserInfo; // Importante para o objeto UserInfo
import com.videoUploaderService.service.VideoQueueService;
import com.videoUploaderService.service.VideoStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoUploadControllerTest {

    @Mock
    private VideoStorageService videoStorageService;

    @Mock
    private VideoQueueService videoQueueService;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private VideoUploadController videoUploadController;

    private MockMultipartFile validFile;
    private MockMultipartFile emptyFile;
    private String validToken;
    private UserInfo userInfo;

    @BeforeEach
    void setUp() {
        validFile = new MockMultipartFile(
                "file",
                "test-video.mp4",
                "video/mp4",
                "test video content".getBytes()
        );

        emptyFile = new MockMultipartFile(
                "file",
                "empty.mp4",
                "video/mp4",
                new byte[0]
        );

        validToken = "Bearer token-valido-123";
        userInfo = new UserInfo("usuarioTeste", "email@teste.com");
    }

    @Test
    @DisplayName("Sucesso: Upload com descrição e token válido")
    void uploadVideo_Success_WithDescription() throws IOException {
        // Arrange
        String s3Key = "videos/123.mp4";
        String s3Url = "https://s3.aws/123.mp4";
        String title = "Test Video";
        String description = "Test Description";

        // Simula a decodificação do token
        when(tokenService.decodeToken(validToken)).thenReturn(userInfo);
        
        when(videoStorageService.uploadVideo(any(MultipartFile.class))).thenReturn(s3Key);
        when(videoStorageService.getVideoUrl(s3Key)).thenReturn(s3Url);
        
        // Simula o envio para a fila
        doNothing().when(videoQueueService).sendVideoMessage(
            s3Key, s3Url, title, description, userInfo.username(), userInfo.email()
        );

        // Act (Passando o token como primeiro argumento)
        ResponseEntity<?> response = videoUploadController.uploadVideo(validToken, validFile, title, description);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        
        verify(tokenService).decodeToken(validToken);
        verify(videoQueueService).sendVideoMessage(s3Key, s3Url, title, description, userInfo.username(), userInfo.email());
    }

    @Test
    @DisplayName("Sucesso: Upload sem descrição")
    void uploadVideo_Success_WithoutDescription() throws IOException {
        // Arrange
        String s3Key = "videos/123.mp4";
        String s3Url = "https://s3.aws/123.mp4";
        String title = "Test Video";

        when(tokenService.decodeToken(validToken)).thenReturn(userInfo);
        when(videoStorageService.uploadVideo(any(MultipartFile.class))).thenReturn(s3Key);
        when(videoStorageService.getVideoUrl(s3Key)).thenReturn(s3Url);

        // Act
        ResponseEntity<?> response = videoUploadController.uploadVideo(validToken, validFile, title, null);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        verify(videoQueueService).sendVideoMessage(s3Key, s3Url, title, null, userInfo.username(), userInfo.email());
    }

    @Test
    @DisplayName("Erro 400: Arquivo vazio")
    void uploadVideo_EmptyFile_ReturnsBadRequest() throws IOException {
        // Act
        ResponseEntity<?> response = videoUploadController.uploadVideo(validToken, emptyFile, "Title", null);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Arquivo de vídeo é obrigatório", response.getBody());

        // Garante que não chamou nada
        verifyNoInteractions(tokenService);
        verifyNoInteractions(videoStorageService);
    }

    @Test
    @DisplayName("Erro 500: Falha no S3")
    void uploadVideo_IOException_ReturnsInternalServerError() throws IOException {
        // Arrange
        when(tokenService.decodeToken(validToken)).thenReturn(userInfo);
        when(videoStorageService.uploadVideo(any(MultipartFile.class))).thenThrow(new IOException("S3 falhou"));

        // Act
        ResponseEntity<?> response = videoUploadController.uploadVideo(validToken, validFile, "Title", null);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        verify(videoStorageService).uploadVideo(validFile);
        // Garante que NÃO mandou para fila se o upload falhou
        verifyNoInteractions(videoQueueService);
    }
    
    @Test
    @DisplayName("Erro 400: Arquivo nulo")
    void uploadVideo_NullFile_ReturnsBadRequest() throws IOException {
        // Act
        ResponseEntity<?> response = videoUploadController.uploadVideo(validToken, null, "Title", null);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Erro 401: Token inválido")
    void uploadVideo_InvalidToken_ReturnsUnauthorized() throws IOException {
        // Arrange
        String invalidToken = "Bearer invalid";
        when(tokenService.decodeToken(invalidToken)).thenThrow(new RuntimeException("Token expirado"));

        // Act
        ResponseEntity<?> response = videoUploadController.uploadVideo(invalidToken, validFile, "Title", null);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        
        verifyNoInteractions(videoStorageService);
    }
}
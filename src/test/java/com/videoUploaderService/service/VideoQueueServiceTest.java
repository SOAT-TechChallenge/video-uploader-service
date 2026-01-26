package com.videoUploaderService.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils; // Para injetar o @Value

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoQueueServiceTest {

    @Mock
    private AmazonSQS amazonSQS;

    @Mock
    private ObjectMapper objectMapper;

    private VideoQueueService videoQueueService;

    @Captor
    private ArgumentCaptor<SendMessageRequest> sendMessageRequestCaptor;

    private final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123/video-queue";

    @BeforeEach
    void setUp() {
        // Instancia a service passando os Mocks
        videoQueueService = new VideoQueueService(amazonSQS, QUEUE_URL, objectMapper);
    }

    @Test
    @DisplayName("Deve enviar mensagem para o SQS com sucesso e payload completo")
    void sendVideoMessage_Success_WithDescription() throws JsonProcessingException {
        // Arrange
        String s3Key = "videos/file.mp4";
        String s3Url = "http://s3/videos/file.mp4";
        String title = "Titulo";
        String desc = "Descricao";
        String user = "usuario";
        String email = "email@teste.com";
        String expectedJsonBody = "{\"json\":\"mock\"}";

        // Mockamos o comportamento do ObjectMapper
        // IMPORTANTE: Usamos any(Map.class) para evitar problemas de matching com Mapas
        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn(expectedJsonBody);

        // Act
        videoQueueService.sendVideoMessage(s3Key, s3Url, title, desc, user, email);

        // Assert
        // 1. Verifica se serializou o JSON
        verify(objectMapper, times(1)).writeValueAsString(any(Map.class));

        // 2. Captura o que foi enviado para o SQS
        verify(amazonSQS, times(1)).sendMessage(sendMessageRequestCaptor.capture());
        
        SendMessageRequest capturedRequest = sendMessageRequestCaptor.getValue();
        
        // 3. Validações
        assertEquals(QUEUE_URL, capturedRequest.getQueueUrl());
        assertEquals(expectedJsonBody, capturedRequest.getMessageBody());
    }

    @Test
    @DisplayName("Deve enviar mensagem corretamente mesmo sem descrição")
    void sendVideoMessage_Success_WithoutDescription() throws JsonProcessingException {
        // Arrange
        String expectedJsonBody = "{\"json\":\"mock_no_desc\"}";
        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn(expectedJsonBody);

        // Act
        videoQueueService.sendVideoMessage("key", "url", "Title", null, "user", "email");

        // Assert
        verify(amazonSQS).sendMessage(sendMessageRequestCaptor.capture());
        assertEquals(expectedJsonBody, sendMessageRequestCaptor.getValue().getMessageBody());
    }

    @Test
    @DisplayName("Deve lançar RuntimeException quando falhar a serialização do JSON")
    void sendVideoMessage_JsonProcessingException_ThrowsRuntimeException() throws JsonProcessingException {
        // Arrange
        // Simulamos o erro do Jackson
        when(objectMapper.writeValueAsString(any(Map.class)))
                .thenThrow(new JsonProcessingException("Erro simulação") {});

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            videoQueueService.sendVideoMessage("key", "url", "Title", "Desc", "user", "email")
        );

        assertEquals("Erro ao serializar mensagem de vídeo", exception.getMessage());

        // Garante que NÂO tentou enviar para o SQS se falhou o JSON
        verifyNoInteractions(amazonSQS);
    }
}
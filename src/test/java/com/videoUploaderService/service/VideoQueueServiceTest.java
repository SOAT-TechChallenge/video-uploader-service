package com.videoUploaderService.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

    @InjectMocks
    private VideoQueueService videoQueueService;

    private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789012/video-queue";
    private SendMessageResult sendMessageResult;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(videoQueueService, "queueUrl", QUEUE_URL);
        sendMessageResult = new SendMessageResult();
        sendMessageResult.setMessageId("test-message-id");
    }

    @Test
    void sendVideoMessage_Success_WithDescription() throws JsonProcessingException {
        // Arrange
        String s3Key = "videos/1234567890-abc123.mp4";
        String s3Url = "https://s3.amazonaws.com/bucket/videos/1234567890-abc123.mp4";
        String title = "Test Video";
        String description = "Test Description";
        String jsonBody = "{\"s3Key\":\"videos/1234567890-abc123.mp4\",\"s3Url\":\"https://s3.amazonaws.com/bucket/videos/1234567890-abc123.mp4\",\"title\":\"Test Video\",\"description\":\"Test Description\",\"uploadedAt\":\"2024-01-01T00:00:00Z\"}";

        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);

        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn(jsonBody);
        when(amazonSQS.sendMessage(any(SendMessageRequest.class))).thenReturn(sendMessageResult);

        // Act
        videoQueueService.sendVideoMessage(s3Key, s3Url, title, description);

        // Assert
        verify(objectMapper, times(1)).writeValueAsString(any(Map.class));
        verify(amazonSQS, times(1)).sendMessage(requestCaptor.capture());

        SendMessageRequest capturedRequest = requestCaptor.getValue();
        assertEquals(QUEUE_URL, capturedRequest.getQueueUrl());
        assertEquals(jsonBody, capturedRequest.getMessageBody());
    }

    @Test
    void sendVideoMessage_Success_WithoutDescription() throws JsonProcessingException {
        // Arrange
        String s3Key = "videos/1234567890-abc123.mp4";
        String s3Url = "https://s3.amazonaws.com/bucket/videos/1234567890-abc123.mp4";
        String title = "Test Video";
        String jsonBody = "{\"s3Key\":\"videos/1234567890-abc123.mp4\",\"s3Url\":\"https://s3.amazonaws.com/bucket/videos/1234567890-abc123.mp4\",\"title\":\"Test Video\",\"description\":null,\"uploadedAt\":\"2024-01-01T00:00:00Z\"}";

        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);

        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn(jsonBody);
        when(amazonSQS.sendMessage(any(SendMessageRequest.class))).thenReturn(sendMessageResult);

        // Act
        videoQueueService.sendVideoMessage(s3Key, s3Url, title, null);

        // Assert
        verify(objectMapper, times(1)).writeValueAsString(mapCaptor.capture());
        verify(amazonSQS, times(1)).sendMessage(any(SendMessageRequest.class));

        Map<String, Object> capturedMap = mapCaptor.getValue();
        assertEquals(s3Key, capturedMap.get("s3Key"));
        assertEquals(s3Url, capturedMap.get("s3Url"));
        assertEquals(title, capturedMap.get("title"));
        assertNull(capturedMap.get("description"));
        assertNotNull(capturedMap.get("uploadedAt"));
    }

    @Test
    void sendVideoMessage_Success_VerifiesPayloadStructure() throws JsonProcessingException {
        // Arrange
        String s3Key = "videos/test-key.mp4";
        String s3Url = "https://s3.amazonaws.com/bucket/videos/test-key.mp4";
        String title = "My Video";
        String description = "My Description";
        String jsonBody = "{}";

        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);

        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn(jsonBody);
        when(amazonSQS.sendMessage(any(SendMessageRequest.class))).thenReturn(sendMessageResult);

        // Act
        videoQueueService.sendVideoMessage(s3Key, s3Url, title, description);

        // Assert
        verify(objectMapper, times(1)).writeValueAsString(mapCaptor.capture());

        Map<String, Object> payload = mapCaptor.getValue();
        assertEquals(5, payload.size());
        assertEquals(s3Key, payload.get("s3Key"));
        assertEquals(s3Url, payload.get("s3Url"));
        assertEquals(title, payload.get("title"));
        assertEquals(description, payload.get("description"));
        assertNotNull(payload.get("uploadedAt"));
        assertTrue(payload.get("uploadedAt") instanceof String);
    }

    @Test
    void sendVideoMessage_JsonProcessingException_ThrowsRuntimeException() throws JsonProcessingException {
        // Arrange
        String s3Key = "videos/test.mp4";
        String s3Url = "https://s3.amazonaws.com/bucket/videos/test.mp4";
        String title = "Test";
        JsonProcessingException jsonException = new JsonProcessingException("JSON error") {};

        when(objectMapper.writeValueAsString(any(Map.class))).thenThrow(jsonException);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                videoQueueService.sendVideoMessage(s3Key, s3Url, title, null)
        );

        assertEquals("Erro ao serializar mensagem de vídeo", exception.getMessage());
        assertSame(jsonException, exception.getCause());

        verify(objectMapper, times(1)).writeValueAsString(any(Map.class));
        verify(amazonSQS, never()).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void sendVideoMessage_EmptyStrings_HandlesCorrectly() throws JsonProcessingException {
        // Arrange
        String s3Key = "";
        String s3Url = "";
        String title = "";
        String jsonBody = "{}";

        when(objectMapper.writeValueAsString(any(Map.class))).thenReturn(jsonBody);
        when(amazonSQS.sendMessage(any(SendMessageRequest.class))).thenReturn(sendMessageResult);

        // Act
        videoQueueService.sendVideoMessage(s3Key, s3Url, title, null);

        // Assert
        verify(objectMapper, times(1)).writeValueAsString(any(Map.class));
        verify(amazonSQS, times(1)).sendMessage(any(SendMessageRequest.class));
    }
}



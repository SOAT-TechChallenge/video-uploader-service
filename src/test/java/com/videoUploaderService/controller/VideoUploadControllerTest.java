package com.videoUploaderService.controller;

import com.videoUploaderService.service.VideoQueueService;
import com.videoUploaderService.service.VideoStorageService;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoUploadControllerTest {

    @Mock
    private VideoStorageService videoStorageService;

    @Mock
    private VideoQueueService videoQueueService;

    @InjectMocks
    private VideoUploadController videoUploadController;

    private MockMultipartFile validFile;
    private MockMultipartFile emptyFile;

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
    }

    @Test
    void uploadVideo_Success_WithDescription() throws IOException {
        // Arrange
        String s3Key = "videos/1234567890-abc123.mp4";
        String s3Url = "https://s3.amazonaws.com/bucket/videos/1234567890-abc123.mp4";
        String title = "Test Video";
        String description = "Test Description";

        when(videoStorageService.uploadVideo(any(MultipartFile.class))).thenReturn(s3Key);
        when(videoStorageService.getVideoUrl(s3Key)).thenReturn(s3Url);
        doNothing().when(videoQueueService).sendVideoMessage(s3Key, s3Url, title, description);

        // Act
        ResponseEntity<?> response = videoUploadController.uploadVideo(validFile, title, description);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Upload realizado com sucesso", body.get("message"));
        assertEquals(s3Key, body.get("s3Key"));
        assertEquals(s3Url, body.get("s3Url"));

        verify(videoStorageService, times(1)).uploadVideo(validFile);
        verify(videoStorageService, times(1)).getVideoUrl(s3Key);
        verify(videoQueueService, times(1)).sendVideoMessage(s3Key, s3Url, title, description);
    }

    @Test
    void uploadVideo_Success_WithoutDescription() throws IOException {
        // Arrange
        String s3Key = "videos/1234567890-abc123.mp4";
        String s3Url = "https://s3.amazonaws.com/bucket/videos/1234567890-abc123.mp4";
        String title = "Test Video";

        when(videoStorageService.uploadVideo(any(MultipartFile.class))).thenReturn(s3Key);
        when(videoStorageService.getVideoUrl(s3Key)).thenReturn(s3Url);
        doNothing().when(videoQueueService).sendVideoMessage(eq(s3Key), eq(s3Url), eq(title), isNull());

        // Act
        ResponseEntity<?> response = videoUploadController.uploadVideo(validFile, title, null);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(videoStorageService, times(1)).uploadVideo(validFile);
        verify(videoStorageService, times(1)).getVideoUrl(s3Key);
        verify(videoQueueService, times(1)).sendVideoMessage(s3Key, s3Url, title, null);
    }

    @Test
    void uploadVideo_EmptyFile_ReturnsBadRequest() throws IOException {
        // Act
        ResponseEntity<?> response = videoUploadController.uploadVideo(emptyFile, "Test Title", null);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Arquivo de vídeo é obrigatório", response.getBody());

        verify(videoStorageService, never()).uploadVideo(any(MultipartFile.class));
        verify(videoQueueService, never()).sendVideoMessage(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void uploadVideo_IOException_ReturnsInternalServerError() throws IOException {
        // Arrange
        String title = "Test Video";
        IOException ioException = new IOException("S3 connection failed");

        when(videoStorageService.uploadVideo(any(MultipartFile.class))).thenThrow(ioException);

        // Act
        ResponseEntity<?> response = videoUploadController.uploadVideo(validFile, title, null);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Erro ao fazer upload do vídeo"));
        assertTrue(response.getBody().toString().contains("S3 connection failed"));

        verify(videoStorageService, times(1)).uploadVideo(validFile);
        verify(videoStorageService, never()).getVideoUrl(anyString());
        verify(videoQueueService, never()).sendVideoMessage(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void uploadVideo_NullFile_ReturnsBadRequest() throws IOException {
        // Act
        ResponseEntity<?> response = videoUploadController.uploadVideo(null, "Test Title", null);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Arquivo de vídeo é obrigatório", response.getBody());

        verify(videoStorageService, never()).uploadVideo(any(MultipartFile.class));
        verify(videoQueueService, never()).sendVideoMessage(anyString(), anyString(), anyString(), anyString());
    }
}


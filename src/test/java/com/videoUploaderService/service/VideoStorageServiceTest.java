package com.videoUploaderService.service;

import com.amazonaws.services.s3.AmazonS3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoStorageServiceTest {

    @Mock
    private AmazonS3 amazonS3;

    @InjectMocks
    private VideoStorageService videoStorageService;

    private static final String BUCKET_NAME = "test-bucket";
    private MockMultipartFile testFile;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(videoStorageService, "bucketName", BUCKET_NAME);
        testFile = new MockMultipartFile(
                "file",
                "test-video.mp4",
                "video/mp4",
                "test video content".getBytes()
        );
    }

    @Test
    void uploadVideo_Success_WithExtension() throws IOException {
        // Arrange
        ArgumentCaptor<String> bucketCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        // Mock do putObject - método void, não precisa de configuração especial

        // Act
        String result = videoStorageService.uploadVideo(testFile);

        // Assert
        assertNotNull(result);
        assertTrue(result.startsWith("videos/"));
        assertTrue(result.endsWith(".mp4"));
        assertTrue(result.contains("-"));

        verify(amazonS3, times(1)).putObject(
                bucketCaptor.capture(),
                keyCaptor.capture(),
                any(java.io.InputStream.class),
                isNull()
        );

        assertEquals(BUCKET_NAME, bucketCaptor.getValue());
        assertEquals(result, keyCaptor.getValue());
    }

    @Test
    void uploadVideo_Success_WithoutExtension() throws IOException {
        // Arrange
        MockMultipartFile fileWithoutExtension = new MockMultipartFile(
                "file",
                "test-video",
                "video/mp4",
                "test content".getBytes()
        );

        // Mock do putObject - método void, não precisa de configuração especial

        // Act
        String result = videoStorageService.uploadVideo(fileWithoutExtension);

        // Assert
        assertNotNull(result);
        assertTrue(result.startsWith("videos/"));
        assertFalse(result.contains("."));
        assertTrue(result.contains("-"));

        verify(amazonS3, times(1)).putObject(eq(BUCKET_NAME), eq(result), any(), isNull());
    }

    @Test
    void uploadVideo_Success_WithMultipleDotsInFilename() throws IOException {
        // Arrange
        MockMultipartFile fileWithMultipleDots = new MockMultipartFile(
                "file",
                "test.video.file.mp4",
                "video/mp4",
                "test content".getBytes()
        );

        // Mock do putObject - método void, não precisa de configuração especial

        // Act
        String result = videoStorageService.uploadVideo(fileWithMultipleDots);

        // Assert
        assertNotNull(result);
        assertTrue(result.startsWith("videos/"));
        assertTrue(result.endsWith(".mp4"));
        assertTrue(result.contains("-"));

        verify(amazonS3, times(1)).putObject(eq(BUCKET_NAME), eq(result), any(), isNull());
    }

    @Test
    void uploadVideo_Success_NullFilename() throws IOException {
        // Arrange
        MockMultipartFile fileWithNullFilename = new MockMultipartFile(
                "file",
                null,
                "video/mp4",
                "test content".getBytes()
        );

        // Mock do putObject - método void, não precisa de configuração especial

        // Act
        String result = videoStorageService.uploadVideo(fileWithNullFilename);

        // Assert
        assertNotNull(result);
        assertTrue(result.startsWith("videos/"));
        assertFalse(result.contains("."));
        assertTrue(result.contains("-"));

        verify(amazonS3, times(1)).putObject(eq(BUCKET_NAME), eq(result), any(), isNull());
    }

    @Test
    void uploadVideo_IOException_PropagatesException() throws IOException {
        // Arrange
        // Simula IOException ao ler o InputStream do arquivo
        MockMultipartFile fileWithIOException = new MockMultipartFile(
                "file",
                "test.mp4",
                "video/mp4",
                "test".getBytes()
        ) {
            @Override
            public java.io.InputStream getInputStream() throws IOException {
                throw new IOException("S3 upload failed");
            }
        };

        // Act & Assert
        assertThrows(IOException.class, () -> videoStorageService.uploadVideo(fileWithIOException));
    }

    @Test
    void getVideoUrl_Success() throws MalformedURLException {
        // Arrange
        String key = "videos/1234567890-abc123.mp4";
        URL expectedUrl = new URL("https://s3.amazonaws.com/test-bucket/videos/1234567890-abc123.mp4");

        when(amazonS3.getUrl(BUCKET_NAME, key)).thenReturn(expectedUrl);

        // Act
        String result = videoStorageService.getVideoUrl(key);

        // Assert
        assertNotNull(result);
        assertEquals(expectedUrl.toString(), result);
        verify(amazonS3, times(1)).getUrl(BUCKET_NAME, key);
    }

    @Test
    void getVideoUrl_DifferentKey_ReturnsCorrectUrl() throws MalformedURLException {
        // Arrange
        String key = "videos/9876543210-xyz789.avi";
        URL expectedUrl = new URL("https://s3.amazonaws.com/test-bucket/videos/9876543210-xyz789.avi");

        when(amazonS3.getUrl(BUCKET_NAME, key)).thenReturn(expectedUrl);

        // Act
        String result = videoStorageService.getVideoUrl(key);

        // Assert
        assertEquals(expectedUrl.toString(), result);
        verify(amazonS3, times(1)).getUrl(BUCKET_NAME, key);
    }
}


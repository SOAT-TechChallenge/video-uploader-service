package com.videoUploaderService.controller;

import com.videoUploaderService.service.TokenService;
import com.videoUploaderService.service.VideoQueueService;
import com.videoUploaderService.service.VideoStorageService;
import com.videoUploaderService.service.TokenService.UserInfo;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/videos")
@Validated
public class VideoUploadController {

    private final VideoStorageService videoStorageService;
    private final VideoQueueService videoQueueService;
    private final TokenService tokenService;

    public VideoUploadController(VideoStorageService videoStorageService, VideoQueueService videoQueueService,
            TokenService tokenService) {
        this.videoStorageService = videoStorageService;
        this.videoQueueService = videoQueueService;
        this.tokenService = tokenService;
    }

    @PostMapping
    public ResponseEntity<?> uploadVideo(
            @RequestHeader("Authorization") String tokenHeader,
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") @NotBlank String title,
            @RequestParam(value = "description", required = false) String description) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("Arquivo de vídeo é obrigatório");
        }

        try {
            UserInfo userInfo = tokenService.decodeToken(tokenHeader);
            String key = videoStorageService.uploadVideo(file);
            String url = videoStorageService.getVideoUrl(key);

            videoQueueService.sendVideoMessage(key, url, title, description, userInfo.username(), userInfo.email());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Upload realizado com sucesso");
            response.put("s3Key", key);
            response.put("s3Url", url);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao fazer upload do vídeo: " + e.getMessage());

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Token inválido ou expirado: " + e.getMessage());
        }
    }
}

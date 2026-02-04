package com.videoUploaderService.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class VideoQueueService {

    private final AmazonSQS amazonSQS;
    private final String queueUrl;
    private final ObjectMapper objectMapper;

    public VideoQueueService(AmazonSQS amazonSQS,
                             @Value("${aws.sqs.queueUrl}") String queueUrl,
                             ObjectMapper objectMapper) {
        this.amazonSQS = amazonSQS;
        this.queueUrl = queueUrl;
        this.objectMapper = objectMapper;
    }

    public void sendVideoMessage(String s3Key, String s3Url, String title, String description) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("s3Key", s3Key);
        payload.put("s3Url", s3Url);
        payload.put("title", title);
        payload.put("description", description);
        payload.put("uploadedAt", Instant.now().toString());

        try {
            String body = objectMapper.writeValueAsString(payload);

            SendMessageRequest request = new SendMessageRequest()
                    .withQueueUrl(queueUrl)
                    .withMessageBody(body);

            amazonSQS.sendMessage(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao serializar mensagem de vídeo", e);
        }
    }
}



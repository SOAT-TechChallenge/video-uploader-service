package com.videoUploaderService.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsConfig {

    private static final Logger logger = LoggerFactory.getLogger(AwsConfig.class);

    @Value("${aws.accessKeyId}")
    private String accessKeyId;

    @Value("${aws.secretKey}")
    private String secretKey;

    @Value("${aws.sessionToken:}")
    private String sessionToken;

    @Value("${aws.region}")
    private String region;


    @Value("${aws.s3.endpoint:}")
    private String s3Endpoint;

    @Value("${aws.sqs.endpoint:}")
    private String sqsEndpoint;

    @Bean
    public AmazonS3 amazonS3() {
        AWSCredentials credentials = createCredentials();

        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials));

        if (s3Endpoint != null && !s3Endpoint.isBlank()) {
            builder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(s3Endpoint, region)
            );
        } else {
            builder.withRegion(region);
        }

        return builder.build();
    }

    @Bean
    public AmazonSQS amazonSQS() {
        AWSCredentials credentials = createCredentials();

        AmazonSQSClientBuilder builder = AmazonSQSClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials));

        if (sqsEndpoint != null && !sqsEndpoint.isBlank()) {
            builder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(sqsEndpoint, region)
            );
        } else {
            builder.withRegion(region);
        }

        return builder.build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    private AWSCredentials createCredentials() {
        boolean hasSessionToken = sessionToken != null && !sessionToken.trim().isEmpty();

        if (hasSessionToken) {
            String cleanToken = sessionToken.trim();
            logger.info("Usando credenciais temporárias com session token (tamanho: {} caracteres)", cleanToken.length());
            return new BasicSessionCredentials(accessKeyId, secretKey, cleanToken);
        } else {
            logger.info("Usando credenciais permanentes (sem session token)");
            return new BasicAWSCredentials(accessKeyId, secretKey);
        }
    }
}



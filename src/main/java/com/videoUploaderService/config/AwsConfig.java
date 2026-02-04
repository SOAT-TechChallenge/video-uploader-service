package com.videoUploaderService.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
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

    @Value("${aws.accessKeyId:}")
    private String accessKeyId;

    @Value("${aws.secretKey:}")
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
        AWSCredentialsProvider credentialsProvider = createCredentialsProvider();

        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withCredentials(credentialsProvider);

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
        AWSCredentialsProvider credentialsProvider = createCredentialsProvider();

        AmazonSQSClientBuilder builder = AmazonSQSClientBuilder.standard()
                .withCredentials(credentialsProvider);

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

    /**
     * Cria o provider de credenciais AWS.
     * Se credenciais explícitas estiverem configuradas, usa elas.
     * Caso contrário, usa DefaultAWSCredentialsProviderChain (IAM roles, variáveis de ambiente, etc).
     */
    private AWSCredentialsProvider createCredentialsProvider() {
        // Verifica se há credenciais explícitas configuradas
        boolean hasExplicitCredentials = accessKeyId != null && !accessKeyId.trim().isEmpty()
                && secretKey != null && !secretKey.trim().isEmpty();

        if (hasExplicitCredentials) {
            // Usa credenciais explícitas (local ou quando necessário)
            boolean hasSessionToken = sessionToken != null && !sessionToken.trim().isEmpty();

            if (hasSessionToken) {
                String cleanToken = sessionToken.trim();
                logger.info("Usando credenciais temporárias explícitas com session token (tamanho: {} caracteres)", cleanToken.length());
                return new AWSStaticCredentialsProvider(
                        new BasicSessionCredentials(accessKeyId.trim(), secretKey.trim(), cleanToken)
                );
            } else {
                logger.info("Usando credenciais permanentes explícitas");
                return new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(accessKeyId.trim(), secretKey.trim())
                );
            }
        } else {
            // Usa DefaultAWSCredentialsProviderChain (IAM roles, variáveis de ambiente, etc)
            // Isso permite usar IAM roles quando rodando na AWS (EKS/ECS)
            logger.info("Credenciais explícitas não configuradas. Usando DefaultAWSCredentialsProviderChain (IAM roles, variáveis de ambiente, etc)");
            return DefaultAWSCredentialsProviderChain.getInstance();
        }
    }
}



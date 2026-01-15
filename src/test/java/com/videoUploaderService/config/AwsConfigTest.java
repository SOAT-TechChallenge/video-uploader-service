package com.videoUploaderService.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sqs.AmazonSQS;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class AwsConfigTest {

    private AwsConfig createBaseConfig() {
        AwsConfig config = new AwsConfig();
        ReflectionTestUtils.setField(config, "accessKeyId", "test-key");
        ReflectionTestUtils.setField(config, "secretKey", "test-secret");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        return config;
    }

    // =========================
    // AMAZON S3
    // =========================

    @Test
    void amazonS3_withoutEndpoint_andWithoutSessionToken() {
        AwsConfig config = createBaseConfig();
        ReflectionTestUtils.setField(config, "sessionToken", "");
        ReflectionTestUtils.setField(config, "s3Endpoint", "");

        AmazonS3 s3 = config.amazonS3();

        assertNotNull(s3);
    }

    @Test
    void amazonS3_withEndpoint() {
        AwsConfig config = createBaseConfig();
        ReflectionTestUtils.setField(config, "sessionToken", "");
        ReflectionTestUtils.setField(config, "s3Endpoint", "http://localhost:4566");

        AmazonS3 s3 = config.amazonS3();

        assertNotNull(s3);
    }

    @Test
    void amazonS3_withBlankEndpoint() {
        AwsConfig config = createBaseConfig();
        ReflectionTestUtils.setField(config, "sessionToken", "");
        ReflectionTestUtils.setField(config, "s3Endpoint", "   ");

        AmazonS3 s3 = config.amazonS3();

        assertNotNull(s3);
    }

    // =========================
    // AMAZON SQS
    // =========================

    @Test
    void amazonSQS_withoutEndpoint() {
        AwsConfig config = createBaseConfig();
        ReflectionTestUtils.setField(config, "sessionToken", "");
        ReflectionTestUtils.setField(config, "sqsEndpoint", "");

        AmazonSQS sqs = config.amazonSQS();

        assertNotNull(sqs);
    }

    @Test
    void amazonSQS_withEndpoint() {
        AwsConfig config = createBaseConfig();
        ReflectionTestUtils.setField(config, "sessionToken", "");
        ReflectionTestUtils.setField(config, "sqsEndpoint", "http://localhost:4566");

        AmazonSQS sqs = config.amazonSQS();

        assertNotNull(sqs);
    }

    // =========================
    // OBJECT MAPPER
    // =========================

    @Test
    void objectMapperBean_created() {
        AwsConfig config = new AwsConfig();

        ObjectMapper mapper = config.objectMapper();

        assertNotNull(mapper);
    }

    // =========================
    // CREDENTIALS
    // =========================

    @Test
    void createCredentials_withoutSessionToken_usesBasicCredentials() {
        AwsConfig config = createBaseConfig();
        ReflectionTestUtils.setField(config, "sessionToken", "");

        AWSCredentials credentials =
                (AWSCredentials) ReflectionTestUtils.invokeMethod(config, "createCredentials");

        assertNotNull(credentials);
        assertTrue(credentials instanceof BasicAWSCredentials);
    }

    @Test
    void createCredentials_withSessionToken_usesSessionCredentials() {
        AwsConfig config = createBaseConfig();
        ReflectionTestUtils.setField(config, "sessionToken", "  session-token-test  ");

        AWSCredentials credentials =
                (AWSCredentials) ReflectionTestUtils.invokeMethod(config, "createCredentials");

        assertNotNull(credentials);
        assertTrue(credentials instanceof BasicSessionCredentials);
    }
}

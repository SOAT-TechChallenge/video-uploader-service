package com.videoUploaderService.config;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sqs.AmazonSQS;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class AwsConfigTest {

    @Test
    void amazonS3Bean_Created_WithoutCustomEndpoint() {
        // Arrange
        AwsConfig config = new AwsConfig();
        ReflectionTestUtils.setField(config, "accessKeyId", "test-key");
        ReflectionTestUtils.setField(config, "secretKey", "test-secret");
        ReflectionTestUtils.setField(config, "sessionToken", "");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "s3Endpoint", "");
        ReflectionTestUtils.setField(config, "sqsEndpoint", "");

        // Act
        AmazonS3 s3 = config.amazonS3();

        // Assert
        assertNotNull(s3);
    }

    @Test
    void amazonSQSBean_Created_WithoutCustomEndpoint() {
        // Arrange
        AwsConfig config = new AwsConfig();
        ReflectionTestUtils.setField(config, "accessKeyId", "test-key");
        ReflectionTestUtils.setField(config, "secretKey", "test-secret");
        ReflectionTestUtils.setField(config, "sessionToken", "");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "s3Endpoint", "");
        ReflectionTestUtils.setField(config, "sqsEndpoint", "");

        // Act
        AmazonSQS sqs = config.amazonSQS();

        // Assert
        assertNotNull(sqs);
    }

    @Test
    void amazonS3Bean_Created_WithCustomEndpoint() {
        // Arrange
        AwsConfig config = new AwsConfig();
        ReflectionTestUtils.setField(config, "accessKeyId", "test-key");
        ReflectionTestUtils.setField(config, "secretKey", "test-secret");
        ReflectionTestUtils.setField(config, "sessionToken", "");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "s3Endpoint", "http://localhost:4566");
        ReflectionTestUtils.setField(config, "sqsEndpoint", "");

        // Act
        AmazonS3 s3 = config.amazonS3();

        // Assert
        assertNotNull(s3);
    }

    @Test
    void amazonSQSBean_Created_WithCustomEndpoint() {
        // Arrange
        AwsConfig config = new AwsConfig();
        ReflectionTestUtils.setField(config, "accessKeyId", "test-key");
        ReflectionTestUtils.setField(config, "secretKey", "test-secret");
        ReflectionTestUtils.setField(config, "sessionToken", "");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "s3Endpoint", "");
        ReflectionTestUtils.setField(config, "sqsEndpoint", "http://localhost:4566");

        // Act
        AmazonSQS sqs = config.amazonSQS();

        // Assert
        assertNotNull(sqs);
    }

    @Test
    void amazonS3Bean_Created_WithBlankEndpoint() {
        // Arrange
        AwsConfig config = new AwsConfig();
        ReflectionTestUtils.setField(config, "accessKeyId", "test-key");
        ReflectionTestUtils.setField(config, "secretKey", "test-secret");
        ReflectionTestUtils.setField(config, "sessionToken", "");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "s3Endpoint", "   ");
        ReflectionTestUtils.setField(config, "sqsEndpoint", "   ");

        // Act
        AmazonS3 s3 = config.amazonS3();

        // Assert
        assertNotNull(s3);
    }

    @Test
    void amazonSQSBean_Created_WithBlankEndpoint() {
        // Arrange
        AwsConfig config = new AwsConfig();
        ReflectionTestUtils.setField(config, "accessKeyId", "test-key");
        ReflectionTestUtils.setField(config, "secretKey", "test-secret");
        ReflectionTestUtils.setField(config, "sessionToken", "");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "s3Endpoint", "");
        ReflectionTestUtils.setField(config, "sqsEndpoint", "   ");

        // Act
        AmazonSQS sqs = config.amazonSQS();

        // Assert
        assertNotNull(sqs);
    }

    @Test
    void amazonS3Bean_Created_WithNullEndpoint() {
        // Arrange
        AwsConfig config = new AwsConfig();
        ReflectionTestUtils.setField(config, "accessKeyId", "test-key");
        ReflectionTestUtils.setField(config, "secretKey", "test-secret");
        ReflectionTestUtils.setField(config, "sessionToken", "");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "s3Endpoint", null);
        ReflectionTestUtils.setField(config, "sqsEndpoint", null);

        // Act
        AmazonS3 s3 = config.amazonS3();

        // Assert
        assertNotNull(s3);
    }

    @Test
    void amazonSQSBean_Created_WithNullEndpoint() {
        // Arrange
        AwsConfig config = new AwsConfig();
        ReflectionTestUtils.setField(config, "accessKeyId", "test-key");
        ReflectionTestUtils.setField(config, "secretKey", "test-secret");
        ReflectionTestUtils.setField(config, "sessionToken", "");
        ReflectionTestUtils.setField(config, "region", "us-east-1");
        ReflectionTestUtils.setField(config, "s3Endpoint", null);
        ReflectionTestUtils.setField(config, "sqsEndpoint", null);

        // Act
        AmazonSQS sqs = config.amazonSQS();

        // Assert
        assertNotNull(sqs);
    }
}


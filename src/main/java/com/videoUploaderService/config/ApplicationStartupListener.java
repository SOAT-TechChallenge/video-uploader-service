package com.videoUploaderService.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class ApplicationStartupListener implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartupListener.class);
    private final Environment environment;

    public ApplicationStartupListener(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(String... args) {
        int port = Integer.parseInt(environment.getProperty("server.port", "8080"));
        
        logger.info("==========================================");
        logger.info("🚀 Video Uploader Service iniciado com sucesso!");
        logger.info("==========================================");
        logger.info("📍 API disponível em:");
        logger.info("   • Local:   http://localhost:{}", port);
        logger.info("   • Network: http://{}:{}", getHostAddress(), port);
        logger.info("");
        logger.info("📋 Endpoints disponíveis:");
        logger.info("   • POST /videos - Upload de vídeo");
        logger.info("");
        logger.info("   Exemplo de uso:");
        logger.info("   curl -X POST http://localhost:{}/videos \\", port);
        logger.info("        -F \"file=@video.mp4\" \\");
        logger.info("        -F \"title=Meu Video\" \\");
        logger.info("        -F \"description=Descrição opcional\"");
        logger.info("==========================================");
    }

    private String getHostAddress() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }
}

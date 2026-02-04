package com.videoUploaderService.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro para validar o token do API Gateway.
 * 
 * Este filtro valida o header x-apigateway-token que deve ser adicionado
 * pelo API Gateway antes de encaminhar a requisição para o ALB.
 * 
 * O ALB também valida esse token, mas esta é uma camada extra de segurança.
 */
@Component
@Order(1)
public class GatewayTokenFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(GatewayTokenFilter.class);
    private static final String GATEWAY_TOKEN_HEADER = "x-apigateway-token";
    private static final String HEALTH_CHECK_PATH = "/actuator/health";

    @Value("${gateway.token:tech-challenge-hackathon}")
    private String expectedToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // Permite health check sem validação de token
        if (requestPath != null && requestPath.contains(HEALTH_CHECK_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader(GATEWAY_TOKEN_HEADER);

        if (token == null || !token.equals(expectedToken)) {
            logger.warn("Requisição rejeitada: token do gateway inválido ou ausente. IP: {}, Path: {}", 
                       request.getRemoteAddr(), requestPath);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Acesso negado. Token do gateway inválido ou ausente.\"}");
            return;
        }

        logger.debug("Token do gateway validado com sucesso para: {}", requestPath);
        filterChain.doFilter(request, response);
    }
}

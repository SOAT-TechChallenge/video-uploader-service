package com.videoUploaderService.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GatewayTokenFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private GatewayTokenFilter filter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(filter, "expectedToken", "tech-challenge-hackathon");
    }

    @Test
    void doFilterInternal_ValidToken_AllowsRequest() throws Exception {
        // Arrange
        when(request.getHeader("x-apigateway-token")).thenReturn("tech-challenge-hackathon");
        when(request.getRequestURI()).thenReturn("/videos");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, times(1)).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void doFilterInternal_InvalidToken_RejectsRequest() throws Exception {
        // Arrange
        when(request.getHeader("x-apigateway-token")).thenReturn("invalid-token");
        when(request.getRequestURI()).thenReturn("/videos");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, never()).doFilter(any(), any());
        verify(response, times(1)).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response, times(1)).setContentType("application/json");
    }

    @Test
    void doFilterInternal_MissingToken_RejectsRequest() throws Exception {
        // Arrange
        when(request.getHeader("x-apigateway-token")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/videos");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, never()).doFilter(any(), any());
        verify(response, times(1)).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void doFilterInternal_HealthCheck_AllowsWithoutToken() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/actuator/health");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, times(1)).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }
}

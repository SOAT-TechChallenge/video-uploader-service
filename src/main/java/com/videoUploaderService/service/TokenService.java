package com.videoUploaderService.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenService {

    @Value("${api.security.token.secret}") // Pegando do application.properties
    private String secret;

    public UserInfo decodeToken(String tokenHeader) {
        String token = tokenHeader.replace("Bearer ", "");
        
        DecodedJWT jwt = JWT.require(Algorithm.HMAC256(secret))
                .build()
                .verify(token);

        return new UserInfo(jwt.getSubject(), jwt.getClaim("email").asString());
    }

    public record UserInfo(String username, String email) {}
}
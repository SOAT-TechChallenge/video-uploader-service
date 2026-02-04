# Fluxo de Requisição: Postman → API Gateway → ALB → ECS

Este documento explica como funciona o fluxo completo de uma requisição do Postman até a aplicação Spring Boot.

## 🔄 Fluxo Completo

```
┌─────────┐      ┌──────────────┐      ┌─────┐      ┌─────┐      ┌─────────────┐
│ Postman │ ───> │ API Gateway  │ ───> │ ALB │ ───> │ ECS │ ───> │ Spring Boot │
└─────────┘      └──────────────┘      └─────┘      └─────┘      └─────────────┘
   (JWT)         (Valida JWT +        (Valida      (Processa)   (Valida token
                 adiciona header)     header)                    + processa)
```

## 📋 Passo a Passo

### 1. **Postman → API Gateway**

**Requisição do Postman:**
```http
POST https://seu-api-gateway.execute-api.us-east-1.amazonaws.com/videos
Authorization: Bearer <JWT_TOKEN>
Content-Type: multipart/form-data

file: <arquivo-video.mp4>
title: Meu Vídeo
description: Descrição opcional
```

**O que acontece:**
- API Gateway recebe a requisição com token JWT no header `Authorization`
- API Gateway valida o JWT (verifica assinatura, expiração, etc.)
- Se válido, API Gateway adiciona o header `x-apigateway-token: tech-challenge-hackathon`
- API Gateway encaminha a requisição para o ALB

### 2. **API Gateway → ALB (Application Load Balancer)**

**Requisição que chega no ALB:**
```http
POST http://<alb-dns-name>/videos
x-apigateway-token: tech-challenge-hackathon
Content-Type: multipart/form-data

file: <arquivo-video.mp4>
title: Meu Vídeo
description: Descrição opcional
```

**O que acontece:**
- ALB verifica se o header `x-apigateway-token` existe e tem o valor `tech-challenge-hackathon`
- Se válido, ALB encaminha para o Target Group (ECS)
- Se inválido ou ausente, ALB retorna `403 Forbidden` com mensagem: "Acesso Direto Negado. Use o API Gateway."

**Configuração no Terraform (`main.tf`):**
```hcl
resource "aws_lb_listener_rule" "allow_gateway" {
  listener_arn = aws_lb_listener.http.arn
  priority     = 100

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.uploader_tg.arn
  }

  condition {
    http_header {
      http_header_name = "x-apigateway-token"
      values           = ["tech-challenge-hackathon"]
    }
  }
}
```

### 3. **ALB → ECS (Spring Boot)**

**Requisição que chega na aplicação:**
```http
POST http://localhost:8080/videos
x-apigateway-token: tech-challenge-hackathon
Content-Type: multipart/form-data

file: <arquivo-video.mp4>
title: Meu Vídeo
description: Descrição opcional
```

**O que acontece:**
- `GatewayTokenFilter` valida o header `x-apigateway-token` (camada extra de segurança)
- Se válido, a requisição é processada pelo `VideoUploadController`
- Se inválido, retorna `403 Forbidden`

**Código do Filtro:**
```java
@Component
@Order(1)
public class GatewayTokenFilter extends OncePerRequestFilter {
    // Valida x-apigateway-token antes de processar a requisição
}
```

### 4. **Processamento na Aplicação**

1. **Validação do arquivo:**
   - Verifica se o arquivo não é nulo ou vazio
   - Retorna `400 Bad Request` se inválido

2. **Upload para S3:**
   - Gera uma chave única: `videos/{timestamp}-{uuid}.mp4`
   - Faz upload do arquivo para o bucket S3
   - Usa IAM roles ou credenciais explícitas (conforme configurado)

3. **Envio para SQS:**
   - Envia mensagem para a fila SQS com:
     - `s3Key`: Chave do arquivo no S3
     - `s3Url`: URL do arquivo no S3
     - `title`: Título do vídeo
     - `description`: Descrição (opcional)

4. **Resposta:**
   ```json
   {
     "message": "Upload realizado com sucesso",
     "s3Key": "videos/1234567890-abc123.mp4",
     "s3Url": "https://s3.amazonaws.com/bucket/videos/1234567890-abc123.mp4"
   }
   ```

## 🔒 Camadas de Segurança

1. **API Gateway:** Valida JWT e adiciona token interno
2. **ALB:** Valida header `x-apigateway-token` (bloqueia acesso direto)
3. **Spring Boot Filter:** Valida header `x-apigateway-token` (camada extra)

## 📝 Configuração

### Variáveis de Ambiente no ECS

No `main.tf`, a variável `GATEWAY_TOKEN` pode ser configurada:

```hcl
environment = [
  # ... outras variáveis
  { name = "GATEWAY_TOKEN", value = "tech-challenge-hackathon" }
]
```

### Configuração no `application.properties`

```properties
gateway.token=${GATEWAY_TOKEN:tech-challenge-hackathon}
```

## 🧪 Testando com Postman

### 1. Obter Token JWT

Primeiro, você precisa obter um token JWT válido (depende da sua configuração de autenticação no API Gateway).

### 2. Configurar Requisição no Postman

**URL:**
```
https://seu-api-gateway.execute-api.us-east-1.amazonaws.com/videos
```

**Headers:**
```
Authorization: Bearer <SEU_JWT_TOKEN>
```

**Body (form-data):**
- `file`: [Selecione o arquivo de vídeo]
- `title`: Meu Vídeo
- `description`: Descrição opcional

### 3. Fluxo Esperado

1. ✅ API Gateway valida JWT
2. ✅ API Gateway adiciona `x-apigateway-token`
3. ✅ ALB valida o token
4. ✅ Spring Boot valida o token
5. ✅ Upload para S3
6. ✅ Mensagem enviada para SQS
7. ✅ Resposta 201 Created com informações do upload

## ⚠️ Troubleshooting

### Erro 403 no ALB

**Causa:** Requisição não passou pelo API Gateway ou token inválido.

**Solução:** Certifique-se de que:
- A requisição está indo para o API Gateway (não diretamente para o ALB)
- O API Gateway está configurado para adicionar o header `x-apigateway-token`

### Erro 403 na Aplicação

**Causa:** Header `x-apigateway-token` ausente ou inválido.

**Solução:** Verifique:
- Se o API Gateway está adicionando o header corretamente
- Se o valor do token corresponde ao configurado (`tech-challenge-hackathon`)

### Erro 401 no API Gateway

**Causa:** Token JWT inválido, expirado ou ausente.

**Solução:** Obtenha um novo token JWT válido.

## 📚 Referências

- [AWS API Gateway](https://docs.aws.amazon.com/apigateway/)
- [AWS Application Load Balancer](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/)
- [Spring Boot Filters](https://docs.spring.io/spring-boot/docs/current/reference/html/web.html#web.servlet.filters)

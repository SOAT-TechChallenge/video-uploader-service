# Video Uploader Service

Serviço Spring Boot para upload de vídeos para AWS S3 com integração com fila SQS.

## 🚀 Funcionalidades

- Upload de vídeos para AWS S3
- Envio de mensagem para fila SQS com informações do vídeo
- Suporte a credenciais temporárias da AWS (session token)
- Configuração via variáveis de ambiente ou arquivo `.env`
- Testes com 100% de cobertura de código

## 📋 Pré-requisitos

- Java 17+
- Maven 3.6+
- Conta AWS com acesso a S3 e SQS
- Bucket S3 configurado
- Fila SQS configurada

## ⚙️ Configuração

### 1. Configurar variáveis de ambiente

Copie o arquivo `.env.example` para `.env`:

```bash
cp .env.example .env
```

Edite o arquivo `.env` com suas credenciais AWS:

```env
AWS_ACCESS_KEY_ID=your-access-key-id
AWS_SECRET_ACCESS_KEY=your-secret-access-key
AWS_SESSION_TOKEN=your-session-token  # Opcional, apenas para credenciais temporárias
AWS_REGION=us-east-1
AWS_S3_BUCKET=your-bucket-name
AWS_SQS_QUEUE_URL=https://sqs.us-east-1.amazonaws.com/YOUR_ACCOUNT_ID/your-queue-name
```

### 2. Variáveis de ambiente obrigatórias

- `AWS_ACCESS_KEY_ID` - Access Key ID da AWS
- `AWS_SECRET_ACCESS_KEY` - Secret Access Key da AWS
- `AWS_S3_BUCKET` - Nome do bucket S3
- `AWS_SQS_QUEUE_URL` - URL completa da fila SQS

### 3. Variáveis opcionais

- `AWS_SESSION_TOKEN` - Session token (necessário apenas para credenciais temporárias)
- `AWS_REGION` - Região AWS (padrão: `us-east-1`)
- `AWS_S3_ENDPOINT` - Endpoint customizado para S3 (ex: LocalStack)
- `AWS_SQS_ENDPOINT` - Endpoint customizado para SQS (ex: LocalStack)

## 🏃 Executando a aplicação

### Desenvolvimento local

```bash
mvn spring-boot:run
```

### Build e execução

```bash
mvn clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

## 📡 Endpoints

### POST /videos

Upload de vídeo para S3 e envio de mensagem para fila SQS.

**Parâmetros:**
- `file` (multipart/form-data) - Arquivo de vídeo (obrigatório)
- `title` (string) - Título do vídeo (obrigatório)
- `description` (string) - Descrição do vídeo (opcional)

**Exemplo de uso:**

```bash
curl -X POST http://localhost:8080/videos \
  -F "file=@video.mp4" \
  -F "title=Meu Video" \
  -F "description=Descrição do vídeo"
```

**Resposta de sucesso (201 Created):**

```json
{
  "message": "Upload realizado com sucesso",
  "s3Key": "videos/1234567890-abc123.mp4",
  "s3Url": "https://s3.amazonaws.com/bucket/videos/1234567890-abc123.mp4"
}
```

## 🐳 Docker

### Build da imagem

```bash
docker build -t video-uploader-service .
```

### Executar container

```bash
docker run -p 8080:8080 \
  --env-file .env \
  video-uploader-service
```

## ☸️ Kubernetes

Aplicar os manifests:

```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret-aws-credentials.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

**Importante:** Atualize o arquivo `k8s/secret-aws-credentials.yaml` com suas credenciais reais antes de aplicar.

## 🧪 Testes

Executar todos os testes:

```bash
mvn test
```

Executar testes com relatório de cobertura:

```bash
mvn clean test jacoco:report
```

O relatório de cobertura estará disponível em: `target/site/jacoco/index.html`

## 📦 Estrutura do projeto

```
video-uploader-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/videoUploaderService/
│   │   │       ├── config/          # Configurações (AWS, Env)
│   │   │       ├── controller/       # Controllers REST
│   │   │       ├── service/          # Serviços de negócio
│   │   │       └── Application.java # Classe principal
│   │   └── resources/
│   │       └── application.properties
│   └── test/                         # Testes unitários
├── k8s/                              # Manifests Kubernetes
├── Dockerfile
├── docker-compose.yml
├── .env.example                      # Template de variáveis de ambiente
└── pom.xml
```

## 🔒 Segurança

- **NUNCA** commite o arquivo `.env` no repositório
- Use variáveis de ambiente em produção
- Para Kubernetes, use Secrets para credenciais sensíveis
- Rotacione suas credenciais AWS regularmente

## 📝 Notas

- O projeto usa AWS SDK v1 (em modo de manutenção até dez/2025)
- O aviso de deprecação do AWS SDK é suprimido automaticamente
- O serviço suporta uploads de até 500MB por padrão (configurável)

## 🤝 Contribuindo

1. Faça fork do projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## 📄 Licença

Este projeto está sob a licença MIT.



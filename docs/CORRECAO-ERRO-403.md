# Correção do Erro 403

## ✅ O que foi corrigido

### 1. **Remoção de Credenciais Explícitas no Terraform**

As credenciais explícitas (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`) foram **removidas** do `main.tf`. 

**Antes:**
```hcl
environment = [
  # ...
  { name = "AWS_ACCESS_KEY_ID", value = var.aws_access_key_id },
  { name = "AWS_SECRET_ACCESS_KEY", value = var.aws_secret_access_key },
  { name = "AWS_SESSION_TOKEN", value = var.aws_session_token },
]
```

**Depois:**
```hcl
environment = [
  # ...
  # Credenciais explícitas removidas - a aplicação usará IAM roles (LabRole)
  { name = "GATEWAY_TOKEN", value = "tech-challenge-hackathon" },
]
```

### 2. **Aplicação Usa IAM Roles Automaticamente**

A aplicação Spring Boot foi configurada para:
- **Se não houver credenciais explícitas** → Usa `DefaultAWSCredentialsProviderChain` (IAM roles)
- **Se houver credenciais explícitas** → Usa as credenciais fornecidas

Como removemos as credenciais explícitas, a aplicação vai usar automaticamente o **LabRole** configurado no ECS task definition.

### 3. **Validação do Token do Gateway**

Adicionada a variável `GATEWAY_TOKEN` para garantir que o filtro valide corretamente o token do API Gateway.

## 🔍 O que verificar

### 1. **Permissões do LabRole**

O `LabRole` (usado como `task_role_arn` no ECS) precisa ter as seguintes permissões:

**Para S3:**
```json
{
  "Effect": "Allow",
  "Action": [
    "s3:PutObject",
    "s3:GetObject",
    "s3:DeleteObject"
  ],
  "Resource": "arn:aws:s3:::video-storage-*/*"
}
```

**Para SQS:**
```json
{
  "Effect": "Allow",
  "Action": [
    "sqs:SendMessage",
    "sqs:GetQueueAttributes"
  ],
  "Resource": "arn:aws:sqs:us-east-1:*:video-processing-queue"
}
```

**Para CloudWatch Logs (já deve ter):**
```json
{
  "Effect": "Allow",
  "Action": [
    "logs:CreateLogGroup",
    "logs:CreateLogStream",
    "logs:PutLogEvents"
  ],
  "Resource": "*"
}
```

### 2. **Verificar se o LabRole tem as permissões**

Execute no AWS CLI:
```bash
aws iam get-role-policy --role-name LabRole --policy-name <nome-da-policy>
```

Ou verifique no console AWS:
1. IAM → Roles → LabRole
2. Verifique as policies anexadas
3. Confirme que tem permissões para S3 e SQS

### 3. **Aplicar as mudanças do Terraform**

```bash
cd infra
terraform plan
terraform apply
```

Isso vai:
- Remover as variáveis de ambiente de credenciais do ECS task
- Adicionar a variável `GATEWAY_TOKEN`
- Atualizar o ECS service (pode levar alguns minutos)

### 4. **Verificar os logs da aplicação**

Após o deploy, verifique os logs do ECS:

```bash
aws logs tail /ecs/video-uploader-service --follow
```

Você deve ver:
```
INFO com.videoUploaderService.config.AwsConfig -- Credenciais explícitas não configuradas. Usando DefaultAWSCredentialsProviderChain (IAM roles, variáveis de ambiente, etc)
```

**Se ainda aparecer:**
```
INFO com.videoUploaderService.config.AwsConfig -- Usando credenciais permanentes explícitas
```

Significa que ainda há credenciais explícitas configuradas (verifique variáveis de ambiente do container).

## 🎯 Resultado Esperado

Após essas mudanças:

1. ✅ **Aplicação usa IAM roles** (LabRole) ao invés de credenciais explícitas
2. ✅ **Erro 403 para de ocorrer** (se o LabRole tiver as permissões corretas)
3. ✅ **Upload para S3 funciona** usando permissões do IAM role
4. ✅ **Envio para SQS funciona** usando permissões do IAM role
5. ✅ **Token do gateway validado** corretamente

## ⚠️ Se o erro 403 continuar

### Possíveis causas:

1. **LabRole não tem permissões suficientes**
   - Solução: Adicione as policies necessárias ao LabRole

2. **ECS service ainda não foi atualizado**
   - Solução: Aguarde alguns minutos após `terraform apply` ou force uma nova deployment:
   ```bash
   aws ecs update-service --cluster video-uploader-cluster --service video-uploader-service --force-new-deployment
   ```

3. **Credenciais explícitas ainda configuradas em outro lugar**
   - Solução: Verifique se não há variáveis de ambiente configuradas manualmente no ECS service

4. **Token do gateway incorreto**
   - Solução: Verifique se o API Gateway está adicionando o header `x-apigateway-token: tech-challenge-hackathon`

## 📝 Resumo

- ✅ Credenciais explícitas removidas do Terraform
- ✅ Aplicação configurada para usar IAM roles
- ✅ Token do gateway configurado
- ⚠️ **Verificar se LabRole tem permissões para S3 e SQS**

O erro 403 **deve parar** após aplicar essas mudanças e garantir que o LabRole tem as permissões corretas.

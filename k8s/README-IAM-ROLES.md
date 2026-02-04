# Configuração de IAM Roles para Kubernetes (IRSA)

Este guia explica como configurar IAM Roles para Service Accounts (IRSA) no EKS para que a aplicação use IAM roles ao invés de credenciais explícitas.

## Por que usar IAM Roles?

- **Segurança**: Não precisa armazenar credenciais em Secrets
- **Melhor prática AWS**: Usa o mecanismo nativo de autenticação
- **Rotação automática**: As credenciais são gerenciadas automaticamente pela AWS
- **Auditoria**: Melhor rastreabilidade de ações

## Pré-requisitos

1. Cluster EKS configurado
2. `kubectl` configurado
3. `aws` CLI configurado
4. Permissões para criar IAM roles e policies

## Passo 1: Criar IAM Role

1. Crie uma IAM Policy com as permissões necessárias:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::seu-bucket/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "sqs:SendMessage",
        "sqs:GetQueueAttributes"
      ],
      "Resource": "arn:aws:sqs:us-east-1:ACCOUNT_ID:sua-fila"
    }
  ]
}
```

2. Crie a IAM Role:

```bash
# Substitua ACCOUNT_ID, CLUSTER_NAME e NAMESPACE pelos seus valores
ACCOUNT_ID="seu-account-id"
CLUSTER_NAME="seu-cluster-eks"
NAMESPACE="default"

# Obter OIDC Provider URL do cluster
OIDC_PROVIDER=$(aws eks describe-cluster --name $CLUSTER_NAME --query "cluster.identity.oidc.issuer" --output text | sed -e "s/^https:\/\///")

# Criar trust policy
cat > trust-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::${ACCOUNT_ID}:oidc-provider/${OIDC_PROVIDER}"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "${OIDC_PROVIDER}:sub": "system:serviceaccount:${NAMESPACE}:video-uploader-service-account",
          "${OIDC_PROVIDER}:aud": "sts.amazonaws.com"
        }
      }
    }
  ]
}
EOF

# Criar a role
aws iam create-role \
  --role-name video-uploader-role \
  --assume-role-policy-document file://trust-policy.json

# Anexar a policy (crie a policy primeiro com as permissões acima)
aws iam attach-role-policy \
  --role-name video-uploader-role \
  --policy-arn arn:aws:iam::${ACCOUNT_ID}:policy/video-uploader-policy
```

## Passo 2: Configurar Service Account no Kubernetes

1. Atualize o arquivo `service-account.yaml` com o ARN da role criada:

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: video-uploader-service-account
  namespace: default
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::ACCOUNT_ID:role/video-uploader-role
```

2. Aplique o Service Account:

```bash
kubectl apply -f k8s/service-account.yaml
```

## Passo 3: Atualizar Deployment

1. No arquivo `deployment.yaml`, descomente e configure o `serviceAccountName`:

```yaml
spec:
  serviceAccountName: video-uploader-service-account
  containers:
    ...
```

2. **Remova ou torne opcionais** as variáveis de ambiente de credenciais:

```yaml
# Remova estas linhas ou torne-as opcionais:
# - name: AWS_ACCESS_KEY_ID
#   valueFrom:
#     secretKeyRef:
#       name: aws-credentials
#       key: accessKeyId
```

3. Aplique o deployment:

```bash
kubectl apply -f k8s/deployment.yaml
```

## Passo 4: Verificar

1. Verifique se o pod está usando o service account:

```bash
kubectl describe pod <pod-name> | grep Service Account
```

2. Verifique os logs da aplicação:

```bash
kubectl logs <pod-name>
```

Você deve ver a mensagem:
```
Credenciais explícitas não configuradas. Usando DefaultAWSCredentialsProviderChain (IAM roles, variáveis de ambiente, etc)
```

## Troubleshooting

### Erro 403 ao acessar S3/SQS

1. Verifique se a IAM role tem as permissões corretas
2. Verifique se o Service Account está anotado corretamente
3. Verifique se o OIDC provider está configurado no cluster EKS

### Verificar credenciais usadas

Execute dentro do pod:

```bash
kubectl exec -it <pod-name> -- env | grep AWS
```

Não deve haver `AWS_ACCESS_KEY_ID` ou `AWS_SECRET_ACCESS_KEY` se estiver usando IAM roles.

## Referências

- [IAM Roles for Service Accounts](https://docs.aws.amazon.com/eks/latest/userguide/iam-roles-for-service-accounts.html)
- [AWS SDK Default Credential Provider Chain](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html)

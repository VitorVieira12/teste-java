# Digital Bank API

API REST simplificada para simulação de um banco digital, desenvolvida como parte do processo seletivo da Compass UOL.

Repositório: https://github.com/VitorVieira12/teste-java

## Quick Start

1. **Abra o Docker Desktop** e aguarde ficar pronto.
2. Suba o PostgreSQL: `docker compose up -d`
3. Confirme o banco: `docker exec digitalbank-postgres pg_isready -U digitalbank -d digitalbank`
4. Rode a aplicação: `mvnw.cmd spring-boot:run` (Windows) ou `./mvnw spring-boot:run` (Linux/macOS)
5. Acesse o Swagger: http://localhost:8080/swagger-ui.html

Credenciais de teste: `alice` / `senha123`

## Pré-requisitos

| Ferramenta | Versão | Por quê |
|------------|--------|---------|
| Java | 21+ | LTS moderna; alinhada ao Spring Boot 3.4 |
| Docker Desktop | recente | Sobe PostgreSQL de forma reproduzível, sem instalar banco localmente |
| Maven | opcional | O projeto inclui Maven Wrapper (`mvnw` / `mvnw.cmd`) |

## Como rodar

### 1. Subir o banco de dados

```bash
docker compose up -d
```

O PostgreSQL roda na porta **5433** do host (não 5432) para evitar conflito com instalações locais de PostgreSQL.

Verifique se o container está saudável:

```bash
docker ps
docker exec digitalbank-postgres pg_isready -U digitalbank -d digitalbank
```

Resposta esperada: `/var/run/postgresql:5432 - accepting connections`

### 2. Executar a aplicação

**Windows (PowerShell ou CMD):**

```bash
mvnw.cmd spring-boot:run
```

**Linux / macOS:**

```bash
./mvnw spring-boot:run
```

**IntelliJ / IDE:** execute a classe `com.compass.digitalbank.DigitalBankApplication`. Certifique-se de que o Docker com PostgreSQL já está rodando.

A API ficará disponível em http://localhost:8080.

### 3. Confirmar que está funcionando

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

No Swagger, teste `POST /api/v1/auth/login` com `alice` / `senha123`. Se retornar um `token`, a API está operacional.

## Testar via Swagger (passo a passo)

1. Abra http://localhost:8080/swagger-ui.html
2. Expanda **Auth** → `POST /api/v1/auth/login`
3. Clique em **Try it out**, envie:

```json
{
  "username": "alice",
  "password": "senha123"
}
```

4. Copie o valor de `token` da resposta
5. Clique no botão **Authorize** (cadeado no topo)
6. Cole: `Bearer SEU_TOKEN_AQUI` e confirme
7. Teste os endpoints protegidos:
   - `GET /api/v1/accounts/me` — saldo e dados da conta
   - `POST /api/v1/transfers` — transferência
   - `GET /api/v1/accounts/{id}/movements` — extrato
   - `GET /api/v1/accounts/me/notifications` — notificações

## Usuários de teste (seed)

| Username | Password | Conta (ID) | Saldo inicial |
|----------|----------|------------|---------------|
| alice | senha123 | 1 | 1000.00 |
| bruno | senha123 | 2 | 500.00 |
| carla | senha123 | 3 | 250.00 |

## Exemplos de uso (PowerShell)

Fluxo completo: login → consultar conta → transferir → ver movimentações e notificações.

```powershell
# 1. Login
$login = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" `
  -Method POST -ContentType "application/json" `
  -Body '{"username":"alice","password":"senha123"}'

$login
# Exemplo de resposta: { "token": "eyJhbGciOiJIUzI1NiJ9...", "tokenType": "Bearer", "expiresInMs": 86400000 }

$headers = @{ Authorization = "Bearer $($login.token)" }

# 2. Consultar minha conta
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/accounts/me" -Headers $headers
# Exemplo: { "id": 1, "name": "Alice Silva", "balance": 1000.00, ... }

# 3. Transferir R$ 50 da Alice (1) para o Bruno (2)
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/transfers" `
  -Method POST -ContentType "application/json" -Headers $headers `
  -Body '{"fromAccountId":1,"toAccountId":2,"amount":50.00}'
# Exemplo: { "id": 1, "fromAccountId": 1, "toAccountId": 2, "amount": 50.00, "status": "COMPLETED", ... }

# 4. Ver movimentações
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/accounts/1/movements?page=0&size=10" -Headers $headers

# 5. Ver notificações
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/accounts/me/notifications?page=0&size=10" -Headers $headers
```

### Registrar nova conta (endpoint público)

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/accounts" `
  -Method POST -ContentType "application/json" `
  -Body '{"name":"Daniel Lima","username":"daniel","password":"senha123","initialBalance":200.00}'
```

### Exemplos com curl (Linux / macOS / Git Bash)

```bash
# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"senha123"}'

# Consultar conta (substitua <token>)
curl http://localhost:8080/api/v1/accounts/me \
  -H "Authorization: Bearer <token>"

# Transferência
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"fromAccountId":1,"toAccountId":2,"amount":50.00}'
```

## Endpoints

| Método | Endpoint | Auth | Descrição |
|--------|----------|------|-----------|
| POST | `/api/v1/auth/login` | Público | Login e emissão de JWT |
| POST | `/api/v1/accounts` | Público | Registro (conta + usuário) |
| GET | `/api/v1/accounts/me` | JWT | Conta autenticada |
| GET | `/api/v1/accounts/{id}` | JWT | Consultar conta (somente a própria) |
| POST | `/api/v1/transfers` | JWT | Transferir da própria conta |
| GET | `/api/v1/accounts/{id}/movements` | JWT | Extrato (somente da própria conta) |
| GET | `/api/v1/accounts/me/notifications` | JWT | Notificações da conta autenticada |

## Troubleshooting

| Problema | Causa provável | Solução |
|----------|----------------|---------|
| `Connection to localhost:5433 refused` | Docker parado ou container não iniciado | Abra o Docker Desktop → `docker compose up -d` |
| `Port 8080 was already in use` | Outra app/container na mesma porta | `netstat -ano \| findstr ":8080"` (Windows) e pare o processo, ou `docker ps` e pare o container conflitante |
| `curl` com `\` não funciona no PowerShell | Sintaxe Unix | Use os exemplos PowerShell acima ou Git Bash |
| Login retorna 401 | Credenciais erradas | Use `alice` / `senha123` ou usuários da tabela seed |
| Transferência retorna 403 | Tentativa de transferir de conta alheia | `fromAccountId` deve ser a conta do token (ex.: Alice = 1) |
| Transferência retorna 409 | Saldo insuficiente | Reduza o valor ou consulte saldo em `/accounts/me` |

## Por que essas escolhas?

### PostgreSQL + Docker Compose

- **Por quê Docker:** o avaliador sobe o ambiente com um comando, sem instalar PostgreSQL manualmente.
- **Por quê PostgreSQL:** banco relacional real, alinhado ao cenário de produção; suporta locks transacionais necessários para concorrência.
- **Por quê porta 5433:** evita conflito com PostgreSQL local comum na 5432.

### Maven Wrapper

- **Por quê:** elimina dependência de Maven instalado globalmente; `./mvnw` ou `mvnw.cmd` baixa a versão correta automaticamente.

### JWT + ownership por conta

- **Por quê JWT:** API REST stateless — sem sessão em servidor, ideal para integração via Bearer token.
- **Por quê ownership:** cada usuário só acessa a própria conta; transferências só podem sair da conta autenticada. Simula controle de acesso real em banco digital.
- **Por quê cadastro público:** quem ainda não tem credencial precisa se registrar antes de obter token.

### Consistência e concorrência

Transferências financeiras exigem atomicidade e proteção contra acesso simultâneo:

1. **`@Transactional`** — débito, crédito, transferência e movimentações ocorrem juntos ou falham juntos (sem saldo inconsistente).
2. **Lock pessimista (`PESSIMISTIC_WRITE`)** — bloqueia as contas durante a operação, evitando condição de corrida (dois débitos simultâneos no mesmo saldo).
3. **Ordenação de locks (menor ID primeiro)** — previne deadlock quando Alice→Bruno e Bruno→Alice ocorrem ao mesmo tempo.
4. **`@Version` + retry** — camada extra contra conflitos de concorrência; se duas threads alterarem a mesma conta, uma retenta até 3 vezes.

### Notificações assíncronas (AFTER_COMMIT)

- **Por quê async:** não bloqueia a resposta HTTP da transferência enquanto envia notificação.
- **Por quê AFTER_COMMIT:** só notifica transferências que de fato foram persistidas; se houver rollback (ex.: saldo insuficiente), o cliente de sucesso não recebe notificação falsa.
- **Por quê log + `notification_logs`:** simula envio real (SMS/e-mail/push) de forma auditável.

### `ddl-auto: update`

- **Por quê:** simplifica setup do teste — o schema é criado/atualizado automaticamente. Em produção, usaria Flyway/Liquibase com migrations versionadas.

### Swagger (springdoc-openapi)

- **Por quê:** documentação interativa exigida pelo teste; permite testar endpoints sem Postman.

## Segurança

### JWT stateless

- Autenticação via Bearer token no header `Authorization`
- Token contém `username` e `accountId`
- Sessão stateless (sem cookies)

### Ownership por conta

Cada usuário está vinculado a uma conta (`User` 1:1 `Account`):

- Só é possível consultar a própria conta
- Transferências só podem originar da conta do token
- Movimentações só podem ser consultadas da própria conta

### Cadastro público com limites

`POST /accounts` permanece público para registro, mas:

- Exige `username` e `password`
- Saldo inicial limitado a `1000.00` (configurável em `app.account.max-initial-balance`)

### Configuração JWT

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:...}   # Base64, mínimo 256 bits
    expiration-ms: 86400000     # 24 horas
```

## Decisões de arquitetura

### Camadas

- **Controller** — exposição REST e validação de entrada
- **Service** — regras de negócio
- **Repository** — persistência com Spring Data JPA
- **Domain** — entidades e enums
- **Security** — JWT, filtros e ownership

### Movimentações

Cada transferência gera dois registros:

- **DEBIT** na conta de origem
- **CREDIT** na conta de destino

### Tratamento de erros

Respostas padronizadas via `@RestControllerAdvice`:

| HTTP | Situação |
|------|----------|
| 401 | Credenciais inválidas ou token ausente |
| 403 | Acesso negado (conta de outro usuário) |
| 404 | Conta não encontrada |
| 400 | Transferência inválida ou erro de validação |
| 409 | Saldo insuficiente ou username duplicado |

## Estrutura do projeto

```
src/main/java/com/compass/digitalbank/
├── config/
├── controller/
├── domain/
│   ├── entity/
│   └── enums/
├── dto/
├── exception/
├── notification/
├── repository/
├── security/
└── service/
```

## Testes

```bash
# Windows
mvnw.cmd test

# Linux / macOS
./mvnw test
```

Cobertura: autenticação JWT, registro, ownership, regras de transferência, notificações e teste de integração de concorrência com Testcontainers.

## CI

O pipeline GitHub Actions (`.github/workflows/ci.yml`) executa `./mvnw test` em cada push/PR para `main`/`master`.

## Stack

- Java 21
- Spring Boot 3.4
- Spring Security + JWT (jjwt)
- Spring Data JPA
- PostgreSQL 16
- springdoc-openapi (Swagger)
- Maven + Maven Wrapper

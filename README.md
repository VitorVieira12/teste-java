# Digital Bank API



API REST simplificada para simulação de um banco digital, desenvolvida como parte do processo seletivo da Compass UOL.



## Pré-requisitos



- Java 21+

- Docker e Docker Compose



## Como rodar



1. Suba o PostgreSQL (porta **5433** no host, para evitar conflito com PostgreSQL local):



```bash

docker compose up -d

```



2. Execute a aplicação:



```bash

./mvnw spring-boot:run

```



On Windows:



```bash

mvnw.cmd spring-boot:run

```



A API ficará disponível em `http://localhost:8080`.



3. Acesse a documentação Swagger:



- Swagger UI: `http://localhost:8080/swagger-ui.html`

- OpenAPI JSON: `http://localhost:8080/api-docs`



## Usuários de teste (seed)



| Username | Password | Saldo inicial |

|----------|----------|---------------|

| alice | senha123 | 1000.00 |

| bruno | senha123 | 500.00 |

| carla | senha123 | 250.00 |



## Fluxo com JWT



1. Login e obtenção do token:



```bash

curl -X POST http://localhost:8080/api/v1/auth/login \

  -H "Content-Type: application/json" \

  -d "{\"username\": \"alice\", \"password\": \"senha123\"}"

```



2. Registrar nova conta (público):



```bash

curl -X POST http://localhost:8080/api/v1/accounts \

  -H "Content-Type: application/json" \

  -d "{\"name\": \"Daniel Lima\", \"username\": \"daniel\", \"password\": \"senha123\", \"initialBalance\": 200.00}"

```



3. Consultar sua conta (requer token):



```bash

curl http://localhost:8080/api/v1/accounts/me \

  -H "Authorization: Bearer <token>"

```



4. Realizar transferência da sua conta:



```bash

curl -X POST http://localhost:8080/api/v1/transfers \

  -H "Content-Type: application/json" \

  -H "Authorization: Bearer <token>" \

  -d "{\"fromAccountId\": 1, \"toAccountId\": 2, \"amount\": 100.00}"

```



5. Consultar movimentações da sua conta:

```bash
curl "http://localhost:8080/api/v1/accounts/1/movements?page=0&size=10" \
  -H "Authorization: Bearer <token>"
```

6. Consultar notificações:

```bash
curl "http://localhost:8080/api/v1/accounts/me/notifications?page=0&size=10" \
  -H "Authorization: Bearer <token>"
```

## CI

O pipeline GitHub Actions (`.github/workflows/ci.yml`) executa `./mvnw test` em cada push/PR, incluindo testes de integração com Testcontainers.



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



## Segurança



### JWT stateless



- Autenticação via Bearer token no header `Authorization`

- Token contém `username` e `accountId`

- Sessão stateless (sem cookies)



### Ownership por conta



Cada usuário está vinculado a uma conta (`User` 1:1 `Account`). Regras:



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



A aplicação segue arquitetura em camadas:



- **Controller**: exposição REST e validação de entrada

- **Service**: regras de negócio

- **Repository**: persistência com Spring Data JPA

- **Domain**: entidades e enums

- **Security**: JWT, filtros e ownership



### Persistência



- PostgreSQL 16 via Docker Compose

- Spring Data JPA com `ddl-auto: update`

- Seed inicial com 3 contas e usuários no startup



### Consistência e concorrência



Transferências financeiras exigem consistência transacional e controle de concorrência:



1. **Transação (`@Transactional`)**: débito, crédito, registro da transferência e movimentações ocorrem atomicamente.

2. **Lock pessimista (`PESSIMISTIC_WRITE`)**: contas são bloqueadas durante a transferência.

3. **Ordenação de locks**: contas são bloqueadas sempre pelo menor ID primeiro, evitando deadlock em transferências simultâneas A→B e B→A.

4. **Optimistic locking (`@Version`)**: campo de versão em `Account` como camada adicional de proteção, com retry limitado em caso de conflito.



### Movimentações



Cada transferência gera dois registros de movimentação:



- **DEBIT** na conta de origem

- **CREDIT** na conta de destino



### Notificações



Após o commit da transferência, um evento `TransferCompletedEvent` dispara notificações assíncronas (`@Async` + `@TransactionalEventListener(AFTER_COMMIT)`) para origem e destino.

Em falhas (saldo insuficiente, conta inexistente, valor inválido etc.), `TransferFailedEvent` notifica o titular da conta de origem com o motivo. Erros de autorização (`403`) não geram notificação.

O envio é simulado via log e persistido em `notification_logs`.



### Tratamento de erros



Respostas padronizadas via `@RestControllerAdvice`:



- `401` — credenciais inválidas ou token ausente

- `403` — acesso negado (conta de outro usuário)

- `404` — conta não encontrada

- `400` — transferência inválida ou erro de validação

- `409` — saldo insuficiente ou username duplicado



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



Executar testes unitários:



```bash

./mvnw test

```



On Windows:



```bash

mvnw.cmd test

```



Os testes cobrem autenticação JWT, registro, ownership, regras de transferência e validações de negócio.



## Stack



- Java 21

- Spring Boot 3.4

- Spring Security + JWT (jjwt)

- Spring Data JPA

- PostgreSQL 16

- springdoc-openapi (Swagger)

- Maven


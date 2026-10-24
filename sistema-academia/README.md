# Sistema Academia - API REST

API REST para gestão completa de academias de bairro: controle de planos, matrículas, mensalidades, aulas coletivas com lista de espera automática, check-in de entrada, fichas de treino versionadas e relatórios gerenciais.

Desenvolvida com **Java 17**, **Spring Boot 4 / Spring Security 6**, autenticação stateless com **Tokens JWT**, testes unitários e funcionais com **JUnit 5 / MockMvc**, cobertura de testes aferida com **JaCoCo**, documentação interativa com **Swagger / OpenAPI 3** e empacotamento com **Docker e Docker Compose**.

---

## Sumário

- [Requisitos Funcionais](#requisitos-funcionais)
- [Arquitetura e Boas Práticas](#arquitetura-e-boas-práticas)
- [Usuários de Teste (Carga Inicial)](#usuários-de-teste-carga-inicial)
- [Como Rodar o Projeto](#como-rodar-o-projeto)
  - [Opção 1: Com Docker Compose (Recomendado)](#opção-1-com-docker-compose-recomendado)
  - [Opção 2: Execução Local com Banco H2 em Memória](#opção-2-execução-local-com-banco-h2-em-memória)
  - [Opção 3: Execução Local com MySQL](#opção-3-execução-local-com-mysql)
- [Testes e Cobertura (JaCoCo)](#testes-e-cobertura-jacoco)
- [Documentação Swagger / OpenAPI](#documentação-swagger--openapi)
- [Lista Completa de Endpoints](#lista-completa-de-endpoints)

---

## Requisitos Funcionais

| Código | Descrição | Regras de Negócio e Implementação |
|---|---|---|
| **RF01** | Cadastro de Aluno | Nome, CPF único (validado com algoritmo oficial), e-mail único e senha (mínimo de 8 caracteres). A resposta nunca expõe a senha nem seu hash. |
| **RF02** | Login e Autenticação JWT | Login com e-mail e senha. Credenciais inválidas não revelam qual dos dois campos está incorreto (mensagem genérica). Retorna token JWT contendo perfil e identificador do usuário. |
| **RF03** | Cadastro de Planos | A recepção cadastra planos com duração de **1, 3 ou 12 meses**, valor mensal, modalidades incluídas e limite semanal de aulas coletivas. |
| **RF04** | Matrícula de Aluno | A recepção matricula o aluno em um plano. Gera todas as mensalidades da vigência com vencimento no mesmo dia de cada mês. Garante que o aluno só possua **uma matrícula ativa**. |
| **RF05** | Pagamento de Mensalidades | Pagamento após o vencimento cobra **multa de 2%**. Aluno com mensalidade vencida há mais de **5 dias** fica automaticamente bloqueado para novas inscrições e check-in. |
| **RF06** | Troca de Plano | Encerra a matrícula atual e cancela as parcelas futuras; o valor proporcional aos dias não utilizados do mês já pago vira **desconto automático** na primeira parcela da nova matrícula. |
| **RF07** | Cancelamento de Matrícula | Mensalidades futuras são canceladas. Em planos de 3 ou 12 meses, é cobrada **multa rescisória de 10%** sobre a soma das mensalidades canceladas. |
| **RF08** | Cadastro de Aulas Coletivas | Modalidade, instrutor, dia da semana, horário de início, duração em minutos e capacidade. Bloqueia sobreposição de horários para o mesmo instrutor. |
| **RF09** | Inscrição em Aula | Aluno se inscreve em aula da semana corrente caso a modalidade esteja em seu plano e não tenha excedido o limite semanal. Sem vagas imediatas, é enfileirado na **lista de espera (FIFO)**. |
| **RF10** | Cancelamento de Inscrição | Permitido até **2 horas antes** do início da aula. Na mesma transação, a vaga é transferida automaticamente para o primeiro aluno da fila de espera. |
| **RF11** | Lista de Presença | O instrutor responsável registra os alunos presentes; os ausentes recebem falta. Aluno com **2 faltas na mesma semana** fica bloqueado para inscrições na semana seguinte. |
| **RF12** | Check-in na Portaria | A recepção registra a entrada física do aluno. Aluno bloqueado (inadimplência > 5 dias) ou sem matrícula ativa tem entrada recusada (422 Unprocessable Entity). |
| **RF13** | Ficha de Treino | O instrutor monta a ficha com exercícios, séries, repetições e carga. A ficha anterior é automaticamente arquivada no histórico. O aluno consulta sua ficha atual e o histórico. |
| **RF14** | Relatório de Inadimplentes | Lista alunos com parcelas vencidas e o valor total do débito (com a multa de 2% já aplicada). |
| **RF15** | Relatório de Ocupação de Aulas | Média percentual de ocupação das aulas coletivas por modalidade no período informado (padrão: últimos 30 dias). |
| **RF16** | Relatório de Frequência Mensal | Total de entradas registradas por aluno no mês especificado (ou mês corrente). |

---

## Arquitetura e Boas Práticas

O projeto segue as diretrizes da arquitetura em camadas do ecossistema Spring:

- **DTOs (Data Transfer Objects):** Records Java imutáveis para requisições e respostas da API, isolando o modelo de dados e impedindo vazamento de entidades JPA e credenciais sensíveis.
- **Validação com Bean Validation:** Validações declarativas nos DTOs (`@NotNull`, `@NotBlank`, `@Size(min=8)`, `@Positive`, anotação personalizada `@DuracaoMesesValida` para 1, 3 e 12 meses, e validação algorítmica de CPF).
- **Camada de Serviço Especializada:** Toda regra de negócio reside em `@Service`, garantindo integridade transacional (`@Transactional`) para operações complexas como troca de plano, cancelamento com promoção de lista de espera e registro de presença.
- **Camada de Repositório:** Interfaces Spring Data JPA com métodos de consulta customizados, projeções e bloqueios pessimistas (`@Lock(PESSIMISTIC_WRITE)`) para evitar condições de corrida (ex: concorrência na última vaga de uma aula coletiva).
- **Tratamento Centralizado de Erros:** `@RestControllerAdvice` (`GlobalExceptionHandler`) mapeando exceções semânticas para respostas JSON padronizadas (`ErroResponse`), com status HTTP apropriados (400, 401, 403, 404, 409, 422).
- **Segurança e JWT:** Spring Security 6 com autenticação stateless via `Bearer JWT`. Controle de autorização fino baseado em perfis (`ROLE_ALUNO`, `ROLE_RECEPCAO`, `ROLE_INSTRUTOR`) com `@PreAuthorize`.

---

## Usuários de Teste (Carga Inicial)

Na inicialização da aplicação, as contas fundamentais são criadas automaticamente:

| Perfil | Nome | E-mail | Senha |
|---|---|---|---|
| **Recepção** | Recepção Academia | `recepcao@academia.com` | `Recepcao@123` |
| **Instrutor 1** | Carlos Instrutor | `instrutor@academia.com` | `Instrutor@123` |
| **Instrutor 2** | Ana Instrutora | `instrutor2@academia.com` | `Instrutor@123` |
| **Aluno (Demo)** | Aluno Demonstração | `aluno@academia.com` | `Aluno@1234` |

> Novos alunos podem se cadastrar a qualquer momento através do endpoint `POST /api/auth/cadastro`.

---

## Como Rodar o Projeto

### Pré-requisitos
- **Java 17** (JDK 17) instalado
- **Maven 3.8+** (ou o próprio `./mvnw` embutido)
- **Docker e Docker Compose** (para execução em contêineres)

---

### Opção 1: Com Docker Compose (Recomendado)

Sobe o banco MySQL 8.0 e a aplicação Spring Boot pré-configurada em contêineres isolados:

```bash
docker compose up --build
```

Para encerrar os contêineres:
```bash
docker compose down
```

---

### Opção 2: Execução Local com Banco H2 em Memória

Não requer MySQL instalado. Ideal para execução rápida e desenvolvimento local:

No Linux / macOS:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

No Windows (PowerShell / Prompt):
```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=h2
```

A API estará disponível em `http://localhost:8080`.

---

### Opção 3: Execução Local com MySQL

1. Certifique-se de que o MySQL esteja rodando na porta `3306` com usuário `academia` e senha `academia` (ou ajuste as variáveis em `src/main/resources/application.properties`).
2. Execute:
```powershell
.\mvnw.cmd spring-boot:run
```

---

## Testes e Cobertura (JaCoCo)

O projeto possui **142 testes automatizados**, divididos entre:
- **Testes Unitários:** Validação isolada de regras de modelos e serviços com mocks via Mockito.
- **Testes Funcionais (E2E):** Testes ponta a ponta simulando requisições HTTP reais com MockMvc, Spring Security, JWT e persistência em banco H2.

### Executar os Testes

```powershell
.\mvnw.cmd test
```

### Executar a Verificação Completa e Gerar Relatório JaCoCo

```powershell
.\mvnw.cmd clean verify
```

O relatório visual de cobertura em HTML é gerado em:
```
target/site/jacoco/index.html
```

> **Cobertura alcançada:**
> - **Linhas de Código:** **> 94%** (exigência mínima do build: 80%)
> - **Branches:** **> 95%**

---

## Documentação Swagger / OpenAPI

Com a aplicação em execução, acesse a documentação interativa com suporte para execução de chamadas:

- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### Como autenticar no Swagger:
1. Faça login chamando o endpoint `POST /api/auth/login` com as credenciais de teste.
2. Copie o valor do campo `token` retornado no corpo da resposta.
3. No topo da página do Swagger UI, clique no botão **Authorize** (cadeado).
4. Cole o token no campo de texto e clique em **Authorize**. Todas as requisições subsequentes incluirão o header `Authorization: Bearer <token>`.

---

## Lista Completa de Endpoints

### 1. Autenticação e Usuários
| Método | Rota | Perfil Permitido | RF | Descrição |
|---|---|---|---|---|
| `POST` | `/api/auth/cadastro` | Público | RF01 | Cadastro de novo aluno |
| `POST` | `/api/auth/login` | Público | RF02 | Login e emissão de token JWT |
| `GET` | `/api/alunos` | `RECEPCAO`, `INSTRUTOR` | - | Lista alunos (filtro por nome) |
| `GET` | `/api/alunos/{id}` | `ALUNO`, `RECEPCAO`, `INSTRUTOR` | - | Consulta dados de um aluno (aluno só consulta a si) |

### 2. Planos e Matrículas
| Método | Rota | Perfil Permitido | RF | Descrição |
|---|---|---|---|---|
| `POST` | `/api/planos` | `RECEPCAO` | RF03 | Cadastra plano (1, 3 ou 12 meses) |
| `GET` | `/api/planos` | Autenticado | RF03 | Lista todos os planos disponíveis |
| `GET` | `/api/planos/{id}` | Autenticado | RF03 | Detalha um plano |
| `POST` | `/api/matriculas` | `RECEPCAO` | RF04 | Matricula aluno e gera mensalidades |
| `GET` | `/api/matriculas/{id}` | `ALUNO`, `RECEPCAO` | RF04 | Consulta matrícula (aluno só acessa a própria) |
| `GET` | `/api/alunos/{alunoId}/matriculas` | `ALUNO`, `RECEPCAO` | RF04 | Histórico de matrículas do aluno |
| `GET` | `/api/alunos/{alunoId}/matriculas/ativa`| `ALUNO`, `RECEPCAO` | RF04 | Retorna a matrícula ativa atual |
| `POST` | `/api/matriculas/{id}/troca-plano` | `RECEPCAO` | RF06 | Troca de plano com crédito proporcional |
| `POST` | `/api/matriculas/{id}/cancelamento` | `RECEPCAO` | RF07 | Cancela matrícula (multa de 10% para 3/12 meses) |

### 3. Mensalidades
| Método | Rota | Perfil Permitido | RF | Descrição |
|---|---|---|---|---|
| `GET` | `/api/alunos/{alunoId}/mensalidades` | `ALUNO`, `RECEPCAO` | RF05 | Lista mensalidades do aluno (filtro por status) |
| `GET` | `/api/mensalidades/{id}` | `ALUNO`, `RECEPCAO` | RF05 | Detalha uma mensalidade |
| `POST` | `/api/mensalidades/{id}/pagamento` | `ALUNO`, `RECEPCAO` | RF05 | Paga mensalidade (calcula multa de 2% se atrasada) |

### 4. Aulas Coletivas e Inscrições
| Método | Rota | Perfil Permitido | RF | Descrição |
|---|---|---|---|---|
| `POST` | `/api/aulas` | `RECEPCAO` | RF08 | Cadastra aula coletiva (valida conflito de horário) |
| `GET` | `/api/aulas` | Autenticado | RF08 | Lista aulas (filtro por modalidade) |
| `GET` | `/api/aulas/{id}` | Autenticado | RF08 | Detalhes de uma aula coletiva |
| `POST` | `/api/aulas/{aulaId}/inscricoes` | `ALUNO` | RF09 | Inscreve aluno na aula ou entra na lista de espera |
| `GET` | `/api/aulas/{aulaId}/inscricoes` | `RECEPCAO`, `INSTRUTOR` | RF09 | Lista confirmados e fila de espera da ocorrência |
| `GET` | `/api/alunos/{alunoId}/inscricoes` | `ALUNO`, `RECEPCAO` | RF09 | Lista inscrições do aluno |
| `POST` | `/api/inscricoes/{id}/cancelamento` | `ALUNO`, `RECEPCAO` | RF10 | Cancela inscrição até 2h antes e promove 1º da fila |
| `POST` | `/api/aulas/{id}/presencas` | `INSTRUTOR` | RF11 | Registra presença/falta (2 faltas geram suspensão) |

### 5. Entrada e Fichas de Treino
| Método | Rota | Perfil Permitido | RF | Descrição |
|---|---|---|---|---|
| `POST` | `/api/checkins` | `RECEPCAO` | RF12 | Registra entrada (bloqueia inadimplente > 5 dias) |
| `POST` | `/api/alunos/{alunoId}/fichas` | `INSTRUTOR` | RF13 | Cria nova ficha (arquiva anterior no histórico) |
| `GET` | `/api/alunos/{alunoId}/fichas/atual` | `ALUNO`, `INSTRUTOR` | RF13 | Consulta ficha de treino vigente |
| `GET` | `/api/alunos/{alunoId}/fichas/historico`| `ALUNO`, `INSTRUTOR`| RF13 | Consulta histórico de fichas anteriores |

### 6. Relatórios Gerenciais
| Método | Rota | Perfil Permitido | RF | Descrição |
|---|---|---|---|---|
| `GET` | `/api/relatorios/inadimplentes` | `RECEPCAO` | RF14 | Lista inadimplentes e valor devido com multa de 2% |
| `GET` | `/api/relatorios/ocupacao-aulas` | `RECEPCAO`, `INSTRUTOR` | RF15 | Taxa de ocupação média das aulas por modalidade |
| `GET` | `/api/relatorios/frequencia` | `RECEPCAO` | RF16 | Frequência de entradas por aluno no mês |

---

## Estrutura do Projeto

```
sistema-academia/
├── src/
│   ├── main/
│   │   ├── java/ifsp/edu/sistema_academia/
│   │   │   ├── config/          # Configurações Spring Security, JWT, Swagger, Seed
│   │   │   ├── controller/      # Controladores REST com anotações OpenAPI e PreAuthorize
│   │   │   ├── dto/             # Records de entrada (request) e saída (response)
│   │   │   ├── exception/       # Tratamento centralizado de erros e exceções de domínio
│   │   │   ├── model/           # Entidades JPA de domínio e Enums
│   │   │   ├── repository/      # Repositórios Spring Data JPA e queries JPQL
│   │   │   ├── security/        # Serviços e conversores JWT
│   │   │   ├── service/         # Serviços de regras de negócio e orquestração
│   │   │   ├── util/            # Utilitários (ex: cálculo de semana ISO-8601)
│   │   │   └── validation/      # Validadores customizados (ex: duração de planos)
│   │   └── resources/
│   │       ├── application.properties      # Configuração principal (MySQL / Docker)
│   │       └── application-h2.properties   # Perfil local H2 em memória
│   └── test/
│       ├── java/ifsp/edu/sistema_academia/
│       │   ├── funcional/       # Testes ponta a ponta com MockMvc (RF01 a RF16)
│       │   ├── model/           # Testes unitários das entidades de domínio
│       │   ├── security/        # Testes de segurança e emissão/validação de JWT
│       │   ├── service/         # Testes unitários das regras de negócio com Mockito
│       │   └── support/         # Fixtures, relógio mutável e base para testes funcionais
│       └── resources/
│           └── application-test.properties # Configuração isolada de testes
├── Dockerfile                   # Build multi-etapas otimizado
├── docker-compose.yml           # Orquestração MySQL 8.0 + API Spring Boot
├── pom.xml                      # Dependências, plugins do Maven e JaCoCo
└── README.md                    # Instruções e documentação completa
```

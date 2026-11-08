# PediuPatrão

Sistema de pedidos para pizzaria/lanchonete, feito com **Java 21, Spring Boot 3.4, Spring Security, Thymeleaf e MongoDB**.

Este repositório reúne a evolução do sistema:
- controle de acesso por perfil (ADMIN, GERENTE e ATENDENTE);
- pedidos com fluxo de status, desconto e cancelamento;
- auditoria das alterações;
- **Dashboard Administrativo** com uma API própria (atividade M2).

## Vídeo de demonstração
[Assistir ao vídeo (até 3 min)](video em produção ainda)

## Documentação
- [Requisitos, endpoints e evidências de testes](docs/DOCUMENTACAO_DASHBOARD.md)
- [Fluxo de branches (gitflow)](docs/GITFLOW.md)

## Como executar

**Precisa de:** JDK 21 e um MongoDB (local ou Atlas gratuito).

1. Clone o repositório e entre na pasta.
2. Por padrão, o sistema conecta em `mongodb://localhost:27017/pizzaria`.
   Para usar o MongoDB Atlas, crie o arquivo `src/main/resources/application-local.properties` com:
   ```
   spring.data.mongodb.uri=mongodb+srv://USUARIO:SENHA@SEU_CLUSTER.mongodb.net/pizzaria?retryWrites=true&w=majority
   pediupatrao.dados-demo=true
   ```
   (`dados-demo=true` cria 80 pedidos de exemplo para o dashboard. Esse arquivo não vai para o Git.)
3. Rode:
   ```
   ./mvnw spring-boot:run "-Dspring-boot.run.profiles=local"
   ```
   Sem o perfil `local`, use apenas `./mvnw spring-boot:run`.
4. Acesse http://localhost:8080
5. Testes automáticos: `./mvnw test`

**Usuários criados automaticamente** (senha `teste123` para todos):

| Usuário | Perfil | O que acessa |
|---|---|---|
| admin | ADMIN | usuários, auditoria, dashboard, pedidos |
| gerente | GERENTE | dashboard, auditoria, pedidos, descontos e cancelamentos |
| atendente | ATENDENTE | clientes e pedidos |

## Dashboard (atividade M2)
Página `/dashboard` (ADMIN e GERENTE), alimentada pela API `/api/dashboard/*`:

| Endpoint | O que devolve |
|---|---|
| `GET /api/dashboard/resumo` | faturamento, pedidos, ticket médio, itens e descontos |
| `GET /api/dashboard/vendas-por-periodo` | vendas por dia ou por mês (gráfico) |
| `GET /api/dashboard/pedidos-por-status` | quantidade de pedidos por status |
| `GET /api/dashboard/produtos-mais-vendidos` | ranking de produtos |
| `GET /api/dashboard/pedidos-recentes` | últimos pedidos |

Filtros: `inicio`, `fim`, `status`, `produtoId` (e `agrupamento`, `limite` em alguns). Pedido cancelado não conta como venda.

## Regras do sistema
- **Perfis:** a autorização é feita no backend (Spring Security), não só escondendo botões.
- **Pedidos:** `RECEBIDO → EM_PREPARACAO → PRONTO → SAIU_PARA_ENTREGA ou RETIRADO → FINALIZADO`.
  Só o GERENTE cancela (com justificativa) e dá desconto de até 20%, antes da saída.
- **Auditoria:** registra usuário, perfil, data/hora, campos alterados (antes e depois) e justificativa.

## Estrutura
- `entity/`, `repository/`, `service/`, `controller/`, `dto/`, `config/`
- `src/main/resources/templates/`: telas Thymeleaf
- `src/test/`: testes unitários
- `docs/`: documentação e evidências
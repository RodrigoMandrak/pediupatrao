# PediuPatrão — Atividade Prática M1 (Parte 2)

Sistema de pedidos de uma pizzaria (Java 21, Spring Boot 3.4, Spring Security, Thymeleaf e MongoDB Atlas),
evoluído com controle de acesso por perfil, controle do fluxo de pedidos e auditoria.

## Como executar

1. Crie um cluster/banco no **MongoDB Atlas** e copie a string de conexão.
2. Em `src/main/resources/application.properties`, troque `spring.data.mongodb.uri` pela sua string.
3. Abra no NetBeans (ou outra IDE) e execute `PizzariaPedidosApplication`, ou rode `./mvnw spring-boot:run`.
4. Acesse `http://localhost:8080`.
5. Testes: `./mvnw test`.

Usuários criados automaticamente na primeira execução (senha **teste123** para todos; troque depois):

| Usuário | Perfil |
|---|---|
| admin | ADMIN |
| gerente | GERENTE |
| atendente | ATENDENTE |

## Requisitos implementados

**RF01 – Perfis e permissões**
- Três perfis: `ADMIN`, `GERENTE`, `ATENDENTE` (enum `Perfil`). `UsuarioDetailsService` usa o perfil real do usuário.
- Autorização no backend em `SecurityConfig` (rotas) e com `@PreAuthorize` nos controllers/services. Esconder botões na tela é só conveniência.
- Só o ADMIN acessa `/usuarios` e `/api/usuarios`. O ADMIN não pode alterar o próprio perfil nem se excluir; perfis inválidos são recusados.
- O endpoint `/api/usuarios`, que antes era público, agora exige ADMIN. A senha não é mais devolvida pela API.
- CSRF reativado: formulários Thymeleaf enviam o token e os `fetch` das telas mandam o cabeçalho (ver `layout.html`).

**RF02 – Pedido**: itens (produto, preço, quantidade), subtotal, desconto, total, responsável e data de entrada.
O preço vem do cadastro do produto e o total é calculado em `PedidoService`; status, total e desconto enviados na requisição são ignorados.

**RF03 – Fluxo**: `RECEBIDO → EM_PREPARACAO → PRONTO → SAIU_PARA_ENTREGA | RETIRADO → FINALIZADO`.
Transições inválidas retornam erro 409. Na saída são gravados data/hora e responsável.

**RF04 – Gerente**: só o GERENTE cancela (justificativa obrigatória, só antes da saída, dados preservados, sem alterações depois)
e aplica desconto percentual (máx. 20%, só antes da saída).

**RF05 – Auditoria**: coleção `auditoria` no MongoDB com usuário, perfil, data/hora, operação, entidade/id,
campos alterados (valor anterior e novo) e justificativa. Audita criação de pedido, mudança de status, desconto, cancelamento
e criação/alteração/exclusão de clientes. Consulta em `/auditoria` e `GET /api/auditoria` (ADMIN e GERENTE).
O repositório (`AuditoriaRepository`) só tem salvar e consultar, e a entidade não tem setters, então não há como alterar ou apagar registros.
Senhas e tokens nunca são gravados.

## Principais arquivos alterados/criados
- `entity/`: `Perfil`, `StatusPedido`, `TipoOperacao`, `ItemPedido`, `Pedido` (evoluído), `Auditoria`, `Alteracao`
- `service/`: `PedidoService`, `ClienteService`, `AuditoriaService`, `UsuarioService`, `UsuarioDetailsService`, `UsuarioLogado`
- `controller/`: `PedidoController`, `AuditoriaController`, `ApiExceptionHandler`, `HomeController`, `UsuarioController`
- `config/`: `SecurityConfig`, `DataInitializer`
- `templates/`: `pedidos.html`, `pedidos/form.html`, `auditoria.html`, sidebar, layout e listas ajustados por perfil
- `src/test`: `PedidoServiceTest`

## Teste manual de acesso indevido (para o vídeo)
Logado como `atendente`, tente `POST /api/pedidos/{id}/desconto` ou `GET /api/auditoria` → **403**.
Logado como `admin`, tente cancelar um pedido → **403**.

## Dashboard Administrativo (atividade M2)
Página `/dashboard` (ADMIN e GERENTE) com indicadores, gráficos, situação dos pedidos, ranking de produtos e filtros, alimentada pela API `/api/dashboard/*`.
Documentação completa (requisitos, endpoints, testes): [docs/DOCUMENTACAO_DASHBOARD.md](docs/DOCUMENTACAO_DASHBOARD.md). Fluxo de branches: [docs/GITFLOW.md](docs/GITFLOW.md).

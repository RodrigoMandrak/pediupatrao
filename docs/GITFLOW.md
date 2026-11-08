# Passo a passo Gitflow (VS Code)

Rode no terminal do VS Code (`Ctrl+'`), dentro da pasta do projeto. Troque a URL pela do seu repositório novo e vazio no GitHub.

```bash
git init
git branch -M main
# commit base: tudo, EXCETO os arquivos novos do dashboard (eles entram nas branches de feature)
git add . ':!docs' ':!src/test/java/com/umc/pediupatrao/service/DashboardServiceTest.java' ':!src/main/resources/templates/dashboard.html' ':!src/main/java/com/umc/pediupatrao/dto' ':!src/main/java/com/umc/pediupatrao/service/DashboardService.java' ':!src/main/java/com/umc/pediupatrao/controller/DashboardController.java' ':!src/main/java/com/umc/pediupatrao/config/DadosDemoInitializer.java'
git commit -m "chore: projeto base PediuPatrão (perfis, pedidos e auditoria)"
git remote add origin https://github.com/SEU_USUARIO/SEU_REPO.git
git push -u origin main

# branch de desenvolvimento
git checkout -b develop
git push -u origin develop

# feature 1: API do dashboard
git checkout -b feature/dashboard-api develop
git add src/main/java src/test
git commit -m "feat(api): endpoints do dashboard com filtros e testes"
git checkout develop && git merge --no-ff feature/dashboard-api -m "merge: feature/dashboard-api"
git push -u origin feature/dashboard-api develop

# feature 2: tela do dashboard
git checkout -b feature/dashboard-ui develop
git add src/main/resources/templates/dashboard.html
git commit -m "feat(ui): tela do dashboard com graficos e filtros"
git checkout develop && git merge --no-ff feature/dashboard-ui -m "merge: feature/dashboard-ui"
git push -u origin feature/dashboard-ui develop

# feature 3: documentação
git checkout -b feature/documentacao develop
git add docs README.md
git commit -m "docs: requisitos, endpoints e evidencias de testes"
git checkout develop && git merge --no-ff feature/documentacao -m "merge: feature/documentacao"

# release e publicação na main
git checkout -b release/1.0.0 develop
git checkout main && git merge --no-ff release/1.0.0 -m "release: 1.0.0"
git tag v1.0.0
git checkout develop && git merge --no-ff release/1.0.0
git push origin main develop --tags
```

Antes de mandar o link no Teams, abra o repositório no GitHub e confira se as branches `main`, `develop` e `feature/...` aparecem.

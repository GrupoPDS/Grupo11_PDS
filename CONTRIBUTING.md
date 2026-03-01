# 📚 Guia de Contribuição — Library System UFU

Bem-vindo(a) ao time! 🎉 Este guia vai te ajudar a entender a arquitetura do projeto, rodar tudo localmente e contribuir com confiança.

---

## 🏗️ Arquitetura do Projeto

```
Grupo11_PDS/
├── backend/          ← API REST (Spring Boot + Java 17)
│   ├── src/main/resources/db/migration/  ← Scripts SQL do Flyway
│   ├── Dockerfile
│   └── pom.xml
├── frontend/         ← Interface Web (React + Vite + TypeScript)
│   ├── src/
│   ├── Dockerfile
│   └── package.json
├── docker/           ← Docker Compose para desenvolvimento local
│   └── compose.yml
├── .github/workflows/ ← Pipelines CI/CD (GitHub Actions)
│   ├── ci-cd.yml     ← Lint, testes, build Docker, Trivy
│   └── security.yml  ← CodeQL (análise estática de segurança)
├── render.yaml       ← Infraestrutura como Código (Render.com)
├── dev.sh            ← Script de atalho para subir o ambiente
└── DEPLOY.md         ← Guia de deploy em produção
```

| Pasta | O que faz |
|---|---|
| `backend/` | API REST com Spring Boot. Contém Entidades, Controllers, Services e as migrações SQL do banco de dados. |
| `frontend/` | Interface React com Vite e TailwindCSS. Se comunica com o backend via `VITE_API_URL`. |
| `docker/` | Arquivo `compose.yml` que sobe o banco PostgreSQL + backend + frontend em containers. |
| `.github/` | Pipelines automatizados que verificam linter, testes e segurança a cada push/PR. |

---

## ✅ Pré-requisitos

Antes de começar, instale na sua máquina:

| Ferramenta | Versão mínima | Como verificar | Link |
|---|---|---|---|
| **Git** | 2.x | `git --version` | [git-scm.com](https://git-scm.com) |
| **Docker Desktop** | 4.x | `docker --version` | [docker.com](https://www.docker.com/products/docker-desktop/) |
| **Java (JDK)** | 17 | `java -version` | [Adoptium](https://adoptium.net/) |
| **Node.js** | 20 | `node --version` | [nodejs.org](https://nodejs.org/) |
| **npm** | 10+ | `npm --version` | (vem com Node.js) |

> 💡 **Dica:** No macOS, use `brew install openjdk@17 node@20` para instalar tudo rapidamente.

---

## 🚀 Passo a Passo para Rodar Localmente

### 1. Clone o repositório

```bash
git clone https://github.com/GrupoPDS/Grupo11_PDS.git
cd Grupo11_PDS
```

### 2. Suba o banco + API com Docker

```bash
# Opção 1: Script de atalho
chmod +x dev.sh && ./dev.sh

# Opção 2: Docker Compose direto
docker compose -f docker/compose.yml up --build
```

Aguarde até ver no terminal:
```
Started LibraryApplication in X seconds
```

🎯 O backend estará rodando em **http://localhost:8080**

### 3. Rode o frontend separadamente (para desenvolvimento)

```bash
cd frontend
npm install
npm run dev
```

🎯 O frontend estará rodando em **http://localhost:5173**

### 4. Teste se tudo está funcionando

```bash
# No terminal, teste a API:
curl http://localhost:8080/api/books
```

---

## 🐳 Rodando os Testes Localmente (Padrão GitHub Actions)

Para garantir que seu código vai passar no pipeline do GitHub (CI/CD) antes de abrir um PR, nós padronizamos a execução dos testes via Docker. Isso significa que **todo mundo roda os testes no mesmo ambiente exato** (Java 17 e Node 20), sem o perigo de "na minha máquina funciona".

Para rodar todos os testes, verificação de formatação e linters do Backend e Frontend de uma vez:

```bash
# Na raiz do projeto, execute:
./run-tests.sh
```

Isso vai subir containers temporários (`docker-compose.test.yml`) e testar tudo. Se der `<span style="color:green">✅ Sucesso</span>`, você está pronto para commitar e abrir seu PR!

---

## ⚠️ A Regra de Ouro do Banco de Dados (Flyway)

> [!CAUTION]
> **NUNCA altere a estrutura do banco diretamente nas classes Java (Entidades).** O Hibernate está em modo `validate` — ele só VERIFICA, não cria tabelas.

### Como funciona:

1. **Quer adicionar uma coluna?** Crie um arquivo SQL em:
   ```
   backend/src/main/resources/db/migration/
   ```

2. **Siga o padrão de nomenclatura:**
   ```
   V<número>__<Descrição>.sql
   ```
   Exemplo:
   ```
   V2__Add_category_to_books.sql
   ```

3. **Depois** atualize a classe Java (`Book.java`, por exemplo) para refletir a mudança.

4. **Use a mensagem de commit:**
   ```bash
   git commit -m "migration: add category column to books"
   ```
   > Isso pula o pipeline completo e roda apenas a verificação de nomenclatura, economizando tempo!

### ❌ O que NÃO fazer:

```java
// NÃO adicione campos na Entidade sem criar o SQL primeiro!
@Column(name = "category")
private String category;  // ← O app vai QUEBRAR se não existir a migração
```

### ✅ O que fazer:

```sql
-- V2__Add_category_to_books.sql
ALTER TABLE books ADD COLUMN category VARCHAR(100);
```

```java
// Agora sim, adicione na Entidade
@Column(name = "category", length = 100)
private String category;
```

---

## 🧊 O Fluxo de Commit (e o "Congelamento")

Quando você rodar `git commit`, o terminal vai parecer que **travou** por alguns segundos. **Não cancele!** 🛑

### O que está acontecendo:

```
$ git commit -m "feat: add search endpoint"
🚧 Guardião Local: auto-formatando código antes do commit...
📐 Formatando Java (Spotless)...      ← ~5 segundos
📐 Formatando Frontend (Prettier)...  ← ~2 segundos
✅ Código formatado e aprovado pelo Guardião Local.
[main abc1234] feat: add search endpoint
```

O **Guardião Local** (pre-commit hook) formata seu código automaticamente:

| Linguagem | Ferramenta | O que faz |
|---|---|---|
| Java | **Spotless** (Google Java Format) | Indentação, imports organizados |
| React/TS | **Prettier** | Aspas, vírgulas, ponto-e-vírgula |

> 💡 **Tradução:** O terminal não travou — ele está deixando seu código bonito para você! Aguarde os ~10 segundos e o commit será feito normalmente.

---

## 🔀 Fluxo de Trabalho com Git

### Para features novas:

```bash
# 1. Atualize a main
git pull origin main

# 2. Crie uma branch
git checkout -b feat/minha-feature

# 3. Code, code, code... 💻

# 4. Commit (aguarde o Guardião!)
git add .
git commit -m "feat: descrição da feature"

# 5. Push
git push origin feat/minha-feature

# 6. Abra um Pull Request no GitHub
```

### Convenção de commits:

| Prefixo | Quando usar | Exemplo |
|---|---|---|
| `feat:` | Nova funcionalidade | `feat: add book search endpoint` |
| `fix:` | Correção de bug | `fix: handle null ISBN in validation` |
| `migration:` | Apenas mudanças no banco | `migration: add category to books` |
| `ci:` | Mudanças na esteira CI/CD | `ci: add Trivy scanner` |
| `docs:` | Documentação | `docs: update README` |
| `refactor:` | Refatoração sem mudança de comportamento | `refactor: extract book mapper` |

---

## 🤖 O que o GitHub Actions verifica (automaticamente)

Quando você fizer push ou abrir um PR, o pipeline roda:

```
☕ Backend     → Spotless (formato) + JUnit (testes) + Flyway (migrações)
⚛️ Frontend    → Prettier (formato) + ESLint (lint)
🐳 Docker      → Build da imagem + Trivy (vulnerabilidades)
🔐 Security    → CodeQL (análise estática de segurança)
```

Se algo falhar, o PR fica com ❌ vermelho. Corrija e faça push novamente!

---

## 🆘 Problemas Comuns

### "O commit está demorando muito!"
O Spotless precisa baixar dependências na primeira vez (~30s). Nas próximas, leva ~5s.

### "Meu código foi alterado sozinho!"
É o Prettier/Spotless formatando automaticamente. Isso é normal e esperado! 🎨

### "O pipeline falhou no Spotless!"
Rode localmente para ver o que precisa mudar:
```bash
cd backend && ./mvnw spotless:apply
```

### "O pipeline falhou no Prettier!"
```bash
cd frontend && npm run format
```

### "A aplicação não sobe — erro no Flyway!"
Provavelmente você alterou uma Entidade sem criar a migração SQL. Veja a [Regra de Ouro](#️-a-regra-de-ouro-do-banco-de-dados-flyway).

---

**Dúvidas?** Mande mensagem no grupo do time! Estamos aqui para ajudar. 💪

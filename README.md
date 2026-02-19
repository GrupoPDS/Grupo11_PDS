# Library System UFU

> Sistema de Gerenciamento de Biblioteca — Prática de Desenvolvimento de Software (PDS), Universidade Federal de Uberlândia.

---

## 🛠 Tech Stack

| Camada | Tecnologia |
|---|---|
| **Backend** | Java 17, Spring Boot 3.2, Maven |
| **Frontend** | React, TypeScript, Vite, TailwindCSS |
| **Banco de Dados** | PostgreSQL 16 |
| **Documentação** | SpringDoc OpenAPI (Swagger) |
| **CI/CD** | GitHub Actions |
| **Infra** | Docker + Docker Compose + Nginx |

---

## 📂 Estrutura do Monorepo

```
├── backend/              # Spring Boot API
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── frontend/             # React + Vite SPA
│   ├── src/
│   ├── package.json
│   ├── Dockerfile
│   └── nginx.conf
├── docker/
│   └── compose.yml       # Orquestra todos os serviços
├── dev.sh                # Script de conveniência
└── .github/workflows/    # CI/CD
```

---

## 🚀 Como Rodar

### Pré-requisitos

- [Docker](https://docs.docker.com/get-docker/) e [Docker Compose](https://docs.docker.com/compose/install/) instalados.

### Subir tudo com um comando

```bash
./dev.sh up
```

Ou diretamente:

```bash
docker compose -f docker/compose.yml up --build
```

| Serviço | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| PostgreSQL | localhost:5432 |

### Outros comandos

```bash
./dev.sh down      # Parar serviços
./dev.sh restart   # Reiniciar tudo
./dev.sh logs      # Ver logs em tempo real
./dev.sh build     # Rebuild sem subir
```

---

## 🧑‍💻 Desenvolvimento Local (sem Docker)

### Backend

```bash
cd backend
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

O frontend dev server roda em **http://localhost:5173** com hot-reload.

---

## 🧪 Testes

```bash
cd backend
mvn clean verify
```

Relatório JaCoCo: `backend/target/site/jacoco/index.html`

---

## 👥 Equipe

Grupo 11 — PDS — UFU

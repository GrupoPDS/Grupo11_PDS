# 🚀 Guia de Deploy — Library System UFU

Este guia explica como colocar o projeto Library UFU em produção usando serviços gratuitos.

---

## Arquitetura de Produção

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│    Vercel        │────▶│    Render        │────▶│    Neon.tech     │
│  (Frontend)      │     │  (Backend API)   │     │  (PostgreSQL)    │
│  React + Vite    │     │  Spring Boot     │     │  Serverless DB   │
│  porta: 443      │     │  porta: 443      │     │  porta: 5432     │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

---

## 1️⃣ Banco de Dados — Neon.tech

[Neon](https://neon.tech) oferece PostgreSQL serverless gratuito (500MB).

### Passo a passo

1. Acesse [neon.tech](https://neon.tech) e crie uma conta (login com GitHub).
2. Clique em **"Create Project"**.
3. Configure:
   - **Project Name:** `library-ufu`
   - **Region:** São Paulo (ou o mais próximo)
   - **PostgreSQL Version:** 16
4. Após criar, copie a **Connection String** que aparece. Ela terá este formato:

```
postgresql://neondb_owner:SENHA@ep-xxx.sa-east-1.aws.neon.tech/neondb?sslmode=require
```

> [!IMPORTANT]
> Guarde essa string! Você vai usá-la nas variáveis de ambiente do Render.

---

## 2️⃣ Backend — Render.com

[Render](https://render.com) hospeda containers Docker gratuitamente.

### Passo a passo

1. Acesse [render.com](https://render.com) e crie uma conta (login com GitHub).
2. Clique em **"New" → "Web Service"**.
3. Conecte seu repositório GitHub (`Grupo11_PDS`).
4. Configure:

| Campo | Valor |
|---|---|
| **Name** | `library-ufu-api` |
| **Region** | Oregon (US West) ou o mais próximo |
| **Branch** | `main` |
| **Root Directory** | `backend` |
| **Runtime** | Docker |
| **Instance Type** | Free |

5. Em **"Environment Variables"**, adicione:

| Variável | Valor | Descrição |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://ep-xxx.neon.tech/neondb?sslmode=require` | String do Neon (troque `postgresql://` por `jdbc:postgresql://`) |
| `SPRING_DATASOURCE_USERNAME` | `neondb_owner` | Usuário do Neon |
| `SPRING_DATASOURCE_PASSWORD` | `sua_senha_do_neon` | Senha do Neon |
| `PORT` | `8080` | Porta da aplicação |
| `LOG_LEVEL` | `INFO` | Nível de log em produção |

> [!WARNING]
> **Atenção com a URL!** O Neon fornece `postgresql://user:pass@host/db`. Para o Spring, troque o prefixo para `jdbc:postgresql://host/db` (sem user:pass, pois eles vão separados).

6. Clique em **"Deploy Web Service"**.
7. Após o deploy, sua API estará em: `https://library-ufu-api.onrender.com`

### Verificar

```bash
curl https://library-ufu-api.onrender.com/swagger-ui.html
```

---

## 3️⃣ Frontend — Vercel

[Vercel](https://vercel.com) é ideal para SPAs React (deploy automático).

### Passo a passo

1. Acesse [vercel.com](https://vercel.com) e crie uma conta (login com GitHub).
2. Clique em **"Add New..." → "Project"**.
3. Importe o repositório `Grupo11_PDS`.
4. Configure:

| Campo | Valor |
|---|---|
| **Root Directory** | `frontend` |
| **Framework Preset** | Vite |
| **Build Command** | `npm run build` |
| **Output Directory** | `dist` |

5. Em **"Environment Variables"**, adicione:

| Variável | Valor |
|---|---|
| `VITE_API_URL` | `https://library-ufu-api.onrender.com` |

6. Clique em **"Deploy"**.
7. Seu frontend estará em: `https://library-ufu.vercel.app`

---

## 📋 Checklist de Deploy

- [ ] Criar projeto no Neon.tech e copiar connection string
- [ ] Criar Web Service no Render, apontar para `backend/`, configurar env vars
- [ ] Aguardar primeiro deploy do Render (pode levar ~5min)
- [ ] Testar API via Swagger: `https://SEU_APP.onrender.com/swagger-ui.html`
- [ ] Criar projeto na Vercel, apontar para `frontend/`, configurar `VITE_API_URL`
- [ ] Testar frontend: `https://SEU_APP.vercel.app`
- [ ] Configurar CORS no backend para aceitar requests do domínio Vercel

---

## ⚠️ Notas Importantes

### Cold Start (Render Free)
O plano Free do Render **desliga** a instância após 15min de inatividade. A primeira request após inatividade leva ~30s para "acordar". Isso é normal.

### CORS em Produção
Você precisará configurar o CORS no backend para aceitar requisições do domínio da Vercel:

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins(
                "http://localhost:3000",
                "http://localhost:5173",
                "https://library-ufu.vercel.app"  // seu domínio Vercel
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
```

### SSL/TLS
O Neon **exige** SSL. A connection string já inclui `?sslmode=require`. Não remova esse parâmetro.

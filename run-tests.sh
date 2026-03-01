#!/bin/bash

# Cores para o terminal
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}=== 🚀 Iniciando bateria de testes em ambiente padronizado Docker ===${NC}"
echo "Isso garantirá que seu código passe no GitHub Actions!"
echo ""

# Roda os testes sequencialmente para garantir que ambos terminem e a gente capture o exit code de cada um
LOG_FILE=$(mktemp)

echo -e "\n${YELLOW}▶ Testando Frontend...${NC}"
docker compose -f docker-compose.test.yml up --build --exit-code-from frontend-test frontend-test | tee "$LOG_FILE"
FRONTEND_EXIT=${PIPESTATUS[0]}

echo -e "\n${YELLOW}▶ Testando Backend...${NC}"
docker compose -f docker-compose.test.yml up --build --exit-code-from backend-test backend-test | tee -a "$LOG_FILE"
BACKEND_EXIT=${PIPESTATUS[0]}

# Combina os exit codes
if [ $FRONTEND_EXIT -eq 0 ] && [ $BACKEND_EXIT -eq 0 ]; then
    EXIT_CODE=0
else
    EXIT_CODE=1
fi

echo ""
echo "================================================================="

if [ $EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}✅ SUCESSO ABSOLUTO!${NC}"
    echo "Frontend (Prettier/ESLint) e Backend (Spotless/JUnit/Flyway) passaram perfeitamente."
    echo "Você está pronto(a) para commitar e abrir o Pull Request."
else
    echo -e "${RED}❌ FALHA DETECTADA NOS TESTES OU LINTERS!${NC}"
    echo "================================================================="
    echo "Resumo dos Erros:"
    
    # Procura na saída do Frontend
    if grep -q "library-frontend-test exited with code" "$LOG_FILE"; then
        echo -e "\n${YELLOW}▶ Frontend (React/TypeScript):${NC}"
        if grep -iq "prettier" "$LOG_FILE" && ! grep -q "prettier --check .$" "$LOG_FILE"; then
            echo -e "  - ${RED}Erro de Formatação (Prettier)${NC}: Rode 'cd frontend && npm run format'"
        fi
        if grep -iq "eslint" "$LOG_FILE" && ! grep -q "npm run lint$" "$LOG_FILE"; then
            echo -e "  - ${RED}Erro de Linter (ESLint)${NC}: Verifique os logs acima para erros do ESLint"
        fi
    fi

    # Procura na saída do Backend
    if grep -q "library-backend-test exited with code" "$LOG_FILE"; then
        echo -e "\n${YELLOW}▶ Backend (Java/Spring):${NC}"
        if grep -iq "Spotless" "$LOG_FILE"; then
            echo -e "  - ${RED}Erro de Formatação (Spotless)${NC}: Rode 'cd backend && ./mvnw spotless:apply'"
        fi
        if grep -iq "COMPILATION ERROR" "$LOG_FILE"; then
            echo -e "  - ${RED}Erro de Compilação${NC}: O código Java não está compilando"
        fi
        if grep -iq "Failures: " "$LOG_FILE" || grep -iq "Errors: " "$LOG_FILE"; then
            echo -e "  - ${RED}Testes Falharam (JUnit)${NC}: Verifique os resultados dos testes acima"
        fi
        if grep -q "Arquivos de migração fora do padrão" "$LOG_FILE"; then
            echo -e "  - ${RED}Erro no Flyway${NC}: O nome do arquivo .sql não segue o padrão V<versão>__<Nome>.sql"
        fi
    fi
    
    echo -e "\n${BLUE}Dica:${NC} Role o terminal para cima para ver os detalhes exatos do erro."
fi

# Limpa o arquivo log temporário
rm -f "$LOG_FILE"

exit $EXIT_CODE

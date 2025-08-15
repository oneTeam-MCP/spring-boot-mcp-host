FROM eclipse-temurin:21-jre

ENV DEBIAN_FRONTEND=noninteractive

# Node.js LTS
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl ca-certificates gnupg \
 && curl -fsSL https://deb.nodesource.com/setup_lts.x | bash - \
 && apt-get install -y --no-install-recommends nodejs \
 && node -v && npm -v \
 && rm -rf /var/lib/apt/lists/*

# npm 전역 설치 안정화에 필요한 빌드 도구
RUN apt-get update && apt-get install -y --no-install-recommends \
      git python3 make g++ \
  && rm -rf /var/lib/apt/lists/*

# npm 설정
ENV npm_config_unsafe_perm=true \
    npm_config_audit=false \
    npm_config_fund=false \
    npm_config_python=/usr/bin/python3

# MCP 서버 패키지 전역 설치
RUN npm i -g --unsafe-perm --no-audit --no-fund \
    @modelcontextprotocol/server-brave-search \
    @modelcontextprotocol/server-filesystem \
    @smithery/cli \
    @hwruchan/chanmcp

# 설치 확인
RUN which mcp-server-brave-search && mcp-server-brave-search --version || true && \
    which mcp-server-filesystem && mcp-server-filesystem --version || true && \
    which smithery && smithery --version || true

WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

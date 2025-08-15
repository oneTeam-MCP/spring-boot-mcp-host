FROM eclipse-temurin:21-jre

# Node.js LTS
RUN apt-get update && apt-get install -y curl ca-certificates \
 && curl -fsSL https://deb.nodesource.com/setup_lts.x | bash - \
 && apt-get install -y nodejs \
 && rm -rf /var/lib/apt/lists/*

# npm 전역 설치 안정화에 필요한 빌드 도구
RUN apt-get update && apt-get install -y --no-install-recommends \
      git python3 make g++ \
  && rm -rf /var/lib/apt/lists/* \

# npm 설정
RUN npm config set unsafe-perm true \
 && npm config set audit false \
 && npm config set fund false \
 && npm config set python /usr/bin/python3

# MCP 서버 패키지 전역 설치
RUN npm install -g \
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
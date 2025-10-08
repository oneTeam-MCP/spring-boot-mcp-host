package oneTeam.mcp_client.config;

import io.modelcontextprotocol.client.McpSyncClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableScheduling
@Slf4j
public class McpHealthManager {

    private final List<McpSyncClient> mcpClients;
    private final Map<String, ClientHealth> healthMap = new ConcurrentHashMap<>();

    public McpHealthManager(List<McpSyncClient> mcpClients) {
        this.mcpClients = mcpClients;
        log.info("McpHealthManager 초기화 - 클라이언트 {}개", mcpClients.size());
    }

    @PostConstruct
    public void init() {
        for (int i = 0; i < mcpClients.size(); i++) {
            healthMap.put("client-" + i, new ClientHealth());
        }
    }

    @Scheduled(fixedRate = 120000, initialDelay = 30000)
    public void healthCheck() {
        for (int i = 0; i < mcpClients.size(); i++) {
            String clientId = "client-" + i;
            McpSyncClient client = mcpClients.get(i);
            ClientHealth health = healthMap.get(clientId);

            try {
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return client.listTools();
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }).get(10, TimeUnit.SECONDS);

                health.recordSuccess();
                log.debug("✓ Health OK: {}", clientId);

            } catch (Exception e) {
                health.recordFailure();
                log.warn("✗ Health FAIL: {} (연속 {}회) - {}",
                        clientId, health.consecutiveFailures, e.getMessage());

                if (health.consecutiveFailures >= 3) {
                    log.error("🚨 MCP 클라이언트 {} 장애 - 복구 불가능", clientId);
                }
            }
        }
    }

    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    public void healthReport() {
        log.info("========== MCP Health Report ==========");

        boolean allHealthy = true;
        for (Map.Entry<String, ClientHealth> entry : healthMap.entrySet()) {
            ClientHealth health = entry.getValue();
            String status = health.isHealthy() ? "정상" : "장애";

            log.info("{}: {} | 마지막 성공: {} | 연속실패: {}회",
                    entry.getKey(),
                    status,
                    health.lastSuccessTime != null ?
                            Duration.between(health.lastSuccessTime, LocalDateTime.now()).toMinutes() + "분 전" : "없음",
                    health.consecutiveFailures);

            if (!health.isHealthy()) {
                allHealthy = false;
            }
        }

        if (!allHealthy) {
            log.error("⚠️ 일부 MCP 클라이언트 장애 - 애플리케이션 재시작 필요");
        }
        log.info("======================================");
    }

    public boolean isAllHealthy() {
        return healthMap.values().stream().allMatch(ClientHealth::isHealthy);
    }

    private static class ClientHealth {
        LocalDateTime lastSuccessTime;
        int consecutiveFailures;

        void recordSuccess() {
            this.lastSuccessTime = LocalDateTime.now();
            this.consecutiveFailures = 0;
        }

        void recordFailure() {
            this.consecutiveFailures++;
        }

        boolean isHealthy() {
            return consecutiveFailures < 3;
        }
    }
}
package oneTeam.mcp_client.config;

import io.modelcontextprotocol.client.McpSyncClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Configuration
@EnableScheduling
@Slf4j
public class McpHealthManager {

    private final List<McpSyncClient> mcpClients;
    private final Map<String, ClientHealth> healthMap = new ConcurrentHashMap<>();
    private final AtomicBoolean isChecking = new AtomicBoolean(false);

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
        if (!isChecking.compareAndSet(false, true)) {
            log.debug("이전 health check 진행 중 - 스킵");
            return;
        }

        try {
            log.debug("=== MCP Health Check 시작 ===");

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
                    }).get(15, TimeUnit.SECONDS);

                    health.recordSuccess();
                    log.debug("✓ Health OK: {}", clientId);

                } catch (TimeoutException e) {
                    health.recordFailure();
                    log.warn("✗ Health TIMEOUT: {} (연속 {}회)",
                            clientId, health.consecutiveFailures);

                } catch (InterruptedException e) {
                    health.recordFailure();
                    log.warn("✗ Health INTERRUPTED: {} (연속 {}회)",
                            clientId, health.consecutiveFailures);
                    Thread.currentThread().interrupt();

                } catch (Exception e) {
                    health.recordFailure();
                    log.warn("✗ Health FAIL: {} (연속 {}회) - {}",
                            clientId, health.consecutiveFailures,
                            e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                }

                if (health.consecutiveFailures >= 3) {
                    log.error("🚨 MCP 클라이언트 {} 장애 ({}회 실패) - Docker 재시작 대기 중",
                            clientId, health.consecutiveFailures);
                }
            }

        } finally {
            isChecking.set(false);
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
            log.error("⚠️ MCP 장애 감지 → Actuator Health: DOWN → Docker 재시작 트리거 예상");
        }
        log.info("======================================");
    }

    public boolean isAllHealthy() {
        return healthMap.values().stream().allMatch(ClientHealth::isHealthy);
    }

    // HealthIndicator에서 사용할 상세 정보
    public Map<String, Object> getHealthDetails() {
        Map<String, Object> details = new HashMap<>();

        for (Map.Entry<String, ClientHealth> entry : healthMap.entrySet()) {
            ClientHealth health = entry.getValue();
            Map<String, Object> clientDetail = new HashMap<>();
            clientDetail.put("healthy", health.isHealthy());
            clientDetail.put("consecutiveFailures", health.consecutiveFailures);
            clientDetail.put("lastSuccess", health.lastSuccessTime != null ?
                    health.lastSuccessTime.toString() : "없음");

            details.put(entry.getKey(), clientDetail);
        }

        return details;
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
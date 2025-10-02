package oneTeam.mcp_client.config;

import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableScheduling
@Slf4j
public class McpHealthManager {

    private final List<McpSyncClient> mcpClients;
    private final Map<String, LocalDateTime> lastSuccessTime = new ConcurrentHashMap<>();

    public McpHealthManager(List<McpSyncClient> mcpClients) {
        this.mcpClients = mcpClients;
        log.info("McpHealthManager 초기화 - 클라이언트 {}개 발견", mcpClients.size());
    }

    @Scheduled(fixedRate = 30000, initialDelay = 10000)
    public void keepAlive() {
        log.info("=== Keep-Alive 시작 (클라이언트 {}개) ===", mcpClients.size());

        for (int i = 0; i < mcpClients.size(); i++) {
            McpSyncClient client = mcpClients.get(i);
            String clientId = "client-" + i;

            try {
                client.listTools();
                lastSuccessTime.put(clientId, LocalDateTime.now());
                log.info("MCP keep-alive OK: {}", clientId);

            } catch (Exception e) {
                log.error("MCP keep-alive failed for {}: {}", clientId, e.getMessage());

                LocalDateTime lastSuccess = lastSuccessTime.get(clientId);
                if (lastSuccess == null ||
                        Duration.between(lastSuccess, LocalDateTime.now()).toMinutes() >= 2) {

                    log.error("MCP client {} unhealthy for 2+ minutes, forcing restart", clientId);
                    forceRestartMcpProcess(client);
                }
            }
        }

        log.info("=== Keep-Alive 완료 ===");
    }

    private void forceRestartMcpProcess(McpSyncClient client) {
        try {
            log.info("Restarting MCP client...");
            if (client instanceof AutoCloseable) {
                ((AutoCloseable) client).close();
            }
            Thread.sleep(3000);
            log.info("MCP client restart completed");
        } catch (Exception e) {
            log.error("Failed to restart MCP process", e);
        }
    }

    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    public void healthReport() {
        log.info("=== MCP Health Report ===");
        for (int i = 0; i < mcpClients.size(); i++) {
            String clientId = "client-" + i;
            LocalDateTime lastSuccess = lastSuccessTime.get(clientId);

            if (lastSuccess != null) {
                long minutesAgo = Duration.between(lastSuccess, LocalDateTime.now()).toMinutes();
                log.info("{}: Last success {} minutes ago", clientId, minutesAgo);
            } else {
                log.warn("{}: Never succeeded", clientId);
            }
        }
    }
}
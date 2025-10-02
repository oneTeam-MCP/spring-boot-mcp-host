package oneTeam.mcp_client.config;

import io.modelcontextprotocol.client.McpSyncClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class McpHealthManager {

    private final ConfigurableApplicationContext applicationContext;
    private final Map<String, LocalDateTime> lastSuccessTime = new ConcurrentHashMap<>();

    /**
     * 30초마다 keep-alive 및 헬스체크
     */
    @Scheduled(fixedRate = 30000, initialDelay = 10000)
    public void keepAlive() {
        log.info("=== Keep-Alive 시작 ===");

        Map<String, McpSyncClient> clients =
                applicationContext.getBeansOfType(McpSyncClient.class);

        log.info("찾은 클라이언트 수: {}", clients.size());
        log.info("클라이언트 목록: {}", clients.keySet());

        for (Map.Entry<String, McpSyncClient> entry : clients.entrySet()) {
            String beanName = entry.getKey();
            McpSyncClient client = entry.getValue();

            try {
                client.listTools();
                lastSuccessTime.put(beanName, LocalDateTime.now());
                log.info("MCP keep-alive OK: {}", beanName);

            } catch (Exception e) {
                log.error("MCP keep-alive failed for {}: {}", beanName, e.getMessage());

                // 마지막 성공 이후 2분 이상 실패면 재시작
                LocalDateTime lastSuccess = lastSuccessTime.get(beanName);
                if (lastSuccess == null ||
                        Duration.between(lastSuccess, LocalDateTime.now()).toMinutes() >= 2) {

                    log.error("MCP client {} unhealthy for 2+ minutes, forcing restart", beanName);
                    forceRestartMcpProcess(client);
                }
            }
        }
    }

    /**
     * MCP 프로세스 강제 재시작
     */
    private void forceRestartMcpProcess(McpSyncClient client) {
        try {
            // 기존 연결 종료
            if (client instanceof AutoCloseable) {
                ((AutoCloseable) client).close();
                log.info("Closed existing MCP client");
            }

            Thread.sleep(3000);

            log.info("MCP client restart initiated, will reconnect on next use");

        } catch (Exception e) {
            log.error("Failed to restart MCP process", e);
        }
    }

    /**
     * 5분마다 전체 상태 체크 및 리포트
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    public void healthReport() {
        Map<String, McpSyncClient> clients =
                applicationContext.getBeansOfType(McpSyncClient.class);

        log.info("=== MCP Health Report ===");
        for (String beanName : clients.keySet()) {
            LocalDateTime lastSuccess = lastSuccessTime.get(beanName);
            if (lastSuccess != null) {
                long minutesAgo = Duration.between(lastSuccess, LocalDateTime.now()).toMinutes();
                log.info("{}: Last success {} minutes ago", beanName, minutesAgo);
            } else {
                log.warn("{}: Never succeeded", beanName);
            }
        }
    }
}

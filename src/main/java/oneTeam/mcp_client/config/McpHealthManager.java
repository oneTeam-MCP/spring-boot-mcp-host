package oneTeam.mcp_client.config;

import io.modelcontextprotocol.client.McpSyncClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@Configuration
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class McpHealthManager {

    private final List<McpSyncClient> mcpClients;

    /**
     * 30초마다 keep-alive로 연결 유지
     */
    @Scheduled(fixedRate = 30000, initialDelay = 10000)
    public void keepAlive() {
        for (McpSyncClient client : mcpClients) {
            try {
                client.listTools();
                log.trace("MCP keep-alive OK");
            } catch (Exception e) {
                log.warn("MCP keep-alive failed: {}", e.getMessage());
                restartClient(client);
            }
        }
    }

    /**
     * 클라이언트 재시작
     */
    private void restartClient(McpSyncClient client) {
        try {
            log.info("Restarting MCP client...");
            if (client instanceof AutoCloseable) {
                ((AutoCloseable) client).close();
            }
            Thread.sleep(2000);
        } catch (Exception e) {
            log.error("Failed to restart MCP client", e);
        }
    }
}

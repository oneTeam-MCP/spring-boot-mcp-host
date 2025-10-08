package oneTeam.mcp_client.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class McpHealthIndicator implements HealthIndicator {

    private final McpHealthManager healthManager;

    public McpHealthIndicator(McpHealthManager healthManager) {
        this.healthManager = healthManager;
    }

    @Override
    public Health health() {
        boolean isHealthy = healthManager.isAllHealthy();

        if (isHealthy) {
            return Health.up()
                    .withDetail("mcp-clients", "모든 클라이언트 정상")
                    .build();
        } else {
            return Health.down()
                    .withDetail("mcp-clients", "일부 클라이언트 장애")
                    .withDetail("details", healthManager.getHealthDetails())
                    .build();
        }
    }
}

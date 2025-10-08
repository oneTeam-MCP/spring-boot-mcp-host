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
        if (healthManager.isAllHealthy()) {
            return Health.up()
                    .withDetail("mcp", "모든 클라이언트 정상")
                    .build();
        } else {
            return Health.down()
                    .withDetail("mcp", "일부 클라이언트 장애")
                    .build();
        }
    }
}

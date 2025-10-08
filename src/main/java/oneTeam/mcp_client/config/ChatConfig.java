package oneTeam.mcp_client.config;

import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Configuration
public class ChatConfig {

    private final AtomicReference<ToolCallback[]> toolCache = new AtomicReference<>();

    @Bean
    public ToolCallbackProvider mcpToolProvider(List<McpSyncClient> clients) {
        // 초기 로딩
        loadTools(clients);

        // 동적 갱신 지원
        return () -> {
            ToolCallback[] cached = toolCache.get();
            if (cached != null) {
                return cached;
            }
            return loadTools(clients);
        };
    }


    private ToolCallback[] loadTools(List<McpSyncClient> clients) {
        try {
            if (clients == null || clients.isEmpty()) {
                log.warn("사용 가능한 MCP 클라이언트 없음");
                return new ToolCallback[0];
            }

            SyncMcpToolCallbackProvider provider = new SyncMcpToolCallbackProvider(clients);
            ToolCallback[] tools = provider.getToolCallbacks();

            if (tools == null) {
                tools = new ToolCallback[0];
            }

            toolCache.set(tools);
            log.info("MCP 툴 {}개 로드 완료", tools.length);
            return tools;

        } catch (Exception e) {
            log.error("MCP 툴 로드 실패", e);
            return new ToolCallback[0];
        }
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel,
                                 ToolCallbackProvider mcpToolProvider) {
        return ChatClient.builder(chatModel)
                .defaultToolCallbacks(mcpToolProvider)
                .build();
    }
}

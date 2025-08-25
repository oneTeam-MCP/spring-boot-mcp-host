package oneTeam.mcp_client.config;

import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Configuration
public class ChatConfig {

    /** 최초 1회만 툴 콜백을 적재하고 캐싱 */
    @Bean
    public ToolCallbackProvider mcpToolProvider(List<McpSyncClient> clients) {
        SyncMcpToolCallbackProvider delegate = new SyncMcpToolCallbackProvider(clients);
        final AtomicReference<ToolCallback[]> cache = new AtomicReference<>();

        return () -> {
            ToolCallback[] cached = cache.get();
            if (cached != null) return cached;

            ToolCallback[] loaded = delegate.getToolCallbacks();
            if (loaded == null) loaded = new ToolCallback[0];
            cache.compareAndSet(null, loaded);
            return cache.get();
        };
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel, ToolCallbackProvider mcpToolProvider) {
        return ChatClient.builder(chatModel)
                .defaultToolCallbacks(mcpToolProvider)
                .build();
    }
}

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
import java.util.concurrent.atomic.AtomicBoolean;

@Configuration
public class ChatConfig {

    /** 최초 호출 시에만 MCP 클라이언트들을 initialize() */
    @Bean
    public ToolCallbackProvider lazyMcpToolProvider(List<McpSyncClient> clients) {
        AtomicBoolean inited = new AtomicBoolean(false);
        SyncMcpToolCallbackProvider delegate = new SyncMcpToolCallbackProvider(clients);

        return () -> {
            if (inited.compareAndSet(false, true)) {
                clients.forEach(McpSyncClient::initialize);
            }
            return delegate.getToolCallbacks();
        };
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel,
                                 ToolCallbackProvider lazyMcpToolProvider) {
        return ChatClient.builder(chatModel)
                .defaultToolCallbacks(lazyMcpToolProvider)
                .build();
    }
}

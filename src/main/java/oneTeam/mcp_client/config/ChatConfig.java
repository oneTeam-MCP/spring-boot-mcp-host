package oneTeam.mcp_client.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel,
                                 SyncMcpToolCallbackProvider tools) {
        return ChatClient.builder(chatModel)
                .defaultTools(tools.getToolCallbacks())
                .build();
    }
}

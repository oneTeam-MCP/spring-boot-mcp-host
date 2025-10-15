package oneTeam.mcp_client.agent.node;

import oneTeam.mcp_client.agent.ConversationState;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CallChatModel implements AsyncNodeAction<ConversationState> {
    private final ChatClient client;
    public CallChatModel(ChatClient client) { this.client = client; }

    @Override
    public CompletableFuture<Map<String,Object>> apply(ConversationState state) {
        String latestUserMessage = state.history().isEmpty() ? "" : state.history().getLast();
        return CompletableFuture.supplyAsync(() -> {
            String aiContent = client.prompt()
                    .user(latestUserMessage)
                    .call()
                    .content();

            return Map.of(ConversationState.HISTORY, aiContent);
        });
    }
}


package oneTeam.mcp_client.agent.node;

import oneTeam.mcp_client.agent.ConversationState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

public class AppendBotMessage implements NodeAction<ConversationState> {
    @Override
    public Map<String, Object> apply(ConversationState state) {
        String botResp = state.value("last_ai_response")
                .map(Object::toString)
                .orElse("");
        return Map.of(ConversationState.HISTORY, botResp);
    }
}


package oneTeam.mcp_client.agent.node;

import oneTeam.mcp_client.agent.ConversationState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

public class AppendUserMessage implements NodeAction<ConversationState> {
    @Override
    public Map<String, Object> apply(ConversationState state) {
        String lastUser = state.value("last_user_input")
                .map(Object::toString)
                .orElse("");
        return Map.of(ConversationState.HISTORY, lastUser);
    }
}

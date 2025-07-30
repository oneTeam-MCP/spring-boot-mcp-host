package oneTeam.mcp_client.agent;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConversationState extends AgentState {
    public static final String HISTORY = "history";
    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            HISTORY, Channels.appender(ArrayList::new)
    );

    public ConversationState(Map<String,Object> init) {
        super(init);
    }

    @SuppressWarnings("unchecked")
    public List<String> history() {
        return (List<String>) value(HISTORY).orElse(List.of());
    }
}


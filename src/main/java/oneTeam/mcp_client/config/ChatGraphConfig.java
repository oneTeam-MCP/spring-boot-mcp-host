package oneTeam.mcp_client.config;

import lombok.RequiredArgsConstructor;
import oneTeam.mcp_client.agent.ConversationState;
import oneTeam.mcp_client.agent.node.AppendUserMessage;
import oneTeam.mcp_client.agent.node.CallChatModel;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Configuration
@RequiredArgsConstructor
public class ChatGraphConfig {
    private final ChatClient chatClient;

    @Bean
    public CompiledGraph<ConversationState> chatAgentExecutor() throws GraphStateException {
        return new StateGraph<>(ConversationState.SCHEMA, ConversationState::new)
                .addNode("append_user", node_async(new AppendUserMessage()))
                .addNode("call_llm", new CallChatModel(chatClient))
                .addEdge(StateGraph.START, "append_user")
                .addEdge("append_user", "call_llm")
                .addEdge("call_llm", StateGraph.END)
                .compile();
    }
}
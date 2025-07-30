package oneTeam.mcp_client.service;

import lombok.RequiredArgsConstructor;
import oneTeam.mcp_client.agent.ConversationState;
import org.bsc.langgraph4j.CompiledGraph;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final CompiledGraph<ConversationState> chatAgentExecutor;

    // 현재 상태를 들고 있을 ConversationState 인스턴스
    private ConversationState state = new ConversationState(Map.of());

    public synchronized String chat(String question) {
        // 이전 history와 새 질문을 함께 초기 입력에 넣고
        Map<String,Object> init = new HashMap<>(Map.of(
                ConversationState.HISTORY, new ArrayList<>(state.history()),
                "last_user_input", question
        ));

        ConversationState newState = chatAgentExecutor
                .invoke(init)
                .orElseThrow(() -> new IllegalStateException("그래프 실행 실패"));

        this.state = newState;

        // history 마지막 응답 반환
        List<String> hist = newState.history();
        return hist.getLast();
    }
}

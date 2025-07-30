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
    public CompletableFuture<Map<String, Object>> apply(ConversationState state) {
        // 1) 상태에서 prompt 문자열 생성
        String prompt = String.join("\n", state.history());

        // 2) call() 을 supplyAsync 로 감싸 블록킹 호출을 비동기 처리
        return CompletableFuture.supplyAsync(() -> {
            // AI 모델에 요청 보내고, 응답 본문(content) 문자열로 얻기
            String aiContent = client.prompt()
                    .user(prompt)
                    .call()       // 동기 호출
                    .content();   // 문자열 추출

            // 3) 그래프 상태에 반영할 업데이트 반환
            return Map.<String, Object>of(
                    "last_ai_response", aiContent
            );
        });
    }
}


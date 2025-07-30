package oneTeam.mcp_client.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oneTeam.mcp_client.agent.ConversationState;
import org.bsc.langgraph4j.CompiledGraph;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final CompiledGraph<ConversationState> chatAgentExecutor;
    private final ChatClient chatClient;
    private ConversationState state = new ConversationState(
            Map.of(ConversationState.HISTORY, new ArrayList<String>())
    );

    /**
     * 동기 대화.
     * 이전 history와 질문을 그래프에 넘기고, 마지막 응답을 꺼내서 반환
     */
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

    /**
     * SSE 기반 스트리밍 대화.
     * SseEmitter를 통해 청크가 생성될 때마다 즉시 클라이언트로 푸시
     */
    public void streamTo(OutputStream os, String question) {
        synchronized (this) {
            state.history().add(question);
        }

        // prompt 구성
        String prompt;
        synchronized (this) {
            prompt = String.join("\n", state.history());
        }

        // 청크마다 즉시 flush
        for (String chunk : chatClient.prompt()
                .user(prompt)
                .stream()
                .content()
                .toIterable()) {
            String sse = "data: " + chunk + "\n";
            try {
                os.write(sse.getBytes(StandardCharsets.UTF_8));
                os.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
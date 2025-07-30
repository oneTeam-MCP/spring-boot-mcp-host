package oneTeam.mcp_client.service;

import lombok.RequiredArgsConstructor;
import oneTeam.mcp_client.agent.ConversationState;
import org.bsc.langgraph4j.CompiledGraph;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final CompiledGraph<ConversationState> chatAgentExecutor;
    private final ChatClient chatClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ConversationState state = new ConversationState(
            Map.of(ConversationState.HISTORY, new ArrayList<String>())
    );

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

    public void stream(String question, SseEmitter emitter) {
        // 사용자 입력 기록
        synchronized(this) {
            state.history().add(question);
        }
        // prompt 구성
        String prompt;
        synchronized(this) {
            prompt = String.join("\n", state.history());
        }

        // 비동기 작업
        executor.submit(() -> {
            try {
                Flux<String> flux = chatClient.prompt()
                        .user(prompt)
                        .stream()
                        .content(); // 청크 단위 텍스트

                flux.subscribe(
                        chunk -> {
                            synchronized (this) {
                                state.history().add(chunk);
                            }
                            try {
                                emitter.send(SseEmitter.event().data(chunk));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        emitter::completeWithError,
                        emitter::complete
                );
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        });
    }
}

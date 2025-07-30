package oneTeam.mcp_client.controller;

import lombok.RequiredArgsConstructor;
import oneTeam.mcp_client.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/chat")
    public Map<String,String> chat(@RequestBody Map<String,String> req) {
        return Map.of("answer", chatService.chat(req.get("question")));
    }

    @PostMapping(path = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody Map<String, String> req) {
        String question = req.get("question");
        SseEmitter emitter = new SseEmitter(30_000L);

        chatService.stream(question, emitter);
        return emitter;
    }
}

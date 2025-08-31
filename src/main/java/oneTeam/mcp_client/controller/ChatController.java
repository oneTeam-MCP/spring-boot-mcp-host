package oneTeam.mcp_client.controller;

import lombok.RequiredArgsConstructor;
import oneTeam.mcp_client.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

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
    public StreamingResponseBody stream(@RequestBody Map<String,String> req) {
        String question = req.get("question");
        return outputStream -> {
            chatService.streamTo(outputStream, question);
        };
    }
}

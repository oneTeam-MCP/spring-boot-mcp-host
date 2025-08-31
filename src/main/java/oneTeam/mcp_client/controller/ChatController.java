package oneTeam.mcp_client.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import oneTeam.mcp_client.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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
    public StreamingResponseBody stream(@RequestBody Map<String,String> req,
                                        HttpServletResponse resp) {

        final String question = req.getOrDefault("question", "");

        resp.setHeader("X-Accel-Buffering", "no");
        resp.setHeader("Cache-Control", "no-cache, no-transform");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());

        return (OutputStream os) -> {
            // 첫 바이트를 바로 보내 20s 대기 타임아웃 회피
            os.write(": ping\n\n".getBytes(StandardCharsets.UTF_8));
            os.flush();

            try {
                chatService.streamTo(os, question);
            } finally {
                os.write("event: done\n".getBytes(StandardCharsets.UTF_8));
                os.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        };
    }
}

package oneTeam.mcp_client.controller;

import lombok.RequiredArgsConstructor;
import oneTeam.mcp_client.service.ChatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService svc;

    @PostMapping("/chat")
    public Map<String,String> chat(@RequestBody Map<String,String> req) {
        return Map.of("answer", svc.chat(req.get("question")));
    }
}

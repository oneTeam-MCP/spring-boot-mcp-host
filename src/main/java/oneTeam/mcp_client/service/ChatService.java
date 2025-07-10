package oneTeam.mcp_client.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatClient client;

    public String chat(String question) {
        return client.prompt()
                .user(question)
                .call()
                .content();
    }
}

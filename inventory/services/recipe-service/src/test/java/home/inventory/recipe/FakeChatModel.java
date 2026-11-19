package home.inventory.recipe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/** Stands in for the AI provider: answers with prepared texts and records the prompts. */
public class FakeChatModel implements ChatModel {

    private final Deque<Object> answers = new ArrayDeque<>();
    private final List<String> prompts = new ArrayList<>();

    /** Next answer: a String (the model's text) or a RuntimeException to throw. */
    public synchronized void answer(Object answer) {
        answers.add(answer);
    }

    public synchronized List<String> prompts() {
        return List.copyOf(prompts);
    }

    @Override
    public synchronized ChatResponse call(Prompt prompt) {
        prompts.add(prompt.getContents());
        Object next = answers.isEmpty() ? "{}" : answers.poll();
        if (next instanceof RuntimeException e) {
            throw e;
        }
        return new ChatResponse(List.of(new Generation(new AssistantMessage((String) next))));
    }
}

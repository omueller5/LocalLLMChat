package org.example;

public class Main {

    static void main(String[] args) {
        System.out.println("=== Local Java Chat (llama.cpp + Qwen 0.5B) ===");

        javax.swing.SwingUtilities.invokeLater(() -> {
            Conversation convo = new Conversation();
            LlamaClient client = new LlamaClient();
            new ChatWindow(convo, client).show();
        });
    }
}

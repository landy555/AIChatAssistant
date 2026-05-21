import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ChatApp {

    private static final String API_KEY = System.getenv("SILICON_API_KEY");

    private static final String SYSTEM_PROMPT =
        "You are a friendly, professional AI assistant. Please answer in clear, concise Chinese."
            + " If the question is about programming, include practical examples when helpful.";

    public static void main(String[] args) {
        System.out.println("==================================");
        System.out.println("   AI Chat Assistant is running   ");
        System.out.println("        (Docker edition)          ");
        System.out.println("==================================");
        System.out.println("quit / exit  -> quit");
        System.out.println("clear        -> clear history");
        System.out.println("history      -> show history");
        System.out.println();

        if (API_KEY == null || API_KEY.isEmpty()) {
            System.out.println("Missing API key.");
            System.out.println("Set SILICON_API_KEY before starting the app.");
            System.out.println("You can also copy .env.example to .env for docker-compose.");
            return;
        }

        ClaudeClient client = new ClaudeClient(API_KEY);
        List<Map<String, String>> history = new ArrayList<>();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("You: ");
            if (!scanner.hasNextLine()) {
                break;
            }

            String userInput = scanner.nextLine().trim();
            if (userInput.isEmpty()) {
                continue;
            }

            String cmd = userInput.toLowerCase();
            if (cmd.equals("quit") || cmd.equals("exit")) {
                System.out.println("Bye.");
                break;
            }
            if (cmd.equals("clear")) {
                history.clear();
                System.out.println("History cleared.\n");
                continue;
            }
            if (cmd.equals("history")) {
                printHistory(history);
                continue;
            }

            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userInput);
            history.add(userMsg);

            try {
                System.out.print("AI: ");
                String reply = client.chat(history, SYSTEM_PROMPT);
                System.out.println(reply);
                System.out.println();

                Map<String, String> aiMsg = new HashMap<>();
                aiMsg.put("role", "assistant");
                aiMsg.put("content", reply);
                history.add(aiMsg);
            } catch (Exception e) {
                System.out.println("Request failed: " + e.getMessage());
                history.remove(history.size() - 1);
            }
        }
    }

    private static void printHistory(List<Map<String, String>> history) {
        if (history.isEmpty()) {
            System.out.println("No conversation history.\n");
            return;
        }

        System.out.println("--- History (" + history.size() / 2 + " rounds) ---");
        for (Map<String, String> msg : history) {
            String role = "user".equals(msg.get("role")) ? "You" : "AI";
            String content = msg.get("content");
            if (content.length() > 50) {
                content = content.substring(0, 50) + "...";
            }
            System.out.println(role + ": " + content);
        }
        System.out.println("-----------------------------\n");
    }
}

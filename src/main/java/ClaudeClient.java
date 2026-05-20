import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ClaudeClient {
    // 最大重试次数
    private static final int MAX_RETRY = 3;
    // 接口超时时间 毫秒
    private static final int TIME_OUT = 10000;

    public String chat(String apiKey, String prompt) {
        String jsonBody = "{\"model\":\"claude-3-sonnet-20240229\",\"max_tokens\":1024,\"messages\":[{\"role\":\"user\",\"content\":\"" + prompt + "\"}]}";

        // 重试逻辑
        for (int retry = 1; retry <= MAX_RETRY; retry++) {
            try {
                URL url = new URL("https://api.anthropic.com/v1/messages");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(TIME_OUT);
                conn.setReadTimeout(TIME_OUT);

                // 请求头
                conn.setRequestProperty("x-api-key", apiKey);
                conn.setRequestProperty("anthropic-version", "2023-06-01");
                conn.setRequestProperty("Content-Type", "application/json");

                // 发送请求体
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.getBytes("UTF-8");
                    os.write(input, 0, input.length);
                }

                // 读取返回结果
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }

                conn.disconnect();
                // 请求成功，直接返回
                return response.toString();

            } catch (IOException e) {
                System.out.println("第" + retry + "次请求失败：" + e.getMessage());
                if (retry == MAX_RETRY) {
                    System.out.println("已达到最大重试次数，请求彻底失败");
                    return "网络请求失败，请稍后再试";
                }
                // 重试前休眠1秒
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return "未知错误";
    }
}

package com.letmese.aikeyboard;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Minimal client for a FREE, keyless LLM endpoint.
 * Uses OpenCode Zen's free model endpoint (no API key needed).
 * Supports multiple "modes" (grammar, formal, casual, social, translate).
 */
public class AiClient {

    private static final String ENDPOINT =
            "https://opencode.ai/zen/v1/chat/completions";
    private static final String MODEL = "x-preview-f-free";

    public enum Mode {
        GRAMMAR("Fix grammar and spelling AND improve clarity. Reply ONLY with the corrected text."),
        FORMAL("Rewrite the following in a professional, formal tone. Reply ONLY with the rewritten text."),
        CASUAL("Rewrite the following in a relaxed, casual, friendly tone. Reply ONLY with the rewritten text."),
        SOCIAL("Turn the following into an engaging social media post with emojis and hashtags. Reply ONLY with the post."),
        TRANSLATE("Translate the following to English. Reply ONLY with the translation.");

        final String instruction;
        Mode(String instruction) { this.instruction = instruction; }
    }

    public static String complete(Mode mode, String text) throws Exception {
        String prompt = mode.instruction + "\n\n" + text;
        String body = "{"
                + "\"model\":\"" + MODEL + "\","
                + "\"messages\":[{\"role\":\"user\",\"content\":" + jsonEscape(prompt) + "}],"
                + "\"max_tokens\":640}"
                .replace("\"messages\"", "\"messages\""); // keep structure intact

        HttpURLConnection conn = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        if (code != 200) throw new Exception("HTTP " + code);

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return extractContent(sb.toString());
    }

    /** Pulls choices[0].message.content from the JSON response without external libs. */
    private static String extractContent(String json) {
        int idx = json.indexOf("\"content\"");
        if (idx < 0) return "";
        int start = json.indexOf('"', idx + 9);
        if (start < 0) return "";
        StringBuilder out = new StringBuilder();
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char n = json.charAt(++i);
                switch (n) {
                    case 'n': out.append('\n'); break;
                    case 't': out.append('\t'); break;
                    case '"': out.append('"'); break;
                    case '\\': out.append('\\'); break;
                    default: out.append(n);
                }
            } else if (c == '"') {
                break;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String jsonEscape(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}

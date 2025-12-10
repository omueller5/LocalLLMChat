package org.example;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LlamaClient {

    // TODO: change path if needed
    private static final String LLAMA_EXE =
            "C:\\\\Users\\\\Owen\\\\AppData\\\\Local\\\\Microsoft\\\\WinGet\\\\Packages\\\\ggml.llamacpp_Microsoft.WinGet.Source_8wekyb3d8bbwe\\\\llama-cli.exe";

    private static final String MODEL_PATH =
            "C:\\\\llama\\\\models\\\\qwen2.5-0.5b-instruct-q4_k_m.gguf";

    public String complete(String prompt) throws IOException, InterruptedException {
        // write prompt to temp file
        File tempPrompt = File.createTempFile("llama_prompt_", ".txt");
        try (FileWriter fw = new FileWriter(tempPrompt)) {
            fw.write(prompt);
        }

        List<String> command = new ArrayList<>();
        command.add(LLAMA_EXE);
        command.add("-m");
        command.add(MODEL_PATH);
        command.add("-no-cnv");
        command.add("--no-display-prompt");
        command.add("--ctx-size");
        command.add("900");
        command.add("--n-predict");
        command.add("128");
        command.add("--temp");
        command.add("0.7");
        command.add("-f");
        command.add(tempPrompt.getAbsolutePath());

        System.out.println("[DEBUG] Running command:");
        System.out.println(String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder raw = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                raw.append(line).append(System.lineSeparator());
            }
        }

        int exitCode = process.waitFor();
        System.out.println("[DEBUG] llama-cli finished with code " + exitCode);

        tempPrompt.delete();

        return cleanOutput(raw.toString());
    }

    private String cleanOutput(String rawText) {
        String out = rawText.replace("\r", "").trim();
        if (out.isEmpty()) return "";

        // drop everything before last banner block if present
        String banner = "***************************";
        int bIdx = out.lastIndexOf(banner);
        if (bIdx != -1) {
            out = out.substring(bIdx + banner.length()).trim();
        }

        StringBuilder cleaned = new StringBuilder();
        for (String line : out.split("\n")) {
            String t = line.trim();
            if (t.isEmpty()) continue;

            if (t.startsWith("sampler") ||
                    t.startsWith("llama_") ||
                    t.startsWith("common_") ||
                    t.startsWith("system_info") ||
                    t.startsWith("generate:") ||
                    t.startsWith("main:") ||
                    t.startsWith("ggml_") ||
                    t.startsWith("IMPORTANT:")) {
                continue;
            }
            cleaned.append(t).append(" ");
        }

        String result = cleaned.toString().trim();

        // 🔹 Strip model end markers like [end of text]
        result = result.replace("[end of text]", "").trim();

        return result;
    }

}

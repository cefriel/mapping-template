package it.cefriel.template;

import nu.xom.Builder;
import nu.xom.ParsingException;
import nu.xom.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {

    // Remove Prefix
    public String rp(String s) {
        if (s!=null)
            if (s.contains("#")) {
                String[] split = s.split("#");
                return split[1];
            }
        return s;
    }
    // Remove Prefix through regex
    public String sp(String s, String regex) {
        if (s!=null) {
            String[] split = s.split(regex);
            if (split.length > 1)
                return split[1];
        }
        return s;
    }

    // Get timestamp
    public String getTimestamp() {
        DateTimeFormatter formatterOutput = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return LocalDateTime.now().format(formatterOutput);
    }

    public static String format(String xml) throws ParsingException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Serializer serializer = new Serializer(out);
        serializer.setIndent(4);  // or whatever you like
        serializer.write(new Builder().build(xml, ""));
        return out.toString("UTF-8");
    }

    public Map<String, Map<String, String>> getMap(List<Map<String, String>> results, String key) {
        Map<String, Map<String, String>> results_map = new HashMap<>();
        for (Map<String,String> result : results)
            results_map.put(result.get(key), result);
        return results_map;
    }

    public Map<String, List<Map<String, String>>> getListMap(List<Map<String, String>> results, String key) {
        Map<String, List<Map<String, String>>> results_map = new HashMap<>();
        for (Map<String,String> result : results)
            results_map.put(result.get(key), new ArrayList<>());
        for (Map<String,String> result : results)
            results_map.get(result.get(key)).add(result);
        return results_map;
    }

    public String trimTemplate(String templatePath) throws IOException {
        String newTemplatePath = templatePath + ".tmp.vm";
        List<String> newLines = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(templatePath), StandardCharsets.UTF_8)) {
            newLines.add(line.trim().replace("\n", "").replace("\r",""));
        }
        String result = String.join(" ", newLines);
        try (PrintWriter out = new PrintWriter(newTemplatePath)) {
            out.println(result);
        }
        return newTemplatePath;
    }
}

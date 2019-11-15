package it.cefriel.template;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;
import nu.xom.Serializer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Utils {

    public static DateTimeFormatter formatterOutput = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private String prefix;

    // Remove Prefix
    public String rp(String s) {
        if (s!=null && prefix!=null)
            if (s.contains(prefix)) {
                return s.replace(prefix, "");
            }
        return s;
    }

    public void setPrefix(String prefix) {
        if (prefix != null)
            this.prefix = prefix;
    }

    // Get string after occurence of specified substring
    public String sp(String s, String substring) {
        if (s!=null) {
            return s.substring(s.indexOf(substring) + 1);
        }
        return s;
    }

    // Escape URL
    public String eu(String url) {
        if (url != null)
            return url.replaceAll("/", "\\\\/");
        return null;
    }


    // Get timestamp
    public String getTimestamp() {
        return LocalDateTime.now().format(formatterOutput);
    }

    // Get formatted date
    public String getFormattedDate(int year, int month, int dayOfMonth, int hour, int minute) {
        LocalDateTime dt = LocalDateTime.of(0,0,0,0,0);
        return dt.format(formatterOutput);
    }

    // Format GTFS date
    public String formatGTFSDate(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        DateTimeFormatter formatterOutput = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return LocalDate.parse(dateString, formatter).format(formatterOutput);
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

    public void writeToFile(String text, String path, boolean formatXml) throws ParsingException, IOException {
        if (formatXml) {
            Document doc = new Builder().build(text, "");
            writeToFileXml(doc, path);
        } else {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));
            out.write(text);
        }
    }

    public void writeToFileXml(Document doc, String path) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(path)));
        Serializer serializer = new Serializer(bos, "ISO-8859-1");
        serializer.setIndent(4);
        serializer.setMaxLength(0);
        serializer.write(doc);
        bos.close();
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

    public Map<String,String> parseMap(String filePath) throws IOException {
        Path path = FileSystems.getDefault().getPath(filePath);
        Map<String, String> mapFromFile = Files.lines(path)
                .filter(s -> s.matches("^\\w+:.+"))
                .collect(Collectors.toMap(k -> k.split(":")[0], v -> v.substring(v.indexOf(":") + 1)));
        return mapFromFile;
    }

    public Map<String,String> parseCsvMap(String filePath) throws IOException {
        Path path = FileSystems.getDefault().getPath(filePath);
        List<String[]> fromCSV = Files.lines(path)
                .map(s -> s.split(","))
                .collect(Collectors.toList());
        Map<String,String> mapFromFile = new HashMap<>();
        String[] keys = fromCSV.get(0);
        String[] values = fromCSV.get(1);
        for(int i = 0; i < keys.length; i++)
            mapFromFile.put(keys[i], values[i]);
        return mapFromFile;
    }
}

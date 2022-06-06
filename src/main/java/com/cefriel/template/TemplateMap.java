package com.cefriel.template;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TemplateMap extends HashMap<String, String> {

    public void parseMap(String filePath, boolean isCsv) throws IOException {
        Path path = FileSystems.getDefault().getPath(filePath);
        Stream<String> lines = Files.lines(path);
        if (isCsv)
            putAll(parseCsvMap(lines));
        else
            putAll(parseMap(lines));
    }

    public void parseMap(InputStream is, boolean isCsv) throws IOException {
        Stream<String> lines = new BufferedReader(new InputStreamReader(is,
                StandardCharsets.UTF_8)).lines();
        if (isCsv)
            putAll(parseCsvMap(lines));
        else
            putAll(parseMap(lines));
    }

    private Map<String,String> parseMap(Stream<String> lines) {
        return lines.filter(s -> s.matches("^(.+?):.+"))
                .collect(Collectors.toMap(k -> k.split(":")[0], v -> v.substring(v.indexOf(":") + 1)));
    }

    private Map<String,String> parseCsvMap(Stream<String> lines) {
        List<String[]> fromCSV = lines
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

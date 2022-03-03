package com.cefriel.lowerer;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MapConfigurator {

    private String keyValuePairsPath;
    private String keyValueCsvPath;

    private Map<String, String> map;

    public MapConfigurator() {
        map = new HashMap<>();
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

    public String getKeyValuePairsPath() {
        return keyValuePairsPath;
    }

    public void setKeyValuePairsPath(String keyValuePairsPath) throws IOException {
        if(this.keyValuePairsPath !=  null)
            map.keySet().removeAll(parseMap(this.keyValuePairsPath).keySet());

        this.keyValuePairsPath = keyValuePairsPath;

        if(keyValuePairsPath !=  null)
            map.putAll(parseMap(keyValuePairsPath));
    }

    public String getKeyValueCsvPath() {
        return keyValueCsvPath;
    }

    public void setKeyValueCsvPath(String keyValueCsvPath) throws IOException {
        if(this.keyValueCsvPath !=  null)
            map.keySet().removeAll(parseCsvMap(this.keyValueCsvPath).keySet());

        this.keyValueCsvPath = keyValueCsvPath;

        if(keyValueCsvPath !=  null)
            map.putAll(parseCsvMap(keyValueCsvPath));
    }

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> map) {
        this.map = map;
    }

}

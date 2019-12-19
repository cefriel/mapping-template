package com.cefriel.kt.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoweringUtils {

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
            return s.substring(s.indexOf(substring) + substring.length());
        }
        return s;
    }

    // Escape URL
    public String eu(String url) {
        if (url != null)
            return url.replaceAll("/", "\\\\/");
        return null;
    }

    public String hash(String s) {
        if (s == null)
            return null;
        return Integer.toString(Math.abs(s.hashCode()));
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

}

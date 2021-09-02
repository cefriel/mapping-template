/*
 * Copyright (c) 2019-2021 Cefriel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cefriel.utils;

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

    // Get string before occurence of specified substring
    public String p(String s, String substring) {
        if (s!=null) {
            return s.substring(0, s.indexOf(substring));
        }
        return s;
    }

    public String replace(String s, String regex, String replacement) {
        if (s != null)
            return s.replaceAll(regex, replacement);
        return null;
    }

    // Escape URL
    public String eu(String url) {
        if (url != null)
            return url.replaceAll("/", "\\\\/");
        return null;
    }

    public String newline() {
        return "\n";
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

    public boolean checkString(String s){
        return s != null && !s.trim().isEmpty();
    }

    public <T> boolean checkList(List<T> l){
        return l != null && !l.isEmpty();
    }

    public <T> boolean checkList(List<T> l, T o){
        return checkList(l) && l.contains(o);
    }

    public <K,V> boolean checkMap(Map<K,V> m){
        return m != null && !m.isEmpty();
    }

    public <K,V> boolean checkMap(Map<K, V> m, K key){
        return checkMap(m) && m.containsKey(key);
    }

    public <K,V> List<V> getListMapValue(Map<K, List<V>> listMap, K key){
        if (checkMap(listMap, key))
            return listMap.get(key);
        else
            return new ArrayList<>();
    }

}

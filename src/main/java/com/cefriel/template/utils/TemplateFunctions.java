/*
 * Copyright (c) 2019-2022 Cefriel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.cefriel.template.utils;

import com.cefriel.template.io.Reader;
import com.cefriel.template.io.csv.CSVReader;
import com.cefriel.template.io.json.JSONReader;
import com.cefriel.template.io.rdf.RDFReader;
import com.cefriel.template.io.xml.XMLReader;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateFunctions {

    private String prefix;

    /**
     * If a prefix is set, removes it from the parameter {@code String s}. If a prefix is not set,
     * or the prefix is not contained in the given string it returns the string as it is.
     * @param s String representing an IRI
     * @return String value
     */
    public String rp(String s) {
        if (s!=null && prefix!=null)
            if (s.contains(prefix)) {
                return s.replace(prefix, "");
            }
        return s;
    }

    /**
     * Set the prefix used by the {@link #rp(String s)} function
     * @param prefix String prefix
     */
    public void setPrefix(String prefix) {
        if (prefix != null)
            this.prefix = prefix;
    }

    /**
     * Returns the substring of the parameter {@code String s} after the first occurrence
     * of the parameter {@code String substring}.
     * @param s String to be modified
     * @param substring Pattern to get the substring
     * @return Suffix substring
     */
    public String sp(String s, String substring) {
        if (s!=null) {
            return s.substring(s.indexOf(substring) + substring.length());
        }
        return s;
    }

    /**
     * Returns the substring of the parameter {@code String s} before the first occurrence
     * of the parameter {@code String substring}.
     * @param s String to be modified
     * @param substring Pattern to get the substring
     * @return Prefix substring
     */
    public String p(String s, String substring) {
        if (s!=null) {
            return s.substring(0, s.indexOf(substring));
        }
        return s;
    }

    /**
     * Returns a string replacing all the occurrences of the regex with the replacement provided.
     * @param s String to be modified
     * @param regex Regex to be matched
     * @param replacement String to be used as replacement
     * @return Modified string
     */
    public String replace(String s, String regex, String replacement) {
        if (s != null)
            return s.replaceAll(regex, replacement);
        return null;
    }

    /**
     * Returns a new line char.
     * @return A new line char.
     */
    public String newline() {
        return "\n";
    }

    /**
     * Returns a string representing the hash of the parameter {@code String s}.
     * @param s String to be hashed.
     * @return String representing the computed hash.
     */
    public String hash(String s) {
        if (s == null)
            return null;
        return Integer.toString(s.hashCode());
    }

    /**
     * Returns {@code true} if the string is not null and not an empty string.
     * @param s String to be checked
     * @return boolean
     */
    public boolean checkString(String s){
        return s != null && !s.trim().isEmpty();
    }

    /**
     * Creates a support data structure ({@link java.util.HashMap}) to access query results faster. Builds a map associating
     * a single row with its value w.r.t a specified column ({@code key} parameter). The assumption is
     * that for each row the value for the given column is unique, otherwise, the result will be incomplete.
     * @param results The result of a SPARL query
     * @param key The variable to be used to build the map
     * @return Map associating to each value of the variable {@code key} a row of the query results.
     */
    public Map<String, Map<String, String>> getMap(List<Map<String, String>> results, String key) {
        Map<String, Map<String, String>> results_map = new HashMap<>();
        if (results != null) {
            for (Map<String, String> result : results)
                results_map.put(result.get(key), result);
        }
        return results_map;
    }

    /**
     * Creates a support data structure ({@link java.util.HashMap}) to access query results faster. Builds a map associating
     * a value with all rows having that as value for a specified column ({@code key} parameter).
     * @param results The result of a SPARL query
     * @param key The variable to be used to build the map
     * @return Map associating to each value of the variable {@code key} a {@link java.util.List} of rows in the query results.
     */
    public Map<String, List<Map<String, String>>> getListMap(List<Map<String, String>> results, String key) {
        Map<String, List<Map<String, String>>> results_map = new HashMap<>();
        if (results != null) {
            for (Map<String, String> result : results)
                results_map.put(result.get(key), new ArrayList<>());
            for (Map<String, String> result : results)
                results_map.get(result.get(key)).add(result);
        }
        return results_map;
    }

    public List<Map<String,String>> splitColumn(List<Map<String, String>> df, String columnName, String regex) {
        for (Map<String, String> row: df) {
            String[] values = row.get(columnName).split(regex);
            Map<String, String> x = new HashMap<>();
            for(int i=0; i < values.length; i++){
                String key = columnName + (i + 1);
                row.put(key, values[i]);
            }
            row.remove(columnName);
        }
        return df;
    }

    /**
     * Returns {@code true} if {@code l} is not null and not an empty string.
     * @param l List to be checked
     * @param <T> Type of objects contained in the list
     * @return boolean
     */
    public <T> boolean checkList(List<T> l){
        return l != null && !l.isEmpty();
    }

    /**
     * Returns {@code true} if {@code l} is not null, not empty and contains {@code o}.
     * @param l List to be checked
     * @param o Value in the list to be checked
     * @param <T> Type of objects contained in the list
     * @return boolean
     */
    public <T> boolean checkList(List<T> l, T o){
        return checkList(l) && l.contains(o);
    }

    /**
     * Returns {@code true} if {@code m} is not null and not empty.
     * @param m Map to be checked
     * @param <K> Type for keys in the map
     * @param <V> Type for values in the map
     * @return boolean
     */
    public <K,V> boolean checkMap(Map<K,V> m){
        return m != null && !m.isEmpty();
    }

    /**
     * Returns {@code true} if {@code m} is not null, not empty and contains {@code key} as key.
     * @param m Map to be checked
     * @param key Key to be checked
     * @param <K> Type for keys in the map
     * @param <V> Type for values in the map
     * @return boolean
     */
    public <K,V> boolean checkMap(Map<K, V> m, K key){
        return checkMap(m) && m.containsKey(key);
    }

    /**
     * If {@link #checkMap(Map, Object)} is {@code true} returns the value for {@code key} in {@code map},
     * otherwise returns {@code null}.
     * @param map Map to be accessed
     * @param key Key to be used to access the map
     * @param <K> Type for keys in the map
     * @param <V> Type for values in the map
     * @return The value of type {@code V} associated with {@code key} in the map
     */
    public <K,V> V getMapValue(Map<K, V> map, K key){
        if (checkMap(map, key))
            return map.get(key);
        else
            return null;
    }

    /**
     * If {@link #checkMap(Map, Object)} is {@code true} returns the list for {@code key} in {@code map},
     * otherwise returns an empty {@link java.util.List}.
     * @param listMap Map to be accessed
     * @param key Key to be used to access the map
     * @param <K> Type for keys in the map
     * @param <V> Type for lists used as value in the map
     * @return The list of type {@code V} associated with {@code key} in the map
     */
    public <K,V> List<V> getListMapValue(Map<K, List<V>> listMap, K key){
        if (checkMap(listMap, key))
            return listMap.get(key);
        else
            return new ArrayList<>();
    }

    /**
     * Reads given file as a string.
     * @param fileName path to the file
     * @return the file's contents
     * @throws IOException if read fails for any reason
     */
    public String getFileAsString(String fileName) throws IOException {
        String content = Files.readString(Paths.get(fileName));
        return content;
    }

    /**
     * Get a RDFReader to query the RDF content of the provided file.
     * The RDF format is inferred from the extension of the file (default: Turtle).
     * @param fileName The file path for the RDF file.
     * @return An RDFReader
     * @throws Exception
     */
    public RDFReader getRDFReaderFromFile(String fileName) throws Exception {
        RDFReader rdfReader = new RDFReader();
        if (fileName != null)
            if ((new File(fileName)).exists()) {
                RDFFormat format = Rio.getParserFormatForFileName(fileName).orElse(RDFFormat.TURTLE);
                rdfReader.addFile(fileName, format);
            }
            else
                throw new IllegalArgumentException("FILE: " + fileName + " FOR RDFREADER DOES NOT EXIST");

        return rdfReader;
    }

    /**
     * Get a RDFReader to query the RDF content of the provided string.
     * The RDF format can be provided specifying the MIME type (default: text/turtle).
     * @param s The RDF string.
     * @param MIMEType The MIME type for the RDF format.
     * @return An RDFReader
     * @throws Exception
     */
    public RDFReader getRDFReaderFromString(String s, String MIMEType) throws Exception {
        Repository repo = new SailRepository(new MemoryStore());
        RDFReader rdfReader = new RDFReader(repo);
        RDFFormat rdfFormat = Rio.getParserFormatForMIMEType(MIMEType).orElse(RDFFormat.TURTLE);
        rdfReader.addString(s, rdfFormat);
        return rdfReader;
    }

    /**
     * Get a XMLReader to query the XML content of the provided file.
     * @param fileName The file path for the XML file.
     * @return An XMLReader
     * @throws Exception
     */
    public XMLReader getXMLReaderFromFile(String fileName) throws Exception {
        File xmlDocument = new File(fileName);
        return new XMLReader(xmlDocument);
    }

    /**
     * Get a XMLReader to query the XML content of the provided string.
     * @param s The XML string.
     * @return An XMLReader
     * @throws Exception
     */
    public XMLReader getXMLReaderFromString(String s) throws Exception {
        if (s != null) {
            return new XMLReader(s);
        }
        return new XMLReader("");
    }

    /**
     * Get a JSONReader to query the JSON content of the provided file.
     * @param fileName The file path for the JSON file.
     * @return A JSONReader
     * @throws Exception
     */
    public JSONReader getJSONReaderFromFile(String fileName) throws Exception {
        File jsonDocument = new File(fileName);
        return new JSONReader(jsonDocument);
    }

    /**
     * Get a JSONReader to query the JSON content of the provided string.
     * @param s The JSON string.
     * @return An JSONReader
     * @throws Exception
     */
    public JSONReader getJSONReaderFromString(String s) throws Exception {
        if (s != null) {
            return new JSONReader(s);
        }
        return new JSONReader("");
    }

    public CSVReader getCSVReaderFromFile(String fileName) throws IOException {
        File f = new File(fileName);
        return new CSVReader(f);
    }

    public CSVReader getCSVReaderFromString(String s) throws Exception {
        if (s != null) {
            return new CSVReader(s);
        }
        return new CSVReader("");
    }

    /**
     * Merge two lists of results from queries on a {@link Reader}.
     * @param results Results of a query
     * @param otherResults Results to be merged
     * @return Merged results
     */
    public List<Map<String,String>> mergeResults(List<Map<String,String>> results, List<Map<String,String>> otherResults) {
        if (checkList(results)) {
            if (checkList(otherResults))
                results.addAll(otherResults);
            return results;
        } else if(checkList(otherResults))
            return otherResults;
        else return new ArrayList<>();
    }
}

/*
 * Copyright (c) 2019-2023 Cefriel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cefriel.template.utils;

import com.cefriel.template.io.Reader;
import com.cefriel.template.io.csv.CSVReader;
import com.cefriel.template.io.json.JSONReader;
import com.cefriel.template.io.rdf.RDFReader;
import com.cefriel.template.io.sql.SQLReader;
import com.cefriel.template.io.xml.XMLReader;
import org.eclipse.rdf4j.common.net.ParsedIRI;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TemplateFunctions {

    private String prefix;

    /**
     * If a prefix is set, removes it from the parameter {@code String s}. If a prefix is not set,
     * or the prefix is not contained in the given string it returns the string as it is.
     *
     * @param s String representing an IRI
     * @return String value
     */
    public String rp(String s) {
        if (s != null && prefix != null) if (s.contains(prefix)) {
            return s.replace(prefix, "");
        }
        return s;
    }

    /**
     * Set the prefix used by the {@link #rp(String s)} function
     *
     * @param prefix String prefix
     */
    public void setPrefix(String prefix) {
        if (prefix != null) this.prefix = prefix;
    }

    /**
     * Returns the substring of the parameter {@code String s} after the first occurrence
     * of the parameter {@code String substring}.
     *
     * @param s         String to be modified
     * @param substring Pattern to get the substring
     * @return Suffix substring
     */
    public String sp(String s, String substring) {
        if (s != null) {
            return s.substring(s.indexOf(substring) + substring.length());
        }
        return s;
    }

    /**
     * Returns the substring of the parameter {@code String s} before the first occurrence
     * of the parameter {@code String substring}.
     *
     * @param s         String to be modified
     * @param substring Pattern to get the substring
     * @return Prefix substring
     */
    public String p(String s, String substring) {
        if (s != null) {
            return s.substring(0, s.indexOf(substring));
        }
        return s;
    }

    /**
     * Returns a string replacing all the occurrences of the regex with the replacement provided.
     *
     * @param s           String to be modified
     * @param regex       Regex to be matched
     * @param replacement String to be used as replacement
     * @return Modified string
     */
    public String replace(String s, String regex, String replacement) {
        if (s != null) return s.replaceAll(regex, replacement);
        return null;
    }

    /**
     * Returns a new line char.
     *
     * @return A new line char.
     */
    public String newline() {
        return "\n";
    }

    /**
     * Returns a string representing the hash of the parameter {@code String s}.
     *
     * @param s String to be hashed.
     * @return String representing the computed hash.
     */
    public String hash(String s) {
        if (s == null) return null;
        return Integer.toString(s.hashCode());
    }

    /**
     * Returns {@code true} if the string is not null and not an empty string.
     *
     * @param s String to be checked
     * @return boolean
     */
    public static boolean checkString(String s){
        return s != null && !s.trim().isEmpty();
    }

    /**
     * Returns {@code true} if all the strings in the list are not null and not an empty string.
     * @param s List of string to be checked
     * @return boolean
     */
    public static boolean checkStrings(List<String> s){
        return s != null && s.stream().allMatch(TemplateFunctions::checkString);
    }

    /**
     * Creates a support data structure ({@link java.util.HashMap}) to access query results faster. Builds a map associating
     * a single row with its value w.r.t a specified column ({@code key} parameter). The assumption is
     * that for each row the value for the given column is unique, otherwise, the result will be incomplete.
     *
     * @param results The result of a SPARL query
     * @param key     The variable to be used to build the map
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
     *
     * @param results The result of a SPARL query
     * @param key     The variable to be used to build the map
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

    public List<Map<String, String>> splitColumn(List<Map<String, String>> df, String columnName, String regex) {
        for (Map<String, String> row : df) {
            String[] values = row.get(columnName).split(regex);
            Map<String, String> x = new HashMap<>();
            for (int i = 0; i < values.length; i++) {
                String key = columnName + (i + 1);
                row.put(key, values[i]);
            }
            row.remove(columnName);
        }
        return df;
    }

    /**
     * Returns {@code true} if {@code l} is not null and not an empty list.
     *
     * @param l   List to be checked
     * @param <T> Type of objects contained in the list
     * @return boolean
     */
    public <T> boolean checkList(List<T> l) {
        return l != null && !l.isEmpty();
    }

    /**
     * Returns {@code true} if {@code l} is not null, not empty and contains {@code o}.
     *
     * @param l   List to be checked
     * @param o   Value in the list to be checked
     * @param <T> Type of objects contained in the list
     * @return boolean
     */
    public <T> boolean checkList(List<T> l, T o) {
        return checkList(l) && l.contains(o);
    }

    /**
     * Returns {@code true} if {@code m} is not null and not empty.
     *
     * @param m   Map to be checked
     * @param <K> Type for keys in the map
     * @param <V> Type for values in the map
     * @return boolean
     */
    public <K, V> boolean checkMap(Map<K, V> m) {
        return m != null && !m.isEmpty();
    }

    /**
     * Returns {@code true} if {@code m} is not null, not empty and contains {@code key} as key.
     *
     * @param m   Map to be checked
     * @param key Key to be checked
     * @param <K> Type for keys in the map
     * @param <V> Type for values in the map
     * @return boolean
     */
    public <K, V> boolean checkMap(Map<K, V> m, K key) {
        return checkMap(m) && m.containsKey(key);
    }

    /**
     * If {@link #checkMap(Map, Object)} is {@code true} returns the value for {@code key} in {@code map},
     * otherwise returns {@code null}.
     *
     * @param map          Map to be accessed
     * @param key          Key to be used to access the map
     * @param <K>          Type for keys in the map
     * @param <V>          Type for values in the map
     * @param defaultValue Value to return when key is not found in map. Defaults to null if not passed as parameter.
     * @return The value of type {@code V} associated with {@code key} in the map
     */
    public <K, V> V getMapValue(Map<K, V> map, K key, V defaultValue) {
        return checkMap(map, key) ? map.get(key) : defaultValue;
    }

    public <K, V> V getMapValue(Map<K, V> map, K key) {
        return getMapValue(map, key, null);
    }

    /**
     * If {@link #checkMap(Map, Object)} is {@code true} returns the list for {@code key} in {@code map},
     * otherwise returns an empty {@link java.util.List}.
     *
     * @param listMap Map to be accessed
     * @param key     Key to be used to access the map
     * @param <K>     Type for keys in the map
     * @param <V>     Type for lists used as value in the map
     * @return The list of type {@code V} associated with {@code key} in the map
     */
    public <K, V> List<V> getListMapValue(Map<K, List<V>> listMap, K key) {
        if (checkMap(listMap, key)) return listMap.get(key);
        else return new ArrayList<>();
    }

    /**
     * Reads given file as a string.
     *
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
     *
     * @param fileName The file path for the RDF file.
     * @return An RDFReader
     * @throws Exception
     */
    public RDFReader getRDFReaderFromFile(String fileName) throws Exception {
        RDFReader rdfReader = new RDFReader();
        if (fileName != null) if ((new File(fileName)).exists()) {
            RDFFormat format = Rio.getParserFormatForFileName(fileName).orElse(RDFFormat.TURTLE);
            rdfReader.addFile(fileName, format);
        } else throw new IllegalArgumentException("FILE: " + fileName + " FOR RDFREADER DOES NOT EXIST");

        return rdfReader;
    }

    /**
     * Get a RDFReader to query the RDF content of the provided string.
     * The RDF format can be provided specifying the MIME type (default: text/turtle).
     *
     * @param s        The RDF string.
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
     * Get a RDFReader to query the RDF content of a remote triplestore.
     *
     * @param address      Address of the triplestore
     * @param repositoryId Repository Id for the triplestore
     * @param context      Optional named graph to be considered
     * @return An RDFReader
     * @throws Exception
     */
    public RDFReader getRDFReaderForRepository(String address, String repositoryId, String context) throws Exception {
        if (context != null) return new RDFReader(address, repositoryId, context);
        else return new RDFReader(address, repositoryId);
    }

    /**
     * Get a XMLReader to query the XML content of the provided file.
     *
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
     *
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
     *
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
     *
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

    /**
     * Get a CSVReader to query the CSV content of the provided file.
     *
     * @param fileName The file path for the CSV file.
     * @return A CSVReader
     * @throws Exception
     */
    public CSVReader getCSVReaderFromFile(String fileName) throws Exception {
        File f = new File(fileName);
        return new CSVReader(f);
    }

    /**
     * Get a CSVReader to query the CSV content of the provided string.
     *
     * @param s The CSV string.
     * @return A CSVReader
     * @throws Exception
     */
    public CSVReader getCSVReaderFromString(String s) throws Exception {
        if (s != null) {
            return new CSVReader(s);
        }
        return new CSVReader("");
    }

    /**
     * Get a SQLReader for a remote database.
     *
     * @param driver       Driver id in JDBC for the database considered
     * @param url          URL to access the database
     * @param databaseName Name of the database
     * @param username     Username for the database
     * @param password     Password for the database
     * @return An SQLReader
     * @throws Exception
     */
    public SQLReader getSQLReaderFromDatabase(String driver, String url, String databaseName, String username, String password) throws Exception {
        return new SQLReader(driver, url, databaseName, username, password);
    }

    /**
     * Get a SQLReader for a remote database.
     *
     * @param jdbcDSN      JDBC DSN for the database considered
     * @param username     Username for the database
     * @param password     Password for the database
     * @return An SQLReader
     * @throws Exception
     */
    public SQLReader getSQLReaderFromDatabase(String jdbcDSN, String username, String password) throws Exception {
        return new SQLReader(jdbcDSN, username, password);
    }

    /**
     * Merge two lists of results from queries on a {@link Reader}.
     *
     * @param results      Results of a query
     * @param otherResults Results to be merged
     * @return Merged results
     */
    public List<Map<String, String>> mergeResults(List<Map<String, String>> results, List<Map<String, String>> otherResults) {
        if (checkList(results)) {
            if (checkList(otherResults)) results.addAll(otherResults);
            return results;
        } else if (checkList(otherResults)) return otherResults;
        else return new ArrayList<>();
    }
    
    private Set<String> commonColumnNames(List<Map<String, String>> leftTable, List<Map<String, String>> rightTable, String leftKey, String rightKey) {
        // check if tables share column names
        Set<String> leftTableKeys = new HashSet<>(leftTable.get(0).keySet());
        Set<String> rightTableKeys = new HashSet<>(rightTable.get(0).keySet());

        // Find common keys between left and right tables
        Set<String> commonKeys = new HashSet<>(leftTableKeys);
        commonKeys.retainAll(rightTableKeys);

        if (!commonKeys.isEmpty()) {
            // Check if joining on the same key, and it's the only shared column name
            List<String> commonKeyList = new ArrayList<>(commonKeys);
            String commonKey = commonKeyList.get(0);
            if (commonKeyList.size() == 1 && commonKey.equals(leftKey) && commonKey.equals(rightKey)) {
                // If joining on the same key, and it's the only shared column name, proceed
                // Example: Joining on key "b" and "b" and no other column names are shared
                // return empty with "ok" semantic
                return Collections.emptySet();
            } else {
                // Throw exception if columns have the same name and can't perform join
                return commonKeys;
            }
        }
        return commonKeys;
    }

    public List<Map<String, String>> leftJoin(List<Map<String, String>> leftTable, List<Map<String, String>> rightTable, String key) {
        return leftJoin(leftTable, rightTable, key, key);
    }

    public List<Map<String, String>> leftJoin(List<Map<String, String>> leftTable, List<Map<String, String>> rightTable, String leftKey, String rightKey) {

        if (leftTable == null && rightTable == null)
            throw new IllegalArgumentException("tables in join cannot be null");
        if (leftTable == null) throw new IllegalArgumentException("leftTable cannot be null");
        if (rightTable == null) throw new IllegalArgumentException("rightTable cannot be null");

        if (leftTable.isEmpty()) return Collections.emptyList();
        // if the right table is empty (columns but no rows, impossible with maps like we use) the result should be all the colums from the left table + all columns from right table with null as values

        var commonKeys = commonColumnNames(leftTable, rightTable, leftKey, rightKey);
        if (!commonKeys.isEmpty()) {
            throw new RuntimeException("Cannot perform inner join on tables due to duplicate column names: " + commonKeys + ". Column names can be renamed using the 'renameDataFrameColumn' function");
        } else {
            Map<String, List<Map<String, String>>> rightTableMap = new HashMap<>();
            for (Map<String, String> rightMapEntry : rightTable) {
                String key = rightMapEntry.get(rightKey);
                if (!rightTableMap.containsKey(key)) {
                    rightTableMap.put(key, List.of(rightMapEntry));
                } else {
                    List<Map<String, String>> value = new ArrayList<>(rightTableMap.get(key));
                    value.add(rightMapEntry);
                    rightTableMap.put(key, value);
                }
            }

            Map<String, String> emptyRightRow = new HashMap<>();

            for (String k : rightTable.get(0).keySet()) {
                emptyRightRow.put(k, null);
            }
            List<Map<String, String>> result = new ArrayList<>();

            for (var leftRow : leftTable) {
                List<Map<String, String>> matches = rightTableMap.get(leftRow.get(leftKey));
                HashMap<String, String> joinedRow;
                if (matches != null) {   // add all columns from each table to the result
                    for (Map<String, String> match : matches) {
                        joinedRow = new HashMap<>(match);
                        joinedRow.putAll(leftRow);
                        result.add(joinedRow);
                    }

                } else {
                    joinedRow = new HashMap<>(emptyRightRow);
                    // written in this order the null values in the emptyRightRow get overwritten (if present) by present values in leftRow
                    // TLDR do not swap the previous and next lines
                    joinedRow.putAll(leftRow);
                    result.add(joinedRow);
                }
            }
            return result;
        }
    }

    public List<Map<String, String>> innerJoin(List<Map<String, String>> leftTable, List<Map<String, String>> rightTable, String key) {
        return innerJoin(leftTable, rightTable, key, key);
    }

    public List<Map<String, String>> innerJoin(List<Map<String, String>> leftTable, List<Map<String, String>> rightTable, String leftKey, String rightKey) {

        if (leftTable == null && rightTable == null)
            throw new IllegalArgumentException("tables in join cannot be null");
        if (leftTable == null) throw new IllegalArgumentException("leftTable cannot be null");
        if (rightTable == null) throw new IllegalArgumentException("rightTable cannot be null");

        // if either table is empty the return is an empty table/dataframe
        if (leftTable.isEmpty() || rightTable.isEmpty()) return Collections.emptyList();

        var commonKeys = commonColumnNames(leftTable, rightTable, leftKey, rightKey);
        if (!commonKeys.isEmpty()) {
            throw new RuntimeException("Cannot perform inner join on tables due to duplicate column names: " + commonKeys + ". Column names can be renamed using the 'renameDataFrameColumn' function");
        } else {
            Map<String, List<Map<String, String>>> leftTableMap = new HashMap<>();
            for (Map<String, String> leftMapEntry : leftTable) {
                String key = leftMapEntry.get(leftKey);
                if (!leftTableMap.containsKey(key)) {
                    leftTableMap.put(key, List.of(leftMapEntry));
                } else {
                    List<Map<String, String>> value = new ArrayList<>(leftTableMap.get(key));
                    value.add(leftMapEntry);
                    leftTableMap.put(key, value);
                }
            }

            List<Map<String, String>> result = new ArrayList<>();

            for (Map<String, String> rightRow : rightTable) {
                List<Map<String, String>> matches = leftTableMap.get(rightRow.get(rightKey));
                if (matches != null) {   // add all columns from each table to the result
                    for (Map<String, String> match : matches) {
                        HashMap<String, String> joinedRow = new HashMap<>(match);
                        joinedRow.putAll(rightRow);
                        result.add(joinedRow);
                    }
                }
            }
            return result;
        }
    }

    public List<Map<String, String>> renameDataframeColumn(List<Map<String, String>> dataFrame, String oldColumn, String newColumn) {
        if (dataFrame != null) {
            if (!dataFrame.isEmpty()) {
                Set<String> columnNames = dataFrame.get(0).keySet();
                if (columnNames.contains(newColumn)) {
                    throw new IllegalArgumentException("dataframe already contain a column named " + newColumn);
                }

                for (int i = 0; i < dataFrame.size(); i++) {
                    Map<String, String> row = dataFrame.get(i);
                    Map<String, String> updatedRow = new HashMap<>(row);

                    String v = updatedRow.remove(oldColumn);
                    updatedRow.put(newColumn, v);
                    dataFrame.set(i, updatedRow);
                }
            }
            return dataFrame;
        } else {
            throw new IllegalArgumentException("dataframe cannot be null");
        }
    }

    /**
     * Remove duplicate rows from a dataframe
     * @param dataframe Dataframe as input
     * @return dataframe without duplicated rows
     */
    public static List<Map<String, String>> removeDuplicatedRows(List<Map<String, String>> dataframe) {
        return dataframe.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Simple hash function that generates a hash composed only of letters (a-z).
     * @param input
     * @return computed hash
     */
    public static String literalHash(String input) {
        String hashString = Integer.toString(input.hashCode());
        char[] charArray = hashString.toCharArray();
        StringBuilder sb = new StringBuilder();

        for (char c : charArray) {
            if (Character.isDigit(c)) {
                // Convert digit to corresponding letter
                int digit = Character.getNumericValue(c);
                char letter = (char) ('a' + digit);
                sb.append(letter);
            } else if (c == '-') {
                // Replace minus sign with 'z'
                sb.append('z');
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * Generates a hash composed only of letters (a-z) for an array of Strings. Based on {@link #literalHash(String)}.
     * @param inputs Array of Strings
     * @return computed hash
     */
    public static String appendHash(String... inputs) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String i : inputs)
            if(i != null)
                stringBuilder.append(i);
        return literalHash(stringBuilder.toString());
    }

    /**
     * Checks if the given string is an absolute URI based on string pattern.
     *
     * @param str the string to check
     * @return true if the string is an absolute URI, false otherwise
     */
    public boolean isAbsoluteURI(String str) {
        if (str == null) {
            return false;
        }

        // Regex to match schemes like http, https, ftp, file, etc.
        String regex = "^[a-zA-Z][a-zA-Z0-9+.-]*:.*$";
        return str.matches(regex);
    }

    /**
     * Encode URL component and replaces + with %20 and * with %2A.
     * @param component of a URL to be encoded
     * @return Encoded component
     */
    public static String encodeURIComponent(String component) {
        final StringBuilder builder = new StringBuilder();

        // TODO Check why # is not encoded in 22c, related to Termtype?
        if (component.contains("#")) {
            String[] parts = component.split("#",2);
            component = URLEncoder.encode(parts[0], StandardCharsets.UTF_8) + "#"
                    + URLEncoder.encode(parts[1], StandardCharsets.UTF_8);
        } else
            component = URLEncoder.encode(component, StandardCharsets.UTF_8);

        for (char c : component.toCharArray()) {
            if (c == '+')
                builder.append("%20");
            else if (c == '*')
                builder.append("%2A");
            else
                builder.append(c);
        }
        return builder.toString();
    }

}

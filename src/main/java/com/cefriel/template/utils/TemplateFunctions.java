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
import java.net.URI;
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
    private String baseIRI;

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
    public static String sp(String s, String substring) {
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
    public static String p(String s, String substring) {
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
    public static String replace(String s, String regex, String replacement) {
        if (s != null) return s.replaceAll(regex, replacement);
        return null;
    }

    /**
     * Returns a new line char.
     *
     * @return A new line char.
     */
    public static String newline() {
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
    public static Map<String, Map<String, String>> getMap(List<Map<String, String>> results, String key) {
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
    public static Map<String, List<Map<String, String>>> getListMap(List<Map<String, String>> results, String key) {
        Map<String, List<Map<String, String>>> results_map = new HashMap<>();
        if (results != null) {
            for (Map<String, String> result : results)
                results_map.put(result.get(key), new ArrayList<>());
            for (Map<String, String> result : results)
                results_map.get(result.get(key)).add(result);
        }
        return results_map;
    }

    public static List<Map<String, String>> splitColumn(List<Map<String, String>> df, String columnName, String regex) {
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
    public static <T> boolean checkList(List<T> l) {
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
    public static <T> boolean checkList(List<T> l, T o) {
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
    public static <K, V> boolean checkMap(Map<K, V> m) {
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
    public static <K, V> boolean checkMap(Map<K, V> m, K key) {
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
    public static <K, V> V getMapValue(Map<K, V> map, K key, V defaultValue) {
        return checkMap(map, key) ? map.get(key) : defaultValue;
    }

    public static <K, V> V getMapValue(Map<K, V> map, K key) {
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
    public static <K, V> List<V> getListMapValue(Map<K, List<V>> listMap, K key) {
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
    public static String getFileAsString(String fileName) throws IOException {
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
    public static RDFReader getRDFReaderFromFile(String fileName) throws Exception {
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
    public static RDFReader getRDFReaderFromString(String s, String MIMEType) throws Exception {
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
    public static RDFReader getRDFReaderForRepository(String address, String repositoryId, String context) throws Exception {
        if (context != null) 
          return new RDFReader(address, repositoryId, context);
        else 
          return new RDFReader(address, repositoryId);
    }

    /**
     * Get a XMLReader to query the XML content of the provided file.
     *
     * @param fileName The file path for the XML file.
     * @return An XMLReader
     * @throws Exception
     */
    public static XMLReader getXMLReaderFromFile(String fileName) throws Exception {
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
    public static XMLReader getXMLReaderFromString(String s) throws Exception {
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
    public static JSONReader getJSONReaderFromFile(String fileName) throws Exception {
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
    public static JSONReader getJSONReaderFromString(String s) throws Exception {
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
    public static CSVReader getCSVReaderFromFile(String fileName) throws Exception {
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
    public static CSVReader getCSVReaderFromString(String s) throws Exception {
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
    public static SQLReader getSQLReaderFromDatabase(String driver, String url, String databaseName, String username, String password) throws Exception {
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
    public static SQLReader getSQLReaderFromDatabase(String jdbcDSN, String username, String password) throws Exception {
        return new SQLReader(jdbcDSN, username, password);
    }

    /**
     * Merge two lists of results from queries on a {@link Reader}.
     *
     * @param results      Results of a query
     * @param otherResults Results to be merged
     * @return Merged results
     */
    public static List<Map<String, String>> mergeResults(List<Map<String, String>> results, List<Map<String, String>> otherResults) {
        if (checkList(results)) {
            if (checkList(otherResults)) results.addAll(otherResults);
            return results;
        } else if (checkList(otherResults)) return otherResults;
        else return new ArrayList<>();
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

        // TODO Check why # is not encoded in rml-tc-22c, related to Termtype?
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

    public void setBaseIRI(String baseIRI) {
        this.baseIRI = baseIRI;
    }

    public String resolveIRI(String s) throws Exception {
        if(s != null) {
            if (!isAbsoluteURI(s)) {
                s = baseIRI + s;
                s = new URI(s).toString();
            } else {
                URLComponents url = new URLComponents(s);
                s = url.getEncodedURL();
            }

            return s;
        }
        return s;
    }

    public String resolveDatatype(String literal, String datatype) throws Exception {
        return "\"" + transformDatatypeString(literal, datatype) + "\"^^<" + resolveIRI(datatype) + ">";
    }

    public String resolveSQLDatatype(String literal, String type) {
        if (type != null) {
            String xsdType = getXsdFromSqlDatatypes(type);
            if (xsdType != null)
                return "\"" + transformDatatypeString(literal, xsdType) + "\"^^<" + xsdType + ">";
        }
        return "\"" + literal + "\"";
    }

    public String resolveLanguage(String literal, String language) {
        if(!literal.startsWith("\""))
            literal = "\"" + literal + "\"";
        if (isValidrrLanguage(language))
            return literal + "@" + language;
        else
            return literal;
    }

    /**
     * Return XSD datatype from SQL datatype
     * @param sqlDatatype SQL datatype
     * @return XSD datatype corresponding to the input SQL datatype
     */
    public static String getXsdFromSqlDatatypes(String sqlDatatype) {
        Map<String, String> map = new HashMap<>() {{
            put("DOUBLE", "http://www.w3.org/2001/XMLSchema#double");
            put("FLOAT", "http://www.w3.org/2001/XMLSchema#double");
            put("VARBINARY", "http://www.w3.org/2001/XMLSchema#hexBinary");
            put("DECIMAL", "http://www.w3.org/2001/XMLSchema#decimal");
            put("INTEGER", "http://www.w3.org/2001/XMLSchema#integer");
            put("INT", "http://www.w3.org/2001/XMLSchema#integer");
            put("BIT", "http://www.w3.org/2001/XMLSchema#boolean");
            put("BOOL", "http://www.w3.org/2001/XMLSchema#boolean");
            put("DATE", "http://www.w3.org/2001/XMLSchema#date");
            put("TIME", "http://www.w3.org/2001/XMLSchema#time");
            put("TIMESTAMP", "http://www.w3.org/2001/XMLSchema#dateTime");
            put("DATETIME", "http://www.w3.org/2001/XMLSchema#dateTime");
        }};

        if(sqlDatatype != null)
            for(String datatype : map.keySet())
                if(sqlDatatype.toUpperCase().contains(datatype))
                    return map.get(datatype);

        return null;
    }

    // From rmlmapper https://github.com/RMLio/rmlmapper-java/blob/f8d15d97efb9a30359b05f37a28328584fe62744/src/main/java/be/ugent/rml/Utils.java#L661
    public static String transformDatatypeString(String input, String datatype) {
        switch (datatype) {
            case "http://www.w3.org/2001/XMLSchema#hexBinary":
                return input;
            case "http://www.w3.org/2001/XMLSchema#decimal":
                return "" + Double.parseDouble(input);
            case "http://www.w3.org/2001/XMLSchema#integer":
                return "" + Integer.parseInt(input);
            case "http://www.w3.org/2001/XMLSchema#double":
                return formatToScientific(Double.parseDouble(input));
            case "http://www.w3.org/2001/XMLSchema#boolean":
                switch (input.toLowerCase()) {
                    case "t":
                    case "true":
                    case "1":
                        return "true";
                    default:
                        return "false";
                }
            case "http://www.w3.org/2001/XMLSchema#date":
                return input;
            case "http://www.w3.org/2001/XMLSchema#time":
                return input;
            case "http://www.w3.org/2001/XMLSchema#dateTime":
                return input.replace(" ", "T");
            default:
                return input;
        }
    }

    /**
     * From rmlmapper <a href="https://github.com/RMLio/rmlmapper-java/blob/f8d15d97efb9a30359b05f37a28328584fe62744/src/main/java/be/ugent/rml/Utils.java#L704">...</a>
     */
    private static String formatToScientific(Double d) {
        BigDecimal input = BigDecimal.valueOf(d).stripTrailingZeros();
        int precision = input.scale() < 0
                ? input.precision() - input.scale()
                : input.precision();
        StringBuilder s = new StringBuilder("0.0");
        for (int i = 2; i < precision; i++) {
            s.append("#");
        }
        s.append("E0");
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        DecimalFormat df = (DecimalFormat) nf;
        df.applyPattern(s.toString());
        return df.format(d);
    }

    /**
     * From rmlmapper <a href="https://github.com/RMLio/rmlmapper-java/blob/f8d15d97efb9a30359b05f37a28328584fe62744/src/main/java/be/ugent/rml/Utils.java#L385">...</a>
     */
    public static boolean isValidrrLanguage(String s) {
        Pattern regexPatternLanguageTag = Pattern.compile("^((?:(en-GB-oed|i-ami|i-bnn|i-default|i-enochian|i-hak|i-klingon|i-lux|i-mingo|i-navajo|i-pwn|i-tao|i-tay|i-tsu|sgn-BE-FR|sgn-BE-NL|sgn-CH-DE)|(art-lojban|cel-gaulish|no-bok|no-nyn|zh-guoyu|zh-hakka|zh-min|zh-min-nan|zh-xiang))|((?:([A-Za-z]{2,3}(-(?:[A-Za-z]{3}(-[A-Za-z]{3}){0,2}))?)|[A-Za-z]{4})(-(?:[A-Za-z]{4}))?(-(?:[A-Za-z]{2}|[0-9]{3}))?(-(?:[A-Za-z0-9]{5,8}|[0-9][A-Za-z0-9]{3}))*(-(?:[0-9A-WY-Za-wy-z](-[A-Za-z0-9]{2,8})+))*(-(?:x(-[A-Za-z0-9]{1,8})+))?)|(?:x(-[A-Za-z0-9]{1,8})+))$");
        return regexPatternLanguageTag.matcher(s).matches();
    }

}

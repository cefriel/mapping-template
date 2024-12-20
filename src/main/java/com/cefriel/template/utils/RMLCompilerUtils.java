/*
 * Copyright (c) Cefriel.
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
package com.cefriel.template.utils;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RMLCompilerUtils extends TemplateFunctions {
    final Pattern templatePattern = Pattern.compile("\\{([^{}]+)\\}");
    final Pattern referencePattern = Pattern.compile("\\$\\{(.*?)\\}");

    private String baseIRI;

    public void setBaseIRI(String baseIRI) {
        this.baseIRI = baseIRI;
    }

    @Override
    public String hash(String s) {
        return literalHash(s);
    }

    public List<String> getReferencesFromTriple(String subject, String predicate, String object, String graph){
        List<String> matches = new ArrayList<>();
        matches.addAll(getReferencesFromString(subject));
        matches.addAll(getReferencesFromString(predicate));
        matches.addAll(getReferencesFromString(object));
        matches.addAll(getReferencesFromString(graph));
        Set<String> distinctMatches = new HashSet(matches);
        
        return distinctMatches.stream()
            .filter(x -> !x.equals("esc.h"))
            .map(x -> "${" + x + "}")
            .collect(Collectors.toList());
    }

    private List<String> getReferencesFromString(String s){
        if (s != null) {
            Matcher matcher = referencePattern.matcher(s);

            List<String> matches = new ArrayList();
            while (matcher.find()) {
                matches.add(matcher.group(1));
            }
            return matches;
        }
        else
            return new ArrayList<>();
    }

    public String getIteratorFromString(String s){
        if (s != null) {
            Matcher matcher = referencePattern.matcher(s);

            while (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    public List<String> getReferencesFromTemplate(String input){
        Matcher matcher = templatePattern.matcher(input);

        List<String> matches = new ArrayList();
        while (matcher.find()) {
            matches.add(matcher.group(1));
        }
        return matches;
    }

    public String resolveTemplate(String input) {  
        List<String> matches = getReferencesFromTemplate(input);
        for(String m : matches)
            input = input.replace("{" + m + "}", resolveReference(m));
        return input;
    }

    public String resolveReference(String input) {
        return "${" + input + "}";
    }

    private String matchAndReplaceReference(String s) {
        Matcher matcher = referencePattern.matcher(s);

        List<String> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.group(1));
        }

        for (String m : matches) {
            String replace = m;
            replace = "${i." + hash(m) + "}";
            s = s.replace("${" + m + "}", replace);
        }

        s = s.replaceAll("#", "\\${esc.h}");

        return s;
    }

    public String encodeReferencesIRI(String s, String termType) {
        if (s != null) {
            s = matchAndReplaceReference(s);

            if (s.startsWith("data:"))
                return "<" + s + ">";
            else {
                if (termType != null)
                    if (termType.equals("http://w3id.org/rml/IRI") && !isAbsoluteURI(s))
                        return "<" + baseIRI + "$functions.encodeURIComponent(\"" + s + "\")>";
                Matcher matcher = referencePattern.matcher(s);
                if (!matcher.find() && isAbsoluteURI(s))
                    return "<" + s + ">";
                else
                    return "<$functions.resolveIRI(\"" + s + "\")>";
            }
        }
        return null;
    }

    public String encodeReferencesLiteral(String s) {
        if (s != null) {
            s = matchAndReplaceReference(s);
            return "\"" + s + "\"";
        }
        return null;
    }

    public String encodeDatatype(String literal, String datatype) {
        if (datatype != null) {
            datatype = matchAndReplaceReference(datatype);
            return "$functions.resolveDatatype(" + literal + ",\"" + datatype + "\")";
        }
        return literal;
    }

    public String encodeSQLDatatype(String literal, String column, String sourceHash) {
        return "$functions.resolveSQLDatatype(" + literal + ", $types_" + sourceHash + ".get(\"" + column + "\"))";
    }
    public String encodeLanguage(String literal, String language) {
        if (language != null) {
            language = matchAndReplaceReference(language);
            return "$functions.resolveLanguage(" + literal + ",\"" + language + "\")";
        }
        return literal;
    }

    public String encodeBlankNode(String s){
        if (s != null){
            s = matchAndReplaceReference(s);
            return "_:$functions.encodeURIComponent(\"" + s + "\")";
        }
        return s;
    }

    public List<String> getDistinct(List<String> list) {
        return list.stream()
                   .distinct() // Removes duplicates
                   .collect(Collectors.toList());
    }

    public String sanitizeJSON(String ref) {
        return ref.replaceAll("'", "\\\\\"");
    }

    public String getAsCommaSeparatedString(List<String> list) {
        return String.join(",", list);
    }

    public String getAsStringArray(List<String> list) {
        if (!list.isEmpty() && !list.contains(""))
            return "[\"" + String.join("\",\"", list) + "\"]";
        return "[]";
    }

    public List<Map<String,String>> hashVariablesDataFrame(List<Map<String,String>> dataframe) {
        return dataframe.stream().map(
            e -> e.entrySet().stream()
            .collect(Collectors.toMap(x -> hash(x.getKey()), Map.Entry::getValue)))
            .collect(Collectors.toList());
    }

    public boolean checkReference(String s){
        if(s.startsWith("$"))
            if(s.length() - s.replace("$", "").length() == 1)
                return true;
        return false;                     
    }

    /**
     * From rmlmapper <a href="https://github.com/RMLio/rmlmapper-java/blob/6492743f9c81523b6e9142c929d3aaecc78d67eb/src/main/java/be/ugent/rml/Utils.java#L634">...</a>
     *
     * Get the base directive from a turtle file if provided
     * @param rmlPath - input stream of the turtle file
     * @return - base directive or null
     */
    public String getBaseIRI(Path rmlPath) {
        String turtle;
        try {
            turtle = IOUtils.toString(Files.newInputStream(rmlPath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            turtle = "";
        }

        Pattern p = Pattern.compile("@base <([^<>]*)>");
        Matcher m = p.matcher(turtle);

        if (m.find()) {
            return m.group(1);
        } else {
            return null;
        }
    }

}


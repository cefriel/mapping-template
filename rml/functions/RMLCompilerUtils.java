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

import com.cefriel.template.utils.TemplateFunctions;

import java.lang.StringBuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RMLCompilerUtils extends TemplateFunctions {

    final Pattern templatePattern = Pattern.compile("\\{(.*?)\\}");
    final Pattern referencePattern = Pattern.compile("\\$\\{(.*?)\\}");

    public List<String> getReferencesFromTriple(String subject, String predicate, String object, String graph){
        List<String> matches = new ArrayList<>();
        matches.addAll(getReferencesFromString(subject));
        matches.addAll(getReferencesFromString(predicate));
        matches.addAll(getReferencesFromString(object));
        matches.addAll(getReferencesFromString(graph));
        Set<String> distinctMatches = new HashSet(matches);
        
        return distinctMatches.stream()
            .map(x -> "$" + x)
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

    private String encodeReferences(String s, boolean isURI, boolean hashVariable) {
        if (s != null) {
            Matcher matcher = referencePattern.matcher(s);

            List<String> matches = new ArrayList();
            while (matcher.find()) {
                matches.add(matcher.group(1));
            }
            
            for(String m : matches) {
                String replace = m;
                if (hashVariable)
                    replace = "${i." + hash(m) + "}";
                else
                    replace = "${i." + m + "}";
                if(isURI)
                    s = s.replace("${" + m + "}", "$functions.encodeURI(" + replace + ")");
                else
                    s = s.replace("${" + m + "}", replace);
            }
        }
        return s;
    }

    public String encodeReferencesIRI(String s, boolean hashVariable) {
        return encodeReferences(s, true, hashVariable);
    }

    public String encodeReferencesLiteral(String s, boolean hashVariable) {
        return encodeReferences(s, false, hashVariable);
    }

    public List<String> getDistinct(List<String> list) {
        return list.stream()
                   .distinct() // Removes duplicates
                   .collect(Collectors.toList());
    }

    /**
     * Simple hash function that generates a hash composed only of letters (a-z).
     * @param input
     * @return
     */
    @Override
    public String hash(String input) {
        int hash = 0;
        for (int i = 0; i < input.length(); i++) {
            hash = (hash * 31 + input.charAt(i)) % 26;
        }
        StringBuilder stringBuilder = new StringBuilder();
        char baseChar = 'a';
        for (int i = 0; i < input.length(); i++) {
            stringBuilder.append((char) (baseChar + (hash + i) % 26));
        }
        return stringBuilder.toString();
    }

}

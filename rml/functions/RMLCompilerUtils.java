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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

public class RMLCompilerUtils extends TemplateFunctions {

    final Pattern templatePattern = Pattern.compile("\\{(.*?)\\}");

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
        return "${i." + input + "}";
    }

}


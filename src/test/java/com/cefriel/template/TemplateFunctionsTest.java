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

package com.cefriel.template;

import com.cefriel.template.io.Reader;
import com.cefriel.template.utils.TemplateFunctions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateFunctionsTest {

    public class CustomTemplateFunctions extends TemplateFunctions {
        public String printMessage() {
            return "test";
        }
    }
    @Test
    public void getMapValueTest() {
        TemplateFunctions templateFunctions = new TemplateFunctions();
        HashMap<String, String> row = new HashMap<>();
        row.put("a", "a");

        // if the key is present return the value
        Assertions.assertEquals("a", templateFunctions.getMapValue(row, "a"));
        // if key is not present and no default value has been specified return null
        Assertions.assertNull(templateFunctions.getMapValue(row, "b"));
        // if key is not present but default value has been specified return default value
        Assertions.assertEquals("default", templateFunctions.getMapValue(row, "b", "default"));
    }
    @Test
    public void splitColumnTest() {
        HashMap<String, String> row1 = new HashMap<>();
        row1.put("column", "value1 value2");
        HashMap<String, String> row2 = new HashMap<>();
        row2.put("column", "value1 value2");
        List<Map<String, String>> df;
        df = new ArrayList<>();
        df.add(row1);
        df.add(row2);

        var templateUtils = new TemplateFunctions();

        df = templateUtils.splitColumn(df, "column", " ");
        for (var row: df) {
            assert (templateUtils.checkMap(row, "column1"));
            assert (row.get("column1").equals("value1"));
            assert (templateUtils.checkMap(row, "column2"));
            assert (row.get("column2").equals("value2"));
        }
    }

    @Test
    public void testCustomFunctions() throws Exception {

        Path template = Paths.get("src/test/resources/custom-functions/template.vm");
        CustomTemplateFunctions customTemplateFunctions = new CustomTemplateFunctions();
        TemplateExecutor executor = new TemplateExecutor((Reader) null, customTemplateFunctions, true, false, false, null, null);

        String result = executor.executeMapping(template);
        Assertions.assertEquals(result, customTemplateFunctions.printMessage());
    }

    @Test
    public void testCustomFunctionsStreamMode() throws Exception {

        File template = new File("src/test/resources/custom-functions/template.vm");
        FileInputStream fileInputStream = new FileInputStream(template);

        CustomTemplateFunctions customTemplateFunctions = new CustomTemplateFunctions();
        TemplateExecutor executor = new TemplateExecutor((Reader) null, customTemplateFunctions, true, false, false, null, null);
        String result = executor.executeMapping(fileInputStream);

        Assertions.assertEquals(result, customTemplateFunctions.printMessage());
    }


}

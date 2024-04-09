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

import com.cefriel.template.io.csv.CSVReader;
import com.cefriel.template.utils.TemplateFunctions;
import org.eclipse.rdf4j.query.algebra.Str;
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
        TemplateExecutor executor = new TemplateExecutor();
        Path template = Paths.get("src/test/resources/custom-functions/template.vm");

        CustomTemplateFunctions customTemplateFunctions = new CustomTemplateFunctions();

        String result = executor.executeMapping(null,  template, false, false, false, null, null, customTemplateFunctions);
        Assertions.assertEquals(result, customTemplateFunctions.printMessage());
    }

    @Test
    public void testCustomFunctionsStreamMode() throws Exception {
        TemplateExecutor executor = new TemplateExecutor();
        File template = new File("src/test/resources/custom-functions/template.vm");
        FileInputStream fileInputStream = new FileInputStream(template);

        CustomTemplateFunctions customTemplateFunctions = new CustomTemplateFunctions();
        String result = executor.executeMapping(null, fileInputStream, null, null, customTemplateFunctions);

        Assertions.assertEquals(result, customTemplateFunctions.printMessage());
    }

    @Test
    public void testInnerJoinSameColumn() {
        TemplateFunctions templateFunctions = new TemplateFunctions();
        List<Map<String, String>> table1 = new ArrayList<>();
        table1.add(Map.of("a", "1", "b", "4"));
        table1.add(Map.of("a", "2", "b", "5"));
        table1.add(Map.of("a", "1", "b", "5"));
        List<Map<String, String>> table2 = new ArrayList<>();
        table2.add(Map.of("b", "1", "d", "5"));
        table2.add(Map.of("b", "4", "d", "33"));
        table2.add(Map.of("b", "5", "d", "99"));
        table2.add(Map.of("b", "5", "d", "6"));

        var result = templateFunctions.innerJoin(table1, table2, "b");
        assert(result.size() == 5);
        for (var row : result) {
            assert (row.size() == 3);
        }
    }

    @Test
    public void testInnerJoinDifferentColumn() {
        TemplateFunctions templateFunctions = new TemplateFunctions();
        List<Map<String, String>> table1 = new ArrayList<>();
        table1.add(Map.of("a", "1", "b", "4"));
        table1.add(Map.of("a", "2", "b", "5"));
        table1.add(Map.of("a", "1", "b", "5"));
        List<Map<String, String>> table2 = new ArrayList<>();
        table2.add(Map.of("c", "1", "d", "5"));
        table2.add(Map.of("c", "4", "d", "33"));
        table2.add(Map.of("c", "5", "d", "99"));
        table2.add(Map.of("c", "5", "d", "6"));

        var result = templateFunctions.innerJoin(table1, table2, "b", "d");
        assert(result.size() == 2);
        for (var row : result) {
            assert (row.size() == 4);
        }
    }

    @Test
    public void testInnerJoinDuplicateColumnNames() {
        TemplateFunctions templateFunctions = new TemplateFunctions();
        List<Map<String, String>> table1 = new ArrayList<>();
        table1.add(Map.of("d", "1", "b", "4"));
        table1.add(Map.of("d", "2", "b", "5"));

        List<Map<String, String>> table2 = new ArrayList<>();
        table2.add(Map.of("b", "1", "d", "5"));
        table2.add(Map.of("b", "4", "d", "33"));
        table2.add(Map.of("b", "5", "d", "99"));

        Assertions.assertThrows(RuntimeException.class, () -> templateFunctions.innerJoin(table1, table2, "b", "d"));
    }
    @Test
    public void testInnerJoinRenameColumn() {
        TemplateFunctions templateFunctions = new TemplateFunctions();
        List<Map<String, String>> table1 = new ArrayList<>();
        table1.add(Map.of("d", "1", "b", "4"));
        table1.add(Map.of("d", "2", "b", "5"));
        List<Map<String, String>> table2 = new ArrayList<>();
        table2.add(Map.of("b", "1", "d", "5"));
        table2.add(Map.of("b", "4", "d", "33"));
        table2.add(Map.of("b", "5", "d", "99"));

        table1 = templateFunctions.renameDataFrameColumn(table1, "d", "newName");
        table2 = templateFunctions.renameDataFrameColumn(table2, "b", "otherNewName");

        var result = templateFunctions.innerJoin(table1, table2, "b", "d");
        assert(result.size() == 1);
        for (var row : result) {
            assert (row.size() == 4);
        }
    }
    @Test
    public void testLeftJoinSameColumn() {
        TemplateFunctions templateFunctions = new TemplateFunctions();
        List<Map<String, String>> table1 = new ArrayList<>();
        table1.add(Map.of("a", "1", "b", "4"));
        table1.add(Map.of("a", "2", "b", "5"));
        table1.add(Map.of("a", "3", "b", "6"));
        List<Map<String, String>> table2 = new ArrayList<>();
        table2.add(Map.of("b", "1", "d", "5"));
        table2.add(Map.of("b", "4", "d", "33"));
        table2.add(Map.of("b", "5", "d", "99"));
        table2.add(Map.of("b", "5", "d", "6"));

        var result = templateFunctions.leftJoin(table1, table2, "b");
        assert(result.size() == 4);
        for (var row : result) {
            assert (row.size() == 3);
        }
    }

    @Test
    public void testLeftJoinDifferentColumn() {
        TemplateFunctions templateFunctions = new TemplateFunctions();
        List<Map<String, String>> table1 = new ArrayList<>();
        table1.add(Map.of("a", "1", "b", "4"));
        table1.add(Map.of("a", "2", "b", "5"));
        table1.add(Map.of("a", "1", "b", "5"));
        List<Map<String, String>> table2 = new ArrayList<>();
        table2.add(Map.of("c", "1", "d", "5"));
        table2.add(Map.of("c", "4", "d", "33"));
        table2.add(Map.of("c", "5", "d", "99"));
        table2.add(Map.of("c", "5", "d", "6"));

        var result = templateFunctions.leftJoin(table1, table2, "b", "d");
        assert(result.size() == 3);
        for (var row : result) {
            assert (row.size() == 4);
        }
    }
    @Test
    public void testLeftJoinDuplicateColumnNames() {
        TemplateFunctions templateFunctions = new TemplateFunctions();
        List<Map<String, String>> table1 = new ArrayList<>();
        table1.add(Map.of("d", "1", "b", "4"));
        table1.add(Map.of("d", "2", "b", "5"));

        List<Map<String, String>> table2 = new ArrayList<>();
        table2.add(Map.of("b", "1", "d", "5"));
        table2.add(Map.of("b", "4", "d", "33"));
        table2.add(Map.of("b", "5", "d", "99"));

        Assertions.assertThrows(RuntimeException.class, () -> templateFunctions.leftJoin(table1, table2, "b", "d"));
    }
    @Test
    public void testLeftJoinRenameColumn() {
        TemplateFunctions templateFunctions = new TemplateFunctions();
        List<Map<String, String>> table1 = new ArrayList<>();
        table1.add(Map.of("d", "1", "b", "4"));
        table1.add(Map.of("d", "2", "b", "5"));
        table1.add(Map.of("a", "3", "b", "6"));
        List<Map<String, String>> table2 = new ArrayList<>();
        table2.add(Map.of("b", "1", "d", "5"));
        table2.add(Map.of("b", "4", "d", "33"));
        table2.add(Map.of("b", "5", "d", "99"));

        table1 = templateFunctions.renameDataFrameColumn(table1, "d", "newName");
        table2 = templateFunctions.renameDataFrameColumn(table2, "b", "otherNewName");

        var result = templateFunctions.innerJoin(table1, table2, "b", "d");
        assert(result.size() == 1);
        for (var row : result) {
            assert (row.size() == 4);
        }
    }
}

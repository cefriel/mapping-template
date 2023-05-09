package com.cefriel.template;

import com.cefriel.template.io.json.JSONReader;
import com.cefriel.template.utils.TemplateFunctions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateFunctionsTest {

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
    public void escapeToolTest() throws Exception {
        TemplateExecutor executor = new TemplateExecutor();
        File template = new File("src/test/resources/escape-tool/template.vm");

        String result = executor.executeMapping(new JSONReader(" "),  template.getPath(), false, false,null, null, new TemplateFunctions());
        String expectedOutput = "test \n";

        expectedOutput = expectedOutput.replaceAll("\\r\\n", "\n");
        result = result.replaceAll("\\r\\n", "\n");

        assert(expectedOutput.equals(result));
    }
}

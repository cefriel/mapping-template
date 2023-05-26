package com.cefriel.template;

import com.cefriel.template.io.json.JSONReader;
import com.cefriel.template.utils.TemplateFunctions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class JsonReaderTest {

    @Test
    public void jsonTest() throws Exception {
        JSONReader reader = new JSONReader(new File("src/test/resources/json/example.json"));
        TemplateExecutor executor = new TemplateExecutor();
        File template = new File("src/test/resources/json/template.vm");

        String result = executor.executeMapping(reader,  template.getPath(), false, false,null, null, new TemplateFunctions());
        String expectedOutput = Files.readString(Paths.get("src/test/resources/json/correct-output.ttl"));

        expectedOutput = expectedOutput.replaceAll("\\r\\n", "\n");
        result = result.replaceAll("\\r\\n", "\n");

        assert(expectedOutput.equals(result));
    }
}


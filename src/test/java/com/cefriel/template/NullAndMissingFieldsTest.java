package com.cefriel.template;
import com.cefriel.template.io.json.JSONReader;
import com.cefriel.template.utils.TemplateFunctions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class NullAndMissingFieldsTest {

    @Test
    public void nullAndMissingFieldsTest() throws Exception {
        JSONReader jsonReader = new JSONReader(new File("src/test/resources/null-and-missing-fields/modified.json"));
        TemplateExecutor executor = new TemplateExecutor();
        File template = new File("src/test/resources/null-and-missing-fields/template.vm");

        String result = executor.executeMapping(jsonReader,  template.getPath(), false, false,null, null, new TemplateFunctions());
        String expectedOutput = Files.readString(Paths.get("src/test/resources/null-and-missing-fields/correct-output.txt"));

        expectedOutput = expectedOutput.replaceAll("\\r\\n", "\n");
        result = result.replaceAll("\\r\\n", "\n");

        assert(expectedOutput.equals(result));

    }
}

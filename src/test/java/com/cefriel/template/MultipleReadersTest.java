package com.cefriel.template;

import com.cefriel.template.io.Reader;
import com.cefriel.template.io.csv.CSVReader;
import com.cefriel.template.utils.TemplateFunctions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class MultipleReadersTest {

    @Test
    public void testMultipleReaders() throws Exception {
        Map<String, Reader> readers = Map.of("reader1", new CSVReader(new File("src/test/resources/multiple-readers/a.csv")),
                "reader2", new CSVReader(new File("src/test/resources/multiple-readers/b.csv")));
        Path template = Paths.get("src/test/resources/multiple-readers/template.vm");
        TemplateExecutor templateExecutor = new TemplateExecutor( true, false, false, null);
        String result = templateExecutor.executeMapping(readers, template, new TemplateFunctions(), null).replaceAll("\\r\\n", "\n");;
        String expectedOutput = "1,2,3,4,5,6".replaceAll("\\r\\n", "\n");
        assert expectedOutput.equals(result);
    }
}

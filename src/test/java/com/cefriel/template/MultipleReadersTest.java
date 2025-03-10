package com.cefriel.template;

import com.cefriel.template.io.Reader;
import com.cefriel.template.io.csv.CSVReader;
import com.cefriel.template.io.json.JSONReader;
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
        String result = templateExecutor.executeMapping(readers, template).replaceAll("\\r\\n", "\n");;
        String expectedOutput = "1,2,3,4,5,6";
        assert expectedOutput.equals(result);
    }

    @Test
    public void testMultipleReadersSingleReader() throws Exception {
        // when only one reader is passed along the readers map
        Map<String, Reader> readers = Map.of("reader1", new JSONReader(new File("src/test/resources/multiple-readers-single-reader/a.json")));
        Path template = Paths.get("src/test/resources/multiple-readers-single-reader/template.vm");
        TemplateExecutor templateExecutor = new TemplateExecutor( true, false, false, null);
        String result = templateExecutor.executeMapping(readers, template).replaceAll("\\r\\n", "\n");;
        String expectedOutput = "1,2,3,1,2,3";
        assert expectedOutput.equals(result);
    }
}

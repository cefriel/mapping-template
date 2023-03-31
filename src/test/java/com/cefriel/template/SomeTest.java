package com.cefriel.template;
import com.cefriel.template.io.json.JSONReader;
import com.cefriel.template.io.rdf.RDFReader;
import com.cefriel.template.utils.TemplateFunctions;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SomeTest {

    private String resolvePath(String folder, String file) {
        return "src/test/resources/" + folder + "/" + file;
    }

    @Test
    public void someTest() throws Exception {
        JSONReader jsonReader = new JSONReader(new File("src/test/resources/modified.json"));
        TemplateExecutor executor = new TemplateExecutor();
        File template = new File("src/test/resources/template.vm");
        String result = executor.executeMapping(jsonReader,  template.getPath(), false, false,null, null, new TemplateFunctions());
        System.out.println(result);

    }
}

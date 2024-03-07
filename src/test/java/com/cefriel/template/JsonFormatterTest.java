package com.cefriel.template;

import com.cefriel.template.io.json.JSONFormatter;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class JsonFormatterTest {

    @Test
    public void jsonFormatterTest() throws IOException {
        JSONFormatter jsonFormatter = new JSONFormatter();
        String input1 = "       {}";
        String input2 = "                 []";
        String inputMixed = "[{},{}]";

        assert(jsonFormatter.formatJSON(input1).equals("{}"));
        assert(jsonFormatter.formatJSON(input2).equals("[]"));
        String expected = "[\n" + "    {},\n" + "    {}\n" + "]";
        assert (jsonFormatter.formatJSON(inputMixed).equals(expected));
    }
}

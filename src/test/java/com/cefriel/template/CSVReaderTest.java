package com.cefriel.template;

import com.cefriel.template.io.csv.CSVReader;
import de.siegmar.fastcsv.reader.CsvRow;
import de.siegmar.fastcsv.reader.NamedCsvReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class CSVReaderTest {

    private static CSVReader csvReader;
    private static String filePath = "src/test/resources/testCSVWithHeaders.csv";

    @Test
    public void testCSVReadFile() throws Exception {
        this.csvReader = new CSVReader(filePath);
        List<Map<String, String>> results = this.csvReader.executeQueryStringValue("");
        assert (!results.isEmpty());
        assert (results.size() == 1);
        Map<String, String> row = results.get(0);
        assert(row.get("id").equals("6523"));
        assert(row.get("stop").equals("25"));
        assert(row.get("latitude").equals("50.901389"));
        assert(row.get("longitude").equals("4.484444"));
    }
}

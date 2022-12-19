package com.cefriel.template;

import com.cefriel.template.io.csv.CSVReader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

public class CSVReaderTest {

    private static CSVReader csvReader;
    private static String filePath = "src/test/resources/testCSVWithHeaders.csv";
    @Test
    public void testCSVReadFile() throws Exception {
        File f = new File(filePath);
        csvReader = new CSVReader(f);
        List<Map<String, String>> results = csvReader.getDataframe();
        assert (!results.isEmpty());
        assert (results.size() == 1);
        Map<String, String> row = results.get(0);
        assert(row.get("id").equals("6523"));
        assert(row.get("stop").equals("25"));
        assert(row.get("latitude").equals("50.901389"));
        assert(row.get("longitude").equals("4.484444"));
    }
    @Test
    public void testCSVReadFromString() throws Exception {
        String s = "id,stop,latitude,longitude\n6523,25,50.901389,4.484444";
        csvReader = new CSVReader(s);
        List<Map<String, String>> results = csvReader.getDataframe();
        assert (!results.isEmpty());
        assert (results.size() == 1);
        Map<String, String> row = results.get(0);
        assert(row.get("id").equals("6523"));
        assert(row.get("stop").equals("25"));
        assert(row.get("latitude").equals("50.901389"));
        assert(row.get("longitude").equals("4.484444"));
    }
}

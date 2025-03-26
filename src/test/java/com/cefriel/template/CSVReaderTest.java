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
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

public class CSVReaderTest {

    private static CSVReader csvReader;

    @Test
    public void testCSVReadFile() throws Exception {
        String filePath = "src/test/resources/testCSVWithHeaders.csv";
        File f = new File(filePath);
        csvReader = new CSVReader(f);
        List<Map<String, String>> results = csvReader.getDataframe("id", "stop", "latitude", "longitude");
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

    @Test
    public void testCSVReaderMultipleDataframes() throws Exception {
        String s = "id,stop,latitude,longitude\n6523,25,50.901389,4.484444";
        csvReader = new CSVReader(s);
        List<Map<String, String>> results = csvReader.getDataframe();
        List<Map<String, String>> results2 = csvReader.getDataframe();
        assert (!results.isEmpty());
        assert (!results2.isEmpty());
    }

    @Test
    public void testCSVReaderFileWithBOM() throws Exception {
        File csvFile = new File("src/test/resources/csv-reader/test.csv");
        csvReader = new CSVReader(csvFile);
        List<Map<String, String>> results = csvReader.getDataframe();
        assert (!results.isEmpty());
        assert (results.get(0).keySet().size() == 3);
    }
}

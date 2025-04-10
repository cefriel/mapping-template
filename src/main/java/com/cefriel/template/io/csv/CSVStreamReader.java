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

package com.cefriel.template.io.csv;

import com.cefriel.template.utils.TemplateFunctions;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class CSVStreamReader extends CSVReaderAbstract {

    public CsvReader<NamedCsvRecord> document;

    public CSVStreamReader(File file) throws IOException {
        if (Files.exists(file.toPath())) {
            try (CsvReader<NamedCsvRecord> input = CsvReader.builder().ofNamedCsvRecord(file.toPath())) {
                headers = input.stream().findFirst().orElseThrow().getHeader();
            }
            this.document = CsvReader.builder().ofNamedCsvRecord(file.toPath());
        } else
            throw new IllegalArgumentException("File does not exist: " + file.getPath());
    }

    public CSVStreamReader(String csv) throws IOException {
        try (CsvReader<NamedCsvRecord> input = CsvReader.builder().ofNamedCsvRecord(csv)) {
            headers = input.stream().findFirst().orElseThrow().getHeader();
        }
        this.document = CsvReader.builder().ofNamedCsvRecord(csv);
    }

    public List<Map<String, String>> getDataframe() throws Exception {
        String[] columns = headers.toArray(new String[0]);
        return getDataframe(columns);
    }

    public List<Map<String, String>> getDataframe(String... columns) throws Exception {
        // Return entire dataframe if no columns are provided or if empty string is provided
        if ((columns == null || columns.length == 0) || (columns.length == 1 && columns[0].isEmpty()))
            return getDataframe();

        int columnCount = 0;
        for(String c : columns) {
            if (!headers.contains(c))
                throw new IllegalArgumentException("Column " + c + " not found");
            columnCount += 1;
        }
        // For stream behaviour rowCount can not be obtained
        Collection<Map<String, String>> dataframe = onlyDistinct ? new HashSet<>() : new ArrayList<>();

        final int mapSize = columnCount;
        this.document.stream().forEach(row -> {
            HashMap<String, String> map = new HashMap<>(mapSize);
            for (String c : columns) {
                if(hashVariable)
                    map.put(TemplateFunctions.literalHash(c), row.getField(c));
                else
                    map.put(c, row.getField(c));
            }
            dataframe.add(map);
        });

        return new ArrayList<>(dataframe);
    }
}
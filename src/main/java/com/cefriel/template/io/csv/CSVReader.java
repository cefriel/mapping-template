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
import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class CSVReader extends CSVReaderAbstract {

    private final List<NamedCsvRecord> csvRecords;

    public CSVReader(File file) throws IOException {
        if (Files.exists(file.toPath())) {
            try (CsvReader<NamedCsvRecord> input = CsvReader.builder()
                    .detectBomHeader(true)
                    .ofNamedCsvRecord(file.toPath())) {
                this.csvRecords = input.stream().collect(Collectors.toList());
            }
            headers = csvRecords.get(0).getHeader();
        } else {
            throw new IllegalArgumentException("File does not exist: " + file.getPath());
        }
    }

    public CSVReader(String csv) throws IOException {
        try (CsvReader<NamedCsvRecord> input = CsvReader.builder()
                .ofNamedCsvRecord(csv)) {
            this.csvRecords = input.stream().collect(Collectors.toList());
        }
        headers = csvRecords.get(0).getHeader();
    }

    public List<Map<String, String>> getDataframe() throws Exception {
        if (csvRecords.isEmpty()) {
            return Collections.emptyList();
        }
        return getDataframe(headers.toArray(new String[0]));
    }

    public List<Map<String, String>> getDataframe(String... columns) throws Exception {
        if (csvRecords.isEmpty()) {
            return Collections.emptyList();
        }

        if ((columns == null || columns.length == 0) || (columns.length == 1 && columns[0].isEmpty())) {
            return getDataframe();
        }

        int columnCount = 0;
        for (String c : columns) {
            if (!headers.contains(c)) {
                throw new IllegalArgumentException("Column " + c + " not found");
            }
            columnCount++;
        }

        // initialize collection with max possible size. Could be fewer rows if only distinct rows are requested in the dataframe.
        int rowCount = csvRecords.size();
        Collection<Map<String, String>> dataframe = onlyDistinct ? new HashSet<>(rowCount) : new ArrayList<>(rowCount);

        for (NamedCsvRecord row : csvRecords) {
            Map<String, String> map = new HashMap<>(columnCount);
            for (String c : columns) {
                String value = row.getField(c);
                if ("xml".equalsIgnoreCase(this.outputFormat)) {
                    value = StringEscapeUtils.escapeXml11(value);
                }
                if (hashVariable) {
                    map.put(TemplateFunctions.literalHash(c), value);
                } else {
                    map.put(c, value);
                }
            }
            dataframe.add(map);
        }
        return new ArrayList<>(dataframe);
    }
}

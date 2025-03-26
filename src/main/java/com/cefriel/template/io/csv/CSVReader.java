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

import com.cefriel.template.io.Reader;
import com.cefriel.template.utils.TemplateFunctions;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class CSVReader implements Reader {

    private final List<NamedCsvRecord> csvRecords;
    private boolean hashVariable;
    private boolean onlyDistinct;

    public CSVReader(File file) throws IOException {
        if (Files.exists(file.toPath())) {
            this.csvRecords = CsvReader.builder().ofNamedCsvRecord(file.toPath()).
                    stream()
                    .collect(Collectors.toList());
        } else {
            throw new IllegalArgumentException("File does not exist: " + file.getPath());
        }
    }

    public CSVReader(String csv) {
        this.csvRecords = CsvReader.builder().ofNamedCsvRecord(csv)
                .stream()
                .collect(Collectors.toList());
    }

    @Override
    public void setQueryHeader(String header) {}

    @Override
    public void appendQueryHeader(String s) {}

    public List<Map<String, String>> getDataframe() throws Exception {
        if (csvRecords.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> headers = csvRecords.get(0).getHeader();
        return getDataframe(headers.toArray(new String[0]));
    }

    @Override
    public List<Map<String, String>> getDataframe(String query) throws Exception {
        String[] columns = query.split(",");
        return getDataframe(columns);
    }

    public List<Map<String, String>> getDataframe(String... columns) throws Exception {
        if (csvRecords.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> headers = csvRecords.get(0).getHeader();

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
                if (hashVariable) {
                    map.put(TemplateFunctions.literalHash(c), row.getField(c));
                } else {
                    map.put(c, row.getField(c));
                }
            }
            dataframe.add(map);
        }
        return new ArrayList<>(dataframe);
    }

    @Override
    public void debugQuery(String query, Path destinationPath) throws Exception {}

    @Override
    public void setVerbose(boolean verbose) {}

    @Override
    public void setOutputFormat(String outputFormat) {}

    @Override
    public void setHashVariable(boolean hashVariable) {
        this.hashVariable = hashVariable;
    }

    @Override
    public void setOnlyDistinct(boolean onlyDistinct) {
        this.onlyDistinct = onlyDistinct;
    }

    @Override
    public void shutDown() {}
}

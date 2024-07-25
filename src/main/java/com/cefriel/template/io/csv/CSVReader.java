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
import de.siegmar.fastcsv.reader.NamedCsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class CSVReader implements Reader {

    public NamedCsvReader document;

    public CSVReader(File file) throws IOException {
        if (Files.exists(file.toPath()))
            this.document = NamedCsvReader.builder().build(file.toPath());
        else
            throw new IllegalArgumentException("File does not exist: " + file.getPath());
    }

    public CSVReader(String csv) {
        this.document = NamedCsvReader.builder().build(csv);
    }
    @Override
    public void setQueryHeader(String header) {

    }

    @Override
    public void appendQueryHeader(String s) {

    }

    @Override
    public List<Map<String, String>> getDataframe(String query) throws Exception {
        String[] headers = query.split(",");
        return getDataframe(headers);
    }

    @Override
    public List<Map<String, String>> getDataframe() throws Exception {
        return getDataframe(new String[0]);
    }

    @Override
    public void debugQuery(String query, Path destinationPath) throws Exception {

    }

    public List<Map<String, String>> getDataframe(String... columns) {
        Set<String> columnsToKeep;
        Set<String> fileColumns = this.document.getHeader();
        if ((columns == null || columns.length == 0) || (columns.length == 1 && columns[0].isEmpty()))
            columnsToKeep = fileColumns;
        else {
            columnsToKeep = new HashSet<>(Arrays.asList(columns));
            for (String column : columnsToKeep) {
                if(!fileColumns.contains(column))
                    throw new IllegalArgumentException("Header not found in document: " + column);
            }
        }

        List<Map<String, String>> output = new ArrayList<>();
        for (NamedCsvRow row : this.document) {
            Map<String, String> map = columnsToKeep.stream().collect(Collectors.toMap(
                    header -> header,
                    row::getField
            ));
            output.add(map);
        }
        return output;
    }
    @Override
    public void setVerbose(boolean verbose) {

    }

    /**
     * Not implemented for CSVReader yet.
     * @param outputFormat String identifying the output format
     */
    @Override
    public void setOutputFormat(String outputFormat) { return;}

    @Override
    public void shutDown() {

    }
}

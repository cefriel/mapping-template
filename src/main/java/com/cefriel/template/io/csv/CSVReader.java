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
import de.siegmar.fastcsv.reader.CsvRow;
import de.siegmar.fastcsv.reader.NamedCsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.util.*;

public class CSVReader implements Reader {

    public NamedCsvReader document;
    private boolean hashVariable;
    private boolean onlyDistinct;

    public CSVReader(File file) throws IOException {
        if (Files.exists(file.toPath()))
            this.document = NamedCsvReader.builder().build(file.toPath());
        else
            throw new IllegalArgumentException("FILE: " + file.getPath() + " FOR CSVREADER DOES NOT EXIST");
    }

    public CSVReader(String csv) throws IOException {
        this.document = NamedCsvReader.builder().build(csv);
    }
    @Override
    public void setQueryHeader(String header) {

    }

    @Override
    public void appendQueryHeader(String s) {

    }

    public List<Map<String, String>> getDataframe() throws Exception {
        Set<String> headers = this.document.getHeader();
        String[] columns = headers.toArray(new String[0]);
        return getDataframe(columns);
    }
    @Override
    public List<Map<String, String>> getDataframe(String query) throws Exception {
        String[] columns = query.split(",");
        return getDataframe(columns);
    }

    public List<Map<String, String>> getDataframe(String... columns) throws Exception {
        Set<String> headers = this.document.getHeader();
        int columnCount = 0;
        for(String c : columns) {
            if (!headers.contains(c))
                throw new IllegalArgumentException("Column " + c + " not found");
            columnCount += 1;
        }
        // TODO Check if rowCount can be obtained to properly initialise the collection capacity
        Collection<Map<String,String>> dataframe;
        if (onlyDistinct)
            dataframe = new ArrayList<>();
        else
            dataframe = new HashSet<>();
        for (NamedCsvRow row : this.document) {
            HashMap<String, String> map = new HashMap<>(columnCount);
            for (String c : columns) {
                if(hashVariable)
                    map.put(TemplateFunctions.literalHash(c), row.getField(c));
                else
                    map.put(c, row.getField(c));
            }
            dataframe.add(map);
        }
        return new ArrayList<>(dataframe);
    }

    @Override
    public void debugQuery(String query, String destinationPath) throws Exception {

    }

    @Override
    public void setVerbose(boolean verbose) {}

    /**
     * Not implemented for CSVReader yet.
     * @param outputFormat String identifying the output format
     */
    @Override
    public void setOutputFormat(String outputFormat) { return;}

    @Override
    public void setHashVariable(boolean hashVariable) {
        this.hashVariable = hashVariable;
    }

    @Override
    public void setOnlyDistinct(boolean onlyDistinct) {
        this.onlyDistinct = onlyDistinct;
    }

    @Override
    public void shutDown() {

    }
}

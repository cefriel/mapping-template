package com.cefriel.template.io.csv;

import com.cefriel.template.io.Reader;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import de.siegmar.fastcsv.reader.NamedCsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class CSVReader implements Reader {

    public NamedCsvReader document;

    public CSVReader(File file) throws IOException {
        this.document = NamedCsvReader.builder().build(file.toPath());
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

    public List<Map<String, String>> getDataframe() {
        return getDataframe("");
    }
    @Override
    public List<Map<String, String>> getDataframe(String query) {
        Set<String> headers = this.document.getHeader();
        List<Map<String, String>> output = new ArrayList<>();
        for (NamedCsvRow row : this.document) {
            HashMap<String, String> map = new HashMap<>();
            for (String header : headers) {
                map.put(header, row.getField(header));
            }
            output.add(map);
        }
        return output;
    }

    @Override
    public void debugQuery(String query, String destinationPath) throws Exception {

    }

    @Override
    public void setVerbose(boolean verbose) {

    }

    @Override
    public void shutDown() {

    }
}

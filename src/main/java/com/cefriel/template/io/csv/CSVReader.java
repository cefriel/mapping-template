package com.cefriel.template.io.csv;

import com.cefriel.template.io.Reader;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRow;
import de.siegmar.fastcsv.reader.NamedCsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRow;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class CSVReader implements Reader {

    public NamedCsvReader document;
    private boolean skipHeaderLine = false;
    // todo have to pass a file and a list of headers in the correct order
    // the headers will be used to access the data and create the map kv representation

    private CSVReader(Path filePath) throws IOException {
        this.document = NamedCsvReader.builder().build(filePath);
    }

    public CSVReader(String filePath) throws IOException {
        this(Path.of(filePath));
    }
    @Override
    public void setQueryHeader(String header) {

    }

    @Override
    public void appendQueryHeader(String s) {

    }

    @Override
    public List<Map<String, String>> executeQueryStringValue(String query) throws Exception {
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

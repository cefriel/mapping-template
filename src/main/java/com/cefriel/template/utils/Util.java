package com.cefriel.template.utils;

import com.cefriel.template.io.Reader;
import com.cefriel.template.io.csv.CSVReader;
import com.cefriel.template.io.json.JSONReader;
import com.cefriel.template.io.rdf.RDFReader;
import com.cefriel.template.io.xml.XMLReader;

import java.io.File;

public class Util {

    public static boolean validInputFormat(String format) {
        return MappingTemplateConstants.VALID_INPUT_FORMATS.contains(format);
    }

    public static Reader createReader(String filePath, String format, boolean verbose) throws Exception {
        if (validInputFormat(format)) {
            File f = new File(filePath);
            Reader reader = switch (format) {
                case "xml" -> new XMLReader(f);
                case "json" -> new JSONReader(f);
                case "csv" -> new CSVReader(f);
                case "rdf" -> new RDFReader(); // todo rewrite rdf reader
                default -> throw new IllegalArgumentException("A reader for FORMAT: " + format + " is not supported");
            };
            reader.setVerbose(verbose);
            return reader;
        }
        else throw new IllegalArgumentException("A reader for FORMAT: " + format + " is not supported");
    }


}

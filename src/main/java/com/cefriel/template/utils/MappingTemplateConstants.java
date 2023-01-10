package com.cefriel.template.utils;

import java.util.Arrays;
import java.util.HashSet;

public class MappingTemplateConstants {
    public static final HashSet<String> INPUT_FORMATS = new HashSet<>(Arrays.asList(
            "xml", "csv", "json", "rdf"));

    public static final HashSet<String> FORMATTER_FORMATS = new HashSet<>(Arrays.asList(
            "xml", "turtle", "rdfxml", "nt", "jsonld"));
}

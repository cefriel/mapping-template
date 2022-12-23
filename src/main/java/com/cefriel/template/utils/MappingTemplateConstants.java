package com.cefriel.template.utils;

import java.util.Arrays;
import java.util.HashSet;

public class MappingTemplateConstants {
    public static final HashSet<String> VALID_INPUT_FORMATS = new HashSet<>(Arrays.asList(
            "xml", "csv", "json", "rdf"));
}

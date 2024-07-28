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
package com.cefriel.template.utils;

import org.eclipse.rdf4j.model.IRI;

import java.math.BigDecimal;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RMLTemplateFunctions extends TemplateFunctions {

    private String baseIRI;

    public void setBaseIRI(String baseIRI) {
        this.baseIRI = baseIRI;
    }

    public String resolveIRI(String s) throws Exception {
        if(s != null) {
            if (!isAbsoluteURI(s)) {
                s = baseIRI + encodeURIComponent(s);
            } else {
                URLComponents url = new URLComponents(s);
                s = url.getEncodedURL();
            }

            return s;
        }
        return s;
    }

    public String resolveDatatype(String literal, String datatype) throws Exception {
        return "\"" + transformDatatypeString(literal, datatype) + "\"^^<" + resolveIRI(datatype) + ">";
    }

    public String resolveSQLDatatype(String literal, String type) {
        if (type != null) {
            String xsdType = getXsdFromSqlDatatypes(type);
            if (xsdType != null)
                return "\"" + transformDatatypeString(literal, xsdType) + "\"^^<" + xsdType + ">";
        }
        return "\"" + literal + "\"";
    }

    public String resolveLanguage(String literal, String language) {
        if(!literal.startsWith("\""))
            literal = "\"" + literal + "\"";
        if (isValidrrLanguage(language))
            return literal + "@" + language;
        else
            return literal;
    }

    // TODO Move as setOutputFormat("rdf") in SQL Reader
    /**
     * Return XSD datatype from SQL datatype
     * @param sqlDatatype SQL datatype
     * @return XSD datatype corresponding to the input SQL datatype
     */
    public static String getXsdFromSqlDatatypes(String sqlDatatype) {
        Map<String, String> map = new HashMap<>() {{
            put("DOUBLE", "http://www.w3.org/2001/XMLSchema#double");
            put("FLOAT", "http://www.w3.org/2001/XMLSchema#double");
            put("VARBINARY", "http://www.w3.org/2001/XMLSchema#hexBinary");
            put("DECIMAL", "http://www.w3.org/2001/XMLSchema#decimal");
            put("INTEGER", "http://www.w3.org/2001/XMLSchema#integer");
            put("INT", "http://www.w3.org/2001/XMLSchema#integer");
            put("BIT", "http://www.w3.org/2001/XMLSchema#boolean");
            put("BOOL", "http://www.w3.org/2001/XMLSchema#boolean");
            put("DATE", "http://www.w3.org/2001/XMLSchema#date");
            put("TIME", "http://www.w3.org/2001/XMLSchema#time");
            put("TIMESTAMP", "http://www.w3.org/2001/XMLSchema#dateTime");
            put("DATETIME", "http://www.w3.org/2001/XMLSchema#dateTime");
        }};

        if(sqlDatatype != null)
            for(String datatype : map.keySet())
                if(sqlDatatype.toUpperCase().contains(datatype))
                    return map.get(datatype);

        return null;
    }

    // From rmlmapper https://github.com/RMLio/rmlmapper-java/blob/f8d15d97efb9a30359b05f37a28328584fe62744/src/main/java/be/ugent/rml/Utils.java#L661
    public static String transformDatatypeString(String input, String datatype) {
        switch (datatype) {
            case "http://www.w3.org/2001/XMLSchema#hexBinary":
                return input;
            case "http://www.w3.org/2001/XMLSchema#decimal":
                return "" + Double.parseDouble(input);
            case "http://www.w3.org/2001/XMLSchema#integer":
                return "" + Integer.parseInt(input);
            case "http://www.w3.org/2001/XMLSchema#double":
                return formatToScientific(Double.parseDouble(input));
            case "http://www.w3.org/2001/XMLSchema#boolean":
                switch (input) {
                    case "t":
                    case "true":
                    case "TRUE":
                    case "1":
                        return "true";
                    default:
                        return "false";
                }
            case "http://www.w3.org/2001/XMLSchema#date":
                return input;
            case "http://www.w3.org/2001/XMLSchema#time":
                return input;
            case "http://www.w3.org/2001/XMLSchema#dateTime":
                return input.replace(" ", "T");
            default:
                return input;
        }
    }

    /**
     * From rmlmapper <a href="https://github.com/RMLio/rmlmapper-java/blob/f8d15d97efb9a30359b05f37a28328584fe62744/src/main/java/be/ugent/rml/Utils.java#L704">...</a>
     */
    private static String formatToScientific(Double d) {
        BigDecimal input = BigDecimal.valueOf(d).stripTrailingZeros();
        int precision = input.scale() < 0
                ? input.precision() - input.scale()
                : input.precision();
        StringBuilder s = new StringBuilder("0.0");
        for (int i = 2; i < precision; i++) {
            s.append("#");
        }
        s.append("E0");
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        DecimalFormat df = (DecimalFormat) nf;
        df.applyPattern(s.toString());
        return df.format(d);
    }

    /**
     * From rmlmapper <a href="https://github.com/RMLio/rmlmapper-java/blob/f8d15d97efb9a30359b05f37a28328584fe62744/src/main/java/be/ugent/rml/Utils.java#L385">...</a>
     */
    public static boolean isValidrrLanguage(String s) {
        Pattern regexPatternLanguageTag = Pattern.compile("^((?:(en-GB-oed|i-ami|i-bnn|i-default|i-enochian|i-hak|i-klingon|i-lux|i-mingo|i-navajo|i-pwn|i-tao|i-tay|i-tsu|sgn-BE-FR|sgn-BE-NL|sgn-CH-DE)|(art-lojban|cel-gaulish|no-bok|no-nyn|zh-guoyu|zh-hakka|zh-min|zh-min-nan|zh-xiang))|((?:([A-Za-z]{2,3}(-(?:[A-Za-z]{3}(-[A-Za-z]{3}){0,2}))?)|[A-Za-z]{4})(-(?:[A-Za-z]{4}))?(-(?:[A-Za-z]{2}|[0-9]{3}))?(-(?:[A-Za-z0-9]{5,8}|[0-9][A-Za-z0-9]{3}))*(-(?:[0-9A-WY-Za-wy-z](-[A-Za-z0-9]{2,8})+))*(-(?:x(-[A-Za-z0-9]{1,8})+))?)|(?:x(-[A-Za-z0-9]{1,8})+))$");
        return regexPatternLanguageTag.matcher(s).matches();
    }

}

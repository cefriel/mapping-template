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

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RMLTemplateFunctions extends TemplateFunctions {

    /**
     * Encode URIs using URLEncoder and replacing + with %20 and * with %2A.
     * @param url
     * @return Encoded URI
     */
    public static String encodeURI(String url) {
        if (url != null && !url.isEmpty()) {
            //TODO Improve validation of URL, objective is not to encode first part of URLs
            if (!url.contains("://")) {
                final StringBuilder builder = new StringBuilder();
                final String encoded = URLEncoder.encode(url, StandardCharsets.UTF_8);

                for (char c : encoded.toCharArray()) {
                    if (c == '+')
                        builder.append("%20");
                    else if (c == '*')
                        builder.append("%2A");
                    else
                        builder.append(c);
                }

                // TODO Check if IRISafe is needed
                return toIRISafe(builder.toString());
            }
        }
        return url;
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

    // From rmlmapper https://github.com/RMLio/rmlmapper-java/blob/f8d15d97efb9a30359b05f37a28328584fe62744/src/main/java/be/ugent/rml/Utils.java#L704
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
     * From rmlmapper https://github.com/RMLio/rmlmapper-java/blob/f8d15d97efb9a30359b05f37a28328584fe62744/src/main/java/be/ugent/rml/Utils.java#L385
     */
    public static boolean isValidrrLanguage(String s) {
        Pattern regexPatternLanguageTag = Pattern.compile("^((?:(en-GB-oed|i-ami|i-bnn|i-default|i-enochian|i-hak|i-klingon|i-lux|i-mingo|i-navajo|i-pwn|i-tao|i-tay|i-tsu|sgn-BE-FR|sgn-BE-NL|sgn-CH-DE)|(art-lojban|cel-gaulish|no-bok|no-nyn|zh-guoyu|zh-hakka|zh-min|zh-min-nan|zh-xiang))|((?:([A-Za-z]{2,3}(-(?:[A-Za-z]{3}(-[A-Za-z]{3}){0,2}))?)|[A-Za-z]{4})(-(?:[A-Za-z]{4}))?(-(?:[A-Za-z]{2}|[0-9]{3}))?(-(?:[A-Za-z0-9]{5,8}|[0-9][A-Za-z0-9]{3}))*(-(?:[0-9A-WY-Za-wy-z](-[A-Za-z0-9]{2,8})+))*(-(?:x(-[A-Za-z0-9]{1,8})+))?)|(?:x(-[A-Za-z0-9]{1,8})+))$");
        return regexPatternLanguageTag.matcher(s).matches();
    }

    /**
     * From BURP <a href="https://github.com/kg-construct/BURP/blob/be44513f16b03c67bb414edc926219bc8ce614a3/src/main/java/burp/util/Util.java">...</a>
     *
     * Translate a string into its IRI safe value as per R2RML's steps
     *
     * @param string
     * @return
     */
    public static String toIRISafe(String string) {
        // The IRI-safe version of a string is obtained by applying the following
        // transformation to any character that is not in the iunreserved
        // production in [RFC3987].
        StringBuffer sb = new StringBuffer();
        for(char c : string.toCharArray()) {
            if(inIUNRESERVED(c)) sb.append(c);
            else sb.append('%' + Integer.toHexString((int) c).toUpperCase());
        }
        return sb.toString();
    }

    /**
     *  From BURP https://github.com/kg-construct/BURP/blob/be44513f16b03c67bb414edc926219bc8ce614a3/src/main/java/burp/util/Util.java
     *
     *	Check whether the characters are part of iunreserved as per
     *  https://tools.ietf.org/html/rfc3987#section-2.2
     */
    private static boolean inIUNRESERVED(char c) {
        if("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~".indexOf(c) != -1) return true;
        else if (c >= 160 && c <= 55295) return true;
        else if (c >= 63744 && c <= 64975) return true;
        else if (c >= 65008 && c <= 65519) return true;
        else if (c >= 65536 && c <= 131069) return true;
        else if (c >= 131072 && c <= 196605) return true;
        else if (c >= 196608 && c <= 262141) return true;
        else if (c >= 262144 && c <= 327677) return true;
        else if (c >= 327680 && c <= 393213) return true;
        else if (c >= 393216 && c <= 458749) return true;
        else if (c >= 458752 && c <= 524285) return true;
        else if (c >= 524288 && c <= 589821) return true;
        else if (c >= 589824 && c <= 655357) return true;
        else if (c >= 655360 && c <= 720893) return true;
        else if (c >= 720896 && c <= 786429) return true;
        else if (c >= 786432 && c <= 851965) return true;
        else if (c >= 851968 && c <= 917501) return true;
        else if (c >= 921600 && c <= 983037) return true;
        return false;
    }

}

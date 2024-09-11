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

package com.cefriel.template;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TemplateMap extends HashMap<String, String> {
    private static final Logger log = LoggerFactory.getLogger(TemplateMap.class);

    public TemplateMap(Map<String, String> map) {
        putAll(map);
    }

    public TemplateMap(Path filePath, boolean isCsv) throws IOException {
        Stream<String> lines = Files.lines(filePath);
        if (isCsv)
            putAll(parseCsvMap(lines));
        else
            putAll(parseMap(lines));
        log.info(filePath + " parsed");
        log.info("Parsed " + this.size() + " key-value pairs");
    }

    public TemplateMap(InputStream is, boolean isCsv) {
        Stream<String> lines = new BufferedReader(new InputStreamReader(is,
                StandardCharsets.UTF_8)).lines();
        if (isCsv)
            putAll(parseCsvMap(lines));
        else
            putAll(parseMap(lines));
        log.info("TEMPLATE-MAP stream parsed");
        log.info("Parsed " + this.size() + " key-value pairs");
    }
    private Map<String,String> parseMap(Stream<String> lines) {
        return lines.filter(s -> s.matches("^(.+?):.+"))
                .collect(Collectors.toMap(k -> k.split(":")[0], v -> v.substring(v.indexOf(":") + 1)));
    }

    private Map<String,String> parseCsvMap(Stream<String> lines) {
        List<String[]> fromCSV = lines
                .map(s -> s.split(","))
                .collect(Collectors.toList());
        Map<String,String> mapFromFile = new HashMap<>();
        String[] keys = fromCSV.get(0);
        String[] values = fromCSV.get(1);
        for(int i = 0; i < keys.length; i++)
            mapFromFile.put(keys[i], values[i]);
        return mapFromFile;
    }

}

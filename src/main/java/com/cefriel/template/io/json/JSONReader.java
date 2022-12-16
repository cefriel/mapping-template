/*
 * Copyright (c) 2019-2022 Cefriel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.cefriel.template.io.json;

import com.cefriel.template.io.Reader;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class JSONReader implements Reader {

    private final Logger log = LoggerFactory.getLogger(JSONReader.class);
    Object document;

    public JSONReader(String json) throws FileNotFoundException {
        Configuration conf = Configuration.defaultConfiguration()
                .addOptions(Option.ALWAYS_RETURN_LIST);

        document = conf.jsonProvider().parse(json);
    }

    public JSONReader(File file) throws IOException {
        Configuration conf = Configuration.defaultConfiguration()
                .addOptions(Option.ALWAYS_RETURN_LIST);

        document = conf.jsonProvider().parse(Files
                .readString(Paths.get(file.getPath())));
    }
    @Override
    public void setQueryHeader(String header) {}

    @Override
    public void appendQueryHeader(String s) {}

    @Override
    public List<Map<String, String>> getDataframe(String query) throws Exception {
        Object queryDoc = Configuration.defaultConfiguration().jsonProvider().parse(query);
        String iterator = JsonPath.read(queryDoc, "$.iterator");
        Set<String> keys = JsonPath.read(queryDoc, "$.paths.keys()");
        List<Map<String, String>> output = new ArrayList<>();
        try {
            List<String> results = JsonPath.read(document, iterator);
            for (int i=0; i< results.size(); i++)
                output.add(new HashMap<>());
            for(String key : keys) {
                String path = JsonPath.read(queryDoc, "$.paths." + key);
                List<Object> objects = JsonPath.read(document, iterator + "." + path);
                for(int i=0; i< objects.size(); i++)
                    output.get(i).put(key, objects.get(i).toString());
            }
        } catch (Exception e) {
            Map<String, String> map = new HashMap<>();
            for(String key : keys) {
                String path = JsonPath.read(queryDoc, "$.paths." + key);
                Object object = JsonPath.read(document, iterator + "." + path);
                map.put(key, object.toString());
            }
            output.add(map);
        }
        return output;
    }

    @Override
    public List<Map<String, String>> getDataframe() throws Exception {
        return null;
    }

    @Override
    public void debugQuery(String query, String destinationPath) throws Exception {
        log.warn("Debug operation not implemented");
    }

    @Override
    public void setVerbose(boolean verbose) {
        log.warn("Verbose option not implemented");
    }

    @Override
    public void shutDown() {}
}

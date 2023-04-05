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
import com.jayway.jsonpath.PathNotFoundException;
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
        if (Files.exists(file.toPath())) {
            Configuration conf = Configuration.defaultConfiguration()
                    .addOptions(Option.ALWAYS_RETURN_LIST);

            document = conf.jsonProvider().parse(Files.readString(Paths.get(file.getPath())));
        } else
            throw new IllegalArgumentException("FILE: " + file.getPath() + " FOR JSONREADER DOES NOT EXIST");

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
        // config to get results as jsonPaths
        Configuration conf = Configuration.builder().options(Option.AS_PATH_LIST).build();
        try {
            // Extract paths for each node identified by the iterator
            List<String> results = JsonPath.using(conf).parse(document).read(iterator);
            for (int i=0; i< results.size(); i++)
                output.add(new HashMap<>());
            for(String key : keys) {
                String path = JsonPath.read(queryDoc, "$.paths." + key);
                List<Object> objects = JsonPath.read(document, iterator + "." + path);

                // CASE 1, all the nodes identified by the iterator have the key (sub field)
                // A single query to extract all values for the key
                if(objects.size() == results.size()) {
                    for(int i=0; i< objects.size(); i++) {
                        String value = objects.get(i) == null ? "null" : objects.get(i).toString();
                        output.get(i).put(key, value);
                    }
                }
                // CASE 2, not all nodes have the key (sub field)
                // For each node a query is executed to get the value for the key (sub field)
                // The subquery is composed of the jsonpath for the node and the specific key (sub field)
                else {
                    for(int i=0; i< results.size();i++) {
                        String topPath = results.get(i);
                        try {
                            var x = JsonPath.read(document, topPath + "." + path);
                            var value = x == null ? "null" : x.toString();
                            output.get(i).put(key, value);
                        } catch (PathNotFoundException pe) {
                            // what happens when the path is not found? i.e. the item does not have the field the jsonPath is pointing to
                            // for now we do not put the key in the map
                            log.warn("PATH NOT FOUND: " + topPath + "." + path);
                        }
                    }
                }
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

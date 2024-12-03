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

package com.cefriel.template.io.json;

import com.cefriel.template.io.Formatter;
import com.jayway.jsonpath.InvalidJsonException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;


public class JSONFormatter implements Formatter {

    @Override
    public void formatFile(String filepath) throws Exception {
        Path path = Paths.get(filepath);
        String jsonContent = new String(Files.readAllBytes(path));
        String prettyJsonString = formatJSON(jsonContent);
        Files.write(path, prettyJsonString.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Override
    public String formatString(String s) throws Exception {
        return formatJSON(s);
    }

    public String formatJSON(String json) throws IOException {
        // Parse the JSON string
        json = json.trim(); // Trim leading and trailing whitespace

        // If it's a JSON Object
        if (json.startsWith("{")) {
            JSONObject jsonObject = new JSONObject(json);
            return jsonObject.toString(4); // Indent with 4 spaces
        }
        // If it's a JSON Array
        else if (json.startsWith("[")) {
            JSONArray jsonArray = new JSONArray(json);
            return jsonArray.toString(4); // Indent with 4 spaces
        }

        else {
            throw new InvalidJsonException();
        }

    }
}

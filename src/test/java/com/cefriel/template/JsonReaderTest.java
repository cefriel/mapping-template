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

import com.cefriel.template.io.json.JSONReader;
import com.cefriel.template.utils.TemplateFunctions;
import com.cefriel.template.utils.Util;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class JsonReaderTest {

    @Test
    public void jsonTest() throws Exception {
        JSONReader reader = (JSONReader) Util.createReader("json", List.of(Paths.get("src/test/resources/json/example.json")), null, null, null, null, null, null);
        TemplateExecutor executor = new TemplateExecutor();
        Path template = Paths.get("src/test/resources/json/template.vm");

        String result = executor.executeMapping(reader, template, false, false,null, null, new TemplateFunctions());
        String expectedOutput = Files.readString(Paths.get("src/test/resources/json/correct-output.ttl"));

        expectedOutput = expectedOutput.replaceAll("\\r\\n", "\n");
        result = result.replaceAll("\\r\\n", "\n");

        assert(expectedOutput.equals(result));
    }
}


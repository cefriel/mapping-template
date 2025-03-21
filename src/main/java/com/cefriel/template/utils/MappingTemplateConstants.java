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

import java.util.Arrays;
import java.util.HashSet;

public class MappingTemplateConstants {
    public static final HashSet<String> INPUT_FORMATS = new HashSet<>(Arrays.asList(
            "xml", "csv", "json", "rdf", "mysql", "postgresql"));

    public static final HashSet<String> FORMATTER_FORMATS = new HashSet<>(Arrays.asList(
            "xml", "json", "turtle", "rdfxml", "nt", "nq", "jsonld"));
}

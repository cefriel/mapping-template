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

package com.cefriel.template.io.csv;

import com.cefriel.template.io.Reader;
import com.cefriel.template.utils.TemplateFunctions;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

import java.nio.file.Path;
import java.util.*;

public abstract class CSVReaderAbstract implements Reader {

    List<String> headers;
    String outputFormat;
    boolean hashVariable;
    boolean onlyDistinct;

    @Override
    public void setQueryHeader(String header) {}

    @Override
    public void appendQueryHeader(String s) {}

    public abstract List<Map<String, String>> getDataframe() throws Exception ;

    @Override
    public List<Map<String, String>> getDataframe(String query) throws Exception {
        String[] columns = query.split(",");
        return getDataframe(columns);
    }

    public abstract List<Map<String, String>> getDataframe(String... columns) throws Exception ;

    @Override
    public void debugQuery(String query, Path destinationPath) throws Exception {}

    @Override
    public void setVerbose(boolean verbose) {}

    @Override
    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    @Override
    public void setHashVariable(boolean hashVariable) {
        this.hashVariable = hashVariable;
    }

    @Override
    public void setOnlyDistinct(boolean onlyDistinct) {
        this.onlyDistinct = onlyDistinct;
    }

    @Override
    public void shutDown() {}
}

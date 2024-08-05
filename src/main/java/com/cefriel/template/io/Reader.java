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

package com.cefriel.template.io;

import java.util.List;
import java.util.Map;

public interface Reader {

    /**
     * Add header prepended to each query executed through the Reader.
     * @param header The string to be prepended.
     */
    void setQueryHeader(String header);

    /**
     * Append a string to the header to be prepended to each query executed through the Reader.
     * @param s The string to be appended to the current header.
     */
    void appendQueryHeader(String s);

    /**
     * Execute the query returning a {@link java.util.List} of {@link java.util.Map}{@code <String,String>}.
     * Each map in the list represents a row of the results returned executing the query. Each key
     * in the map is bound to the value returned for the considered row.
     * @param query The query to be executed
     * @return The result of the query
     * @throws Exception
     */
    List<Map<String, String>> getDataframe(String query) throws Exception;
    List<Map<String, String>> getDataframe() throws Exception;

    /**
     * Execute the query saving a TSV representation of the result as a file
     * at {@code destinationPath}.
     * @param query The query to be executed
     * @param destinationPath The path of the destination file
     * @throws Exception
     */
    void debugQuery(String query, String destinationPath) throws Exception;

    /**
     * If {@code verbose} is set, debug information about executed queries are logged
     * by the Reader.
     * @param verbose
     */
    void setVerbose(boolean verbose);

    /**
     * If {@code outputFormat} is set the values returned in a Dataframe are escaped accordingly.
     * @param outputFormat String identifying the output format
     */
    void setOutputFormat(String outputFormat);

    /**
     * If {@code hashVariable} is set the variables returned in a Dataframe are hashed according to
     * {@link com.cefriel.template.utils.TemplateFunctions#literalHash(String)}.
     * @param hashVariable
     */
    void setHashVariable(boolean hashVariable);

    /**
     * Shutdown the Reader.
     */
    void shutDown();

}

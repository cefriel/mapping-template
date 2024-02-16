/*
 * Copyright (c) 2019-2024 Cefriel.
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

package com.cefriel.template.io.sql;

import com.cefriel.template.io.Reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.*;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQLReader implements Reader {

    private final Logger log = LoggerFactory.getLogger(SQLReader.class);

    private Connection conn;

    private String queryHeader;
    private boolean verbose;
    private static final Object lock = new Object();


    public SQLReader(String driver,  String url, String database, String username, String password) {
        if (!url.contains("jdbc:"))
            url = "jdbc:" + driver + "://" + url + "/" + database;
        log.info("Connection to database with URL: " + url);

        try {
            conn = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    public SQLReader(Connection conn) {
        this.conn = conn;
    }

    /**
     * Executes a SQL query returning a {@code ResultSet}.
     * @param query SQL query to be executed
     * @return ResultSet for the SQL query executed
     */
    public ResultSet executeQuery(String query) {

        ResultSet resultSet = null;

        if (queryHeader != null) {
            query = queryHeader + query;
        }
        if (verbose) {
            log.info("Query executed: \n" + query);
        }

        try {
            synchronized (lock) {
                Statement statement = conn.createStatement();
                resultSet = statement.executeQuery(query);
            }

            return resultSet;

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }

        return null;

    }

    /**
     * Executes a SQL query returning a list of rows as {@code List<Map<String,String>>}.
     * @param query SQL query to be executed
     * @return Result of the SQL query
     */
    public List<Map<String, String>> getDataframe(String query) {

        List<Map<String, String>> dataframe = new ArrayList<>();

        try (ResultSet resultSet = executeQuery(query)) {
            int columnCount = resultSet.getMetaData().getColumnCount();

            while (resultSet.next()) {
                Map<String, String> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = resultSet.getMetaData().getColumnName(i);
                    String columnValue = resultSet.getString(i);
                    row.put(columnName, columnValue);
                }
                dataframe.add(row);
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }

        return dataframe;

    }

    @Override
    public List<Map<String, String>> getDataframe() throws Exception {
        return null;
    }

    /**
     * Executes the SQL query in the {@code query} file writing the results in the CSV
     * format in {@code destinationPath}.
     * @param query SQL query to be executed
     * @param destinationPath File to save the results of the SQL query
     * @throws IOException If an error occurs in handling the files
     */
    public void debugQuery(String query, String destinationPath) throws IOException {
        try (ResultSet resultSet = executeQuery(query)) {
            try (FileWriter writer = new FileWriter(destinationPath)) {
                int columnCount = resultSet.getMetaData().getColumnCount();

                for (int i = 1; i <= columnCount; i++) {
                    writer.append(resultSet.getMetaData().getColumnName(i));
                    if (i < columnCount) {
                        writer.append(',');
                    }
                }
                writer.append('\n');

                // Write data rows
                while (resultSet.next()) {
                    for (int i = 1; i <= columnCount; i++) {
                        writer.append(resultSet.getString(i));
                        if (i < columnCount) {
                            writer.append(',');
                        }
                    }
                    writer.append('\n');
                }
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void shutDown() {
        try {
            // Closing connection in finally block
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void setQueryHeader(String header) {
        this.queryHeader = header;
    }

    @Override
    public void appendQueryHeader(String s) {
        this.queryHeader += s;
    }

    @Override
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Not implemented for JSONReader yet.
     * @param outputFormat String identifying the output format
     */
    @Override
    public void setOutputFormat(String outputFormat) { return;}

}

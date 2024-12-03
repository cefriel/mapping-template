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
import com.cefriel.template.utils.TemplateFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.sql.*;
import java.util.*;

public class SQLReader implements Reader {

    private final Logger log = LoggerFactory.getLogger(SQLReader.class);
  
    private Connection conn;
    private List<String> tables = new ArrayList<>();
    private String queryHeader;
    private boolean verbose;

    private static final Object lock = new Object();

    private String outputFormat;
    private boolean hashVariable;
    private boolean onlyDistinct;
    private boolean useDoubleQuotes;

    public SQLReader(String jdbcDSN, String username, String password) {
        if (!jdbcDSN.contains("jdbc:"))
            jdbcDSN = "jdbc:" + jdbcDSN;

        log.info("Connection to database: " + jdbcDSN);

        try {
            conn = DriverManager.getConnection(jdbcDSN, username, password);
            PreparedStatement tablesQueryStatement;

            // Prepare and execute table query based on the database driver
            if (jdbcDSN.contains("mysql")) {
                tablesQueryStatement = conn.prepareStatement("SELECT table_name FROM information_schema.tables WHERE table_schema = ?;");
                // TODO Check if it is a correct assumption to extract the database. For sure it is true if the other Constructor is used.
                String[] parts = jdbcDSN.split("/");
                String database = parts[parts.length - 1];
                tablesQueryStatement.setString(1, database);
            } else if (jdbcDSN.contains("postgresql")) {
                useDoubleQuotes = true;
                tablesQueryStatement = conn.prepareStatement("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE'");
            } else {
                throw new IllegalArgumentException("SQLReader does not support the driver indicated " + jdbcDSN);
            }

            ResultSet resultSet = tablesQueryStatement.executeQuery();
            while (resultSet.next()) {
                String tableName = resultSet.getString("table_name");
                this.tables.add(tableName);
            }
        } catch (SQLException e) {
            log.error("Error connecting to the database: " + e.getMessage(), e);
        }
   }

    public SQLReader(String driver, String url, String database, String username, String password) {
        this(driver + "://" + url + "/" + database, username, password);
    }

    public SQLReader(Connection conn) {
        this.conn = conn;
    }

    /**
     * Executes a SQL query returning a {@code ResultSet}.
     *
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

    private List<Map<String, String>> populateDataframe(int rowCount, ResultSet resultSet, String filterVariables) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        Collection<Map<String,String>> dataframe;
        if (onlyDistinct)
            dataframe = new ArrayList<>(rowCount);
        else
            dataframe = new HashSet<>(rowCount);

        List<String> filters = null;
        if (filterVariables != null)
            filters = Arrays.asList(filterVariables.split(","));

        while (resultSet.next()) {
            Map<String, String> row = new HashMap<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                int columnType = metaData.getColumnType(i);
                String columnValue;
                if (columnType == Types.BINARY || columnType == Types.VARBINARY) {
                    byte[] binaryData = resultSet.getBytes(i);
                    columnValue = bytesToHex(binaryData);
                } else
                    columnValue = resultSet.getString(i);
                if(filters == null || filters.contains(columnName))
                    if (hashVariable)
                        row.put(TemplateFunctions.literalHash(columnName), columnValue);
                    else
                        row.put(columnName, columnValue);
            }
            dataframe.add(row);
        }

        return new ArrayList<>(dataframe);
    }
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X", b));
        }
        return hexString.toString();
    }

    private Map<String, String> populateColumnTypes(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        Map<String, String> columnTypeName = new HashMap<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            columnTypeName.put(metaData.getColumnLabel(i),metaData.getColumnTypeName(i));
        }

        return columnTypeName;
    }

    /**
     * Executes a SQL query returning the types of the columns in the result as {@code Map<String,String>}.
     *
     * @param query SQL query to be executed
     * @return Types of the columns in the result
     */
    public Map<String, String> getColumnTypes(String query) {
        Map<String, String> columnTypes = null;
        String queryCheck = query.toLowerCase();

        if (!queryCheck.contains("select")) {
            if (tables.contains(query))
                query = "SELECT * FROM " + query;
            else
                throw new InvalidParameterException("Table " + query + " does not exist.");
        }
        
        try (ResultSet resultSet = executeQuery(query)) {
            columnTypes = populateColumnTypes(resultSet);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
            
        return columnTypes;
    }

    /**
     * Executes a SQL query returning a list of rows as {@code List<Map<String,String>>}.
     *
     * @param query SQL query to be executed
     * @return Result of the SQL query
     */
    public List<Map<String, String>> getDataframe(String query) {
        return getFilteredDataFrame(query, null);
    }

    /**
     * Executes a SQL query returning a list of rows as {@code List<Map<String,String>>} filtered
     * by the variables provided as comma-separated list.
     *
     * @param query SQL query to be executed
     * @return Result of the SQL query
     */
    public List<Map<String, String>> getFilteredDataFrame(String query, String filterVariables) {
        String queryCheck = query.toLowerCase();
        List<Map<String, String>> dataframe = new ArrayList<>();

        if (!queryCheck.contains("select")) {
            if (tables.contains(query)) {
                if (filterVariables != null) {
                    List<String> filters = Arrays.asList(filterVariables.split(","));
                    if (useDoubleQuotes)
                        filters.replaceAll(s -> "\"" + s + "\"");
                    else
                        filters.replaceAll(s -> "`" + s + "`");
                    String selectVariables = String.join(",",filters);
                    query = "SELECT DISTINCT " + selectVariables + " FROM " + query;
                    filterVariables = null;
                } else
                    query = "SELECT * FROM " + query;
            }
            else
               throw new InvalidParameterException("Table " + query + " does not exist.");
        }
        
        int rowCount = getRowCount(query);

        try (ResultSet resultSet = executeQuery(query)) {
            dataframe = populateDataframe(rowCount, resultSet, filterVariables);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        
        return dataframe;
    }

    private int getRowCount(String query) {
        int fromIndex = query.toUpperCase().indexOf("FROM");
        String countQuery = "SELECT COUNT(*) " + query.substring(fromIndex);
        try (ResultSet resultSet = executeQuery(countQuery)) {
            if (resultSet.next())
                return resultSet.getInt(1);
            else
                return 0;
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return 0;
    }

    @Override
    public List<Map<String, String>> getDataframe() throws Exception {
        return null;
    }

    private void queryResultToWriter(Writer writer, ResultSet resultSet) throws SQLException, IOException {
        int columnCount = resultSet.getMetaData().getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            writer.append(resultSet.getMetaData().getColumnLabel(i));
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

    /**
     * Executes the SQL query in the {@code query} file writing the results in the CSV
     * format in {@code destinationPath}.
     *
     * @param query           SQL query to be executed
     * @param destinationPath File to save the results of the SQL query
     * @throws IOException If an error occurs in handling the files
     */
    public void debugQuery(String query, Path destinationPath) throws IOException {
        String queryCheck = query.toLowerCase();
        if (queryCheck.contains("select")) {
            try (ResultSet resultSet = executeQuery(query)) {
                try (BufferedWriter writer = new BufferedWriter(Files.newBufferedWriter(destinationPath))) {
                    queryResultToWriter(writer, resultSet);
                }
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        } else {
            if (tables.contains(query)) {
                String q = "SELECT * FROM " + query;
                try {
                    PreparedStatement preparedStatement = conn.prepareStatement(q);
                    ResultSet resultSet = preparedStatement.executeQuery();
                    try (BufferedWriter writer = new BufferedWriter(Files.newBufferedWriter(destinationPath))) {
                        queryResultToWriter(writer, resultSet);
                    }
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                throw new InvalidParameterException("Table " + query + " does not exist.");
            }
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
     * Implemented for RDF type conversion
     * @param outputFormat String identifying the output format
     */
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

}

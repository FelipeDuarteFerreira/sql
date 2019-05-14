/*
 *   Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package com.amazon.opendistroforelasticsearch.sql.esintgtest;

import org.elasticsearch.client.Request;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;


/**
 * The following are tests for SHOW/DESCRIBE query support under Pretty Format Response protocol using JDBC format.
 *
 * Unlike SELECT queries, the JDBC format response of SHOW and DESCRIBE queries has determined "schema" fields.
 *
 * Since these integration tests are receiving the JSON response as output, "datarows" values can't be validated by
 * key since it is a JSONArray, so the expected length of "schema" will be used instead as well as the expected
 * position of the field data in "datarows".
 *
 * These are the outputs of "schema" for SHOW and DESCRIBE, the position of the value in "datarows" will match the
 * position of the field in "schema":
 *
 * 1) SHOW query (based on the getTables() method listed here https://docs.oracle.com/javase/8/docs/api/java/sql/DatabaseMetaData.html)
 *    "schema": [
 *     {
 *       "name": "TABLE_CAT",
 *       "type": "keyword"
 *     },
 *     {
 *       "name": "TABLE_SCHEM",
 *       "type": "keyword"
 *     },
 *     {
 *       "name": "TABLE_NAME",
 *       "type": "keyword"
 *     },
 *     {
 *       "name": "TABLE_TYPE",
 *       "type": "keyword"
 *     },
 *     {
 *       "name": "REMARKS",
 *       "type": "keyword"
 *     },
 *     {
 *       "name": "TYPE_CAT",
 *       "type": "keyword"
 *     },
 *     {
 *       "name": "TYPE_SCHEM",
 *       "type": "keyword"
 *     },
 *     {
 *       "name": "TYPE_NAME",
 *       "type": "keyword"
 *     },
 *     {
 *       "name": "SELF_REFERENCING_COL_NAME",
 *       "type": "keyword"
 *     },
 *     {
 *       "name": "REF_GENERATION",
 *       "type": "keyword"
 *     }
 *   ]
 *
 * 2) DESCRIBE query (based on the getColumns() method listed here https://docs.oracle.com/javase/8/docs/api/java/sql/DatabaseMetaData.html)
 *    "schema": [
 *     {
 *       "name": "TABLE_CAT",
 *       "type": "keyword"
 *     },
 *     {
 *       "name": "TABLE_SCHEM",
 *       "type": "keyword"
 *     },
 *     {
 *       "name": "TABLE_NAME",
 *       "type": "keyword"
 *     },
 *     {
 *       "name": "COLUMN_NAME",
 *       "type": "keyword"
 *     },
 *     {
 *       "name": "DATA_TYPE",
 *       "type": "integer"
 *     },
 *     {
 *       "name": "TYPE_NAME",
 *       "type": "keyword"
 *     },
 *     {
 *       "name": "COLUMN_SIZE",
 *       "type": "integer"
 *     },
 *     {
 *       "name": "BUFFER_LENGTH",
 *       "type": "integer"
 *     },
 *     {
 *       "name": "DECIMAL_DIGITS",
 *       "type": "integer"
 *     },
 *     {
 *       "name": "NUM_PREC_RADIX",
 *       "type": "integer"
 *     },
 *     {
 *       "name": "NULLABLE",
 *       "type": "integer"
 *     },
 *     {
 *       "name": "REMARKS",
 *       "type": "keyword"
 *     },
 *     {
 *       "name": "COLUMN_DEF",
 *       "type": "keyword"
 *     },
 *     {
 *       "name": "SQL_DATA_TYPE",
 *       "type": "integer"
 *     },
 *     {
 *       "name": "SQL_DATETIME_SUB",
 *       "type": "integer"
 *     },
 *     {
 *       "name": "CHAR_OCTET_LENGTH",
 *       "type": "integer"
 *     },
 *     {
 *       "name": "ORDINAL_POSITION",
 *       "type": "integer"
 *     },
 *     {
 *       "name": "IS_NULLABLE",
 *       "type": "keyword"
 *     },
 *     {
 *       "name": "SCOPE_CATALOG",
 *       "type": "keyword"
 *     },
 *     {
 *       "name": "SCOPE_SCHEMA",
 *       "type": "keyword"
 *     },
 *     {
 *       "name": "SCOPE_TABLE",
 *       "type": "keyword"
 *     },
 *     {
 *       "name": "SOURCE_DATA_TYPE",
 *       "type": "short"
 *     },
 *     {
 *       "name": "IS_AUTOINCREMENT",
 *       "type": "keyword"
 *     },
 *     {
 *       "name": "IS_GENERATEDCOLUMN",
 *       "type": "keyword"
 *     }
 *   ]
 */
public class MetaDataQueriesIT extends SQLIntegTestCase {

    // Number of fields in the response, assuming no filters, based on output shown above
    private static final int SHOW_FIELD_LENGTH = 10;

    private static final int DESCRIBE_FIELD_LENGTH = 24;

    private static final String TABLE_TYPE = "BASE TABLE";

    @Override
    protected void init() throws Exception {
        loadIndex(Index.ACCOUNT);
        loadIndex(Index.PHRASE);
        loadIndex(Index.GAME_OF_THRONES);
    }

    @Override
    protected Request getSqlRequest(String request, boolean explain) {
        Request sqlRequest = super.getSqlRequest(request, explain);
        sqlRequest.addParameter("format", "jdbc");

        return sqlRequest;
    }

    @Test
    public void showSingleIndex() throws IOException {
        JSONObject response = executeQuery(String.format("SHOW TABLES LIKE %s", TestsConstants.TEST_INDEX_ACCOUNT));

        String[] fields = {"TABLE_CAT", "TABLE_NAME", "TABLE_TYPE"};
        checkContainsColumns(getSchema(response), fields);

        JSONArray dataRows = getDataRows(response);
        assertThat(dataRows.length(), equalTo(1));
        assertThat(dataRows.getJSONArray(0).length(), equalTo(SHOW_FIELD_LENGTH));

        /*
         * Assumed indices of fields in dataRows based on "schema" output for SHOW given above:
         * "TABLE_CAT"  : 0
         * "TABLE_NAME" : 2
         * "TABLE_TYPE" : 3
         */
        JSONArray row = dataRows.getJSONArray(0);
        assertThat(row.get(0), equalTo(getClusterName()));
        assertThat(row.get(2), equalTo(TestsConstants.TEST_INDEX_ACCOUNT));
        assertThat(row.get(3), equalTo(TABLE_TYPE));
    }

    @Test
    public void showCaseSensitivityCheck() throws IOException {
        JSONObject response = executeQuery(String.format("show tables like %s", TestsConstants.TEST_INDEX_ACCOUNT));

        String[] fields = {"TABLE_CAT", "TABLE_NAME", "TABLE_TYPE"};
        checkContainsColumns(getSchema(response), fields);

        JSONArray dataRows = getDataRows(response);
        assertThat(dataRows.length(), equalTo(1));
        assertThat(dataRows.getJSONArray(0).length(), equalTo(SHOW_FIELD_LENGTH));

        JSONArray row = dataRows.getJSONArray(0);
        assertThat(row.get(0), equalTo(getClusterName()));
        assertThat(row.get(2), equalTo(TestsConstants.TEST_INDEX_ACCOUNT));
        assertThat(row.get(3), equalTo(TABLE_TYPE));
    }

    @Test
    public void showWildcardIndex() throws IOException {
        JSONObject response = executeQuery(String.format("SHOW TABLES LIKE %s%%", TestsConstants.TEST_INDEX));

        String pattern = String.format("%s.*", TestsConstants.TEST_INDEX);
        JSONArray dataRows = getDataRows(response);
        assertThat(dataRows.length(), equalTo(3));
        for (int i = 0; i < dataRows.length(); i++) {
            JSONArray row = dataRows.getJSONArray(i);
            String tableName = row.getString(2);

            assertTrue(tableName.matches(pattern));
        }
    }

    @Test
    public void describeSingleIndex() throws IOException {
        JSONObject response = executeQuery(String.format("DESCRIBE TABLES LIKE %s", TestsConstants.TEST_INDEX_ACCOUNT));

        // Schema for DESCRIBE is filled with a lot of fields that aren't used so only the important
        // ones are checked for here
        String[] fields = {"TABLE_NAME", "COLUMN_NAME", "TYPE_NAME"};
        checkContainsColumns(getSchema(response), fields);

        JSONArray dataRows = getDataRows(response);
        assertThat(dataRows.length(), greaterThan(0));
        assertThat(dataRows.getJSONArray(0).length(), equalTo(DESCRIBE_FIELD_LENGTH));

        /*
         * Assumed indices of fields in dataRows based on "schema" output for DESCRIBE given above:
         * "TABLE_NAME"  : 2
         * "COLUMN_NAME" : 3
         * "TYPE_NAME"   : 5
         */
        JSONArray row = dataRows.getJSONArray(0);
        assertThat(row.get(2), equalTo(TestsConstants.TEST_INDEX_ACCOUNT));
        assertThat(row.get(3), not(equalTo(JSONObject.NULL)));
        assertThat(row.get(5), not(equalTo(JSONObject.NULL)));
    }

    @Test
    public void describeCaseSensitivityCheck() throws IOException {
        JSONObject response = executeQuery(String.format("describe tables like %s", TestsConstants.TEST_INDEX_ACCOUNT));

        String[] fields = {"TABLE_NAME", "COLUMN_NAME", "TYPE_NAME"};
        checkContainsColumns(getSchema(response), fields);

        JSONArray dataRows = getDataRows(response);
        assertThat(dataRows.length(), greaterThan(0));
        assertThat(dataRows.getJSONArray(0).length(), equalTo(DESCRIBE_FIELD_LENGTH));

        JSONArray row = dataRows.getJSONArray(0);
        assertThat(row.get(2), equalTo(TestsConstants.TEST_INDEX_ACCOUNT));
        assertThat(row.get(3), not(equalTo(JSONObject.NULL)));
        assertThat(row.get(5), not(equalTo(JSONObject.NULL)));
    }

    @Test
    public void describeWildcardIndex() throws IOException {
        JSONObject response = executeQuery(String.format("DESCRIBE TABLES LIKE %s%%", TestsConstants.TEST_INDEX));

        String pattern = String.format("%s.*", TestsConstants.TEST_INDEX);
        JSONArray dataRows = getDataRows(response);
        assertThat(dataRows.length(), greaterThan(0));
        for (int i = 0; i < dataRows.length(); i++) {
            JSONArray row = dataRows.getJSONArray(i);
            String tableName = row.getString(2);

            assertTrue(tableName.matches(pattern));
        }
    }

    @Test
    public void describeWildcardColumn() throws IOException {
        JSONObject response = executeQuery(String.format("DESCRIBE TABLES LIKE %s COLUMNS LIKE %%name",
                                TestsConstants.TEST_INDEX_ACCOUNT));

        String pattern = ".*name";
        JSONArray dataRows = getDataRows(response);
        assertThat(dataRows.length(), greaterThan(0));
        for (int i = 0; i < dataRows.length(); i++) {
            JSONArray row = dataRows.getJSONArray(i);
            String columnName = row.getString(3);

            assertTrue(columnName.matches(pattern));
        }
    }

    @Test
    public void describeSingleCharacterWildcard() throws IOException {
        JSONObject response = executeQuery(String.format("DESCRIBE TABLES LIKE %s COLUMNS LIKE %%na_e",
                                TestsConstants.TEST_INDEX_ACCOUNT));

        String pattern = ".*na.e";
        JSONArray dataRows = getDataRows(response);
        assertThat(dataRows.length(), greaterThan(0));
        for (int i = 0; i < dataRows.length(); i++) {
            JSONArray row = dataRows.getJSONArray(i);
            String columnName = row.getString(3);

            assertTrue(columnName.matches(pattern));
        }
    }

    private JSONArray getSchema(JSONObject jdbcResponse) { return jdbcResponse.getJSONArray("schema"); }

    private JSONArray getDataRows(JSONObject jdbcResponse) {
        return jdbcResponse.getJSONArray("datarows");
    }

    private String getClusterName() {
        return this.admin().cluster()
                .prepareHealth()
                .get()
                .getClusterName();
    }

    private void checkContainsColumns(JSONArray schema, String... fields) {
        List<String> columnNames = new ArrayList<>();
        for (int i = 0; i < schema.length(); i++) {
            JSONObject column = schema.getJSONObject(i);
            columnNames.add(column.getString("name"));
        }

        assertThat(columnNames, hasItems(fields));
    }
}
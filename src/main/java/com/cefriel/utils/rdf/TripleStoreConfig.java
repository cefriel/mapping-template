/*
 * Copyright 2020 Cefriel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cefriel.utils.rdf;

public class TripleStoreConfig {

    private final String DB_ADDRESS;
    private final String REPOSITORY_ID;

    public TripleStoreConfig(String address, String repository) {
        DB_ADDRESS = address;
        REPOSITORY_ID = repository;
    }

    public String getAddress() {
        return DB_ADDRESS;
    }

    public String getRepositoryID() {
        return REPOSITORY_ID;
    }

}

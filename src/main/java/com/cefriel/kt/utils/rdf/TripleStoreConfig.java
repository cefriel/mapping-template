package com.cefriel.kt.utils.rdf;

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

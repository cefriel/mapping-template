package com.cefriel.io;

import net.sf.saxon.trans.XPathException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface Reader {

    List<Map<String, String>> executeQueryStringValue(String query) throws Exception;
    void debugQuery(String query, String destinationPath) throws Exception;

    void setVerbose(boolean verbose);
    void shutDown();

}

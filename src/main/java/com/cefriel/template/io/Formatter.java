package com.cefriel.template.io;

public interface Formatter {

    public void formatFile(String filepath) throws Exception;
    public String formatString(String s) throws Exception;

}

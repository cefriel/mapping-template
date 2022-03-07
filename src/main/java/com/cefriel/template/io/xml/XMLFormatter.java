package com.cefriel.template.io.xml;

import com.cefriel.template.io.Formatter;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Serializer;

import java.io.*;

public class XMLFormatter implements Formatter {

    @Override
    public void formatFile(String filepath) throws Exception {
        Builder builder = new Builder();
        InputStream ins = new BufferedInputStream(new FileInputStream(filepath));
        Document doc = builder.build(ins);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filepath));
        formatXML(doc, bos);
        bos.close();
    }

    @Override
    public String formatString(String s) throws Exception {
        Builder builder = new Builder();
        InputStream ins = new BufferedInputStream(new ByteArrayInputStream(s.getBytes()));
        Document doc = builder.build(ins);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        formatXML(doc, baos);
        String formatted = baos.toString();
        baos.close();
        return formatted;
    }

    public void formatXML(Document doc, OutputStream os) throws IOException {
        Serializer serializer = new Serializer(os, "utf-8");
        serializer.setIndent(2);
        serializer.setMaxLength(0);
        serializer.write(doc);
    }
}

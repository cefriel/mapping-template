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

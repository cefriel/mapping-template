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

package com.cefriel.template.io.rdf;

import com.cefriel.template.io.Formatter;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.TurtleWriterSettings;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.FileOutputStream;
import java.io.StringWriter;
import java.util.Objects;

public class RDFFormatter implements Formatter {

    private RDFFormat rdfFormatInput;
    private RDFFormat rdfFormatOutput;

    public RDFFormatter() {
        this.rdfFormatInput = null;
        this.rdfFormatOutput = null;
    }

    public RDFFormatter(RDFFormat rdfFormatInput) {
        this.rdfFormatInput = rdfFormatInput;
        this.rdfFormatOutput = null;
    }

    @Override
    public void formatFile(String filepath) throws Exception {
        Repository repo = new SailRepository(new MemoryStore());
        RDFReader reader = new RDFReader(repo);

        RDFFormat format = rdfFormatInput;
        if (format == null)
            format = Rio.getParserFormatForFileName(filepath).orElse(RDFFormat.TURTLE);

        reader.addFile(filepath, format);

        format = Objects.requireNonNullElseGet(rdfFormatOutput,
                () -> Rio.getParserFormatForFileName(filepath)
                        .orElse(RDFFormat.TURTLE));

        try (FileOutputStream out = new FileOutputStream(filepath)) {
            RDFWriter writer = Rio.createWriter(format, out);
            dump(writer, repo);
        }
    }

    @Override
    public String formatString(String s) throws Exception {
        Repository repo = new SailRepository(new MemoryStore());
        RDFReader reader = new RDFReader(repo);

        RDFFormat format = RDFFormat.TURTLE;
        if (rdfFormatInput != null)
            format = rdfFormatInput;
        reader.addString(s, rdfFormatInput);

        if (rdfFormatOutput != null)
            format = rdfFormatOutput;

        StringWriter sw = new StringWriter();
        RDFWriter writer = Rio.createWriter(format, sw);
        dump(writer, repo);
        return sw.toString();
    }

    public void dump(RDFWriter writer, Repository repo) {
        try (RepositoryConnection conn = repo.getConnection()) {
            // inline blank nodes where possible
            writer.getWriterConfig()
                    .set(BasicWriterSettings.INLINE_BLANK_NODES, Boolean.TRUE);
            writer.getWriterConfig()
                    .set(BasicWriterSettings.PRETTY_PRINT, Boolean.TRUE);
            writer.getWriterConfig()
                    .set(TurtleWriterSettings.ABBREVIATE_NUMBERS, Boolean.FALSE);
            conn.export(writer);
        }
    }

    public RDFFormat getRdfFormatInput() {
        return rdfFormatInput;
    }

    public void setRdfFormatInput(RDFFormat rdfFormatInput) {
        this.rdfFormatInput = rdfFormatInput;
    }

    public RDFFormat getRdfFormatOutput() {
        return rdfFormatOutput;
    }

    public void setRdfFormatOutput(RDFFormat rdfFormatOutput) {
        this.rdfFormatOutput = rdfFormatOutput;
    }

}

/*
 * Copyright (c) 2019-2021 Cefriel.
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

package com.cefriel.template;

import com.cefriel.template.io.rdf.RDFReader;
import com.cefriel.template.utils.LoweringUtils;
import com.cefriel.io.rdf.RDFWriter;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.*;

public class DemoRdfLowerer {

    public static void main(String ... argv) throws Exception {
        Repository repo;
        repo = new SailRepository(new MemoryStore());

        RDFWriter.baseIRI = "http://www.cefriel.com/data/";
        RDFWriter writer = new RDFWriter(repo, null);
        writer.addFile("demo/input.ttl");


        RDFReader reader = new RDFReader(repo);

        TemplateExecutor lowerer = new TemplateExecutor(reader, new LoweringUtils());

        File template = new File("demo/template.vm");
        InputStream templateStream = new FileInputStream(template);
        System.out.println(lowerer.lower(templateStream));
    }
}

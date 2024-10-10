# mapping-template

[![Maven Central](https://img.shields.io/maven-central/v/com.cefriel/mapping-template.svg?label=Maven%20Central)](https://search.maven.org/artifact/com.cefriel/mapping-template)

A template-based component exploiting [Apache Velocity](https://velocity.apache.org/) to define declarative mappings for schema and data transformations.

A quantitative and qualitative evaluation of the tool is available in the repository [mapping-template-eval](https://github.com/cefriel/mapping-template-eval).

### Mapping Template Language (MTL)

The [Wiki](https://github.com/cefriel/mapping-template/wiki/Mapping-Template-Language-(MTL)) contains the documentation to specify compliant mapping templates.

Example templates are provided in the [examples](https://github.com/cefriel/mapping-template/tree/main/examples) folder.

### RDF Mapping Language support (RML to MTL)

The `mapping template` supports the execution of RML mappings that are automatically compiled to an MTL template and executed. The `mapping template` is currently compliant with the `rml-core` specification (https://w3id.org/rml/portal).

The RML mapping can be passed with the `--rml` option for usage via CLI and a [test case](src/test/java/com/cefriel/template/RMLTests.java) is made available to exemplify the usage as a library.

### Usage as a Library

The  `mapping-template` can be used as a library through the `TemplateExecutor` class. It allows to execute mapping templates accessing data from the filesystem or through `InputStream`s. Configuration examples can be found in the `Main` class and in the `test` folder.

The `mapping-template` is available on Maven Central and can be added as a dependency in Java projects as described [here](https://search.maven.org/artifact/com.cefriel/mapping-template). Using Maven the following dependency should be specified in the `pom.xml` selecting a [release](https://github.com/cefriel/mapping-template/releases) version:
```
<dependency>
  <groupId>com.cefriel</groupId>
  <artifactId>mapping-template</artifactId>
  <version>${version}</version>
</dependency>
```
The component can be used as an external library to launch multiple template executions in parallel.

### Usage via CLI
This is the intended usage of the `mapping-template.jar`.

```
usage: java -jar mapping-template.jar <options>
options:
  -b, --basepath <arg>            Base path for files (input, template, output). Default value is './'.
  -c, --contextIRI <arg>          IRI identifies the named graph for context-aware querying of the repository. 
                                  Default behaviour: the entire repository is considered for querying.
  -q, --query <arg>               Path to the file containing a query.
  -dq, --debug-query              Saves in the output file the result of the query provided with -q option.
  -f, --format <arg>              Activate procedures for specific output formats. Supported values: 'xml' 
                                  (XML escape, XML parsing to check structure, indentation), 'turtle', 'rdfxml', 'nt', 'json'.
  -fir, --fail-invalid-ref        If this option is enabled, the execution fails every time a variable in the template can not be accessed.
  -fun, --functions <arg>         Provide the path to a Java file defining a TemplateFunctions subclass.
  -if, --input-format <arg>       Format for the input(s). Supported values are: 'csv', 'json', 'xml', 'rdf', 'mysql', 'postgresql'.
  -i, --input <arg>               Path for the input file. Multiple input files are supported if the '--input-format' is 'rdf'.
  -kv, --key-value <arg>          Path for a file containing a key:value pair for each line. These pairs
                                  are made available as a map in the template.
  -kvc, --key-value-csv <arg>     Path for a csv file with one line interpreted as a set of key[column]-value[line] pairs. 
                                  These pairs are made available as a map in the template.
  -o, --output <arg>              Path of output file. Default: output.txt
  -rml, --compile-rml <arg>       Provide an RML mapping file to be executed
  -t, --template <arg>            Path of template file. Default: template.vm
  -tm, --time <arg>               Path of file reporting template execution time. Default: timing not saved. 
  -tr, --trim                     Trim newlines from the template before executing it to reduce memory usage.
  -url, --remote-url <arg>        Address for accessing remote database (relational or triplestore).
  -id, --remote-id <arg>          Identifier of the remote database or repository for triplestores.
  -us, --username <arg>           Username for accessing remote database.
  -psw, --password <arg>          Password for accessing remote database.
  -v, --verbose                   Debug information are logged.
```
Instructions on how to run the example mapping templates via command line are provided in the [examples/README](https://github.com/cefriel/mapping-template/tree/main/examples/README.md).

A `$reader` is initialized based on the specified `-if` option. Additional `Reader`s should be defined in the template using the available functions.

If `-url` and `-id` options are set a remote database/repository is used for queries and the `-i`  option is ignored. If they are not set the `-i` option is mandatory. Assumptions to use a remote database/repository are: (i) it is up and running, and (ii) data are already in there.

### References

Projects using the `mapping-template`:
- SmartEdge: Semantic Low-code Programming Tools for Edge Intelligence https://www.smart-edge.eu/ (GA 101092908)
- TANGENT: Enhanced Data Processing Techniques for Dynamic Management of Multimodal Traffic https://tangent-h2020.eu/ (GA 955273)

Publications:
- Scrocca, M., Carenini, A., Grassi, M., Comerio, M., & Celino, I. (2024). `Not Everybody Speaks RDF: Knowledge Conversion between Different Data Representations`. In: Fifth International Workshop on Knowledge Graph Construction co-located with the ESWC2024. CEUR-WS. https://ceur-ws.org/Vol-3718/paper3.pdf
- Scrocca, M., Grassi, M., Comerio, M., Carriero, V. A., Dias, T. D., Da Silva, A. V., & Celino, I. (2024). `Intelligent Urban Traffic Management via Semantic Interoperability across Multiple Heterogeneous Mobility Data Sources`. In: The Semantic Web – ISWC 2024. Springer. (_to appear_). https://arxiv.org/abs/2407.10539

### Commercial Support

If you need commercial support for the `mapping-template` contact us at [chimera-dev@cefriel.com](mailto:chimera-dev@cefriel.com).

### License

_Copyright (c) 2019-2024 Cefriel._

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

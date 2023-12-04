# mapping-template

[![Maven Central](https://img.shields.io/maven-central/v/com.cefriel/mapping-template.svg?label=Maven%20Central)](https://search.maven.org/artifact/com.cefriel/mapping-template)

A template-based component exploiting [Apache Velocity](https://velocity.apache.org/) to define declarative mappings for schema and data transformations.

### Mapping Template Language (MTL)

The [Wiki](https://github.com/cefriel/mapping-template) contains the documentation to specify compliant mapping templates.

Example templates are provided in the [examples](https://github.com/cefriel/mapping-template/tree/main/examples) folder.

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
                                  (XML escape, XML parsing to check structure, indentation), 'turtle', 'rdfxml', 'nt'.
  -fun, --functions <arg>         Provide the path to a Java file defining a TemplateFunctions subclass.
  -if, --input-format <arg>       Format for the input file(s). Supported values are: 'csv', 'json', 'xml', 'rdf'.
  -i, --input <arg>               Path for the input file. Multiple input files are supported if the '--input-format' is 'rdf'.
  -kv, --key-value <arg>          Path for a file containing a key:value pair for each line. These pairs
                                  are made available as a map in the template.
  -kvc, --key-value-csv <arg>     Path for a csv file with one line interpreted as a set of key[column]-value[line] pairs. 
                                  These pairs are made available as a map in the template.
  -o, --output <arg>              Path of output file. Default: output.txt
  -r, --repository <arg>          Repository Id related to the triples store.
  -t, --template <arg>            Path of template file. Default: template.vm
  -tm, --time <arg>               Path of file reporting template execution time. Default: timing not saved. 
  -tr, --trim                     Trim newlines from the template before executing it to reduce memory usage.
  -ts, --ts-address <arg>         Triples store address.
  -v, --verbose                   Debug information are logged.
```
Instructions on how to run the example mapping templates via command line are provided in the [examples/README](https://github.com/cefriel/mapping-template/tree/main/examples/README.md).

A `$reader` is initialized based on the specified `-if` option. Additional `Reader`s should be defined in the template using the available functions.

If `-ts` and `-r` options are set a remote repository is used for queries and the `-if rdf`  option is ignored. If they are not set the `-if rdf` option is mandatory. Assumptions to use a remote repository are: the triples store is up and running, and triples are already in there.

### Commercial Support

If you need commercial support for the `mapping-template` contact us at [chimera-dev@cefriel.com](mailto:chimera-dev@cefriel.com).

### License

_Copyright (c) 2019-2023 Cefriel._

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

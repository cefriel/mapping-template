# rdf-template

A template-based component exploiting [Apache Velocity](https://velocity.apache.org/) to read/write RDF. The `rdf-template` is maintained by Cefriel as a building block of the [Chimera](https://github.com/cefriel/chimera) solution for semantic data conversion.

A demo of the tool is available in the folder [demo](https://github.com/cefriel/rdf-lowerer/tree/master/demo).

### Functionalities
The main functionalities provided by this component are:

#### RDF lowering

Query RDF triples to generate a custom output.

- Enable _SPARQL queries_ in the template accessing an HTTP Repository or an in-memory repository initialized with triples from one or multiple RDF files
- Possibility to access a portion of the repository contextualizing queries with respect to a specific named graph

#### XML lifting

Access XML files to generate custom RDF triples. It enables _XQuery queries_ in the template to access an XML file.

#### General functionalities

- Access custom _utility functions_ in the template
- Procedures for specific _output formats_ 
  - XML option to validate and indent XML files
  - RDF options to validate and serialise RDF in different formats
- Possibility to provide _generic key-value pairs_ at runtime then made accessible through a map data structure in the template
- Run _parametric templates_ executed once for each resulting row of the provided SPARQL query 
  - Providing a query through the `-q` option is it possible to execute _parametric templates_ with respect to the given query, e.g., it is possible to run the same template for each element of a specific class contained in the input data. The template is executed for each resulting row of the provided query; in each template execution, the specific row is bound to a given variable (`$x`) that is made accessible as a map (keys as specified for column names) in the template.

#### Performance improvement
- _Trim Template_: `--trim` option to remove newlines in the template before executing it (high reduction in memory usage)
- _FileOutputStream_: write template produced directly to file without retaining it in memory (reduction in memory usage)
- _Support data structure_: definition of functions building support data structures to access query results in the template (reduction in execution time)

#### Usage as a Library

The  `rdf-template`  can be used via command line but also as a library through the `TemplateExecutor` class. It allows to execute templates through the filesystem or through `InputStream`s. Configuration examples can be found in the `Main` class and in the `test` folder.

### Documentation
This section contains the documentation to use the tool and to produce compliant Apache Velocity templates.

#### Template Default Variables
The `rdf-template` component offers a set of already bound variables that can be used in the template.

##### `$reader` 

The `$reader` variable offers methods to execute queries accordingly to the provided input.

If RDF input files are provided it is bound to `RDFReader`:

- `setPrefixes(String prefixes)`: set the RDF prefixes that should be concatened with the provided queries.
- `executeQuery(String query)`: Executes a SPARQL query returning a list of rows as `List<Map<String,Value>>`.
- `executeQueryStringValue(String query)`: As `executeQuery` but it returns a `List<Map<String,String>>` extracting the string value of each `Value` returned. The query, the duration and the number of rows returned are logged if the `verbose` option is enabled.
- `executeQueryStringValueXML(String query)`: As `executeQuery` but it returns a `List<Map<String,String>>` extracting the string value of each `Value` returned and escaping XML special chars. The query, the duration and the number of rows returned are logged if the `verbose` option is enabled.

- `setContext(String context)`: Set the IRI of the context (named graph) for read/write operations on the repository.

If XML input files are provided it is bound to `XMLReader`:

- `setNamespaces(String namespaces)`: set the XQuery preamble (e.g. namespaces definition) that should be concatened with the provided queries.
- `executeQueryStringValue(String query)`: Executes a XQuery query returning a list of rows as `List<Map<String,Value>>` if a `map` is provided, otherwise the result can be accessed using the `value` key.

##### `$functions`

The `$functions` variable offers a set of utility methods that can be extended defining sub-classes (see the section below). Default functions are:

- `rp(String s)`: if a prefix is set, removes it from the parameter string. If a prefix is not set, or the prefix is not contained in the given string it returns the string as it is.
- `setPrefix(String prefix)`: set a prefix for the `rp` method.
- `sp(String s, String substring)`: returns the substring of the parameter string after the first occurrence of the parameter substring.
- `p(String s, String substring)`: returns the substring of the parameter string before the first occurrence of the parameter substring.
- `replace(String s, String regex, String replacement)`: returns a string replacing all the occurrences of the regex with the replacement provided.
- `newline()`: returns a newline string.
- `hash(String s)`: returns a string representing the hash of the parameter.
- `checkString(String s)`: returns `true` if the string is not null and not an empty string.
- `getMap(List<Map<String, String>> results, String key)`: creates a support data structure to access query results faster. Builds a map associating a single row with its value w.r.t a specified column (key parameter). The assumption is for each row the value for the given column is unique, otherwise, the result will be incomplete.
- `getListMap(List<Map<String, String>> results, String key)`: creates a support data structure to access query results faster. Builds a map associating a value with all rows having that as value for a specified column (key parameter).
- `checkList(List<T> l)`: returns `true` if the list is not null and not empty.
- `checkList(List<T> l, T o)`: returns `true` if the list is not null, not empty and contains `o`.
- `checkMap(Map<K,V> m)`: returns `true` if the map is not null and not empty.
- `checkMap(Map<K, V> m, K key)`: returns `true` if the map is not null, not empty and contains the key `key`.
- `getMapValue(Map<K, V> map, K key)`: if `checkMap(map, key)` is `true` returns the value for `key` in `map`, otherwise returns `null`. 
- `getListMapValue(Map<K, List<V>> listMap, K key)`: if `checkMap(listMap, key)` is `true` returns the value for `key` in `listMap`, otherwise returns an empty list.
- `getRDFReaderFromFile(String filename)` and `getRDFReaderFromString(String s)`: returns dynamically a RDFReader from a RDF file or string
- `getXMLReaderFromFile(String filename)` and `getXMLReaderFromString(String s)`: returns dynamically a XMLReader from a RDF file or string

##### `$map`
The `$map` variable contains all key-value pairs specified with both `-kv` and `-kvc` options.

#### TemplateUtils subclasses
We report here the subclasses of `TemplateUtils`, the functions introduced to extend the default set and the option to activate them.
- TransmodelTemplateUtils (`-u transmodel`):
  - `getTimestamp()`: returns current timestamp in `yyyy-MM-dd'T'HH:mm:ss` format.
  - `getFormattedDate(int year, int month, int dayOfMonth, int hour, int minute)`: returns date and time specified as `yyyy-MM-dd'T'HH:mm:ss`.
  - `formatGTFSDate(String dateString)`: format GTFS dates in `yyyy-MM-dd'T'HH:mm:ss` format.

#### `rdf-template.jar` ####
This is the intended usage of the `rdf-template.jar`.

```
usage: java -jar rdf-template.jar <options>
options:
  -b, --basepath <arg>            Base path for files (input, template, output). Default value is './'.
  -c, --contextIRI <arg>          IRI identifies the named graph for context-aware querying of the repository. 
                                  Default behaviour: the entire repository is considered for querying.
  -dq, --debug-query              Saves in the output file the result of the query provided with -q option.
  -f, --format <arg>              Activate procedures for specific output formats. Supported values: 'xml' 
                                  (XML parsing to check structure, indentation), 'turtle', 'rdfxml', 'nt'.
  -rdf, --rdf <arg>               Path(s) of input RDF file(s), if no remote repository is specified an 
                                  in-memory repository is initialized and triples are made available for querying.
  -xml, --xml <arg>               Path of input XML file.
  -kv, --key-value <arg>          Path for a file containing a key:value pair for each line. These pairs
                                  are made available as a map in the template.
  -kvc, --key-value-csv <arg>     Path for a csv file with one line interpreted as a set of key[column]-value[line] pairs. 
                                  These pairs are made available as a map in the template.
  -o, --output <arg>              Path of output file. Default: output.txt
  -q, --query <arg>               Set a query for parametric templates execution, or for debugging if -dq option is enabled.
  -r, --repository <arg>          Repository Id related to the triples store.
  -t, --template <arg>            Path of template file. Default: template.vm
  -tr, --trim                     Trim newlines from the template before executing it to reduce memory usage.
  -ts, --ts-address <arg>         Triples store address.
  -u, --utils <arg>               Set a specific class of utils to be bound as $functions variable in the template.
                                  Default is generic functions (TemplateUtils). Supported values: 'transmodel'.
  -v, --verbose                   Debug information are logged.
```
Only one between `-xml` and `-rdf` option can be used at once, the `$reader` is initialised accordingly.

If `-ts` and `-r` options are set a remote repository is used for queries and the `-i` option is ignored, if they are not set `-i` option is mandatory. Assumptions to use a remote repository are: the triples store is up and running, and triples are already in there.

#### Tips ####
- If it is feasible for the specific case, splitting templates into multiple files and then combining them improves performances. 
- It is better to avoid nested cycles in the template.
- The component can be used as an external library to launch multiple template executions in parallel.

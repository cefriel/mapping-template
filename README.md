# rdf-lowerer

A template-based lowerer for RDF repositories exploiting Apache Velocity and maintained by Cefriel as a building block of the [Chimera](https://github.com/cefriel/chimera) solution for semantic data conversion.

### Functionalities
The main functionalities provided by this component are:
- Enable _SPARQL queries_ in the template accessing an HTTP Repository or an in-memory repository initialized with triples from a file
- Access custom _utility functions_ in the template
- Activate procedures for specific _output formats_ (e.g. XML option to validate and indent XML files)
- Possibility to provide _generic key-value pairs_ at runtime then made accessible through a map data structure in the template
- Run _parametric templates_ executed once for each resulting row of the provided SPARQL query 

#### Performance improvement
- _Trim Template_: remove spaces and newlines in the template before executing it (high reduction in memory usage)
- _FileOutputStream_: write template produced directly to file without retaining it in memory (reduction in memory usage)
- _Support data structure_: definition of functions building support data structures to access query results in the template (reduction in execution time)

### Documentation
This section contains the documentation to use the tool and to produce compliant Apache Velocity templates.

#### Template Default Variables
The `velocity-lowerer` component offers a set of already bound variables that can be used in the template:
- `$reader` offers methods to execute SPARQL queries:
  - `executeQuery(String query)`: Executes a SPARQL query returning a list of rows as `List<Map<String,Value>>`.
  - `executeQueryStringValue(String query)`: As `executeQuery` but it returns a `List<Map<String,String>>` extracting the string value of each `Value` returned.
  - `executeQueryStringValueXML(String query)` As `executeQuery` but it returns a `List<Map<String,String>>` extracting the string value of each `Value` returned and escaping XML special chars.
- `$functions` offers a set of utility methods that can be extended defining sub-classes (see section below). Default functions are:
  - `rp(String s)`: if a prefix is set, removes it from the parameter string. If a prefix is not set, or the prefix is not contained in the given string it returns the string as it is.
  - `setPrefix(String prefix)`: set a prefix for the `rp` method.
  - `sp(String s, String substring)`: returns the substring of the parameter string after the first occurence of the parameter substring.
  - `getMap(List<Map<String, String>> results, String key)`: creates a support data structure to access query results faster. Builds a map associating a single row with its value w.r.t a specified column (key parameter). Assumption is for each row the value for the given column is unique, otherwise result will be incompleted.
  - `getListMap(List<Map<String, String>> results, String key)`: creates a support data structure to access query results faster. Builds a map associating a value with all rows having that as value for a specified column (key parameter).
- `$map` variable contains all key value pairs specified with both `-kv` and `-kvc` options.

#### LoweringUtils subclasses
We report here the subclasses of LoweringUtils, the functions introduced to extend the default set and the option to activate them.
- TransmodelLoweringUtils (`-u transmodel`):
  - `getTimestamp()`: returns current timestamp in `yyyy-MM-dd'T'HH:mm:ss` format.
  - `getFormattedDate(int year, int month, int dayOfMonth, int hour, int minute)`: returns date and time specified as `yyyy-MM-dd'T'HH:mm:ss`.
  - `formatGTFSDate(String dateString)`: format GTFS dates in `yyyy-MM-dd'T'HH:mm:ss` format.
  
#### Parametric Templates
Providing a SPARQL query through the `-q` option is it possible to execute _parametric templates_ with respect to the given query, e.g., it is possible to run the same template for each element of a specific class contained in the database. The template is executed for each resulting row of the provided query; in each template execution the specific row is bound to a given variable (`x`) that is made accessible as a map (keys as specified for column names) in the template.

#### `rdf-lowerer.jar` ####
This is the intended usage of the `rdf-lowerer.jar`.
```
usage: java -jar rdf-lowerer.jar <options>
options:
  -b, --basepath <arg>            Base path for files (input, template, output). Default value is './'.
  -f, --format <arg>              Activate procedures for specific output formats. Supported values: 'xml' (XML parsing to check                                           structure, indentation)
  -i, --input <arg>               Path of input file containing triples, if no remote repository is specified an in-memory repository
                                  is initialized and triples in this file are made available for querying. Default: input.ttl
  -kv, --key-value <arg>          Path for a file containing a key:value pair for each line. These pairs
                                  are made available as a map in the template.
  -kvc, --key-value-csv <arg>     Path for a csv file with one line interpreted as a set of key[column]-value[line] pairs. 
                                  These pairs are made available as a map in the template.
  -o, --output <arg>              Path of output file. Default: output.xml
  -q, --query <arg>               Set a query for parametric templates execution.
  -r, --repository <arg>          Repository Id related to the triples store.
  -t, --template <arg>            Path of template file. Default: template.vm
  -ts, --ts-address <arg>         Triples store address.
  -u, --utils <arg>               Set a specific class of utils to be bound as $functions variable in the template.
                                  Default is generic functions (LoweringUtils). Supported values: 'transmodel'.
```
If `-ts` and `-r` options are set a remote repository is used for queries and the `-i` option is ignored, if they are not set `-i` option is mandatory. Assumptions to use a remote repository are: the triples store is up and running, and triples are already in there.

#### Tips ####
- If it is feasible for the specific case, splitting templates in multiple files and then combining them improves performances. 
- It is better to avoid nested cycles in the template.
- The component can be used also as an external library using class `com.cefriel.kt.lower.TemplateLowerer`, e.g., to configure it to launch multiple lowering procedures in parallel.

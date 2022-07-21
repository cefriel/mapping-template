# Demo

This is a simple demo converting triples about instances of `gtfs:Agency` (ontology [Linked GTFS](https://github.com/OpenTransport/linked-gtfs)) back to the [GTFS](https://developers.google.com/transit/gtfs/reference) file `agency.csv`.

The lowerer use the file `input.ttl` to load the triples in an in-memory repository. Then it uses the `template.vm` file defining how to query the repository and how to populate the provided skeleton.

#### Template Execution

You can run it following these steps:

  1. Compile the source code _or_ download the jar from the [releases](https://github.com/cefriel/rdf-lowerer/releases) section on Github
  2. Put the jar in the `demo` folder (if needed, rename the jar as `rdf-template.jar`)
  3. Run the following command
  ```
  java -jar rdf-template.jar -b "./agency/" -i input.ttl -o agency.csv -t template.vm
  ```
  4. Try with multiple input files
  ```
  java -jar rdf-template.jar -b "./agency-multiple-input/" -i input.ttl input2.ttl -o agency.csv -t template.vm
  ```
  5. The expected output is provided in the file `agency.csv`

#### Parametric Template Execution

A demo of a parametric execution of the template can be run using the following command:

```
java -jar rdf-template.jar -b "./agency-parametric/" -i input.ttl -o agency.csv -t template.vm -q query.txt
```

If the parametric query defines an `?id` variable, its value is used as suffix for the output files of each parametric execution. Otherwise, an incremental identifier is automatically assigned.

In the demo, the parametric variable `$x` is used in the SPARQL query in the template. However, the variable `$x`, bound to a result row of the parametric query, can be also simply used in the template definition. 

#### Debug Query

A demo debugging a SPARQL query on the input file(s) can be run using the following command:

```
java -jar rdf-template.jar -b "./agency-debug/" -i input.ttl -q query.txt -dq
```

The result is saved in the file `output.txt` (filename can be modified using the `-o` option).




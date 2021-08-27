# Demo

This is a simple demo converting triples about instances of `gtfs:Agency` (ontology [Linked GTFS](https://github.com/OpenTransport/linked-gtfs)) back to the [GTFS](https://developers.google.com/transit/gtfs/reference) file `agency.csv`.

The lowerer use the file `input.ttl` to load the triples in an in-memory repository. Then it uses the `template.vm` file defining how to query the repository and how to populate the provided skeleton.

You can run it following these steps:
  1. Compile the source code 
  2. Rename the jar as `rdf-lowerer.jar` and put it in the `demo` folder
  3. Run the following command
  ```
  java -jar rdf-lowerer.jar -b "./" -i input.ttl -o agency.csv -t template.vm
  ```
  4. Try with multiple input files
  ```
  java -jar rdf-lowerer.jar -b "./" -i input.ttl input2.ttl -o agency.csv -t template.vm
  ```
  5. The expected output is provided in the file `agency.csv`
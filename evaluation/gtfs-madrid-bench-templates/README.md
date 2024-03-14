## Quantitative Evaluation 
To test the performance and scalability of the `mapping-template` tool, we considered the GTFS-Madrid-Bench. The benchmark provides a set of (R2)RML mappings and a generator to create input data sources in different formats and sizes. We considered three data formats (CSV, XML and JSON) and three scaling factors (1,10,100) comparing the `mapping-template` tool with the 
[`rmlmapper`](https://github.com/RMLio/rmlmapper-java) v6.1.2 and [`morph-kgc`](https://github.com/RMLio/rmlmapper-java) v2.3.1 processors. We adopted a set of RML mappings simplifying the _join_ operation for the GTFS shapes file as in [1].

A set of templates implementing the same mapping rules were generated for the `mapping-template` tool (`*-no-self-join.vm`). In this first set of templates, we defined a _join_ operation between two data frames as specified by the join condition in RML to guarantee a more fair comparison. An additional set of templates (`*-no-join.vm`), compared in the evaluation as `mapping-template-nj`, is defined to test the performances of the template approach using optimised mappings without _join_ operations.

[1] Arenas-Guerrero, Julián, et al. "Knowledge graph construction with R2RML and RML: an ETL system-based overview." Second International Workshop on Knowledge Graph Construction. 2021.

Conversion examples
===================

This folder contains several example mapping templates implementing different mapping scenarios. The instructions on how to run the examples using the `mapping-template` tool are reported for each example. 

RML CSV example
----------

The CSV example from the [RML specification](https://rml.io/specs/rml/#example-CSV):

``` {.csv}
id,stop,latitude,longitude
6523,25,50.901389,4.484444
```

Can be converted to RDF with the following template. Note that the
header information of the original CSV file is used to determine the
name of the properties which will be used in the Apache Velocity
template file.

``` {.vtl}
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>.
@prefix transit: <http://vocab.org/transit/terms/>.
@prefix xsd: <http://www.w3.org/2001/XMLSchema#>.
@prefix wgs84_pos: <http://www.w3.org/2003/01/geo/wgs84_pos#>.
@prefix ex: <http://airport.example.com/>.

#set ($data = $reader.getDataframe())

#foreach($row in $data)
ex:$row.id rdf:type transit:Stop ;
  transit:route "$row.stop"^^xsd:int ;
  wgs84_pos:lat "$row.latitude" ;
  wgs84_pos:long "$row.longitude" .
#end
```

From the command line, using the following command

``` {.bash org-language="sh"}
java -jar mapping-template.jar --input-format csv --input example.csv -t template.vm -f turtle -o output.ttl
```

which specifies the following parameters:

`--input-format`
:   The format of the input file.

`--input`
:	The input file.

`-t`
: The template to use in the mapping process.

`-f`
:   The format against which the output of the mapping process will be
    validated against.

`-o`
:   The output file.

The following RDF (Turtle) output file is produced:

``` {.ttl}
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix transit: <http://vocab.org/transit/terms/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix wgs84_pos: <http://www.w3.org/2003/01/geo/wgs84_pos#> .
@prefix ex: <http://airport.example.com/>.

ex:6523 a transit:Stop;
  transit:route "25"^^xsd:int;
  wgs84_pos:lat "50.901389";
  wgs84_pos:long "4.484444" .
```

RML XML example
----------

The XML example from the [RML
specification](https://rml.io/specs/rml/#example-XML):

``` {.xml}
<?xml version="1.0" encoding="UTF-8"?>
<transport>
    <bus id="25">
    <route>
        <stop id="645">International Airport</stop>
        <stop id="651">Conference center</stop>
    </route>
    </bus>
</transport>
```

can be converted to rdf with the following template:

``` {.vtl}
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>.
@prefix transit: <http://vocab.org/transit/terms/>.
@prefix xsd: <http://www.w3.org/2001/XMLSchema#>.
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix ex: <http://trans.example.com/>.

#set( $query = '
    for $stop in /transport/bus/route//stop
    return map {
        "stopId": $stop/@id,
        "stopName": $stop/text(),
        "busId": $stop/ancestor::bus/@id
    }')
#set( $data = $reader.getDataframe($query))

#foreach($stop in $data)
ex:$stop.busId rdf:type transit:stop ;
  transit:stop "$stop.stopId"^^xsd:int ;
  rdfs:label "$stop.stopName" .
#end
```

From the command line, using the following command

``` {.bash org-language="sh"}
java -jar mapping-template.jar --input-format --input example.xml -t template.vm -f turtle -o output.ttl
```

which specifies the following parameters:

`--input-format`
:   The format of the input file.

`--input`
:	The input file.

`-t`
:   The template to use in the mapping process.

`-f`
:   The format against which the output of the mapping process will be
    validated against.

`-o`
:   The output file.

The following RDF (Turtle) output file is produced:

``` {.ttl}
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix transit: <http://vocab.org/transit/terms/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix ex: <http://trans.example.com/>.

ex:25 a transit:stop;
  transit:stop "645"^^xsd:int, "651"^^xsd:int;
  rdfs:label "International Airport", "Conference center" .
```

RML JSON example
-----------

The JSON example from the [RML
specification](https://rml.io/specs/rml/#example-JSON):

``` {.json}
{
    "venue":
    {
    "latitude": "51.0500000",
    "longitude": "3.7166700"
    },
    "location":
    {
    "continent": " EU",
    "country": "BE",
    "city": "Brussels"
    }
}
```

can be converted to RDF with the following template:

``` {.vtl}
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#>.
@prefix wgs84_pos: <http://www.w3.org/2003/01/geo/wgs84_pos#lat>.
@prefix gn: <http://www.geonames.org/ontology#>.
@prefix ex: <http://loc.example.com/city/>.

#set ($xs = $reader.getDataframe('{
     "iterator": "$",
     "paths": {"latitude": "venue.latitude",
       "longitude": "venue.longitude",
       "continent": "location.continent",
       "country": "location.country",
       "city": "location.city"}}'))

#foreach($x in $xs)
ex:$x.city rdf:type schema:city ;
  wgs84_pos:lat "$x.latitude" ;
  wgs84_pos:long "$x.longitude" ;
  gn:countryCode "$x.country" .
#end
```

From the command line, using the following command

``` {.bash org-language="sh"}
java -jar mapping-template.jar --input-format json --input example.json -t template.vm -f turtle -o output.ttl
```

which specifies the following parameters:

`--input-format`
:   The format of the input file.

`--input`
:	The input file.

`-t`
:   The template to use in the mapping process.

`-f`
:   The format against which the output of the mapping process will be
    validated against.

`-o`
:   The output file.

The following RDF (Turtle) output file is produced:

``` {.ttl}
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#>.
@prefix wgs84_pos: <http://www.w3.org/2003/01/geo/wgs84_pos#lat>.
@prefix gn: <http://www.geonames.org/ontology#>.
@prefix ex: <http://loc.example.com/city/>.

ex:Brussels a schema:city ;
  wgs84_pos:lat "51.0500000" ;
  wgs84_pos:long "3.7166700" ;
  gn:countryCode "BE" .
```

YARRRML example
-----------

The example from the [YARRRML tutorial](https://rml.io/yarrrml/tutorial/getting-started/):

people.csv
``` {.csv}
id,firstname,lastname,debutEpisode,hairColor
0,Natsu,Dragneel,1,pink
1,Gray,Fullbuster,2,dark blue
2,Gajeel,Redfox,21,black
3,Lucy,Heartfilia,1,blonde
4,Erza,Scarlet,4,scarlet
```

episodes.csv
``` {.csv}
number,title,airdate
1,Fairy Tail,12/10/2009
2,"The Fire Dragon, the Monkey, and the Ox",19/10/2009
3,Infiltrate! The Everlue Mansion!,26/10/2009
4,DEAR KABY,02/11/2009
```

can be converted to RDF with the following template:

``` {.vtl}
@prefix ex: <http://www.example.com/> .
@prefix e: <http://myontology.com/> .
@prefix dbo: <http://dbpedia.org/ontology/> .
@prefix schema: <http://schema.org/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

#set ($charReader = $functions.getCSVReaderFromFile("people.csv"))
#set ($episodeReader = $functions.getCSVReaderFromFile("episodes.csv"))
#set ($characters = $charReader.getDataframe())
#set ($episodes = $episodeReader.getDataframe())
#set ($episodeAppearences = $functions.getMap($episodes, "number"))

#foreach($ep in $episodes)
ex:episode_$ep.number a schema:Episode ;
    schema:title "$ep.title" .
#end

#foreach($ch in $characters)
ex:Characters {
ex:$ch.id a schema:Person ;
    schema:givenName "$ch.firstname" ;
    schema:lastName "$ch.lastname" ;
    e:debutEpisode "$ch.debutEpisode"^^xsd:int ;
    dbo:hairColor "$ch.hairColor.toUpperCase()"@en .
}

ex:Episodes { ex:$ch.id e:debutEpisode "$ch.debutEpisode"^^xsd:integer . }

#set($tEpisode = $functions.getMapValue($episodeAppearences, $ch.debutEpisode))
#if($tEpisode.number) 
ex:Characters {
ex:$ch.id e:appearsIn ex:episode_$tEpisode.number .
}

ex:Episodes {
ex:$ch.id e:appearsIn ex:episode_$tEpisode.number .
}
#end
#end
```

When multiple files are needed in a mapping they can be specified directly in the template and do not need to be passed in as parameters from the command line.

This example showcases how to produce RDF quads, the usage of functions, the specification of data types and language tags and the usage of joins.

Quads can be produced by directly specifing to which graph the RDF triple belongs to. In this case we adopt the TriG RDF syntax.

``` {.vtl}
ex:Characters { ex:$char.id schema:givenName "$char.firstname" . }
```

``` {.ttl}
ex:Characters { ex:0 schema:givenName "Natsu" . }
```

Datatypes and language tags are similarly directly expressed in the template.
Note that to convert the hairColor string property to uppercase the Java function `toUpperCase()` can be used thanks to the [Apache Velocity template language (VTL)](https://velocity.apache.org/engine/2.3/user-guide.html).

``` {.vtl}
ex:$char.id dbo:hairColor "$char.hairColor.toUpperCase()"@en .
```

``` {.ttl}
ex:0 dbo:hairColor "PINK"@en .
```

The join condition is expressed by:

``` {.vtl}
...
#set ($episodeAppearences = $functions.getMap($episodes, "number"))
...
...
#set($tEpisode = $functions.getMapValue($mEpisodeAppearences, $char.debutEpisode))
#if($tEpisode.number) 
ex:Characters { ex:$ch.id e:appearsIn ex:episode_$tEpisode.number . }

ex:Episodes { ex:$ch.id e:appearsIn ex:episode_$tEpisode.number . }
#end
```

The two keys involved in the join are `number` and `debutEpisode`, respectively found in the `episodes.csv` and `people.csv` files. 
Because we know that each character debuts in at most one episode, we can use the support function `getMap(df, key)` to create a support data structure to optimize the join process. Otherwise, the `getListMap(df, key)` function could be used to access the multiple rows having the same value for a certain column (key).

The join operation is completed by retrieving the episode appearance of a character by their `debutEpisode` key. An explicit null check is required to make sure that a value is present for the particular `debutEpisode` key.

From the command line, using the following command

``` {.bash org-language="sh"}
java -jar mapping-template.jar -t template.vm -o output.trig
```

which specifies the following parameters

`-t`
:   The template to use in the mapping process.

`-o`
:   The output file.

the following RDF output file is produced:

``` {.ttl}
@prefix ex: <http://www.example.com/> .
@prefix e: <http://myontology.com/> .
@prefix dbo: <http://dbpedia.org/ontology/> .
@prefix schema: <http://schema.org/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

ex:episode_1 a schema:Episode ;
    schema:title "Fairy Tail" .
ex:episode_2 a schema:Episode ;
    schema:title "The Fire Dragon, the Monkey, and the Ox" .
ex:episode_3 a schema:Episode ;
    schema:title "Infiltrate! The Everlue Mansion!" .
ex:episode_4 a schema:Episode ;
    schema:title "DEAR KABY" .

ex:Characters {
ex:0 a schema:Person ;
    schema:givenName "Natsu" ;
    schema:lastName "Dragneel" ;
    e:debutEpisode "1"^^xsd:int ;
    dbo:hairColor "PINK"@en .
}
ex:Episodes { ex:0 e:debutEpisode "1"^^xsd:integer . }
ex:Characters { ex:0 e:appearsIn ex:episode_1 . }
ex:Episodes { ex:0 e:appearsIn ex:episode_1 . }
ex:Characters {
ex:1 a schema:Person ;
    schema:givenName "Gray" ;
    schema:lastName "Fullbuster" ;
    e:debutEpisode "2"^^xsd:int ;
    dbo:hairColor "DARK BLUE"@en .
}
ex:Episodes { ex:1 e:debutEpisode "2"^^xsd:integer . }
ex:Characters { ex:1 e:appearsIn ex:episode_2 . }
ex:Episodes { ex:1 e:appearsIn ex:episode_2 . }
ex:Characters {
ex:2 a schema:Person ;
    schema:givenName "Gajeel" ;
    schema:lastName "Redfox" ;
    e:debutEpisode "21"^^xsd:int ;
    dbo:hairColor "BLACK"@en .
}
ex:Episodes { ex:2 e:debutEpisode "21"^^xsd:integer . }
ex:Characters {
ex:3 a schema:Person ;
    schema:givenName "Lucy" ;
    schema:lastName "Heartfilia" ;
    e:debutEpisode "1"^^xsd:int ;
    dbo:hairColor "BLONDE"@en .
}
ex:Episodes { ex:3 e:debutEpisode "1"^^xsd:integer . }
ex:Characters { ex:3 e:appearsIn ex:episode_1 . }
ex:Episodes { ex:3 e:appearsIn ex:episode_1 . }
ex:Characters {
ex:4 a schema:Person ;
    schema:givenName "Erza" ;
    schema:lastName "Scarlet" ;
    e:debutEpisode "4"^^xsd:int ;
    dbo:hairColor "SCARLET"@en .
}
ex:Episodes { ex:4 e:debutEpisode "4"^^xsd:integer . }
ex:Characters { ex:4 e:appearsIn ex:episode_4 .}
ex:Episodes { ex:4 e:appearsIn ex:episode_4 . }
```

RDF-star
----------

The CSV example from the [RML-star specification](https://kg-construct.github.io/rml-star-spec/#nested):

``` {.csv}
entity,type,confidence,predictor
Alice,Person,0.8,alpha
Alice,Giraffe,1.0,alpha
Bobby,Dog,0.6,alpha
Bobby,Giraffe,1.0,beta
```

Can be converted to RDF-star with the following template. 

``` {.vtl}
@prefix ex: <http://example.org/>
#set ($data = $reader.getDataframe())

#foreach($row in $data)
<< << ex:$row.entity a ex:$row.type >> ex:confidence $row.confidence >> ex:predictedBy ex:$row.predictor .
#end
```

From the command line, using the following command

``` {.bash org-language="sh"}
java -jar mapping-template.jar --input-format csv --input example.csv -t template.vm -f n3 -o output.ttl
```

which specifies the following parameters:

`--input-format`
:   The format of the input file.

`--input`
:	The input file.

`-t`
:   The template to use in the mapping process.

`-f`
:   The format against which the output of the mapping process will be
    validated against.

`-o`
:   The output file.

The following RDF output file is produced:

``` {.ttl}
@prefix ex: <http://example.org/>

<< << ex:Alice a ex:Person >> ex:confidence 0.8 >> ex:predictedBy ex:alpha .
<< << ex:Alice a ex:Giraffe >> ex:confidence 1.0 >> ex:predictedBy ex:alpha .
<< << ex:Bobby a ex:Dog >> ex:confidence 0.6 >> ex:predictedBy ex:alpha .
<< << ex:Bobby a ex:Giraffe >> ex:confidence 1.0 >> ex:predictedBy ex:beta .
```

CSV multiple values to RDF
----------

The following CSV example file contains multiple values in the title column.

``` {.csv}
book_id,title
001,Il Gattopardo-it
002,Dune-en
003,La Sirena-it
```

It can be converted to RDF with the following template. 

``` {.vtl}
@prefix dc: <http://purl.org/dc/elements/1.1/> .
@prefix ex: <http://example.org/stuff/1.0/> .

#set ($bookDF = $reader.getDataframe())
#set ($bookDF = $functions.splitColumn($bookDF, "title", "-"))

#foreach($row in $bookDF)
ex:book\/$row.book_id a ex:Book;
dc:title "${row.title1}"@${row.title2}.

#end
```

A column containing multiple values can be split into multiple columns
by the `splitColumn(df, column, separatorRegex)` function.  The
content of the column is split into *n* values for *n* new columns
according to the number *n* of separatorRegex regex matches on the
source column value. The new columns follow the `"original column
name""match number"` naming convention.  In the example the `title`
column which contains two values, is split into the `title1` and
`title2` columns.

From the command line, using the following command

``` {.bash org-language="sh"}
java -jar mapping-template.jar --input-format csv --input example.csv -t template.vm -f n3 -o output.ttl
```

which specifies the following parameters

`--input-format`
:   The format of the input file.

`--input`
:	The input file.

`-t`
:   The template to use in the mapping process.

`-f`
:   The format against which the output of the mapping process will be
    validated against.

`-o`
:   The output file.

The following RDF output file is produced:

``` {.ttl}
@prefix dc: <http://purl.org/dc/elements/1.1/> .
@prefix ex: <http://example.org/stuff/1.0/> .

ex:book\/001 a ex:Book;
dc:title "Il Gattopardo"@it.

ex:book\/002 a ex:Book;
dc:title "Dune"@en.

ex:book\/003 a ex:Book;
dc:title "La Sirena"@it.
```

CSV to JSON example
----------
The output format for the mapping process is not limited to RDF. On the contrary, the templated based approach allows for potentially any output format.
In this example, we will look at converting data from the csv format to the json format.

The following CSV file

``` {.csv}
book_id,title
001,Il Gattopardo
002,Dune
003,La Sirena
```

Can be converted to JSON with the following template

``` {.vtl}
#set ($bookDF = $reader.getDataframe())

{
    "books":[
#foreach($row in $bookDF)
      {
          "id": "$row.book_id",
          "title": "$row.title"
      } #if(!$foreach.last),#end
#end
    ]
}
```

Note the usage of functions offered by [Apache Velocity](https://velocity.apache.org/engine/2.3/vtl-reference.html#foreach-loops-through-a-list-of-objects) to correctly handle the json array.

From the command line, using the following command

``` {.bash org-language="sh"}
java -jar mapping-template.jar --input-format csv --input example.csv -t template.vm -o output.json
```

which specifies the following parameters:

`--input-format`
:   The format of the input file.

`--input`
:	The input file.

`-t`
:   The template to use in the mapping process.

`-o`
:   The output file.

The following JSON output file is produced:

``` {.json}
{
    "books":[
      {
          "id": "001",
          "title": "Il Gattopardo"
      } ,
      {
          "id": "002",
          "title": "Dune"
      } ,
      {
          "id": "003",
          "title": "La Sirena"
      } 
    ]
}
```

R2RML example
----------
The example from the [R2RML Test Case D016](https://www.w3.org/2001/sw/rdb2rdf/test-cases/#D016-1table1primarykey10columns3rowsSQLdatatypes) shows a template accessing data from a MySQL and a Postgres relational databases.

The content of the database is initialised as reported in the `.sql` files.

``` {.sql}
CREATE TABLE "Patient" (
"ID" INTEGER,
"FirstName" VARCHAR(50),
"LastName" VARCHAR(50),
"Sex" VARCHAR(6),
"Weight" REAL,
"Height" FLOAT,
"BirthDate" DATE,
"EntranceDate" TIMESTAMP,
"PaidInAdvance" BOOLEAN,
"Photo" VARBINARY(200),
PRIMARY KEY ("ID")
);

INSERT INTO "Patient" ("ID", "FirstName","LastName","Sex","Weight","Height","BirthDate","EntranceDate","PaidInAdvance","Photo") 
VALUES (10,'Monica','Geller','female',80.25,1.65,'1981-10-10','2009-10-10 12:12:22',[..]);

INSERT INTO "Patient" ("ID", "FirstName","LastName","Sex","Weight","Height","BirthDate","EntranceDate","PaidInAdvance","Photo") 
VALUES (11,'Rachel','Green','female',70.22,1.70,'1982-11-12','2008-11-12 09:45:44',TRUE,[..]);

INSERT INTO "Patient" ("ID", "FirstName","LastName","Sex","Weight","Height","BirthDate","EntranceDate","PaidInAdvance","Photo") 
VALUES (12,'Chandler','Bing','male',90.31,1.76,'1978-04-06','2007-03-12 02:13:14',TRUE,[..]');

```

The data be converted to RDF with the following template.

``` {.vtl}
@prefix ex: <http://example.com/> .
@prefix foaf: <http://xmlns.com/foaf/0.1/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .

#set($patients = $reader.getDataframe('
SELECT *
FROM patient
'))

#foreach($p in $patients)
<http://example.com/Patient$p.ID> a foaf:Person;
    ex:birthdate "$p.BirthDate"^^xsd:date ;
    ex:entrancedate "$p.EntranceDate"^^xsd:datetime .
#end

```

Two scripts `run-mysql.sh` and `run-postgres.sh` are provided to initialize and run two Docker containers with MySQL and Postgres databases, respectively.
From the command line, the following commands can be used to execute the template (also reported in the script `run.sh`)

``` {.bash org-language="sh"}
java -jar mapping-template.jar --username r2rml --password r2rml -url localhost:3306 -id r2rml -if mysql -f turtle -o output-mysql.ttl -t template.vm

java -jar mapping-template.jar --username r2rml --password r2rml -url localhost:5432 -id r2rml -if postgresql -f turtle -o output-postgres.ttl -t template.vm
```

which specifies the following parameters:

`-if`
:   The format of the input file, in this case the type of database considered.

`-url`
:	The URL to reach the database.

`--username`
:	The username for accessing the database.

`--password`
:	The password for accessing the database.

`-t`
: The template to use in the mapping process.

`-f`
:   The format against which the output of the mapping process will be
    validated against.

`-o`
:   The output file.

The following RDF (Turtle) output file is produced:

``` {.ttl}
@prefix ex: <http://example.com/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix foaf: <http://xmlns.com/foaf/0.1/> .

ex:Patient10 a foaf:Person;
  ex:birthdate "1981-10-10"^^xsd:date;
  ex:entrancedate "2009-10-10 12:12:22"^^xsd:datetime .

ex:Patient11 a foaf:Person;
  ex:birthdate "1982-11-12"^^xsd:date;
  ex:entrancedate "2008-11-12 09:45:44"^^xsd:datetime .

ex:Patient12 a foaf:Person;
  ex:birthdate "1978-04-06"^^xsd:date;
  ex:entrancedate "2007-03-12 02:13:14"^^xsd:datetime .

```
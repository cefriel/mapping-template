Conversion examples
===================

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
java -jar mapping-template.jar --csv example.csv -t template.vm -f turtle -o output.ttl
```

which specifies the following parameters:

--csv
:   The source CSV file.

-t
: The template to use in the mapping process.

-f
:   The format against which the output of the mapping process will be
    validated against.

-o
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
java -jar mapping-template.jar --xml example.xml -t template.vm -f turtle -o output.ttl
```

which specifies the following parameters:

--xml
:   The source XML file.

-t
:   The template to use in the mapping process.

-f
:   The format against which the output of the mapping process will be
    validated against.

-o
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
java -jar mapping-template.jar --json example.json -t template.vm -f turtle -o output.ttl
```

which specifies the following parameters:

--json
:   The source JSON file.

-t
:   The template to use in the mapping process.

-f
:   The format against which the output of the mapping process will be
    validated against.

-o
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

Yarrrml example
-----------

The example from the [Yarrrml tutorial](https://rml.io/yarrrml/tutorial/getting-started/):

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

#set ($charReader = $functions.getCSVReaderFromFile("./join-example/people.csv"))
#set ($episodeReader = $functions.getCSVReaderFromFile("./join-example/episodes.csv"))
#set ($charDF = $charReader.getDataframe())
#set ($episodeDF = $episodeReader.getDataframe())
#set ($mEpisodeAppearences = $functions.getMap($episodeDF, "number"))

#foreach($ep in $episodeDF)
ex:episode_$ep.number a schema:Episode ;
    schema:title "$ep.title" .
#end

#foreach($char in $charDF)
ex:$char.id a schema:Person ex:Characters ;
    schema:givenName "$char.firstname" ex:Characters ;
    schema:lastName "$char.lastname" ex:Characters .
    e:debutEpisode "$char.debutEpisode"^^xsd:integer ex:Characters ;
    dbo:hairColor "$char.hairColor.toUpperCase()"@en ex:Characters .

#set($tEpisode = $functions.getMapValue($mEpisodeAppearences, $char.debutEpisode))
#if($tEpisode.number) 
ex:$char.id e:appearsIn ex:episode_$tEpisode.number ex:Characters .
    e:appearsIn ex:episode_$tEpisode.number ex:Episodes .
#end
ex:$char.id e:debutEpisode "$char.debutEpisode"^^xsd:integer ex:Episodes .
#end
```

When multiple files are needed in a mapping they can be specified directly in the template and do not need to be passed in as parameters from the command line.

This example showcases how to produce RDF quads, the usage of functions, the specification of data types and language tags and the usage of joins.

Quads can be produced by directly specifing to which graph the RDF triple belongs to.

``` {.vtl}
ex:$char.id schema:givenName "$char.firstname" ex:Characters .
```

``` {.ttl}
ex:0 schema:givenName "Natsu" ex:Characters .
```

Datatypes and language tags are similarly directly expressed in the template.
Note that to convert the hairColor string property to uppercase the Java function `toUpperCase()` can be used thanks to the [Apache Velocity template language (VTL)](https://velocity.apache.org/engine/2.3/user-guide.html).

``` {.vtl}
ex:$char.id dbo:hairColor "$char.hairColor.toUpperCase()"@en ex:Characters .
```

``` {.ttl}
ex:0 dbo:hairColor "PINK"@en ex:Characters .
```

The join condition is expressed by:

``` {.vtl}
...
#set ($mEpisodeAppearences = $functions.getMap($episodeDF, "number"))
...
...
#set($tEpisode = $functions.getMapValue($mEpisodeAppearences, $char.debutEpisode))
#if($tEpisode.number) 
ex:$char.id e:appearsIn ex:episode_$tEpisode.number ex:Characters .
ex:$char.id e:appearsIn ex:episode_$tEpisode.number ex:Episodes .
#end
```

The two keys involved in the join are "number" and "debutEpisode", respectively found in the episodes.csv and people.csv files. 
Because we know that each character debuts in at most one episode we can use the support function "getMap(df, key)" to create a support data structure to optimize the join process. Were this assumption not to hold the "getListMap(df, key)" function can be used to access the multiple rows where the column value (key) is the same.

The join operation is completed by retrieving the episode appearance of a character by their debutEpisode key. An explicit null check is required to make sure that a value is present for the particular debutEpisode key.

From the command line, using the following command

``` {.bash org-language="sh"}
java -jar mapping-template.jar -t template.vm -f n3 -o output.ttl
```

which specifies the following parameters:

`-t`
:   The template to use in the mapping process.

`-f`
:   The format against which the output of the mapping process will be
    validated against.

`-o`
:   The output file.

The following RDF output file is produced:

``` {.ttl}
@prefix ex: <http://www.example.com/> .
@prefix e: <http://myontology.com/> .
@prefix dbo: <http://dbpedia.org/ontology/> .
@prefix schema: <http://schema.org/> .

ex:episode_1 a schema:Episode .
ex:episode_1 schema:title "Fairy Tail" .

ex:episode_2 a schema:Episode .
ex:episode_2 schema:title "The Fire Dragon, the Monkey, and the Ox" .

ex:episode_3 a schema:Episode .
ex:episode_3 schema:title "Infiltrate! The Everlue Mansion!" .

ex:episode_4 a schema:Episode .
ex:episode_4 schema:title "DEAR KABY" .


ex:0 a schema:Person ex:Characters .
ex:0 schema:givenName "Natsu" ex:Characters .
ex:0 schema:lastName "Dragneel" ex:Characters .
ex:0 e:debutEpisode "1"^^xsd:integer ex:Characters .
ex:0 dbo:hairColor "PINK"@en ex:Characters .
ex:0 e:appearsIn ex:episode_1 ex:Characters .
ex:0 e:appearsIn ex:episode_1 ex:Episodes .
ex:0 e:debutEpisode "1"^^xsd:integer ex:Episodes .

ex:1 a schema:Person ex:Characters .
ex:1 schema:givenName "Gray" ex:Characters .
ex:1 schema:lastName "Fullbuster" ex:Characters .
ex:1 e:debutEpisode "2"^^xsd:integer ex:Characters .
ex:1 dbo:hairColor "DARK BLUE"@en ex:Characters .
ex:1 e:appearsIn ex:episode_2 ex:Characters .
ex:1 e:appearsIn ex:episode_2 ex:Episodes .
ex:1 e:debutEpisode "2"^^xsd:integer ex:Episodes .

ex:2 a schema:Person ex:Characters .
ex:2 schema:givenName "Gajeel" ex:Characters .
ex:2 schema:lastName "Redfox" ex:Characters .
ex:2 e:debutEpisode "21"^^xsd:integer ex:Characters .
ex:2 dbo:hairColor "BLACK"@en ex:Characters .
ex:2 e:debutEpisode "21"^^xsd:integer ex:Episodes .

ex:3 a schema:Person ex:Characters .
ex:3 schema:givenName "Lucy" ex:Characters .
ex:3 schema:lastName "Heartfilia" ex:Characters .
ex:3 e:debutEpisode "1"^^xsd:integer ex:Characters .
ex:3 dbo:hairColor "BLONDE"@en ex:Characters .
ex:3 e:appearsIn ex:episode_1 ex:Characters .
ex:3 e:appearsIn ex:episode_1 ex:Episodes .
ex:3 e:debutEpisode "1"^^xsd:integer ex:Episodes .

ex:4 a schema:Person ex:Characters .
ex:4 schema:givenName "Erza" ex:Characters .
ex:4 schema:lastName "Scarlet" ex:Characters .
ex:4 e:debutEpisode "4"^^xsd:integer ex:Characters .
ex:4 dbo:hairColor "SCARLET"@en ex:Characters .
ex:4 e:appearsIn ex:episode_4 ex:Characters .
ex:4 e:appearsIn ex:episode_4 ex:Episodes .
ex:4 e:debutEpisode "4"^^xsd:integer ex:Episodes .
```

RDF-Star
----------

The CSV example from the [RML Star specification](https://kg-construct.github.io/rml-star-spec/#nested):

``` {.csv}
entity,type,confidence,predictor
Alice,Person,0.8,alpha
Alice,Giraffe,1.0,alpha
Bobby,Dog,0.6,alpha
Bobby,Giraffe,1.0,beta
```

Can be converted to RDF-Star with the following template. 

``` {.vtl}
@prefix ex: <http://example.org/>
#set ($data = $reader.getDataframe())

#foreach($row in $data)
<< << ex:$row.entity a ex:$row.type >> ex:confidence $row.confidence >> ex:predictedBy ex:$row.predictor .
#end
```

From the command line, using the following command

``` {.bash org-language="sh"}
java -jar mapping-template.jar --csv example.csv -t template.vm -f n3 -o output.ttl
```

which specifies the following parameters:

`--csv`
:   The source CSV file.

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

The following CSV example file contains multiple values for in the title column.

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
source column value. The new columns follow the "original column
name""match number" naming convention.  In the example the "title"
column which contains two values, is split into the "title1" and
"title2" columns.

From the command line, using the following command

``` {.bash org-language="sh"}
java -jar mapping-template.jar --csv example.csv -t template.vm -f n3 -o output.ttl
```

which specifies the following parameters:

--csv
:   The source CSV file.

-t
:   The template to use in the mapping process.

-f
:   The format against which the output of the mapping process will be
    validated against.

-o
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

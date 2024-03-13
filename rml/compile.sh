#!/bin/bash

folder_path="./mappings"

for file in "$folder_path"/*.ttl; do
    if [ -f "$file" ]; then
        # Extract filename without extension
        filename=$(basename "$file")
        filename_no_ext="${filename%.*}"
        echo $filename
        java -jar ../target/mapping-template.jar -i $folder_path/$filename -if rdf -t rml-compiler.vm -o $filename_no_ext.rml.vm -fun functions/RMLCompilerUtils.java

        mv ./$filename_no_ext.rml.vm ./mappings/
        cd mappings
        java -jar ../../target/mapping-template.jar -t $filename_no_ext.rml.vm  -o output.rdf -f turtle --trim
        cd ..
    fi
done



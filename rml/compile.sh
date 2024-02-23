#!/bin/bash

folder_path="./mappings"

for file in "$folder_path"/*; do
    if [ -f "$file" ]; then
        # Extract filename without extension
        filename=$(basename "$file")
        filename_no_ext="${filename%.*}"
        
        java -jar ../target/mapping-template.jar -i $filename_no_ext -t rml-compiler.vm -o $filename_no_ext.rml.vm

        # TODO Add validation via SHACL shapes
    fi
done


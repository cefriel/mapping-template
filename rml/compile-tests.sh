#!/bin/bash

folder_path="./test-cases"

for subfolder in "$folder_path"/*-CSV; do
    echo $subfolder

    cp rml-compiler.vm $subfolder
    cp functions/RMLCompilerUtils.java $subfolder
    
    cd $subfolder
    java -jar ../../../target/mapping-template.jar -i mapping.ttl -if rdf -t rml-compiler.vm -o mapping.rml.vm -fun RMLCompilerUtils.java

    java -jar ../../../target/mapping-template.jar -t mapping.rml.vm -o output.rdf -f nt --trim

    rm ./rml-compiler.vm
    rm -r ./functions

    cat output.rdf
    echo "**********************"
    cat output.nq

    echo "Is it correct? (yes/no)"
    read answer
    if [ "$answer" = "yes" ]; then
        echo "Continuing..."
    else
        break
    fi

    cd ../..
    
done


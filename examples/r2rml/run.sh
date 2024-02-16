java -jar mapping-template.jar --username r2rml --password r2rml -url localhost:3306 -id r2rml --verbose -if mysql -f turtle -o output-mysql.ttl -t template.vm

java -jar mapping-template.jar --username r2rml --password r2rml -url localhost:5432 -id r2rml --verbose -if postgresql -f turtle -o output-postgres.ttl -t template.vm
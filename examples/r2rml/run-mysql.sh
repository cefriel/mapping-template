docker run -d --name mysql-container -p 3306:3306 -e MYSQL_ROOT_PASSWORD=r2rml -e MYSQL_USER=r2rml -e MYSQL_PASSWORD=r2rml -e MYSQL_DATABASE=r2rml -v ./d016.sql:/docker-entrypoint-initdb.d/d016.sql mysql:8.0


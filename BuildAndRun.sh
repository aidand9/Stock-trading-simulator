cd pgsample
mvn compile

#
# Op 1: run with maven
#
#
mvn exec:java -D"exec.mainClass"="cs.toronto.edu.Main"

#
# Op 2: run with java
#
#
cd target/classes
java -cp .:$HOME/.m2/repository/org/postgresql/postgresql/42.7.3/postgresql-42.7.3.jar cs/toronto/edu/Main


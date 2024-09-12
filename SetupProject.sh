mvn archetype:generate -DgroupId=cs.toronto.edu -DartifactId=pgsample -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
cp Main.xml pgsample/pom.xml
cp Main.java pgsample/src/main/java/cs/toronto/edu/
rm pgsample/src/main/java/cs/toronto/edu/App.java

# Utility for building java project
# Supply class name as an argument to execute that class

javac -d ~/git/cdl/java/bin ~/git/cdl/java/src/com/foster/cdl/*.java -Xlint:unchecked

if ! [ -z $1 ]
then
  filename="com.foster.cdl.$1"
  java -cp ~/git/cdl/java/bin $filename
fi

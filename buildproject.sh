# Utility for building java project
# Supply class name as an argument to execute that class

javac -d ~/git/cdl/bin ~/git/cdl/src/com/foster/cdl/*.java -Xlint:unchecked

if ! [ -z $1 ]
then
  filename="com.foster.cdl.$1"
  java -cp ~/git/cdl/bin $filename
fi

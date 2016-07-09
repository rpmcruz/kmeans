all:
	javac -Xlint Kmeans.java
	javac -Xlint Kapplet.java

clean:
	rm -f *.class *~

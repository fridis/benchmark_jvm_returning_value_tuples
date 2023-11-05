CLASSES = \
  src/main/java/dev/flang/be/jvm/benchmarks/jmh/ReturningValueTuples.java \
  src/main/java/dev/flang/be/jvm/benchmarks/jmh/ReturningValueTuplesCases.java

JAR = target/benchmark_returning_value_tuples.jar

.PHONY: build

build: $(JAR)

$(JAR): $(CLASSES)
	mvn clean verify

# run using jmh just to test is the benchmark run works, not for actually
# measuring
run_fast: $(JAR)
	java -jar $(JAR) -f 0 -i 1 -wi 0 -jvmArgs "-Xmx64m" -bm All -r 1s

# run using jmh to check throughput
run: $(JAR)
	java -jar $(JAR) -f 1 -jvmArgs "-Xmx64m" -bm thrpt -r 1s

# run using jmh to check All
run_all: $(JAR)
	java -jar $(JAR) -f 1 -jvmArgs "-Xmx64m" -bm All -r 1s

# run using hand-made benchmark framework, includes tests using fields in
#currentThread()
run_directly: $(JAR)
	java -classpath $(JAR) dev.flang.be.jvm.benchmarks.jmh.ReturningValueTuples;


clean:
	find . -name "*~" -exec rm {} \;
	rm -rf target

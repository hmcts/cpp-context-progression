# Instructions for running the performance tests

1. The maven plugin for jmeter should handle most things and it is possible to run jmeter with:

    mvn clean verify -pl progression-performance-test -P progression-performance-test

2. If you want to see and edit the jmeter tests you can launch Jmeter gui with:
         mvn jmeter:gui -pl progression-performance-test -P progression-performance-test

3. The test results will be save in a .jtl file in ./target/jmeter/results/ and can be viewed in any of the results
   listeners within Jmeter gui.

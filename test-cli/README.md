# How To Run Query Tests

* Goto Vyne and issue a query that you want to test

![Vyne Query](docs/vyne%20query%20builder.png)

* Once Query Results are ready, click on Download button and select 'as Test Case' option:

![Download Test Case](docs/save_as_test_case.png)

* Give a name to your test case:

![Save Test case](docs/save_as.png)

* Download the 'test zip' file:

![Download](docs/download.png)

* Extract the Zip

* Run 'vynetest' app with the path of the extracted folder:

<code>
./vynetest queryTest -p [PATH TO YOUR TEST QUERY PACKAGE FOLDER]
</code>

# Taxonomy Extraction from a Test Case

* Goto Vyne and issue a query that you want to test

![Vyne Query](docs/vyne%20query%20builder.png)

* Once Query Results are ready, click on Download button and select 'as Test Case' option:

![Download Test Case](docs/save_as_test_case.png)

* Give a name to your test case:

![Save Test case](docs/save_as.png)

* Download the 'test zip' file:

![Download](docs/download.png)

* Extract the Zip

* Run 'vynetest' app with the path of the extracted folder and the path of destination folder for extracted taxonomy:

<code>
./vynetest extractSchema -p [PATH TO YOUR TEST QUERY PACKAGE FOLDER] -o [PATH TO YOUR TARGET TAXONOMY FOLDER]
</code>


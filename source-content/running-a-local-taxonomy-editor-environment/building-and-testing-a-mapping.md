# Building & Testing a mapping

As you edit and save your taxonomy, as described in [Editing a taxonomy](editing-a-taxonomy.md), updates are automatically sync'd into your local Vyne instance.

You can use the Data Explorer to see how a source file will be read by Vyne against your schema as you go.

For our demonstration, we'll use this sample mapping:

You can paste this into a file called `Orders.taxi` somewhere in your schema directory:

```text
[[
A price that a symbol was traded at
]]
type Price inherits Decimal

[[
The opening price at the beginning of a period
]]
type OpenPrice inherits Price

[[
The closing price at the end of a trading period
]]
type ClosePrice inherits Price

[[
The ticker for a tradable instrument
]]
type Symbol inherits String

type OrderWindowSummaryCsv {
    orderDate : DateTime( @format = 'yyyy-MM-dd hh-a' ) by column(1)
    symbol : Symbol by column(2)
    open : Price by column(3)
    close : Price by column(4)
}
```

Here's a sample source file to go along with it:

{% file src="../.gitbook/assets/coinbase\_btcusd\_single.csv" caption="Sample CSV File" %}

### Uploading a source

Drag and drop your source file into the Data Explorer.

![Drag and drop your files.](../.gitbook/assets/image%20%2814%29.png)

As you drop, the raw contents of the file will be displayed in the panel below.

Some files, such as CSV have additional import options that you can configure in the drop-down

![Configure the parameters for how files should be parsed](../.gitbook/assets/image.png)

Enter the name of the type to parse the content as, and you'll see the panel below update.

![](../.gitbook/assets/image%20%2833%29.png)

Click on the Parsed Data tab, and you'll see how the schema you've authored is applied to the content in the file.

Click on any of the cells to see the details of the type that the value has been parsed into:

![](../.gitbook/assets/image%20%2839%29.png)

### Edit and reload

As you make changes and save in Visual Studio Code, you can simply refresh the page in Vyne, and reload the source file, to see how changes have been applied to the mapping.



### 


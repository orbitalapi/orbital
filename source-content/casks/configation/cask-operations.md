---
description: >-
  Casks expose their underlying data through RESTful operations.  This section
  describes how to configure the operations generated, through the usage of
  annotations
---

# Querying casks through operations

## Introduction

Data housed in casks are made queryable by exposing RESTful HTTP operations.  The specific operations exposed \(along with indices to improve the performance of those queries\) is configured through the usage of annotations.

Generated operations are described with a taxi schema, which is published to Vyne, to make the queries callable during Vyne queries.

Annotating a field will expose additional queries for the provided data.  Each field may contain multiple annotations, generating multiple queries.

There are no default queries - fields must be annotated to expose queries.

In the above case, cask generates two methods for a model with four fields. 

## Supported annotations

### @Id

Adding @Id on top of a field declaration:

```text
model BlogPost {
 @Id
 blogPostId: BlogPostId
}

```

will result in the following operation being generated:

```text
@HttpOperation(method = "GET" , url = "/api/cask/findSingleBy/BlogPost/blogPostId/{id}")
operation findSingleByOrderID( 
   @PathVariable(name = "id") id : BlogPostId
) : BlogPost( BlogPostId = id )
```

It is exepcted \(but not enforced\) that `@Id` based queries return a single result.  However, this is not strictly enforced during ingestion.  The behaviour for how to handle multiple records is defined through configuration.

  See this section of [Configuration](./#query-behaviours).

{% page-ref page="./" %}

### @PrimaryKey

Defining a primary key on models will generate a corresponding primary key on the database columns.  The resulting operation is the same as generated for `@Id`, but ingestion behaviour is changed to UPSERT rather than INSERT.

### @Indexed

Generates an index \(non-unique\) on the specified column.  This annotation does not result in any new query operations being exposed, but rather improves the query performance \(at the cost of write performance\).

### @Before

Restricted to date-based fields \(`Date`, `Time`,`Instant`, `DateTime` \).  Generates a query that returns records where the field is before a specified value.

Adding `@Before` on a field:

```text
type BlogPost {
    @Before
    publicationDate: PublicationDate
}
```

will result in the following operations being generated:

```text
@HttpOperation(method = "GET" , url = "/api/cask/BlogPost/publicationDate/Before/{before}")
operation findByPublicationDateBefore( 
  @PathVariable(name = "before") before : PublicationDate 
) : BlogPost[]( PublicationDate < before )   
```

### @After

Restricted to date-based fields \(`Date`, `Time`,`Instant`, `DateTime` \).  Generates a query that returns records where the field is after a specified value.

Adding `@After` on a field:

```text
type BlogPost {
    @After
    publicationDate: PublicationDate
}
```

will result in the following operations being generated:

```text
@HttpOperation(method = "GET" , url = "/api/cask/BlogPost/publicationDate/After/{after}")
operation findByPublicationDateAfter( 
   @PathVariable(name = "after") after : PublicationDate 
) : BlogPost[]( PublicationDate > after )
```

### @Between

Restricted to date-based fields \(`Date`, `Time`,`Instant`, `DateTime` \).  Generates a query that returns records where the field is between two values.

In the below example resulting operation allows you to query by date ranges:

```text
type BlogPost {
    @Between
    publicationDate: PublicationDate
}
```

will result in the following operation being generated:

```text
 @HttpOperation(method = "GET" , url = "/api/cask/BlogPost/publicationDate/Between/{start}/{end}")
operation findByPublicationDateBetween( 
           @PathVariable(name = "start") start : PublicationDate,
           @PathVariable(name = "end") start : PublicationDate  
       ) : BlogPost[]( PublicationDate >= start, PublicationDate < end )   
```

### @Association \(Deprecated\)

{% hint style="warning" %}
We've deprecated this annotation, as we feel it's not very self-documenting.  It will be replaced by new annotations - `@FindOne` and `@FindMany` in an upcoming release.
{% endhint %}

Adding @Association on top of field declaration:

```text
type BlogPost {
    @Association
    author : UserName
}
```

will generate the following operations:

```text
@HttpOperation(method = "GET" , url = "/api/cask/findOneBy/BlogPost/author/{author}")
operation findOneByAuthor( 
   @PathVariable(name = "author") author : UserName 
) : BlogPost
        
@HttpOperation(method = "POST" , url = "/api/cask/findMultipleBy/BlogPost/symbol")
operation findMultipleBySymbol( 
   @RequestBody author : UserName[] 
) : BlogPost[]

```


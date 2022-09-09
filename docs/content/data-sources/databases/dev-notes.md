# This is not documentation, this is for devs

# Design notes - JDBC connections


 * Introducing the concept of "connectors", which are a basic abstraction for defining a thing that connects to a data source.
 * The intent is that we'll use Connectors for connecting to databases, streaming sources (kafak / rabbit) etc
 * Migrated `OperationInvoker` to ` io.vyne.query.connectors

## Jdbc connections

Connection configuration starts with a `JdbcConnectionConfiguration` which is a 
lightweight (persistence & json serializable for UI's) entity for defining how to connect
to a db.

We have some UI tools for auto generating the forms to create these for
different databases (postgres / mysql etc)

This is the lightweight input into a `JdbcUrlConnectionProvider`, which is responsible for
building the `NamedParameterJdbcTemplate` used for interacting with the b.

Note `JdbcUrlConnectionProvider` implements `JdbcConnectionProvider` (which is a type of `Connector`).  We also have
a test friendly version called `SimpleJdbcConnectionProvider` which is used in tests,
and is simpler to set up.

The main entry point for Vyne getting a connection to a database is a 
`JdbcConnectionRegistry`, which contains a list of `JdbcConnectionProvider`s.

This is also used in the UI to display a list of available connections, and do UI stuff.

### Auth
We've recently added the authentication manager (after work on the JBC stuff stopped).
We need to think about how to make these interoperate, but it should be 
relatively simple, and just a matter of injecting the `AuthTokenRepository` into the
`JdbcUrlConnectionProvider` to get access to credentials.

See `AuthTokenInjectingRequestFactory` for how this is handled with HTTP.

## Declaring a database connection in a schema
Two new annotations
* `@Table`
* `@DatabaseService`


```taxi
@Table(connectionName = "movies", name = "movie")
model Movie {
   id : MovieId
   title : MovieTitle
}

@DatabaseService( connectionName = "movies" )
service MovieDb {
   vyneQl query movieQuery(body:VyneQlQuery):Movie[] with capabilities {
         filter(=,in,like),
         sum,
         count
      }
}
```
### Schema generation
Not yet implemented, but we'll need a way of Vyne generating a schema
for a table.

The actual schema is a pretty simple template:

```taxi
@DatabaseService( connectionName = "${connectionName}" )
service ${DatabaseName} {
   vyneQl query ${model}query(body:VyneQlQuery):${model}[] with capabilities {
         filter(=,in,like),
         sum,
         count
      }
}
```

We'd just add that for every table that's been mapped.

### Publishing a schema for a database
Not sure how we'll handle this.
At the moment, I suspect a good MVP is just a standard taxi project pulled
from a git repo, and served with our schema server.

The schema server now supports multiple destinations, so in theory this process looks like this:

 * Someone creates a new taxi project (`taxi init`)
 * Someone writes the schema
 * Commits and pushes to a git repo
 * They add that git repo into the config of the schema server, and off we go.

However, that separates connection details and schemas. Not ideal, but not a show-stopper
for v1 of this feature.

## Vyne invoking a query

We have a `JdbcInvoker` which is the equivalent to a `RemoteOperationInvoker`, both
of which implement `OperationInvoker`.

To Vyne, it's just an implementation of OperationInvoker, where `canSupport(Service,Operation) == true`
if the service is annotated with `@io.vyne.jdbc.DatabaseService`

Most of the heavy lifting is converting the TaxiQL query to a SQL query.

This is handled already in `TaxiQlToSqlConverter`.  Likely we'll need to flesh this
out, but it has decent support, and is tested.


# Persisting changes to a schema
This branch has the beginnings of letting users make changes to schemas
in the browser, which get written to disk.

This is part of letting users add new database connections, and map the tables to types, from
the browser.


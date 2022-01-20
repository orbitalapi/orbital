# Analytics Server

Analytics server stores and serves metadata about an executed query. Data lineage for each output attributes and remote services invoked
are examples of such metadata. 

Analytics server can store this data either in H2 or in Postgres and h2 is the default storage option. 
Vyne Query Server should also be configured to pass the required analytics data to the analytics server.
 
## Vyne Query Server Configuration:
You can configure Vyne Query Server either by using:

```
remote-analytics
```

spring profile or by passing:

```
--vyne.analytics.mode=Remote
--vyne.analytics.persistRemoteCallResponses=true
--vyne.analytics.persistResults=true
--spring.autoconfigure.exclude: org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration, org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration, org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
```

command line arguments.

Default spring application name of the analytics server is:

```
VYNE-ANALYTICS-SERVER
```

If you use a different spring application name for the analytics server through ```--spring.application.name``` configuration options then use:

```
--vyne.history.historyServerApplicationName
```

in Vyne Query Server to specify the analytics server application name.

## Analytics Server Configuration

By default Analytics server is configured to store analytics data in a H2 database. To use a Postgres database, create an empty postgres database and 
pass the following postgres related options:

```
--spring.datasource.url="jdbc:postgresql://DATABASE_HOST:DATABASE_PORT/DATABASE_NAME"
--spring.datasource.username="DB USERNAME"
--spring.datasource.password="DB PASSWORD"
```

For a quick spin with postgres, you can create an empty database called 'vyne-analytics' on your local postgres with 'vynedb' as username and password instance and
run the app with 'postgres' profile. 

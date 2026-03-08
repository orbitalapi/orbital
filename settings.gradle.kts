rootProject.name = "orbital-platform"

// Include all top-level modules
include(
    ":vyne-core-types",
    ":events-api", 
    ":vyne-search",
    ":utils",
    ":taxiql-query-engine",
    ":vyne-spring",
    ":vyne-spring-http",
    ":schema-store-client",
    ":vyne-query-service",
    ":vyne-client",
    ":vyne-query-api",
    ":vyne-client-spring",
    ":schema-server",
    ":pipelines",
    ":test-cli",
    ":vyne-csv-utils",
    ":vyne-history-core",
    ":connectors",
    ":schema-server-api",
    ":vyne-analytics-server",
    ":schema-management",
    ":schema-server-core",
    ":protobuf-utils",
    ":licensing",
    ":vyne-spring-http-client",
    ":datatype-converters",
    ":taxi-playground",
    ":station",
    ":auth-common",
    ":query-node-core",
    ":query-node-service",
    ":history-persistence",
    ":history-service",
    ":cockpit-core",
    ":query-node-api",
    ":monitoring-common",
    ":analytics",
    ":spring-utils",
    ":copilot",
    ":auth-tokens",
    ":query-node-native",
    ":formats-common",
    ":taxi-playground-core",
    ":policy-evaluator",
    ":avro-message-format",
    ":test-utils",
    ":plugin-api",
    ":plugin-loader",
    ":nebula-support",
    ":orbital-taxi-publisher",
    ":persistence-utils",
    ":metrics-utils",
    ":regression-tests"
)

// Include nested connector modules
include(
    ":connectors-core",
    ":connectors-calcite", 
    ":hazelcast-connector",
    ":jdbc-connector",
    ":kafka-connector",
    ":soap-connector"
)

// AWS Connectors
include(
    ":aws-connectors",
    ":aws-core",
    ":dynamo-db-connector",
    ":lambda-connector",
    ":s3-connector",
    ":sqs-connector"
)

// Azure Connectors
include(
    ":azure-connectors",
    ":blob-connector",
    ":servicebus-connector"
)

// NoSQL Connectors
include(
    ":nosql-connectors",
    ":mongodb-connector"
)

// Licensing modules
include(
    ":license-api",
    ":license-client"
)

// Pipeline modules
include(
    ":stream-engine",
    ":pipeline-jet",
    ":pipeline-jet-api"
)

// Schema management modules
include(
    ":schema-api",
    ":schema-consumer-api",
    ":schema-http-common",
    ":schema-http-consumer",
    ":schema-http-publisher",
    ":schema-publisher-api",
    ":schema-publisher-cli",
    ":schema-rsocket-common",
    ":schema-rsocket-consumer",
    ":schema-rsocket-publisher",
    ":schema-spring"
)

// Set project directories for nested modules
project(":connectors-core").projectDir = file("connectors/connectors-core")
project(":connectors-calcite").projectDir = file("connectors/connectors-calcite")
project(":hazelcast-connector").projectDir = file("connectors/hazelcast-connector")
project(":jdbc-connector").projectDir = file("connectors/jdbc-connector")
project(":kafka-connector").projectDir = file("connectors/kafka-connector")
project(":soap-connector").projectDir = file("connectors/soap-connector")

// AWS Connectors
project(":aws-connectors").projectDir = file("connectors/aws-connectors")
project(":aws-core").projectDir = file("connectors/aws-connectors/aws-core")
project(":dynamo-db-connector").projectDir = file("connectors/aws-connectors/dynamo-db-connector")
project(":lambda-connector").projectDir = file("connectors/aws-connectors/lambda-connector")
project(":s3-connector").projectDir = file("connectors/aws-connectors/s3-connector")
project(":sqs-connector").projectDir = file("connectors/aws-connectors/sqs-connector")

// Azure Connectors
project(":azure-connectors").projectDir = file("connectors/azure-connectors")
project(":blob-connector").projectDir = file("connectors/azure-connectors/blob-connector")
project(":servicebus-connector").projectDir = file("connectors/azure-connectors/servicebus-connector")

// NoSQL Connectors
project(":nosql-connectors").projectDir = file("connectors/nosql-connectors")
project(":mongodb-connector").projectDir = file("connectors/nosql-connectors/mongodb-connector")

// Licensing
project(":license-api").projectDir = file("licensing/license-api")
project(":license-client").projectDir = file("licensing/license-client")

// Pipelines
project(":stream-engine").projectDir = file("pipelines/stream-engine")
project(":pipeline-jet").projectDir = file("pipelines/pipeline-jet")
project(":pipeline-jet-api").projectDir = file("pipelines/pipeline-jet-api")

// Schema management
project(":schema-api").projectDir = file("schema-management/schema-api")
project(":schema-consumer-api").projectDir = file("schema-management/schema-consumer-api")
project(":schema-http-common").projectDir = file("schema-management/schema-http-common")
project(":schema-http-consumer").projectDir = file("schema-management/schema-http-consumer")
project(":schema-http-publisher").projectDir = file("schema-management/schema-http-publisher")
project(":schema-publisher-api").projectDir = file("schema-management/schema-publisher-api")
project(":schema-publisher-cli").projectDir = file("schema-management/schema-publisher-cli")
project(":schema-rsocket-common").projectDir = file("schema-management/schema-rsocket-common")
project(":schema-rsocket-consumer").projectDir = file("schema-management/schema-rsocket-consumer")
project(":schema-rsocket-publisher").projectDir = file("schema-management/schema-rsocket-publisher")
project(":schema-spring").projectDir = file("schema-management/schema-spring")
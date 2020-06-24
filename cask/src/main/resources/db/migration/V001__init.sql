CREATE TABLE IF NOT EXISTS CASK_CONFIG (
    tableName varchar(32) NOT NULL,
    qualifiedTypeName varchar(255) NOT NULL,
    versionHash varchar(32) NOT NULL,
    sourceSchemaIds text[] NOT NULL,
    sources text[] NOT NULL,
    deltaAgainstTableName varchar(32),
    insertedAt timestamp NOT NULL
);

CREATE TABLE IF NOT EXISTS CASK_MESSAGE (
    id varchar(40) NOT NULL PRIMARY  KEY, -- UUID
    qualifiedTypeName varchar(255) NOT NULL,
    readCachePath varchar(255),
    insertedAt timestamp NOT NULL
);

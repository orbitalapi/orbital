package com.orbitalhq.connectors.nosql.mongodb.registry

import com.orbitalhq.connectors.config.mongodb.MongoConnectionConfiguration
import com.orbitalhq.connectors.registry.ConnectionRegistry

interface MongoConnectionRegistry: ConnectionRegistry<MongoConnectionConfiguration>

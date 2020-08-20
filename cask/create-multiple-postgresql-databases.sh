#!/bin/bash

set -e
set -u

if [ -n "$POSTGRES_EXTRA_DATABASES" ]; then
	echo "Multiple database creation requested: $POSTGRES_EXTRA_DATABASES"
	for database in $(echo $POSTGRES_EXTRA_DATABASES | tr ',' ' '); do
		echo "  Creating database '$database'"
      psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
          CREATE DATABASE $database;
          GRANT ALL PRIVILEGES ON DATABASE $database TO $POSTGRES_USER;
EOSQL
	done
	echo "Multiple databases created"
fi

This module contains the database migrations to produce
an Orbital database schema.

Previously, we only had a single module that contained migrations,
(history-core), which was used in multiple runtime applications, depending
on the deployment config.

As things are evolving to using more database config, this approach
doesn't work, as we can't have flyway migrations provided from
multiple modules, without na

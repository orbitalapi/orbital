---
title: Configuration
description: How to configure cask behaviour at runtime
---

Cask behaviour can be configured at runtime with the following properties

## Query behaviours

All of the below config paramters are prefixed by `cask.query-options`. \(eg: `cask.query-options.findOneMatchesManyBehaviour`

| Setting | Value | Behaviour |
| :--- | :--- | :--- |
| `findOneMatchesManyBehaviour` | `THROW_ERROR`  \(default\) | If a findOne\(\) query matches multiple records, throw a `403: Bad Request` error  |
|  | `RETURN_FIRST` | If a findOne\(\) query matches multiple records, return the first record \(ordering is down to the cask config, and is often non-determnistic\) |
| `queryMatchesNoneBehaviour` | `RETURN_EMPTY` | If a query matches no records, return an empty response |
|  | `THROW_404` \(default\) | If a query matches no records, throw a 404 error |

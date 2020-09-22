package io.vyne.cask.query

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.lang.RuntimeException

// Define the exceptions we throw from the DAO layer when performing
// cask queries.
// Set the ResponseStatus code so that we generate 4xx errors, (indicating
// something wrong on the caller side), not 5xx errors (indicating something
// wrong in the server itself).
// Try and use meaningful messages

@ResponseStatus(HttpStatus.NOT_FOUND)
class CaskQueryEmptyResultsException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class CaskBadRequestException(message: String) : RuntimeException(message)

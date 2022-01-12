package io.vyne.queryService

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class OperationNotPermittedException : RuntimeException("This operation is not permitted")



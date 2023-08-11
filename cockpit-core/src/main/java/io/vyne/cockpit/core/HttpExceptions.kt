package io.vyne.cockpit.core

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class NotAuthorizedException(message: String = "This operation is not permitted") : RuntimeException(message)



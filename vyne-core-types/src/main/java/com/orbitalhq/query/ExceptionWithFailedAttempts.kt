package com.orbitalhq.query

import com.orbitalhq.models.DataSource

interface ExceptionWithFailedAttempts {
   val failedAttempts: List<DataSource>
}

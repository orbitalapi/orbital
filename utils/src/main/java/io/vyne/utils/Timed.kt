package io.vyne.utils

import com.google.common.base.Stopwatch
import io.vyne.utils.log
import java.util.concurrent.TimeUnit

object Timer {
    val log = Timer.log()
}

fun <T> xtimed(name: String, log: Boolean = false, timeUnit: TimeUnit = TimeUnit.MILLISECONDS, block: () -> T): T {
    return block()
}

fun <T> timed(name: String, log: Boolean = true, timeUnit: TimeUnit = TimeUnit.MILLISECONDS, block: () -> T): T {
    val stopwatch = Stopwatch.createStarted()
    val response = block()
    if (log) {
        Timer.log.info("$name completed in ${stopwatch.duration(timeUnit)}")
    }

    return response
}

fun Stopwatch.duration(timeUnit: TimeUnit): String {
    val suffix = when (timeUnit) {
        TimeUnit.SECONDS -> "s"
        TimeUnit.MILLISECONDS -> "ms"
        TimeUnit.MICROSECONDS -> "μs"
        TimeUnit.NANOSECONDS -> "ns"
        else -> timeUnit.name
    }
    return "${this.elapsed(timeUnit)}$suffix"
}

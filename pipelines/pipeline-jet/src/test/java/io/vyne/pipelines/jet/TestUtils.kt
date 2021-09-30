package io.vyne.pipelines.jet

import java.util.LinkedList
import java.util.Queue

fun <T> queueOf(vararg items:T):Queue<T> {
   return LinkedList(listOf(*items))
}

package com.orbitalhq.config

import com.orbitalhq.SourcePackage
import reactor.core.publisher.Flux

interface ConfigSourceLoader {
   /**
    * Returns the source of the hocon file(s).
    * Some loaders - such as schema loader
    * may return multiple files.  It's up to something
    * upstream to work out how to merge them together.
    *
    * The returned source pacakge should only contain the
    * requested configuration files.
    *
    * Design choice: Using SourcePackage here, as we
    * need to support updates / writes to this content, targeting
    * multiple different projects / write destinations.
    *
    * The package metatdata in the source package makes this possible
    *
    *
    */
   fun load(): List<SourcePackage>

   /**
    * A flux that emits a signal whenever the sources have changed.
    * The class of this Hocon loader is returned, to help with logging
    */
   val contentUpdated: Flux<Class<out ConfigSourceLoader>>
}



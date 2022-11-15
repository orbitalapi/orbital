package io.vyne.models.facts

import io.vyne.models.TypedInstance
import lang.taxi.accessors.ProjectionFunctionScope

data class ScopedFact(val scope: ProjectionFunctionScope, val fact: TypedInstance)

package com.orbitalhq.models.facts

import com.orbitalhq.models.TypedInstance
import lang.taxi.accessors.ProjectionFunctionScope

data class ScopedFact(val scope: ProjectionFunctionScope, val fact: TypedInstance)

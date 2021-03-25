package io.vyne.utils

import lang.taxi.types.ObjectType
import lang.taxi.types.Type

fun Type.isNonScalarObjectType(): Boolean {
   return this is ObjectType && this.fields.isNotEmpty()
}

package com.orbitalhq.models

import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.types.FieldProjection
import lang.taxi.types.FormatsAndZoneOffset

/**
 * A class who is capable of projecting one instance to another type.
 *
 * Depending on the implementation, the projector *may* support querying to resolve
 * entities - which, if so, is bleeding into the territory of a ProjectionProvider.
 *
 * This is intended for providing access back to a TypedObjectFactory when evaluating
 * expressions, whereas the ProjectionProvider is intended for executing top-level projections,
 * which include queries etc.
 */
interface ValueProjector {
   fun project(
      valueToProject: TypedInstance,
      projection: FieldProjection,
      // because the projection.targetType is a Taxi Type
      targetType: Type,
      schema: Schema,
      nullValues: Set<String>,
      source: DataSource,
      format: FormatsAndZoneOffset?,
      nullable: Boolean,
      allowContextQuerying: Boolean
   ): TypedInstance
}

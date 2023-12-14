import { findType, QualifiedName, Schema, Type } from 'src/app/services/schema';

export function buildInheritable(type: Type, schema: Schema): Inheritable {
  if (type.inheritsFrom && type.inheritsFrom.length > 1) {
    throw new Error('Multiple inheritance not supported');
  }
  let inheritsFrom: Inheritable = null;
  if (type.inheritsFrom && type.inheritsFrom.length === 1) {
    inheritsFrom = buildInheritable(findType(schema, type.inheritsFrom[0].fullyQualifiedName), schema);
  }
  let aliasFor: Inheritable = null;
  if (type.aliasForType) {
    aliasFor = buildInheritable(findType(schema, type.aliasForType.fullyQualifiedName), schema);
  }
  return {
    name: type.name,
    inheritsFrom,
    aliasFor
  };
}

export interface Inheritable {
  name: QualifiedName;
  inheritsFrom: Inheritable | null;
  aliasFor: Inheritable | null;
}

import { Documented, QualifiedName } from 'src/app/services/schema';
import { isNullOrUndefined } from 'util';

export class NewTypeSpec implements Documented {
  namespace: string | null;
  typeName: string;
  inheritsFrom: QualifiedName | null;
  typeDoc: string | null;

  // TODO : Work out if a type is a new type or not.
  isNewType = true;
}

export function qualifiedName(newTypeSpec: NewTypeSpec): QualifiedName {
  if (isNullOrUndefined(newTypeSpec.namespace)) {
    return QualifiedName.from(newTypeSpec.typeName);
  } else {
    return QualifiedName.from(`${newTypeSpec.namespace}.${newTypeSpec.typeName}`);
  }
}

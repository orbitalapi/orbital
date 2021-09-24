import {NewTypeSpec} from './type-editor.component';
import {QualifiedName} from '../services/schema';

export function generateTaxi(spec: NewTypeSpec): string {

  function generateNamespace(namespace: string | null): string {
    return namespace ? `namespace ${namespace}` : '';
  }

  function inheritsFrom(typeName: QualifiedName | null): string {
    return typeName ? `inherits ${typeName.fullyQualifiedName}` : '';
  }

  function typeDoc(value: string | null): string {
    return value ? `[[
${value}
]]` : '';
  }

  return `
${generateNamespace(spec.namespace)}

${typeDoc(spec.typeDoc)}
type ${spec.typeName} ${inheritsFrom(spec.inheritsFrom)}`.trim();
}

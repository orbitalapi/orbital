import { Operation, Parameter, QualifiedName, Schema, SchemaMember } from '../../services/schema';

export interface OperationSpecification {
  operation: Operation;
  name: QualifiedName;
  params: Parameter[];
}

export function getOperationFromQualifiedName(name: QualifiedName, schema: Schema):
  OperationSpecification {
  const operation = schema.operations.find(operation => operation.qualifiedName.fullyQualifiedName === name.fullyQualifiedName);
  return {
    operation,
    name: operation.qualifiedName,
    params: operation.parameters
  };
}

export function getOperationFromMember(selectedMember: SchemaMember | null, schema: Schema):
  OperationSpecification {
  if (selectedMember === null) {
    return {
      operation: null,
      name: null,
      params: []
    };
  }
  return getOperationFromQualifiedName(selectedMember.name, schema);
}


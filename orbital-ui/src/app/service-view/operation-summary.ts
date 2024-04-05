import { Operation, QualifiedName, splitOperationQualifiedName } from 'src/app/services/schema';
import { isNullOrUndefined } from '../utils/utils';

export interface OperationSummary {
  name: string;
  typeDoc: string | null;
  url: string;
  method: string;
  returnType: QualifiedName;
  serviceName: string;
}

export function toOperationSummary(operation: Operation): OperationSummary {
  if (isNullOrUndefined(operation)) return null;
  const httpOperationMetadata = operation.metadata.find(metadata => metadata.name.fullyQualifiedName === 'HttpOperation');
  const method = httpOperationMetadata ? httpOperationMetadata.params['method'] : null;
  const url = httpOperationMetadata ? httpOperationMetadata.params['url'] : null;

  const nameParts = splitOperationQualifiedName(operation.qualifiedName.fullyQualifiedName);
  const serviceName = nameParts.serviceName;
  return {
    name: nameParts.operationName,
    method: method,
    url: url,
    typeDoc: operation.typeDoc,
    returnType: operation.returnTypeName,
    serviceName
  } as OperationSummary;
}

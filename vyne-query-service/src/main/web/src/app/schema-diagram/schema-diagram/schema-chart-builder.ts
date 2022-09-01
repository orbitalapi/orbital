import {
  arrayMemberTypeNameOrTypeNameFromName,
  Operation,
  QualifiedName,
  QueryOperation,
  Schema,
  SchemaMember,
  SchemaMemberType,
  Service,
  ServiceMember,
  Type
} from '../../services/schema';
import { NodeType } from './schema-flow.react';
import { Node, XYPosition } from 'react-flow-renderer';
import { SchemaChartController } from './schema-chart.controller';
import { splitOperationQualifiedName } from '../../service-view/service-view.component';

function getNodeKind(member: SchemaMember): NodeType {
  if (member.kind === 'TYPE') {
    return 'Model';
  } else if (member.kind === 'SERVICE') {
    return 'Service'
  }
}

export interface Links {
  inputs: Link[];
  outputs: Link[];
}

export interface ModelLinks extends Links {
  attributeLinks: { [key: string]: Links }
}


function buildModelLinks(type: Type, schema: Schema, operations: ServiceMember[]): ModelLinks {
  const modelLinks = buildLinksForType(type.name, schema, operations, null)
  const thisNodeId = getNodeId('TYPE', type.name)
  const attributeLinks: { [key: string]: Links } = {};
  Object.keys(type.attributes).map(fieldName => {
    const fieldType = type.attributes[fieldName].type;
    attributeLinks[fieldName] = buildLinksForType(fieldType, schema, operations, {
      name: type.name,
      nodeId: thisNodeId,
      field: fieldName
    });
  });
  const returnValue = {
    attributeLinks,
    ...modelLinks
  }
  console.log(`Build links for ${type.name.shortDisplayName}`, returnValue);
  return returnValue;

}

export function collectionOperations(schema: Schema): ServiceMember[] {
  return schema.services.flatMap(service => (service.operations as ServiceMember[]).concat(service.queryOperations as ServiceMember[]))
}

function buildLinksForType(typeName: QualifiedName, schema: Schema, operations: ServiceMember[], parent: { name: QualifiedName, nodeId: string, field: string } | null): Links {
  const typeNodeId = getNodeId('TYPE', typeName)
  const consumingOperations: Link[] = [];

  // When building links for a type, we could either be linking to a model directly,
  // or to a field within another model.  (As determined by the parent input).
  const source = (parent) ? {
    sourceNodeId: parent.nodeId,
    sourceNodeName: parent.name,
    sourceHandleId: HandleIds.modelFieldOutbound(parent.name, parent.field),
    inverseSourceHandleId: HandleIds.modelFieldInbound(parent.name, parent.field)
  } : {
    sourceNodeId: typeNodeId,
    sourceNodeName: typeName,
    sourceHandleId: HandleIds.modelOutbound(typeName),
    inverseSourceHandleId: HandleIds.modelInbound(typeName)
  }

  // Build links from thisType -[to]-> OperationParam
  operations.forEach(operation => {
    const paramAcceptingType = operation.parameters.find(param => param.typeName.parameterizedName === typeName.parameterizedName);
    if (paramAcceptingType) {
      const nameParts = splitOperationQualifiedName(operation.qualifiedName.fullyQualifiedName);
      consumingOperations.push({
        ...source,

        targetNodeId: getNodeId('SERVICE', QualifiedName.from(nameParts.serviceName)),
        targetHandleId: HandleIds.serviceOperationInbound(QualifiedName.from(nameParts.serviceName), QualifiedName.from(nameParts.operationName)),
        targetNodeName: QualifiedName.from(nameParts.serviceName)
      })
    }
  })

  // BuildLinks from OperationReturnType -[to]-> this Type
  const producedByOperations: Link[] = [];
  operations.forEach(operation => {
    const nameParts = splitOperationQualifiedName(operation.qualifiedName.fullyQualifiedName);
    const returnType = operation.returnTypeName;
    if (returnType.parameterizedName == typeName.parameterizedName || returnType.parameters.length === 1 && returnType.parameters[0].parameterizedName === typeName.parameterizedName) {
      producedByOperations.push({
        sourceNodeId: getNodeId('SERVICE', QualifiedName.from(nameParts.serviceName)),
        sourceHandleId: HandleIds.serviceOperationOutbound(QualifiedName.from(nameParts.serviceName), QualifiedName.from(nameParts.operationName)),
        sourceNodeName: QualifiedName.from(nameParts.serviceName),


        targetNodeId: typeNodeId,
        targetHandleId: HandleIds.modelInbound(typeName),
        targetNodeName: typeName
      })
    }
  })

  const producedByOtherTypes: Link[] = [];
  const consumedByOtherTypes: Link[] = [];
  // Build links for models which have this type as an attribute
  schema.types
    .filter(t => !t.isScalar)
    .filter(typeInSchema => {
      // Exclude our own type...
      if (typeInSchema.name.fullyQualifiedName === typeName.fullyQualifiedName) {
        return false;
      }
      // ... or the parent type
      if (typeInSchema.name.fullyQualifiedName === parent?.name?.fullyQualifiedName) {
        return false;
      }
      return true;
    })
    .forEach(typeInSchema => {
      Object.keys(typeInSchema.attributes).forEach(fieldName => {
        const field = typeInSchema.attributes[fieldName];
        const fieldTypeName = arrayMemberTypeNameOrTypeNameFromName(field.type);
        if (fieldTypeName.fullyQualifiedName === typeName.fullyQualifiedName) {
          consumedByOtherTypes.push({
            ...source,

            targetNodeId: getNodeId('TYPE', typeInSchema.name),
            targetNodeName: typeInSchema.name,
            targetHandleId: HandleIds.modelFieldInbound(typeInSchema.name, fieldName)
          });
          producedByOtherTypes.push({
            sourceNodeId: getNodeId('TYPE', typeInSchema.name),
            sourceNodeName: typeInSchema.name,
            sourceHandleId: HandleIds.modelFieldOutbound(typeInSchema.name, fieldName),

            // This is the "inverse" side, ie., links to the source (as determined above),
            targetNodeId: source.sourceNodeId,
            targetNodeName: source.sourceNodeName,
            targetHandleId: source.inverseSourceHandleId
          })
        }
      })
    })

  return {
    outputs: consumingOperations.concat(consumedByOtherTypes),
    inputs: producedByOperations.concat(producedByOtherTypes)
  }
}

export interface ServiceLinks extends Links {
  operationLinks: { [key: string]: Links }
}

function buildServiceLinks(service: Service, schema: Schema, operations: ServiceMember[]): ServiceLinks {
  const operationLinks: { [key: string]: Links } = {};
  service.operations.forEach(operation => {
    operationLinks[operation.name] = buildOperationLinks(operation, service)
  })
  service.queryOperations.forEach(operation => {
    operationLinks[operation.name] = buildOperationLinks(operation, service)
  })
  return {
    outputs: [],
    inputs: [],
    operationLinks
  }
}

export interface Link {
  sourceNodeName: QualifiedName;
  sourceNodeId: string;
  sourceHandleId: string;

  targetNodeName: QualifiedName;
  targetNodeId: string;
  targetHandleId: string;
}

function buildOperationLinks(operation: Operation | QueryOperation, service: Service): Links {
  const serviceNodeId = getNodeId('SERVICE', service.name);
  const nameParts = splitOperationQualifiedName(operation.qualifiedName.fullyQualifiedName);
  const inputs: Link[] = operation.parameters.map(param => {
    let paramTypeName = arrayMemberTypeNameOrTypeNameFromName(param.typeName);
    return {
      sourceNodeId: getNodeId('TYPE', paramTypeName),
      sourceHandleId: HandleIds.modelOutbound(paramTypeName),
      sourceNodeName: paramTypeName,

      targetNodeId: serviceNodeId,
      targetHandleId: HandleIds.serviceOperationInbound(QualifiedName.from(nameParts.serviceName), QualifiedName.from(nameParts.operationName)),
      targetNodeName: QualifiedName.from(nameParts.serviceName)
    }
  });

  let returnTypeName = arrayMemberTypeNameOrTypeNameFromName(operation.returnTypeName);
  const outputs: Link[] = [{
    sourceNodeId: serviceNodeId,
    sourceHandleId: HandleIds.serviceOperationOutbound(QualifiedName.from(nameParts.serviceName), QualifiedName.from(nameParts.operationName)),
    sourceNodeName: service.name,

    targetNodeId: getNodeId('TYPE', returnTypeName),
    targetHandleId: HandleIds.modelInbound(returnTypeName),
    targetNodeName: returnTypeName,
  }]
  // Not sure if returning operation links here is helpful.
  return {
    inputs,
    outputs
  }
}

function buildLinks(member: SchemaMember, schema: Schema, operations: ServiceMember[]): Links {
  switch (member.kind) {
    case 'SERVICE':
      // I think the most natural links for a service are actually at the operation level.
      return buildServiceLinks(member.member as Service, schema, operations);
    case 'TYPE':
      return buildModelLinks(member.member as Type, schema, operations)
    default:
      throw new Error('No strategy for building links for member kind ' + member.kind);
  }
}

function getNodeId(schemaMemberType: SchemaMemberType, name: QualifiedName): string {
  return `${schemaMemberType.toLowerCase()}-${name.fullyQualifiedName}`
}

export function buildSchemaNode(schema: Schema, member: SchemaMember, operations: ServiceMember[], controller: SchemaChartController, position: XYPosition = {
  x: 100,
  y: 100
}): Node<MemberWithLinks> {
  const links = buildLinks(member, schema, operations)
  return {
    id: getNodeId(member.kind, member.name),
    draggable: true,
    selectable: true,

    data: {
      member,
      links,
      chartController: controller
    },
    type: getNodeKind(member),
    position
  } as Node<MemberWithLinks>
}


export interface MemberWithLinks {
  member: SchemaMember
  links: Links

  chartController: SchemaChartController
}


class HandleIds {
  static modelOutbound(name: QualifiedName): string {
    return `model-${name.fullyQualifiedName}-outbound`;
  }

  static modelInbound(name: QualifiedName): string {
    return `model-${name.fullyQualifiedName}-inbound`;
  }

  static modelFieldInbound(modelName: QualifiedName, fieldName: string): string {
    return `model-${modelName.fullyQualifiedName}-field-${fieldName}-inbound`;
  }

  static modelFieldOutbound(modelName: QualifiedName, fieldName: string): string {
    return `model-${modelName.fullyQualifiedName}-field-${fieldName}-outbound`;
  }

  static serviceOperationInbound(serviceName: QualifiedName, operationName: QualifiedName): string {
    return `service-${serviceName.fullyQualifiedName}-operation-${operationName.fullyQualifiedName}-inbound`
  }

  static serviceOperationOutbound(serviceName: QualifiedName, operationName: QualifiedName): string {
    return `service-${serviceName.fullyQualifiedName}-operation-${operationName.fullyQualifiedName}-outbound`
  }
}

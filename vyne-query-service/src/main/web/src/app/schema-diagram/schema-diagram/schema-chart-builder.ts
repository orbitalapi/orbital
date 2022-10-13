import {
  arrayMemberTypeNameOrTypeNameFromName,
  collectAllServiceOperations,
  QualifiedName,
  Schema,
  SchemaMember,
  SchemaMemberType,
  Service,
  ServiceMember,
  Type
} from '../../services/schema';
import { AppendLinksHandler, NodeType } from './schema-flow.react';
import { Edge, Node, Position, XYPosition } from 'reactflow';
import { splitOperationQualifiedName } from '../../service-view/service-view.component';
import { CSSProperties } from 'react';

function getNodeKind(member: SchemaMember): NodeType {
  if (member.kind === 'TYPE') {
    return 'Model';
  } else if (member.kind === 'SERVICE') {
    return 'Service'
  }
}

export interface EdgeParams {
  sourceCanFloat: boolean;
  targetCanFloat: boolean;
  label: string;
}

export interface Links {
  inputs: Link[];
  outputs: Link[];
}

export function collectLinks(links: Links | null): Link[] {
  if (!links) {
    return [];
  }
  return links.inputs.concat(links.outputs);
}


export function collectAllLinks(data: MemberWithLinks) {
  let childLinks = [];
  if (data.links instanceof ModelLinks || data.links instanceof ServiceLinks) {
    childLinks = data.links.collectAllChildLinks()
  }
  return collectLinks(data.links).concat(childLinks);
}

export interface HasChildLinks extends Links {
  collectAllChildLinks(): Link[]
}

export function edgeSourceAndTargetExist(nodes: Map<string, Node<MemberWithLinks>>, edge: Edge): boolean {
  const sourceAndTargetExist = nodes.has(edge.source) && nodes.has(edge.target)
  if (!sourceAndTargetExist) {
    return false;
  }
  const sourceHandleExists = collectAllLinks(nodes.get(edge.source).data)
    .some(link => link.sourceHandleId === edge.sourceHandle);
  const targetHandleExists = collectAllLinks(nodes.get(edge.target).data)
    .some(link => link.targetHandleId === edge.targetHandle);
  return sourceHandleExists && targetHandleExists;
}

export class ModelLinks implements HasChildLinks {
  public constructor(public readonly inputs: Link[], public readonly outputs: Link[], public readonly attributeLinks: { [key: string]: Links }) {
  }

  collectAllChildLinks(): Link[] {
    return Object.values(this.attributeLinks).flatMap(links => collectLinks(links));
  }

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
  const returnValue = new ModelLinks(
    modelLinks.inputs,
    modelLinks.outputs,
    attributeLinks
  )
  return returnValue;

}

export function collectionOperations(schema: Schema): ServiceMember[] {
  return schema.services.flatMap(service => {
    return (service.operations as ServiceMember[])
      .concat(service.queryOperations as ServiceMember[])
      .concat(service.tableOperations as ServiceMember[])
      .concat(service.streamOperations as ServiceMember[])
  })
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
    inverseSourceHandleId: HandleIds.modelFieldInbound(parent.name, parent.field),
    sourceMemberType: 'TYPE' as SchemaMemberType,
  } : {
    sourceNodeId: typeNodeId,
    sourceNodeName: typeName,
    sourceHandleId: HandleIds.modelOutbound(typeName),
    sourceMemberType: 'TYPE' as SchemaMemberType,
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
        targetNodeName: QualifiedName.from(nameParts.serviceName),
        targetMemberType: 'OPERATION'
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
        sourceMemberType: 'OPERATION',


        targetNodeId: typeNodeId,
        targetHandleId: HandleIds.modelInbound(typeName),
        targetNodeName: typeName,
        targetMemberType: 'TYPE',
      })
    }
  })

  const modelLinks: Map<string, Link> = new Map<string, Link>();
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
          const link = {
            ...source,

            targetNodeId: getNodeId('TYPE', typeInSchema.name),
            targetNodeName: typeInSchema.name,
            targetHandleId: HandleIds.modelFieldInbound(typeInSchema.name, fieldName),
            targetMemberType: 'TYPE'
          } as Link;
          // We want an id where source -> destination and destination -> source provide the same id.
          // This is to prevent duplicate linkes
          const linkId = [link.sourceNodeId, link.targetNodeId].sort((a, b) => a.localeCompare(b))
            .join('-')
          link.linkId = linkId;
          modelLinks.set(linkId, link);

          // Commented out, as model links are bidirectional, so we only need one.
          // producedByOtherTypes.push({
          //   sourceNodeId: getNodeId('TYPE', typeInSchema.name),
          //   sourceNodeName: typeInSchema.name,
          //   sourceHandleId: HandleIds.modelFieldOutbound(typeInSchema.name, fieldName),
          //   sourceMemberType: 'TYPE',
          //
          //   // This is the "inverse" side, ie., links to the source (as determined above),
          //   targetNodeId: source.sourceNodeId,
          //   targetNodeName: source.sourceNodeName,
          //   targetHandleId: source.inverseSourceHandleId,
          //   targetMemberType: source.sourceMemberType
          // })
        }
      })
    })

  return {
    // Model links "float" (ie., have no concept of input/output),
    // so it doesn't matter where we add them.
    outputs: consumingOperations.concat(Array.from(modelLinks.values())),
    inputs: producedByOperations
  }
}

export class ServiceLinks implements HasChildLinks {
  constructor(public readonly inputs: Link[], public readonly outputs: Link[], public readonly operationLinks: { [key: string]: Links }) {
  }

  collectAllChildLinks(): Link[] {
    return Object.values(this.operationLinks).flatMap(links => collectLinks(links));
  }


}

function buildServiceLinks(service: Service, schema: Schema, operations: ServiceMember[]): ServiceLinks {
  const operationLinks: { [key: string]: Links } = {};
  collectAllServiceOperations(service)
    .forEach(serviceMember => {
      operationLinks[serviceMember.name] = buildOperationLinks(serviceMember, service)
    })
  return new ServiceLinks(
    [],
    [],
    operationLinks)
}

export interface Link {
  sourceNodeName: QualifiedName;
  sourceNodeId: string;
  sourceHandleId: string;
  sourceMemberType: SchemaMemberType;

  targetNodeName: QualifiedName;
  targetNodeId: string;
  targetHandleId: string;
  targetMemberType: SchemaMemberType;

  linkStyle?: CSSProperties;

  // Optional, as we only set it if we wish to check for duplicates
  linkId?: string;
}

function buildOperationLinks(operation: ServiceMember, service: Service): Links {
  const serviceNodeId = getNodeId('SERVICE', service.name);
  const nameParts = splitOperationQualifiedName(operation.qualifiedName.fullyQualifiedName);
  const inputs: Link[] = operation.parameters.map(param => {
    let paramTypeName = arrayMemberTypeNameOrTypeNameFromName(param.typeName);
    return {
      sourceNodeId: getNodeId('TYPE', paramTypeName),
      sourceHandleId: HandleIds.modelOutbound(paramTypeName),
      sourceNodeName: paramTypeName,
      sourceMemberType: 'TYPE',

      targetNodeId: serviceNodeId,
      targetHandleId: HandleIds.serviceOperationInbound(QualifiedName.from(nameParts.serviceName), QualifiedName.from(nameParts.operationName)),
      targetNodeName: QualifiedName.from(nameParts.serviceName),
      targetMemberType: 'OPERATION'

    }
  });

  let returnTypeName = arrayMemberTypeNameOrTypeNameFromName(operation.returnTypeName);
  const outputs: Link[] = [{
    sourceNodeId: serviceNodeId,
    sourceHandleId: HandleIds.serviceOperationOutbound(QualifiedName.from(nameParts.serviceName), QualifiedName.from(nameParts.operationName)),
    sourceNodeName: service.name,
    sourceMemberType: 'OPERATION',

    targetNodeId: getNodeId('TYPE', returnTypeName),
    targetHandleId: HandleIds.modelInbound(returnTypeName),
    targetNodeName: returnTypeName,
    targetMemberType: 'TYPE'
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

export function getNodeId(schemaMemberType: SchemaMemberType, name: QualifiedName): string {
  return `${schemaMemberType.toLowerCase()}-${name.fullyQualifiedName}`
}

export function buildSchemaNode(schema: Schema, member: SchemaMember, operations: ServiceMember[], appendLinksHandler: AppendLinksHandler, position: XYPosition = {
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
      appendNodesHandler: appendLinksHandler
    },
    type: getNodeKind(member),
    position
  } as Node<MemberWithLinks>
}


export interface MemberWithLinks {
  member: SchemaMember;
  links: Links;

  appendNodesHandler: AppendLinksHandler
}


export class HandleIds {
  static modelOutbound(name: QualifiedName): string {
    return `model-${name.fullyQualifiedName}`;
  }

  static modelInbound(name: QualifiedName): string {
    return `model-${name.fullyQualifiedName}`;
  }

  static modelFieldInbound(modelName: QualifiedName, fieldName: string): string {
    return `model-${modelName.fullyQualifiedName}-field-${fieldName}`;
  }

  static modelFieldOutbound(modelName: QualifiedName, fieldName: string): string {
    return `model-${modelName.fullyQualifiedName}-field-${fieldName}`;
  }

  static serviceOperationInbound(serviceName: QualifiedName, operationName: QualifiedName): string {
    return `service-${serviceName.fullyQualifiedName}-operation-${operationName.fullyQualifiedName}-inbound`
  }

  static serviceOperationOutbound(serviceName: QualifiedName, operationName: QualifiedName): string {
    return `service-${serviceName.fullyQualifiedName}-operation-${operationName.fullyQualifiedName}-outbound`
  }

  static appendPositionToHandleId(handleId: string, position: Position): string {
    // We need a consistent handle id, which is the one React Flow uses for referencing
    // the handle within the node.
    // However, we also need distinct ids for each handle.
    // The handles themselves are floating, so each link can have multiple handles, each of
    // which needs a unique id.
    // To deal with this, we append a handleId for the RHS, but not the LHS.
    // This is simple, and consistent
    return position === Position.Right ? handleId + '-rhs' : handleId;
  }
}

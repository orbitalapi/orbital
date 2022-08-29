import {
  findSchemaMember,
  Operation,
  QualifiedName,
  Schema,
  SchemaMember,
  Service,
  ServiceMember,
  Type
} from '../../services/schema';
import { NodeType, SchemaChartState } from './schema-flow.react';
import { Edge, Node, XYPosition } from 'react-flow-renderer';
import { SchemaChartController } from './schema-chart.controller';

function getNodeKind(member: SchemaMember): NodeType {
  if (member.kind === 'TYPE') {
    return 'Model';
  } else if (member.kind === 'SERVICE') {
    // TODO : How do we specify API vs Db vs Kafka etc?
    const service = member.member as Service;
    switch (service.serviceType) {
      case 'Kafka':
        return 'Kafka';
      case 'Api':
        return 'Api';
      default: {
        console.warn(`No node type defined for ${service.serviceType}, so will use Api`)
        return 'Api';
      }
    }
  } else {
    throw new Error('No node type defined for schema member type ' + member.kind)
  }

}

export interface Links {
  producedBy: QualifiedName[];
  consumedBy: QualifiedName[];
}

export interface ModelLinks extends Links {
  attributeLinks: { [key: string]: Links }
}


function buildModelLinks(type: Type, schema: Schema, operations: ServiceMember[]): ModelLinks {
  const modelLinks = buildLinksForType(type.name, schema, operations)

  const attributeLinks: { [key: string]: Links } = {};
  Object.keys(type.attributes).map(fieldName => {
    const fieldType = type.attributes[fieldName].type;
    attributeLinks[fieldName] = buildLinksForType(fieldType, schema, operations)
  });
  return {
    attributeLinks,
    ...modelLinks
  }

}

export function collectionOperations(schema: Schema): ServiceMember[] {
  return schema.services.flatMap(service => (service.operations as ServiceMember[]).concat(service.queryOperations as ServiceMember[]))
}

function buildLinksForType(typeName: QualifiedName, schema: Schema, operations: ServiceMember[]): Links {
  const consumedBy: QualifiedName[] = operations.filter(operation => {
      return operation.parameters.some(p => p.typeName.parameterizedName === typeName.parameterizedName);
    }
  ).map(operation => operation.qualifiedName)

  const producedBy: QualifiedName[] = operations.filter(operation => {
    const returnType = operation.returnTypeName;
    return returnType.parameterizedName == typeName.parameterizedName || returnType.parameters.length === 1 && returnType.parameters[0].parameterizedName === typeName.parameterizedName;
  }).map(o => o.qualifiedName)

  return {
    consumedBy,
    producedBy
  }

}

function buildOperationLinks(operation: Operation, schema: Schema, operations: ServiceMember[]): Links {
  // Not sure if returning operation links here is helpful.
  return { producedBy: [], consumedBy: [] }
}

function buildLinks(member: SchemaMember, schema: Schema, operations: ServiceMember[]): Links {
  switch (member.kind) {
    case 'SERVICE':
      // I think the most natural links for a service are actually at the operation level.
      return { producedBy: [], consumedBy: [] }
    case 'OPERATION':
      return buildOperationLinks(member.member as Operation, schema, operations)
    case 'TYPE':
      return buildModelLinks(member.member as Type, schema, operations)
  }
}

export function buildSchemaNode(schema: Schema, member: SchemaMember, operations: ServiceMember[], controller: SchemaChartController, position: XYPosition = {
  x: 0,
  y: 0
}): Node<MemberWithLinks> {
  const links = buildLinks(member, schema, operations)
  return {
    id: member.name.parameterizedName,
    draggable: true,
    data: {
      member,
      links,
      chartController: controller
    },
    type: getNodeKind(member),
    position
  } as Node<MemberWithLinks>
}


export function buildSchemaChart(schema: Schema, memberNames: string[], controller: SchemaChartController): SchemaChartState {
  const operations = collectionOperations(schema);
  const nodes = memberNames.map(name => buildSchemaNode(
    schema, findSchemaMember(schema, name), operations, controller
  ))


  const edges: Edge[] = [];
  return new SchemaChartState(
    nodes,
    edges
  );

}

export interface MemberWithLinks {
  member: SchemaMember
  links: Links

  chartController: SchemaChartController
}



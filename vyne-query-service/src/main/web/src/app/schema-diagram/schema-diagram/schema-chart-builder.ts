import { findSchemaMember, Schema, SchemaMember, Service } from '../../services/schema';
import { NodeType, SchemaChartState } from './schema-flow.react';
import { Edge, Node } from 'react-flow-renderer';

function findNodeType(member: SchemaMember): NodeType {
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

export function buildSchemaChart(schema: Schema, memberNames: string[]): SchemaChartState {
  const nodes = memberNames.map(memberName => {
    const member = findSchemaMember(schema, memberName)
    const nodeType: NodeType = findNodeType(member)
    return {
      id: member.name.parameterizedName,
      data: member,
      type: nodeType,
      position: {
        y: 10,
        x: 10
      }
    } as Node<SchemaMember>
  })
  const edges: Edge[] = [];
  return {
    nodes,
    edges
  }

}

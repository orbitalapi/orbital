import { findSchemaMember, Schema, SchemaMember } from '../../services/schema';
import { NodeType, SchemaChartState } from './schema-flow.react';
import { Edge, Node } from 'react-flow-renderer';

function findNodeType(member: SchemaMember): NodeType {
  if (member.kind === 'TYPE') {
    return 'Model';
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

import * as React from 'react';
import { Handle, Node, Position } from 'react-flow-renderer';
import { Operation, SchemaMember, Service } from '../../../services/schema';
import { SchemaNodeContainer } from './schema-node-container';
import { MemberWithLinks } from '../schema-chart-builder';

function KafkaNode(node: Node<MemberWithLinks>) {
  const service = node.data.member.member as Service;

  function OperationList() {
    return (<>
      <tr className={'small-heading'}>
        <td colSpan={2}>Topics</td>
      </tr>
      {service.operations.map(operation => {
        return <tr>
          <td>{operation.metadata.find(m => m.name.fullyQualifiedName == 'io.vyne.kafka.KafkaOperation')?.params['topic']}</td>
          <td>{operation.returnTypeName.shortDisplayName}</td>
        </tr>
      })}
    </>)
  }

  function QueryOperationList() {
    return (<>
      <tr className={'small-heading'}>
        <td colSpan={2}>Query Operations</td>
      </tr>
      {service.queryOperations.map(operation => {
        return <tr>
          <td><span className={'tag'}>{operation.grammar}</span>{operation.qualifiedName.shortDisplayName}</td>
          <td>{operation.returnTypeName.shortDisplayName}</td>
        </tr>
      })}
    </>)
  }


  return (
    <SchemaNodeContainer>
      <table>
        <thead>
        <tr className={'small-heading'}>
          <th colSpan={2}>Kafka</th>
        </tr>
        <tr className={'member-name'}>
          <th colSpan={2}>{node.data.member.name.shortDisplayName}</th>
        </tr>

        </thead>
        <tbody>
        {service.operations.length > 0 && <OperationList></OperationList>}
        {service.queryOperations.length > 0 && <QueryOperationList></QueryOperationList>}
        </tbody>
      </table>
      <Handle type="source" position={Position.Bottom} id="a"/>
    </SchemaNodeContainer>
  )
}

export default KafkaNode

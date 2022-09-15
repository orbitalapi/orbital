import * as React from 'react';
import { Node, Position } from 'react-flow-renderer';
import { Operation, QueryOperation, Service, StreamOperation, TableOperation } from '../../../services/schema';
import { SchemaNodeContainer } from './schema-node-container';
import { collectLinks, Links, MemberWithLinks, ServiceLinks } from '../schema-chart-builder';
import { LinkHandle } from './link-handle';

type OperationLike = Operation | QueryOperation | StreamOperation | TableOperation;

interface OperationsListProps {
  operations: OperationLike[]
  heading: string;
  showInputs: boolean;
}


function ApiNode(node: Node<MemberWithLinks>) {
  const service = node.data.member.member as Service;

  function NoArgOperation(props: { operation: OperationLike, operationLinks: Links }) {
    const { operation, operationLinks } = props;
    return (<>
      <tr>
        <td colSpan={2} className="">
          <div className={'handle-container'}>
            <LinkHandle node={node} links={operationLinks.inputs} position={Position.Left}></LinkHandle>
            {operation.qualifiedName.shortDisplayName}
          </div>

        </td>
        <td>
          <div className={'handle-container'}>
            {operation.returnTypeName.shortDisplayName}
            <LinkHandle node={node} links={operationLinks.outputs} position={Position.Right}></LinkHandle>
          </div>
        </td>
      </tr>
    </>)
  }

  function OperationWithArgs(props: { operation: OperationLike, operationLinks: Links }) {
    const { operation, operationLinks } = props;
    return (<>
      <tr>
        <td colSpan={3} className="operation-name">
          {operation.qualifiedName.shortDisplayName}
        </td>
      </tr>
      <tr>
        <td className={'small-heading'}>Input</td>
        <td></td>
        <td className={'small-heading'}>Output</td>
      </tr>
      <tr className={'operation-params'}>
        <td className={'parameter-list handle-container'}>
          <div className="parameter-list">
            {operation.parameters.map(param => {
              return param.typeName.shortDisplayName
            }).join(', ')
            }
            <LinkHandle node={node} links={operationLinks.inputs} position={Position.Left}></LinkHandle>
          </div>
        </td>
        <td>{operation.parameters.length > 0 ? '→' : ''}</td>
        <td>
          <div className={'handle-container'}>
            {operation.returnTypeName.shortDisplayName}
            <LinkHandle node={node} links={operationLinks.outputs} position={Position.Right}></LinkHandle>
          </div>
        </td>
      </tr>
    </>)
  }

  function OperationList(props: OperationsListProps) {
    return (<>
      <tr className={'small-heading'}>
        <td colSpan={3}>{props.heading}</td>
      </tr>
      {props.operations.map(operation => {
        const serviceLinks = node.data.links as ServiceLinks;
        const operationLinks = serviceLinks.operationLinks[operation.name];
        return (<React.Fragment key={operation.qualifiedName.parameterizedName}>
          {props.showInputs ? (
              <OperationWithArgs operation={operation} operationLinks={operationLinks}></OperationWithArgs>) :
            (<NoArgOperation operation={operation} operationLinks={operationLinks}></NoArgOperation>)}
        </React.Fragment>)
      })}
    </>)
  }

  function VersionTags() {
    if (!service.version) {
      return <></>
    } else {
      return <tr>
        <th colSpan={2}>
          <div className={'version-tags'}>
            {service.version.map(version => {
              return <div id={'version.version'} className={'version-tag'}>{version.version}</div>
            })}
          </div>
        </th>
      </tr>
    }
  }

  function getIcon(): string {
    switch (service.serviceKind) {
      case 'Api':
        return 'assets/img/chart-icons/api-icon.svg';
      case 'Database':
        return 'assets/img/chart-icons/database-icon.svg';
      case 'Kafka' :
        return 'assets/img/chart-icons/kafka-icon.svg';
      default: {
        console.log(`No icon defined for service kind ${service.serviceKind}, so using Api`);
        return 'assets/img/chart-icons/api-icon.svg';
      }
    }
  }

  return (
    <SchemaNodeContainer>
      <div className={'node-icon-outer-container'}>
        <div className={'node-icon-container'}>
          <img src={getIcon()}></img>
        </div>
      </div>


      <table className={'service'}>
        <thead>
        <tr className={'small-heading'}>
          <th colSpan={3}>{service.serviceKind || 'Service'}</th>
        </tr>
        <tr className={'member-name'}>
          <th colSpan={3}>{node.data.member.name.shortDisplayName}</th>
        </tr>
        <VersionTags></VersionTags>

        </thead>
        <tbody>
        {service.operations.length > 0 &&
          <OperationList operations={service.operations} heading={'Operations'} showInputs={true}></OperationList>}
        {service.queryOperations.length > 0 &&
          <OperationList operations={service.queryOperations} heading={'Operations'} showInputs={true}></OperationList>}
        {service.tableOperations.length > 0 &&
          <OperationList operations={service.tableOperations} heading={'Tables'} showInputs={false}></OperationList>}
        {service.streamOperations.length > 0 &&
          <OperationList operations={service.streamOperations} heading={'Streams'} showInputs={false}></OperationList>}
        </tbody>
      </table>
    </SchemaNodeContainer>
  )
}

export default ApiNode

import { Handle, Node, Position } from 'react-flow-renderer';
import { Link, MemberWithLinks } from '../schema-chart-builder';
import { HandleType } from 'react-flow-renderer/dist/esm/types/handles';
import * as React from 'react';

export interface LinkHandleProps {
  node: Node<MemberWithLinks>,
  links: Link[],
  handleType: HandleType
  handleId? : string
}

export function LinkHandle(props: LinkHandleProps) {
  if (!props.links || props.links.length === 0) {
    return <></>
  }
  const thisHandleId = props.handleType === 'source' ? props.links[0].sourceHandleId : props.links[0].targetHandleId;
  const position = (props.handleType === 'source') ? Position.Right : Position.Left;

  function clickHandler(handleId: string) {
    if (!props.links || props.links.length === 0) {
    }
    const controller = props.node.data.chartController;
    controller.appendNodesAndEdgesForLinks(props.node, props.links, position);
  }

  return <Handle type={'source'} position={position}
                 id={thisHandleId}
                 key={thisHandleId}
                 onClick={clickHandler}></Handle>
}

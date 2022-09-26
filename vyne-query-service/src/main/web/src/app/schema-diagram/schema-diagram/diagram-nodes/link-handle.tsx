import { Handle, Node, Position } from 'react-flow-renderer';
import { HandleIds, Link, MemberWithLinks } from '../schema-chart-builder';
import * as React from 'react';
import { AppendLinksProps } from 'src/app/schema-diagram/schema-diagram/schema-flow.react';

export interface LinkHandleProps {
  node: Node<MemberWithLinks>,
  links: Link[],
  position: Position.Right | Position.Left,
  handleId?: string,
  allowConnectionToFloat?: boolean
}

export function LinkHandle(props: LinkHandleProps) {
  if (!props.links || props.links.length === 0) {
    return <></>
  }
  const handleId = props.node.id === props.links[0].sourceNodeId ? props.links[0].sourceHandleId : props.links[0].targetHandleId;
  const handleIdWithSide = props.allowConnectionToFloat ? HandleIds.appendPositionToHandleId(handleId, props.position) : handleId;

  function clickHandler(handleId: string) {
    if (!props.links || props.links.length === 0) {
    }
    props.node.data.appendNodesHandler({
      nodeRequestingLink: props.node,
      links: props.links,
      direction: props.position
    } as AppendLinksProps);
  }

  return <Handle type={'source'} position={props.position}
                 id={handleIdWithSide}
                 key={handleIdWithSide}
                 onClick={clickHandler}></Handle>
}

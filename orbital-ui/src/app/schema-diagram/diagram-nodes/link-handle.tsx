import { Handle, Node, Position } from '@xyflow/react';
import { HandleIds, Link, MemberWithLinks } from '../schema-chart-builder';
import * as React from 'react';
import { AppendLinksProps } from 'src/app/schema-diagram/schema-flow.react';

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
  if (props.links.some(link => link.linkKind === 'lineage')) {
  }

  // TODO : For some reason, we're getting passed a collection of links, not all of which are relevant to this
  // node.
  const ourLinks = props.links.filter(link => link.sourceNodeId === props.node.id || link.targetNodeId === props.node.id)
  if (ourLinks.length === 0) {
    console.error(`Incorrect links were passed to a handle - there were no links present for node id ${props.node.id}, instead the following links were present:`, props.links)
    return <></>;
  }

  const handleId = props.node.id === ourLinks[0].sourceNodeId ? ourLinks[0].sourceHandleId : ourLinks[0].targetHandleId;
  const handleIdWithSide = props.allowConnectionToFloat ? HandleIds.appendPositionToHandleId(handleId, props.position) : handleId;

  function clickHandler() {
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

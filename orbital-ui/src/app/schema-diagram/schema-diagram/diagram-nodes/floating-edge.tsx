import * as React from 'react';
import { useCallback } from 'react';
import { BaseEdge, useStore, getBezierPath } from 'reactflow';

import { getEdgeCoords } from './edge-utils';

// Taken from : https://reactflow.dev/docs/examples/edges/simple-floating-edges/
function SimpleFloatingEdge({ source, target, markerEnd, style, data, sourceHandleId, targetHandleId }) {
  const sourceNode = useStore(useCallback((store) => store.nodeInternals.get(source), [source]));
  const targetNode = useStore(useCallback((store) => store.nodeInternals.get(target), [target]));

  if (!sourceNode || !targetNode) {
    return null;
  }

  const {
    sx,
    sy,
    tx,
    ty,
    sourcePos,
    targetPos
  } = getEdgeCoords(sourceNode, sourceHandleId, data.sourceCanFloat, targetNode, targetHandleId, data.targetCanFloat);

  const [edgePath, labelX, labelY] = getBezierPath({
    sourceX: sx,
    sourceY: sy,
    sourcePosition: sourcePos,
    targetPosition: targetPos,
    targetX: tx,
    targetY: ty,
  })
  const labelXNumb = labelX as any as number;

  return (
    <BaseEdge
      labelX={labelXNumb}
      labelY={labelY}
      path={edgePath}
      label={data.label}
      labelStyle={{opacity: style.opacity, transition: 'opacity 150ms ease-in-out'}}
      labelBgStyle={{opacity: 0.9}}
      style={style}
      markerEnd={markerEnd}
      interactionWidth={15}
    />
  );
}

export default SimpleFloatingEdge;

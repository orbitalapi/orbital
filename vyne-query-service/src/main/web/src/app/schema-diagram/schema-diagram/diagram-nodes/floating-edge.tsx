import { useCallback } from 'react';
import { useStore, getBezierPath } from 'react-flow-renderer';

import { getEdgeParams } from './edge-utils';
import * as React from 'react';

// Taken from : https://reactflow.dev/docs/examples/edges/simple-floating-edges/
function SimpleFloatingEdge({ id, source, target, markerEnd, style, sourceHandleId, targetHandleId }) {
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
  } = getEdgeParams(sourceNode, sourceHandleId, targetNode, targetHandleId);

  const d = getBezierPath({
    sourceX: sx,
    sourceY: sy,
    sourcePosition: sourcePos,
    targetPosition: targetPos,
    targetX: tx,
    targetY: ty,
  });

  return (
    <path
      id={id}
      className="react-flow__edge-path"
      d={d}
      strokeWidth={5}
      markerEnd={markerEnd}
      style={style}
    />
  );
}

export default SimpleFloatingEdge;

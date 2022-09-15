import { useCallback } from 'react';
import { useStore, getBezierPath, EdgeText, getSimpleBezierEdgeCenter } from 'react-flow-renderer';

import { getEdgeCoords } from './edge-utils';
import * as React from 'react';
import { EdgeParams } from 'src/app/schema-diagram/schema-diagram/schema-chart-builder';

// Taken from : https://reactflow.dev/docs/examples/edges/simple-floating-edges/
function SimpleFloatingEdge({ id, source, target, markerEnd, style, sourceHandleId, targetHandleId, label, data }) {
  const sourceNode = useStore(useCallback((store) => store.nodeInternals.get(source), [source]));
  const targetNode = useStore(useCallback((store) => store.nodeInternals.get(target), [target]));

  if (!sourceNode || !targetNode) {
    return null;
  }
  const edgeParams: EdgeParams = data as EdgeParams;

  const {
    sx,
    sy,
    tx,
    ty,
    sourcePos,
    targetPos
  } = getEdgeCoords(sourceNode, sourceHandleId, edgeParams.sourceCanFloat,  targetNode, targetHandleId, edgeParams.targetCanFloat);

  const d = getBezierPath({
    sourceX: sx,
    sourceY: sy,
    sourcePosition: sourcePos,
    targetPosition: targetPos,
    targetX: tx,
    targetY: ty,
  });

  const [centerX, centerY, offsetX, offsetY] = getSimpleBezierEdgeCenter({
    sourceX: sx,
    sourceY: sy,
    sourcePosition: sourcePos,
    targetPosition: targetPos,
    targetX: tx,
    targetY: ty,
  })

  return (
    <>
      <EdgeText
        x={centerX}
        y={centerY}
        label={label}
      />
      <path
        id={id}
        className="react-flow__edge-path"
        d={d}
        strokeWidth={5}
        markerEnd={markerEnd}
        style={style}
      />
    </>
  );
}

export default SimpleFloatingEdge;

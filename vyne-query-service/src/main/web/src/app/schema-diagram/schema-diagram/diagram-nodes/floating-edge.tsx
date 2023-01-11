import { useCallback } from 'react';
import { useStore, getBezierPath, getSmoothStepPath } from 'reactflow';

import { getEdgeCoords } from './edge-utils';
import * as React from 'react';
import { EdgeParams } from 'src/app/schema-diagram/schema-diagram/schema-chart-builder';
import { BaseEdge } from 'reactflow';

// Taken from : https://reactflow.dev/docs/examples/edges/simple-floating-edges/
function SimpleFloatingEdge({
                              id, source, target, markerEnd, style, data, sourceX,
                              sourceY,
                              targetX,
                              targetY,
                              sourcePosition,
                              sourceHandleId,
                              targetPosition,
                              targetHandleId
                            }) {
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
    <BaseEdge labelX={labelXNumb}
              labelY={labelY}
              path={edgePath}
              label={data.label}
              markerEnd={markerEnd}
              style={style}
              />
  );
}

export default SimpleFloatingEdge;

import { useCallback } from 'react';
import { useStore, getBezierPath, getSmoothStepPath } from 'reactflow';

import { getEdgeCoords } from './edge-utils';
import * as React from 'react';
import { EdgeParams } from 'src/app/schema-diagram/schema-diagram/schema-chart-builder';

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


  const [edgePath] = getBezierPath({
    sourceX: sx,
    sourceY: sy,
    sourcePosition: sourcePos,
    targetPosition: targetPos,
    targetX: tx,
    targetY: ty,
  })

  return (
    <>
      <path
        id={id}
        className="react-flow__edge-path"
        d={edgePath}
        strokeWidth={5}
        markerEnd={markerEnd}
        style={style}
      >
        <text>
          <textPath
            href={`#${id}`}
            style={{ fontSize: '12px' }}
            startOffset="50%"
            textAnchor="middle"
          >{data.label}
          </textPath>
        </text>
      </path>
    </>
  );
}

export default SimpleFloatingEdge;

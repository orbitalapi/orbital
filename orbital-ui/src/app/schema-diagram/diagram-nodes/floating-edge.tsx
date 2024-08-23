import * as React from 'react';
import { useCallback } from 'react';
import {BaseEdge, useStore, getBezierPath, EdgeLabelRenderer} from '@xyflow/react';

import { getEdgeCoords } from './edge-utils';

// Taken from : https://reactflow.dev/docs/examples/edges/simple-floating-edges/
function SimpleFloatingEdge({ source, target, markerEnd, style, data, sourceHandleId, targetHandleId }) {
  const sourceNode = useStore(useCallback((store) => store.nodeLookup.get(source), [source]));
  const targetNode = useStore(useCallback((store) => store.nodeLookup.get(target), [target]));

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
    <>
      <BaseEdge
        path={edgePath}
        style={style}
        markerEnd={markerEnd}
        interactionWidth={15}
      />
      {/*
        Note: using an EdgeLabelRenderer instead of the label attributes above
              as it allows the labels to animate with the edges correctly
      */}
      <EdgeLabelRenderer>
        <div
          style={{...style,
            position: 'absolute',
            transform: `translate(-50%, -50%) translate(${labelX}px,${labelY}px)`,
            background: '#FFFFFFDD',
            padding: 3,
            fontSize: 10,
            fontWeight: 500,
            borderRadius: 2,
            color: style.stroke,
            display: data.label === undefined ? 'none' : 'block'
          }}
          className="nodrag nopan"
        >
          {data.label}
        </div>
      </EdgeLabelRenderer>
    </>
  );
}

export default SimpleFloatingEdge;

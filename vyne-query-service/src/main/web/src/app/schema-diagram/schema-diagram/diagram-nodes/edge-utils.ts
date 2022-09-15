import { Position, Node } from 'react-flow-renderer';
import { internalsSymbol } from 'react-flow-renderer';
import { HandleIds } from 'src/app/schema-diagram/schema-diagram/schema-chart-builder';

// This whole file taken from : https://reactflow.dev/docs/examples/edges/simple-floating-edges/

// returns the position (top,right,bottom or right) passed node compared to
function getCoords(nodeA: Node, nodeAHandleId: string, nodeB: Node, canFloat: boolean): [number, number, Position] {
  const centerA = getNodeCenter(nodeA);
  const centerB = getNodeCenter(nodeB);

  const horizontalDiff = Math.abs(centerA.x - centerB.x);
  const verticalDiff = Math.abs(centerA.y - centerB.y);

  const position = centerA.x > centerB.x ? Position.Left : Position.Right;

  // when the horizontal difference between the nodes is bigger, we use Position.Left or Position.Right for the handle
  // if (horizontalDiff > verticalDiff) {
  //   position = centerA.x > centerB.x ? Position.Left : Position.Right;
  // } else {
    // here the vertical difference between the nodes is bigger, so we use Position.Top or Position.Bottom for the handle
    // position = centerA.y > centerB.y ? Position.Top : Position.Bottom;
  // }

  const [x, y] = getHandleCoordsByPosition(nodeA, nodeAHandleId, position, canFloat);
  return [x, y, position];
}

function getHandleCoordsByPosition(node: Node, handleId: string, handlePosition: Position.Left | Position.Right, canFloat: boolean) {
  const handleIdWithPosition = canFloat ? HandleIds.appendPositionToHandleId(handleId, handlePosition) : handleId;
  const handle = node[internalsSymbol].handleBounds.source.find(
    (h) => h.id === handleIdWithPosition
  );


  // Default size, in case we haven't yet measured.
  const DEFAULT_SIZE = 8;
  let offsetX = (handle.width || DEFAULT_SIZE) / 2;
  let offsetY = (handle.height || DEFAULT_SIZE) / 2;

  // this is a tiny detail to make the markerEnd of an edge visible.
  // The handle position that gets calculated has the origin top-left, so depending which side we are using, we add a little offset
  // when the handlePosition is Position.Right for example, we need to add an offset as big as the handle itself in order to get the correct position
  switch (handlePosition) {
    case Position.Left:
      offsetX = 0;
      break;
    case Position.Right:
      offsetX = handle.width;
      break;
  }

  const x = node.positionAbsolute.x + handle.x + offsetX;
  const y = node.positionAbsolute.y + handle.y + offsetY;

  return [x, y];
}

function getNodeCenter(node) {
  return {
    x: node.positionAbsolute.x + node.width / 2,
    y: node.positionAbsolute.y + node.height / 2,
  };
}

// returns the parameters (sx, sy, tx, ty, sourcePos, targetPos) you need to create an edge
export function getEdgeCoords(source: Node, sourceHandleId: string, sourceCanFloat: boolean, target: Node, targetHandleId: string, targetCanFloat: boolean) {
  const [sx, sy, sourcePos] = getCoords(source, sourceHandleId, target, sourceCanFloat);
  const [tx, ty, targetPos] = getCoords(target, targetHandleId, source, targetCanFloat);

  return {
    sx,
    sy,
    tx,
    ty,
    sourcePos,
    targetPos,
  };
}

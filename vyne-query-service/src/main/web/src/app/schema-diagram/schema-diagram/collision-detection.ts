import { Box, System, Vector } from 'detect-collisions';
import { Node } from 'reactflow';
import { HORIZONTAL_GAP } from './schema-chart.controller';

export class CollisionDetector {
  private physics: System;
  private boxToNode = new Map<Box, Node>();
  private nodeToBox = new Map<string, Box>();

  constructor(private readonly nodes: Node[], private readonly preferredStationaryNodes: Node[], private readonly direction: 'right' | 'left') {
    this.physics = new System();
    // first, add all the nodes:

    nodes.forEach(node => {
      try {
        const box = new Box(node.position, node.width, node.height);
        this.boxToNode.set(box, node);
        this.nodeToBox.set(node.id, box);
        this.physics.insert(box);
      } catch (e) {
        debugger;
      }
    });
    this.physics.update();
  }

  private findNodeToMove(nodesColliding: Node<any>[]) {
    const candidateNodesToMove = nodesColliding.filter(collidingNode => !this.preferredStationaryNodes.some(stationaryNode => stationaryNode.id == collidingNode.id))
    if (candidateNodesToMove.length === 0) {
      // There were no good choices - take the first one.
      return nodesColliding[0];
    } else {
      return candidateNodesToMove[0];
    }
  }

  private move(node: Node, box: Box, vector: Vector, movedNodes:Node[]) {
    // This is a naieve solution, and will not work in a bunch of cases.
    // Need to consider:
    // - IS the vector supposed to be applied to boxA or boxB?
    // - If the wrong box, how do we adjust to account for the width of the actual box.
    let xDelta = Math.abs(vector.x) + HORIZONTAL_GAP;
    if (this.direction === 'left') {
      xDelta = xDelta * -1; // Perhaps here we + the width of the other box?
    }
    node.position = {
      x: node.position.x + xDelta,
      y: node.position.y - vector.y
    }
    box.setPosition(node.position.x, node.position.y);
    movedNodes.push(node);
  }
  adjustLayout(remainingLoops: number = 100): Node[] {
    const movedNodes: Node[] = [];




    this.physics.checkAll(response => {
      const nodeA = this.boxToNode.get(response.a as Box);
      const nodeB = this.boxToNode.get(response.b as Box);
      if (movedNodes.includes(nodeA) || movedNodes.includes(nodeB)) {
        // Don't move them again.
      } else {
        const nodeToMove = this.findNodeToMove([nodeA, nodeB]);
        if (nodeToMove === nodeA) {
          this.move(nodeA, response.a as Box, response.overlapV, movedNodes);
        } else {
          this.move(nodeB, response.b as Box, response.overlapV, movedNodes);
        }
      }
    });
    if (movedNodes.length > 0 && remainingLoops > 0) {
      this.physics.update();
      return this.adjustLayout(remainingLoops - 1);
    } else {
      return this.nodes;
    }
  }


}

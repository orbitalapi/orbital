import { QueryPlanDiagramData, DiagramLink } from '../services/query.service';

/**
 * Finds all nodes and edges that lead to a specific member (field) in the query plan.
 * Traverses backwards from the target to find all dependencies.
 *
 * Backend link organization (from QueryPlanDiagramBuilder.kt):
 * - Header links: sourceHandleId == sourceId or targetHandleId == targetId
 * - Member links: stored in memberLinks map, contains both inbound AND outbound links for that member
 */
export function findPathsToMember(
  data: QueryPlanDiagramData,
  targetNodeId: string,
  targetHandleId: string
): { nodeIds: Set<string>; edgeIds: Set<string> } {
  const nodeIds = new Set<string>();
  const edgeIds = new Set<string>();
  const visited = new Set<string>();

  // Create a map for quick node lookup
  const nodeMap = new Map(data.nodes.map(node => [node.id, node]));

  // Helper to create a unique key for tracking visited nodes/handles
  const makeKey = (nodeId: string, handleId: string) => `${nodeId}:${handleId}`;

  // Helper to create edge ID matching the format used in buildNodesAndEdges
  const makeEdgeId = (link: DiagramLink) =>
    `${link.sourceId}-${link.sourceHandleId}-${link.targetId}-${link.targetHandleId}`;

  // Recursive function to traverse backwards from target
  function traverse(nodeId: string, handleId: string) {
    const key = makeKey(nodeId, handleId);
    if (visited.has(key)) return;
    visited.add(key);

    // Always include the node itself
    nodeIds.add(nodeId);

    const node = nodeMap.get(nodeId);
    if (!node) return;

    // Collect all inbound links for this node/handle
    const inboundLinks: DiagramLink[] = [];

    // Check if this is a header-level handle (handleId == nodeId)
    const isHeaderHandle = handleId === nodeId;

    if (isHeaderHandle) {
      // Header-level: use inboundHeaderLinks
      inboundLinks.push(...node.inboundHeaderLinks);

      // If this is an operation/service/expression node with no header links, check member parameters
      // Operations and expressions receive their inputs through member parameters, not the header
      if (node.inboundHeaderLinks.length === 0 &&
          (node.kind === 'OPERATION' || node.kind === 'SERVICE' || node.kind === 'EXPRESSION')) {
        // Find all members that are parameters/inputs (have inbound links in their memberLinks)
        for (const member of node.members) {
          const memberLinks = node.memberLinks[member.handleId] || [];
          const memberInboundLinks = memberLinks.filter(
            link => link.targetId === nodeId && link.targetHandleId === member.handleId
          );
          if (memberInboundLinks.length > 0) {
            inboundLinks.push(...memberInboundLinks);
          }
        }
      }
    } else {
      // Member-level: filter memberLinks for this handle where we are the target
      const memberSpecificLinks = node.memberLinks[handleId] || [];

      // memberLinks contains both inbound and outbound for a member
      // Filter to only inbound links (where this node/handle is the target)
      const inbound = memberSpecificLinks.filter(
        link => link.targetId === nodeId && link.targetHandleId === handleId
      );
      inboundLinks.push(...inbound);

      // If this is a member and we found no direct inbound links, it means this member
      // is populated from the node header. Traverse to the node header.
      if (inbound.length === 0) {
        traverse(nodeId, nodeId);
        return;
      }
    }

    // Process each inbound link
    for (const link of inboundLinks) {
      // Add the edge to filtered set
      edgeIds.add(makeEdgeId(link));

      // Recursively traverse the source node/handle
      traverse(link.sourceId, link.sourceHandleId);
    }
  }

  // Start traversal from the target member
  traverse(targetNodeId, targetHandleId);

  return { nodeIds, edgeIds };
}

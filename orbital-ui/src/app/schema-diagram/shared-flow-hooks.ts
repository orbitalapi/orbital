import { useEffect, useCallback } from 'react';
import { toPng } from 'html-to-image';
import { FitViewOptions, Edge, Node } from '@xyflow/react';

/**
 * Shared hook for handling ESC key to exit full screen.
 */
export const useEscapeKey = (onEscape: () => void) => {
  useEffect(() => {
    const handleEsc = (event: KeyboardEvent) => {
      if (event.keyCode === 27) onEscape();
    };
    window.addEventListener('keydown', handleEsc);

    return () => {
      window.removeEventListener('keydown', handleEsc);
    };
  }, [onEscape]);
};

/**
 * Shared fit view options for React Flow diagrams.
 */
export const fitViewOptions: FitViewOptions = {
  padding: 0.75,
  includeHiddenNodes: true,
  duration: 1000
};

/**
 * Downloads the diagram as a PNG image.
 */
export function downloadDiagramImage(fileName: string = 'diagram.png') {
  const reactFlowCanvas = document.querySelector('.react-flow__viewport') as HTMLElement;
  if (reactFlowCanvas) {
    toPng(reactFlowCanvas, {
      filter: (node: HTMLElement) => {
        // Exclude controls and other UI elements from the export
        if (
          node?.classList?.contains('react-flow__minimap') ||
          node?.classList?.contains('react-flow__controls')
        ) {
          return false;
        }
        return true;
      },
    }).then((dataUrl) => {
      const a = document.createElement('a');
      a.setAttribute('download', fileName);
      a.setAttribute('href', dataUrl);
      a.click();
    });
  }
}

/**
 * Shared mouse handlers for highlighting connected nodes and edges on hover.
 * Returns handlers for node enter, edge enter, and leave events.
 */
export function useFlowHoverHandlers(
  store: any, // Return type of useStoreApi()
  setNodes: (nodes: Node[]) => void,
  setEdges: (edges: Edge[]) => void,
  options: { skipOnCondition?: boolean } = {}
) {
  const onNodeMouseEnter = useCallback(
    (_, node: Node) => {
      if (options.skipOnCondition) return;

      const { edges, nodes } = store.getState();
      const id = node.id;
      let hasChange = false;
      const activeEdges: Edge[] = [];

      const mappedEdges = edges.map((edge) => {
        let targetOpacity = 1;
        if (edge.source !== id && edge.target !== id) {
          hasChange = true;
          targetOpacity = 0.2;
        } else {
          activeEdges.push(edge);
        }
        return {
          ...edge,
          style: {
            ...edge.style,
            opacity: targetOpacity,
          },
        };
      });
      if (hasChange) {
        setEdges(mappedEdges);
      }

      if (nodes.length <= 2) return;

      hasChange = false;
      const filteredNodes = nodes.map((node) => {
        let targetOpacity = 1;
        if (
          !activeEdges.filter((edge) => node.id === edge.source || node.id === edge.target).length &&
          node.id !== id
        ) {
          hasChange = true;
          targetOpacity = 0.2;
        }
        return {
          ...node,
          style: {
            ...node.style,
            opacity: targetOpacity,
          },
        };
      });
      if (hasChange) setNodes(filteredNodes);
    },
    [setEdges, setNodes, store, options.skipOnCondition]
  );

  const onEdgeMouseEnter = useCallback(
    (_, edge: Edge) => {
      const { edges, nodes } = store.getState();
      const id = edge.id;
      let hasChange = false;

      const mappedNodes = nodes.map((node) => {
        let targetOpacity = 1;
        if (node.id !== edge.source && node.id !== edge.target) {
          hasChange = true;
          targetOpacity = 0.2;
        }
        return {
          ...node,
          style: {
            ...node.style,
            opacity: targetOpacity,
          },
        };
      });
      if (hasChange) setNodes(mappedNodes);

      hasChange = false;
      const mappedEdges = edges.map((edge) => {
        let targetOpacity = 1;
        if (edge.id !== id) {
          hasChange = true;
          targetOpacity = 0.2;
        }
        return {
          ...edge,
          style: {
            ...edge.style,
            opacity: targetOpacity,
          },
        };
      });
      if (hasChange) setEdges(mappedEdges);
    },
    [setEdges, setNodes, store]
  );

  const onEdgeOrNodeMouseLeave = useCallback(
    (_, edge: Edge | Node) => {
      const { edges, nodes } = store.getState();
      const mappedNodes = nodes.map((node) => {
        return {
          ...node,
          style: {
            ...node.style,
            opacity: 1,
          },
        };
      });
      const mappedEdges = edges.map((edge) => {
        return {
          ...edge,
          style: {
            ...edge.style,
            opacity: 1,
          },
        };
      });
      setNodes(mappedNodes);
      setEdges(mappedEdges);
    },
    [setEdges, setNodes, store]
  );

  return {
    onNodeMouseEnter,
    onEdgeMouseEnter,
    onEdgeOrNodeMouseLeave,
  };
}

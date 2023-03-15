## Flow chart frameworks:

 * React Flow (https://reactflow.dev/) - Would need a react bridge
   * Actively maintained
   * MIT License
   * Needs a React bridge.
   * Nodes can be anything, not limited to SVG
   * Requires a fixed size area to render to.  Could be problematic.
 * Ngx Graph - We use this now, but:
   * limited to layout of SVG components, which limits complexity of things like tables
   * Single connection per node, can't do field-level connections in a table
   * Very few updates, looks like it's in maintanence mode
 * JointJS
    * Powerful.  Low-level API
    * Free and Paid versions

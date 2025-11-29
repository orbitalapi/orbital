function tablerIcons(...names: string[]): Record<string, string> {
  return Object.fromEntries(
    names.map(name => [name, `assets/img/tabler/${name}.svg`])
  );
}

/**
 * Centralized mapping of icon identifiers to their asset paths.
 * This allows consistent icon usage across all diagram node types.
 */
export const NODE_ICON_MAPPING: Record<string, string> = {

  ...tablerIcons(
    "code-variable",
    "arrow-right-to-arc",
    "arrow-left-from-arc",
    "blocks",
    "help-hexagon",
    "switch-horizontal",
    "square-letter-c",
    "file-lambda"),
  // Service types
  "API": "assets/img/chart-icons/api-icon.svg",
  "Database": "assets/img/tabler/database.svg",
  "Kafka": "assets/img/chart-icons/kafka-icon.svg",

  // Common icons
  "request": "assets/img/tabler/arrow-right.svg",
  "response": "assets/img/tabler/arrow-left.svg",
  "model": "assets/img/tabler/box.svg",
  "type": "assets/img/tabler/code.svg",
  "operation": "assets/img/tabler/function.svg",
  "service": "assets/img/chart-icons/api-icon.svg",
  "query": "assets/img/tabler/search.svg",
  "expression": "assets/img/tabler/math-function.svg",
  "constant": "assets/img/tabler/constant.svg"
};

/**
 * Gets the icon path for a given icon identifier.
 * Returns undefined if no mapping exists.
 */
export function getNodeIcon(iconId: string | null | undefined): string | undefined {
  if (!iconId) {
    return undefined;
  }
  return NODE_ICON_MAPPING[iconId];
}

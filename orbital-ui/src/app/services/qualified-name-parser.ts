import {QualifiedName} from "./schema";

/**
 * Parser for converting fully qualified type names into QualifiedName instances
 */
export class QualifiedNameParser {
  /**
   * Parses a fully qualified type name into a QualifiedName instance.
   * Handles nested generic types and multiple type parameters.
   *
   * @param fullyQualifiedName Full type name (e.g., "lang.taxi.Array<lang.taxi.Int>")
   * @returns Parsed QualifiedName instance
   */
  public static parse(fullyQualifiedName: string): QualifiedName {
    // Split the name into base name and parameter string
    const { baseName, paramStr } = QualifiedNameParser.splitNameAndParameters(fullyQualifiedName);
    const parts = baseName.split('.');
    const name = parts[parts.length - 1];
    const namespace = parts.slice(0, parts.length - 1).join('.');

    const qualifiedName = new QualifiedName();
    qualifiedName.name = name;
    qualifiedName.namespace = namespace;
    qualifiedName.fullyQualifiedName = baseName;
    qualifiedName.parameterizedName = fullyQualifiedName;

    // Recursively parse parameters if present
    if (paramStr !== null) {
      const parameters = QualifiedNameParser.parseParameters(paramStr);
      qualifiedName.parameters = parameters.map(param => QualifiedNameParser.parse(param));
    }

    qualifiedName.longDisplayName = fullyQualifiedName;
    qualifiedName.shortDisplayName = this.buildShortDisplayName(name, qualifiedName.parameters);

    return qualifiedName;
  }

  /**
   * Builds the short display name for the type, including parameters.
   * Uses simple names instead of fully qualified names.
   */
  private static buildShortDisplayName(name, parameters): string {
    let result = name;
    if (parameters.length > 0) {
      const paramNames = parameters.map(p => p.shortDisplayName).join(', ');
      result += `<${paramNames}>`;
    }
    return result;
  }

  /**
   * Parses a parameter string into individual type parameters.
   * Handles nested generic types by tracking angle bracket depth.
   */
  private static parseParameters(paramStr: string): string[] {
    if (!paramStr.includes(',')) {
      return [paramStr];
    }

    const params: string[] = [];
    let depth = 0;          // Tracks nesting level of angle brackets
    let start = 0;          // Start index of current parameter

    for (let i = 0; i < paramStr.length; i++) {
      if (paramStr[i] === '<') depth++;
      else if (paramStr[i] === '>') depth--;
      // Only split on commas at top level (depth === 0)
      else if (paramStr[i] === ',' && depth === 0) {
        params.push(paramStr.substring(start, i).trim());
        start = i + 1;
      }
    }

    params.push(paramStr.substring(start).trim());
    return params;
  }

  /**
   * Splits a type name into its base name and parameter string.
   */
  private static splitNameAndParameters(fullName: string): { baseName: string, paramStr: string | null } {
    const paramStart = fullName.indexOf('<');
    if (paramStart === -1) {
      return { baseName: fullName, paramStr: null };
    }

    const baseName = fullName.substring(0, paramStart);
    const paramStr = fullName.substring(paramStart + 1, fullName.lastIndexOf('>'));
    return { baseName, paramStr };
  }
}

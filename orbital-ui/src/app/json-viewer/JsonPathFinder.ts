import {Position} from "../model-designer/taxi-parser.service";
import {isNullOrUndefined} from "../utils/utils";

/**
 * Provides JSONPath expressions for a given location
 * against JSON.
 *
 * The expectation is that JSON is formatted, such that a single key/value pair exists per line.
 */
export class JSONPathFinder {
  private lines: string[];

  // index is a LineNumber
  // the values are a series of jsonPaths
  // Assumption is a single jsonPAth entry per line,
  // which is true for formatted JSON
  private pathsByLineNumber: string[] = [];

  constructor(json: string) {
    this.lines = json.split("\n");
    console.time('buildPathMap');
    this.pathsByLineNumber = this.buildPathMap(json);
    console.timeEnd('buildPathMap')
  }

  /**
   * Returns a list of positions of lines with colons in them,
   * showing the line number and char number.
   *
   * Assumes each line only contains a single ':' (which is true
   * if the JSON has been formatted)
   */
  public getColonMap(): Array<{ position: Position, path: string }> {
    return this.lines
      .map((line, lineIndex) => {
        const column = line.indexOf(':');
        if (column == -1) {
          return null;
        }
        const path = this.getPath(lineIndex, column);
        return {
          position: {line: lineIndex, char: column},
          path
        }
      })
      .filter(v => v != null);
  }

  public getPath(lineNumber: number, column: number): string | undefined {
    return this.pathsByLineNumber[lineNumber];
  }

  /**
   * Returns an array of json paths.
   * Note this is a sparse array - not every index is guaranteed to be populated.
   * Instead, the index is the index of the line number where the jsonPath is present
   */
  private buildPathMap(json: string): string[] {
    let inString = false;
    let key = "";
    let buildingKey = false;
    // The index of the current position, as the number
    // of characters from the start of the string
    let charOffset = 0;

    let currentLine = 0;
    let column = 0;

    const pathBuilder = new PathBuilder();

    const pathMap = new Map<number, string>();
    const pathPositions: string[] = [];

    for (const char of json) {
      switch (char) {
        case "\n":
          currentLine++;
          column = 0;
          key = "";
          break;
        case "{":
          if (!inString) {
            buildingKey = true;
            key = "";
          }
          break;
        case "[":
          if (!inString) {
            pathBuilder.startArray()
            buildingKey = false;
          }
          break;
        case "}":
          if (!inString) {
            pathBuilder.endField();
          }
          break;
        case "]":
          if (!inString) {
            pathBuilder.endArray()
          }
          break;
        case ":":
          if (!inString) {
            buildingKey = false;
          }
          break;
        case ",":
          if (!inString) {
            if (pathBuilder.isInArray) {
              pathBuilder.incrementArrayIndex()
            } else {
              pathBuilder.endField();
              buildingKey = true;
              key = "";
            }
          }
          break;
        case "\"":
          inString = !inString;
          if (!inString && buildingKey) {
            buildingKey = false;
            pathBuilder.startField(key)
            let currentPath = pathBuilder.getCurrentPath();
            pathMap.set(charOffset - key.length, currentPath);
            if (!isNullOrUndefined(pathPositions[currentLine])) {
              console.log('WARNING: Overwriting existing position - this shouldnt happen')
            }
            pathPositions[currentLine] = currentPath;
          }
          break;
        default:
          if (inString && buildingKey) {
            key += char;
          }
      }
      column++;
      charOffset++;
    }

    // TODO : If this turns out to be more correct,
    // then return pathPositions, not pathMap
    return pathPositions;
  }
}

class PathBuilder {
  pathElements: Array<string | number> = ['$'];

  getCurrentPath(): string {
    return this.pathElements
      .map(element => {
        if (typeof element === 'string') {
          return element
        } else {
          return `[${element}]`
        }
      })
      .join('.')
  }

  get isInArray(): boolean {
    return typeof this.currentElement() === 'number';
  }

  endElementOrArrayItem() {
    const currentElement = this.currentElement();
    if (typeof currentElement === 'string') {
      this.endField()
    } else {
      this.incrementArrayIndex();
    }
  }

  currentElement(): string | number {
    return this.pathElements[this.pathElements.length - 1];
  }

  startField(key: string) {
    this.pathElements.push(key)
  }

  startArray() {
    this.pathElements.push(0)
  }

  incrementArrayIndex() {
    if (typeof this.currentElement() !== 'number') {
      throw new Error('Cannot increment array index, as not currently in one.  Current path: ' + this.getCurrentPath());
    }
    const lastIndex = this.pathElements.pop() as number;
    this.pathElements.push(lastIndex + 1);
  }

  endArray() {
    if (typeof this.currentElement() !== 'number') {
      throw new Error('Cannot end array, as not currently in one.  Current path: ' + this.getCurrentPath());
    }
    this.pathElements.pop()

  }

  endField() {
    if (typeof this.currentElement() !== 'string') {
      throw new Error('Cannot end object, as not currently in one.  Current path: ' + this.getCurrentPath());
    }
    this.pathElements.pop()
  }

}

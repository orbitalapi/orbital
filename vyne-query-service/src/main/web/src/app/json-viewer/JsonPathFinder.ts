export class JSONPathFinder {
  private lines: string[];
  private pathMap: Map<number, string>;

  constructor(json: string) {
    this.lines = json.split("\n");
    console.time('buildPathMap');
    this.pathMap = this.buildPathMap(json);
    console.timeEnd('buildPathMap')
  }

  public getPath(lineNumber: number, column: number): string | undefined {
    const charPosition = this.getCharPosition(lineNumber, column);
    const key = this.findClosestKey(charPosition);
    return this.pathMap.get(key);
  }

  private buildPathMap(json: string): Map<number, string> {
    let inString = false;
    let key = "";
    let buildingKey = false;
    let charPosition = 0;

    const pathBuilder = new PathBuilder();

    const pathMap = new Map<number, string>();

    for (const char of json) {
      switch (char) {
        case "{":
          if (!inString) {
            buildingKey = true;
            key = "";
            break;
          }
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
            pathMap.set(charPosition - key.length, pathBuilder.getCurrentPath())
          }
          break;
        default:
          if (inString && buildingKey) {
              key += char;
          }
      }
      charPosition++;
    }

    return pathMap;
  }

  private getCharPosition(lineNumber: number, column: number): number {
    let charPosition = 0;

    for (let i = 0; i < lineNumber - 1; i++) {
      charPosition += this.lines[i].length + 1;
    }

    return charPosition + column;
  }

  private findClosestKey(charPosition: number) {
    let keys = Array.from(this.pathMap.keys());
    for (let i = keys.length - 1; i >= 0; i--) {
      if (keys[i] <= charPosition) {
        return keys[i];
      }
    }
    return -1;
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

  get isInArray():boolean {
    return typeof this.currentElement() === 'number';
  }

  endElementOrArrayItem() {
    const currentElement = this.currentElement();
    if (typeof  currentElement === 'string') {
      this.endField()
    } else {
      this.incrementArrayIndex();
    }
  }

  currentElement():string | number {
    return this.pathElements[this.pathElements.length - 1];
  }
  startField(key:string) {
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

import {QualifiedName} from "../../services/schema";
import {isNullOrUndefined} from "util";

function hasVerbClause(lines: string[]) {
  return lines.some(line => line.trim().startsWith('find {') || line.trim().startsWith('stream {'))
}

function prependImport(lines: string[], typeToAdd: QualifiedName): string[] {
  const lastImportLine = lines.findIndex(line => !line.trim().startsWith('import'))
  lines.splice(lastImportLine, 0, `import ${typeToAdd.fullyQualifiedName}`);
  return lines;
}

function appendFindClause(lines: string[], typeToAdd: QualifiedName): string[] {
  lines.push(`find { ${typeToAdd.shortDisplayName} }`);
  return lines;
}

function removeNewLineAndWhitespace(lines: string[]): string {
  return lines.map(line => line.replace(/\s/g, ""))
    .join('')
}

function hasProjection(lines: string[]): boolean {
  const content = removeNewLineAndWhitespace(lines);
  return content.includes('}as{');
}

function indent(s: string, number: number) {
  const prefix = ' '.repeat(number);
  return `${prefix}${s}`;
}

function lowerFirstLetter(s: string): string {
  return s.charAt(0).toLowerCase() + s.slice(1);
}

function typeAsField(typeToAdd: QualifiedName): string {
  return `${lowerFirstLetter(typeToAdd.name)}: ${typeToAdd.shortDisplayName}`;
}

function appendProjection(lines: string[], typeToAdd: QualifiedName, projectAsArray: boolean): string[] {
  const closingTag = projectAsArray ? '}[]' : '}';
  lines.push('as {', indent(typeAsField(typeToAdd), 3), closingTag)
  return lines;
}

function appendTypeToExistingProjection(lines: string[], typeToAdd: QualifiedName): string[] {

  const projectionClose = lines.lastIndexOf('}');
  lines.splice(projectionClose, 0, indent(typeAsField(typeToAdd), 3));
  return lines;
}

function isQueryingArray(lines: string[]): boolean {
  const findLine = lines.find(s => s.trim().startsWith("find {"))
  if (isNullOrUndefined(findLine)) {
    return false
  } else {
    return findLine.includes('[]');
  }
}

export function appendToQuery(existingQuery: string, typeToAdd: QualifiedName): string {
  let lines = existingQuery.split('\n');
  lines = prependImport(lines, typeToAdd);

  if (hasVerbClause(lines)) {
    const projectionExists = hasProjection(lines);
    if (!projectionExists) {
      lines = appendProjection(lines, typeToAdd, isQueryingArray(lines));
    } else {
      lines = appendTypeToExistingProjection(lines, typeToAdd);
    }

  } else {
    lines = appendFindClause(lines, typeToAdd);
  }
  return lines.join("\n");
}


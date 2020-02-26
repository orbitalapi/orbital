export class QualifiedName {
  name: string;
  namespace: string;
  fullyQualifiedName: string;

  static nameOnly(fullyQualifiedName: string): string {
    const parts = fullyQualifiedName.split('.');
    return parts[parts.length - 1];
  }

  static from(fullyQualifiedName: string): QualifiedName {
    const parts = fullyQualifiedName.split('.');
    const name = QualifiedName.nameOnly(fullyQualifiedName);
    const namespace = parts.slice(0, parts.length - 1).join('.');
    const qualifiedName = new QualifiedName();
    qualifiedName.fullyQualifiedName = fullyQualifiedName;
    qualifiedName.namespace = namespace;
    qualifiedName.name = name;
    return qualifiedName;
  }
}

export interface FieldMap {
  [key: string]: Field;
}


export interface Type {
  name: QualifiedName;
  attributes: FieldMap;
  modifiers: Array<Modifier>;
  scalar: boolean;
  aliasForType: QualifiedName;
  enumValues: Array<string>;
  sources: Array<SourceCode>;
  closed: boolean;
  typeDoc?: string;
}

export interface Field {
  type: TypeReference;
  modifiers: Array<Modifier>;
}


export interface SchemaSpec {
  name: string;
  version: string;
  defaultNamespace: string;
}


export interface TypeReference {
  name: QualifiedName;
  isCollection: Boolean;
  constraints: Array<any>;
  fullyQualifiedName: string;
}

export enum Modifier {
  PARAMETER_TYPE = 'PARAMETER_TYPE',
  ENUM = 'ENUM',
  CLOSED = 'CLOSED',
  PRIMITIVE = 'PRIMITIVE'
}

export enum FieldModifier {
  CLOSED = 'CLOSED'
}


export interface SourceCode {
  origin: string;
  language: string;
  content: string;
}

export interface Schema {
  types: Array<Type>;
  services: Array<Service>;
  operations: Array<Operation>;
  // TODO : Are these still required / meaningful?
  // attributes: Set<QualifiedName>
}

export interface Parameter {
  type: Type;
  name: string;
  metadata: Array<Metadata>;
  constraints: Array<any>;
}

export interface Metadata {
  name: QualifiedName;
  params: Map<string, any>;
}


export interface Operation {
  name: string;
  parameters: Array<Parameter>;
  returnType: Type;
  metadata: Array<Metadata>;
  contract: OperationContract;
  sources: SourceCode[];
  typeDoc?: string;
}

export interface Service {
  name: QualifiedName;
  operations: Array<Operation>;
  metadata: Array<Metadata>;
  sourceCode: SourceCode;
  typeDoc?: string;
}

export function isService(candidate): candidate is Service {
  return (candidate as Service).operations !== undefined;
}

export function isType(candidate): candidate is Type {
  return (candidate as Type).scalar !== undefined;
}

export function isOperation(candidate): candidate is Operation {
  return (candidate as Operation).returnType !== undefined;
}

export interface OperationContract {
  returnType: Type;
  constraints: Array<any>;
}


export interface SchemaGraphNode {
  id: string;
  label: string;
  type: string; // Consider adding the enum ElementType here
  nodeId: string;
}

export interface SchemaGraphLink {
  source: string;
  target: string;
  label: string;
}

function isMap(value: any): value is Map<any, any> {
  return value.keys !== undefined && value.get !== undefined;
}

export class SchemaGraph {
  static empty(): SchemaGraph {
    return new SchemaGraph(new Map(), new Map());
  }

  static from(nodes, links): SchemaGraph {
    const graph = this.empty();
    graph.mergeToMap(nodes, graph.nodes);
    graph.mergeToMap(links, graph.links);
    return graph;
  }

  constructor(
    public readonly nodes: Map<string, SchemaGraphNode>,
    public readonly links: Map<number, SchemaGraphLink>
  ) {
  }

  add(other: SchemaGraph) {
    this.mergeToMap(other.nodes, this.nodes);
    this.mergeToMap(other.links, this.links);
  }

  toNodeSet(): SchemaNodeSet {
    return {
      nodes: Array.from(this.nodes.values()),
      links: Array.from(this.links.values())
    };
  }


  mergeToMap(source: Map<any, any>, target) {
    function setOnTarget(key, value) {
      if (isMap(target)) {
        if (!target.has(key)) {
          target.set(key, value);
        }
      } else {
        if (!target[key]) {
          target[key] = value;
        }
      }
    }

    if (isMap(source)) {
      source.forEach((value, key) => {
        setOnTarget(key, value);
      });
    } else {
      Object.keys(source).forEach(key => {
        setOnTarget(key, source[key]);
      });
    }

  }

}


export interface SchemaNodeSet {
  nodes: SchemaGraphNode[];
  links: SchemaGraphLink[];
}


export class SchemaMember {
  constructor(
    public readonly name: QualifiedName,
    public readonly kind: SchemaMemberType,
    public readonly aliasForType: string,
    public readonly member: Type | Service | Operation,
    public readonly sources: SourceCode[]
  ) {
    this.attributeNames = kind === SchemaMemberType.TYPE
      ? Object.keys((member as Type).attributes)
      : [];
  }

  attributeNames: string[];

  static fromService(service: Service): SchemaMember[] {
    return service.operations.map(operation => {
      return this.fromOperation(operation, service);
    });

  }

  private static fromOperation(operation: Operation, service: Service) {
    return new SchemaMember(
      {
        name: operation.name,
        fullyQualifiedName: service.name.fullyQualifiedName + ' #' + operation.name,
        namespace: service.name.namespace
      },
      SchemaMemberType.OPERATION,
      null,
      operation,
      operation.sources
    );
  }

  static fromType(type: Type): SchemaMember {
    return new SchemaMember(
      type.name,
      SchemaMemberType.TYPE,
      (type.aliasForType) ? type.aliasForType.fullyQualifiedName : null,
      type,
      type.sources
    );
  }

  static fromSchema(schema: Schema): SchemaMember[] {
    const result = schema.types.map(t => this.fromType(t));
    schema.services.forEach(s => result.concat(this.fromService(s)));

    result.sort((a, b) => {
      if (a.name.name.toLowerCase() < b.name.name.toLowerCase()) {
        return -1;
      } else if (a.name.name.toLowerCase() > b.name.name.toLowerCase()) {
        return 1;
      } else {
        return 0;
      }
    });
    return result;
  }
}

export enum SchemaMemberType {
  SERVICE = 'SERVICE',
  TYPE = 'TYPE',
  OPERATION = 'OPERATION'
}

export interface TypedInstance {
  type: Type;
  value: any;
}

export interface VersionedSchema {
  name: string;
  version: string;
  content: string;
}


export interface Message {
  message: string;
  level: Level;
  link?: string;
}

export enum Level {
  INFO = 'INFO',
  WARN = 'WARN',
  ERROR = 'ERROR'
}


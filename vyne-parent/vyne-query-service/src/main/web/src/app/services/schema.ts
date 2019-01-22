export class QualifiedName {
  name: string;
  namespace: string;
  fullyQualifiedName: string;

  static nameOnly(fullyQualifiedName: string): string {
    let parts = fullyQualifiedName.split(".");
    return parts[parts.length - 1];
  }

  static from(fullyQualifiedName: string): QualifiedName {
    const parts = fullyQualifiedName.split(".");
    const name = QualifiedName.nameOnly(fullyQualifiedName);
    const namespace = parts.splice(parts.length - 1, 1).join(".");
    const qualifiedName = new QualifiedName();
    qualifiedName.fullyQualifiedName = fullyQualifiedName;
    qualifiedName.namespace = namespace;
    qualifiedName.name = name;
    return qualifiedName
  }
}


export interface Type {
  name: QualifiedName
  attributes: Map<string, TypeReference>
  modifiers: Array<Modifier>
  scalar: boolean
  aliasForType: QualifiedName,
  enumValues: Array<string>,
  sources: Array<SourceCode>
}


export interface SchemaSpec {
  name: string;
  version: string;
  defaultNamespace: string
}



export interface TypeReference {
  name: QualifiedName
  isCollection: Boolean
  constraints: Array<any>
  fullyQualifiedName: string
}

export enum Modifier {
  PARAMETER_TYPE = "PARAMETER_TYPE",
  ENUM = "ENUM",
  PRIMITIVE = "PRIMITIVE"
}


export interface SourceCode {
  origin: string
  language: string
  content: string
}

export interface Schema {
  types: Array<Type>
  services: Array<Service>
  operations: Array<Operation>
  // TODO : Are these still required / meaningful?
  // attributes: Set<QualifiedName>
}

export interface Parameter {
  type: Type
  name: string
  metadata: Array<Metadata>
  constraints: Array<any>
}

export interface Metadata {
  name: QualifiedName
  params: Map<string, any>
}


export interface Operation {
  name: string
  parameters: Array<Parameter>
  returnType: Type
  metadata: Array<Metadata>
  contract: OperationContract
  sources: SourceCode[]
}

export interface Service {
  name: QualifiedName
  operations: Array<Operation>
  metadata: Array<Metadata>,
  sourceCode: SourceCode
}

export interface OperationContract {
  returnType: Type
  constraints: Array<any>
}


export interface SchemaGraphNode {
  id: string
  label: string
  type: string // Consider adding the enum ElementType here
  nodeId: string
}

export interface SchemaGraphLink {
  source: string
  target: string
  label: string
}

export class SchemaGraph {

  constructor(
    public readonly nodes: Map<string, SchemaGraphNode>,
    public readonly links: Map<number, SchemaGraphLink>
  ) {
  }

  add(other: SchemaGraph) {
    this.mergeToMap(other.nodes, this.nodes);
    this.mergeToMap(other.links, this.links)
  }

  toNodeSet(): SchemaNodeSet {
    return {
      nodes: Array.from(this.nodes.values()),
      links: Array.from(this.links.values())
    }
  }


  mergeToMap(source: Map<any, any>, target) {
    Object.keys(source).forEach(key => {
      if (!target[key]) {
        target.set(key, source[key])
      }
    })
  }

  static empty(): SchemaGraph {
    return new SchemaGraph(new Map(), new Map())
  }
}


export interface SchemaNodeSet {
  nodes: SchemaGraphNode[]
  links: SchemaGraphLink[]
}


export class SchemaMember {
  constructor(
    public readonly name: QualifiedName,
    public readonly kind: SchemaMemberType,
    public readonly aliasForType: string,
    public readonly member: Type | Service | Operation,
    public readonly sources: SourceCode[]
  ) {
    this.attributeNames = kind == SchemaMemberType.TYPE
      ? Object.keys((member as Type).attributes)
      : []
  }

  attributeNames: string[];

  static fromService(service: Service): SchemaMember[] {
    return service.operations.map(operation => {
      return new SchemaMember(
        {
          name: operation.name,
          fullyQualifiedName: service.name.fullyQualifiedName + " #" + operation.name,
          namespace: service.name.namespace
        },
        SchemaMemberType.OPERATION,
        null,
        operation,
        operation.sources
      )
    })

  }

  static fromType(type: Type): SchemaMember {
    return new SchemaMember(
      type.name,
      SchemaMemberType.TYPE,
      (type.aliasForType) ? type.aliasForType.fullyQualifiedName : null,
      type,
      type.sources
    )
  }

  static fromSchema(schema: Schema): SchemaMember[] {
    let result = schema.types.map(t => this.fromType(t));
    schema.services.forEach(s => result.concat(this.fromService(s)));

    result.sort((a, b) => {
      if (a.name.name.toLowerCase() < b.name.name.toLowerCase()) {
        return -1
      } else if (a.name.name.toLowerCase() > b.name.name.toLowerCase()) {
        return 1
      } else {
        return 0
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
  INFO = "INFO",
  WARN = "WARN",
  ERROR = "ERROR"
}


import {PrimitiveTypeNames} from './taxi';
import {isNullOrUndefined} from 'util';

export class QualifiedName {
  name: string;
  namespace: string;
  fullyQualifiedName: string;
  parameterizedName: string;
  parameters: QualifiedName[] = [];
  longDisplayName: string;
  shortDisplayName: string;

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

export interface Documented {
  typeDoc: string | null;
}

export interface Named {
  name: QualifiedName;
}

export interface NamedAndDocumented extends Documented, Named {
}

export interface Type extends Documented, Named {
  name: QualifiedName;
  attributes: FieldMap;
  collectionType: Type | null;
  modifiers: Array<Modifier>;
  isScalar: boolean;
  format: string;
  hasFormat: boolean;
  aliasForType: QualifiedName;
  basePrimitiveTypeName: QualifiedName;
  enumValues: Array<EnumValues>;
  sources: Array<VersionedSource>;
  isClosed: boolean;
  isCollection: boolean;
  isParameterType: boolean;
  typeParameters: QualifiedName[];
  inheritsFrom: QualifiedName[];
}

export interface EnumValues {
  name: string;
  value: any;
  synonyms: Array<string>;
  typeDoc: string | null;
}

export interface Field extends Documented {
  type: QualifiedName;
  modifiers: Array<Modifier>;
  defaultValue?: any;
  nullable?: boolean;
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


// Deprecate, favour VersionedSource and ParsedSource
export interface SourceCode {
  origin: string;
  language: string;
  content: string;
  version?: string;
}

export interface SourceCompilationError {
  detailMessage: string;
  sourceName: string;
  line: number;
  char: number;
}

export interface ParsedSource {
  source: VersionedSource;
  errors: SourceCompilationError[];
  isValid: boolean;
}

function buildArrayType(schema: TypeCollection, typeName: string, anonymousTypes: Type[] = []): Type {
  if (schema.constructedArrayTypes === undefined) {
    schema.constructedArrayTypes = {};
  }
  // This caching is important, as if we call this method multiple times,
  // and create new (but equivalent) type instances, then equality checks
  // fails, which cause setters and bindings to go nuts
  if (schema.constructedArrayTypes[typeName]) {
    return schema.constructedArrayTypes[typeName];
  }

  const innerType = typeName.replace('lang.taxi.Array<', '')
    .replace('>', '');
  const arrayType = schema.types.find(t => t.name.parameterizedName === 'lang.taxi.Array');
  const name = QualifiedName.from(arrayType.name.fullyQualifiedName);
  name.parameterizedName = typeName;
  const paramType = findType(schema, innerType, anonymousTypes);
  name.parameters = [paramType.name];
  name.shortDisplayName = paramType.name.shortDisplayName + '[]';
  name.longDisplayName = paramType.name.longDisplayName + '[]';
  const result = {
    ...arrayType,
    name,
    collectionType: paramType,
    isParameterType: true,
    isCollection: true,
    typeParameters: name.parameters
  } as Type;

  schema.constructedArrayTypes[typeName] = result;
  return result;
}

export function findType(schema: TypeCollection, typeName: string, anonymousTypes: Type[] = []): Type {
  if (schema.anonymousTypes === undefined) {
    schema.anonymousTypes = {};
  }
  if (typeName === 'lang.taxi.Array') {
    console.warn('A search was performed for a raw array.  Favour parameterizedName over QualifiedName to avoid this');
  }
  // TODO : Actual support for generics
  if (typeName.startsWith('lang.taxi.Array<')) {
    return buildArrayType(schema, typeName, anonymousTypes);
  }
  let name = schema.types.find(t => t.name.parameterizedName === typeName);
  if (!name) {
    name = schema.anonymousTypes[typeName];
    if (!name) {
      name = anonymousTypes.find(anonymousType => anonymousType.name.fullyQualifiedName === typeName);
      if (!name) {
        throw new Error(`No type name ${typeName} was found`);
      } else {
        schema.anonymousTypes[typeName] = name;
      }
    }
  }
  return name;
}

export interface TypeCollection {
  types: Array<Type>;
  constructedArrayTypes?: { [key: string]: Type };
  anonymousTypes?: { [key: string]: Type };
}

export interface Schema extends TypeCollection {

  types: Array<Type>;
  services: Array<Service>;
  operations: Array<Operation>;
  // TODO : Are these still required / meaningful?
  // attributes: Set<QualifiedName>
}

export interface Parameter {
  type: QualifiedName;
  name: string;
  metadata: Array<Metadata>;
  constraints: Array<any>;
}

export interface Metadata {
  name: QualifiedName;
  params: { [index: string]: any };
}


export interface Operation extends SchemaMemberNamed {
  name: string;
  qualifiedName: QualifiedName;
  parameters: Array<Parameter>;
  returnType: QualifiedName;
  metadata: Array<Metadata>;
  contract: OperationContract;
  // sources: VersionedSource[];
  typeDoc?: string;
  operationType: string | null;
}

export interface Service extends SchemaMemberNamed, Named, Documented {
  qualifiedName: string; // This is messy, and needs fixing up.
  operations: Operation[];
  queryOperations: QueryOperation[];
  metadata: Metadata[];
  // Source not currently loaded for services, will load async
  // sourceCode: VersionedSource;
}

export interface QueryOperation {
  name: QualifiedName;
  parameters: Parameter[];
  returnType: QualifiedName;
  metadata: Metadata[];
  grammar: string;
  capabilities: any[];
  typeDoc?: string;
}

// Matches SchemaMember.kt, but we already have a class called SchemaMember
export interface SchemaMemberNamed {
  memberQualifiedName: QualifiedName;
}

export interface Pipeline {
  inputChannel: any; // TODO
  inputType: Type;
  persistRawInput: boolean;

  outputChannel: any; // TODO
  outputType: Type;
}

export function isService(candidate): candidate is Service {
  return (candidate as Service).operations !== undefined;
}

export function isType(candidate): candidate is Type {
  return (candidate as Type).isScalar !== undefined;
}

export function isOperation(candidate): candidate is Operation {
  return (candidate as Operation).returnType !== undefined;
}

export function isMappedSynonym(candidate): candidate is MappedSynonym {
  const mappedValueCandidate = candidate as MappedValue;
  return mappedValueCandidate.dataSourceName === 'Mapped' && mappedValueCandidate.mappingType === MappingType.SYNONYM;
}

export interface OperationContract {
  returnType: QualifiedName;
  constraints: Array<any>;
}


export interface SchemaGraphNode {
  id: string;
  label: string;
  type: string; // Consider adding the enum ElementType here
  nodeId: string;
  subHeader?: string | null;
  value?: any | null;
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
    public readonly sources: VersionedSource[]
  ) {
    try {
      this.attributeNames = kind === SchemaMemberType.TYPE
        ? Object.keys((member as Type).attributes)
        : [];
    } catch (error) {
      console.error(error);
    }


  }

  attributeNames: string[];

  static fromService(service: Service): SchemaMember[] {
    return service.operations.map(operation => {
      return this.fromOperation(operation, service);
    });

  }

  private static fromOperation(operation: Operation, service: Service) {
    const qualifiedName = service.name.fullyQualifiedName + ' #' + operation.name;
    return new SchemaMember(
      {
        name: operation.name,
        fullyQualifiedName: qualifiedName,
        namespace: service.name.namespace,
        parameters: [],
        parameterizedName: qualifiedName,
        shortDisplayName: operation.name,
        longDisplayName: qualifiedName
      },
      SchemaMemberType.OPERATION,
      null,
      operation,
      [] // sources not currently returned for operations. Will load these async in the future
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

export interface VersionedSource {
  name: string;
  version: string;
  content: string;
  id?: string;
  contentHash?: string;
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


export function getCollectionMemberType(type: Type, schema: Schema, defaultIfUnknown: Type | String = type): Type {
  function resolveDefaultType(): Type {
    return (typeof defaultIfUnknown === 'string') ? findType(schema, defaultIfUnknown) : defaultIfUnknown as Type;
  }

  if (type.name.fullyQualifiedName === PrimitiveTypeNames.ARRAY) {
    return collectionMemberTypeFromArray(type.name, schema, resolveDefaultType);
  } else if (type.aliasForType && type.aliasForType.fullyQualifiedName === PrimitiveTypeNames.ARRAY) {
    return collectionMemberTypeFromArray(type.aliasForType, schema, resolveDefaultType);
  } else {
    // console.warn('Cannot determine collection type from a Non-Array type.  Returning default value');
    return resolveDefaultType();
  }
}

function collectionMemberTypeFromArray(name: QualifiedName, schema: Schema, defaultValue: () => Type): Type {
  if (name.parameters.length === 1) {
    return findType(schema, name.parameters[0].fullyQualifiedName);
  } else {
    console.warn('Received a raw Array type (without parameters.  This is discouraged.  Returning default type');
    return defaultValue();
  }
}

export interface UntypedInstance {
  value: any;
  type: UnknownType;
  nearestType: Type | null;
}

export function asNearestTypedInstance(untypedInstance: UntypedInstance): TypedInstance {
  if (untypedInstance.nearestType === null) {
    throw new Error('NearestType must be populated in order to cast to TypedInstance');
  }
  return {
    value: untypedInstance.value,
    type: untypedInstance.nearestType
  } as TypedInstance;
}

export enum UnknownType {
  UnknownType = 'UnknownType'
}

export type InstanceLike = TypedInstance | TypedObjectAttributes | TypeNamedInstance;
export type InstanceLikeOrCollection = InstanceLike | InstanceLike[];
export type TypeInstanceOrAttributeSet = TypedInstance | TypedObjectAttributes;

export interface TypedObjectAttributes {
  [key: string]: TypeInstanceOrAttributeSet;
}

export function getTypeName(instance: InstanceLike): string {
  if (isTypedInstance(instance)) {
    return instance.type.name.fullyQualifiedName;
  } else if (isTypeNamedInstance(instance)) {
    return instance.typeName;
  } else {
    // No good reason for not supporting this, just haven't hit the usecase yet, and it's not
    // obvious how we should support it.
    throw new Error('Looks like the instance is a TypedObjectAttributes, which isn\'t yet supported');
  }
}

export function isUntypedInstance(instance: any): instance is UntypedInstance {
  return !isNullOrUndefined(instance.type) && instance.type === UnknownType.UnknownType;
}

export function isTypedInstance(instance: any): instance is TypedInstance {
  return instance && instance.type !== undefined && instance.value !== undefined;
}

export function isTypedNull(instance: InstanceLikeOrCollection): instance is TypedInstance {
  const instanceAny = instance as any;
  return instanceAny && instanceAny.type !== undefined && isNullOrUndefined(instanceAny.value);
}

export function isTypeNamedInstance(instance: any): instance is TypeNamedInstance {
  const instanceAny = instance as any;
  return instanceAny && instanceAny.typeName !== undefined && instanceAny.value !== undefined;
}

export function isTypeNamedNull(instance: any): instance is TypeNamedInstance {
  const instanceAny = instance as any;
  return instanceAny && instanceAny.typeName !== undefined && isNullOrUndefined(instanceAny.value);
}

export function isTypedCollection(instance: any): instance is TypeNamedInstance[] {
  return instance && Array.isArray(instance) && instance[0] && isTypeNamedInstance(instance[0]);
}


export interface TypeNamedInstance {
  typeName: string;
  value: any;
  source?: DataSource;
}


export interface DataSourceReference {
  dataSourceIndex: number;
}

export interface DataSource {
  dataSourceName: DataSourceType;
}

export enum MappingType {
  SYNONYM = 'SYNONYM'
}

export interface MappedValue extends DataSource {
  mappingType: MappingType;
  dataSourceName: 'Mapped';
}

export interface MappedSynonym extends MappedValue {
  source: TypeNamedInstance;
  mappingType: MappingType.SYNONYM;
}

export type DataSourceType =
  'Provided'
  | 'Mapped'
  | 'Operation result'
  | 'Defined in schema'
  | 'Undefined source'
  | 'Multiple sources';

// import {ElseCondition, Instruction, InstructionType, Policy, PolicyStatement, RuleSet} from "./policies";

import {QualifiedName, Type} from "../services/types.service";

interface SourceElement {
  src(): string

  imports(): QualifiedName[]
}

interface PlainTextElement {
  description(): string
}

export class Policy implements SourceElement {
  constructor(
    public name: QualifiedName,
    public targetTypeName: QualifiedName,
    public ruleSets: RuleSet[] = []
  ) {
  }

  static createNew(targetType: Type): Policy {
    let qualifiedName = targetType.name.namespace + "." + "NewPolicy";

    return new Policy(QualifiedName.from(qualifiedName), targetType.name, [
      new RuleSet([
        new PolicyStatement(new ElseCondition(), new Instruction(InstructionType.PERMIT))
      ])
    ])
  }

  src(): string {
    const importSrc = this.imports().map(imp => `import ${imp.fullyQualifiedName}`).join("\n");

    return `
${importSrc}

namespace ${this.name.namespace}
    
policy ${this.name.name} against ${this.targetTypeName.fullyQualifiedName} {
${this.ruleSets.map(r => r.src()).join("\n")}
}`;
  }

  imports(): QualifiedName[] {
    let imports = [this.targetTypeName];
    imports = imports.concat(this.ruleSets.flatMap(r => r.imports()));
    return imports
  }
}


export class RuleSetUtils {
  static elsePrefixWord(ruleSet: RuleSet): string {
    if (!ruleSet) return "";
    return (ruleSet.statements.length > 1) ? "Otherwise, " : "Always ";
  }
}

export class RuleSet implements SourceElement {
  constructor(public statements: PolicyStatement[] = []) {
  }

  appendStatement(policyStatement: PolicyStatement) {
    this.statements.splice(this.statements.length - 1, 0, policyStatement)
  }

  src(): string {
    // TODO - scopes etc
    const statementSrc = this.statements.map(s => `    ${s.src()}`).join("\n");
    return `  read {
${statementSrc}
  }`;
  }

  imports(): QualifiedName[] {
    return this.statements.flatMap(s => s.imports())
  }

  removeStatement(statement: PolicyStatement) {
    const idx = this.statements.indexOf(statement);
    this.statements.splice(idx, 1);
  }
}

export class PolicyStatement implements SourceElement {
  constructor(public condition: Condition,
              public instruction: Instruction,
              public editing: boolean = false) {
  }


  src(): string {
    return `${this.condition.src()} -> ${this.instruction.src()}`;
  }

  imports(): QualifiedName[] {
    return this.condition.imports().concat(this.instruction.imports())
  }

}

export class Operator {
  constructor(readonly symbol: string, readonly label: string) {
  }

  static EQUALS = new Operator("=", "equals");
  static NOT_EQUAL = new Operator("!=", "does not equal");
  static IN = new Operator("in", "is in");

  static operators: Operator[] = [
    Operator.EQUALS,
    Operator.NOT_EQUAL,
    Operator.IN
  ];

  static EQUALS_PROPERTY: DisplayOperator = {
    operator: Operator.EQUALS,
    label: 'equals property',
    literalOrProperty: 'property',
    matches: isOperatorType(Operator.EQUALS, 'RelativeSubject')
  };
  static displayOperators: DisplayOperator[] = [
    Operator.EQUALS_PROPERTY,
    {
      operator: Operator.EQUALS,
      label: 'equals value',
      literalOrProperty: 'literal',
      matches: isOperatorType(Operator.EQUALS, 'LiteralSubject')
    },
    {
      operator: Operator.NOT_EQUAL,
      label: 'does not equal property',
      literalOrProperty: 'property',
      matches: isOperatorType(Operator.NOT_EQUAL, 'RelativeSubject')
    },
    {
      operator: Operator.NOT_EQUAL,
      label: 'does not equal value',
      literalOrProperty: 'literal',
      matches: isOperatorType(Operator.NOT_EQUAL, 'LiteralSubject')
    },
    {operator: Operator.IN, label: 'is in', matches: isOperatorType(Operator.IN, 'LiteralArraySubject')}
  ];
}


export interface Condition extends SourceElement {
  readonly text: string
}

export class ElseCondition implements Condition {
  src(): string {
    return "else"
  }

  readonly text: string = "else";

  description(): string {
    return "Otherwise,";
  }

  imports(): QualifiedName[] {
    return []
  }
}

export class CaseCondition implements Condition, PlainTextElement {
  readonly text: string = "case";
  lhSubject: Subject;
  rhSubject: Subject;
  operator: Operator = Operator.EQUALS;

  // Operators are tricky, as equals property vs equals value isn't knowable
  // until after the rhSubject is set.
  // DisplayOperator is set when editing in the UI, and derivable when
  // receiving from the server
  private _displayOperator: DisplayOperator;

  get displayOperator(): DisplayOperator {
    if (this._displayOperator) return this._displayOperator;

    const derived = this.deriveDisplayOperator();
    return (derived) ? derived : Operator.EQUALS_PROPERTY;
  }

  set displayOperator(value: DisplayOperator) {
    this._displayOperator = value;
  }

  private deriveDisplayOperator(): DisplayOperator {
    return Operator.displayOperators.find(o => o.matches(this))
  }

  imports(): QualifiedName[] {
    if (!this.lhSubject || !this.rhSubject) return [];
    return this.lhSubject.imports().concat(this.rhSubject.imports())
  }

  src(): string {
    if (!this.lhSubject || !this.rhSubject) return "";
    return `case ${this.lhSubject.src()} ${this.operator.symbol} ${this.rhSubject.src()}`;
  }

  description(): string {
    if (!this.lhSubject || !this.rhSubject) return null;
    return `${this.lhSubject.description()} ${this.operator.label} ${this.rhSubject.description()}`;
  }
}

export type  SubjectType = 'RelativeSubject' | 'LiteralSubject' | 'LiteralArraySubject';

export interface Subject extends SourceElement, PlainTextElement {
  readonly type: SubjectType
}

export class RelativeSubject implements Subject {
  readonly type = "RelativeSubject";

  constructor(
    public source: RelativeSubjectSource,
    public targetTypeName: QualifiedName,
    public propertyName?: string
  ) {
  }

  src(): string {
    return `${this.source.toString().toLowerCase()}.${this.targetTypeName.fullyQualifiedName}`;
  }

  imports(): QualifiedName[] {
    return [this.targetTypeName]
  }

  description(): string {
    return this.targetTypeName.name
  }
}

export enum RelativeSubjectSource {
  CALLER = "CALLER",
  THIS = "THIS"
}

export class LiteralArraySubject implements Subject {
  readonly type = "LiteralArraySubject";

  constructor(public values: any[]) {
  }

  src(): string {
    return `[ ${this.values.map(v => `"${v}"`).join(",")} ]`;
  }

  imports(): QualifiedName[] {
    return []
  }


  description(): string {
    return this.src();
  }
}

export class LiteralSubject implements Subject {
  readonly type = "LiteralSubject";

  constructor(public value: any) {
  }

  src(): string {
    return `"${this.value.toString()}"`;
  }

  imports(): QualifiedName[] {
    return []
  }

  description(): string {
    return this.src();
  }
}

export class Instruction implements SourceElement, PlainTextElement {
  constructor(
    public type: InstructionType,
    public processor?: InstructionProcessor) {
  }

  static permit(): Instruction {
    return new Instruction(InstructionType.PERMIT)
  }

  src(): string {
    const processorString = (this.processor) ? this.processor.src() : "";
    return `${this.type.toString().toLowerCase()}${processorString}`;
  }

  imports(): QualifiedName[] {
    return []
  }

  description(): string {
    const processorString = (this.processor) ? this.processor.description() : "";
    return `${this.type.toString().toLowerCase()}${processorString}`
  }
}

export class InstructionProcessor
  implements SourceElement, PlainTextElement {
  name: string;
  args: any[];

  src(): string {
    return ` using ${this.name}${this.argsString}`;
  }

  imports(): QualifiedName[] {
    return []
  }

  private get argsString(): string {
    return (this.args) ? `( ${this.args.join(",")} )` : "";
  }

  description(): string {
    const nameParts = this.name.split(".");
    const shortName = nameParts[nameParts.length - 1];
    return ` using ${shortName}${this.argsString}`;
  }
}

export enum InstructionType {
  PERMIT = "PERMIT",
  PROCESS = "PROCESS",
  FILTER = "FILTER"
}


function isOperatorType(operator: Operator, subjectType: SubjectType) {
  return function (caseCondition: CaseCondition) {
    if (!caseCondition || !caseCondition.rhSubject) return false;
    return caseCondition.rhSubject.type == subjectType && caseCondition.operator == operator;
  }
}

export interface DisplayOperator {
  operator: Operator;
  label: string;
  literalOrProperty?: 'literal' | 'property';
  matches: (CaseCondition) => boolean
}



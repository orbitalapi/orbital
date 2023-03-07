// import {ElseCondition, Instruction, InstructionType, Policy, PolicyStatement, RuleSet} from "./policies";

import {QualifiedName, Type} from '../services/schema';
import {plainToClass} from 'class-transformer';

interface SourceElement {
  src(): string;

  imports(): QualifiedName[];
}

interface PlainTextElement {
  description(): string;
}

export class Policy implements SourceElement {
  constructor(
    public name: QualifiedName,
    public targetTypeName: QualifiedName,
    public ruleSets: RuleSet[] = []
  ) {
  }

  static createNew(targetType: Type): Policy {
    const qualifiedName = targetType.name.namespace + '.' + 'NewPolicy';

    return new Policy(QualifiedName.from(qualifiedName), targetType.name, [
      new RuleSet([
        new PolicyStatement(new ElseCondition(), new PermitInstruction())
      ])
    ]);
  }

  src(): string {
    const importSrc = this.imports().map(imp => `import ${imp.fullyQualifiedName}`).join('\n');

    return `
${importSrc}

namespace ${this.name.namespace}

policy ${this.name.name} against ${this.targetTypeName.fullyQualifiedName} {
${this.ruleSets.map(r => r.src()).join('\n')}
}`;
  }

  imports(): QualifiedName[] {
    let imports = [this.targetTypeName];
    imports = imports.concat(this.ruleSets.flatMap(r => r.imports()));
    return imports;
  }

  static parseDtoArray(policyDtoArray: any[]): Policy[] {
    return policyDtoArray.map(dto => this.parseDto(dto));
  }

  static parseDto(dto: any): Policy {
    const name = plainToClass(QualifiedName, dto.name as QualifiedName);
    const typeName = plainToClass(QualifiedName, dto.targetTypeName as QualifiedName);
    return new Policy(name, typeName, dto.ruleSets.map(r => RuleSet.fromDto(r)));
  }
}


export class RuleSetUtils {
  static elsePrefixWord(ruleSet: RuleSet): string {
    if (!ruleSet) { return ''; }
    return (ruleSet.statements.length > 1) ? 'Otherwise, ' : 'Always ';
  }
}

export class RuleSet implements SourceElement {
  constructor(public statements: PolicyStatement[] = []) {
  }

  appendStatement(policyStatement: PolicyStatement) {
    this.statements.splice(this.statements.length - 1, 0, policyStatement);
  }

  src(): string {
    // TODO - scopes etc
    const statementSrc = this.statements.map(s => `    ${s.src()}`).join('\n');
    return `  read {
${statementSrc}
  }`;
  }

  imports(): QualifiedName[] {
    return this.statements.flatMap(s => s.imports());
  }

  removeStatement(statement: PolicyStatement) {
    const idx = this.statements.indexOf(statement);
    this.statements.splice(idx, 1);
  }

  static fromDto(rulesetDto: any): RuleSet {
    return new RuleSet(rulesetDto.statements.map(s => PolicyStatement.fromDto(s)));
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
    return this.condition.imports().concat(this.instruction.imports());
  }

  static fromDto(dto: any): PolicyStatement {
    return new PolicyStatement(
      ConditionUtils.fromDto(dto.condition),
      InstructionUtils.fromDto(dto.instruction)
    );
  }
}

export class Operator {
  constructor(readonly symbol: string, readonly label: string, readonly enumName: string) {
  }

  static EQUALS = new Operator('=', 'equals', 'EQUAL');
  static NOT_EQUAL = new Operator('!=', 'does not equal', 'NOT_EQUAL');
  static IN = new Operator('in', 'is in', 'IN');

  static operators: Operator[] = [
    Operator.EQUALS,
    Operator.NOT_EQUAL,
    Operator.IN
  ];

  static fromEnum(enumValue: string): Operator {
    return this.operators.find(o => o.enumName == enumValue);
  }

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


class ConditionUtils {
  static fromDto(dto: any): Condition {
    switch (dto.type) {
      case 'else':
        return new ElseCondition();
      case 'case':
        return CaseCondition.fromDto(dto);
    }
  }
}

export interface Condition extends SourceElement {
  readonly type: string;
}

export class ElseCondition implements Condition {
  src(): string {
    return 'else';
  }

  readonly type: string = 'else';

  description(): string {
    return 'Otherwise,';
  }

  imports(): QualifiedName[] {
    return [];
  }
}

export class CaseCondition implements Condition, PlainTextElement {
  readonly type: string = 'case';

  constructor(
    public lhSubject: Subject,
    public operator: Operator = Operator.EQUALS,
    public rhSubject: Subject
  ) {
  }

  static empty(): CaseCondition {
    return new CaseCondition(null, Operator.EQUALS, null);
  }


  // Operators are tricky, as equals property vs equals value isn't knowable
  // until after the rhSubject is set.
  // DisplayOperator is set when editing in the UI, and derivable when
  // receiving from the server
  private _displayOperator: DisplayOperator;

  get displayOperator(): DisplayOperator {
    if (this._displayOperator) { return this._displayOperator; }

    const derived = this.deriveDisplayOperator();
    return (derived) ? derived : Operator.EQUALS_PROPERTY;
  }

  set displayOperator(value: DisplayOperator) {
    this._displayOperator = value;
  }

  private deriveDisplayOperator(): DisplayOperator {
    return Operator.displayOperators.find(o => o.matches(this));
  }

  imports(): QualifiedName[] {
    if (!this.lhSubject || !this.rhSubject) { return []; }
    return this.lhSubject.imports().concat(this.rhSubject.imports());
  }

  src(): string {
    if (!this.lhSubject || !this.rhSubject) { return ''; }
    return `case ${this.lhSubject.src()} ${this.displayOperator.operator.symbol} ${this.rhSubject.src()}`;
  }

  description(): string {
    if (!this.lhSubject || !this.rhSubject) { return null; }
    return `${this.lhSubject.description()} ${this.displayOperator.label} ${this.rhSubject.description()}`;
  }

  static fromDto(dto: any): CaseCondition {
    const c = new CaseCondition(
      SubjectUtils.fromDto(dto.lhSubject),
      Operator.fromEnum(dto.operator),
      SubjectUtils.fromDto(dto.rhSubject)
    );
    return c;
  }
}

export type  SubjectType = 'RelativeSubject' | 'LiteralSubject' | 'LiteralArraySubject';

class SubjectUtils {
  static fromDto(subject: any): Subject {
    switch (subject.type) {
      case 'RelativeSubject':
        return RelativeSubject.fromDto(subject);
      case 'LiteralArraySubject' :
        return new LiteralArraySubject(subject.values);
      case 'LiteralSubject' :
        return new LiteralSubject(subject.value);
    }
  }
}

export interface Subject extends SourceElement, PlainTextElement {
  readonly type: SubjectType;
}

export class RelativeSubject implements Subject {
  static TYPE_TOKEN = '{thisType}';
  readonly type = 'RelativeSubject';

  constructor(
    public source: RelativeSubjectSource,
    public targetTypeName: QualifiedName
  ) {
  }

  src(): string {
    return `${this.source.toString().toLowerCase()}.${this.targetTypeName.fullyQualifiedName}`;
  }

  imports(): QualifiedName[] {
    return [this.targetTypeName];
  }

  description(): string {
    // HACK:  using a repacement token, since I can't easily access the type this subect is defined
    // against in this method
    const prefix = (this.source == RelativeSubjectSource.THIS) ? `this ${RelativeSubject.TYPE_TOKEN}'s` : 'caller\'s';
    return `${prefix} ${this.targetTypeName.name}`;
  }

  static fromDto(subject: any): RelativeSubject {
    const targetTypeName: QualifiedName = plainToClass(QualifiedName, subject.targetTypeName as QualifiedName);
    return new RelativeSubject(subject.source, targetTypeName);
  }
}

export enum RelativeSubjectSource {
  CALLER = 'CALLER',
  THIS = 'THIS'
}

export class LiteralArraySubject implements Subject {
  readonly type = 'LiteralArraySubject';

  constructor(public values: any[]) {
  }

  src(): string {
    return `[ ${this.values.map(v => `"${v}"`).join(',')} ]`;
  }

  imports(): QualifiedName[] {
    return [];
  }


  description(): string {
    return this.src();
  }
}

export class LiteralSubject implements Subject {
  readonly type = 'LiteralSubject';

  constructor(public value: any) {
  }

  src(): string {
    return `"${this.value.toString()}"`;
  }

  imports(): QualifiedName[] {
    return [];
  }

  description(): string {
    return this.src();
  }
}

export interface Instruction extends SourceElement, PlainTextElement {
  readonly type: InstructionType;
}

export class PermitInstruction implements Instruction {
  readonly type: InstructionType = InstructionType.PERMIT;

  description(): string {
    return 'permit';
  }

  imports(): QualifiedName[] {
    return [];
  }

  src(): string {
    return 'permit';
  }
}

export class FilterInstruction implements Instruction {

  static filterAll(): FilterInstruction {
    const i = new FilterInstruction([]);
    i.isFilterAll = true;
    return i;
  }

  static filterAttributes(): FilterInstruction {
    const i = new FilterInstruction([]);
    i.isFilterAll = false;
    return i;
  }

  constructor(public fieldNames: string[]) {
  }

  // Set when editing in the UI.  Not sent from the server
  private _isExplicitFilterAttributes: boolean;

  get isFilterAll(): boolean {
    if (this._isExplicitFilterAttributes) { return false; }
    return this.fieldNames == null || this.fieldNames.length == 0;
  }

  // Only set from the UI when editing
  set isFilterAll(value: boolean) {
    this._isExplicitFilterAttributes = !value;
  }

  readonly type: InstructionType = InstructionType.FILTER;

  description(): string {
    const fieldNameList = (!this.fieldNames || this.fieldNames.length == 0) ? ' entire record' : ` properties ${this.fieldNames.join(', ')}`;
    return `filter ${fieldNameList}`;
  }

  imports(): QualifiedName[] {
    return [];
  }

  src(): string {
    const fieldNameList = (!this.fieldNames || this.fieldNames.length == 0) ? '' : `( ${this.fieldNames.join(', ')} )`;
    return `filter ${fieldNameList}`;
  }

  static fromDto(dto: FilterInstruction): FilterInstruction {
    return new FilterInstruction(dto.fieldNames);
  }
}

class InstructionUtils {
  static fromDto(dto: Instruction): Instruction {
    switch (dto.type) {
      case InstructionType.PERMIT:
        return new PermitInstruction();
      case InstructionType.FILTER:
        return FilterInstruction.fromDto(dto as FilterInstruction);
    }
  }

}

// Commented out while processors are disabled.
// https://gitlab.com/vyne/vyne/issues/52
// export class Instruction implements SourceElement, PlainTextElement {
//   constructor(
//     public type: InstructionType,
//     public processor?: InstructionProcessor) {
//   }
//
//   static permit(): Instruction {
//     return new Instruction(InstructionType.PERMIT)
//   }
//
//   src(): string {
//     const processorString = (this.processor) ? this.processor.src() : "";
//     return `${this.type.toString().toLowerCase()}${processorString}`;
//   }
//
//   imports(): QualifiedName[] {
//     return []
//   }
//
//   description(): string {
//     const processorString = (this.processor) ? this.processor.description() : "";
//     return `${this.type.toString().toLowerCase()}${processorString}`
//   }
//
//   static fromDto(dto: Instruction): Instruction {
//     return new Instruction(
//       dto.type,
//       dto.processor
//     )
//   }
// }
//
// export class InstructionProcessor
//   implements SourceElement, PlainTextElement {
//   name: string;
//   args: any[];
//
//   src(): string {
//     return ` using ${this.name}${this.argsString}`;
//   }
//
//   imports(): QualifiedName[] {
//     return []
//   }
//
//   private get argsString(): string {
//     return (this.args) ? `( ${this.args.join(",")} )` : "";
//   }
//
//   description(): string {
//     const nameParts = this.name.split(".");
//     const shortName = nameParts[nameParts.length - 1];
//     return ` using ${shortName}${this.argsString}`;
//   }
// }

export enum InstructionType {
  PERMIT = 'PERMIT',
  // PROCESS = "PROCESS",
  FILTER = 'FILTER',
}


function isOperatorType(operator: Operator, subjectType: SubjectType) {
  return function (caseCondition: CaseCondition) {
    if (!caseCondition || !caseCondition.rhSubject) { return false; }
    return caseCondition.rhSubject.type == subjectType && caseCondition.operator == operator;
  };
}

export interface DisplayOperator {
  operator: Operator;
  label: string;
  literalOrProperty?: 'literal' | 'property';
  matches: (CaseCondition) => boolean;
}


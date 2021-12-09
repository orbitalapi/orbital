import {Component, EventEmitter, Input, Output} from '@angular/core';
import {FilterInstruction, Instruction, InstructionType, PermitInstruction, PolicyStatement} from './policies';
import {Type} from '../services/schema';
import {MatSelectChange} from '@angular/material/select';

@Component({
  selector: 'app-instruction-selector',
  template: `
    <div style="display: flex">
      <mat-form-field *ngIf="instructionOption">
        <mat-select [(value)]="instructionOption">
          <mat-option *ngFor="let option of options" [value]="option">{{option.label}}</mat-option>
        </mat-select>
      </mat-form-field>

      <mat-form-field *ngIf="instructionOption.showAttributeSelector" class="attribute-selector">
        <mat-select placeholder="Attributes" multiple [(value)]="filterInstruction.fieldNames"
                    (selectionChange)="emitStatementUpdated()">
          <mat-option *ngFor="let attribute of attributes" [value]="attribute">{{attribute}}</mat-option>
        </mat-select>
      </mat-form-field>

      <!-- TODO : Processors are currently disabled -->
      <!--<div class="processor" *ngIf="instruction?.processor">-->
      <!--<span>using&nbsp;</span>-->
      <!--<mat-select [(value)]="instruction.processor.name" (selectionChange)="onProcessorChanged($event)">-->
      <!--<mat-option *ngFor="let processor of processors" [value]="processor">{{processor}}</mat-option>-->
      <!--</mat-select>-->
      <!--</div>-->
    </div>
  `,
  styleUrls: ['./instruction-selector.component.scss']
})
export class InstructionSelectorComponent {

  get filterInstruction(): FilterInstruction {
    if (!this.statement || !this.statement.instruction || this.statement.instruction.type !== InstructionType.FILTER) {
      return null;
    }

    return this.statement.instruction as FilterInstruction;
  }

  readonly options: InstructionOption[] = [
    {
      label: 'permit',
      onSelect: () => new PermitInstruction(),
      matches: (statement: PolicyStatement) => statement.instruction.type === InstructionType.PERMIT,
      showAttributeSelector: false
    },
    {
      label: 'filter attributes',
      onSelect: () => FilterInstruction.filterAttributes(),
      matches: (statement: PolicyStatement) => statement.instruction.type === InstructionType.FILTER &&
        !((statement.instruction as FilterInstruction).isFilterAll),
      showAttributeSelector: true
    },
    {
      label: 'filter entire result',
      onSelect: () => FilterInstruction.filterAll(),
      matches: (statement: PolicyStatement) => statement.instruction.type === InstructionType.FILTER &&
        (statement.instruction as FilterInstruction).isFilterAll,
      showAttributeSelector: false
    }
  ];

  @Input()
  statement: PolicyStatement;

  @Input()
  policyType: Type;

  get attributes(): string[] {
    if (!this.policyType) {
      return [];
    }
    return Object.keys(this.policyType.attributes);
  }

  get instructionOption(): InstructionOption {
    if (!this.statement) {
      return this.options[0];
    }
    return this.options.find(o => o.matches(this.statement));
  }

  set instructionOption(value: InstructionOption) {
    this.statement.instruction = value.onSelect();
    this.emitStatementUpdated();
  }


  // https://gitlab.com/vyne/vyne/issues/52
  // readonly processors = [
  //   "vyne.StringMasker"
  // ];

  ngOnInit() {
  }

  emitStatementUpdated() {
    this.statementUpdated.emit();
  }

  @Output()
  statementUpdated: EventEmitter<string> = new EventEmitter();

  onInstructionTypeChanged($event: MatSelectChange) {
    // Processors are disabled:
    //   https://gitlab.com/vyne/vyne/issues/52
    //   if ($event.value == InstructionType.PROCESS) {
    //     this.instruction.processor = new InstructionProcessor()
    //   } else {
    //     this.instruction.processor = null;
    //   }
    //   this.statementUpdated.emit("");
    // let value = $event.value as InstructionOption;
    // value.onSelect()
  }

  onProcessorChanged($event: MatSelectChange) {
    this.statementUpdated.emit('');
  }
}

interface InstructionOption {
  label: string;
  onSelect: () => Instruction;
  matches: (PolicyStatement) => boolean;
  showAttributeSelector: boolean;

}

import {Component, EventEmitter, Input, Output} from '@angular/core';
import {CaseCondition, Instruction, InstructionProcessor, InstructionType} from "./policies";
import {MatSelectChange} from "@angular/material";
import {Schema} from "../services/schema";

@Component({
  selector: 'app-instruction-selector',
  template: `
    <div style="display: flex">
      <mat-form-field *ngIf="instruction">
        <mat-select [(value)]="instruction.type" (selectionChange)="onInstructionTypeChanged($event)">
          <mat-option *ngFor="let option of options" [value]="option">{{option.toString().toLowerCase()}}</mat-option>
        </mat-select>
      </mat-form-field>
      <div class="processor" *ngIf="instruction?.processor">
        <span>using&nbsp;</span>
        <mat-select [(value)]="instruction.processor.name" (selectionChange)="onProcessorChanged($event)">
          <mat-option *ngFor="let processor of processors" [value]="processor">{{processor}}</mat-option>
        </mat-select>
      </div>
    </div>
  `,
  styleUrls: ['./instruction-selector.component.scss']
})
export class InstructionSelectorComponent {


  readonly options = [InstructionType.PERMIT, InstructionType.PROCESS, InstructionType.FILTER]

  @Input()
  instruction: Instruction;

  readonly processors = [
    "vyne.StringMasker"
  ];

  ngOnInit() {
  }

  @Output()
  statementUpdated: EventEmitter<string> = new EventEmitter();

  onInstructionTypeChanged($event: MatSelectChange) {
    if ($event.value == InstructionType.PROCESS) {
      this.instruction.processor = new InstructionProcessor()
    } else {
      this.instruction.processor = null;
    }
    this.statementUpdated.emit("");
  }

  onProcessorChanged($event: MatSelectChange) {
    this.statementUpdated.emit("");
  }
}

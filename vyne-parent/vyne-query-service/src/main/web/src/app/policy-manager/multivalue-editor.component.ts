import {Component, EventEmitter, Input, Output} from '@angular/core';
import {COMMA, ENTER} from '@angular/cdk/keycodes';
import {MatChipInputEvent} from '@angular/material';
import {CaseCondition, LiteralArraySubject} from "./policies";
import {Schema} from "../services/schema";


@Component({
  selector: 'app-multivalue-editor',
  template: `
    <mat-form-field class="example-chip-list">
      <mat-chip-list #chipList>
        <mat-chip *ngFor="let value of values" [selectable]="selectable"
                  [removable]="removable" (removed)="remove(value)">
          {{value}}
          <mat-icon matChipRemove *ngIf="removable">cancel</mat-icon>
        </mat-chip>
        <input placeholder="Add value..."
               [matChipInputFor]="chipList"
               [matChipInputSeparatorKeyCodes]="separatorKeysCodes"
               [matChipInputAddOnBlur]="addOnBlur"
               (matChipInputTokenEnd)="add($event)">
      </mat-chip-list>
    </mat-form-field>
  `,
  styleUrls: ['./multivalue-editor.component.scss']
})
export class MultivalueEditorComponent {

  selectable = true;
  removable = true;
  addOnBlur = true;
  readonly separatorKeysCodes: number[] = [ENTER, COMMA];

  values: any[] = [];

  @Input()
  caseCondition: CaseCondition;

  @Output()
  statementUpdated: EventEmitter<string> = new EventEmitter();


  add(event: MatChipInputEvent): void {
    const input = event.input;
    const value = event.value;

    // Add our fruit
    if ((value || '').trim()) {
      this.values.push(value.trim());
      this.resetCondition()
    }

    // Reset the input value
    if (input) {
      input.value = '';
    }
  }

  private resetCondition() {
    this.caseCondition.rhSubject = new LiteralArraySubject(this.values);
    this.statementUpdated.emit("");

  }

  remove(value: any): void {
    const index = this.values.indexOf(value);

    if (index >= 0) {
      this.values.splice(index, 1);
    }
  }

}

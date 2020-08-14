import {Component, EventEmitter, Output} from '@angular/core';
import {FormBuilder, FormControl, FormGroup} from '@angular/forms';

@Component({
  selector: 'app-type-name-panel',
  templateUrl: './type-name-panel.component.html',
  styleUrls: ['./type-name-panel.component.scss']
})
export class TypeNamePanelComponent {
  addTypeNameFormGroup: FormGroup;
  typeName = new FormControl();
  @Output() assignedTypeName: EventEmitter<string> = new EventEmitter<string>();

  @Output() isGenerateSchemaClicked = new EventEmitter<boolean>();
  isTypePresent = false;

  constructor(fb: FormBuilder) {
    this.addTypeNameFormGroup = fb.group({
      typeName: this.typeName
    });
  }

  updateTypeName() {
    if (this.typeName.value.length > 3) {
      this.isTypePresent = true;
    } else {
      this.isTypePresent = false;
    }
  }

  showGenerateSchemaPanel($event) {
    this.isTypePresent = true;
    this.assignedTypeName.emit(this.typeName.value);
    this.isGenerateSchemaClicked.emit($event);
  }
}

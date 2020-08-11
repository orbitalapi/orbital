import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Schema, Type} from '../../services/schema';
import {FormBuilder, FormControl, FormGroup} from '@angular/forms';

@Component({
  selector: 'app-type-name-panel',
  templateUrl: './type-name-panel.component.html',
  styleUrls: ['./type-name-panel.component.scss']
})
export class TypeNamePanelComponent {
  addTypeNameFormGroup: FormGroup;
  typeName = new FormControl();
  @Output() changedTypeName: EventEmitter<string> = new EventEmitter<string>();

  constructor(fb: FormBuilder) {
    this.addTypeNameFormGroup = fb.group({
      typeName: this.typeName
    });
  }

  updateTypeName() {
    this.changedTypeName = this.typeName.value;
  }

}

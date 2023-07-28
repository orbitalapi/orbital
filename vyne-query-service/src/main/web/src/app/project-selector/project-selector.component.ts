import {ChangeDetectionStrategy, Component, Input} from '@angular/core';
import {SourcePackageDescription} from "../package-viewer/packages.service";
import {ControlValueAccessor, NG_VALUE_ACCESSOR} from "@angular/forms";

@Component({
  selector: 'app-project-selector',
  template: `
    <tui-select
      [stringify]="stringify"
      [ngModel]="selectedPackage"
      (ngModelChange)="setValue($event)"

    >
      {{prompt}}
      <input
        tuiTextfield [disableControl]="disabled"
      />
      <tui-data-list-wrapper
        *tuiDataList
        [items]="editablePackages | tuiFilterByInputWith : stringify"
        [itemContent]="stringify | tuiStringifyContent"
      ></tui-data-list-wrapper>
    </tui-select>
  `,
  styleUrls: ['./project-selector.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: ProjectSelectorComponent
    }
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectSelectorComponent implements ControlValueAccessor {
  @Input()
  disabled: boolean = false
  @Input()
  packages: SourcePackageDescription[];
  @Input()
  filterToEditableOnly: boolean = true;
  @Input()
  prompt: string
  @Input()
  selectedPackage: SourcePackageDescription | null;

  get editablePackages(): SourcePackageDescription[] {
    if (!this.packages) {
      return [];
    } else if (!this.filterToEditableOnly) {
      return this.packages
    } else {
      return this.packages.filter(p => p.editable)
    }
  }

  readonly stringify = (item: SourcePackageDescription) => item.identifier.name;

  onChange = (value) => {
  };

  onTouched = () => {
  };

  registerOnChange(fn: any): void {
    this.onChange = fn
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  writeValue(obj: any): void {
    this.selectedPackage = obj
  }

  setValue(value: SourcePackageDescription) {
    this.selectedPackage = value;
    this.onChange(value);
    this.onTouched();
  }
}

import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from '@angular/core';
import {CompilationMessage, Schema, Type} from "../../services/schema";

@Component({
    selector: 'app-designer-code-editor-panel',
    template: `
      <app-panel-header title="Taxi model editor">
          <div class="spacer"></div>

          <tui-combo-box
                  class="type-input"
                  [stringify]="stringifyTypeName"
                  tuiTextfieldSize="s"
                  [(ngModel)]="selectedType"
                  (ngModelChange)="handleSelectedTypeChanged($event)">
              Select a type
              <input tuiTextfield placeholder="Select a type"/>
              <tui-data-list-wrapper
                      *tuiDataList
                      [items]="types | tuiFilterByInputWith : stringifyTypeName"
                      [itemContent]="stringifyTypeName | tuiStringifyContent"></tui-data-list-wrapper>
          </tui-combo-box>

      </app-panel-header>

      <as-split gutterSize="5" direction="vertical" unit="pixel">
          <as-split-area size="*">
              <app-code-editor (contentChange)="taxiChange.emit($event)"></app-code-editor>
          </as-split-area>
          <as-split-area size="135">
              <app-compilation-message-list [compilationMessages]="compilationErrors"></app-compilation-message-list>
          </as-split-area>
      </as-split>


  `,
    styleUrls: ['./code-editor-panel.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class CodeEditorPanelComponent {

    readonly stringifyTypeName = (item: Type): string => item.longDisplayName;

    @Output()
    taxiChange = new EventEmitter<string>();

    @Input()
    compilationErrors: CompilationMessage[];

    selectedType: Type;

    @Output()
    selectedTypeChanged = new EventEmitter<string>();

    @Input()
    schema: Schema;

    @Input()
    parsedTypes: Type[];

    get types(): Type[] {
        const newTypes = this.parsedTypes || [];
        return newTypes.concat(this.schema?.types || [])
            .filter(t => !t.fullyQualifiedName.startsWith("io.vyne") && !t.fullyQualifiedName.startsWith("lang.taxi") && !t.fullyQualifiedName.startsWith("taxi.stdlib") && !t.fullyQualifiedName.startsWith("vyne.vyneQl"))
            ;
    }


    handleSelectedTypeChanged($event: Type) {
        this.selectedTypeChanged.emit($event.fullyQualifiedName)
    }
}

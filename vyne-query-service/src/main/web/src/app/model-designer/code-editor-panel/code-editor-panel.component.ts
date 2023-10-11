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
                    [valueContent]="value"
                    [(ngModel)]="selectedType"
                    (ngModelChange)="handleSelectedTypeChanged($event)">
                Select a type
                <input tuiTextfield placeholder="Select a type"/>
                <ng-template #value let-item>
                    <div class="type-option">
                        <span class="type-name">{{item.name.shortDisplayName}}</span>
                      <span class="mono-badge small" *ngIf="item.name.namespace">{{item.name.namespace}}</span>
                    </div>
                </ng-template>
                <ng-template tuiDataList>
                    <tui-data-list *ngFor="let item of types | tuiFilterByInputWith : stringifyTypeName">
                        <button tuiOption [value]="item">
                          <div class="type-option">
                            <span class="type-name">{{item.name.shortDisplayName}}</span>
                            <span class="mono-badge small" *ngIf="item.name.namespace">{{item.name.namespace}}</span>
                          </div>
                        </button>
                    </tui-data-list>
                </ng-template>
                <!--              <tui-data-list-wrapper-->
                <!--                      *tuiDataList-->
                <!--                      [items]="types | tuiFilterByInputWith : stringifyTypeName"-->
                <!--                      [itemContent]="stringifyTypeName | tuiStringifyContent"></tui-data-list-wrapper>-->
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

    readonly stringifyTypeName = (item: Type): string => item.name.shortDisplayName;

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
      this.parsedTypes.filter(t => t.name.shortDisplayName)
        const newTypes = this.parsedTypes || [];
        return newTypes.concat(this.schema?.types || [])
            .filter(t => !t.fullyQualifiedName.startsWith("io.vyne")
              && !t.fullyQualifiedName.startsWith("lang.taxi")
              && !t.fullyQualifiedName.startsWith("taxi.stdlib")
              && !t.fullyQualifiedName.startsWith("vyne.vyneQl")
              && !t.fullyQualifiedName.startsWith("Anonymous")
            )
            ;
    }


    handleSelectedTypeChanged($event: Type) {
        this.selectedTypeChanged.emit($event.fullyQualifiedName)
    }
}

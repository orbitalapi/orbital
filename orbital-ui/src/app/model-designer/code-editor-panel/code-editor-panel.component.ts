import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from '@angular/core';
import {CompilationMessage, Schema, SchemaMember, Type} from "../../services/schema";

@Component({
    selector: 'app-designer-code-editor-panel',
    template: `
        <app-panel-header title="Taxi model editor">
            <div class="spacer"></div>

            <app-type-autocomplete-tui
                    class="type-input"
                    [schema]="schema"
                    [additionalTypes]="parsedTypes"
                    (selectedTypeChanged)="handleSelectedTypeChanged($event)"></app-type-autocomplete-tui>
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


    handleSelectedTypeChanged($event: SchemaMember) {
        this.selectedTypeChanged.emit($event.name.fullyQualifiedName)
    }
}

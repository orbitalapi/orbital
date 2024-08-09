import {Clipboard} from '@angular/cdk/clipboard';
import {ChangeDetectionStrategy, Component, EventEmitter, HostBinding, Inject, Input, Output} from '@angular/core';
import {TuiAlertService, TuiNotification} from '@taiga-ui/core';
import {CompilationMessage, Schema, SchemaMember, Type} from "../../services/schema";

@Component({
    selector: 'app-designer-code-editor-panel',
    template: `
        <tui-notification *ngIf="disabled" class="onboarding-text" status="neutral" size="s">
          Once you've provided your data sample, create a Taxi schema here to describe your data
        </tui-notification>
        <app-panel-header title="Taxi model editor" [isSecondary]="true">
            <div class="spacer"></div>
            <app-type-autocomplete-tui
                    class="type-input"
                    [schema]="schema"
                    [additionalTypes]="parsedTypes"
                    (selectedTypeChanged)="handleSelectedTypeChanged($event)"
            ></app-type-autocomplete-tui>
            <a
              tuiLink
              class="button-link"
              tuiHint="Copy model"
              tuiHintAppearance="onDark"
              (click)="copyModel(taxi)"
            >
              <img src="assets/img/tabler/copy.svg">
            </a>
        </app-panel-header>
        <app-code-editor
          [compilationMessages]="compilationErrors"
          [showCompilationProblemsPanel]="true"
          [setFocus]="!disabled"
          (contentChange)="taxi=$event; taxiChange.emit($event)"
        ></app-code-editor>
    `,
    styleUrls: ['./code-editor-panel.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class CodeEditorPanelComponent {
    @Input()
    compilationErrors: CompilationMessage[];

    @Input()
    schema: Schema;

    @Input()
    parsedTypes: Type[];

    @Input()
    @HostBinding('class.is-disabled')
    disabled: boolean;

    @Output()
    selectedTypeChanged = new EventEmitter<SchemaMember>();

    @Output()
    taxiChange = new EventEmitter<string>();

    taxi: string

    constructor(
      private clipboard: Clipboard,
      @Inject(TuiAlertService) private readonly alerts: TuiAlertService,
    ) {
    }

    handleSelectedTypeChanged($event: SchemaMember) {
        this.selectedTypeChanged.emit($event)
    }

    copyModel(query: string) {
      this.clipboard.copy(query);
      this.alerts.open('Copied to clipboard', {status: TuiNotification.Success})
        .subscribe()
    }
}

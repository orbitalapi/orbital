import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter, HostBinding,
  Input,
  OnChanges,
  Output,
  SimpleChanges
} from '@angular/core';
import {ModelParseRequest, TaxiParseResult, TaxiParserService} from "../taxi-parser.service";
import {BehaviorSubject, combineLatest, EMPTY} from "rxjs";
import {catchError, debounceTime, map, mergeMap} from "rxjs/operators";
import {isNullOrUndefined} from "util";
import {CompilationMessage, findType, Schema, Type} from "../../services/schema";
import {SourceWithTypeHints} from "../../json-viewer/json-results-view.component";

@Component({
  selector: 'app-designer-parse-result-panel',
  template: `
    <tui-notification *ngIf="disabled" class="onboarding-text" status="neutral" size="s">
      @if (!taxi && !targetType) {
        As you edit your Taxi schema above, parsing results will appear here
      } @else {
        Select a type from the drop-down in the model editor above to see parsing results
      }
    </tui-notification>
    <app-panel-header title="Parse Result" [isSecondary]="true">
        <div class="spacer"></div>
        <progress tuiProgressBar size="s" *ngIf="working"></progress>
    </app-panel-header>
    <app-json-viewer [json]="sourceWithHints" *ngIf="sourceWithHints" [readOnly]="true"></app-json-viewer>
    <div class="row center">
        <tui-notification *ngIf="errorMessage" status="error">{{errorMessage}}</tui-notification>
    </div>
    `,
  styleUrls: ['./parse-result-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ParseResultPanelComponent implements OnChanges {

  working: boolean = true;

  private source$ = new BehaviorSubject<string>("");
  private taxi$ = new BehaviorSubject<string>("")
  private targetType$ = new BehaviorSubject<string>("")

  @Output()
  compilationErrorsChanged = new EventEmitter<CompilationMessage[]>();

  @Input()
  @HostBinding('class.is-disabled')
  disabled: boolean;

  get sourceWithHints(): SourceWithTypeHints {
    if (isNullOrUndefined(this.parseResult?.parseResult?.json)) return null;
    return {
      source: this.parseResult.parseResult.json,
      typeHints: this.parseResult.parseResult.typeHints
    }
  }

  get anonymousTypes(): Type[] {
    return this.parseResult?.newTypes || [];
  }

  get resultType(): Type {
    if (!isNullOrUndefined(this.targetType) && !isNullOrUndefined(this.schema)) {
      return findType(this.schema, this.targetType, this.anonymousTypes)
    } else {
      return null;
    }
  }

  constructor(private service: TaxiParserService, private changeDetector: ChangeDetectorRef) {
    combineLatest([this.source$, this.taxi$, this.targetType$]).pipe(
      debounceTime(250),
      map(([source, taxi, targetType]) => ({source, taxi, targetType})),
      mergeMap(({source, taxi, targetType}) => {
        this.working = true;
        const hasModelDefinition = !isNullOrUndefined(source) && !isNullOrUndefined(targetType) && targetType.length > 0;
        const modelRequest: ModelParseRequest | null = (hasModelDefinition) ? {
          model: source,
          targetType,
          includeTypeInformation: true
        } : null
        return this.service.parseTaxiModel({
          model: modelRequest,
          taxi,
        }).pipe(
          catchError(errorResponse => {
            this.working = false;
            this.errorMessage = errorResponse.error.message;
            this.changeDetector.markForCheck();
            return EMPTY
          })
        )
      }),
    ).subscribe(
      value => {
        this.working = false;
        this.parseResult = value;

        this.parsedTypesChanged.emit(value.newTypes);
        this.changeDetector.markForCheck();
        this.compilationErrorsChanged.emit(value.compilationErrors);
        this.errorMessage = value.hasParseErrors ? value.parseResult.parseErrors.join("\n") : null;
      },
      errorResponse => {
        console.error(errorResponse)
        this.working = false;
        this.parseResult = null;
        this.errorMessage = errorResponse.error.message;
        this.changeDetector.markForCheck();
      }
    )
  }

  @Input()
  source: string;

  @Input()
  schema: Schema;

  @Input()
  taxi: string;

  @Input()
  targetType: string;

  @Output()
  parsedTypesChanged = new EventEmitter<Type[]>();

  parseResult: TaxiParseResult | null = null;
  errorMessage: string | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.source) {
      this.source$.next(changes['source'].currentValue)
    }
    if (changes.taxi) {
      this.taxi$.next(changes['taxi'].currentValue)
    }
    if (changes.targetType) {
      this.targetType$.next(changes['targetType'].currentValue)
    }
    if (changes.disabled) {
      this.errorMessage = null;
    }
  }
}

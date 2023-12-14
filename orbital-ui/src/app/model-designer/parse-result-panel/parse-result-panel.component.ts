import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges
} from '@angular/core';
import {ModelParseRequest, TaxiParseResult, TaxiParserService} from "../taxi-parser.service";
import {BehaviorSubject, combineLatest, EMPTY, of} from "rxjs";
import {catchError, debounceTime, map, mergeMap} from "rxjs/operators";
import {isNullOrUndefined} from "util";
import {CompilationMessage, findType, Schema, Type, TypeNamedInstance} from "../../services/schema";
import {Observable} from "rxjs/internal/Observable";
import {PARSE_RESULT} from "./parsedData";

@Component({
    selector: 'app-designer-parse-result-panel',
    template: `
        <app-panel-header title="Parse Result">
            <div class="spacer"></div>
            <progress tuiProgressBar size="s" *ngIf="working"></progress>
        </app-panel-header>
        <!--            <app-type-named-instance-tree-->
        <!--                    [anonymousTypes]="anonymousTypes" [type]="resultType" [instance]="parsedEntity"-->
        <!--                    ></app-type-named-instance-tree>-->
        <!--        <app-tabbed-results-view [anonymousTypes]="anonymousTypes" [type]="resultType" [instances$]="parsedEntity$"-->
        <!--                                 [profilerEnabled]="false"-->
        <!--                                 [downloadSupported]="false"-->
        <!--        ></app-tabbed-results-view>-->

        <app-json-viewer [json]="parseResult?.parseResult?.json" *ngIf="parseResult" [typeHints]="parseResult?.parseResult?.typeHints" [readOnly]="true"></app-json-viewer>
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
    }
}

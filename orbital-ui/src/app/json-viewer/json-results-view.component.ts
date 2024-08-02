import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { Observable, Subscription } from 'rxjs';
import { findType, InstanceLike, Schema, Type } from 'src/app/services/schema';
import {ModelParseResult, Position, TypePosition} from '../model-designer/taxi-parser.service';
import { AppConfig, AppInfoService } from '../services/app-info.service';
import { ValueWithTypeName } from '../services/models';
import { isNullOrUndefined } from '../utils/utils';
import { JSONPathFinder } from './JsonPathFinder';


export type SourceWithTypeHints = {
  source: string;
  typeHints: TypePosition[]
}

export function isSourceWithTypeHints(obj: any): obj is SourceWithTypeHints {
  return typeof obj.source === 'string' && Array.isArray(obj.typeHints)
}


@Component({
  selector: 'app-json-results-view',
  template: `
    <app-json-viewer
      [json]="jsonOrSource"
      [readOnly]="true"
      [showResultsSizeWarning]="instanceJson.length > 5000"
      [isResponseLarge]="isResponseLarge"
    ></app-json-viewer>
  `,
  styleUrls: ['./json-results-view.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class JsonResultsViewComponent {

  instanceJson: string[];
  allInstancesJson: string = '';
  private instancesToJson = new Map<ValueWithTypeName, string>();
  private _instanceSubscription: Subscription;
  private appConfig: AppConfig;

  constructor(
    private changeDetector: ChangeDetectorRef,
    private appInfoService: AppInfoService,
  ) {
    appInfoService.getConfig()
      .subscribe(next => this.appConfig = next);
  }

  private emptySrcWithTypeHints(): SourceWithTypeHints {
    return {
      source: '',
      typeHints: []
    }
  }

  sourceWithTypeHints: SourceWithTypeHints = this.emptySrcWithTypeHints();

  get jsonOrSource(): string | SourceWithTypeHints {
    if (!isNullOrUndefined(this.sourceWithTypeHints)) {
      return this.sourceWithTypeHints
    } else {
      return this.allInstancesJson;
    }
  }


  private _schema: Schema;
  @Input()
  get schema(): Schema {
    return this._schema;
  }

  set schema(value: Schema) {
    this._schema = value;
    this.updateInlineHints();

  }

  @Input()
  isStreamingQuery: boolean;

  @Input()
  isResponseLarge: boolean;

  private _instances$: Observable<InstanceLike>;

  @Input()
  get instances$(): Observable<InstanceLike> {
    return this._instances$;
  }

  set instances$(value: Observable<InstanceLike>) {
    if (value === this._instances$) {
      return;
    }
    if (this._instanceSubscription) {
      this._instanceSubscription.unsubscribe();
    }
    this._instances$ = value;
    this.instanceJson = [];

    this.allInstancesJson = '';
    this.sourceWithTypeHints = this.emptySrcWithTypeHints();
    this.instancesToJson.clear();
    this.changeDetector.markForCheck();

    this._instanceSubscription = this._instances$.subscribe(nextResult => {
      let json = JSON.stringify(nextResult.value, null, 3);
      this.isStreamingQuery ? this.instanceJson.unshift(json) : this.instanceJson.push(json);
      const concatenatedJson = (this.allInstancesJson.length > 0) ?
        this.isStreamingQuery ?
          json + '\n\n' + this.allInstancesJson:
          this.allInstancesJson + '\n\n' + json :
        json;
      this.allInstancesJson = concatenatedJson;
      this.instancesToJson.set(nextResult as ValueWithTypeName, json)
      this.updateInlineHints()
      this.changeDetector.markForCheck();
    });
  }

  private updateInlineHints() {
    // For now, only supporting inline hints with a single result.
    if (this.instancesToJson.size != 1) {
      this.sourceWithTypeHints = null;
      return;
    }

    if (!this.schema)
      return;

    const value = Array.from(this.instancesToJson.keys())[0]
    const json = this.instancesToJson.get(value);

    try {
      const typeHints = this.createInlineHintsForValue(json, value);
      this.sourceWithTypeHints = {
        source: json,
        typeHints
      }
    } catch (e) {
      console.error('Failed to build type hints', e)
      this.sourceWithTypeHints = {
        source: json,
        typeHints: []
      }
    }
    this.changeDetector.markForCheck();
  }

  private createInlineHintsForValue(json: string, nextResult: ValueWithTypeName): TypePosition[] {
    const rootType = findType(this._schema, nextResult.typeName, nextResult.anonymousTypes);
    const pathFinder = new JSONPathFinder(json)
    const colonMap = pathFinder.getColonMap()
    const typePositions = colonMap.map(pathAndPosition => {
      const fieldNames = pathAndPosition.path.split('.');
      fieldNames.splice(0, 1) // remove the first entry, which is always '$' in a jsonPath
      const type = this.getTypeFromJsonPath(rootType, fieldNames, this.schema, nextResult.anonymousTypes);
      const oneBasedPosition: Position = {
        line: pathAndPosition.position.line + 1, // vscode line numbers are 1-based
        char: pathAndPosition.position.char + 2 // vscode char indices are 1-based, plus 1 to offset after the ':'
      }
      const typePosition: TypePosition = {
        start: oneBasedPosition,
        startOffset: 0, // TODO : I don't think this is used
        path: fieldNames.join('.'),
        type: type.name
      }
      return typePosition
    })
    return typePositions;
  }

  private getTypeFromJsonPath(rootType: Type, fieldNames: string[], schema: Schema, anonymousTypes: Type[]): Type {
    return fieldNames.reduce(
      (previousValue: Type, attributeName: string) => {
        let type: Type;
        if (previousValue['missing']) {
          return {
            name: previousValue.name + '.' + attributeName,
            missing: true
          } as any as Type
        }

        // If we're navigating into an array, use the array member type.
        if (attributeName.startsWith('[')) {
          if (!previousValue.isCollection) {
            throw new Error(`Trying to inspect path: ${fieldNames.join(',')}, but at attribute ${attributeName} expected to be within a collection, but was ${previousValue.name.parameterizedName}`)
          }
          type = previousValue.collectionType;
        } else {
          // Otherwise, use the field type
          const field = previousValue.attributes[attributeName];
          if (isNullOrUndefined(field)) {
            return {
              name: `unknown - field ${attributeName} not found`,
              missing: true
            } as any as Type
          }
          try {
            type = findType(schema, field.type.parameterizedName, anonymousTypes)
          } catch (e) {
            console.warn(`Type ${field.type.parameterizedName} not found in the schema or ${anonymousTypes.length} anonymous types`)
            return {
              name: field.type.parameterizedName + ' (missing)',
              missing: true
            } as any as Type
          }

        }
        return type
      },
      rootType
    )
  }
}

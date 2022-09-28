import {Component, Input, OnInit} from '@angular/core';
import {OperationQueryResult, TypesService} from '../services/types.service';
import {ActivatedRoute, ParamMap} from '@angular/router';
import {filter, map} from 'rxjs/operators';
import {Schema, Type} from '../services/schema';
import { buildInheritable, Inheritable } from '../inheritence-graph/inheritance-graph.component';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-type-viewer-container',
  styleUrls: ['./type-viewer-container.component.scss'],
  template: `
    <app-header-bar title="Catalog">
    </app-header-bar>
    <app-type-viewer [type]="type"
                     [schema]="schema"
                     [schema$]="schema$"
                     class="page-content"
                     [inheritanceView]="inheritenceView"
                     [typeUsages]="typeUsages"
                     [showAttributes]="showAttributes"
                     [showDocumentation]="showDocumentation"
                     [showTags]="showTags"
                     [showTaxi]="showTaxi"
                     [showUsages]="showUsages"
                     [showInheritanceGraph]="showInheritanceGraph"
    ></app-type-viewer>`
})
export class TypeViewerContainerComponent implements OnInit {
  typeUsages: OperationQueryResult;
  schema: Schema;

  schema$: Observable<Schema>;

  constructor(private typeService: TypesService, private activeRoute: ActivatedRoute) {
    this.schema$ = typeService.getTypes();
    typeService.getTypes()
      .subscribe(schema => this.schema = schema);
  }

  private typeName: string;
  type: Type;
  inheritenceView: Inheritable;

  @Input()
  showAttributes = true;

  @Input()
  showTags = true;

  @Input()
  showDocumentation = true;

  @Input()
  showUsages = true;

  @Input()
  showInheritanceGraph = true;

  @Input()
  showTaxi = true;

  ngOnInit() {
    this.activeRoute.paramMap.pipe(
      map((params: ParamMap) => params.get('typeName'))
    ).subscribe(typeName => {
      this.typeName = typeName;
      this.typeService
        .getType(this.typeName)
        .pipe(filter(x => x !== undefined))
        .subscribe(type => {
          this.type = type;
          this.typeService.getTypes().subscribe(schema => {
            this.inheritenceView = buildInheritable(this.type, schema);
          });
        });

      this.typeService
        .getTypeUsages(this.typeName)
        .subscribe(usages => {
          this.typeUsages = usages;
        });
    });
  }
}

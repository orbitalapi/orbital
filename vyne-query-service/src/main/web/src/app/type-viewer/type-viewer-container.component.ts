import { Component, Input, OnInit } from '@angular/core';
import { OperationQueryResult, TypesService } from '../services/types.service';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { filter, map } from 'rxjs/operators';
import { Schema, Type } from '../services/schema';
import { buildInheritable, Inheritable } from '../inheritence-graph/inheritance-graph.component';
import { combineLatest, Observable } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ChangesetService } from 'src/app/services/changeset.service';

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
                     [inheritanceView]="inheritanceView"
                     [typeUsages]="typeUsages"
                     [showAttributes]="showAttributes"
                     [showDocumentation]="showDocumentation"
                     [editable]="true"
                     [showTags]="showTags"
                     [showTaxi]="showTaxi"
                     [showUsages]="showUsages"
                     [showInheritanceGraph]="showInheritanceGraph"
    ></app-type-viewer>`,
})
export class TypeViewerContainerComponent implements OnInit {
  typeUsages: OperationQueryResult;
  schema: Schema;

  schema$: Observable<Schema>;

  constructor(
    private typeService: TypesService,
    private activeRoute: ActivatedRoute,
    private changesetService: ChangesetService,
    private snackBar: MatSnackBar,
  ) {
    this.schema$ = typeService.getTypes();
    typeService.getTypes()
      .subscribe(schema => this.schema = schema);
  }

  private typeName: string;
  type: Type;
  inheritanceView: Inheritable;

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
    const typeName$ = this.activeRoute.paramMap.pipe(
      map((params: ParamMap) => params.get('typeName')),
    );
    combineLatest([typeName$, this.schema$]).subscribe(([typeName]) => {
      this.typeName = typeName;
      this.typeService
        .getType(this.typeName)
        .pipe(filter(x => x !== undefined))
        .subscribe(type => {
          this.type = type;
          this.typeService.getTypes().subscribe(schema => {
            this.inheritanceView = buildInheritable(this.type, schema);
          });
        });

      this.typeService
        .getTypeUsages(this.typeName)
        .subscribe(usages => {
          this.typeUsages = usages;
        });
    });
  }

  /** From Merge conflict:
   * hasUncommittedChanges():Observable<boolean> {
   *     return this.changesetService.activeChangeset$.pipe(
   *       map(changeset => changeset.name !== mainBranchName)
   *     )
   *   }
   *
   *   commitChanges() {
   *     this.changesetService.finalizeChangeset().subscribe((response) => {
   *       this.snackBar.open('Changes pushed', 'Dismiss', { duration: 10000 });
   *       if (response.link !== null) {
   *         const didTabOpenSuccessfully = window.open(response.link, '_blank');
   *         if (didTabOpenSuccessfully !== null) {
   *           didTabOpenSuccessfully.focus();
   *         } else {
   *           this.snackBar.open(
   *             'Failed to open the PR. You can find it on this link: ' + response.link,
   *             'Dismiss',
   *             { duration: 30000 },
   *           );
   *         }
   *       }
   *     });
   *   }
   */
}

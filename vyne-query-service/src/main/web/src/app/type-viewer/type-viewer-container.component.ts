import {Component, OnInit} from '@angular/core';
import {TypesService} from '../services/types.service';
import {ActivatedRoute, ParamMap} from '@angular/router';
import {filter, map} from 'rxjs/operators';
import {Schema, Type} from '../services/schema';
import {buildInheritable, Inheritable} from '../inheritence-graph/inheritance-graph.component';
@Component({
  selector: 'app-type-viewer-container',
  template: `
    <app-type-viewer [type]="type" [inheritanceView]="inheritenceView"></app-type-viewer>`
})
export class TypeViewerContainerComponent implements OnInit {
  constructor(private typeService: TypesService, private activeRoute: ActivatedRoute) {
  }
  private typeName: string;
  type: Type;
  inheritenceView: Inheritable;
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
    });
  }
}

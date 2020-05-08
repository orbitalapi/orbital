import {Component, OnInit} from '@angular/core';
import {TypesService} from '../services/types.service';
import {ActivatedRoute, ParamMap} from '@angular/router';
import {map} from 'rxjs/operators';
import {Type} from '../services/schema';

@Component({
  selector: 'app-type-viewer-container',
  template: `
    <app-type-viewer [type]="type"></app-type-viewer>`
})
export class TypeViewerContainerComponent implements OnInit {

  constructor(private typeService: TypesService, private activeRoute: ActivatedRoute) {
  }

  private typeName: string;
  type: Type;

  ngOnInit() {
    this.activeRoute.paramMap.pipe(
      map((params: ParamMap) => params.get('typeName'))
    ).subscribe(typeName => {
      this.typeName = typeName;
      this.typeService.getType(this.typeName).subscribe(type => this.type = type);
    });
  }
}

import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { map } from 'rxjs/operators';

import { Type } from '../services/schema';
import { TypesService } from '../services/types.service';

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
  description = 'A person who buys coffee, hopefully lots of it, and collects points like gollum collects shiney rings.  Filth';

  ngOnInit() {
    this.activeRoute.paramMap.pipe(
      map((params: ParamMap) => params.get('typeName'))
    ).subscribe(typeName => {
      this.typeName = typeName;
      this.typeService.getType(this.typeName).subscribe(type => this.type = type);
    });
  }
}

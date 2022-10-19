import { Component, OnInit } from '@angular/core';
import { TypesService } from 'src/app/services/types.service';
import { Observable } from 'rxjs/internal/Observable';
import { Schema } from 'src/app/services/schema';

@Component({
  selector: 'app-orbital-landing-page',
  templateUrl: './orbital-landing-page.component.html',
  styleUrls: ['./orbital-landing-page.component.scss']
})
export class OrbitalLandingPageComponent {
  schema$: Observable<Schema>;

  constructor(private typeService: TypesService) {
    this.schema$ = typeService.getTypes()
  }

}

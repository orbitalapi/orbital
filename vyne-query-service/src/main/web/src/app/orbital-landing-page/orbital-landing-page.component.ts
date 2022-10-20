import { Component, OnInit } from '@angular/core';
import { TypesService } from 'src/app/services/types.service';
import { Observable } from 'rxjs/internal/Observable';
import { Schema } from 'src/app/services/schema';

@Component({
  selector: 'app-orbital-landing-page',
  styleUrls: ['./orbital-landing-page.component.scss'],
  template: `
    <div class="w-full h-full bg-white p-4 flex flex-col">
      <div class="w-full flex">
        <span class="h3 flex-none">Your services</span>
        <span class="flex-1"></span>
        <button class="flex-none">Import schema</button>
      </div>
      <app-schema-diagram
        class="flex-grow-1 w-full h-full"
        [schema$]="schema$" displayedMembers="services"></app-schema-diagram>
    </div>
  `
})
export class OrbitalLandingPageComponent {
  schema$: Observable<Schema>;

  constructor(private typeService: TypesService) {
    this.schema$ = typeService.getTypes()
  }

}

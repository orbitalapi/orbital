import {Component, Input} from '@angular/core';
import {SchemaMember} from "../services/schema";

@Component({
  selector: 'app-catalog-entry-detail',
  template: `
    <p>
      catalog-entry-detail works!
    </p>
  `,
  styleUrls: ['./catalog-entry-detail.component.scss']
})
export class CatalogEntryDetailComponent {

  @Input()
  member: SchemaMember


}


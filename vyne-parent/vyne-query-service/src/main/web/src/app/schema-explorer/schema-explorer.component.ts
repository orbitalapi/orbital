import { Component, OnInit } from '@angular/core';
import {TypesService, VersionedSchema} from "../services/types.service";
import {Observable} from "rxjs/internal/Observable";

@Component({
  selector: 'app-schema-explorer',
  templateUrl: './schema-explorer.component.html',
  styleUrls: ['./schema-explorer.component.scss']
})
export class SchemaExplorerComponent implements OnInit {


  schemas: Observable<VersionedSchema[]>;

  constructor(private service:TypesService) { }

  ngOnInit() {
    this.schemas = this.service.getVersionedSchemas()

  }

}

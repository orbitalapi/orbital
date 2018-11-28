import { Component, OnInit } from '@angular/core';
import {TypesService, VersionedSchema} from "../services/types.service";
import {Observable} from "rxjs/internal/Observable";
import {Router} from "@angular/router";

@Component({
  selector: 'app-schema-explorer',
  templateUrl: './schema-explorer.component.html',
  styleUrls: ['./schema-explorer.component.scss']
})
export class SchemaExplorerComponent implements OnInit {


  schemas: Observable<VersionedSchema[]>;

  selectedSchema:VersionedSchema;

  constructor(private service:TypesService, private router:Router) { }

  ngOnInit() {
    this.schemas = this.service.getVersionedSchemas()
  }

  importSchemaFromUrl() {
    this.router.navigate(["schema-explorer","import"])
  }
  createNewSchema() {

  }

  selectSchema(schema: VersionedSchema) {
    this.selectedSchema = schema;
  }
}

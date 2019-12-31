import {Component, OnInit} from '@angular/core';
import {TypesService} from '../services/types.service';
import {Observable} from 'rxjs/internal/Observable';
import {Router} from '@angular/router';
import {AppInfoService, QueryServiceConfig} from '../services/app-info.service';
import {SourceCode, VersionedSchema} from '../services/schema';
import {map} from 'rxjs/operators';

@Component({
  selector: 'app-schema-explorer',
  templateUrl: './schema-explorer.component.html',
  styleUrls: ['./schema-explorer.component.scss']
})
export class SchemaExplorerComponent implements OnInit {


  schemas: Observable<SourceCode[]>;

  selectedSchema: VersionedSchema;
  config: QueryServiceConfig;

  constructor(private service: TypesService, private configService: AppInfoService, private router: Router) {
  }

  ngOnInit() {
    this.schemas = this.service.getVersionedSchemas()
      .pipe(map(schemas => {
        return schemas.map(schema => {
            return {
              origin: schema.name,
              language: 'taxi',
              content: schema.content
            } as SourceCode;
          }
        );
      }));

    this.configService.getConfig().subscribe(result => this.config = result);
  }

  importSchemaFromUrl() {
    this.router.navigate(['schema-explorer', 'import']);
  }

  selectSchema(schema: VersionedSchema) {
    this.selectedSchema = schema;
  }
}

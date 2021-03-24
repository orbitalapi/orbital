import {Component, OnInit} from '@angular/core';
import {TypesService} from '../services/types.service';
import {Observable} from 'rxjs/internal/Observable';
import {Router} from '@angular/router';
import {AppInfoService, QueryServiceConfig} from '../services/app-info.service';
import {ParsedSource, SourceCode, VersionedSource} from '../services/schema';
import {map} from 'rxjs/operators';
import {SchemaNotificationService} from '../services/schema-notification.service';

@Component({
  selector: 'app-schema-explorer',
  templateUrl: './schema-explorer.component.html',
  styleUrls: ['./schema-explorer.component.scss']
})
export class SchemaExplorerComponent implements OnInit {


  schemas: Observable<ParsedSource[]>;

  selectedSchema: VersionedSource;
  config: QueryServiceConfig;

  constructor(private service: TypesService,
              private configService: AppInfoService,
              private router: Router,
              private schemaNotificationService: SchemaNotificationService) {
  }

  ngOnInit() {
    this.loadSchemas();
    //this.schemaNotificationService.createSchemaNotificationsSubscription()
    //  .subscribe(() => {
    //    this.loadSchemas();
    //  });
    this.configService.getConfig().subscribe(result => this.config = result);
  }

  private loadSchemas() {
    this.schemas = this.service.getParsedSources();
  }

  importSchemaFromUrl() {
    this.router.navigate(['schema-explorer', 'import']);
  }

  selectSchema(schema: VersionedSource) {
    this.selectedSchema = schema;
  }
}

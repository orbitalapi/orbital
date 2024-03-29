import {Inject, Injectable, Injector} from '@angular/core';
import {Environment, ENVIRONMENT} from 'src/app/services/environment';
import {TuiDialogService} from '@taiga-ui/core';
import {HttpClient} from '@angular/common/http';
import {UpdateDataOwnerRequest} from 'src/app/services/types.service';
import {Metadata, PackageSourceName, QualifiedName, Type, VersionedSource} from 'src/app/services/schema';
import {VyneUser} from 'src/app/services/user-info.service';
import {Observable} from 'rxjs';
import {switchMap} from 'rxjs/operators';
import {ChangesetService} from 'src/app/changeset-selector/changeset.service';

@Injectable({
  providedIn: 'root',
})
export class TypeEditorService {
  constructor(
    @Inject(ENVIRONMENT) private environment: Environment,
    @Inject(TuiDialogService) private readonly dialogService: TuiDialogService,
    @Inject(Injector) private readonly injector: Injector,
    private http: HttpClient,
    private changeSetService: ChangesetService
  ) {
  }


  setTypeDataOwner(type: Type, owner: VyneUser): Observable<Type> {
    return this.changeSetService.ensureChangesetExists()
      .pipe(
        switchMap(changeset => this.http.post<Type>(`${this.environment.serverUrl}/api/types/${type.name.fullyQualifiedName}/owner`,
          {
            id: owner.userId,
            name: owner.name,
            changeset: changeset
          } as UpdateDataOwnerRequest,
        )),
        // tap(() => this.updateChangesets()),
      );
  }

  setTypeMetadata(type: Type, metadata: Metadata[]): Observable<Type> {
    return this.changeSetService.ensureChangesetExists()
      .pipe(
        switchMap(changeset => this.http.post<Type>(`${this.environment.serverUrl}/api/types/${type.name.fullyQualifiedName}/annotations`,
          {
            changeset: changeset,
            annotations: metadata,
          },
        )),
        // tap(() => this.updateChangesets()),
      );
  }

  saveQuery(request: SaveQueryRequest): Observable<SavedQuery> {
    return this.http.post<SavedQuery>(`${this.environment.serverUrl}/api/repository/queries`, request)
  }


}

export interface SaveQueryRequest {
  source: VersionedSource;
  changesetName: string;
  previousPath?: PackageSourceName | null;
}

export interface SavedQuery {
  name: QualifiedName;
  sources: VersionedSource[];
  url: string | null;
}

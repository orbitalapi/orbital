import { Inject, Injectable, Injector } from '@angular/core';
import { Environment, ENVIRONMENT } from 'src/app/services/environment';
import { TuiDialogService } from '@taiga-ui/core';
import { PackagesService } from 'src/app/package-viewer/packages.service';
import { HttpClient } from '@angular/common/http';
import { TypesService, UpdateDataOwnerRequest } from 'src/app/services/types.service';
import { Metadata, Type } from 'src/app/services/schema';
import { VyneUser } from 'src/app/services/user-info.service';
import { Observable } from 'rxjs';
import { switchMap, tap } from 'rxjs/operators';
import { ChangesetService } from 'src/app/services/changeset.service';

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

}

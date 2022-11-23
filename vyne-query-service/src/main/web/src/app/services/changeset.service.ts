import { Inject, Injectable, Injector } from '@angular/core';
import { Environment, ENVIRONMENT } from 'src/app/services/environment';
import { TuiDialogService } from '@taiga-ui/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, merge, of } from 'rxjs';
import { PackageIdentifier, PackagesService, SourcePackageDescription } from 'src/app/package-viewer/packages.service';
import { filter, map, mapTo, share, shareReplay, switchMap, take, tap } from 'rxjs/operators';
import { Observable } from 'rxjs/internal/Observable';
import { QualifiedName, VersionedSource } from 'src/app/services/schema';
import { PolymorpheusComponent } from '@tinkoff/ng-polymorpheus';
import {
  ChangesetNameDialogComponent,
  ChangesetNameDialogSaveHandler
} from 'src/app/changeset-name-dialog/changeset-name-dialog.component';

export const defaultChangesetName = 'main';

@Injectable({
  providedIn: 'root',
})
export class ChangesetService {
  editablePackage$: Observable<SourcePackageDescription | null>;
  availableChangesets$: Observable<Changeset[]>
  activeChangeset$: Observable<Changeset>

  private activeChangesetServerUpdate$: Observable<Changeset>;
  private activeChangesetLocalUpdates$ = new BehaviorSubject<Changeset>(null);

  constructor(
    @Inject(ENVIRONMENT) private environment: Environment,
    @Inject(TuiDialogService) private readonly dialogService: TuiDialogService,
    @Inject(Injector) private readonly injector: Injector,
    private packageService: PackagesService,
    private http: HttpClient
  ) {

    // ... check for the current list of packages....
    this.editablePackage$ = packageService.listPackages()
      .pipe(
        // ... and find the one that's editable ...
        map((packages: SourcePackageDescription[]) => {
          const editable = packages.filter(sourcePackage => sourcePackage.editable)
          if (editable.length === 0) {
            console.error('There are no editable packages configured - editing will fail')
            return null;
          } else if (editable.length > 1) {
            console.error('There are multiple editable packages configured - editing will fail')
            return null;
          }
          return editable[0];
        }),
        shareReplay(1)
      )

    // All known changesets.  Triggered off the list of all editable packages
    this.availableChangesets$ = this.editablePackage$
      .pipe(
        switchMap(packageDescription => {
          return this.getAvailableChangesets(packageDescription.identifier);
        }),
        shareReplay(1)
      )

    this.activeChangesetServerUpdate$ = this.availableChangesets$
      .pipe(
        map(changesets => {
          return changesets.find(c => c.isActive);
        }),
        filter(changeset => changeset !== null && changeset !== undefined)
      )

    this.activeChangeset$ = merge(this.activeChangesetServerUpdate$, this.activeChangesetLocalUpdates$)
      .pipe(
        filter(changeset => changeset !== null && changeset !== undefined)
      )
  }


  getAvailableChangesets(packageIdentifier: PackageIdentifier): Observable<Changeset[]> {
    return this.http.post<ChangesetResponse>(`${this.environment.serverUrl}/api/repository/changesets`, { packageIdentifier: packageIdentifier })
      .pipe(map(response => {
        return response.changesets
      }));
  }


  setActiveChangeset(changeset: Changeset): Observable<SetActiveChangesetResponse> {
    return this.http.post<SetActiveChangesetResponse>(`${this.environment.serverUrl}/api/repository/changesets/active`, {
      packageIdentifier: changeset.packageIdentifier,
      changesetName: changeset.name,
    }).pipe(tap(() => this.activeChangesetLocalUpdates$.next({
      name: changeset.name,
      isActive: true,
      packageIdentifier: changeset.packageIdentifier
    })));
  }

  createChangeset(name: string, packageIdentifier: PackageIdentifier): Observable<Changeset> { // TODO Typing
    const sanitizedName = this.sanitizeChangesetName(name);
    return this.http.post<CreateChangesetResponse>(
      `${this.environment.serverUrl}/api/repository/changeset/create`,
      { changesetName: sanitizedName, packageIdentifier: packageIdentifier },
    ).pipe(
      map(response => response.changeset),
      tap((changeset) => {
        this.activeChangesetLocalUpdates$.next(changeset)
      }));
  }

  addChangesToChangeset(typeName: QualifiedName, schemaNameSuffix: string, schemaText: string): Observable<VersionedSource> {
    return this.ensureChangesetExists()
      .pipe(
        switchMap(changeset => {
          const request: AddChangesToChangesetRequest = {
            edits: [
              {
                name: `${typeName.fullyQualifiedName}.${schemaNameSuffix}.taxi`,
                content: schemaText,
                version: 'next-minor',
              },
            ],
            packageIdentifier: changeset.packageIdentifier,
            changesetName: changeset.name,
          };
          return this.http.post<VersionedSource>(
            `${this.environment.serverUrl}/api/repository/changeset/add`,
            request,
          );

        }));
  }

  finalizeChangeset(): Observable<FinalizeChangesetResponse> {
    return this.activeChangeset$
      .pipe(
        take(1),
        switchMap(changeset => {
          const body: FinalizeChangesetRequest = {
            changesetName: changeset.name,
            packageIdentifier: changeset.packageIdentifier
          };
          return this.http.post<FinalizeChangesetResponse>(
            `${this.environment.serverUrl}/api/repository/changeset/finalize`,
            body,
          );
        }),
        tap(response => this.activeChangesetLocalUpdates$.next({
          name: 'main',
          isActive: true,
          packageIdentifier: response.changeset.packageIdentifier
        })),
      );
  }


  ensureChangesetExists(): Observable<Changeset> {
    return this.openNameDialog(false, (name, packageIdentifier) => this.createChangeset(name, packageIdentifier));
  }

  sanitizeChangesetName(name: string): string {
    // @ts-ignore
    return name.replaceAll(' ', '-').replace(/\W/g, '').toLowerCase();
  }


  openNameDialog(forceOpen: boolean, saveHandler: ChangesetNameDialogSaveHandler): Observable<Changeset> {
    return this.activeChangeset$.pipe(
      take(1),
      switchMap(changeset => {
        if (!forceOpen && changeset.name !== defaultChangesetName) {
          return of(changeset);
        } else {
          return this.dialogService
            .open<Changeset>(new PolymorpheusComponent(ChangesetNameDialogComponent, this.injector), {
              data: {
                changeset,
                saveHandler: saveHandler,
              },
            });
        }
      })
    )


  }


  isCustomChangesetSelected(): Observable<boolean> {
    return this.activeChangeset$.pipe(
      take(1),
      map(changeset => changeset.name !== defaultChangesetName)
    )
  }

  rename(): Observable<void> {
    return this.openNameDialog(true, changesetName => this.updateChangeset(changesetName)).pipe(mapTo(void 0));
  }

  private updateChangeset(changesetName: string): Observable<Changeset> {
    return this.activeChangeset$.pipe(
      take(1),
      switchMap(changeset => {
        return this.http.put<Changeset>(`${this.environment.serverUrl}/api/repository/changeset/update`, {
          packageIdentifier: changeset.packageIdentifier,
          changesetName: changeset.name,
          newChangesetName: changesetName,
        })
      }),
      tap((changeset) => this.activeChangesetLocalUpdates$.next(changeset)),
    );
  }


  selectDefaultChangeset() {
    this.availableChangesets$
      .pipe(
        take(1),
        switchMap(changesets => {
          const defaultChangeset = changesets.find(c => c.name === defaultChangesetName)
          return this.setActiveChangeset(defaultChangeset)
        })
      ).subscribe()
  }

}

export interface ChangesetUpdateResponse {
  changeset: Changeset;
}

export interface SetActiveChangesetResponse extends ChangesetUpdateResponse {
}

export interface CreateChangesetResponse extends ChangesetUpdateResponse {
}

// TODO Make a separate type for the Changeset the consumers care about which does not include e.g. isActive
export interface Changeset {
  name: string;
  isActive: boolean;
  packageIdentifier: PackageIdentifier;
}

export interface ChangesetResponse {
  changesets: Changeset[];
}

export interface AddChangesToChangesetRequest {
  changesetName: string;
  packageIdentifier: any;
  edits: VersionedSource[];
}

export interface FinalizeChangesetRequest {
  changesetName: string;
  packageIdentifier: any;
}

export interface FinalizeChangesetResponse {
  link: string | null;
  changeset: Changeset;
}

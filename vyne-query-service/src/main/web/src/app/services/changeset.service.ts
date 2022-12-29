import { Inject, Injectable, Injector } from '@angular/core';
import { Environment, ENVIRONMENT } from 'src/app/services/environment';
import { TuiDialogService } from '@taiga-ui/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, of } from 'rxjs';
import { PackageIdentifier, PackagesService, SourcePackageDescription } from 'src/app/package-viewer/packages.service';
import { filter, map, mapTo, shareReplay, switchMap, take, tap } from 'rxjs/operators';
import { Observable } from 'rxjs/internal/Observable';
import { QualifiedName, VersionedSource } from 'src/app/services/schema';
import { PolymorpheusComponent } from '@tinkoff/ng-polymorpheus';
import {
  ChangesetNameDialogComponent,
  ChangesetNameDialogSaveHandler,
} from 'src/app/changeset-name-dialog/changeset-name-dialog.component';

interface UpdateChangesetResponse {
  changeset: Changeset;
}

export interface ChangesetUpdateResponse {
  changeset: Changeset;
  changesetOverview: ChangesetOverview;
}

export interface SetActiveChangesetResponse extends ChangesetUpdateResponse {
  changesetOverview: ChangesetOverview;
}

export interface AddChangesToChangesetResponse extends ChangesetUpdateResponse {
}

export interface CreateChangesetResponse extends ChangesetUpdateResponse {
}

// TODO Make a separate type for the Changeset the consumers care about which does not include e.g. isActive
export interface Changeset {
  name: string;
  isActive: boolean;

  isDefault: boolean;
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

export interface ChangesetOverview {
  additions: number;
  changedFiles: number;
  deletions: number;
  author: string;
  description: string;
  lastUpdated: string;
}

export interface FinalizeChangesetResponse {
  changesetOverview: ChangesetOverview;
  link: string | null;
  changeset: Changeset;
}

function resolveEditablePackage(packages: SourcePackageDescription[]): any {
  const editable = packages.filter(sourcePackage => sourcePackage.editable);
  if (editable.length === 0) {
    console.error('There are no editable packages configured - editing will fail');
    return null;
  } else if (editable.length > 1) {
    console.error('There are multiple editable packages configured - editing will fail');
    return null;
  }
  return editable[0];
}


@Injectable({
  providedIn: 'root',
})
export class ChangesetService {
  editablePackage$: Observable<SourcePackageDescription | null> = this.packageService.listPackages()
    .pipe(
      map((packages: SourcePackageDescription[]) => resolveEditablePackage(packages)),
      shareReplay(1),
    );
  activeChangeset$ = new BehaviorSubject<Changeset | null>(null);
  availableChangesets$ = new BehaviorSubject<Changeset[]>([]);

  activeChangesetOverview: ChangesetOverview | null = null;

  constructor(
    @Inject(ENVIRONMENT) private environment: Environment,
    @Inject(TuiDialogService) private readonly dialogService: TuiDialogService,
    @Inject(Injector) private readonly injector: Injector,
    private packageService: PackagesService,
    private http: HttpClient,
  ) {
    this.updateChangesets();
  }

  updateChangesets(): void {
    this.editablePackage$
      .pipe(
        filter(packageDescription => packageDescription != null),
        take(1),
        switchMap(packageDescription => this.getAvailableChangesets(packageDescription.identifier)),
        tap(changesets => this.availableChangesets$.next(changesets)),
        tap(changesets => this.activeChangeset$.next(changesets.find(changeset => changeset.isActive) || null)),
      ).subscribe();
  }

  getAvailableChangesets(packageIdentifier: PackageIdentifier): Observable<Changeset[]> {
    return this.http.post<ChangesetResponse>(`${this.environment.serverUrl}/api/repository/changesets`, { packageIdentifier: packageIdentifier })
      .pipe(map(response => {
        return response.changesets;
      }));
  }


  setActiveChangeset(changeset: Changeset): Observable<SetActiveChangesetResponse> {
    return this.http.post<SetActiveChangesetResponse>(`${this.environment.serverUrl}/api/repository/changesets/active`, {
      packageIdentifier: changeset.packageIdentifier,
      changesetName: changeset.name,
    }).pipe(
      tap(() => this.updateChangesets()),
      tap(response => this.activeChangesetOverview = response.changesetOverview),
    );
  }

  createChangeset(name: string, packageIdentifier: PackageIdentifier): Observable<Changeset> {
    const sanitizedName = this.sanitizeChangesetName(name);
    return this.http.post<CreateChangesetResponse>(
      `${this.environment.serverUrl}/api/repository/changeset/create`,
      { changesetName: sanitizedName, packageIdentifier: packageIdentifier },
    ).pipe(
      map(response => response.changeset),
      switchMap(response => this.setActiveChangeset(response).pipe(map(response => response.changeset))),
      tap(() => this.updateChangesets()));
  }

  addChangesToChangeset(typeName: QualifiedName, schemaNameSuffix: string, schemaText: string): Observable<AddChangesToChangesetResponse> {
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
          return this.http.post<AddChangesToChangesetResponse>(
            `${this.environment.serverUrl}/api/repository/changeset/add`,
            request,
          );
        }),
        tap(response => this.activeChangesetOverview = response.changesetOverview),
      );
  }

  finalizeChangeset(): Observable<FinalizeChangesetResponse> {
    const body: FinalizeChangesetRequest = {
      changesetName: this.activeChangeset$.value.name,
      packageIdentifier: this.activeChangeset$.value.packageIdentifier,
    };
    return this.http.post<FinalizeChangesetResponse>(
      `${this.environment.serverUrl}/api/repository/changeset/finalize`,
      body,
    ).pipe(
      tap(() => this.updateChangesets()),
      tap(response => this.activeChangesetOverview = response.changesetOverview),
    );
  }


  ensureChangesetExists(): Observable<Changeset> {
    return this.openNameDialog(false, (name, packageIdentifier) => this.createChangeset(name, packageIdentifier));
  }

  sanitizeChangesetName(name: string): string {
    return name.replaceAll(' ', '-').replace(/\W/g, '').toLowerCase();
  }


  openNameDialog(forceOpen: boolean, saveHandler: ChangesetNameDialogSaveHandler): Observable<Changeset> {
    if (!forceOpen && !this.activeChangeset$.value.isDefault) {
      return of(this.activeChangeset$.value);
    } else {
      return this.dialogService
        .open<Changeset>(new PolymorpheusComponent(ChangesetNameDialogComponent, this.injector), {
          data: {
            changeset: this.activeChangeset$.value,
            saveHandler: saveHandler,
          },
        });
    }
  }


  isCustomChangesetSelected(): boolean {
    return this.activeChangeset$.value !== null && !this.activeChangeset$.value.isDefault;
  }

  rename(): Observable<void> {
    return this.openNameDialog(true, changesetName => this.updateChangeset(changesetName)).pipe(mapTo(void 0));
  }

  selectDefaultChangeset(): Observable<string> {
    const defaultChangeset = this.availableChangesets$.value.find(changeset => changeset.isDefault);
    return this.setActiveChangeset(defaultChangeset).pipe(map(changeset => changeset.changeset.name));
  }

  private updateChangeset(changesetName: string): Observable<Changeset> {
    return this.http.put<UpdateChangesetResponse>(`${this.environment.serverUrl}/api/repository/changeset/update`, {
      ...this.activeChangeset$.value,
      newChangesetName: changesetName,
    }).pipe(
      map(response => response.changeset),
      tap(() => this.updateChangesets()),
    );
  }
}

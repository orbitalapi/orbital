import {ChangeDetectionStrategy, Component, EventEmitter, Inject, Injector, Input, Output} from '@angular/core';
import {Observable} from 'rxjs/internal/Observable';
import {AuthScheme, AuthTokenMap, NoCredentialsAuthToken} from './auth-manager.service';
import {of} from "rxjs";
import {TuiDialogService} from "@taiga-ui/core";
import {PolymorpheusComponent} from "@tinkoff/ng-polymorpheus";
import {AddTokenPanelComponent} from "./add-token-panel.component";
import {map} from "rxjs/operators";

@Component({
  selector: 'app-token-list',
  template: `
    <app-header-component-layout

      title="Authentication Tokens"
      description="These tokens will be used to authenticate Orbital to services.">
      <ng-container ngProjectAs="buttons">
        <button tuiButton size="m" appearance="outline" (click)="showCreateTokenPopup()">Add a token
        </button>
      </ng-container>
      <div *ngIf="(tokenListSize$ | async) > 0; else empty">
        <table class="token-list">
          <thead>
          <tr>
            <th>Services</th>
            <th>Token type</th>
            <th>Details</th>
          </tr>
          </thead>
          <tbody>
          <tr *ngFor="let authScheme of (_tokens$ | async) | keyvalue">
            <td>{{ authScheme.key }}</td>
            <td>{{ authScheme.value.type }}</td>
            <td>
              <table class="nested-table">
                <tr *ngFor="let configParam of configParams(authScheme.value) | keyvalue">
                  <td class="label-col">{{configParam.key}}</td>
                  <td>{{configParam.value}}</td>

                </tr>
              </table>
            </td>
            <!--                      <td>-->
            <!--                          <button tuiButton size="s" appearance="outline" (click)="editToken(token)">Edit</button>-->
            <!--                          <button tuiButton size="s" appearance="outline" (click)="onDeleteTokenClicked(token)">Delete-->
            <!--                          </button>-->
            <!--                      </td>-->
          </tr>
          </tbody>
        </table>
      </div>
    </app-header-component-layout>
    <ng-template #empty>
      <div class="empty-state-container">
        <img src="assets/img/illustrations/authentication.svg">
        <p>These tokens will be used to authenticate Orbital to services.
        </p>
        <button tuiButton size="l" appearance="primary" (click)="showCreateTokenPopup()">Add a token</button>
      </div>
    </ng-template>
  `,
  styleUrls: ['./token-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TokenListComponent {

  private _tokens$: Observable<AuthTokenMap> = of({})
  @Input()
  get tokens$(): Observable<AuthTokenMap> {
    return this._tokens$;
  }

  set tokens$(value: Observable<AuthTokenMap>) {
    this._tokens$ = value;
    this.tokenListSize$ = this.tokens$
      .pipe(
        map(tokens => Object.keys(tokens).length)
      )
  }

  tokenListSize$: Observable<number> = of(0)

  @Output()
  newTokenSaved = new EventEmitter<NoCredentialsAuthToken>();

  @Output()
  deleteToken = new EventEmitter<NoCredentialsAuthToken>();

  constructor(
    @Inject(Injector) private readonly injector: Injector,
    @Inject(TuiDialogService) private readonly dialogService: TuiDialogService,
  ) {
  }



  showCreateTokenPopup(): void {
    this.dialogService.open(
      new PolymorpheusComponent(AddTokenPanelComponent, this.injector)
    ).subscribe()
    // this.dialogService.open(NewTokenPanelComponent,
    //   {
    //     width: '1200px',
    //     maxWidth: '80vw'
    //   }
    // )
    //   .afterClosed().subscribe(createdToken => {
    //   if (createdToken) {
    //     this.newTokenSaved.emit(createdToken);
    //   }
    // });
  }

  // onDeleteTokenClicked(token: NoCredentialsAuthToken): void {
  //   this.dialogService.open(
  //     ConfirmationDialogComponent,
  //     {
  //       data: new ConfirmationParams(
  //         'Delete token?',
  //         `This will remove the token for service ${token.serviceName}.  This action cannot be undone.`
  //       )
  //     }
  //   ).afterClosed().subscribe((result: ConfirmationAction) => {
  //     if (result === 'OK') {
  //       this.deleteToken.emit(token);
  //     }
  //   });
  // }

  // editToken(token: NoCredentialsAuthToken): void {
  //   this.dialogService.open(NewTokenPanelComponent, {
  //     data: token,
  //     width: '1200px',
  //     maxWidth: '80vw'
  //   })
  //     .afterClosed().subscribe(createdToken => {
  //     if (createdToken) {
  //       this.newTokenSaved.emit(createdToken);
  //     }
  //   });
  // }

  configParams(authScheme: AuthScheme): any {
    const {type, ...authSchemeWithoutType} = authScheme;
    return authSchemeWithoutType
    // return Object.entries(authScheme)
    //   .filter(entry => {
    //     const [key,value] = entry;
    //     return key !== 'type';
    //   })
  }
}

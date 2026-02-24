import {TuiLet} from '@taiga-ui/cdk';
import { TuiBadge } from "@taiga-ui/kit";
import {ChangeDetectionStrategy, ChangeDetectorRef, Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {Observable, of, switchMap} from 'rxjs';
import {catchError, tap} from 'rxjs/operators';
import {UiCustomisations} from '../../environments/ui-customisations';
import {HeaderComponentLayoutComponent} from '../header-component-layout/header-component-layout.component';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import { TuiButton, TuiHint } from '@taiga-ui/core';
import {PoliciesService, PolicySetupReadiness} from '../services/policies.service';
import {SchemaNotificationService} from '../services/schema-notification.service';
import {Policy, TypesService} from '../services/types.service';
import {FilenameDisplayComponent} from "../filename-display/filename-display.component";
import {SchemaMemberNameBadgeComponent} from "../schema-member-name-badge/schema-member-name-badge.component";

@Component({
  selector: 'app-policy-manager',
  standalone: true,
  imports: [
    CommonModule,
    HeaderComponentLayoutComponent,
    TuiButton,
    RouterLink,
    TuiBadge,
    TuiHint,
    FilenameDisplayComponent,
    SchemaMemberNameBadgeComponent,
    TuiLet,
  ],
  template: `
    <app-header-component-layout
      title="Policies"
      [description]="'Policies define data access controls for the data that is served by ' + UiCustomisations.productName"
    >
      <ng-container ngProjectAs="buttons">
        <button *ngIf="primaryAction == 'getStarted'"
                tuiButton
                size="m"
                [routerLink]="'get-started'"
        >
          <img src="assets/img/tabler/lock-cog.svg" class="lock-icon filter-white">
          Get started
        </button>
        <button *ngIf="primaryAction == 'createPolicy'"
                tuiButton
                size="m"
                [routerLink]="'editor'"
        >
          Create a policy
        </button>
      </ng-container>
      <ng-container *tuiLet="policies$ | async as policies">
        <table *ngIf="policies?.length" class="policy-list">
          <thead>
          <tr>
            <th>Policy name</th>
            <th>File</th>
            <th>Policy defined against</th>
            <th>Read</th>
            <th>Write</th>
          </tr>
          </thead>
          <tbody>
          <tr *ngFor="let policy of policies" (click)="navigateToPolicyPage(policy)">
            <td>{{ policy.name.shortDisplayName }}</td>
            <td><app-filename-display [source]="policy.sourceFile"></app-filename-display></td>
            <td><app-schema-member-name-badge [member]="policy.targetType"></app-schema-member-name-badge></td>
            <td [tuiHint]="policy.definesReadPolicy ? 'Defines a read policy' : 'Does not define a read policy'" tuiHintAppearance="dark">
              <img src="assets/img/tabler/check.svg" class="policy-icon" [class.has-policy]="policy.definesReadPolicy">
            </td>
            <td [tuiHint]="policy.definesWritePolicy ? 'Defines a write policy' : 'Does not define a write policy'" tuiHintAppearance="dark">
              <img src="assets/img/tabler/check.svg" class="policy-icon" [class.has-policy]="policy.definesWritePolicy">
            </td>
          </tr>
          </tbody>
        </table>
        <div *ngIf="!policies?.length && !isLoading" class="empty-state-container">
          <img src="assets/img/illustrations/authentication.svg">
          <p>No policies have been created yet.</p>
        </div>
      </ng-container>
    </app-header-component-layout>
  `,
  styleUrls: ['./policy-manager.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PolicyManagerComponent {
  policies$: Observable<Policy[]>;
  setupReadiness: PolicySetupReadiness | null = null;
  isLoading: boolean;
  primaryAction: 'getStarted' | 'createPolicy'

  setPrimaryAction = () => {
    if (!this.setupReadiness) {
      this.primaryAction = null
    } else if (!this.setupReadiness.authTokenTypes.length) {
      this.primaryAction = 'getStarted'
    } else {
      this.primaryAction = 'createPolicy'
    }
  }

  protected readonly UiCustomisations = UiCustomisations;

  constructor(
    private typeService: TypesService,
    private policiesService: PoliciesService,
    private schemaNotificationService: SchemaNotificationService,
    private changeDetector: ChangeDetectorRef,
    private router: Router,
    private activeRoute: ActivatedRoute,
  ) {
    this.policies$ = this.schemaNotificationService.createSchemaNotificationsSubscription()
      .pipe(
        takeUntilDestroyed(),
        tap(() => {
          this.isLoading = true;
          this.setPrimaryAction();
        }),
        switchMap(() =>
          this.policiesService.getPolicySetupReadiness().pipe(
            tap(value => {
              this.setupReadiness = value; // Update setup readiness
              this.changeDetector.markForCheck();
            }),
            switchMap(() =>
              this.typeService.getPolicies().pipe(
                tap(() => {
                  this.isLoading = false;
                  this.setPrimaryAction();
                })
              )
            )
          )
        ),
        catchError(error => {
          this.isLoading = false;
          console.error(error);
          return of([]); // Return an empty observable on error
        })
      );

  }

  navigateToPolicyPage(policy: Policy) {
    this.router.navigate(['editor', policy.name.parameterizedName], {relativeTo: this.activeRoute})
  }
}

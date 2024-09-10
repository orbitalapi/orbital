import {ChangeDetectionStrategy, ChangeDetectorRef, Component} from '@angular/core';
import {CommonModule} from '@angular/common';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {TuiBadgeModule} from '@taiga-ui/kit';
import {Observable, switchMap} from 'rxjs';
import {UiCustomisations} from '../../environments/ui-customisations';
import {HeaderComponentLayoutModule} from '../header-component-layout/header-component-layout.module';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {TuiButtonModule, TuiHintModule} from '@taiga-ui/core';
import {PoliciesService, PolicySetupReadiness} from '../services/policies.service';
import {SchemaNotificationService} from '../services/schema-notification.service';
import {Policy, TypesService} from '../services/types.service';

@Component({
  selector: 'app-policy-manager',
  standalone: true,
  imports: [CommonModule, HeaderComponentLayoutModule, TuiButtonModule, RouterLink, TuiBadgeModule, TuiHintModule],
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
      <div *ngIf="policies$ | async as policies">
        <table *ngIf="policies.length" class="policy-list">
          <thead>
          <tr>
            <th>Policy name</th>
            <th>Policy defined against</th>
            <th>Read</th>
            <th>Write</th>
          </tr>
          </thead>
          <tbody>
          <tr *ngFor="let policy of policies" (click)="navigateToPolicyPage(policy)">
            <td>{{ policy.name.shortDisplayName }}</td>
            <td>{{ policy.targetType.shortDisplayName }}</td>
            <td [tuiHint]="policy.definesReadPolicy ? 'Defines a read policy' : 'Does not define a read policy'" tuiHintAppearance="onDark">
              <img src="assets/img/tabler/check.svg" class="policy-icon" [class.has-policy]="policy.definesReadPolicy">
            </td>
            <td [tuiHint]="policy.definesWritePolicy ? 'Defines a write policy' : 'Does not define a write policy'" tuiHintAppearance="onDark">
              <img src="assets/img/tabler/check.svg" class="policy-icon" [class.has-policy]="policy.definesWritePolicy">
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </app-header-component-layout>
  `,
  styleUrls: ['./policy-manager.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PolicyManagerComponent {
  policies$: Observable<Policy[]>;
  setupReadiness: PolicySetupReadiness | null = null;

  get primaryAction(): 'getStarted' | 'createPolicy' {
    if (!this.setupReadiness) {
      return null
    } else if (!this.setupReadiness.authTokenTypes.length) {
      return 'getStarted'
    } else {
      return 'createPolicy'
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
    this.policiesService.getPolicySetupReadiness().pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      this.setupReadiness = value;
      this.changeDetector.markForCheck();
    })

    this.policies$ = this.schemaNotificationService.createSchemaNotificationsSubscription()
      .pipe(
        switchMap(() => this.typeService.getPolicies()
        )
      );
  }

  navigateToPolicyPage(policy: Policy) {
    this.router.navigate(['editor', policy.name.parameterizedName], {relativeTo: this.activeRoute})
  }
}

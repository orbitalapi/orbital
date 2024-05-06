import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UiCustomisations } from '../../environments/ui-customisations';
import { HeaderComponentLayoutModule } from '../header-component-layout/header-component-layout.module';
import { RouterLink } from '@angular/router';
import { TuiButtonModule } from '@taiga-ui/core';
import { PoliciesService, PolicySetupReadiness } from '../services/policies.service';

@Component({
  selector: 'app-policy-manager',
  standalone: true,
  imports: [CommonModule, HeaderComponentLayoutModule, TuiButtonModule, RouterLink],
  template: `
    <app-header-component-layout
      title="Policies"
      [description]="'Policies define data access controls for the data that is served by ' + UiCustomisations.productName"
    >
      <ng-container ngProjectAs="buttons" *ngIf="!setupReadiness?.authTokenTypes.length">
        <button
          tuiButton
          size="m"
          [routerLink]="'get-started'"
        >
          <img src="assets/img/tabler/lock-cog.svg" class="lock-icon filter-white">
          Get started
        </button>
      </ng-container>
    </app-header-component-layout>
  `,
  styleUrls: ['./policy-manager.component.scss']
})
export class PolicyManagerComponent {
  setupReadiness: PolicySetupReadiness | null;

  protected readonly UiCustomisations = UiCustomisations;

  constructor(
    private policiesService: PoliciesService,
  ) {
    policiesService.getPolicySetupReadiness().pipe(
      takeUntilDestroyed()
    ).subscribe(value => {
      this.setupReadiness = value;
    })
  }
}

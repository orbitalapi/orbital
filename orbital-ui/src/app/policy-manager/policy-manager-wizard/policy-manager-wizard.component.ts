import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { TuiStepperModule, TuiStepState } from '@taiga-ui/kit';
import { UiCustomisations } from '../../../environments/ui-customisations';
import { HeaderComponentLayoutModule } from '../../header-component-layout/header-component-layout.module';
import { SourcePackageDescription } from '../../package-viewer/packages.service';
import { CreateOrReplaceSource } from '../../project-import/schema-importer.service';
import { PoliciesService, PolicySetupReadiness } from '../../services/policies.service';
import { ConfigureAuthStepComponent } from './configure-auth-step/configure-auth-step.component';
import { ConfirmModelStepComponent } from './confirm-model-step/confirm-model-step.component';
import { MapAuthTokenStepComponent } from './map-auth-token-step/map-auth-token-step.component';
import { WritePolicyStepComponent } from './write-policy-step/write-policy-step.component';

@Component({
  selector: 'app-policy-manager-wizard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    HeaderComponentLayoutModule,
    TuiStepperModule,
    ConfigureAuthStepComponent,
    MapAuthTokenStepComponent,
    ConfirmModelStepComponent,
    WritePolicyStepComponent,
  ],
  template: `
    <app-header-component-layout title="Policies: Get started" backLink="/policies">
      <ng-container ngProjectAs="header-components">
        <tui-stepper [(activeItemIndex)]="stepperActiveStep">
          <button tuiStep
                  [stepState]="authenticationSetupState"
                  [disabled]="stepperActiveStep > 0"
          >Configure authentication</button>
          <button tuiStep
                  [stepState]="stepperActiveStep > 1 ? 'pass' : 'normal'"
                  [disabled]="stepperActiveStep !== 1"
          >Map your auth token</button>
          <button tuiStep
                  [stepState]="stepperActiveStep > 2 ? 'pass' : 'normal'"
                  [disabled]="stepperActiveStep !== 2"
          >Confirm model</button>
          <button tuiStep
                  [disabled]="stepperActiveStep !== 3"
          >Write a policy</button>
        </tui-stepper>
      </ng-container>
      <ng-container *ngIf="stepperActiveStep === 0">
        <app-configure-auth-step></app-configure-auth-step>
      </ng-container>
      <ng-container *ngIf="stepperActiveStep === 1">
        <app-map-auth-token-step
          [claims]="setupReadiness?.claims"
          (projectSelected)="onProjectSelected($event)"
          (schemaSubmissionResultReceived)="onSchemaSubmissionResultReceived($event)"
        ></app-map-auth-token-step>
      </ng-container>
      <ng-container *ngIf="stepperActiveStep === 2">
        <app-confirm-model-step
          [pendingEdits]="pendingEdits"
          [selectedProject]="selectedProject"
          (gotoMapAuthTokenStep)="stepperActiveStep = 1"
          (gotoWritePolicyStep)="stepperActiveStep = 3"
        ></app-confirm-model-step>
      </ng-container>
      <ng-container *ngIf="stepperActiveStep === 3">
        <app-write-policy-step></app-write-policy-step>
      </ng-container>
    </app-header-component-layout>

  `,
  styleUrls: ['./policy-manager-wizard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PolicyManagerWizardComponent {
  setupReadiness: PolicySetupReadiness | null;
  selectedProject: SourcePackageDescription
  pendingEdits: CreateOrReplaceSource;
  stepperActiveStep = 0;

  constructor(
    private policiesService: PoliciesService,
    private changeDetectorRef: ChangeDetectorRef,
  ) {
    policiesService.getPolicySetupReadiness().pipe(
      takeUntilDestroyed()
    )
      .subscribe(value => {
        this.setupReadiness = value;
        if (this.setupReadiness.authenticationConfigured) {
          this.stepperActiveStep = 1;
        }
        changeDetectorRef.detectChanges();
      })
  }

  get authenticationSetupState():TuiStepState {
    if (!this.setupReadiness) {
      return "error";
    }
    if (this.setupReadiness.authenticationConfigured) {
      return "pass";
    } else {
      return "normal";
    }
  }

  onSchemaSubmissionResultReceived($event: CreateOrReplaceSource) {
    this.stepperActiveStep = 2;
    this.pendingEdits = $event;
    this.changeDetectorRef.detectChanges();
  }

  onProjectSelected($event: SourcePackageDescription) {
    this.selectedProject = $event;
  }

  protected readonly UiCustomisations = UiCustomisations;
}

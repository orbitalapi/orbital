import {CommonModule} from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  Signal,
  signal,
  computed,
  WritableSignal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {TuiStepper} from '@taiga-ui/kit';
import {tap} from 'rxjs/operators';
import {HeaderComponentLayoutModule} from '../../header-component-layout/header-component-layout.module';
import { PoliciesService, PolicySetupReadiness } from '../../services/policies.service';
import { CreateOrReplaceSource } from '../../project-import/schema-importer.service';
import { SourcePackageDescription } from '../../package-viewer/packages.service';
import {ConfigureAuthStepComponent} from './configure-auth-step/configure-auth-step.component';
import {ConfirmModelStepComponent} from './confirm-model-step/confirm-model-step.component';
import {MapAuthTokenStepComponent} from './map-auth-token-step/map-auth-token-step.component';
import {WritePolicyStepComponent} from './write-policy-step/write-policy-step.component';

@Component({
  selector: 'app-policy-manager-wizard',
  standalone: true,
  imports: [
    TuiStepper,
    CommonModule,
    HeaderComponentLayoutModule,
    ConfigureAuthStepComponent,
    MapAuthTokenStepComponent,
    ConfirmModelStepComponent,
    WritePolicyStepComponent,
  ],
  template: `
    <app-header-component-layout title="Policies: Get started" backLink="/policies">
      <ng-container ngProjectAs="header-components">
        <tui-stepper
          *ngIf="!isLoading()"
          [activeItemIndex]="stepperActiveStep()"
          (activeItemIndexChange)="stepperActiveStep.set($event)">
          <button
            tuiStep
            *ngIf="WizardStep()['Configure'] === 0"
            [stepState]="authenticationSetupState()"
            [disabled]="stepperActiveStep() > WizardStep()['Configure']"
          >
            Configure authentication
          </button>
          <button
            tuiStep
            [stepState]="stepperActiveStep() > WizardStep().Map ? 'pass' : 'normal'"
            [disabled]="stepperActiveStep() !== WizardStep().Map"
          >
            Map your auth token
          </button>
          <button
            tuiStep
            [stepState]="stepperActiveStep() > WizardStep().Confirm ? 'pass' : 'normal'"
            [disabled]="stepperActiveStep() !== WizardStep().Confirm"
          >
            Confirm model
          </button>
          <button tuiStep [disabled]="stepperActiveStep() !== WizardStep().Write">
            Write a policy
          </button>
        </tui-stepper>
      </ng-container>
      <ng-container *ngIf="stepperActiveStep() === WizardStep()['Configure'] && !isLoading()">
        <app-configure-auth-step></app-configure-auth-step>
      </ng-container>
      <ng-container *ngIf="stepperActiveStep() === WizardStep().Map">
        <app-map-auth-token-step
          [claims]="setupReadiness()?.claims"
          (projectSelected)="onProjectSelected($event)"
          (schemaSubmissionResultReceived)="onSchemaSubmissionResultReceived($event)"
        ></app-map-auth-token-step>
      </ng-container>
      <ng-container *ngIf="stepperActiveStep() === WizardStep().Confirm">
        <app-confirm-model-step
          [pendingEdits]="pendingEdits()"
          [selectedProject]="selectedProject()"
          (gotoMapAuthTokenStep)="stepperActiveStep.set(WizardStep().Map)"
          (gotoWritePolicyStep)="stepperActiveStep.set(WizardStep().Write)"
        ></app-confirm-model-step>
      </ng-container>
      <ng-container *ngIf="stepperActiveStep() === WizardStep().Write">
        <app-write-policy-step></app-write-policy-step>
      </ng-container>
    </app-header-component-layout>
  `,
  styleUrls: ['./policy-manager-wizard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PolicyManagerWizardComponent {
  setupReadiness: WritableSignal<PolicySetupReadiness | null> = signal(null);
  pendingEdits: WritableSignal<CreateOrReplaceSource> = signal(null);
  selectedProject: WritableSignal<SourcePackageDescription> = signal(null);
  isLoading = signal(true)

  // Dynamic WizardStep "enum"
  WizardStep = computed(() => {
    if (this.setupReadiness()?.authenticationConfigured) {
      return {
        Map: 0,
        Confirm: 1,
        Write: 2,
      };
    } else {
      return {
        Configure: 0,
        Map: 1,
        Confirm: 2,
        Write: 3,
      };
    }
  });

  stepperActiveStep: WritableSignal<number> = signal(this.WizardStep().Configure);

  constructor(
    private policiesService: PoliciesService,
  ) {
    this.policiesService.getPolicySetupReadiness()
      .pipe(
        takeUntilDestroyed()
      )
      .subscribe((value) => {
        this.setupReadiness.set(value);
        if (value?.authenticationConfigured) {
          this.stepperActiveStep.set(this.WizardStep().Map);
        } else {
          this.stepperActiveStep.set(this.WizardStep().Configure);
        }
        this.isLoading.set(false)
      });
  }

  authenticationSetupState: Signal<'error' | 'pass' | 'normal'> = computed(() => {
    const setupReadiness = this.setupReadiness();
    if (!setupReadiness) {
      return 'error';
    }
    return setupReadiness.authenticationConfigured ? 'pass' : 'normal';
  });

  onSchemaSubmissionResultReceived(event: CreateOrReplaceSource) {
    this.stepperActiveStep.set(this.WizardStep().Confirm);
    this.pendingEdits.set(event);
  }

  onProjectSelected(event: SourcePackageDescription) {
    this.selectedProject.set(event);
  }
}

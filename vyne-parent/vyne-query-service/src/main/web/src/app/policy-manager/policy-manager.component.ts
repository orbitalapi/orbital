import {Component, Input, OnInit} from '@angular/core';
import {Policy} from "./policies";
import {SchemaImportRequest, TypesService} from "../services/types.service";
import {SchemaSpec, Type} from "../services/schema";
import {MatSnackBar} from "@angular/material";

@Component({
  selector: 'app-policy-manager',
  styleUrls: ['./policy-manager.component.scss'],
  template: `
    <div class="content">
      <mat-progress-bar mode="indeterminate" color="accent" *ngIf="loading"></mat-progress-bar>
      <div class="empty-state" *ngIf="!policy">
        <div>Policies let you define rules when data can be read as it's served from Vyne. <a href="javascript:void"
                                                                                              (click)="createNewPolicy()">Create
          a new policy</a> now to get started.
        </div>
        <div class="button-container">
          <button mat-raised-button color="primary" (click)="createNewPolicy()">Add new</button>
        </div>
      </div>
      <div class="policy-editor-container">
        <app-policy-editor [policy]="policy" [policyType]="targetType" *ngIf="policy" (save)="save()"
                           (cancel)="cancel()"></app-policy-editor>
      </div>
    </div>
  `
})
export class PolicyManagerComponent implements OnInit {

  policy: Policy;

  loading: boolean = false;

  @Input()
  targetType: Type;

  constructor(private typeService: TypesService, private snackBar: MatSnackBar) {
  }


  createNewPolicy() {
    this.policy = Policy.createNew(this.targetType)
  }

  save() {
    const spec: SchemaSpec = {
      name: `${this.policy.targetTypeName.fullyQualifiedName}.${this.policy.name.name}Policy`,
      version: 'next-minor',
      defaultNamespace: this.policy.targetTypeName.namespace
    };
    const request = new SchemaImportRequest(
      spec, "taxi", this.policy.src()
    );
    this.loading = true;
    this.typeService.submitSchema(request).subscribe(result => {
      this.loading = false;
      this.snackBar.open("Policy saved", "Dismiss", {duration: 3000});
    }, error => {
      this.loading = false;
      this.snackBar.open("An error occurred.  Your changes have not been saved.", "Dismiss", {duration: 3000});
    })
  }

  ngOnInit() {
    this.loadPolicy();
  }

  private loadPolicy() {
    this.loading = true;
    this.typeService.getPolicies(this.targetType.name.fullyQualifiedName)
      .subscribe(policies => {
        // We only support single policy, so grab the first item from the array if present
        if (policies.length > 0) {
          this.policy = policies[0]
        } else {
          this.policy = null;
        }
        this.loading = false
      }, error => {
        console.log("Failed to load policies: ");
        console.log(error);
        this.loading = false;
      })
  }

  cancel() {
    this.loadPolicy()
  }
}

import {Component, Input, OnInit} from '@angular/core';
import {Policy} from "./policies";
import {Type, TypesService} from "../services/types.service";

@Component({
  selector: 'app-policy-manager',
  styleUrls: ['./policy-manager.component.scss'],
  template: `
    <div class="content">
      <mat-progress-bar mode="indeterminate" *ngIf="loading"></mat-progress-bar>
      <div class="policy-list">
        <div class="add-new">
          <button mat-raised-button color="primary" (click)="createNewPolicy()">Add new</button>
        </div>
        <div *ngFor="let policy of policies" class="policy-list-member"
             [ngClass]="selectedPolicy == policy ? 'selected' : ''"
             (click)="selectPolicy(policy)">
          <div class="policy-name">{{ policy.name.name }}</div>
        </div>
      </div>
      <div class="policy-editor-container">
        <app-policy-editor [policy]="selectedPolicy" *ngIf="selectedPolicy"></app-policy-editor>
      </div>
    </div>
  `
})
export class PolicyManagerComponent implements OnInit {

  selectedPolicy: Policy;
  policies: Policy[] = [];

  loading: boolean = false;

  @Input()
  targetType: Type;

  constructor(private typeService: TypesService) {
  }


  createNewPolicy() {
    const policy = Policy.createNew(this.targetType);
    this.policies.push(policy);
    this.selectedPolicy = policy
  }

  ngOnInit() {
    this.loading = true;
    this.typeService.getPolicies(this.targetType.name.fullyQualifiedName)
      .subscribe(policies => {
        this.policies = policies;
        this.loading = false
      }, error => {
        console.log("Failed to load policies: ");
        console.log(error);
        this.loading = false;
      })
  }

  selectPolicy(policy) {
    this.selectedPolicy = policy
  }

}

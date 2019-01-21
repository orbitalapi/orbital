import {Component, Input, OnInit} from '@angular/core';
import {Policy} from "./policies";
import {Type} from "../services/types.service";

@Component({
  selector: 'app-policy-manager',
  styleUrls: ['./policy-manager.component.scss'],
  template: `
    <div class="content">
      <div class="policy-list">
        <div class="add-new">
          <button mat-raised-button color="primary" (click)="createNewPolicy()">Add new</button>
        </div>
        <div *ngFor="let policy of policies" class="policy-list-member"
             [ngClass]="selectedPolicy == policy ? 'selected' : ''"
             (click)="selectPolicy(policy)">
          <div class="policy-name">{{ policy.name }}</div>
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

  @Input()
  targetType: Type;

  constructor() {
  }

  createNewPolicy() {
    const policy = Policy.createNew(this.targetType);
    this.policies.push(policy);
    this.selectedPolicy = policy
  }

  ngOnInit() {
  }

  selectPolicy(policy) {
    this.selectedPolicy = policy
  }

}

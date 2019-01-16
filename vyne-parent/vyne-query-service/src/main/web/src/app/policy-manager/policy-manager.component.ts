import {Component, Input, OnInit} from '@angular/core';
import {Policy} from "./policies";
import {Type} from "../services/types.service";

@Component({
  selector: 'app-policy-manager',
  templateUrl: './policy-manager.component.html',
  styleUrls: ['./policy-manager.component.scss']
})
export class PolicyManagerComponent implements OnInit {

  selectedPolicy: Policy;
  policies: Policy[] = [];

  @Input()
  targetType: Type

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

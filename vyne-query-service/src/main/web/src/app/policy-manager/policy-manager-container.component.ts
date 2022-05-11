import {Component, Input, OnInit} from '@angular/core';
import {Policy} from './policies';
import {SchemaImportRequest, TypesService} from '../services/types.service';
import {Schema, SchemaSpec, Type} from '../services/schema';
import {Observable} from 'rxjs';
import {MatSnackBar} from '@angular/material/snack-bar';

@Component({
  selector: 'app-policy-manager-container',
  template: `
    <app-policy-manager (save)="save($event)" [loading]="loading" [policy]="policy" (reset)="cancel()"
                        [targetType]="targetType"
                        (createNewPolicy)="createNewPolicy()" [schema]="schema | async"></app-policy-manager>
  `
})
export class PolicyManagerContainerComponent implements OnInit {

  policy: Policy;

  loading = false;

  @Input()
  targetType: Type;

  schema: Observable<Schema>;

  constructor(private typeService: TypesService, private snackBar: MatSnackBar) {
    this.schema = typeService.getTypes();
  }


  save(policy: Policy) {
    this.loading = true;
    this.typeService.createExtensionSchemaFromTaxi(this.policy.targetTypeName, 'Policy', policy.src())
      .subscribe(result => {
        this.loading = false;
        this.snackBar.open('Policy saved', 'Dismiss', {duration: 3000});
      }, error => {
        this.loading = false;
        this.snackBar.open('An error occurred.  Your changes have not been saved.', 'Dismiss', {duration: 3000});
      });
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
          this.policy = policies[0];
        } else {
          this.policy = null;
        }
        this.loading = false;
      }, error => {
        console.log('Failed to load policies: ');
        console.log(error);
        this.loading = false;
      });
  }

  cancel() {
    this.loadPolicy();
  }

  createNewPolicy() {
    this.policy = Policy.createNew(this.targetType);
  }
}

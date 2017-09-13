import { Component, OnInit } from '@angular/core';

import { ActivatedRoute } from '@angular/router';

import { FeaturesService, IFeature } from '../../../../services';

@Component({
  selector: 'qs-feature-form',
  templateUrl: './form.component.html',
  styleUrls: ['./form.component.scss'],
  viewProviders: [ FeaturesService ],
})
export class FeaturesFormComponent implements OnInit  {
  id: string;
  title: string;
  icon: string;
  enabled: boolean;
  user: string;
  feature: IFeature;

  action: string;

  constructor(private _featuresService: FeaturesService, private _route: ActivatedRoute) {}

  goBack(): void {
    window.history.back();
  }

  ngOnInit(): void {
    this._route.url.subscribe((url: any) => {
      this.action = (url.length > 1 ? url[1].path : 'add');
    });
    this._route.params.subscribe((params: {id: string}) => {
      let featureId: string = params.id;
      this._featuresService.get(featureId).subscribe((feature: any) => {
        this.title = feature.title;
        this.user = feature.user;
        this.enabled = (feature.enabled === 1 ? true : false);
        this.id = feature.id;
      });
    });
  }

  save(): void {
    let enabled: number = (this.enabled ? 1 : 0);
    let now: Date = new Date();
    this.feature = {
      title: this.title,
      user: this.user,
      enabled: enabled,
      icon: this.icon,
      id: this.id || this.title.replace(/\s+/g, '.'),
      created: now,
      modified: now,
    };
    if (this.action === 'add') {
      this._featuresService.create(this.feature).subscribe(() => {
        this.goBack();
      });
    } else {
      this._featuresService.update(this.id, this.feature).subscribe(() => {
        this.goBack();
      });
    }
  }
}

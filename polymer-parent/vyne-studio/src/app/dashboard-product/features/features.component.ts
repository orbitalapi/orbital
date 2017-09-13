import { Component, OnInit } from '@angular/core';
import { Title }     from '@angular/platform-browser';

import { TdLoadingService } from '@covalent/core';
import { TdDialogService } from '@covalent/core';

import { FeaturesService, IFeature } from '../../../services';

@Component({
  selector: 'qs-product-features',
  templateUrl: './features.component.html',
  styleUrls: ['./features.component.scss'],
  viewProviders: [ FeaturesService ],
})
export class ProductFeaturesComponent implements OnInit {

  features: IFeature[];
  filteredFeatures: IFeature[];

  constructor(private _titleService: Title,
              private _dialogService: TdDialogService,
              private _featuresService: FeaturesService,
              private _loadingService: TdLoadingService) {

  }
  openConfirm(id: string): void {
    this._dialogService.openConfirm({
      message: 'Are you sure you want to delete this feature? It\'s being used!',
      title: 'Confirm',
      cancelButton: 'No, Cancel',
      acceptButton: 'Yes, Delete',
    }).afterClosed().subscribe((accept: boolean) => {
      if (accept) {
        this.deleteFeature(id);
      } else {
        // DO SOMETHING ELSE
      }
    });
  }
  ngOnInit(): void {
    this._titleService.setTitle( 'Product Features' );
    this.loadFeatures();
  }
  filterFeatures(filterTitle: string = ''): void {
    this.filteredFeatures = this.features.filter((feature: IFeature) => {
      return feature.title.toLowerCase().indexOf(filterTitle.toLowerCase()) > -1;
    });
  }

  loadFeatures(): void {
    this._loadingService.register('features.list');
    this._featuresService.query().subscribe((features: IFeature[]) => {
      this.features = features;
      this.filteredFeatures = features;
      this._loadingService.resolve('features.list');
    }, (error: Error) => {
      this._featuresService.staticQuery().subscribe((features: IFeature[]) => {
        this.features = features;
        this.filteredFeatures = features;
        this._loadingService.resolve('features.list');
      });
    });
  }
  deleteFeature(id: any): void {
    this._loadingService.register('features.list');
    this._featuresService.delete(id).subscribe(() => {
      this.features = this.features.filter((feature: IFeature) => {
        return feature.id !== id;
      });
      this.filteredFeatures = this.filteredFeatures.filter((feature: IFeature) => {
        return feature.id !== id;
      });
      this._loadingService.resolve('features.list');
    }, (error: Error) => {
      this._loadingService.resolve('features.list');
    });
  }
}

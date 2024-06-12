import { moduleMetadata } from "@storybook/angular";
import { CommonModule } from "@angular/common";
import { BrowserModule } from "@angular/platform-browser";
import {of} from 'rxjs';
import {ChangesetService} from '../changeset-selector/changeset.service';
import {testSchema} from '../object-view/test-schema';
import {TypesService} from '../services/types.service';
import { service } from "./service-schema";
import { RouterTestingModule } from "@angular/router/testing";
import {ServiceViewComponent} from './service-view.component';

class MockTypesService implements Partial<TypesService> {
  getTypes = (refresh?: boolean) => of(testSchema)
}
class MockChangesetService implements Partial<ChangesetService> {
}

export default {
  title: "Service view",

  decorators: [
    moduleMetadata({
      declarations: [],
      imports: [CommonModule, BrowserModule, RouterTestingModule, ServiceViewComponent],
      providers: [
        {provide: TypesService, useClass: MockTypesService},
        {provide: ChangesetService, useClass: MockChangesetService}
      ]
    }),
  ],
};

export const ServiceView = () => {
  return {
    template: `<div style="padding: 40px; width: 100%; height: 100%" >
    <app-service-view [service]="service"></app-service-view>
    </div>`,
    props: {
      service: service,
    },
  };
};

ServiceView.story = {
  name: "Service view",
};

import { Injectable } from '@angular/core';
import { Response } from '@angular/http';
import { Observable } from 'rxjs/Observable';

import { HttpInterceptorService, RESTService } from '@covalent/http';
import { MOCK_API } from '../config/api.config';

export interface IFeature {
  title: string;
  id: string;
  user: string;
  modified: Date;
  created: Date;
  icon: string;
  enabled: number;
}

@Injectable()
export class FeaturesService extends RESTService<IFeature> {

  constructor(private _http: HttpInterceptorService) {
    super(_http, {
      baseUrl: MOCK_API,
      path: '/features',
    });
  }

  staticQuery(): Observable<IFeature[]> {
    return this._http.get('data/features.json')
    .map((res: Response) => {
      return res.json();
    });
  }
}

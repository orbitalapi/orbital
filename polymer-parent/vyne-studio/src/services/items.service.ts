import { Injectable } from '@angular/core';
import { Response } from '@angular/http';
import { HttpInterceptorService, RESTService } from '@covalent/http';
import { MOCK_API } from '../config/api.config';

@Injectable()
export class ItemsService extends RESTService<any> {

  constructor(private _http: HttpInterceptorService) {
    super(_http, {
      baseUrl: MOCK_API,
      path: '/items',
    });
  }

  staticQuery(): any {
    return this._http.get('data/items.json')
    .map((res: Response) => {
      return res.json();
    });
  }

  staticGet(id: string): any {
    return this._http.get('data/items.json')
    .map((res: Response) => {
      let item: any;
      res.json().forEach((s: any) => {
        if (s.item_id === id) {
          item = s;
        }
      });
      return item;
    });
  }
}

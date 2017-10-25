import { Http, Headers, Response, Jsonp, RequestOptions } from "@angular/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs/Observable";

import "rxjs/Rx";
import "rxjs/add/operator/map";
import "rxjs/add/operator/toPromise";
import "rxjs/add/operator/catch";
@Injectable()
export class DataService {
  constructor(private http: Http) { }

  getProfile = (): Observable<Response> => {
    return this.http
      .get("api/my-profile")
      .map(res => res.json());
  };
  getContactsCardDemo = (): Observable<Response> => {
    return this.http
      .get("api/my-contacts")
      .map(res => res.json());
  };
  getMailDemo = (): Observable<Response> => {
    return this.http
      .get("api/mail")
      .map(res => res.json());
  };
  getListCardDemo = (): Observable<Response> => {
    return this.http
      .get("api/list")
      .map(res => res.json());
  };
	getChatContacts = (): Observable<Response> => {
		return this.http
			.get("api/chat-messages")
			.map(res => res.json());
	};
	getTabsOverCard = (): Observable<Response> => {
		return this.http
			.get("api/tabs-over-card")
			.map(res => res.json());
	};
	getContacts = (): Observable<Response> => {
		return this.http
			.get("api/my-contacts")
			.map(res => res.json());
	};
}

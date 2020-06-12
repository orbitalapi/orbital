import {Injectable} from '@angular/core';
import {VyneServicesModule} from './vyne-services.module';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../environments/environment';

@Injectable({
  providedIn: VyneServicesModule
})
export class CaskService {
  constructor(private http: HttpClient) {

  }

  publishToCask(caskRequestUrl: string, content: string): Observable<any> {
    return this.http.post<any>(caskRequestUrl, content);
  }
}

import {Injectable} from '@angular/core';
import {Observable, Subject} from 'rxjs';

@Injectable({providedIn: 'root'})
export class CustomCsvTableHeaderService {
  private addTypeSubject = new Subject<string>();
  private removeTypeSubject = new Subject<string>();


  sendFieldName(fieldName: string) {
    this.addTypeSubject.next(fieldName);
  }

  getFieldName(): Observable<string> {
    return this.addTypeSubject.asObservable();
  }

  getTypeToRemove(): Observable<string> {
    return this.removeTypeSubject.asObservable();
  }

  sendTypeToRemove(fieldName: string) {
    this.removeTypeSubject.next(fieldName);
  }
}

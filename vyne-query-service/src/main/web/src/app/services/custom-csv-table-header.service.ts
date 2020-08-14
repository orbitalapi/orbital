import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class CustomCsvTableHeaderService {
  private subject = new Subject<string>();

  sendFieldName(fieldName: string) {
    this.subject.next( fieldName);
  }

  getFieldName(): Observable<string> {
    return this.subject.asObservable();
  }
}

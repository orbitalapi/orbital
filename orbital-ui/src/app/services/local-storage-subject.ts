import {BehaviorSubject, Observable, PartialObserver} from "rxjs";
import {skip, tap} from "rxjs/operators";

export class LocalStorageSubject<T> {
  private readonly subject$: BehaviorSubject<T>

  constructor(private key: string) {
    const initialValue = this.getFromLocalStorage();
    this.subject$ = new BehaviorSubject<T>(initialValue);

    this.subject$
      .pipe(skip(1)) // The first value is from us
      .subscribe(value => {
        localStorage.setItem(this.key, JSON.stringify(value));
      })
  }

  observable():Observable<T> {
    return this.subject$.asObservable();
  }

  private getFromLocalStorage(): T | null {
    const storedValue = localStorage.getItem(this.key)
    return storedValue ? JSON.parse(storedValue) : null;
  }

  setValue(value: T) {
    this.subject$.next(value);
  }


  subscribe(observer: PartialObserver<T>): any {
    return this.subject$.subscribe(observer);
  }

}

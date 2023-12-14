import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ObjectViewComponent } from './object-view.component';

describe('ObjectViewComponent', () => {
  let component: ObjectViewComponent;
  let fixture: ComponentFixture<ObjectViewComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ObjectViewComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ObjectViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

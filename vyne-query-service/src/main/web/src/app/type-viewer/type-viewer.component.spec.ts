import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { TypeViewerComponent } from './type-viewer.component';

describe('TypeViewerComponent', () => {
  let component: TypeViewerComponent;
  let fixture: ComponentFixture<TypeViewerComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ TypeViewerComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TypeViewerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

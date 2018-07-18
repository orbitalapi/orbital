import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TypeDetailComponent } from './type-detail.component';

describe('TypeDetailComponent', () => {
  let component: TypeDetailComponent;
  let fixture: ComponentFixture<TypeDetailComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ TypeDetailComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TypeDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

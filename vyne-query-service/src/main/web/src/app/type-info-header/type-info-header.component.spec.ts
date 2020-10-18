import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TypeInfoHeaderComponent } from './type-info-header.component';

describe('TypeInfoHeaderComponent', () => {
  let component: TypeInfoHeaderComponent;
  let fixture: ComponentFixture<TypeInfoHeaderComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ TypeInfoHeaderComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TypeInfoHeaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

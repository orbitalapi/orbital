import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TaxiEditorComponent } from './taxi-editor.component';

describe('TaxiEditorComponent', () => {
  let component: TaxiEditorComponent;
  let fixture: ComponentFixture<TaxiEditorComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ TaxiEditorComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TaxiEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

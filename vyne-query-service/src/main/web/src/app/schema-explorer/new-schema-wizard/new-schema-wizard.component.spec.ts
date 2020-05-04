import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { NewSchemaWizardComponent } from './new-schema-wizard.component';

describe('NewSchemaWizardComponent', () => {
  let component: NewSchemaWizardComponent;
  let fixture: ComponentFixture<NewSchemaWizardComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ NewSchemaWizardComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NewSchemaWizardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

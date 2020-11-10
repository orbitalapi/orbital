import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CaskIngestionErrorsComponent } from './cask-ingestion-errors.component';

describe('CaskIngestionErrorsComponent', () => {
  let component: CaskIngestionErrorsComponent;
  let fixture: ComponentFixture<CaskIngestionErrorsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ CaskIngestionErrorsComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CaskIngestionErrorsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

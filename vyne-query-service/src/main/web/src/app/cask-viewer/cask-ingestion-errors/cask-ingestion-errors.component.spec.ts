import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { CaskIngestionErrorsComponent } from './cask-ingestion-errors.component';

describe('CaskIngestionErrorsComponent', () => {
  let component: CaskIngestionErrorsComponent;
  let fixture: ComponentFixture<CaskIngestionErrorsComponent>;

  beforeEach(waitForAsync(() => {
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

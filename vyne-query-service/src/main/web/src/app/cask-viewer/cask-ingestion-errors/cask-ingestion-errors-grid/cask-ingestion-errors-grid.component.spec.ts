import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { CaskIngestionErrorsGridComponent } from './cask-ingestion-errors-grid.component';

describe('CaskIngestionErrorsGridComponent', () => {
  let component: CaskIngestionErrorsGridComponent;
  let fixture: ComponentFixture<CaskIngestionErrorsGridComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ CaskIngestionErrorsGridComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CaskIngestionErrorsGridComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

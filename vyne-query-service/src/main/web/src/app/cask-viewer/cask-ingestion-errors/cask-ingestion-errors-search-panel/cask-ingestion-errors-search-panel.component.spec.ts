import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { CaskIngestionErrorsSearchPanelComponent } from './cask-ingestion-errors-search-panel.component';

describe('CaskIngestionErrorsSearchPanelComponent', () => {
  let component: CaskIngestionErrorsSearchPanelComponent;
  let fixture: ComponentFixture<CaskIngestionErrorsSearchPanelComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ CaskIngestionErrorsSearchPanelComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CaskIngestionErrorsSearchPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

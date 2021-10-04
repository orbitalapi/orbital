import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DataCatalogContainerComponent } from './data-catalog-container.component';

describe('DataCatalogContainerComponent', () => {
  let component: DataCatalogContainerComponent;
  let fixture: ComponentFixture<DataCatalogContainerComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DataCatalogContainerComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DataCatalogContainerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

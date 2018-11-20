import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SchemaExplorerComponent } from './schema-explorer.component';

describe('SchemaExplorerComponent', () => {
  let component: SchemaExplorerComponent;
  let fixture: ComponentFixture<SchemaExplorerComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ SchemaExplorerComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SchemaExplorerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

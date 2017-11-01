import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TypeExplorerComponent } from './type-explorer.component';

describe('SchemaExplorerComponent', () => {
  let component: TypeExplorerComponent;
  let fixture: ComponentFixture<TypeExplorerComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ TypeExplorerComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TypeExplorerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});

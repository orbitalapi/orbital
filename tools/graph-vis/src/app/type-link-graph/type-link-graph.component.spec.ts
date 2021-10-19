import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TypeLinkGraphComponent } from './type-link-graph.component';

describe('TypeLinkGraphComponent', () => {
  let component: TypeLinkGraphComponent;
  let fixture: ComponentFixture<TypeLinkGraphComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ TypeLinkGraphComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TypeLinkGraphComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

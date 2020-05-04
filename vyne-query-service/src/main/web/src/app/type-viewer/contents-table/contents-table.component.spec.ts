import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ContentsTableComponent } from './contents-table.component';

describe('ContentsTableComponent', () => {
  let component: ContentsTableComponent;
  let fixture: ComponentFixture<ContentsTableComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ContentsTableComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ContentsTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

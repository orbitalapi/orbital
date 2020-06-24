import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { FileFactSelectorComponent } from './file-fact-selector.component';

describe('FileFactSelectorComponent', () => {
  let component: FileFactSelectorComponent;
  let fixture: ComponentFixture<FileFactSelectorComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ FileFactSelectorComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FileFactSelectorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

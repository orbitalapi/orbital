import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { DescriptionEditorComponent } from './description-editor.component';

describe('DescriptionEditorComponent', () => {
  let component: DescriptionEditorComponent;
  let fixture: ComponentFixture<DescriptionEditorComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ DescriptionEditorComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DescriptionEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

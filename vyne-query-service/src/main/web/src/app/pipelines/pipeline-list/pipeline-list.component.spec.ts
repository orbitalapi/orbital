import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { PipelineListComponent } from './pipeline-list.component';

describe('PipelineListComponent', () => {
  let component: PipelineListComponent;
  let fixture: ComponentFixture<PipelineListComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ PipelineListComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PipelineListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

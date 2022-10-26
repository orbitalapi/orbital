import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PushSchemaConfigPanelComponent } from './push-panel.component';

describe('PushPanelComponent', () => {
  let component: PushSchemaConfigPanelComponent;
  let fixture: ComponentFixture<PushSchemaConfigPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ PushSchemaConfigPanelComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(PushSchemaConfigPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

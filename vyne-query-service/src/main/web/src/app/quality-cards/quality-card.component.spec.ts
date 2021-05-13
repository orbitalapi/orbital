import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { QualityCardComponent } from './quality-card.component';

describe('QualityCardsComponent', () => {
  let component: QualityCardComponent;
  let fixture: ComponentFixture<QualityCardComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ QualityCardComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(QualityCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

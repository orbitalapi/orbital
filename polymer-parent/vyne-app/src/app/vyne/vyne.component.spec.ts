
import { fakeAsync, ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSidenavModule } from '@angular/material/sidenav';
import { VyneComponent } from './vyne.component';

describe('VyneComponent', () => {
  let component: VyneComponent;
  let fixture: ComponentFixture<VyneComponent>;

  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [MatSidenavModule],
      declarations: [VyneComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(VyneComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }));

  it('should compile', () => {
    expect(component).toBeTruthy();
  });
});

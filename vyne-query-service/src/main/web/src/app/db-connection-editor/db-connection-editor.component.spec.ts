import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DbConnectionEditorComponent } from './db-connection-editor.component';

describe('DbConnectionEditorComponent', () => {
  let component: DbConnectionEditorComponent;
  let fixture: ComponentFixture<DbConnectionEditorComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DbConnectionEditorComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DbConnectionEditorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

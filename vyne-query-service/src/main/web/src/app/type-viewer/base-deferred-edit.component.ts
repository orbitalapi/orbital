import {Directive, EventEmitter, Input, Output} from '@angular/core';
import {Field, NamedAndDocumented} from '../services/schema';
import {CommitMode} from './type-viewer.component';

/**
 * Several components in the TypeViewer follow a pattern that allow editing in two modes -
 * either commit immediately (ie., writing back to the server straight away),
 * or wait, and defer updates to laterd
 */
// Need this since we're inherits multiple layers deep.
// See https://github.com/angular/angular/issues/35295
@Directive()
export abstract class BaseDeferredEditComponent<T extends NamedAndDocumented | Field> {

  /**
   * Emitted when the type has been updated, but not committed to the back-end.
   * (ie., when then commitMode = 'explicit')
   */
  @Output()
  updateDeferred = new EventEmitter<T>();

  abstract get type():T

  @Input()
  commitMode: CommitMode = 'immediate';

  protected emitUpdateIfRequired() {
    if (this.commitMode === 'explicit') {
      this.updateDeferred.emit(this.type);
    }
  }

}

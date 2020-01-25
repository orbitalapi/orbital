import {ElementRef, Injector} from '@angular/core';
import * as React from 'react';
import { Editor } from 'rich-markdown-editor';
import * as ReactDOM from 'react-dom';
import {Observable, Subject} from 'rxjs';
import light from './editor-theme';

export type ContentSupplier = () => string;

export interface EditorState {
  // Note that the editor doesn't yet support changing the initialState value.
  // See : https://github.com/outline/rich-markdown-editor/pull/142#issuecomment-573356750
  initialState: string;
  placeholder: string;
  changes$: Subject<ContentSupplier>;
}

export class ReactEditor extends React.Component<EditorState, any> {
  // vyneEditorTheme = {
  //   ...light,
  //   // See body-bg from theme-colors.scss
  //   background: '#f5f6fa'
  // };

  constructor(props) {
    super(props);
  }

  render() {
    return (
      <div className={'renderer'}>
        <Editor
          // theme={this.vyneEditorTheme}
          placeholder={this.props.placeholder}
          defaultValue={this.props.initialState}
          onChange={value => this.props.changes$.next(value)}
        />
      </div>
    );
  }
}

export class ReactEditorWrapper {

  static initialize(
    elementRef: ElementRef,
    state: EditorState
  ) {
    ReactDOM.render(
      <ReactEditor changes$={state.changes$} initialState={state.initialState} placeholder={state.placeholder}/>,
      elementRef.nativeElement
    );
  }
}

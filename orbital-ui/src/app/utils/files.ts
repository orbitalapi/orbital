import {FileSystemFileEntry, NgxFileDropEntry} from 'ngx-file-drop';
import {Observable} from 'rxjs/internal/Observable';
import {Subject} from 'rxjs';

export function readSingleFile(fileDropEntry: NgxFileDropEntry): Observable<string> {
  const result$ = new Subject<string>();
  if (!fileDropEntry.fileEntry.isFile) {
    throw new Error('Expected a single file, but fileDropEntry.fileEntry.isFile == false');
  }

  const fileEntry = fileDropEntry.fileEntry as FileSystemFileEntry;
  fileEntry.file(file => {
    const reader = new FileReader();
    reader.onload = ((event) => {
      const fileText = (event.target as any).result;
      result$.next(fileText);
    });
    reader.readAsText(file);
  });
  return result$;
}

export function joinWithSeparator(path1: string, path2: string, separator): string {
  if (path1.endsWith(separator)) {
    return path1 + path2
  } else {
    return path1 + separator + path2;
  }
}

export function detectSeperator(path: string): string {
  if (path.includes("\\")) {
    return "\\"
  } else {
    return "/"
  }
}

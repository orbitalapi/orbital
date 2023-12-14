import {PackageIdentifier} from "../package-viewer/packages.service";
import {Observable} from "rxjs";
import {Changeset} from "../changeset-selector/changeset.service";

export type ChangesetNameDialogSaveHandler = (name: string, packageIdentifer: PackageIdentifier) => Observable<Changeset>;

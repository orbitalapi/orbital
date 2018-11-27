import {Component, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {
  Level,
  Message,
  SchemaImportRequest,
  SchemaPreview,
  SchemaPreviewRequest,
  TypesService,
  VersionedSchema
} from "../../services/types.service";
import {MatStepper} from "@angular/material";
import {Router} from "@angular/router";

@Component({
  selector: 'app-new-schema-wizard',
  templateUrl: './new-schema-wizard.component.html',
  styleUrls: ['./new-schema-wizard.component.scss']
})
export class NewSchemaWizardComponent implements OnInit {
  importOptionsFormGroup: FormGroup;
  schemaTypes = ["Swagger"];

  schemaPreview: SchemaPreview;
  working: boolean = false;
  errorMessage: string;
  versionedSchema: VersionedSchema;

  constructor(private fb: FormBuilder, private typeService: TypesService, private router: Router) {
    this.importOptionsFormGroup = this.fb.group(
      {
        schema: this.fb.group({
          url: ['', Validators.required],
          schemaType: ['', Validators.required]
        }),
      });
  }

  ngOnInit() {
  }

  generatePreview(stepper: MatStepper) {
    const url = this.importOptionsFormGroup.get("schema.url").value;
    const schemaType: string = this.importOptionsFormGroup.get("schema.schemaType").value;

    this.working = true;
    this.errorMessage = null;
    this.typeService.createSchemaPreview(
      new SchemaPreviewRequest({name: null, version: null, defaultNamespace: null},
        schemaType.toLowerCase(),
        null,
        url
      )
    ).subscribe(result => {
      this.schemaPreview = result;
      stepper.next();
      this.working = false;
    }, error => {
      this.errorMessage = "There was an error loading that schema - " + error.message;
      this.working = false;
    })
  }

  submitSchema(stepper: MatStepper) {
    // TODO : Should really store this on the spec.
    this.working = true;
    // Note - at this point, the schema is always taxi, as it was converted during the
    // preview process.
    const schemaType: string = "taxi"; // this.importOptionsFormGroup.get("schema.schemaType").value.toLowerCase();
    this.typeService.submitSchema(new SchemaImportRequest(
      this.schemaPreview.spec,
      schemaType,
      this.schemaPreview.content
    )).subscribe(result => {
      this.working = false;
      this.versionedSchema = result;
      stepper.next();
    }, error => {
      this.working = false;
      this.errorMessage = error.message;
    })
  }


  getIcon(message: Message) {
    switch (message.level) {
      case Level.INFO:
        return "info";
      case Level.WARN:
        return "warning";
      case Level.ERROR:
        return "error";
    }
  }

  returnToExplorer() {
    this.router.navigate(["schema-explorer"])
  }
}

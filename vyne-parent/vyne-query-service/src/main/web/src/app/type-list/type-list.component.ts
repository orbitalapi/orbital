import {Component, OnInit} from '@angular/core';
import {TypesService} from '../services/types.service';
import * as _ from 'lodash';
import {Router} from '@angular/router';
import {FormControl} from '@angular/forms';
import {Observable} from 'rxjs/internal/Observable';
import {map, startWith} from 'rxjs/operators';
import {Schema, SchemaMember, SchemaMemberType, Service, Type} from '../services/schema';

@Component({
  selector: 'app-type-list',
  templateUrl: './type-list.component.html',
  styleUrls: ['./type-list.component.scss']
})
export class TypeListComponent implements OnInit {

  constructor(private typeService: TypesService, private router: Router) {
  }

  schema: Schema;
  members: SchemaMember[] = [];
  searchInput = new FormControl();

  filteredMembers: Observable<SchemaMember[]>;

  ngOnInit() {
    this.loadTypes();
    this.filteredMembers = this.searchInput.valueChanges.pipe(
      startWith(''),
      map(value => this._filter(value))
    );
  }

  type(schemaMember: SchemaMember): Type {
    return this.schema.types.find((t) => t.name.fullyQualifiedName === schemaMember.name.fullyQualifiedName);
  }

  private loadTypes() {
    this.typeService.getTypes().subscribe(
      schema => {
        const typeMembers: SchemaMember[] = schema.types.map((t) => SchemaMember.fromType(t as Type));
        let operationMembers: SchemaMember[] = [];
        schema.services.forEach((service) => operationMembers = operationMembers.concat(SchemaMember.fromService(service as Service)));
        let members: SchemaMember[] = typeMembers.concat(operationMembers);
        members = _.sortBy(members, [(m: SchemaMember) => {
          return m.name.fullyQualifiedName;
        }]);
        this.schema = schema;
        this.members = members;

        // Reset the search input - ensures that initial state is populated
        this.searchInput.setValue('');
      },
      error => console.log('error : ' + error)
    );
  }

  memberType(member: SchemaMember): string {
    if (member.kind === SchemaMemberType.OPERATION) { return 'Operation'; }
    if (member.kind === SchemaMemberType.TYPE) { return 'Type'; }
    if (member.kind === SchemaMemberType.SERVICE) { return 'Service'; }
    return '?';
  }

  startNewQuery(member: SchemaMember) {
    this.router.navigate(['/query-wizard'], {
      queryParams: {'types': [member.name.fullyQualifiedName]}
    });
  }

  private _filter(value: string): SchemaMember[] {
    if (!this.schema) { return []; }
    if (value === '') { return this.members; }

    // Split by CamelCase:
    const searchWords = value.match(/([A-Z]?[^A-Z]*)/g);

    const filterValue = value.toLowerCase();
    return this.members.filter(member => {
      // Accept exact matches
      if (member.name.name.indexOf(filterValue) !== -1) { return true; }

      // Search for CamelHumps
      // We only look at words in the name - ie., exclude the package
      // TODO : Precompute this
      const memberNameWords = member.name.name.match(/[A-Z]*[^A-Z]+/g);


      let matched = true;
      searchWords.forEach((searchWord, index) => {
        if (matched === true && searchWord.length > 0) {
          matched = (memberNameWords.length > index && memberNameWords[index].indexOf(searchWord) !== -1);
        }
      });
      return matched;
    });
  }

  navigateToMember(member: SchemaMember) {
    this.router.navigate(['/types', member.name.fullyQualifiedName]);
  }
}

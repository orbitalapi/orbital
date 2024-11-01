import { TuiTextfieldControllerModule, TuiSelectModule } from "@taiga-ui/legacy";
import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import {RouterLink} from '@angular/router';
import { TuiDataList } from '@taiga-ui/core';
import { BehaviorSubject, filter, Observable } from 'rxjs';
import {map, tap} from 'rxjs/operators';
import { EndpointMonitorComponent } from '../../endpoint-manager/endpoint-monitor.component';
import { TypesService } from '../../services/types.service';
import { CardComponent } from '../card/card.component';
import {UiCustomisations} from "../../../environments/ui-customisations";

type QueryWithLabel = {
  label: string,
  query: string,
}

@Component({
  selector: 'app-endpoint-stats-card',
  standalone: true,
  imports: [
    CommonModule,
    CardComponent,
    EndpointMonitorComponent,
    TuiSelectModule,
    TuiTextfieldControllerModule,
    FormsModule,
    TuiDataList,
    RouterLink
  ],
  templateUrl: './endpoint-stats-card.component.html',
  styleUrls: ['./endpoint-stats-card.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EndpointStatsCardComponent {
  readonly queries$: Observable<QueryWithLabel[]>;
  readonly selectedQuery$: BehaviorSubject<QueryWithLabel> = new BehaviorSubject(null);
  readonly selectedEndpoint$: BehaviorSubject<string> = new BehaviorSubject(null);

  readonly stringifyQuery = (item: QueryWithLabel) => item.label;

  constructor(typeService: TypesService) {
    this.selectedQuery$.pipe(
      takeUntilDestroyed(),
      filter(query => !!query)
    ).subscribe(query => this.selectedEndpoint$.next(query.query))

    this.queries$ = typeService.getQueries().pipe(
      map(queries => queries.filter(savedQuery => savedQuery.httpEndpoint || savedQuery.websocketOperation)),
      map(queries => {
        if (queries.length) {
          const savedQueries = queries.map(query => {
            return {
              label: `${query.name.shortDisplayName} (${query.queryKind})`,
              query: query.name.shortDisplayName
            }
          })
          return [
            {
              label: "All queries & streams",
              query: ""
            },
            ...savedQueries
          ];
        } else {
          return []
        }
      }),
      tap(queries => queries.length ? this.selectedQuery$.next(queries[0]): null)
    );
  }

  protected readonly UiCustomisations = UiCustomisations;
}

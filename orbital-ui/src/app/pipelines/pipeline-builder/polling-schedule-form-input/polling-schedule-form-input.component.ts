import { Component, Input } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export const POLL_SCHEDULES = [
  { label: 'Every 10 seconds', value: '*/10 * * * * *' },
  { label: 'Every minute', value: '* * * * * *' },
  { label: 'Every hour', value: '0 0 * * * *' },
  { label: 'Midnight every day', value: '0 0 0 * * *' },
  { label: '10:15am every day', value: '0 0 15 10 * ?' },
  { label: '10:15am every week day', value: '0 0 15 10 * MON-FRI' }
];

@Component({
  selector: 'app-polling-schedule-form-input',
  templateUrl: './polling-schedule-form-input.component.html',
  styleUrls: ['./polling-schedule-form-input.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      multi: true,
      useExisting: PollingScheduleFormInputComponent
    }
  ]
})
export class PollingScheduleFormInputComponent implements ControlValueAccessor {

  @Input()
  pollSchedules = POLL_SCHEDULES;
  value: string;

  onChange: (value: string) => void = (value: string) => {
  };

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
  }

  writeValue(value: string): void {
    this.value = value;
  }

  updateValue(value: string): void {
    this.value = value;
    this.onChange(value);
  }
}

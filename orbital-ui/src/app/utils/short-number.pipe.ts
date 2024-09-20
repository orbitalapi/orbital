import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'shortNumber',
  standalone: true
})
export class ShortNumberPipe implements PipeTransform {

  transform(value: number | string, ...args: unknown[]): string {
    const num = typeof value === 'number' ? value : parseFloat(value);
    if (isNaN(num)) return '';
    const fractions = typeof args[0] === 'number' ? Math.round(args[0]) : 1;
    const degrees = [
      { sym: "T", val: Math.pow(10, 12) },
      { sym: "B", val: Math.pow(10, 9) },
      { sym: "M", val: Math.pow(10, 6) },
      { sym: "k", val: 1000 }
    ];
    const rounder = Math.pow(10, fractions);
    let abs = Math.abs(num);
    let symbol = '';
    for (const degree of degrees) {
      const sample = abs / degree.val;
      const rounded = Math.round(sample * rounder) / rounder;
      if (rounded >= 1) {
        abs = rounded;
        symbol = degree.sym;
        break;
      }
    }
    return (num < 0 ? '-' : '') + abs + symbol;
  }

}

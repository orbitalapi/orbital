import { Pipe, PipeTransform } from '@angular/core';

@Pipe({name: 'camelCaseToDash'})
export class CamelCaseToDashPipe implements PipeTransform
{
    /**
     * Transform
     *
     * @param {string} value
     * @param {any[]} args
     * @returns {string}
     */
    transform(value: string, args: any[] = []): string
    {
        return value ? String(value).replace(/([A-Z])/g, (g) => `-${g[0].toLowerCase()}`) : '';
    }
}

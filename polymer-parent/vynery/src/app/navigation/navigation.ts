import {FuseNavigation} from '@fuse/types';

export const navigation: FuseNavigation[] = [
    {
        id: 'applications',
        title: 'Applications',
        translate: 'NAV.APPLICATIONS',
        type: 'group',
        children: [
            {
                id: 'type-explorer',
                title: 'Type explorer',
                translate: 'nav.type-explorer.title',
                type: 'item',
                icon: 'explore',
                url: '/type-explorer',
                // badge    : {
                //     title    : '25',
                //     translate: 'NAV.SAMPLE.BADGE',
                //     bg       : '#F44336',
                //     fg       : '#FFFFFF'
                // }
            },
            {
                id: 'schema-editor',
                title: 'Schema editor',
                translate: 'nav.schema-editor.title',
                type: 'item',
                icon: 'code',
                url: '/schema-editor',
                // badge    : {
                //     title    : '25',
                //     translate: 'NAV.SAMPLE.BADGE',
                //     bg       : '#F44336',
                //     fg       : '#FFFFFF'
                // }
            }
        ]
    }
];

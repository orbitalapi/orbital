alter table CASK_CONFIG
    add exposestype boolean default false not null;

alter table CASK_CONFIG
    add exposesservice boolean default true not null;

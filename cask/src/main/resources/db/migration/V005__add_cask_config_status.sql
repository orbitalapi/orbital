alter table cask_config
    add status varchar(32) default 'ACTIVE';

alter table cask_config
    add replacedBy varchar(32) null;


alter table CASK_MESSAGE
    drop readCachePath;

alter table CASK_MESSAGE
    add messageId int;

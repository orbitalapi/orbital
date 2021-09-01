alter table cask_config
    add column IF NOT EXISTS days_to_retain integer default 100000;


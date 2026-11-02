-- One row per event ID, so a repeated request can't send twice. No addresses and no message text.
create table email_log (
    event_id    varchar(120) primary key,
    event_type  varchar(80)  not null,
    status      varchar(20)  not null,
    attempts    integer      not null default 0,
    created_at  timestamp    not null,
    updated_at  timestamp    not null,
    sent_at     timestamp
);

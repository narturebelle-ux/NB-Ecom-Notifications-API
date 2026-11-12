-- Resend's ID for each sent email, to trace a complaint or a bounce. An ID only: still no address or message text.
alter table email_log add column provider_message_id varchar(100);

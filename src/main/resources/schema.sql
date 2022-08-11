-- contains the individual twitter accounts this system can manage
create table if not exists twitter_accounts
(
    username      varchar(1000) not null,
    access_token  text not null,
    refresh_token text not null,
    created       timestamp     not null  ,
    updated       timestamp     not null ,
    primary key (username)
);

create table if not exists twitter_clients
(
    client_id  text not null,
    secret  text not null,
    primary key (client_id)
);

create table if not exists twitter_scheduled_tweets
(
    username     varchar(1000) not null references twitter_accounts (username),
    json_request text          not null,
    scheduled    timestamp     not null,
    client_id    varchar(1000) not null references twitter_clients (client_id),
    sent         timestamp     null,
    primary key (username, client_id, scheduled, json_request)
);
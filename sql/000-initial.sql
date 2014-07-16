create schema common;
grant all on schema common to ladders;

create table common.user (
    id bigserial primary key,
    username varchar(64),
    password varchar(61)
);
grant all on common.user to ladders;
grant all on common.user_id_seq to ladders;

create table common.post (
    id bigserial primary key,
    creator_user bigint not null references common.user(id),
    content text
);
grant all on common.post to ladders;
grant all on common.post_id_seq to ladders;

create table common.comment (
    id bigserial primary key,
    post_id bigint not null references common.post(id),
    ord int not null,
    content text
);
grant all on common.comment to ladders;
grant all on common.comment_id_seq to ladders;

create table common.challenge (
    id bigserial primary key,
    challenger_user bigint not null references common.user(id),
    opponent_user bigint not null references common.user(id),
    post_id bigint not null references common.post(id),
    challenge_date timestamp not null,
    accepted_date timestamp
);
grant all on common.challenge to ladders;
grant all on common.challenge_id_seq to ladders;

create table common.challenge_resolution (
    challenge_id bigint not null references common.challenge(id),
    winner_user bigint not null references common.user(id),
    resolution_date timestamp not null
);
grant all on common.challenge_resolution to ladders;

create table common.ladder (
);
grant all on common.ladder to ladders;

create table common.user_ladder (
);
grant all on common.user_ladder to ladders;
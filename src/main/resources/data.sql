create table if not exists user_table
(
    id         bigint       not null
        primary key,
    first_name varchar(40),
    last_name  varchar(60),
    email      varchar(255) not null
        unique,
    password   varchar(255),
    role       varchar(255)
        constraint user_table_role_check
            check ((role)::text = ANY ((ARRAY ['USER'::character varying, 'ADMIN'::character varying])::text[]))
);

create table if not exists task
(
    author_id   bigint
        constraint fknubdxo1xrrq1581v80b7mnhxh
            references user_table,
    created_at  timestamp(6),
    executor_id bigint
        constraint fkfrin7ema3xvk43ku7ykkjb2bn
            references user_table,
    id          bigint       not null
        primary key,
    updated_at  timestamp(6),
    description varchar(255),
    priority    varchar(255) not null
        constraint task_priority_check
            check ((priority)::text = ANY
                   ((ARRAY ['HIGH'::character varying, 'REGULAR'::character varying, 'LOW'::character varying])::text[])),
    status      varchar(255) not null
        constraint task_status_check
            check ((status)::text = ANY
                   ((ARRAY ['ON_HOLD'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying])::text[])),
    title       varchar(255)
);

create table if not exists comment
(
    author_id  bigint
        constraint fkr2rtfnh4t1lmb7l6b3nms48ta
            references user_table,
    created_at timestamp(6) not null,
    id         bigint       not null
        primary key,
    task_id    bigint
        constraint fkfknte4fhjhet3l1802m1yqa50
            references task,
    updated_at timestamp(6),
    content    varchar(255)
);

create index if not exists email_idx
    on user_table (email);


create sequence if not exists user_table_seq
    increment by 50;

create sequence if not exists comment_seq
    increment by 50;

create sequence if not exists task_seq
    increment by 50;











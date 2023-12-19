create table if not exists user_table
(
    id         bigint not null
        primary key,
    first_name varchar(40),
    last_name  varchar(60),
    email      varchar(255),
    password   varchar(255),
    role       varchar(255)
        constraint user_table_role_check
            check ((role)::text = ANY ((ARRAY ['USER'::character varying, 'ADMIN'::character varying])::text[]))
    );

create table if not exists user_tasks_as_author
(
    tasks_as_author bigint,
    user_id         bigint not null
        constraint fkmdv7yqcc6r1obnjrni2dsdfl1
            references user_table
);

create table if not exists user_tasks_as_executor
(
    tasks_as_executor bigint,
    user_id           bigint not null
        constraint fkolnwoqa76rdc2807kla8n2cul
            references user_table
);

create table if not exists task
(
    author_id   bigint       not null,
    created_at  timestamp(6),
    executor_id bigint,
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
    author_id  bigint not null,
    created_at timestamp(6),
    id         bigint not null
        primary key,
    task_id    bigint not null,
    updated_at timestamp(6),
    content    varchar(255)
);

create sequence if not exists user_table_seq
    increment by 50;

create sequence if not exists comment_seq
    increment by 50;

create sequence if not exists task_seq
    increment by 50;











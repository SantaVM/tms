CREATE SEQUENCE if not exists comment_seq START WITH 2 INCREMENT BY 1;
CREATE SEQUENCE if not exists task_seq START WITH 2 INCREMENT BY 1;
CREATE SEQUENCE if not exists user_table_seq START WITH 2 INCREMENT BY 1;

CREATE TABLE if not exists user_table
(
    id        BIGINT NOT NULL,
    first_name VARCHAR(40),
    last_name  VARCHAR(60),
    email     VARCHAR(255),
    password  VARCHAR(255),
    role      VARCHAR(255) check (role in ('USER','ADMIN')),
    CONSTRAINT pk_user_table PRIMARY KEY (id)
);

CREATE TABLE if not exists comment
(
    id        BIGINT NOT NULL,
    task_id   BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content   VARCHAR(255),
    created_at TIMESTAMP,
    updated_at   TIMESTAMP,
    CONSTRAINT pk_comment PRIMARY KEY (id)
);

CREATE TABLE if not exists task
(
    id          BIGINT       NOT NULL,
    title       VARCHAR(255),
    description VARCHAR(255),
    status      VARCHAR(255) NOT NULL check (status in ('ON_HOLD','IN_PROGRESS','COMPLETED')),
    priority    VARCHAR(255) NOT NULL check (priority in ('HIGH','REGULAR','LOW')),
    author_id   BIGINT       NOT NULL,
    executor_id BIGINT,
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP,
    CONSTRAINT pk_task PRIMARY KEY (id)
);

ALTER TABLE user_table
    ADD CONSTRAINT uc_user_table_email UNIQUE (email);

CREATE INDEX email_idx ON user_table (email);

ALTER TABLE task
    ADD CONSTRAINT FK_TASK_ON_AUTHOR FOREIGN KEY (author_id) REFERENCES user_table (id);

ALTER TABLE task
    ADD CONSTRAINT FK_TASK_ON_EXECUTOR FOREIGN KEY (executor_id) REFERENCES user_table (id);

ALTER TABLE comment
    ADD CONSTRAINT FK_COMMENT_ON_AUTHOR FOREIGN KEY (author_id) REFERENCES user_table (id);

ALTER TABLE comment
    ADD CONSTRAINT FK_COMMENT_ON_TASK FOREIGN KEY (task_id) REFERENCES task (id);

-- NEXTVAL('user_table_seq')

insert into user_table
(email, first_name, last_name, password, role, id)
values
    ('adm@site.com', 'Admin', 'Admin', '$2a$10$.sGf.fZ2GVXmYIU35E5wSOMRbPj2A1i.nBeJF2De9OHj6hv8DSo2O', 'ADMIN', 1);

insert into task
(author_id, created_at, description, executor_id, priority, status, title, updated_at, id)
values
    (1, CURRENT_TIMESTAMP, 'First description', 1, 'HIGH', 'COMPLETED', 'The first task', current_timestamp, 1);

insert into comment
(author_id, content, created_at, task_id, id)
values
    (1, 'some content', current_timestamp, 1, 1);

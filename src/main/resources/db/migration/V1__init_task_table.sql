CREATE TABLE task (
                      id                BIGSERIAL PRIMARY KEY,
                      creator_id        BIGINT NOT NULL,
                      assigned_user_id  BIGINT,
                      status            VARCHAR(20) NOT NULL,
                      create_date_time  TIMESTAMP NOT NULL,
                      deadline_date     TIMESTAMP NOT NULL,
                      priority          VARCHAR(20) NOT NULL,
                      done_date_time    TIMESTAMP,
                      version           BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_task_creator_id ON task (creator_id);
CREATE INDEX idx_task_assigned_user_id ON task (assigned_user_id);
CREATE INDEX idx_task_status ON task (status);
CREATE INDEX idx_task_priority ON task (priority);
CREATE INDEX idx_task_assigned_user_status ON task (assigned_user_id, status);
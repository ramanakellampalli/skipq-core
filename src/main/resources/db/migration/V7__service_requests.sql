CREATE TABLE service_requests (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES users(id),
    role            VARCHAR(10)  NOT NULL,
    type            VARCHAR(30)  NOT NULL,
    subject         VARCHAR(255) NOT NULL,
    description     TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    admin_response  TEXT,
    admin_notes     TEXT,
    admin_responded_at TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_service_requests_user_id ON service_requests(user_id);
CREATE INDEX idx_service_requests_status  ON service_requests(status);

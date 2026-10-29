-- Additive records; existing orders, stock and financial entries are unchanged.
CREATE TABLE order_aftercare (
    order_id bigint NOT NULL PRIMARY KEY REFERENCES orders(id),
    flower_photo_url varchar(500), flower_photo_public_id varchar(255),
    photo_revision integer NOT NULL DEFAULT 0,
    photo_status varchar(30) NOT NULL DEFAULT 'NONE',
    florist_note varchar(500), photo_uploaded_at timestamp, photo_uploaded_by varchar(100),
    approval_comment varchar(500), photo_reviewed_at timestamp,
    delivery_photo_url varchar(500), delivery_photo_public_id varchar(255),
    delivery_note varchar(500), delivery_recorded_at timestamp, delivery_recorded_by varchar(100),
    customer_received_at timestamp,
    complaint_status varchar(30) NOT NULL DEFAULT 'NONE', complaint_reason varchar(30),
    complaint_description varchar(1000), complaint_photo_url varchar(500), complaint_photo_public_id varchar(255),
    complaint_created_at timestamp, support_response varchar(1000), support_updated_at timestamp, support_updated_by varchar(100),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_order_aftercare_photo_status CHECK (photo_status IN ('NONE','WAITING','APPROVED','CHANGES_REQUESTED')),
    CONSTRAINT ck_order_aftercare_complaint_status CHECK (complaint_status IN ('NONE','OPEN','IN_REVIEW','RESOLVED','REJECTED')),
    CONSTRAINT ck_order_aftercare_photo_revision CHECK (photo_revision >= 0)
);

-- Additive records; existing orders, stock and financial entries are unchanged.
CREATE TABLE dbo.order_aftercare (
    order_id bigint NOT NULL PRIMARY KEY REFERENCES dbo.orders(id),
    flower_photo_url varchar(500), flower_photo_public_id varchar(255),
    photo_revision int NOT NULL DEFAULT 0,
    photo_status varchar(30) NOT NULL DEFAULT 'NONE',
    florist_note nvarchar(500), photo_uploaded_at datetime2, photo_uploaded_by varchar(100),
    approval_comment nvarchar(500), photo_reviewed_at datetime2,
    delivery_photo_url varchar(500), delivery_photo_public_id varchar(255),
    delivery_note nvarchar(500), delivery_recorded_at datetime2, delivery_recorded_by varchar(100),
    customer_received_at datetime2,
    complaint_status varchar(30) NOT NULL DEFAULT 'NONE', complaint_reason varchar(30),
    complaint_description nvarchar(1000), complaint_photo_url varchar(500), complaint_photo_public_id varchar(255),
    complaint_created_at datetime2, support_response nvarchar(1000), support_updated_at datetime2, support_updated_by varchar(100),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT CK_order_aftercare_photo_status CHECK (photo_status IN ('NONE','WAITING','APPROVED','CHANGES_REQUESTED')),
    CONSTRAINT CK_order_aftercare_complaint_status CHECK (complaint_status IN ('NONE','OPEN','IN_REVIEW','RESOLVED','REJECTED')),
    CONSTRAINT CK_order_aftercare_photo_revision CHECK (photo_revision >= 0)
);

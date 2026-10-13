-- Product snapshots are attached only when the customer sends a question.
-- Existing messages, inventories and orders are unchanged.
IF OBJECT_ID(N'dbo.chat_product_contexts', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.chat_product_contexts (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_chat_product_contexts PRIMARY KEY,
        message_id BIGINT NOT NULL CONSTRAINT UQ_chat_product_context_message UNIQUE,
        product_id BIGINT NOT NULL,
        product_name NVARCHAR(200) NOT NULL,
        product_image_url VARCHAR(500) NULL,
        product_price DECIMAL(38,2) NOT NULL,
        CONSTRAINT FK_chat_product_context_message FOREIGN KEY (message_id) REFERENCES dbo.chat_messages(id)
    );
END;

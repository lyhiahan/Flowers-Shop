-- Additive SQL Server migration. Existing conversations and account data are retained.
IF OBJECT_ID(N'dbo.chat_conversations', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.chat_conversations (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_chat_conversations PRIMARY KEY,
        conversation_key VARCHAR(100) NOT NULL,
        kind VARCHAR(16) NOT NULL,
        customer_id BIGINT NOT NULL,
        store_id BIGINT NULL,
        last_message NVARCHAR(2000) NULL,
        created_at DATETIME2 NOT NULL,
        updated_at DATETIME2 NOT NULL,
        CONSTRAINT UQ_chat_conversations_key UNIQUE (conversation_key),
        CONSTRAINT FK_chat_conversations_customer FOREIGN KEY (customer_id) REFERENCES dbo.users(id),
        CONSTRAINT FK_chat_conversations_store FOREIGN KEY (store_id) REFERENCES dbo.stores(id)
    );
END;

IF OBJECT_ID(N'dbo.chat_messages', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.chat_messages (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_chat_messages PRIMARY KEY,
        conversation_id BIGINT NOT NULL,
        sender_id BIGINT NOT NULL,
        client_message_id VARCHAR(36) NOT NULL,
        content NVARCHAR(2000) NOT NULL,
        image_url VARCHAR(500) NULL,
        image_public_id VARCHAR(255) NULL,
        created_at DATETIME2 NOT NULL,
        CONSTRAINT UQ_chat_messages_retry UNIQUE (conversation_id, sender_id, client_message_id),
        CONSTRAINT FK_chat_messages_conversation FOREIGN KEY (conversation_id) REFERENCES dbo.chat_conversations(id),
        CONSTRAINT FK_chat_messages_sender FOREIGN KEY (sender_id) REFERENCES dbo.users(id)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.chat_messages') AND name = N'idx_chat_messages_conversation_id')
    CREATE INDEX idx_chat_messages_conversation_id ON dbo.chat_messages (conversation_id, id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.chat_conversations') AND name = N'idx_chat_conversations_customer_updated')
    CREATE INDEX idx_chat_conversations_customer_updated ON dbo.chat_conversations (customer_id, updated_at DESC, id DESC);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.chat_conversations') AND name = N'idx_chat_conversations_store_updated')
    CREATE INDEX idx_chat_conversations_store_updated ON dbo.chat_conversations (store_id, updated_at DESC, id DESC);

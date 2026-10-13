-- Additive only: do not alter or remove existing conversations, messages or orders.
IF OBJECT_ID(N'dbo.chat_access_audits', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.chat_access_audits (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_chat_access_audits PRIMARY KEY,
        admin_id BIGINT NOT NULL,
        conversation_id BIGINT NOT NULL,
        accessed_at DATETIME2(7) NOT NULL,
        before_message_id BIGINT NULL,
        CONSTRAINT FK_chat_audit_admin FOREIGN KEY (admin_id) REFERENCES dbo.users(id),
        CONSTRAINT FK_chat_audit_conversation FOREIGN KEY (conversation_id) REFERENCES dbo.chat_conversations(id)
    );
END;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.chat_access_audits') AND name=N'idx_chat_audit_admin_time')
    CREATE INDEX idx_chat_audit_admin_time ON dbo.chat_access_audits(admin_id, accessed_at);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'dbo.chat_access_audits') AND name=N'idx_chat_audit_conversation_time')
    CREATE INDEX idx_chat_audit_conversation_time ON dbo.chat_access_audits(conversation_id, accessed_at);

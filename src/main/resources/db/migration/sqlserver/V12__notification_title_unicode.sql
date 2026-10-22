-- Legacy databases may predate the nationalized Notification.title mapping.
IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.notifications')
      AND name = N'title'
      AND system_type_id <> TYPE_ID(N'nvarchar')
)
BEGIN
    ALTER TABLE dbo.notifications ALTER COLUMN title nvarchar(200) NOT NULL;
END;

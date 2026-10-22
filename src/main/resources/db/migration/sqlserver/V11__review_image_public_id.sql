IF COL_LENGTH('dbo.reviews', 'image_public_id') IS NULL
BEGIN
    ALTER TABLE dbo.reviews ADD image_public_id NVARCHAR(255) NULL;
END;

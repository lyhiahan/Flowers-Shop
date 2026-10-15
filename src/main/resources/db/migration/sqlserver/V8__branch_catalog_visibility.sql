-- Branch-level controls for shared products. Existing stock quantities and order history stay intact.
ALTER TABLE dbo.store_inventories ADD
    is_selling BIT NOT NULL CONSTRAINT DF_store_inventory_selling DEFAULT (1),
    is_deleted BIT NOT NULL CONSTRAINT DF_store_inventory_deleted DEFAULT (0);

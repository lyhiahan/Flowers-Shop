// Build an explicit recovery whitelist from the project's existing seed catalog.
// Run with Node; does not connect to or change the database.
const fs = require('node:fs');
const source = fs.readFileSync('src/main/java/com/finalproject/service/ProductSeederService.java', 'utf8');
const templates = [...source.matchAll(/new ProductTemplate\(\s*"([^"\n]+)"\s*,[\s\S]*?new BigDecimal\("([0-9.]+)"\)/g)]
    .map(match => ({ name: match[1], price: match[2] }));
if (templates.length !== 27 || new Set(templates.map(t => t.name)).size !== 27) throw new Error('Seed catalog changed; review whitelist before repairing.');
const literal = text => "N'" + text.replaceAll("'", "''") + "'";
const values = templates.map(t => `(${literal(t.name)}, ${t.price})`).join(',\n');
const sql = `-- Generated from ProductSeederService.java by scripts/build-product-unicode-repair.cjs.
-- Only restores unambiguous seed names whose preserved characters AND price match.
-- Does not delete products or change their price, stock, store, images or orders.
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF EXISTS (SELECT 1 FROM sys.columns c JOIN sys.types t ON c.user_type_id=t.user_type_id
           WHERE c.object_id=OBJECT_ID('dbo.products') AND c.name='name' AND t.name <> 'nvarchar')
    ALTER TABLE dbo.products ALTER COLUMN name NVARCHAR(200) NOT NULL;

DECLARE @catalog TABLE (correct_name NVARCHAR(200) NOT NULL PRIMARY KEY, price DECIMAL(18,2) NOT NULL);
INSERT INTO @catalog (correct_name,price) VALUES
${values};

-- Escape existing LIKE metacharacters; only the lost '?' positions are wildcards.
SELECT p.id, CONVERT(NVARCHAR(200),p.name) AS previous_name, MIN(c.correct_name) AS restored_name
INTO #repairs
FROM dbo.products p JOIN @catalog c ON p.price_buy=c.price
 AND LEN(p.name)=LEN(c.correct_name)
 AND c.correct_name COLLATE Latin1_General_100_BIN2 LIKE
     REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(CONVERT(NVARCHAR(200),p.name),N'!',N'!!'),N'%',N'!%'),N'_',N'!_'),N'[',N'!['),N'?',N'_') COLLATE Latin1_General_100_BIN2 ESCAPE N'!'
WHERE CHARINDEX(N'?',p.name)>0
GROUP BY p.id,p.name HAVING COUNT(*)=1;

IF OBJECT_ID('dbo.product_name_unicode_repair_backup','U') IS NULL
    CREATE TABLE dbo.product_name_unicode_repair_backup (
        product_id BIGINT NOT NULL PRIMARY KEY,
        previous_name NVARCHAR(200) NOT NULL,
        restored_name NVARCHAR(200) NOT NULL,
        repaired_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
    );
INSERT INTO dbo.product_name_unicode_repair_backup (product_id,previous_name,restored_name)
SELECT r.id,r.previous_name,r.restored_name FROM #repairs r
WHERE NOT EXISTS (SELECT 1 FROM dbo.product_name_unicode_repair_backup b WHERE b.product_id=r.id);
UPDATE p SET name=r.restored_name FROM dbo.products p JOIN #repairs r ON p.id=r.id;
SELECT COUNT(*) AS restored_product_names FROM #repairs;
SELECT p.id,p.name FROM dbo.products p JOIN #repairs r ON p.id=r.id ORDER BY p.id;
SELECT COUNT(*) AS unmatched_question_mark_names FROM dbo.products WHERE CHARINDEX(N'?',name)>0;
COMMIT TRANSACTION;
`;
fs.writeFileSync('sql/migrations/20261003_product_unicode.sql', sql, 'utf8');
console.log(`Generated recovery whitelist for ${templates.length} seed names.`);

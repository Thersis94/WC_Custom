select * from PRODUCT_ATTRIBUTE_XR where VALUE_TXT like '%"""%'


update PRODUCT set PRODUCT_NM = REPLACE(product_nm, '"""', '^')
go
update PRODUCT set PRODUCT_NM = REPLACE(product_nm, '""', '^')
go
update PRODUCT set PRODUCT_NM = REPLACE(product_nm, '"', '')
go
update PRODUCT set PRODUCT_NM = REPLACE(product_nm, '^', '"')
go
update PRODUCT set desc_txt = cast(replace(cast(desc_txt as nvarchar(max)),'"""', '^') as ntext)
go
update PRODUCT set desc_txt = cast(replace(cast(desc_txt as nvarchar(max)),'""', '^') as ntext)
go
update PRODUCT set desc_txt = cast(replace(cast(desc_txt as nvarchar(max)),'"', '') as ntext)
go
update PRODUCT set desc_txt = cast(replace(cast(desc_txt as nvarchar(max)),'^', '"') as ntext)
go
update PRODUCT_ATTRIBUTE_XR set VALUE_TXT = REPLACE(VALUE_TXT, '"""', '^')
go
update PRODUCT_ATTRIBUTE_XR set VALUE_TXT = REPLACE(VALUE_TXT, '""', '^')
go
update PRODUCT_ATTRIBUTE_XR set VALUE_TXT = REPLACE(VALUE_TXT, '"', '')
go
update PRODUCT_ATTRIBUTE_XR set VALUE_TXT = REPLACE(VALUE_TXT, '^', '"')
go
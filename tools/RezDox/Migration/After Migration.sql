ALTER TABLE custom.REZDOX_RESIDENCE_ATTRIBUTE RENAME FORM_FIELD_ID TO SLUG_TXT;
ALTER TABLE custom.REZDOX_RESIDENCE_ATTRIBUTE ALTER COLUMN SLUG_TXT TYPE Varchar(50);

ALTER TABLE custom.REZDOX_BUSINESS_ATTRIBUTE RENAME FORM_FIELD_ID TO SLUG_TXT;
ALTER TABLE custom.REZDOX_BUSINESS_ATTRIBUTE ALTER COLUMN SLUG_TXT TYPE Varchar(50);

ALTER TABLE custom.REZDOX_PROJECT_ATTRIBUTE RENAME FORM_FIELD_ID TO SLUG_TXT;
ALTER TABLE custom.REZDOX_PROJECT_ATTRIBUTE ALTER COLUMN SLUG_TXT TYPE Varchar(50);

ALTER TABLE custom.REZDOX_PROJECT_MATERIAL_ATTRIBUTE RENAME FORM_FIELD_ID TO SLUG_TXT;
ALTER TABLE custom.REZDOX_PROJECT_MATERIAL_ATTRIBUTE ALTER COLUMN SLUG_TXT TYPE Varchar(50);

ALTER TABLE custom.REZDOX_TREASURE_ITEM_ATTRIBUTE RENAME FORM_FIELD_ID TO SLUG_TXT;
ALTER TABLE custom.REZDOX_TREASURE_ITEM_ATTRIBUTE ALTER COLUMN SLUG_TXT TYPE Varchar(50);

update custom.rezdox_residence_attribute set slug_txt = 'lastSoldPrice' where slug_txt = 'RESIDENCE_PURCHASE_PRICE';
update custom.rezdox_residence_attribute set slug_txt = 'bedrooms' where slug_txt = 'RESIDENCE_BEDS';
update custom.rezdox_residence_attribute set slug_txt = 'bathrooms' where slug_txt = 'RESIDENCE_BATHS';
update custom.rezdox_residence_attribute set slug_txt = 'finishedSqFt' where slug_txt = 'RESIDENCE_F_SQFT';
update custom.rezdox_residence_attribute set slug_txt = 'unfinishedSqFt' where slug_txt = 'RESIDENCE_UF_SQFT';

-- Fix business category names with trailing new lines
update custom.rezdox_business_category
set category_nm = regexp_replace(category_nm, '\r|\n', '', 'g');

-- Business data fixes to match expected form builder values
update custom.rezdox_business_attribute
set value_txt = '1'
where value_txt = 'true' and slug_txt in ('BUSINESS_BONDED', 'BUSINESS_LICENSED', 'BUSINESS_BBB_ACCRED', 'BUSINESS_INSURED', 'BUSINESS_WARRANTIES');

update custom.rezdox_business_attribute
set value_txt = '0'
where value_txt = 'false' and slug_txt in ('BUSINESS_BONDED', 'BUSINESS_LICENSED', 'BUSINESS_BBB_ACCRED', 'BUSINESS_INSURED', 'BUSINESS_WARRANTIES');

update custom.rezdox_business_attribute
set slug_txt = concat('BUSINESS_SOCIALMEDIA_', upper(value_txt)), value_txt = 1
where slug_txt like 'BUSINESS_SOCIALMEDIA_%';

update custom.rezdox_business_attribute
set slug_txt = concat('BUSINESS_PAYMENTS_', upper(value_txt)), value_txt = 1
where slug_txt like 'BUSINESS_PAYMENTS_%';

insert into custom.rezdox_business_attribute (attribute_id, business_id, slug_txt, value_txt, create_dt)
select replace(newid(),'-',''), business_id, concat('BUSINESS_HOURS_OPS_START_', upper(split_part(value_txt, '||', 1))), concat(lpad(case split_part(value_txt, '||', 2) when '12' then '00' else split_part(value_txt, '||', 2) end, 2, '0'), ':', split_part(value_txt, '||', 3)), create_dt
from custom.rezdox_business_attribute
where slug_txt like 'BUSINESS_HOURS_OPS_%' and split_part(value_txt, '||', 2) != '0' and strpos(value_txt, '||') > 0;

insert into custom.rezdox_business_attribute (attribute_id, business_id, slug_txt, value_txt, create_dt)
select replace(newid(),'-',''), business_id, concat('BUSINESS_HOURS_OPS_END_', upper(split_part(value_txt, '||', 1))), concat(case split_part(value_txt, '||', 5) when '12' then '12' else cast(split_part(value_txt, '||', 5) as integer) + 12 end, ':', split_part(value_txt, '||', 6)), create_dt
from custom.rezdox_business_attribute
where slug_txt like 'BUSINESS_HOURS_OPS_%' and split_part(value_txt, '||', 2) != '0' and strpos(value_txt, '||') > 0;

delete from custom.rezdox_business_attribute
where slug_txt in ('BUSINESS_HOURS_OPS_0', 'BUSINESS_HOURS_OPS_1', 'BUSINESS_HOURS_OPS_2', 'BUSINESS_HOURS_OPS_3', 'BUSINESS_HOURS_OPS_4', 'BUSINESS_HOURS_OPS_5', 'BUSINESS_HOURS_OPS_6');


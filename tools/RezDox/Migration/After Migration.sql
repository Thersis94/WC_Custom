-- Change FORM_FIELD_ID to SLUG_TXT
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


-- Set slug_txt expected by incoming zillow data
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


-- Fix image and document paths in migrated data
update custom.rezdox_business set ad_file_url = null where ad_file_url = '0';
update custom.rezdox_business set photo_url = null where photo_url = '0';
update custom.rezdox_business set photo_url = concat('/legacy/profile/full/', photo_url) where photo_url is not null;
update custom.rezdox_business set ad_file_url = concat('/legacy/ads/', ad_file_url) where ad_file_url is not null;
update custom.rezdox_residence set profile_pic_pth = concat('/legacy/profile/full/', profile_pic_pth) where profile_pic_pth is not null;
update custom.rezdox_member set profile_pic_pth = concat('/legacy/profile/full/', profile_pic_pth) where profile_pic_pth is not null;
update custom.rezdox_reward set image_url = concat('/binary/org/REZDOX/legacy/rezrewards/', image_url) where image_url is not null;
update custom.rezdox_document set file_pth = concat('/legacy/history/files/', document_nm) where project_id is not null;
update custom.rezdox_document set file_pth = concat('/legacy/tbox/files/', document_nm) where treasure_item_id is not null;
update custom.rezdox_photo set image_url = concat('/legacy/album/full/', album_id, '/', split_part(image_url, '/', 6)) where album_id is not null;
update custom.rezdox_photo set image_url = concat('/legacy/tbox/full/', photo_nm) where treasure_item_id is not null;
update custom.rezdox_photo set image_url = concat('/legacy/history/full/', photo_nm) where project_id is not null;


-- Delete residence room slugs, this data is stored in the room table
delete from custom.rezdox_residence_attribute where slug_txt = 'RESIDENCE_ROOM_DETAILS';


-- Fill in empty zestimate values
update custom.rezdox_residence_attribute set value_txt = '0' where slug_txt = 'RESIDENCE_ZESTIMATE' and value_txt = '';


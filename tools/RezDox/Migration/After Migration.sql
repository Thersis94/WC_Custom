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


-- Give all rooms a name if they don't have one, based on the room type name
update custom.rezdox_room set room_nm = rt.type_nm
from custom.rezdox_room_type rt
where custom.rezdox_room.room_type_cd = rt.room_type_cd and room_nm is null;


--move the documents tied to treasure items over to the photos table.
INSERT INTO custom.rezdox_photo (photo_id, album_id, treasure_item_id, project_id, photo_nm, desc_txt, image_url, thumbnail_url, order_no, create_dt, update_dt)
SELECT document_id, null, treasure_item_id, project_id, document_nm, description_txt, file_pth, null,null,create_dt, update_dt
from custom.rezdox_document where treasure_item_id is not null;

--delete the documents just migrated to the photos table.
delete from custom.rezdox_document where treasure_item_id is not null;

--drop the link between the treasure_item and document tables
alter table custom.rezdox_document drop treasure_item_id;

--drop beneficiary_member_id from treasure_item and replace with a text field
alter table custom.rezdox_treasure_item drop beneficiary_member_id;
alter table custom.rezdox_treasure_item add beneficiary_nm varchar(100);

--populate the treasure_item.beneficiary_nm column using the data from the _attribute table.
update custom.rezdox_treasure_item a set beneficiary_nm=b.value_txt
from custom.rezdox_treasure_item_attribute b
where a.treasure_item_id=b.treasure_item_id and b.value_txt != '-' and b.slug_txt='BENEFICIARY';

--drop the above data from the _attribute table, so it's not duplicate & orphan.
delete from custom.rezdox_treasure_item_attribute where slug_txt='BENEFICIARY';

--purge legacy data placeholders that are meaningless and inconvenience users
delete from custom.rezdox_treasure_item_attribute where value_txt='' or value_txt='-' or value_txt='1970-01-01';

--move the documents tied to projects over to the photos table.
INSERT INTO custom.rezdox_photo (photo_id, album_id, treasure_item_id, project_id, photo_nm, desc_txt, image_url, thumbnail_url, order_no, create_dt, update_dt)
SELECT document_id, null, null, project_id, document_nm, description_txt, file_pth, null,null,create_dt, update_dt
from custom.rezdox_document where project_id is not null;

--delete the documents just migrated to the photos table.
delete from custom.rezdox_document where project_id is not null;

--drop the document table - it's empty now.
drop table custom.rezdox_document;

-- new fields in project
alter table custom.rezdox_project add END_DT timestamp;
alter table custom.rezdox_project add DESC_TXT varchar;

--bring endDate over from create or update dates...that's all we know?
update custom.rezdox_project set end_dt=coalesce(update_dt, create_dt) where end_dt is null;

-- Add moderated flag to business reviews
alter table custom.rezdox_member_business_review add moderated_flg Integer Default 0;

-- cleanup
update custom.rezdox_project_type set type_nm=replace(type_nm,'&amp;','&');
update custom.rezdox_treasure_category set category_nm=replace(category_nm,'&amp;','&');
delete from custom.rezdox_project_attribute where value_txt='' or value_txt='-' or value_txt=null or value_txt='()-';
update custom.rezdox_project set desc_txt=replace(desc_txt,'&amp;','&');
update custom.rezdox_project_attribute set value_txt=replace(value_txt,'&amp;','&');
update custom.rezdox_treasure_item_attribute set value_txt=replace(value_txt,'&amp;','&');
update custom.rezdox_project set project_nm=replace(project_nm,'&amp;','&');
update custom.rezdox_project_attribute set slug_txt='PROJECT_OWNER' where slug_txt='PROJECT_WRITEIN1';
alter table custom.rezdox_project alter column residence_id drop not null;
update custom.rezdox_project_attribute set slug_txt='PROJECT_OWNER' where slug_txt='PROJECT_PROVIDER';

-- updates to data model
alter table custom.rezdox_project add proj_discount_no decimal(7,5);
alter table custom.rezdox_project add proj_tax_no decimal(7,5);
alter table custom.rezdox_project add mat_discount_no decimal(7,5);
alter table custom.rezdox_project add mat_tax_no decimal(7,5);
alter table custom.REZDOX_RESIDENCE_MEMBER_XR add update_dt timestamp;

-- add profile data to member for easier searching
alter table custom.rezdox_member add first_nm varchar(60);
alter table custom.rezdox_member add last_nm varchar(80);
alter table custom.rezdox_member add email_address_txt varchar(250);

-- fill in empty values from api
update custom.rezdox_residence_attribute set value_txt = '0' where slug_txt = 'RESIDENCE_WALK_SCORE' and value_txt = '';
update custom.rezdox_residence_attribute set value_txt = '0' where slug_txt = 'RESIDENCE_TRANSIT_SCORE' and value_txt = '';
update custom.rezdox_residence_attribute set value_txt = '0' where slug_txt = 'RESIDENCE_SUN_NUMBER' and value_txt = '';
update custom.rezdox_residence_attribute set value_txt = '0' where slug_txt = 'RESIDENCE_SUN_NUMBER' and value_txt is null;

-- changes to memberships, using button ids instead of full button html
-- this query only needs to be run in staging after code push
-- alter table custom.rezdox_membership drop paypal_button_txt;

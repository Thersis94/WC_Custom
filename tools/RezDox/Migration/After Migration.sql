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

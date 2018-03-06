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

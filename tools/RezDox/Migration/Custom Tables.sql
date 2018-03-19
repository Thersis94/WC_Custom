/*
Created		1/3/2018
Modified		1/12/2018
Project		
Model			
Company		
Author		
Version		
Database		PostgreSQL 8.1 
*/


/* Create Tables */


Create table custom.REZDOX_RESIDENCE
(
	RESIDENCE_ID Varchar(32) NOT NULL,
	RESIDENCE_NM Varchar(100) NOT NULL,
	ADDRESS_TXT Varchar(150),
	ADDRESS2_TXT Varchar(150),
	CITY_NM Varchar(50),
	STATE_CD Varchar(25),
	COUNTRY_CD Varchar(3),
	ZIP_CD Varchar(10),
	LATITUDE_NO Double precision,
	LONGITUDE_NO Double precision,
	PROFILE_PIC_PTH Varchar(250),
	LAST_SOLD_DT Timestamp,
	FOR_SALE_DT Timestamp,
	PRIVACY_FLG Integer NOT NULL Default 0,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_REZDOX_RESIDENCE primary key (RESIDENCE_ID)
) Without Oids;


Create table custom.REZDOX_PROJECT
(
	PROJECT_ID Varchar(32) NOT NULL,
	RESIDENCE_ID Varchar(32) NOT NULL,
	ROOM_ID Varchar(32),
	BUSINESS_ID Varchar(32),
	PROJECT_CATEGORY_CD Varchar(32) NOT NULL,
	PROJECT_TYPE_CD Varchar(32) NOT NULL,
	PROJECT_NM Varchar(100) NOT NULL,
	LABOR_NO Double precision,
	TOTAL_NO Double precision,
	RESIDENCE_VIEW_FLG Integer NOT NULL Default 1,
	BUSINESS_VIEW_FLG Integer NOT NULL Default 1,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_REZDOX_PROJECT primary key (PROJECT_ID)
) Without Oids;


Create table custom.REZDOX_MEMBER
(
	MEMBER_ID Varchar(32) NOT NULL,
	PROFILE_ID Varchar(32) NOT NULL,
	REGISTER_SUBMITTAL_ID Varchar(32) NOT NULL,
	STATUS_FLG Integer NOT NULL Default 0,
	PRIVACY_FLG Integer NOT NULL Default 0,
	PROFILE_PIC_PTH Varchar(500),
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_REZDOX_MEMBER primary key (MEMBER_ID)
) Without Oids;


Create table custom.REZDOX_BUSINESS
(
	BUSINESS_ID Varchar(32) NOT NULL,
	BUSINESS_NM Varchar(150) NOT NULL,
	ADDRESS_TXT Varchar(150),
	ADDRESS2_TXT Varchar(150),
	CITY_NM Varchar(50),
	STATE_CD Varchar(2),
	ZIP_CD Varchar(10),
	COUNTRY_CD Varchar(3),
	LATITUDE_NO Double precision,
	LONGITUDE_NO Double precision,
	MAIN_PHONE_TXT Varchar(20),
	ALT_PHONE_TXT Varchar(20),
	EMAIL_ADDRESS_TXT Varchar(250),
	WEBSITE_URL Varchar(500),
	PHOTO_URL Varchar(250),
	AD_FILE_URL Varchar(250),
	PRIVACY_FLG Integer NOT NULL Default 0,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_REZDOX_BUSINESS primary key (BUSINESS_ID)
) Without Oids;


Create table custom.REZDOX_PHOTO
(
	PHOTO_ID Varchar(32) NOT NULL,
	ALBUM_ID Varchar(32),
	TREASURE_ITEM_ID Varchar(32),
	PROJECT_ID Varchar(32),
	PHOTO_NM Varchar(100) NOT NULL,
	DESC_TXT Varchar(1000),
	IMAGE_URL Varchar(500),
	THUMBNAIL_URL Varchar(500),
	ORDER_NO Integer Default 0,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_REZDOX_PHOTO primary key (PHOTO_ID)
) Without Oids;


Create table custom.REZDOX_RESIDENCE_ATTRIBUTE
(
	ATTRIBUTE_ID Varchar(32) NOT NULL,
	RESIDENCE_ID Varchar(32) NOT NULL,
	FORM_FIELD_ID Varchar(32) NOT NULL,
	VALUE_TXT Text,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_REZDOX_RESIDENCE_ATTRIBUTE primary key (ATTRIBUTE_ID)
) Without Oids;


Create table custom.REZDOX_RESIDENCE_MEMBER_XR
(
	RESIDENCE_MEMBER_XR_ID Varchar(32) NOT NULL,
	MEMBER_ID Varchar(32) NOT NULL,
	RESIDENCE_ID Varchar(32) NOT NULL,
	STATUS_FLG Integer NOT NULL Default 0,
	CREATE_DT Timestamp NOT NULL,
constraint pk_REZDOX_RESIDENCE_MEMBER_XR primary key (RESIDENCE_MEMBER_XR_ID)
) Without Oids;


Create table custom.REZDOX_PROJECT_ATTRIBUTE
(
	ATTRIBUTE_ID Varchar(32) NOT NULL,
	PROJECT_ID Varchar(32) NOT NULL,
	FORM_FIELD_ID Varchar(32) NOT NULL,
	VALUE_TXT Text,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_REZDOX_PROJECT_ATTRIBUTE primary key (ATTRIBUTE_ID)
) Without Oids;


Create table custom.REZDOX_BUSINESS_ATTRIBUTE
(
	ATTRIBUTE_ID Varchar(32) NOT NULL,
	BUSINESS_ID Varchar(32) NOT NULL,
	FORM_FIELD_ID Varchar(32) NOT NULL,
	VALUE_TXT Text,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_REZDOX_BUSINESS_ATTRIBUTE primary key (ATTRIBUTE_ID)
) Without Oids;


Create table custom.REZDOX_BUSINESS_CATEGORY
(
	BUSINESS_CATEGORY_CD Varchar(32) NOT NULL,
	PARENT_CD Varchar(32),
	CATEGORY_NM Varchar(100) NOT NULL,
	ORDER_NO Integer NOT NULL Default 0,
	CREATE_DT Timestamp NOT NULL,
constraint pk_REZDOX_BUSINESS_CATEGORY primary key (BUSINESS_CATEGORY_CD)
) Without Oids;


Create table custom.REZDOX_MEMBER_BUSINESS_REVIEW
(
	BUSINESS_REVIEW_ID Varchar(32) NOT NULL,
	MEMBER_ID Varchar(32) NOT NULL,
	BUSINESS_ID Varchar(32) NOT NULL,
	RATING_NO Integer NOT NULL,
	REVIEW_TXT Text,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_REZDOX_MEMBER_BUSINESS_REVIEW primary key (BUSINESS_REVIEW_ID)
) Without Oids;


Create table custom.REZDOX_PROJECT_CATEGORY
(
	PROJECT_CATEGORY_CD Varchar(32) NOT NULL,
	CATEGORY_NM Varchar(50) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
constraint pk_REZDOX_PROJECT_CATEGORY primary key (PROJECT_CATEGORY_CD)
) Without Oids;


Create table custom.REZDOX_PROJECT_TYPE
(
	PROJECT_TYPE_CD Varchar(32) NOT NULL,
	TYPE_NM Varchar(50) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
constraint pk_REZDOX_PROJECT_TYPE primary key (PROJECT_TYPE_CD)
) Without Oids;


Create table custom.REZDOX_ROOM_TYPE
(
	ROOM_TYPE_CD Varchar(32) NOT NULL,
	ROOM_CATEGORY_CD Varchar(32) NOT NULL,
	TYPE_NM Varchar(50) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
constraint pk_REZDOX_ROOM_TYPE primary key (ROOM_TYPE_CD)
) Without Oids;


Create table custom.REZDOX_PROJECT_MATERIAL
(
	PROJECT_MATERIAL_ID Varchar(32) NOT NULL,
	PROJECT_ID Varchar(32) NOT NULL,
	MATERIAL_NM Varchar(100) NOT NULL,
	QUANTITY_NO Integer NOT NULL Default 1,
	COST_NO Double precision NOT NULL Default 0.00,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_REZDOX_PROJECT_MATERIAL primary key (PROJECT_MATERIAL_ID)
) Without Oids;


Create table custom.REZDOX_PROJECT_MATERIAL_ATTRIBUTE
(
	ATTRIBUTE_ID Varchar(32) NOT NULL,
	PROJECT_MATERIAL_ID Varchar(32) NOT NULL,
	FORM_FIELD_ID Varchar(32) NOT NULL,
	VALUE_TXT Text,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_REZDOX_PROJECT_MATERIAL_ATTRIBUTE primary key (ATTRIBUTE_ID)
) Without Oids;


Create table custom.REZDOX_CONNECTION
(
	CONNECTION_ID Varchar(32) NOT NULL,
	SNDR_MEMBER_ID Varchar(32),
	RCPT_MEMBER_ID Varchar(32),
	SNDR_BUSINESS_ID Varchar(32),
	RCPT_BUSINESS_ID Varchar(32),
	APPROVED_FLG Integer,
	CREATE_DT Timestamp,
constraint pk_REZDOX_CONNECTION primary key (CONNECTION_ID)
) Without Oids;


Create table custom.REZDOX_MEMBERSHIP
(
	MEMBERSHIP_ID Varchar(32) NOT NULL,
	MEMBERSHIP_NM Varchar(50) NOT NULL,
	GROUP_CD Varchar(5) NOT NULL,
	STATUS_FLG Integer NOT NULL Default 0,
	COST_NO Double precision NOT NULL,
	QTY_NO Integer NOT NULL Default 1,
	NEW_MBR_DFLT_FLG Integer NOT NULL Default 0,
	PAYPAL_BUTTON_TXT Text,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_REZDOX_MEMBERSHIP primary key (MEMBERSHIP_ID)
) Without Oids;


Create table custom.REZDOX_BUSINESS_MEMBER_XR
(
	BUSINESS_MEMBER_XR_ID Varchar(32) NOT NULL,
	MEMBER_ID Varchar(32),
	BUSINESS_ID Varchar(32) NOT NULL,
	STATUS_FLG Integer NOT NULL Default 0,
	CREATE_DT Timestamp NOT NULL,
constraint pk_REZDOX_BUSINESS_MEMBER_XR primary key (BUSINESS_MEMBER_XR_ID)
) Without Oids;


Create table custom.REZDOX_TREASURE_ITEM
(
	TREASURE_ITEM_ID Varchar(32) NOT NULL,
	OWNER_MEMBER_ID Varchar(32) NOT NULL,
	RESIDENCE_ID Varchar(32),
	ROOM_ID Varchar(32),
	TREASURE_CATEGORY_CD Varchar(10),
	BENEFICIARY_MEMBER_ID Varchar(32),
	ITEM_NM Varchar(150) NOT NULL,
	VALUATION_NO Double precision,
	QUANTITY_NO Integer Default 1,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_REZDOX_TREASURE_ITEM primary key (TREASURE_ITEM_ID)
) Without Oids;


Create table custom.REZDOX_TREASURE_ITEM_ATTRIBUTE
(
	ATTRIBUTE_ID Varchar(32) NOT NULL,
	TREASURE_ITEM_ID Varchar(32) NOT NULL,
	FORM_FIELD_ID Varchar(32) NOT NULL,
	VALUE_TXT Text,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_REZDOX_TREASURE_ITEM_ATTRIBUTE primary key (ATTRIBUTE_ID)
) Without Oids;


Create table custom.REZDOX_MEMBER_MESSAGE
(
	MEMBER_MESSAGE_ID Varchar(32) NOT NULL,
	SNDR_MEMBER_ID Varchar(32),
	RCPT_MEMBER_ID Varchar(32),
	MESSAGE_ID Varchar(32) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
constraint pk_REZDOX_MEMBER_MESSAGE primary key (MEMBER_MESSAGE_ID)
) Without Oids;


Create table custom.REZDOX_NOTIFICATION
(
	NOTIFICATION_ID Varchar(32) NOT NULL,
	MEMBER_ID Varchar(32),
	BUSINESS_ID Varchar(32),
	MESSAGE_ID Varchar(32) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
constraint pk_REZDOX_NOTIFICATION primary key (NOTIFICATION_ID)
) Without Oids;


Create table custom.REZDOX_PAYMENT_TYPE
(
	PAYMENT_TYPE_ID Varchar(32) NOT NULL,
	TYPE_NM Varchar(30) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_REZDOX_PAYMENT_TYPE primary key (PAYMENT_TYPE_ID)
) Without Oids;


Create table custom.REZDOX_PROMOTION
(
	PROMOTION_ID Varchar(32) NOT NULL,
	PROMOTION_NM Varchar(100) NOT NULL,
	PROMOTION_CD Varchar(20),
	DESCRIPTION_TXT Varchar(250),
	TERMS_TXT Text,
	DISCOUNT_PCT_NO Double precision NOT NULL Default 0.0,
	STATUS_FLG Integer NOT NULL Default 0,
	START_DT Timestamp,
	EXPIRE_DT Timestamp,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_REZDOX_PROMOTION primary key (PROMOTION_ID)
) Without Oids;


Create table custom.REZDOX_MEMBERSHIP_PROMOTION
(
	MEMBERSHIP_PROMOTION_ID Varchar(32) NOT NULL,
	PROMOTION_ID Varchar(32) NOT NULL,
	MEMBERSHIP_ID Varchar(32) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
constraint pk_REZDOX_MEMBERSHIP_PROMOTION primary key (MEMBERSHIP_PROMOTION_ID)
) Without Oids;


Create table custom.REZDOX_SUBSCRIPTION
(
	SUBSCRIPTION_ID Varchar(32) NOT NULL,
	TRANSACTION_ID Varchar(32),
	MEMBER_ID Varchar(32) NOT NULL,
	MEMBERSHIP_ID Varchar(32) NOT NULL,
	PROMOTION_ID Varchar(32),
	COST_NO Double precision Default 0.00,
	DISCOUNT_NO Double precision,
	QTY_NO Integer NOT NULL Default 0,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_REZDOX_SUBSCRIPTION primary key (SUBSCRIPTION_ID)
) Without Oids;


Create table custom.REZDOX_REWARD
(
	REWARD_ID Varchar(32) NOT NULL,
	REWARD_TYPE_CD Varchar(10) NOT NULL,
	REWARD_NM Varchar(100) NOT NULL,
	ACTION_SLUG_TXT Varchar(20) NOT NULL,
	POINT_VALUE_NO Integer NOT NULL Default 0,
	ORDER_NO Integer NOT NULL Default 0,
	ACTIVE_FLG Integer NOT NULL Default 1,
	IMAGE_URL Varchar(250),
	CURRENCY_VALUE_NO Double precision,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_REZDOX_REWARD primary key (REWARD_ID)
) Without Oids;


Create table custom.REZDOX_MEMBER_REWARD
(
	MEMBER_REWARD_ID Varchar(32) NOT NULL,
	MEMBER_ID Varchar(32) NOT NULL,
	REWARD_ID Varchar(32) NOT NULL,
	POINT_VALUE_NO Integer NOT NULL Default 0,
	CURRENCY_VALUE_NO Double precision,
	APPROVAL_FLG Integer NOT NULL Default 0,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_REZDOX_MEMBER_REWARD primary key (MEMBER_REWARD_ID)
) Without Oids;


Create table custom.REZDOX_ROOM_CATEGORY
(
	ROOM_CATEGORY_CD Varchar(32) NOT NULL,
	CATEGORY_NM Varchar(50) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
constraint pk_REZDOX_ROOM_CATEGORY primary key (ROOM_CATEGORY_CD)
) Without Oids;


Create table custom.REZDOX_TREASURE_CATEGORY
(
	TREASURE_CATEGORY_CD Varchar(10) NOT NULL,
	CATEGORY_NM Varchar(30) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
constraint pk_REZDOX_TREASURE_CATEGORY primary key (TREASURE_CATEGORY_CD)
) Without Oids;


Create table custom.REZDOX_BUSINESS_SPECIALTY_TYPE
(
	BUSINESS_SPECIALTY_TYPE_CD Varchar(32) NOT NULL,
	TYPE_NM Varchar(100) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
constraint pk_REZDOX_BUSINESS_SPECIALTY_TYPE primary key (BUSINESS_SPECIALTY_TYPE_CD)
) Without Oids;


Create table custom.REZDOX_BUSINESS_SPECIALTY
(
	BUSINESS_SPECIALTY_ID Varchar(32) NOT NULL,
	BUSINESS_ID Varchar(32) NOT NULL,
	BUSINESS_SPECIALTY_TYPE_CD Varchar(32) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
constraint pk_REZDOX_BUSINESS_SPECIALTY primary key (BUSINESS_SPECIALTY_ID)
) Without Oids;


Create table custom.REZDOX_REWARD_TYPE
(
	REWARD_TYPE_CD Varchar(10) NOT NULL,
	TYPE_NM Varchar(30) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
constraint pk_REZDOX_REWARD_TYPE primary key (REWARD_TYPE_CD)
) Without Oids;


Create table custom.REZDOX_ROOM
(
	ROOM_ID Varchar(32) NOT NULL,
	RESIDENCE_ID Varchar(32) NOT NULL,
	ROOM_TYPE_CD Varchar(32) NOT NULL,
	ROOM_NM Varchar(100),
	LENGTH_FOOT_NO Integer,
	LENGTH_INCH_NO Integer,
	WIDTH_FOOT_NO Integer,
	WIDTH_INCH_NO Integer,
	HEIGHT_FOOT_NO Integer,
	HEIGHT_INCH_NO Integer,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_REZDOX_ROOM primary key (ROOM_ID)
) Without Oids;


Create table custom.REZDOX_BUSINESS_CATEGORY_XR
(
	BUSINESS_CATEGORY_XR_ID Varchar(32) NOT NULL,
	BUSINESS_ID Varchar(32) NOT NULL,
	BUSINESS_CATEGORY_CD Varchar(32) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
constraint pk_REZDOX_BUSINESS_CATEGORY_XR primary key (BUSINESS_CATEGORY_XR_ID)
) Without Oids;


Create table custom.REZDOX_DOCUMENT
(
	DOCUMENT_ID Varchar(32) NOT NULL,
	TREASURE_ITEM_ID Varchar(32),
	PROJECT_ID Varchar(32),
	DOCUMENT_NM Varchar(100) NOT NULL,
	DESCRIPTION_TXT Varchar(1000),
	FILE_PTH Varchar(500),
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_REZDOX_DOCUMENT primary key (DOCUMENT_ID)
) Without Oids;


Create table custom.REZDOX_MESSAGE
(
	MESSAGE_ID Varchar(32) NOT NULL,
	MESSAGE_TXT Text NOT NULL,
	APPROVAL_FLG Integer,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_REZDOX_MESSAGE primary key (MESSAGE_ID)
) Without Oids;


Create table custom.REZDOX_TRANSACTION
(
	TRANSACTION_ID Varchar(32) NOT NULL,
	PAYMENT_TYPE_ID Varchar(32) NOT NULL,
	TRANSACTION_CD Varchar(50),
	CREATE_DT Timestamp NOT NULL,
constraint pk_REZDOX_TRANSACTION primary key (TRANSACTION_ID)
) Without Oids;


Create table custom.REZDOX_ALBUM
(
	ALBUM_ID Varchar(32) NOT NULL,
	RESIDENCE_ID Varchar(32),
	BUSINESS_ID Varchar(32),
	ALBUM_NM Varchar(100) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_REZDOX_ALBUM primary key (ALBUM_ID)
) Without Oids;


/* Create Foreign Keys */

Alter table custom.REZDOX_PROJECT add Constraint RESIDENCE_PROJECT_FKEY foreign key (RESIDENCE_ID) references custom.REZDOX_RESIDENCE (RESIDENCE_ID) on update restrict on delete restrict;

Alter table custom.REZDOX_RESIDENCE_ATTRIBUTE add Constraint RESIDENCE_ATTRIBUTE_FKEY foreign key (RESIDENCE_ID) references custom.REZDOX_RESIDENCE (RESIDENCE_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_RESIDENCE_MEMBER_XR add Constraint RESIDENCE_MEMBER_FKEY foreign key (RESIDENCE_ID) references custom.REZDOX_RESIDENCE (RESIDENCE_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_TREASURE_ITEM add Constraint RESIDENCE_TREASURE_FKEY foreign key (RESIDENCE_ID) references custom.REZDOX_RESIDENCE (RESIDENCE_ID) on update restrict on delete set null;

Alter table custom.REZDOX_ROOM add Constraint RESIDENCE_ROOM_FKEY foreign key (RESIDENCE_ID) references custom.REZDOX_RESIDENCE (RESIDENCE_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_ALBUM add Constraint RESIDENCE_ALBUM_FKEY foreign key (RESIDENCE_ID) references custom.REZDOX_RESIDENCE (RESIDENCE_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_PROJECT_ATTRIBUTE add Constraint PROJECT_ATTRIBUTE_FKEY foreign key (PROJECT_ID) references custom.REZDOX_PROJECT (PROJECT_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_PROJECT_MATERIAL add Constraint PROJECT_MATERIAL_FKEY foreign key (PROJECT_ID) references custom.REZDOX_PROJECT (PROJECT_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_PHOTO add Constraint RESIDENCE_PROJECT_PHOTO_FKEY foreign key (PROJECT_ID) references custom.REZDOX_PROJECT (PROJECT_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_DOCUMENT add Constraint RESIDENCE_PROJECT_DOCUMENT_FKEY foreign key (PROJECT_ID) references custom.REZDOX_PROJECT (PROJECT_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_RESIDENCE_MEMBER_XR add Constraint MEMBER_RESIDENCE_FKEY foreign key (MEMBER_ID) references custom.REZDOX_MEMBER (MEMBER_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_MEMBER_BUSINESS_REVIEW add Constraint MEMBER_BUSINESS_REVIEW_FKEY foreign key (MEMBER_ID) references custom.REZDOX_MEMBER (MEMBER_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_CONNECTION add Constraint MEMBER_CONNECTOR_FKEY foreign key (SNDR_MEMBER_ID) references custom.REZDOX_MEMBER (MEMBER_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_BUSINESS_MEMBER_XR add Constraint BUSINESS_MEMBER_FKEY foreign key (MEMBER_ID) references custom.REZDOX_MEMBER (MEMBER_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_MEMBER_MESSAGE add Constraint SNDR_MEMBER_MESSAGE_FKEY foreign key (SNDR_MEMBER_ID) references custom.REZDOX_MEMBER (MEMBER_ID) on update restrict on delete set null;

Alter table custom.REZDOX_MEMBER_MESSAGE add Constraint RCPT_MEMBER_MESSAGE_FKEY foreign key (RCPT_MEMBER_ID) references custom.REZDOX_MEMBER (MEMBER_ID) on update restrict on delete set null;

Alter table custom.REZDOX_NOTIFICATION add Constraint MEMBER_NOTIFICATION_FKEY foreign key (MEMBER_ID) references custom.REZDOX_MEMBER (MEMBER_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_SUBSCRIPTION add Constraint MEMBER_SUBSCRIPTION_FKEY foreign key (MEMBER_ID) references custom.REZDOX_MEMBER (MEMBER_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_MEMBER_REWARD add Constraint MEMBER_REWARD_FKEY foreign key (MEMBER_ID) references custom.REZDOX_MEMBER (MEMBER_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_CONNECTION add Constraint MEMBER_CONNECTEE_FKEY foreign key (RCPT_MEMBER_ID) references custom.REZDOX_MEMBER (MEMBER_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_TREASURE_ITEM add Constraint OWNER_MEMBER_TREASURE_ITEM_FKEY foreign key (OWNER_MEMBER_ID) references custom.REZDOX_MEMBER (MEMBER_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_TREASURE_ITEM add Constraint BENEFICIARY_MEMBER_TREASURE_ITEM_FKEY foreign key (BENEFICIARY_MEMBER_ID) references custom.REZDOX_MEMBER (MEMBER_ID) on update restrict on delete set null;

Alter table custom.REZDOX_BUSINESS_ATTRIBUTE add Constraint BUSINESS_ATTRIBUTE_FKEY foreign key (BUSINESS_ID) references custom.REZDOX_BUSINESS (BUSINESS_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_MEMBER_BUSINESS_REVIEW add Constraint BUSINESS_REVIEW_FKEY foreign key (BUSINESS_ID) references custom.REZDOX_BUSINESS (BUSINESS_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_CONNECTION add Constraint BUSINESS1_CONNECTION_FKEY foreign key (SNDR_BUSINESS_ID) references custom.REZDOX_BUSINESS (BUSINESS_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_BUSINESS_MEMBER_XR add Constraint BUSINESS_BUSINESS_MEMBER_FKEY foreign key (BUSINESS_ID) references custom.REZDOX_BUSINESS (BUSINESS_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_NOTIFICATION add Constraint BUSINESS_NOTIFICATION_FKEY foreign key (BUSINESS_ID) references custom.REZDOX_BUSINESS (BUSINESS_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_BUSINESS_SPECIALTY add Constraint BUSINESS_SPECIALTY_FKEY foreign key (BUSINESS_ID) references custom.REZDOX_BUSINESS (BUSINESS_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_CONNECTION add Constraint BUSINESS2_CONNECTION_FKEY foreign key (RCPT_BUSINESS_ID) references custom.REZDOX_BUSINESS (BUSINESS_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_PROJECT add Constraint BUSINESS_RESIDENCE_PROJECT foreign key (BUSINESS_ID) references custom.REZDOX_BUSINESS (BUSINESS_ID) on update restrict on delete restrict;

Alter table custom.REZDOX_BUSINESS_CATEGORY_XR add Constraint BUSINESS_CATEGORY_FKEY foreign key (BUSINESS_ID) references custom.REZDOX_BUSINESS (BUSINESS_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_ALBUM add Constraint BUSINESS_ALBUM_FKEY foreign key (BUSINESS_ID) references custom.REZDOX_BUSINESS (BUSINESS_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_BUSINESS_CATEGORY add Constraint BUSINESS_CATEGORY_SKEY foreign key (PARENT_CD) references custom.REZDOX_BUSINESS_CATEGORY (BUSINESS_CATEGORY_CD) on update restrict on delete restrict;

Alter table custom.REZDOX_BUSINESS_CATEGORY_XR add Constraint BUSINESS_CATEGORY_XR_FKEY foreign key (BUSINESS_CATEGORY_CD) references custom.REZDOX_BUSINESS_CATEGORY (BUSINESS_CATEGORY_CD) on update restrict on delete restrict;

Alter table custom.REZDOX_PROJECT add Constraint PROJECT_CATEGORY_FKEY foreign key (PROJECT_CATEGORY_CD) references custom.REZDOX_PROJECT_CATEGORY (PROJECT_CATEGORY_CD) on update restrict on delete restrict;

Alter table custom.REZDOX_PROJECT add Constraint PROJECT_TYPE_FKEY foreign key (PROJECT_TYPE_CD) references custom.REZDOX_PROJECT_TYPE (PROJECT_TYPE_CD) on update restrict on delete restrict;

Alter table custom.REZDOX_ROOM add Constraint RESIDENCE_ROOM_TYPE_FKEY foreign key (ROOM_TYPE_CD) references custom.REZDOX_ROOM_TYPE (ROOM_TYPE_CD) on update restrict on delete restrict;

Alter table custom.REZDOX_PROJECT_MATERIAL_ATTRIBUTE add Constraint PROJECT_MATERIAL_ATTRIBUTE_FKEY foreign key (PROJECT_MATERIAL_ID) references custom.REZDOX_PROJECT_MATERIAL (PROJECT_MATERIAL_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_MEMBERSHIP_PROMOTION add Constraint MEMBERSHIP_PROMTION_FKEY foreign key (MEMBERSHIP_ID) references custom.REZDOX_MEMBERSHIP (MEMBERSHIP_ID) on update restrict on delete restrict;

Alter table custom.REZDOX_SUBSCRIPTION add Constraint MEMBERSHIP_SUBSCRIPTION_FKEY foreign key (MEMBERSHIP_ID) references custom.REZDOX_MEMBERSHIP (MEMBERSHIP_ID) on update restrict on delete restrict;

Alter table custom.REZDOX_TREASURE_ITEM_ATTRIBUTE add Constraint TREASURE_ITEM_ATTRIBUTE_FKEY foreign key (TREASURE_ITEM_ID) references custom.REZDOX_TREASURE_ITEM (TREASURE_ITEM_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_PHOTO add Constraint TREASURE_ITEM_PHOTO_FKEY foreign key (TREASURE_ITEM_ID) references custom.REZDOX_TREASURE_ITEM (TREASURE_ITEM_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_DOCUMENT add Constraint TREASURE_ITEM_DOCUMENT_FKEY foreign key (TREASURE_ITEM_ID) references custom.REZDOX_TREASURE_ITEM (TREASURE_ITEM_ID) on update restrict on delete cascade;

Alter table custom.REZDOX_TRANSACTION add Constraint PAYMENT_TYPE_TRANSACTION_FKEY foreign key (PAYMENT_TYPE_ID) references custom.REZDOX_PAYMENT_TYPE (PAYMENT_TYPE_ID) on update restrict on delete restrict;

Alter table custom.REZDOX_MEMBERSHIP_PROMOTION add Constraint PROMOTION_MEMBERSHIP_FKEY foreign key (PROMOTION_ID) references custom.REZDOX_PROMOTION (PROMOTION_ID) on update restrict on delete restrict;

Alter table custom.REZDOX_SUBSCRIPTION add Constraint PROMOTION_SUBSCRIPTION_FKEY foreign key (PROMOTION_ID) references custom.REZDOX_PROMOTION (PROMOTION_ID) on update restrict on delete restrict;

Alter table custom.REZDOX_MEMBER_REWARD add Constraint REWARD_MEMB_REW_FKEY foreign key (REWARD_ID) references custom.REZDOX_REWARD (REWARD_ID) on update restrict on delete restrict;

Alter table custom.REZDOX_ROOM_TYPE add Constraint CATEGORY_ROOM_FKEY foreign key (ROOM_CATEGORY_CD) references custom.REZDOX_ROOM_CATEGORY (ROOM_CATEGORY_CD) on update restrict on delete restrict;

Alter table custom.REZDOX_TREASURE_ITEM add Constraint TREASURE_CATEGORY_ITEM_FKEY foreign key (TREASURE_CATEGORY_CD) references custom.REZDOX_TREASURE_CATEGORY (TREASURE_CATEGORY_CD) on update restrict on delete restrict;

Alter table custom.REZDOX_BUSINESS_SPECIALTY add Constraint SPECIALTY_TYPE_FKEY foreign key (BUSINESS_SPECIALTY_TYPE_CD) references custom.REZDOX_BUSINESS_SPECIALTY_TYPE (BUSINESS_SPECIALTY_TYPE_CD) on update restrict on delete restrict;

Alter table custom.REZDOX_REWARD add Constraint REWARD_TYPE_FKEY foreign key (REWARD_TYPE_CD) references custom.REZDOX_REWARD_TYPE (REWARD_TYPE_CD) on update restrict on delete restrict;

Alter table custom.REZDOX_PROJECT add Constraint RESIDENCE_ROOM_PROJECT_FKEY foreign key (ROOM_ID) references custom.REZDOX_ROOM (ROOM_ID) on update restrict on delete restrict;

Alter table custom.REZDOX_TREASURE_ITEM add Constraint RESIDENCE_ROOM_TREASURE_ITEM_FKEY foreign key (ROOM_ID) references custom.REZDOX_ROOM (ROOM_ID) on update restrict on delete set null;

Alter table custom.REZDOX_MEMBER_MESSAGE add  foreign key (MESSAGE_ID) references custom.REZDOX_MESSAGE (MESSAGE_ID) on update restrict on delete restrict;

Alter table custom.REZDOX_NOTIFICATION add  foreign key (MESSAGE_ID) references custom.REZDOX_MESSAGE (MESSAGE_ID) on update restrict on delete restrict;

Alter table custom.REZDOX_SUBSCRIPTION add Constraint TRANSACTION_SUBSCRIPTION_FKEY foreign key (TRANSACTION_ID) references custom.REZDOX_TRANSACTION (TRANSACTION_ID) on update restrict on delete restrict;

Alter table custom.REZDOX_PHOTO add Constraint ALBUM_PHOTO_FKEY foreign key (ALBUM_ID) references custom.REZDOX_ALBUM (ALBUM_ID) on update restrict on delete cascade;



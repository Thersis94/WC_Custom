/*
Created		4/26/2007
Modified		9/17/2007
Project		
Model			
Company		
Author		
Version		
Database		MS SQL 2005 
*/


Create table [ANS_SURGEON]
(
	[SURGEON_ID] Nvarchar(40) NOT NULL,
	[SURGEON_TYPE_ID] Integer NOT NULL,
	[STATUS_ID] Integer NOT NULL,
	[SPECIALTY_ID] Integer NOT NULL,
	[SALES_REP_ID] Varchar(32) NOT NULL,
	[TITLE_NM] Nvarchar(10) NULL,
	[FIRST_NM] Nvarchar(40) NULL,
	[MIDDLE_NM] Nvarchar(30) NULL,
	[LAST_NM] Nvarchar(40) NULL,
	[SUFFIX_NM] Nvarchar(10) NULL,
	[EMAIL_ADDRESS_TXT] Nvarchar(125) NULL,
	[WEBSITE_URL] Nvarchar(125) NULL,
	[LANGUAGES_TXT] Nvarchar(40) NULL,
	[LOCALE_TXT] Nvarchar(10) NULL,
	[UPDATE_DT] Datetime NULL,
	[SPOUSE_NM] Nvarchar(100) NULL,
	[CHILDREN_NM] Nvarchar(255) NULL,
	[CREATE_DT] Datetime NULL,
Constraint [pk_ANS_SURGEON] Primary Key ([SURGEON_ID])
) 
go

Create table [ANS_CLINIC]
(
	[CLINIC_ID] Varchar(40) NOT NULL,
	[SURGEON_ID] Nvarchar(40) NOT NULL,
	[CLINIC_NM] Nvarchar(80) NULL,
	[ADDRESS_TXT] Nvarchar(100) NULL,
	[ADDRESS2_TXT] Nvarchar(100) NULL,
	[ADDRESS3_TXT] Nvarchar(100) NULL,
	[CITY_NM] Nvarchar(50) NULL,
	[STATE_CD] Nvarchar(5) NULL,
	[ZIP_CD] Nvarchar(12) NULL,
	[LATITUDE_NO] Numeric(12,8) NULL,
	[LONGITUDE_NO] Numeric(12,8) NULL,
	[GEO_MATCH_CD] Nvarchar(20) NULL,
	[PRIMARY_LOCATION_FLG] Integer NULL,
	[CREATE_DT] Datetime NULL,
	[UPDATE_DT] Datetime NULL,
Constraint [pk_ANS_CLINIC] Primary Key ([CLINIC_ID])
) 
go

Create table [ANS_PHONE]
(
	[PHONE_ID] Varchar(32) NOT NULL,
	[PHONE_TYPE_ID] Integer NOT NULL,
	[CLINIC_ID] Varchar(40) NOT NULL,
	[AREA_CD] Nvarchar(5) NULL,
	[EXCHANGE_NO] Nvarchar(5) NULL,
	[LINE_NO] Nvarchar(6) NULL,
	[CREATE_DT] Datetime NULL,
Constraint [pk_ANS_PHONE] Primary Key ([PHONE_ID])
) 
go

Create table [ANS_PHONE_TYPE]
(
	[PHONE_TYPE_ID] Integer NOT NULL,
	[TYPE_NM] Nvarchar(30) NULL,
	[CREATE_DT] Datetime NULL,
Constraint [pk_ANS_PHONE_TYPE] Primary Key ([PHONE_TYPE_ID])
) 
go

Create table [ANS_STAFF]
(
	[STAFF_ID] Varchar(32) NOT NULL,
	[STAFF_TYPE_ID] Integer NOT NULL,
	[SURGEON_ID] Nvarchar(40) NOT NULL,
	[MEMBER_NM] Nvarchar(80) NULL,
	[EMAIL_TXT] Nvarchar(255) NULL,
	[PHONE_NO] Varchar(15) NULL,
	[COMMENTS_TXT] Nvarchar(512) NULL,
	[CREATE_DT] Datetime NULL,
	[UPDATE_DT] Datetime NULL,
Constraint [pk_ANS_STAFF] Primary Key ([STAFF_ID])
) 
go

Create table [ANS_STAFF_TYPE]
(
	[STAFF_TYPE_ID] Integer NOT NULL,
	[TYPE_NM] Nvarchar(40) NOT NULL,
	[CREATE_DT] Datetime NULL,
Constraint [pk_ANS_STAFF_TYPE] Primary Key ([STAFF_TYPE_ID])
) 
go

Create table [ANS_SPECIALTY]
(
	[SPECIALTY_ID] Integer NOT NULL,
	[SPECIALTY_NM] Nvarchar(80) NULL,
	[CREATE_DT] Datetime NULL,
Constraint [pk_ANS_SPECIALTY] Primary Key ([SPECIALTY_ID])
) 
go

Create table [ANS_STATUS]
(
	[STATUS_ID] Integer NOT NULL,
	[STATUS_NM] Nvarchar(40) NULL,
	[CREATE_DT] Datetime NULL,
Constraint [pk_ANS_STATUS] Primary Key ([STATUS_ID])
) 
go

Create table [ANS_SURGEON_TYPE]
(
	[SURGEON_TYPE_ID] Integer NOT NULL,
	[TYPE_NM] Nvarchar(40) NOT NULL,
	[CREATE_DT] Datetime NULL,
Constraint [pk_ANS_SURGEON_TYPE] Primary Key ([SURGEON_TYPE_ID])
) 
go

Create table [ANS_SALES_AREA]
(
	[AREA_ID] Varchar(32) NOT NULL,
	[AREA_NM] Nvarchar(40) NULL,
	[CREATE_DT] Datetime NULL,
	[UPDATE_DT] Datetime NULL,
Constraint [pk_ANS_SALES_AREA] Primary Key ([AREA_ID])
) 
go

Create table [ANS_SALES_REGION]
(
	[REGION_ID] Varchar(32) NOT NULL,
	[AREA_ID] Varchar(32) NOT NULL,
	[REGION_NM] Nvarchar(40) NULL,
	[CREATE_DT] Datetime NULL,
	[UPDATE_DT] Datetime NULL,
Constraint [pk_ANS_SALES_REGION] Primary Key ([REGION_ID])
) 
go

Create table [ANS_SALES_REP]
(
	[SALES_REP_ID] Varchar(32) NOT NULL,
	[REGION_ID] Varchar(32) NOT NULL,
	[ANS_LOGIN_ID] Varchar(40) NOT NULL,
	[ROLE_ID] Varchar(32) NOT NULL,
	[FIRST_NM] Nvarchar(40) NOT NULL,
	[LAST_NM] Nvarchar(40) NOT NULL,
	[EMAIL_ADDRESS_TXT] Nvarchar(256) NULL,
	[PHONE_NUMBER_TXT] Varchar(15) NULL,
	[UPDATE_DT] Datetime NULL,
	[CREATE_DT] Datetime NULL,
Constraint [pk_ANS_SALES_REP] Primary Key ([SALES_REP_ID])
) 
go

Create table [ANS_XR_EVENT_SURGEON]
(
	[EVENT_SURGEON_ID] Varchar(32) NOT NULL,
	[SURGEON_ID] Nvarchar(40) NOT NULL,
	[EVENT_ENTRY_ID] Varchar(32) NULL,
	[CREATE_DT] Datetime NULL,
Constraint [pk_ANS_XR_EVENT_SURGEON] Primary Key ([EVENT_SURGEON_ID])
) 
go

Create table [ANS_BUSINESS_PLAN]
(
	[BUSINESS_PLAN_ID] Varchar(32) NOT NULL,
	[CATEGORY_ID] Integer NOT NULL,
	[FIELD_NM] Nvarchar(100) NOT NULL,
	[ACTIVE_FLG] Integer NULL,
	[SELECT_FLG] Integer NULL,
	[CREATE_DT] Datetime NOT NULL,
Constraint [pk_ANS_BUSINESS_PLAN] Primary Key ([BUSINESS_PLAN_ID])
) 
go

Create table [ANS_XR_SURGEON_BUSPLAN]
(
	[SURGEON_BUSPLAN_ID] Varchar(32) NOT NULL,
	[BUSINESS_PLAN_ID] Varchar(32) NOT NULL,
	[SURGEON_ID] Nvarchar(40) NOT NULL,
	[SELECTED_FLG] Integer NULL,
	[VALUE_TXT] Nvarchar(1024) NULL,
	[BP_YEAR_NO] Integer NULL,
	[CREATE_DT] Datetime NOT NULL,
Constraint [pk_ANS_XR_SURGEON_BUSPLAN] Primary Key ([SURGEON_BUSPLAN_ID])
) 
go

Create table [ANS_BP_CATEGORY]
(
	[CATEGORY_ID] Integer NOT NULL,
	[CATEGORY_NM] Nvarchar(100) NULL,
	[CREATE_DT] Datetime NULL,
Constraint [pk_ANS_BP_CATEGORY] Primary Key ([CATEGORY_ID])
) 
go


Alter table [ANS_CLINIC] add Constraint [SURGEON_CLINIC_FKEY] foreign key([SURGEON_ID]) references [ANS_SURGEON] ([SURGEON_ID])  on update no action on delete cascade 
go
Alter table [ANS_STAFF] add Constraint [ANS_SURGEON_STAFF_FKEY] foreign key([SURGEON_ID]) references [ANS_SURGEON] ([SURGEON_ID])  on update no action on delete no action 
go
Alter table [ANS_XR_EVENT_SURGEON] add Constraint [ANS_SURGEON_XR_EVENT_FKEY] foreign key([SURGEON_ID]) references [ANS_SURGEON] ([SURGEON_ID])  on update no action on delete no action 
go
Alter table [ANS_XR_SURGEON_BUSPLAN] add Constraint [ANS_XR_SURGEON_BP_FKEY] foreign key([SURGEON_ID]) references [ANS_SURGEON] ([SURGEON_ID])  on update no action on delete cascade 
go
Alter table [ANS_PHONE] add Constraint [ANS_CLINIC_PHONE_FKEY] foreign key([CLINIC_ID]) references [ANS_CLINIC] ([CLINIC_ID])  on update no action on delete cascade 
go
Alter table [ANS_PHONE] add Constraint [PHONE_TYPE_FKEY] foreign key([PHONE_TYPE_ID]) references [ANS_PHONE_TYPE] ([PHONE_TYPE_ID])  on update no action on delete no action 
go
Alter table [ANS_STAFF] add Constraint [ANS_STAFF_TYPE_FKEY] foreign key([STAFF_TYPE_ID]) references [ANS_STAFF_TYPE] ([STAFF_TYPE_ID])  on update no action on delete no action 
go
Alter table [ANS_SURGEON] add Constraint [ANS_SPECIALTY_SURGEON_FKEY] foreign key([SPECIALTY_ID]) references [ANS_SPECIALTY] ([SPECIALTY_ID])  on update no action on delete no action 
go
Alter table [ANS_SURGEON] add Constraint [ANS_STATUS_SURGEON_FKEY] foreign key([STATUS_ID]) references [ANS_STATUS] ([STATUS_ID])  on update no action on delete no action 
go
Alter table [ANS_SURGEON] add Constraint [ANS_SURGEON_TYPEFKEY] foreign key([SURGEON_TYPE_ID]) references [ANS_SURGEON_TYPE] ([SURGEON_TYPE_ID])  on update no action on delete no action 
go
Alter table [ANS_SALES_REGION] add Constraint [ANS_SALES_AREA_REGION_FKEY] foreign key([AREA_ID]) references [ANS_SALES_AREA] ([AREA_ID])  on update no action on delete no action 
go
Alter table [ANS_SALES_REP] add Constraint [ANS_REGION_REP_FKEY] foreign key([REGION_ID]) references [ANS_SALES_REGION] ([REGION_ID])  on update no action on delete no action 
go
Alter table [ANS_SURGEON] add Constraint [ANS_SURGEON_REP_FKEY] foreign key([SALES_REP_ID]) references [ANS_SALES_REP] ([SALES_REP_ID])  on update no action on delete no action 
go
Alter table [ANS_XR_SURGEON_BUSPLAN] add Constraint [ANS_XR_BUSPLAN_SUR_FKEY] foreign key([BUSINESS_PLAN_ID]) references [ANS_BUSINESS_PLAN] ([BUSINESS_PLAN_ID])  on update no action on delete no action 
go
Alter table [ANS_BUSINESS_PLAN] add Constraint [ANS_CATEGORY_BUSINESS_PLAN_FKEY] foreign key([CATEGORY_ID]) references [ANS_BP_CATEGORY] ([CATEGORY_ID])  on update no action on delete no action 
go


Set quoted_identifier on
go


Set quoted_identifier off
go


/** Add the Phone Types to the System **/
insert into ans_phone_type (phone_type_id, type_nm, create_dt) values (1, 'Home','4/26/2007')
insert into ans_phone_type (phone_type_id, type_nm, create_dt) values (2, 'Work','4/26/2007')
insert into ans_phone_type (phone_type_id, type_nm, create_dt) values (3, 'Cell','4/26/2007')
insert into ans_phone_type (phone_type_id, type_nm, create_dt) values (4, 'Fax','4/26/2007')
insert into ans_phone_type (phone_type_id, type_nm, create_dt) values (5, 'OM','4/26/2007')



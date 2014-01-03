/*
Created		10/16/2007
Modified		11/8/2010
Project		
Model			
Company		
Author		
Version		
Database		MS SQL 2005 
*/


Alter table [CU_UNIT_LEDGER] drop constraint [CU_REQUEST_LEDGER_FKEY]
go

Alter table [CU_UNIT_LEDGER] drop constraint [CU_UNIT_LEDGER_FKEY]
go

Alter table [CODMAN_CU_UNIT] drop constraint [CU_STATUS_UNIT_FKEY]
go

Alter table [CODMAN_CU_TRANSACTION] drop constraint [CU_STATUS_REQUEST_FKEY]
go

Alter table [CODMAN_CU_PERSON] drop constraint [CU_TERRITORY_REP_FKEY]
go

Alter table [CODMAN_CU_ACCOUNT] drop constraint [CU_REP_ACCT_FKEY]
go

Alter table [CODMAN_CU_PHYSICIAN] drop constraint [CU_PHYS_ACCT_FKEY]
go

Alter table [CODMAN_CU_TRANSACTION] drop constraint [CU_ACCT_REQUEST_FKEY]
go

Alter table [CODMAN_CU_TRANSACTION] drop constraint [CU_PHYS_TRANSACTION_FKEY]
go

Alter table [CODMAN_CU_TRANSACTION] drop constraint [CU_TRANS_TYPE_FKEY]
go


Drop table [CODMAN_CU_TRANSACTION_TYPE] 
go
Drop table [CU_UNIT_LEDGER] 
go
Drop table [CODMAN_CU_PHYSICIAN] 
go
Drop table [CODMAN_CU_ACCOUNT] 
go
Drop table [CODMAN_CU_PERSON] 
go
Drop table [TERRITORY] 
go
Drop table [CODMAN_CU_STATUS] 
go
Drop table [CODMAN_CU_UNIT] 
go
Drop table [CODMAN_CU_TRANSACTION] 
go


Create table [CODMAN_CU_TRANSACTION]
(
	[TRANSACTION_ID] Nvarchar(32) NOT NULL,
	[TRANSACTION_TYPE_ID] Nvarchar(32) NOT NULL,
	[STATUS_ID] Nvarchar(32) NOT NULL,
	[ACCOUNT_ID] Nvarchar(32) NOT NULL,
	[PHYSICIAN_ID] Nvarchar(32) NULL,
	[REQUEST_NO] Integer Identity(1,1) NOT NULL,
	[APPROVAL_DT] Datetime NULL,
	[UNIT_CNT_NO] Integer Default 1 NULL,
	[DROPSHIP_FLG] Integer NULL,
	[ADDRESS_TXT] Nvarchar(160) NULL,
	[ADDRESS2_TXT] Nvarchar(160) NULL,
	[CITY_NM] Nvarchar(80) NULL,
	[STATE_CD] Nvarchar(5) NULL,
	[ZIP_CD] Nvarchar(10) NULL,
	[COUNTRY_CD] Varchar(2) NULL,
	[REQUESTING_PARTY_NM] Nvarchar(80) NULL,
	[NOTES_TXT] Ntext NULL,
	[CREATE_DT] Datetime NOT NULL,
	[UPDATE_DT] Datetime NULL,
Constraint [pk_CODMAN_CU_TRANSACTION] Primary Key ([TRANSACTION_ID])
) 
go

Create table [CODMAN_CU_UNIT]
(
	[UNIT_ID] Nvarchar(32) NOT NULL,
	[UNIT_STATUS_ID] Nvarchar(32) NOT NULL,
	[SERIAL_NO_TXT] Nvarchar(100) NOT NULL,
	[SOFTWARE_REV_NO] Nvarchar(50) NULL,
	[ENGINEER_NM] Nvarchar(100) NULL,
	[CREATE_DT] Datetime NOT NULL,
	[UPDATE_DT] Datetime NULL,
	[DEPLOYED_DT] Datetime NULL,
Constraint [pk_CODMAN_CU_UNIT] Primary Key ([UNIT_ID])
) 
go

Create table [CODMAN_CU_STATUS]
(
	[STATUS_ID] Nvarchar(32) NOT NULL,
	[STATUS_NM] Nvarchar(50) NOT NULL,
	[STATUS_TYPE_NO] Integer NOT NULL,
	[CREATE_DT] Datetime NOT NULL,
Constraint [pk_CODMAN_CU_STATUS] Primary Key ([STATUS_ID])
) 
go

Create table [TERRITORY]
(
	[TERRITORY_ID] Nvarchar(32) NOT NULL,
	[ORGANIZATION_ID] Nvarchar(32) NOT NULL,
	[TERRITORY_NM] Nvarchar(15) NULL,
	[CREATE_DT] Datetime NULL,
	[UPDATE_DT] Datetime NULL,
Constraint [pk_TERRITORY] Primary Key ([TERRITORY_ID])
) 
go

Create table [CODMAN_CU_PERSON]
(
	[PERSON_ID] Nvarchar(32) NOT NULL,
	[TERRITORY_ID] Nvarchar(32) NULL,
	[PROFILE_ID] Nvarchar(32) NOT NULL,
	[SAMPLE_ACCT_NO] Nvarchar(50) NULL,
	[CREATE_DT] Datetime NOT NULL,
	[UPDATE_DT] Datetime NULL,
Constraint [pk_CODMAN_CU_PERSON] Primary Key ([PERSON_ID])
) 
go

Create table [CODMAN_CU_ACCOUNT]
(
	[ACCOUNT_ID] Nvarchar(32) NOT NULL,
	[PERSON_ID] Nvarchar(32) NULL,
	[ACCOUNT_NO] Nvarchar(50) NOT NULL,
	[ACCOUNT_NM] Nvarchar(100) NOT NULL,
	[PHONE_NO_TXT] Nvarchar(20) NULL,
	[ADDRESS_TXT] Nvarchar(160) NULL,
	[ADDRESS2_TXT] Nvarchar(160) NULL,
	[CITY_NM] Nvarchar(80) NULL,
	[STATE_CD] Nvarchar(5) NULL,
	[ZIP_CD] Nvarchar(10) NULL,
	[COUNTRY_CD] Varchar(2) NULL,
	[CREATE_DT] Datetime NOT NULL,
	[UPDATE_DT] Datetime NULL,
Constraint [pk_CODMAN_CU_ACCOUNT] Primary Key ([ACCOUNT_ID])
) 
go

Create table [CODMAN_CU_PHYSICIAN]
(
	[PHYSICIAN_ID] Nvarchar(32) NOT NULL,
	[ACCOUNT_ID] Nvarchar(32) NOT NULL,
	[PROFILE_ID] Nvarchar(32) NOT NULL,
	[CREATE_DT] Datetime NOT NULL,
	[UPDATE_DT] Datetime NULL,
Constraint [pk_CODMAN_CU_PHYSICIAN] Primary Key ([PHYSICIAN_ID])
) 
go

Create table [CU_UNIT_LEDGER]
(
	[LEDGER_ID] Nvarchar(32) NOT NULL,
	[UNIT_ID] Nvarchar(32) NOT NULL,
	[TRANSACTION_ID] Nvarchar(32) NOT NULL,
	[ACTIVE_RECORD_FLG] Integer NOT NULL,
	[NOTES_TXT] Ntext NULL,
	[CREATE_DT] Datetime NOT NULL,
	[UPDATE_DT] Datetime NULL,
Constraint [pk_CU_UNIT_LEDGER] Primary Key ([LEDGER_ID])
) 
go

Create table [CODMAN_CU_TRANSACTION_TYPE]
(
	[TRANSACTION_TYPE_ID] Nvarchar(32) NOT NULL,
	[TYPE_NM] Nvarchar(50) NOT NULL,
	[CREATE_DT] Datetime NOT NULL,
Constraint [pk_CODMAN_CU_TRANSACTION_TYPE] Primary Key ([TRANSACTION_TYPE_ID])
) 
go


Alter table [CU_UNIT_LEDGER] add Constraint [CU_REQUEST_LEDGER_FKEY] foreign key([TRANSACTION_ID]) references [CODMAN_CU_TRANSACTION] ([TRANSACTION_ID])  on update no action on delete cascade 
go
Alter table [CU_UNIT_LEDGER] add Constraint [CU_UNIT_LEDGER_FKEY] foreign key([UNIT_ID]) references [CODMAN_CU_UNIT] ([UNIT_ID])  on update no action on delete cascade 
go
Alter table [CODMAN_CU_UNIT] add Constraint [CU_STATUS_UNIT_FKEY] foreign key([UNIT_STATUS_ID]) references [CODMAN_CU_STATUS] ([STATUS_ID])  on update no action on delete no action 
go
Alter table [CODMAN_CU_TRANSACTION] add Constraint [CU_STATUS_REQUEST_FKEY] foreign key([STATUS_ID]) references [CODMAN_CU_STATUS] ([STATUS_ID])  on update no action on delete no action 
go
Alter table [CODMAN_CU_PERSON] add Constraint [CU_TERRITORY_REP_FKEY] foreign key([TERRITORY_ID]) references [TERRITORY] ([TERRITORY_ID])  on update no action on delete Set Null 
go
Alter table [CODMAN_CU_ACCOUNT] add Constraint [CU_REP_ACCT_FKEY] foreign key([PERSON_ID]) references [CODMAN_CU_PERSON] ([PERSON_ID])  on update no action on delete Set Null 
go
Alter table [CODMAN_CU_PHYSICIAN] add Constraint [CU_PHYS_ACCT_FKEY] foreign key([ACCOUNT_ID]) references [CODMAN_CU_ACCOUNT] ([ACCOUNT_ID])  on update no action on delete cascade 
go
Alter table [CODMAN_CU_TRANSACTION] add Constraint [CU_ACCT_REQUEST_FKEY] foreign key([ACCOUNT_ID]) references [CODMAN_CU_ACCOUNT] ([ACCOUNT_ID])  on update no action on delete cascade 
go
Alter table [CODMAN_CU_TRANSACTION] add Constraint [CU_PHYS_TRANSACTION_FKEY] foreign key([PHYSICIAN_ID]) references [CODMAN_CU_PHYSICIAN] ([PHYSICIAN_ID])  on update no action on delete no action 
go
Alter table [CODMAN_CU_TRANSACTION] add Constraint [CU_TRANS_TYPE_FKEY] foreign key([TRANSACTION_TYPE_ID]) references [CODMAN_CU_TRANSACTION_TYPE] ([TRANSACTION_TYPE_ID])  on update no action on delete no action 
go

/* request status IDs */
insert into codman_cu_status (status_id, status_nm, status_type_no, create_dt) values ('10','Pending',1,GETDATE());
insert into codman_cu_status (status_id, status_nm, status_type_no, create_dt) values ('13','Approved/Processing',1,GETDATE());
insert into codman_cu_status (status_id, status_nm, status_type_no, create_dt) values ('15','Complete',1,GETDATE());
insert into codman_cu_status (status_id, status_nm, status_type_no, create_dt) values ('18','Denied',1,GETDATE());

/* unit status IDs */
insert into codman_cu_status (status_id, status_nm, status_type_no, create_dt) values ('20','Available',2,GETDATE());
insert into codman_cu_status (status_id, status_nm, status_type_no, create_dt) values ('23','In-Use',2,GETDATE());
insert into codman_cu_status (status_id, status_nm, status_type_no, create_dt) values ('25','Being Serviced',2,GETDATE());
insert into codman_cu_status (status_id, status_nm, status_type_no, create_dt) values ('26','Pending Transfer',2,GETDATE());
insert into codman_cu_status (status_id, status_nm, status_type_no, create_dt) values ('28','Decommissioned',2,GETDATE());

/* territory IDs */
insert into territory (territory_id, organization_id, territory_nm, create_dt) values ('512','CODMAN','512',GETDATE());
insert into territory (territory_id, organization_id, territory_nm, create_dt) values ('513','CODMAN','513',GETDATE());
insert into territory (territory_id, organization_id, territory_nm, create_dt) values ('515','CODMAN','515',GETDATE());
insert into territory (territory_id, organization_id, territory_nm, create_dt) values ('535','CODMAN','535',GETDATE());
insert into territory (territory_id, organization_id, territory_nm, create_dt) values ('536','CODMAN','536',GETDATE());
insert into territory (territory_id, organization_id, territory_nm, create_dt) values ('581','CODMAN','581',GETDATE());
insert into territory (territory_id, organization_id, territory_nm, create_dt) values ('582','CODMAN','582',GETDATE());
insert into territory (territory_id, organization_id, territory_nm, create_dt) values ('537','CODMAN','537',GETDATE());
insert into territory (territory_id, organization_id, territory_nm, create_dt) values ('540','CODMAN','540',GETDATE());
insert into territory (territory_id, organization_id, territory_nm, create_dt) values ('543','CODMAN','543',GETDATE());
insert into territory (territory_id, organization_id, territory_nm, create_dt) values ('584','CODMAN','584',GETDATE());
insert into territory (territory_id, organization_id, territory_nm, create_dt) values ('585','CODMAN','585',GETDATE());

/* Transaction types */
insert into codman_cu_transaction_type (transaction_type_id, type_nm, create_dt) values ('1','New Request',GETDATE());
insert into codman_cu_transaction_type (transaction_type_id, type_nm, create_dt) values ('2','Unit Transfer',GETDATE());

Set quoted_identifier on
go


Set quoted_identifier off
go



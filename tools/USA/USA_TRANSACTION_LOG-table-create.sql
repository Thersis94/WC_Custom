/*
Created		1/16/2015
Modified		1/16/2015
Project		
Model			
Company		
Author		
Version		
Database		MS SQL 2005 
*/


Create table [USA_TRANSACTION_LOG]
(
	[TRANS_LOG_ID] Integer Identity NOT NULL,
	[SITE_ID] Nvarchar(100) NULL,
	[TRANS_ID] Nvarchar(64) NULL,
	[PROFILE_ID] Nvarchar(32) NULL,
	[TRANS_TYPE] Varchar(20) NULL,
	[DATA_TXT_TYPE] Varchar(20) NULL,
	[DATA_TXT_ENC] Ntext NULL,
	[TRANS_DT] Datetime NULL,
Primary Key ([TRANS_LOG_ID])
) 
go


Set quoted_identifier on
go


Set quoted_identifier off
go



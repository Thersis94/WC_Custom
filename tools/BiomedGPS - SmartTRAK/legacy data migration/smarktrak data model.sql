
/**
WC Core changes:
*/
alter table core.currency add ABBR_TXT varchar(10); 
alter table core.currency add SYMBOL_TXT varchar(3); 
alter table core.currency add EXCH_RATE_NO decimal(10,4);

alter table authentication_log add OPER_SYS_TXT varchar(30), add BROWSER_TXT varchar(30), add DEVICE_TXT varchar(20), add IP_ADDR_TXT varchar(128);

alter table core.solr_action add ACL_TYPE_NO int;
alter table core.solr_action add GROUP_FLG int;
alter table core.solr_action add GROUP_FIELD_TXT varchar(128);
alter table core.solr_action add GROUP_LIMIT_NO int;

 --update module display for email views
alter table core.module_display add column VIEW_TXT text, add column VIEW_TYPE_NO int4 default 0;


create table core.SOLR_SYNONYM (
	solr_synonym_id varchar(32) not null,
	solr_collection_id varchar(32) not null,
	left_term varchar(128),
	synonym_list varchar not null,
	index_flg int not null,
	create_dt timestamp not null,
	update_dt timestamp,
	primary key (solr_synonym_id)
) Without Oids;

ALTER TABLE core.SOLR_SYNONYM ADD CONSTRAINT solr_collection_fkey FOREIGN KEY (solr_collection_id) 
REFERENCES core.SOLR_COLLECTION(solr_collection_id) on delete cascade on update restrict;


CREATE TABLE core.pageview_user (
	pageview_user_id varchar(32) NOT NULL,
	site_id varchar(32) NOT NULL,
	profile_id varchar(32) NULL,
	session_id varchar(125) NULL,
	page_id varchar(32) NULL,
	request_uri_txt varchar(255) NULL,
	query_str_txt varchar(1024) NULL,
	visit_dt timestamp NOT NULL,
	create_dt timestamp NOT NULL,
	src_pageview_id int4 NULL,
	visit_day_no int,
	visit_month_no int,
	visit_year_no int,
	CONSTRAINT pk_pageview_user PRIMARY KEY (pageview_user_id),
	CONSTRAINT site_pageview_user_fkey FOREIGN KEY (site_id) REFERENCES core.site(site_id)
) Without Oids;

ALTER TABLE core.pageview_user ADD CONSTRAINT page_id_fkey FOREIGN KEY (page_id) 
REFERENCES core.page(page_id) on delete cascade on update restrict;

ALTER TABLE core.pageview_user ADD CONSTRAINT profile_fkey FOREIGN KEY (profile_id) 
REFERENCES core.PROFILE(PROFILE_ID) on delete cascade on update restrict;

--update our dollar record
update core.currency set abbr_txt='USD', symbol_txt='$', exch_rate_no=1 where currency_type_id='dollars';

--add other currency types used by Smarttrak
insert into core.currency (currency_type_id, currency_nm, create_dt, abbr_txt, symbol_txt, exch_rate_no) values
('EUR','Euro',current_timestamp,'EUR','€',0.94),
('CAD','Canadian Dollar',current_timestamp,'CAD','C$',1.32),
('AUD','Australian Dollar',current_timestamp,'AUD','A$',1.36),
('GBP','British Pound',current_timestamp,'GBP','£',0.82),
('DKK','Danish Krone',current_timestamp,'DKK','kr',NULL),
('SEK','Swedish krona',current_timestamp,'SEK','kr',NULL),
('JPY','Japanese Yen',current_timestamp,'JPY','¥‎',115.57),
('NIS','New Israeli Sheqel',current_timestamp,'NIS','₪',3.84),
('CHF','Swiss Franc',current_timestamp,'CHF','₣',1.01);

--extend blog column - no longer needed -JM 1.10.17
--alter table core.blog alter column short_desc_txt type varchar(5000);

--new table to hold data for external file handler
CREATE TABLE core.PROFILE_DOCUMENT
(
	PROFILE_DOCUMENT_ID  varchar(32) NOT NULL,
	ACTION_ID  varchar(32) NOT NULL,
	FEATURE_ID  varchar(32) NOT NULL,
	ORGANIZATION_ID  varchar(32) NOT NULL,
	PROFILE_ID varchar(32), 
	FILE_NM  varchar(175) NOT NULL,
	FILE_PATH_URL  varchar(250) NOT NULL,
	FILE_TYPE_CD varchar(10),
	SIZE_BYTE_NO INTEGER, 
	CREATE_DT TIMESTAMP NOT NULL,
	UPDATE_DT TIMESTAMP,
	primary key (PROFILE_DOCUMENT_ID)
) Without Oids;

ALTER TABLE core.PROFILE_DOCUMENT
ADD CONSTRAINT profile_document_sb_action_fk 
FOREIGN KEY (action_id) 
REFERENCES core.sb_action(action_id) on delete restrict on update restrict;

ALTER TABLE core.PROFILE_DOCUMENT
ADD CONSTRAINT profile_document_organization_fk 
FOREIGN KEY (organization_id) 
REFERENCES core.organization(organization_id) on delete cascade on update restrict;

ALTER TABLE core.PROFILE_DOCUMENT
ADD CONSTRAINT profile_document_profile_fk 
FOREIGN KEY (profile_id) 
REFERENCES core.profile(profile_id) on delete cascade on update restrict;

ALTER TABLE core.PROFILE_DOCUMENT
ADD CONSTRAINT profile_document_file_type_fk 
FOREIGN KEY (file_type_cd) 
REFERENCES core.file_type(file_type_cd) on delete restrict on update restrict;


/**
 * RUN DATAMODEL.sql for datafeed schema
**/



Create table core.SUPPORT_TICKET
(
	TICKET_ID Varchar(32) NOT NULL,
	ORGANIZATION_ID varchar(32) NOT NULL,
	REPORTER_ID varchar(32) NOT NULL,
	ASSIGNED_ID varchar(32) NULL,
	TICKET_NO Integer,
	STATUS_CD Integer NOT NULL,
	NOTIFY_FLG Integer NULL,
	DESC_TXT Varchar NOT NULL,
	REFERRER_URL Varchar(150) NULL,
	CREATE_DT timestamp not NULL,
	UPDATE_DT timestamp null,
	primary key (TICKET_ID)
) Without Oids;

Create table core.SUPPORT_ACTIVITY
(
	ACTIVITY_ID Varchar(32) NOT NULL,
	TICKET_ID Varchar(32) NOT NULL,
	PROFILE_ID varchar(32) NOT NULL,
	DESC_TXT Varchar NULL,
	INTERNAL_FLG Integer NULL,
	EFFORT_NO Decimal(5,2) NULL,
	COST_NO Decimal(5,2) null,
	CREATE_DT timestamp not null,
	primary key (ACTIVITY_ID)
) Without Oids;

Create table core.SUPPORT_ATTACHMENT_XR
(
	ATTACHMENT_XR_ID Varchar(32) NOT NULL,
	TICKET_ID Varchar(32) NOT NULL,
	PROFILE_DOCUMENT_ID varchar(32) NOT NULL,
	CREATE_DT timestamp not null,
	DESC_TXT Varchar(500) NOT null,
	primary key (ATTACHMENT_XR_ID)
) Without Oids;

ALTER TABLE core.support_ticket ALTER COLUMN assigned_id TYPE varchar(32) USING assigned_id::varchar ;
Alter table core.SUPPORT_TICKET add constraint organization_support_ticket_fkey foreign key(ORGANIZATION_ID) references ORGANIZATION (ORGANIZATION_ID)  on update no action on delete cascade;
Alter table core.SUPPORT_TICKET add constraint profile_support_ticket_fkey foreign key(REPORTER_ID) references PROFILE (PROFILE_ID)  on update no action on delete set null;
Alter table core.SUPPORT_TICKET add constraint profile2_support_ticket_fkey  foreign key(ASSIGNED_ID) references PROFILE (PROFILE_ID)  on update no action on delete set null;
Alter table core.SUPPORT_ACTIVITY add constraint profile_support_activity_fkey  foreign key(PROFILE_ID) references PROFILE (PROFILE_ID)  on update no action on delete set null;
Alter table core.SUPPORT_ATTACHMENT_XR add constraint profile_document_support_attachment_fkey  foreign key(PROFILE_DOCUMENT_ID) references PROFILE_DOCUMENT (PROFILE_DOCUMENT_ID)  on update no action on delete cascade;
Alter table core.SUPPORT_ATTACHMENT_XR add constraint ticket_attachment_fkey  foreign key(TICKET_ID) references SUPPORT_TICKET (TICKET_ID)  on update no action on delete cascade;
Alter table core.SUPPORT_ACTIVITY add constraint support_ticket_activity_fkey  foreign key(TICKET_ID) references SUPPORT_TICKET (TICKET_ID)  on update no action on delete cascade;

--
-- trigger to increament human-readable ticket_no field, at the organizationId level, on row INSERT.
--
CREATE OR REPLACE FUNCTION inc_support_ticket_no() 
RETURNS trigger as
$$
begin
	new.ticket_no = 1+(select max(ticket_no) from core.support_ticket where organization_id=new.organization_id);
	return new;
END;

$$
LANGUAGE 'plpgsql';

CREATE TRIGGER inc_support_ticket_no_trigger BEFORE INSERT 
ON support_ticket FOR EACH ROW EXECUTE PROCEDURE inc_support_ticket_no();


/*Create email widget table*/
Create table core.EMAIL_CAMPAIGN_WIDGET(
	email_campaign_widget_id varchar(32) NOT NULL,
	email_instance_id varchar(32) NOT NULL,
	action_group_id varchar(32),
	module_display_id varchar(32) NOT NULL,
	config_opt1_txt varchar(100),
	config_opt2_txt varchar(100),
	create_dt timestamp NOT NULL,
	update_dt timestamp,
	primary key (email_campaign_widget_id)
) Without Oids;

--Add constraints
alter table core.EMAIL_CAMPAIGN_WIDGET
add foreign key (email_instance_id) REFERENCES email_campaign_instance(campaign_instance_id) ON DELETE CASCADE,
add foreign key (module_display_id) REFERENCES core.module_display(module_display_id);

create table core.EMAIL_CAMPAIGN_PROFILE_CONFIG (
 PROFILE_CONFIG_ID varchar(32) not null,
 PROFILE_ID varchar(32) not null,
 CAMPAIGN_INSTANCE_ID varchar(32) not null,
 CAMPAIGN_LOG_ID varchar(32) not null,
 KEY_NM varchar(80),
 VALUE_TXT text,
 CREATE_DT timestamp not null,
 primary key (PROFILE_CONFIG_ID)
) without oids;

--Add constraints
alter table core.EMAIL_CAMPAIGN_PROFILE_CONFIG
add foreign key (profile_id) REFERENCES core.profile(profile_id) ON DELETE CASCADE,
add foreign key (campaign_instance_id) REFERENCES core.email_campaign_instance (campaign_instance_id) ON DELETE CASCADE,
add foreign key (campaign_log_id) REFERENCES core.email_campaign_log (campaign_log_id) ON DELETE CASCADE;

-- change log table - wc core.
Create table core.CHANGE_LOG
(
	CHANGE_LOG_ID Varchar(32) NOT NULL,
	WC_SYNC_ID Varchar(32) NOT NULL,
	ORIG_TXT Varchar,
	DIFF_TXT Varchar NOT NULL,
	TYPE_CD Varchar(32),
	CREATE_DT timestamp NOT NULL,
Constraint pk_CHANGE_LOG Primary Key (CHANGE_LOG_ID)
);

Alter table core.CHANGE_LOG add foreign key(WC_SYNC_ID) references core.WC_SYNC (WC_SYNC_ID)  on update no action on delete cascade;


Create table core.SENT_EMAIL_PARAM
(
	SENT_EMAIL_PARAM_ID Varchar(32) NOT NULL,
	CAMPAIGN_LOG_ID Varchar(32) NULL,
	KEY_NM Varchar(150) NOT NULL,
	VALUE_TXT Varchar NOT NULL,
	CREATE_DT Timestamp NOT NULL,
	Primary Key (SENT_EMAIL_PARAM_ID)
) Without oids;

Alter table core.SENT_EMAIL_PARAM add foreign key(CAMPAIGN_LOG_ID) references core.EMAIL_CAMPAIGN_LOG (CAMPAIGN_LOG_ID)  on update no action on delete cascade;


--for UUID generation support: newid()
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


/* Create Tables */

Create table custom.BIOMEDGPS_GA_COLUMN
(
	GA_COLUMN_ID Varchar(32) NOT NULL,
	SECTION_ID Varchar(32) NOT NULL,
	COLUMN_NM Varchar(50) NOT NULL,
	ORDER_NO Smallint,
	BUTTON_TXT Varchar(10),
	SPECIAL_RULES_TXT Varchar(255),
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (GA_COLUMN_ID)
) Without Oids;

Create table custom.BIOMEDGPS_GA_COLUMN_ATTRIBUTE_XR
(
	COLUMN_ATTRIBUTE_XR_ID Varchar(32) NOT NULL,
	GA_COLUMN_ID Varchar(32) NOT NULL,
	ATTRIBUTE_ID Varchar(32) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
 primary key (COLUMN_ATTRIBUTE_XR_ID)
) Without Oids;


Create table custom.BIOMEDGPS_GA_SAVESTATE
(
	SAVE_STATE_ID Varchar(32) NOT NULL,
	USER_ID Varchar(32),
	LAYOUT_NM Varchar(50) NOT NULL,
	ORDER_NO Smallint,
	SLUG_TXT Varchar(10),
	OBJECT_BLOB Bytea,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (SAVE_STATE_ID)
) Without Oids;

Create table custom.BIOMEDGPS_COMPANY
(
	COMPANY_ID Varchar(32) NOT NULL,
	PARENT_ID Varchar(32),
	COMPANY_NM Varchar(100) NOT NULL,
	STARTUP_FLG Integer,
	SHORT_NM_TXT Varchar(30),
	ALIAS_NM Varchar(200),
	CURRENCY_TYPE_ID Varchar(32) NOT NULL,
	STATUS_NO Varchar(1) NOT NULL,
	REVENUE_NO Bigint,
	FOUNDED_YR Smallint,
	REVENUE_YR Varchar(6),
	COMPLETION_SCORE_NO Smallint,
	PRODUCT_NO Integer,
	PROFILE_NO Integer,
	PEOPLE_NO Integer,
	HOLDING_TXT Varchar(12),
	STOCK_ABBR_TXT Varchar(10),
	EXCHANGE_ID Varchar(32),
	FISCAL_YR_END_MON Varchar(12),
	ARCHIVE_REASON_TXT Varchar(250),
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (COMPANY_ID)
) Without Oids;


Create table custom.BIOMEDGPS_ALLIANCE_TYPE
(
	ALLIANCE_TYPE_ID Varchar(32) NOT NULL,
	TYPE_NM Varchar(100) NOT NULL,
	SECTION_CD Varchar(10) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
 primary key (ALLIANCE_TYPE_ID)
) Without Oids;


Create table custom.BIOMEDGPS_COMPANY_ALLIANCE_XR
(
	COMPANY_ALLIANCE_XR_ID Varchar(32) NOT NULL,
	ALLIANCE_TYPE_ID Varchar(32) NOT NULL,
	COMPANY_ID Varchar(32) NOT NULL,
	REL_COMPANY_ID Varchar(32) NOT NULL,
	ORDER_NO Smallint NULL,
	REFERENCE_TXT Varchar(200) NULL,
	CREATE_DT Timestamp NOT NULL,
 primary key (COMPANY_ALLIANCE_XR_ID)
) Without Oids;


Create table custom.BIOMEDGPS_COMPANY_INVESTOR
(
	INVESTOR_ID Varchar(32) NOT NULL,
	INVESTOR_COMPANY_ID Varchar(32) NOT NULL,
	INVESTEE_COMPANY_ID Varchar(32) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
 primary key (INVESTOR_ID)
) Without Oids;


Create table custom.BIOMEDGPS_LINK
(
	LINK_ID Varchar(32) NOT NULL,
	COMPANY_ID Varchar(32),
	MARKET_ID Varchar(32),
	PRODUCT_ID Varchar(32),
	INSIGHT_ID Varchar(32),
	UPDATE_ID Varchar(32),
	URL_TXT Varchar(1024) NOT NULL,
	CHECK_DT Timestamp,
	STATUS_NO Smallint,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (LINK_ID)
) Without Oids;


Create table custom.BIOMEDGPS_PRODUCT
(
	PRODUCT_ID Varchar(32) NOT NULL,
	PARENT_ID Varchar(32),
	COMPANY_ID Varchar(32) NOT NULL,
	PRODUCT_NM Varchar(255) NOT NULL,
	SHORT_NM Varchar(50),
	ALIAS_NM Varchar(255),
	ORDER_NO Smallint,
	META_KYWD_TXT Varchar(255),
	META_DESC Varchar(255),
	URL_ALIAS_TXT Varchar(100),
	GA_USAGE_FLG Smallint,
	REFERENCE_TXT Varchar(100),
	AUTHOR_PROFILE_ID Varchar(32),
	STATUS_NO Varchar(1) NOT NULL,
	PRODUCT_GROUP_ID Varchar(32),
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (PRODUCT_ID)
) Without Oids;


Create table custom.BIOMEDGPS_PRODUCT_ATTRIBUTE
(
	ATTRIBUTE_ID Varchar(32) NOT NULL,
	PARENT_ID Varchar(32),
	ATTRIBUTE_NM Varchar(100) NOT NULL,
	ABBR_NM Varchar(30),
	ACTIVE_FLG Smallint,
	ORDER_NO Smallint,
	TYPE_CD Varchar(20),
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (ATTRIBUTE_ID)
) Without Oids;


Create table custom.BIOMEDGPS_PRODUCT_ATTRIBUTE_XR
(
	PRODUCT_ATTRIBUTE_ID Varchar(32) NOT NULL,
	ATTRIBUTE_ID Varchar(32) NOT NULL,
	PRODUCT_ID Varchar(32) NOT NULL,
	TITLE_TXT Varchar(250),
	ALT_TITLE_TXT Varchar(250),
	VALUE_TXT Varchar,
	ORDER_NO Smallint,
	STATUS_NO Varchar(1),
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (PRODUCT_ATTRIBUTE_ID)
) Without Oids;


Create table custom.BIOMEDGPS_PRODUCT_MODULESET_XR
(
	PRODUCT_MODULESET_ID Varchar(32) NOT NULL,
	MODULESET_ID Varchar(32) NOT NULL,
	ATTRIBUTE_ID Varchar(32) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
 primary key (PRODUCT_MODULESET_ID)
) Without Oids;


Create table custom.BIOMEDGPS_PRODUCT_MODULESET
(
	MODULESET_ID Varchar(32) NOT NULL,
	SET_NM Varchar(50) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
 primary key (MODULESET_ID)
) Without Oids;


Create table custom.BIOMEDGPS_PRODUCT_ALLIANCE_XR
(
	PRODUCT_ALLIANCE_XR_ID Varchar(32) NOT NULL,
	ALLIANCE_TYPE_ID Varchar(32) NOT NULL,
	PRODUCT_ID Varchar(32) NOT NULL,
	COMPANY_ID Varchar(32) NOT NULL,
	REFERENCE_TXT Varchar(200) NOT NULL,
	ORDER_NO Smallint,
	GA_DISPLAY_FLG Smallint NOT NULL Default 0,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (PRODUCT_ALLIANCE_XR_ID)
) Without Oids;


Create table custom.BIOMEDGPS_MARKET
(
	MARKET_ID Varchar(32) NOT NULL,
	PARENT_ID Varchar(32),
	MARKET_NM Varchar(150) NOT NULL,
	SHORT_NM Varchar(50),
	ALIAS_NM Varchar(255),
	STATUS_NO Varchar(1),
	ORDER_NO Smallint,
	REGION_CD varchar(3),
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (MARKET_ID)
) Without Oids;


Create table custom.BIOMEDGPS_MARKET_ATTRIBUTE
(
	ATTRIBUTE_ID Varchar(32) NOT NULL,
	PARENT_ID Varchar(32),
	ATTRIBUTE_NM Varchar(100) NOT NULL,
	ACTIVE_FLG Smallint,
	ORDER_NO Smallint,
	TYPE_CD Varchar(15),
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (ATTRIBUTE_ID)
) Without Oids;


Create table custom.BIOMEDGPS_MARKET_ATTRIBUTE_XR
(
	MARKET_ATTRIBUTE_ID Varchar(32) NOT NULL,
	MARKET_ID Varchar(32) NOT NULL,
	ATTRIBUTE_ID Varchar(32) NOT NULL,
	VALUE_TXT Varchar,
	TITLE_TXT Varchar(256),
	VALUE_1_TXT Varchar(256),
	ORDER_NO Smallint,
	STATUS_NO Varchar(1),
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (MARKET_ATTRIBUTE_ID)
) Without Oids;


Create table custom.BIOMEDGPS_SECTION
(
	SECTION_ID Varchar(32) NOT NULL,
	PARENT_ID Varchar(32),
	SECTION_NM Varchar(50) NOT NULL,
	ABBR_NM Varchar(20),
	ORDER_NO Smallint,
	SOLR_TOKEN_TXT Varchar(30) NOT NULL,
	FD_PUB_YR Smallint,
	FD_PUB_QTR Smallint,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (SECTION_ID)
) Without Oids;


Create table custom.BIOMEDGPS_COMPANY_SECTION
(
	COMPANY_SECTION_XR_ID Varchar(32) NOT NULL,
	SECTION_ID Varchar(32) NOT NULL,
	COMPANY_ID Varchar(32) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
 primary key (COMPANY_SECTION_XR_ID)
) Without Oids;


Create table custom.BIOMEDGPS_NOTE
(
	NOTE_ID Varchar(32) NOT NULL,
	USER_ID Varchar(32) NOT NULL,
	TEAM_ID Varchar(32),
	COMPANY_ID Varchar(32),
	PRODUCT_ID Varchar(32),
	MARKET_ID Varchar(32),
	ATTRIBUTE_ID Varchar(32),
	NOTE_NM Varchar(150),
	NOTE_TXT Varchar NOT NULL,
	FILE_PATH_TXT Varchar(200),
	EXPIRATION_DT Timestamp,
	CREATE_DT Timestamp,
	UPDATE_DT Timestamp,
 primary key (NOTE_ID)
) Without Oids;


Create table custom.BIOMEDGPS_PRODUCT_SECTION
(
	PRODUCT_SECTION_XR_ID Varchar(32) NOT NULL,
	PRODUCT_ID Varchar(32) NOT NULL,
	SECTION_ID Varchar(32) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
 primary key (PRODUCT_SECTION_XR_ID)
) Without Oids;


Create table custom.BIOMEDGPS_MARKET_SECTION
(
	MARKET_SECTION_XR_ID Varchar(32) NOT NULL,
	MARKET_ID Varchar(32) NOT NULL,
	SECTION_ID Varchar(32) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
 primary key (MARKET_SECTION_XR_ID)
) Without Oids;


Create table custom.BIOMEDGPS_UPDATE
(
	UPDATE_ID Varchar(32) NOT NULL,
	MARKET_ID Varchar(32),
	PRODUCT_ID Varchar(32),
	COMPANY_ID Varchar(32),
	TITLE_TXT Varchar(150) NOT NULL,
	TYPE_CD smallint NOT NULL,
	MESSAGE_TXT Varchar NOT NULL,
	TWITTER_TXT Varchar(200),
	PUBLISH_DT Timestamp,
	STATUS_CD Varchar(1),
	TWEET_FLG smallint,
	CREATOR_PROFILE_ID Varchar(32) NOT NULL,
	ORDER_NO smallint default 0,
	EMAIL_FLG smallint default 1,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (UPDATE_ID)
) Without Oids;

Create table custom.BIOMEDGPS_UPDATE_SECTION
(
	UPDATE_SECTION_XR_ID Varchar(32) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
	SECTION_ID Varchar(32) NOT NULL,
	UPDATE_ID Varchar(32) NOT NULL,
 primary key (UPDATE_SECTION_XR_ID)
) Without Oids;

Create table custom.BIOMEDGPS_COMPANY_LOCATION
(
	LOCATION_ID Varchar(32) NOT NULL,
	COMPANY_ID Varchar(32) NOT NULL,
	LOCATION_NM Varchar(100) NOT NULL,
	ADDRESS_TXT Varchar(150),
	ADDRESS2_TXT Varchar(150),
	CITY_NM Varchar(50),
	STATE_CD Varchar(25),
	ZIP_CD Varchar(15),
	COUNTRY_CD Varchar(3),
	PHONE_TXT Varchar(20),
	ALT_PHONE_TXT Varchar(20),
	PRIMARY_LOCN_FLG Smallint,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (LOCATION_ID)
) Without Oids;


Create table custom.BIOMEDGPS_COMPANY_ATTRIBUTE
(
	ATTRIBUTE_ID Varchar(32) NOT NULL,
	PARENT_ID Varchar(32),
	ATTRIBUTE_NM Varchar(100) NOT NULL,
	ACTIVE_FLG Smallint,
	TYPE_NM Varchar(20) NOT NULL,
	DISPLAY_ORDER_NO Smallint,
	URL_ALIAS_TXT Varchar(100),
	CREATE_DT Timestamp,
	UPDATE_DT Timestamp,
 primary key (ATTRIBUTE_ID)
) Without Oids;


Create table custom.BIOMEDGPS_COMPANY_ATTRIBUTE_XR
(
	COMPANY_ATTRIBUTE_ID Varchar(32) NOT NULL,
	ATTRIBUTE_ID Varchar(32) NOT NULL,
	COMPANY_ID Varchar(32) NOT NULL,
	VALUE_TXT Varchar,
	TITLE_TXT Varchar(250),
	ALT_TITLE_TXT Varchar(250),
	ORDER_NO Smallint,
	STATUS_NO Varchar(1),
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (COMPANY_ATTRIBUTE_ID)
) Without Oids;



Create table custom.BIOMEDGPS_USER
(
	USER_ID Varchar(32) NOT NULL,
	PROFILE_ID Varchar(32) NOT NULL,
	ACCOUNT_ID Varchar(32),
	REGISTER_SUBMITTAL_ID Varchar(32),
	STATUS_CD varchar(10),
	GA_AUTH_FLG Smallint,
	FD_AUTH_FLG Smallint,
	MKT_AUTH_FLG Smallint,
	ACCT_OWNER_FLG Smallint,
	EXPIRATION_DT Timestamp,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (USER_ID)
) Without Oids;


Create table custom.BIOMEDGPS_USER_TEAM_XR
(
	USER_TEAM_XR_ID Varchar(32) NOT NULL,
	TEAM_ID Varchar(32) NOT NULL,
	USER_ID Varchar(32) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
 primary key (USER_TEAM_XR_ID)
) Without Oids;


Create table custom.BIOMEDGPS_ACCOUNT
(
	ACCOUNT_ID Varchar(32) NOT NULL,
	COMPANY_ID Varchar(32),
	ACCOUNT_NM Varchar(50) NOT NULL,
	TYPE_ID Varchar(20),
	START_DT Timestamp NOT NULL,
	EXPIRATION_DT Timestamp,
	OWNER_PROFILE_ID Varchar(32),
	ADDRESS_TXT Varchar(75),
	ADDRESS2_TXT Varchar(75),
	CITY_NM Varchar(50),
	STATE_CD Varchar(5),
	ZIP_CD Varchar(10),
	COUNTRY_CD Varchar(3),
	STATUS_NO Varchar(1),
	GA_AUTH_FLG Smallint,
	FD_AUTH_FLG Smallint,
	MKT_AUTH_FLG Smallint,
	SEATS_NO Smallint,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (ACCOUNT_ID)
) Without Oids;


Create table custom.BIOMEDGPS_TEAM
(
	TEAM_ID Varchar(32) NOT NULL,
	ACCOUNT_ID Varchar(32) NOT NULL,
	TEAM_NM Varchar(50) NOT NULL,
	DEFAULT_FLG Smallint,
	PRIVATE_FLG Smallint,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (TEAM_ID)
) Without Oids;


Create table custom.BIOMEDGPS_ACCOUNT_ACL
(
	ACCOUNT_ACL_ID Varchar(32) NOT NULL,
	SECTION_ID Varchar(32) NOT NULL,
	ACCOUNT_ID Varchar(32) NOT NULL,
	BROWSE_NO Smallint,
	UPDATES_NO Smallint,
	FD_NO Smallint,
	GA_NO Smallint,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (ACCOUNT_ACL_ID)
) Without Oids;


Create table custom.BIOMEDGPS_STOCK_EXCHANGE
(
	EXCHANGE_ID Varchar(32) NOT NULL,
	NAME_TXT Varchar(50) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (EXCHANGE_ID)
) Without Oids;



Create table custom.BIOMEDGPS_FD_REVENUE
(
	REVENUE_ID Varchar(32) NOT NULL,
	COMPANY_ID Varchar(32) NOT NULL,
	SECTION_ID Varchar(32),
	REGION_CD Varchar(5) NOT NULL,
	YEAR_NO Smallint NOT NULL,
	Q1_NO Bigint,
	Q2_NO Bigint,
	Q3_NO Bigint,
	Q4_NO Bigint,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (REVENUE_ID)
) Without Oids;


Create table custom.BIOMEDGPS_FD_SCENARIO
(
	SCENARIO_ID Varchar(32) NOT NULL,
	USER_ID Varchar(32) NOT NULL,
	TEAM_ID Varchar(32),
	SCENARIO_NM Varchar(50) NOT NULL,
	STATUS_FLG Varchar(1),
	REFRESH_DT Timestamp,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (SCENARIO_ID)
) Without Oids;


Create table custom.BIOMEDGPS_FD_SCENARIO_OVERLAY
(
	OVERLAY_ID Varchar(32) NOT NULL,
	COMPANY_ID Varchar(32),
	SCENARIO_ID Varchar(32) NOT NULL,
	REVENUE_ID Varchar(32),
	YEAR_NO Smallint,
	Q1_NO Bigint,
	Q2_NO Bigint,
	Q3_NO Bigint,
	Q4_NO Bigint,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (OVERLAY_ID)
) Without Oids;


Create table custom.BIOMEDGPS_FD_REVENUE_FOOTNOTE
(
	FOOTNOTE_ID Varchar(32) NOT NULL,
	REGION_CD Varchar(5) NOT NULL,
	FOOTNOTE_TXT Varchar(500) NOT NULL,
	EXPIRATION_DT Timestamp,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
	SECTION_ID Varchar(32) NOT NULL,
	COMPANY_ID Varchar(32),
 primary key (FOOTNOTE_ID)
) Without Oids;



Create table custom.BIOMEDGPS_PRODUCT_REGULATORY
(
	REGULATORY_ID Varchar(32) NOT NULL,
	REGION_ID Varchar(32) NOT NULL,
	PATH_ID Varchar(32) NOT NULL,
	PRODUCT_ID Varchar(32) NOT NULL,
	STATUS_ID Varchar(32),
	STATUS_DT Date,
	INTRO_DT Date,
	REFERENCE_TXT Varchar(255),
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (REGULATORY_ID)
) Without Oids;


Create table custom.BIOMEDGPS_REGULATORY_STATUS
(
	STATUS_ID Varchar(32) NOT NULL,
	STATUS_NM Varchar(30) NOT NULL,
	STATUS_TXT Varchar(15),
	CREATE_DT Timestamp NOT NULL,
 primary key (STATUS_ID)
) Without Oids;


Create table custom.BIOMEDGPS_REGULATORY_PATH
(
	PATH_ID Varchar(32) NOT NULL,
	PATH_NM Varchar(50) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
 primary key (PATH_ID)
) Without Oids;


Create table custom.BIOMEDGPS_REGULATORY_REGION
(
	REGION_ID Varchar(32) NOT NULL,
	REGION_NM Varchar(30) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
 primary key (REGION_ID)
) Without Oids;


Create table custom.BIOMEDGPS_INSIGHT
(
	INSIGHT_ID Varchar(32) NOT NULL,
	CREATOR_PROFILE_ID Varchar(32) NOT NULL,
	TITLE_TXT Varchar(255) NOT NULL,
	TYPE_CD Smallint NOT NULL,
	ABSTRACT_TXT Varchar(1000),
	BYLINE_TXT Varchar(255),
	CONTENT_TXT Varchar,
	SIDE_CONTENT_TXT Varchar(7500),
	FEATURED_FLG Smallint,
	FEATURED_IMAGE_TXT Varchar(255),
	STATUS_CD Varchar(1) NOT NULL,
	ORDER_NO Smallint,
	PUBLISH_DT Timestamp,
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
 primary key (INSIGHT_ID)
) Without Oids;


Create table custom.BIOMEDGPS_INSIGHT_SECTION
(
	INSIGHT_SECTION_XR_ID Varchar(32) NOT NULL,
	INSIGHT_ID Varchar(32) NOT NULL,
	SECTION_ID Varchar(32) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
 primary key (INSIGHT_SECTION_XR_ID)
) Without Oids;


create table custom.BIOMEDGPS_EXPLORER_QUERY (
	explorer_query_id varchar(32) not null,
	user_id varchar(32) not null,
	query_txt varchar not null,
	query_nm varchar(128) not null,
	create_dt timestamp not null,
 primary key (explorer_query_id)
) Without Oids;



Create table custom.BIOMEDGPS_AUDIT_LOG
(
	AUDIT_LOG_ID Varchar(32) NOT NULL,
	COMPANY_ID Varchar(32) NOT NULL,
	AUDITOR_PROFILE_ID Varchar(32) NOT NULL,
	STATUS_CD Varchar(30) NOT NULL,
	START_DT Timestamp NOT NULL,
	COMPLETE_DT Timestamp,
	UPDATE_DT Timestamp NOT NULL,
 primary key (AUDIT_LOG_ID)
) Without Oids;


/* Create Foreign Keys */
Alter table custom.BIOMEDGPS_GA_COLUMN_ATTRIBUTE_XR add  foreign key (GA_COLUMN_ID) references custom.BIOMEDGPS_GA_COLUMN (GA_COLUMN_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_GA_COLUMN add  foreign key (SECTION_ID) references custom.BIOMEDGPS_SECTION (SECTION_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_COMPANY add  foreign key (PARENT_ID) references custom.BIOMEDGPS_COMPANY (COMPANY_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_COMPANY add  foreign key (EXCHANGE_ID) references custom.BIOMEDGPS_STOCK_EXCHANGE (EXCHANGE_ID) on update restrict on delete set null;

Alter table custom.BIOMEDGPS_COMPANY_INVESTOR add  foreign key (INVESTOR_COMPANY_ID) references custom.BIOMEDGPS_COMPANY (COMPANY_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_COMPANY_INVESTOR add  foreign key (INVESTEE_COMPANY_ID) references custom.BIOMEDGPS_COMPANY (COMPANY_ID) on update restrict on delete restrict;

Alter table custom.BIOMEDGPS_PRODUCT add  foreign key (COMPANY_ID) references custom.BIOMEDGPS_COMPANY (COMPANY_ID) on update restrict on delete restrict;

Alter table custom.BIOMEDGPS_COMPANY_SECTION add  foreign key (COMPANY_ID) references custom.BIOMEDGPS_COMPANY (COMPANY_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_COMPANY_ALLIANCE_XR add  foreign key (ALLIANCE_TYPE_ID) references custom.BIOMEDGPS_ALLIANCE_TYPE (ALLIANCE_TYPE_ID) on update restrict on delete restrict;

Alter table custom.BIOMEDGPS_COMPANY_ALLIANCE_XR add  foreign key (COMPANY_ID) references custom.BIOMEDGPS_COMPANY (COMPANY_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_COMPANY_ALLIANCE_XR add  foreign key (REL_COMPANY_ID) references custom.BIOMEDGPS_COMPANY (COMPANY_ID) on update restrict on delete restrict;

Alter table custom.BIOMEDGPS_COMPANY_LOCATION add  foreign key (COMPANY_ID) references custom.BIOMEDGPS_COMPANY (COMPANY_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_COMPANY_ATTRIBUTE_XR add  foreign key (COMPANY_ID) references custom.BIOMEDGPS_COMPANY (COMPANY_ID) on update restrict on delete restrict;

Alter table custom.BIOMEDGPS_ACCOUNT add  foreign key (COMPANY_ID) references custom.BIOMEDGPS_COMPANY (COMPANY_ID) on update restrict on delete restrict;

Alter table custom.BIOMEDGPS_LINK add  foreign key (COMPANY_ID) references custom.BIOMEDGPS_COMPANY (COMPANY_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_LINK add  foreign key (PRODUCT_ID) references custom.BIOMEDGPS_PRODUCT (PRODUCT_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_LINK add  foreign key (MARKET_ID) references custom.BIOMEDGPS_MARKET (MARKET_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_LINK add  foreign key (INSIGHT_ID) references custom.BIOMEDGPS_INSIGHT (INSIGHT_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_LINK add  foreign key (UPDATE_ID) references custom.BIOMEDGPS_UPDATE (UPDATE_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_PRODUCT add  foreign key (PARENT_ID) references custom.BIOMEDGPS_PRODUCT (PRODUCT_ID) on update restrict on delete restrict;

Alter table custom.BIOMEDGPS_PRODUCT_ATTRIBUTE_XR add  foreign key (PRODUCT_ID) references custom.BIOMEDGPS_PRODUCT (PRODUCT_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_PRODUCT_SECTION add  foreign key (PRODUCT_ID) references custom.BIOMEDGPS_PRODUCT (PRODUCT_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_PRODUCT_ATTRIBUTE_XR add  foreign key (ATTRIBUTE_ID) references custom.BIOMEDGPS_PRODUCT_ATTRIBUTE (ATTRIBUTE_ID) on update restrict on delete restrict;

Alter table custom.BIOMEDGPS_PRODUCT_ATTRIBUTE add  foreign key (PARENT_ID) references custom.BIOMEDGPS_PRODUCT_ATTRIBUTE (ATTRIBUTE_ID) on update restrict on delete restrict;

Alter table custom.BIOMEDGPS_MARKET_ATTRIBUTE_XR add  foreign key (MARKET_ID) references custom.BIOMEDGPS_MARKET (MARKET_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_MARKET add  foreign key (PARENT_ID) references custom.BIOMEDGPS_MARKET (MARKET_ID) on update restrict on delete cascade;


Alter table custom.BIOMEDGPS_MARKET_SECTION add  foreign key (MARKET_ID) references custom.BIOMEDGPS_MARKET (MARKET_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_MARKET_ATTRIBUTE_XR add  foreign key (ATTRIBUTE_ID) references custom.BIOMEDGPS_MARKET_ATTRIBUTE (ATTRIBUTE_ID) on update restrict on delete restrict;

Alter table custom.BIOMEDGPS_MARKET_ATTRIBUTE add  foreign key (PARENT_ID) references custom.BIOMEDGPS_MARKET_ATTRIBUTE (ATTRIBUTE_ID) on update restrict on delete restrict;

Alter table custom.BIOMEDGPS_SECTION add  foreign key (PARENT_ID) references custom.BIOMEDGPS_SECTION (SECTION_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_COMPANY_SECTION add  foreign key (SECTION_ID) references custom.BIOMEDGPS_SECTION (SECTION_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_PRODUCT_SECTION add  foreign key (SECTION_ID) references custom.BIOMEDGPS_SECTION (SECTION_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_MARKET_SECTION add  foreign key (SECTION_ID) references custom.BIOMEDGPS_SECTION (SECTION_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_COMPANY_ATTRIBUTE_XR add  foreign key (ATTRIBUTE_ID) references custom.BIOMEDGPS_COMPANY_ATTRIBUTE (ATTRIBUTE_ID) on update restrict on delete restrict;

Alter table custom.BIOMEDGPS_COMPANY_ATTRIBUTE add  foreign key (PARENT_ID) references custom.BIOMEDGPS_COMPANY_ATTRIBUTE (ATTRIBUTE_ID) on update restrict on delete restrict;

Alter table custom.BIOMEDGPS_NOTE add  foreign key (USER_ID) references custom.BIOMEDGPS_USER (USER_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_NOTE add  foreign key (TEAM_ID) references custom.BIOMEDGPS_TEAM (TEAM_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_NOTE add  foreign key (COMPANY_ID) references custom.BIOMEDGPS_COMPANY (COMPANY_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_NOTE add  foreign key (MARKET_ID) references custom.BIOMEDGPS_MARKET (MARKET_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_NOTE add  foreign key (PRODUCT_ID) references custom.BIOMEDGPS_PRODUCT (PRODUCT_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_USER_TEAM_XR add  foreign key (USER_ID) references custom.BIOMEDGPS_USER (USER_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_TEAM add  foreign key (ACCOUNT_ID) references custom.BIOMEDGPS_ACCOUNT (ACCOUNT_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_USER add  foreign key (ACCOUNT_ID) references custom.BIOMEDGPS_ACCOUNT (ACCOUNT_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_USER_TEAM_XR add  foreign key (TEAM_ID) references custom.BIOMEDGPS_TEAM (TEAM_ID) on update restrict on delete cascade;


Alter table custom.BIOMEDGPS_FD_REVENUE_FOOTNOTE add  foreign key (SECTION_ID) references custom.BIOMEDGPS_SECTION (SECTION_ID) on update restrict on delete restrict;

Alter table custom.BIOMEDGPS_FD_REVENUE_FOOTNOTE add  foreign key (COMPANY_ID) references custom.BIOMEDGPS_COMPANY (COMPANY_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_FD_SCENARIO add foreign key (USER_ID) references custom.BIOMEDGPS_USER (USER_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_FD_SCENARIO add foreign key (TEAM_ID) references custom.BIOMEDGPS_TEAM (TEAM_ID) on update restrict on delete set null;

Alter table custom.BIOMEDGPS_FD_SCENARIO_OVERLAY add  foreign key (COMPANY_ID) references custom.BIOMEDGPS_COMPANY (COMPANY_ID) on update restrict on delete restrict;

Alter table custom.BIOMEDGPS_FD_SCENARIO_OVERLAY add  foreign key (REVENUE_ID) references custom.BIOMEDGPS_FD_REVENUE (REVENUE_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_FD_SCENARIO_OVERLAY add  foreign key (SCENARIO_ID) references custom.BIOMEDGPS_FD_SCENARIO (SCENARIO_ID) on update restrict on delete cascade;


Alter table custom.BIOMEDGPS_PRODUCT_ALLIANCE_XR add  foreign key (ALLIANCE_TYPE_ID) references custom.BIOMEDGPS_ALLIANCE_TYPE (ALLIANCE_TYPE_ID) on update restrict on delete restrict;

Alter table custom.BIOMEDGPS_PRODUCT_ALLIANCE_XR add  foreign key (COMPANY_ID) references custom.BIOMEDGPS_COMPANY (COMPANY_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_PRODUCT_ALLIANCE_XR add  foreign key (PRODUCT_ID) references custom.BIOMEDGPS_PRODUCT (PRODUCT_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_PRODUCT_MODULESET_XR add  foreign key (ATTRIBUTE_ID) references custom.BIOMEDGPS_PRODUCT_ATTRIBUTE (ATTRIBUTE_ID) on update restrict on delete restrict;

Alter table custom.BIOMEDGPS_PRODUCT_MODULESET_XR add  foreign key (MODULESET_ID) references custom.BIOMEDGPS_PRODUCT_MODULESET (MODULESET_ID) on update restrict on delete restrict;


Alter table custom.BIOMEDGPS_PRODUCT_REGULATORY add  foreign key (PRODUCT_ID) references custom.BIOMEDGPS_PRODUCT (PRODUCT_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_PRODUCT_REGULATORY add  foreign key (STATUS_ID) references custom.BIOMEDGPS_REGULATORY_STATUS (STATUS_ID) on update restrict on delete restrict;

Alter table custom.BIOMEDGPS_PRODUCT_REGULATORY add  foreign key (PATH_ID) references custom.BIOMEDGPS_REGULATORY_PATH (PATH_ID) on update restrict on delete restrict;

Alter table custom.BIOMEDGPS_PRODUCT_REGULATORY add  foreign key (REGION_ID) references custom.BIOMEDGPS_REGULATORY_REGION (REGION_ID) on update restrict on delete restrict;


Alter table custom.BIOMEDGPS_UPDATE add  foreign key (COMPANY_ID) references custom.BIOMEDGPS_COMPANY (COMPANY_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_UPDATE add  foreign key (PRODUCT_ID) references custom.BIOMEDGPS_PRODUCT (PRODUCT_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_UPDATE_SECTION add  foreign key (SECTION_ID) references custom.BIOMEDGPS_SECTION (SECTION_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_UPDATE_SECTION add  foreign key (UPDATE_ID) references custom.BIOMEDGPS_UPDATE (UPDATE_ID) on update restrict on delete cascade;


Alter table custom.BIOMEDGPS_ACCOUNT_ACL add  foreign key (SECTION_ID) references custom.BIOMEDGPS_SECTION (SECTION_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_ACCOUNT_ACL add  foreign key (ACCOUNT_ID) references custom.BIOMEDGPS_ACCOUNT (ACCOUNT_ID) on update restrict on delete cascade;


Alter table custom.BIOMEDGPS_INSIGHT_SECTION add  foreign key (SECTION_ID) references custom.BIOMEDGPS_SECTION (SECTION_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_INSIGHT_SECTION add  foreign key (INSIGHT_ID) references custom.BIOMEDGPS_INSIGHT (INSIGHT_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_EXPLORER_QUERY add  foreign key (USER_ID) references custom.BIOMEDGPS_USER (USER_ID) on update restrict on delete cascade;

Alter table custom.BIOMEDGPS_AUDIT_LOG add  foreign key (COMPANY_ID) references custom.BIOMEDGPS_COMPANY (COMPANY_ID) on update restrict on delete cascade;




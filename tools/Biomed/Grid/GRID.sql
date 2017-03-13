delete from custom.biomedgps_market_attribute_xr where market_attribute_id like 'MARKET%';
delete from custom.biomedgps_market_attribute where attribute_id = 'GRID';
drop table if exists custom.BIOMEDGPS_GRID_DETAIL;
drop table if exists custom.BIOMEDGPS_GRID;
drop table if exists custom.BIOMEDGPS_GRID_TYPE;
drop table if exists custom.BIOMEDGPS_GRID_DETAIL_TYPE;
drop table if exists custom.BIOMEDGPS_CHART_TYPE;

/** Create the main GRID Table **/
Create table custom.BIOMEDGPS_GRID
(
	GRID_ID Varchar(32) NOT NULL,
	GRID_TYPE_CD Varchar(20),
	TITLE_NM Varchar(128) NOT NULL,
	SUBTITLE_NM Varchar(80),
	DISCLAIMER_TXT Varchar(576),
	Y_TITLE_PRI_NM Varchar(40),
	Y_TITLE_SEC_NM Varchar(40),
	X_TITLE_NM Varchar(80),
	SERIES_LABEL_TXT Varchar(80),
	SERIES_1_NM Varchar(80),
	SERIES_2_NM Varchar(80),
	SERIES_3_NM Varchar(80),
	SERIES_4_NM Varchar(80),
	SERIES_5_NM Varchar(80),
	SERIES_6_NM Varchar(80),
	SERIES_7_NM Varchar(80),
	SERIES_8_NM Varchar(80),
	SERIES_9_NM Varchar(80),
	SERIES_10_NM Varchar(80),
	SLUG_TXT Varchar(16),
	APPROVE_FLG Boolean,
	DECIMAL_DISPLAY_NO Integer,
	UPDATE_DT Timestamp,
	CREATE_DT Timestamp NOT NULL,
constraint pk_BIOMEDGPS_GRID primary key (GRID_ID)
) Without Oids;

/** Create the main Grid Type table.  Lookup for chart, table or finance dashboard */
Create table custom.BIOMEDGPS_GRID_TYPE
(
	GRID_TYPE_CD Varchar(20) NOT NULL,
	TYPE_NM Varchar(40) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
constraint pk_BIOMEDGPS_GRID_TYPE primary key (GRID_TYPE_CD)
) Without Oids;

/** Rows for the grid */
Create table custom.BIOMEDGPS_GRID_DETAIL
(
	GRID_DETAIL_ID Varchar(32) NOT NULL,
	GRID_ID Varchar(32) NOT NULL,
	GRID_DETAIL_TYPE_CD Varchar(20),
	LABEL_TXT Varchar(256) NOT NULL,
	ORDER_NO Integer,
	VALUE_1_TXT Varchar(256),
	VALUE_2_TXT Varchar(256),
	VALUE_3_TXT Varchar(256),
	VALUE_4_TXT Varchar(256),
	VALUE_5_TXT Varchar(256),
	VALUE_6_TXT Varchar(256),
	VALUE_7_TXT Varchar(256),
	VALUE_8_TXT Varchar(256),
	VALUE_9_TXT Varchar(256),
	VALUE_10_TXT Varchar(256),
	CREATE_DT Timestamp NOT NULL,
	UPDATE_DT Timestamp,
constraint pk_BIOMEDGPS_GRID_DETAIL primary key (GRID_DETAIL_ID)
) Without Oids;

/* Identifier for the row style.  Data, total, etc ...*/
Create table custom.BIOMEDGPS_GRID_DETAIL_TYPE
(
	GRID_DETAIL_TYPE_CD Varchar(20) NOT NULL,
	TYPE_NM Varchar(40) NOT NULL,
	CSS_CLASS_NM Varchar(40) NOT NULL,
	CREATE_DT Timestamp NOT NULL,
constraint pk_BIOMEDGPS_GRID_DETAIL_TYPE primary key (GRID_DETAIL_TYPE_CD)
) Without Oids;

/* List of support chart types */
Create table custom.BIOMEDGPS_CHART_TYPE
(
	CHART_TYPE_CD Varchar(20) NOT NULL,
	TYPE_NM Varchar(40) NOT NULL,
	FA_ICON_TXT Varchar(32),
	CREATE_DT Timestamp NOT NULL,
constraint pk_BIOMEDGPS_CHART_TYPE primary key (CHART_TYPE_CD)
) Without Oids;

/* Create Foreign Keys */
Create index IX_BIOMEDGEP_GRID_DETAIL_FKEY_BIOMEDGPS_GRID_DETAIL on custom.BIOMEDGPS_GRID_DETAIL (GRID_ID);
Alter table custom.BIOMEDGPS_GRID_DETAIL add Constraint BIOMEDGEP_GRID_DETAIL_FKEY foreign key (GRID_ID) references custom.BIOMEDGPS_GRID (GRID_ID) on update restrict on delete cascade;
Create index IX_BIOMED_GRID_TYPE_GRID_FKEY_BIOMEDGPS_GRID on custom.BIOMEDGPS_GRID (GRID_TYPE_CD);
Alter table custom.BIOMEDGPS_GRID add Constraint BIOMED_GRID_TYPE_GRID_FKEY foreign key (GRID_TYPE_CD) references custom.BIOMEDGPS_GRID_TYPE (GRID_TYPE_CD) on update restrict on delete restrict;
Create index IX_BIOMEDGPS_GRIDDETAIL_TYPE_FKEY_BIOMEDGPS_GRID_DETAIL on custom.BIOMEDGPS_GRID_DETAIL (GRID_DETAIL_TYPE_CD);
Alter table custom.BIOMEDGPS_GRID_DETAIL add Constraint BIOMEDGPS_GRIDDETAIL_TYPE_FKEY foreign key (GRID_DETAIL_TYPE_CD) references custom.BIOMEDGPS_GRID_DETAIL_TYPE (GRID_DETAIL_TYPE_CD) on update restrict on delete restrict;

/* populate the row attributes for the grids */
insert into custom.biomedgps_grid_detail_type values 
('DATA', 'Chartable Grid Data', 'bs-data', now()),
('UNCHARTED_DATA', 'Non-Chartable Grid Data', 'bs-nochart-data', now()),
('HEADING', 'Heading Row', 'bs-heading', now()),
('SUB_TOTAL', 'Sub-Total Row', 'bs-sub-total', now()),
('TOTAL', 'Total Row', 'bs-total', now());

/* Set the grid types */
insert into custom.biomedgps_grid_type values 
('CHART', 'Biomed Chart Data Orig', now()),
('TABLE', 'BioMed Table Data Orig', now()),
('FINANCE_DASHBOARD', 'Biomed Financial Dashboard Genertated', now()),
('GRID', 'BioMed Grid Data', now());

/** Load the chart types */
insert into custom.biomedgps_chart_type values 
('AREA','Area Chart','area-chart', now()),
('BAR','Bar Chart','bar-chart', now()),
('BUBBLE','Bubble Chart','toggle-off', now()),
('COLUMN','Column Chart','bar-chart', now()),
('COMBO','Combo Chart','object-group', now()),
('DONUT','Donut Chart','pie-chart', now()),
('GEO','Geo Chart','globe', now()),
('LINE','Line Chart','line-chart', now()),
('PIE','Pie Chart','pie-chart', now()),
('SCATTER','Scatter Chart','plus-square-o', now()),
('TABLE','Table Grid','table', now());

/* Copy data from charts_chart to biomedgps_grid */
insert into custom.biomedgps_grid (grid_id, 
title_nm, update_dt, slug_txt, subtitle_nm, disclaimer_txt, decimal_display_no, 
series_1_nm, series_2_nm, series_3_nm, series_4_nm, series_5_nm, series_6_nm, 
series_7_nm, series_8_nm, series_9_nm, series_10_nm, y_title_pri_nm, create_dt, approve_flg, grid_type_cd)
SELECT  ('CHART_' + trim(to_char(id, '9999'))), title, last_update, slug, subtitle, disclaimer, decimals,
series1, series2, series3, series4, series5, series6, series7,
series8, series9, series10, y_axis_title, last_update, published, 'CHART' 
from biomedgps.charts_chart;

/* Copy detail from charts_series to biomedgps_grid_detail */
insert into custom.biomedgps_grid_detail (grid_detail_id, grid_id, 
label_txt, order_no, update_dt, value_1_txt,value_2_txt, value_3_txt, value_4_txt,  value_5_txt, 
value_6_txt, value_7_txt, value_8_txt, value_9_txt, value_10_txt, create_dt)
SELECT id, 'CHART_' + trim(to_char(chart_id, '9999')), label, sort_order, last_update, value1, value2,
value3, value4, value5, value6, value7, value8, value9, value10, last_update
from biomedgps.charts_series;

/* Add an attribute to the market attribute table */
insert into custom.biomedgps_market_attribute (attribute_id, attribute_nm, active_flg, type_cd, create_dt) 
values
('GRID', 'Grids and Charts', 1, 'GRID', now());

/* Add column to martket attribues xr
alter table custom.biomedgps_market_attribute_xr add column VALUE_1_TXT varchar(256); */

/* Associate the charts to the market reports */
insert into custom.biomedgps_market_attribute_xr (
market_attribute_id, market_id, attribute_id, create_dt, order_no, status_no, value_txt, value_1_txt
)
select 'MARKET_CHART_' + trim(to_char(id, '9999')), object_id, 
'GRID', now(), "order", 
case
	when published then 'P'
	else 'E'
end,
case 
	when chart_type = 'p' then 'PIE'
	when chart_type = 'b' then 'COLUMN'
end,
'CHART_' + trim(to_char(id, '9999'))
from biomedgps.charts_chart
where content_type_id = 62 and object_id in (select to_number(market_id, '999999') from custom.biomedgps_market);

/** Migrates the data from the embed_grid table to the new grid tables **/
insert into custom.biomedgps_grid (grid_id, grid_type_cd, title_nm, disclaimer_txt, slug_txt,
series_1_nm, series_2_nm, series_3_nm, series_4_nm, series_5_nm, series_6_nm, series_7_nm, 
series_8_nm, series_9_nm, create_dt, approve_flg)
select 'TABLE_' + trim(to_char(a.id, '9999')), 'TABLE', name, footnote, slug, 
col2,col3,col4,col5,col6,col7,col8,col9,col10, now(), true 
from biomedgps.embeds_grid a
inner join (
	select id, grid_id, col1,col2,col3,col4,col5,col6,col7,col8,col9,col10 
	from biomedgps.embeds_gridrow where id in 
	(select min(id) from biomedgps.embeds_gridrow group by grid_id)
) b on a.id = b.grid_id;

/** Migrates the data from the embed_gridrow table to the new grid detail tables **/
insert into  custom.biomedgps_grid_detail (grid_detail_id, grid_id, order_no, label_txt, value_1_txt,
value_2_txt, value_3_txt, value_4_txt, value_5_txt, value_6_txt, value_7_txt, value_8_txt, value_9_txt, 
create_dt, grid_detail_type_cd)
select 'TABLE_ROW_' + trim(to_char(b.id, '999999')), 'TABLE_' + trim(to_char(grid_id, '999999')),
 ROW_NUMBER() OVER (partition by b.grid_id ORDER BY a.id),
col1,col2,col3,col4,col5,col6,col7,col8,col9,col10, now(),
case
	when (class1 like '%header%') then 'HEADING'
	when (upper(col1) like '%TOTAL%') then 'TOTAL'
	else 'DATA'
end
from biomedgps.embeds_grid a
inner join (
	select id, grid_id, col1,col2,col3,col4,col5,col6,col7,col8,col9,col10, class1
	from biomedgps.embeds_gridrow where id not in
	(select min(id) from biomedgps.embeds_gridrow group by grid_id)
) b on a.id = b.grid_id;

/** Update the newly imported table data to set the row type to uncharted when the row is
	a data row and it is below a total column.  This will ensure only the data rows will 
	be used for charting */
update custom.biomedgps_grid_detail gd
set grid_detail_type_cd = 'UNCHARTED_DATA'
from (
	select b.grid_id, b.grid_detail_id, order_no
	from custom.biomedgps_grid a
	inner join (
		select *
		from custom.biomedgps_grid_detail where order_no in 
		(select min(order_no) from custom.biomedgps_grid_detail where grid_detail_type_cd = 'TOTAL' group by grid_id)
	) b on a.grid_id = b.grid_id
	where grid_detail_type_cd = 'TOTAL'
	group by b.grid_id, b.grid_detail_id, order_no
) as j where gd.grid_id = j.grid_id
and grid_detail_type_cd = 'DATA' and gd.order_no > j.order_no and gd.grid_id like 'TABLE%';

/* Since the data coming over has empty and not null, need to convert to nulls */
update custom.biomedgps_grid_detail set value_10_txt = null where length(value_10_txt) = 0 or value_10_txt = 'null';
update custom.biomedgps_grid_detail set value_9_txt = null where length(value_9_txt) = 0 or value_9_txt = 'null';
update custom.biomedgps_grid_detail set value_8_txt = null where length(value_8_txt) = 0 or value_8_txt = 'null';
update custom.biomedgps_grid_detail set value_7_txt = null where length(value_7_txt) = 0 or value_7_txt = 'null';
update custom.biomedgps_grid_detail set value_6_txt = null where length(value_6_txt) = 0 or value_6_txt = 'null';
update custom.biomedgps_grid_detail set value_5_txt = null where length(value_5_txt) = 0 or value_5_txt = 'null';
update custom.biomedgps_grid_detail set value_4_txt = null where length(value_4_txt) = 0 or value_4_txt = 'null';
update custom.biomedgps_grid_detail set value_3_txt = null where length(value_3_txt) = 0 or value_3_txt = 'null';
update custom.biomedgps_grid_detail set value_2_txt = null where length(value_2_txt) = 0 or value_2_txt = 'null';
update custom.biomedgps_grid_detail set value_1_txt = null where length(value_1_txt) = 0 or value_1_txt = 'null';

/** Add the modules and actions for the grid display amid 
insert into core.MODULE_TYPE (MODULE_TYPE_ID, ORGANIZATION_ID, MODULE_TYPE_NM, CLASS_NM, INDEXER_CLASS_NM, 
ADMIN_FLG, SERVICE_URL, FACADE_FLG, GLOBAL_ADMIN_FLG, DISPLAY_FLG, CREATE_DT, REPORT_FLG, CACHEABLE_FLG, CLONABLE_FLG, 
APPROVABLE_FLG, WIDGET_CATEGORY_ID) values 
('GRID_DISPLAY_ACTION', 'BMG_SMARTTRAK', 'Biomed Grid Display Action', 'com.biomed.smarttrak.action.GridDisplayAction', 
'', 0, '', 0, 0, 1, getdate(), 0, 0, 0, 0, 'UNALLOCATED');

insert into core.MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, 
CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values 
('38bf8c228210c7690a001a75495bc032', 'GRID_DISPLAY_ACTION', '/none', 'Empty View for AMID', 
getdate(), '', 'BMG_SMARTTRAK', 0, 0);

insert into core.sb_action (action_id, organization_id, module_type_id, action_nm,create_dt) values
('7675rhfhrht','BMG_SMARTTRAK','GRID_DISPLAY_ACTION','Placeholder for Grids', now());

insert into core.ajax_module (ajax_module_id, ajax_nm, site_id, action_id, status_cd, create_dt, module_display_id)
values ('fc9f4ae9822a3eae0a001a75863fb4a8', 'gridChart','BMG_SMARTTRAK_2','7675rhfhrht',1,now(),'38bf8c228210c7690a001a75495bc032'); */


-- Companies

--stock exchange
insert into custom.biomedgps_stock_exchange (exchange_id, name_txt, create_dt)
select id, exchange, current_timestamp from biomedgps.companies_stockexchange;


--company table
insert into custom.biomedgps_company
(company_id, parent_id, company_nm, startup_flg, short_nm_txt, alias_nm, currency_type_id, 
status_no, revenue_no, founded_yr, revenue_yr, completion_score_no, product_no, profile_no, 
people_no,holding_txt, stock_abbr_txt, exchange_id, fiscal_yr_end_mon, archive_reason_txt, create_dt, update_dt)
SELECT id, parent_id, name, case when startup then 1 else 0 end, short_name, aka, currency_id, 
upper(trim(status)), revenue, year_founded, revenue_year, completion_score, products, profiles, people, ownership, ticker, 
exchange_id, fiscal_year_end, archive_reason, create_date, last_update FROM biomedgps.companies_company;

-- flush blank values in the holding_txt field
update custom.biomedgps_company set holding_txt=null where length(holding_txt)=0;

--company currency types
update custom.biomedgps_company set currency_type_id='dollars' where currency_type_id='1';
update custom.biomedgps_company set currency_type_id='EUR' where currency_type_id='2';
update custom.biomedgps_company set currency_type_id='CAD' where currency_type_id='3';
update custom.biomedgps_company set currency_type_id='AUD' where currency_type_id='4';
update custom.biomedgps_company set currency_type_id='GBP' where currency_type_id='5';
update custom.biomedgps_company set currency_type_id='DKK' where currency_type_id='6';
update custom.biomedgps_company set currency_type_id='SEK' where currency_type_id='7';
update custom.biomedgps_company set currency_type_id='JPY' where currency_type_id='8';
update custom.biomedgps_company set currency_type_id='NIS' where currency_type_id='9';
update custom.biomedgps_company set currency_type_id='CHF' where currency_type_id='10';

update custom.biomedgps_company set company_nm='_UNNAMED_' where length(trim(company_nm))=0;

--company_location
update biomedgps.companies_company set state=trim(state);

insert into custom.biomedgps_company_location
(location_id, company_id, location_nm, address_txt, address2_txt, city_nm, state_cd, 
zip_cd, country_cd, phone_txt, alt_phone_txt, primary_locn_flg, create_dt, update_dt)
select id, id, name, address, address1, city, state, zip, country_cd, phone, phone_800, 
0, create_date, last_update FROM biomedgps.companies_company a
inner join core.country b on a.country_id=b.country_nm;

--company alliance type
insert into custom.biomedgps_alliance_type (alliance_type_id, type_nm, section_cd, create_dt)
select id, description, 'COMPANY', current_timestamp from biomedgps.companies_alliancetype;

--company alliance
insert into custom.biomedgps_company_alliance_xr (company_alliance_xr_id, alliance_type_id, 
company_id, rel_company_id, reference_txt, order_no, create_dt)
select id, relationship_id, company_id, alliance_id, reference, sort_order, current_timestamp 
from biomedgps.companies_alliance;

--company investor
insert into custom.biomedgps_company_investor (investor_id, investor_company_id, investee_company_id, create_dt)
select id, from_company_id, to_company_id, current_timestamp from biomedgps.companies_company_investors;

--company attributes

--insert the LINK and GRAPH company attributes, which feed the left column
insert into custom.biomedgps_company_attribute 
(attribute_id, parent_id, attribute_nm, active_flg, type_nm, display_order_no, create_dt) values
('LVL1_LINK',null,'Link',1,'LINK',1,current_timestamp),
('LVL1_GRAPH',null,'Graphic',1,'GRAPH',2,current_timestamp);

/*
--market level (former 1st level) company attributes
insert into custom.biomedgps_company_attribute 
(attribute_id, parent_id, attribute_nm, active_flg, type_nm, display_order_no, create_dt)
select distinct 'LVL1_'+cast(b.id as varchar),null, trim(b.name),
case when b.visible then 1 else 0 end,'HTML',b.default_order, current_timestamp 
from biomedgps.articles_market b;

--segment level (former 2nd level) company attributes
insert into custom.biomedgps_company_attribute 
(attribute_id, parent_id, attribute_nm, active_flg, type_nm, display_order_no, create_dt)
select distinct 'LVL2_'+cast(a.id as varchar),'LVL1_'+cast(b.id as varchar), trim(a.name),
case when b.visible then 1 else 0 end,'HTML',a.default_order, current_timestamp 
from biomedgps.articles_market b
inner join biomedgps.articles_segment a on a.market_id=b.id;

--sub-segment level (former 3nd level) company attributes
insert into custom.biomedgps_company_attribute 
(attribute_id, parent_id, attribute_nm, active_flg, type_nm, display_order_no, create_dt)
select distinct 'LVL3_'+cast(c.id as varchar),'LVL2_'+cast(b.id as varchar),trim(c.name),
case when c.visible then 1 else 0 end,'HTML',c.default_order, current_timestamp
from biomedgps.articles_market a
inner join biomedgps.articles_segment b on a.id=b.market_id 
inner join biomedgps.articles_subsegment c on c.segment_id=b.id;
*/

-- dev query - R&D - product attribute hierarchy - levels 4,3,2,1
select e.summary, e.status, min(e.sort_order), d.subsegment_id, c.name, c.id, b.name, b.id, a.name, a.id
from biomedgps.articles_market a
inner join biomedgps.articles_segment b on a.id=b.market_id 
inner join biomedgps.articles_subsegment c on c.segment_id=b.id
inner join biomedgps.articles_article_subsegments d on c.id=d.subsegment_id
inner join biomedgps.articles_article e on d.article_id=e.id
where e.content_type_id=8 and e.object_id=28649 
group by e.summary, d.subsegment_id, c.name, c.id, b.name, b.id, a.name, a.id, e.status
order by a.name, b.name, c.name, e.summary, e.status;

-- company attributes level 1 - from their market level
insert into custom.biomedgps_company_attribute 
(attribute_id, parent_id, attribute_nm, active_flg, type_nm, display_order_no, create_dt)
select distinct 'LVL1_'+cast(a.id as varchar),null, trim(a.name),
case when a.visible then 1 else 0 end,'HTML',a.default_order, current_timestamp 
from biomedgps.articles_market a
inner join biomedgps.articles_segment b on a.id=b.market_id 
inner join biomedgps.articles_subsegment c on c.segment_id=b.id
inner join biomedgps.articles_article_subsegments d on c.id=d.subsegment_id
inner join biomedgps.articles_article e on d.article_id=e.id
where e.content_type_id=8;

-- company attributes level 2 - from their segment level
insert into custom.biomedgps_company_attribute 
(attribute_id, parent_id, attribute_nm, active_flg, type_nm, display_order_no, create_dt)
select distinct 'LVL2_'+cast(b.id as varchar),'LVL1_'+cast(a.id as varchar), trim(b.name),
case when b.visible then 1 else 0 end,'HTML',b.default_order, current_timestamp 
from biomedgps.articles_market a
inner join biomedgps.articles_segment b on a.id=b.market_id 
inner join biomedgps.articles_subsegment c on c.segment_id=b.id
inner join biomedgps.articles_article_subsegments d on c.id=d.subsegment_id
inner join biomedgps.articles_article e on d.article_id=e.id
where e.content_type_id=8;

-- company attributes level 3 (sub-headings) - from their sub-segment level
insert into custom.biomedgps_company_attribute 
(attribute_id, parent_id, attribute_nm, active_flg, type_nm, display_order_no, create_dt)
select distinct 'LVL3_'+cast(c.id as varchar),'LVL2_'+cast(b.id as varchar), trim(c.name),
case when c.visible then 1 else 0 end,'HTML',c.default_order, current_timestamp 
from biomedgps.articles_market a
inner join biomedgps.articles_segment b on a.id=b.market_id 
inner join biomedgps.articles_subsegment c on c.segment_id=b.id
inner join biomedgps.articles_article_subsegments d on c.id=d.subsegment_id
inner join biomedgps.articles_article e on d.article_id=e.id
where e.content_type_id=8;

-- data cleanup for company attribute names
update biomedgps.articles_article set summary='Revenues & Financial Outlook' 
	where summary='Revenues/ Financial Outlook' and content_type_id=8;
update biomedgps.articles_article set summary='New Article' 
	where summary='New_Article' and content_type_id=8;
update biomedgps.articles_article set summary='Company Profile - Stroke' 
	where summary='Company Profile- Stroke' and content_type_id=8;
update biomedgps.articles_article set summary='Intellectual Property' 
	where summary='Intellectual  Property' and content_type_id=8;
update biomedgps.articles_article set summary='EDITORS NOTES: DO NOT PUBLISH' 
	where summary in ('EDITOR''S NOTES DO NOT PUBLISH','DO NOT PUBLISH - EDITOR''S NOTES',
		'EDITOR''S NOTES: DO NOT USE') and content_type_id=8;
update biomedgps.articles_article set summary='Recalls' 
	where summary='Recall' and content_type_id=8;
update biomedgps.articles_article set summary='Company Overview' 
	where summary='Company Overview-Kevin' and content_type_id=8;
update biomedgps.articles_article set summary='Intellectual Property' 
	where summary='Intellectual property' and content_type_id=8;

-- company attributes level 4 (the names) - from their article summary
insert into custom.biomedgps_company_attribute 
(attribute_id, parent_id, attribute_nm, active_flg, type_nm, display_order_no, create_dt)
select distinct left(cast(c.id as varchar)+regexp_replace(e.summary, '[^a-zA-Z0-9]', '', 'g'),32),
'LVL3_'+cast(c.id as varchar), trim(e.summary), 1,'HTML',
case when min(e.sort_order) > 10000 then min(e.sort_order)/10000 else min(e.sort_order) end, current_timestamp 
from biomedgps.articles_market a
inner join biomedgps.articles_segment b on a.id=b.market_id 
inner join biomedgps.articles_subsegment c on c.segment_id=b.id
inner join biomedgps.articles_article_subsegments d on c.id=d.subsegment_id
inner join biomedgps.articles_article e on d.article_id=e.id
where e.content_type_id=8 
group by cast(c.id as varchar)+regexp_replace(e.summary, '[^a-zA-Z0-9]', '', 'g'),'LVL3_'+cast(c.id as varchar), trim(e.summary)
order by  left(cast(c.id as varchar)+regexp_replace(e.summary, '[^a-zA-Z0-9]', '', 'g'),32);

--normalize status - caps issues
update biomedgps.articles_article set status=upper(trim(status));

-- company attribute _xr
insert into custom.biomedgps_company_attribute_xr (company_attribute_id, attribute_id, 
company_id, value_txt, title_txt, order_no, status_no, create_dt, update_dt)
select replace(newid(),'-',''),left(cast(c.id as varchar)+regexp_replace(e.summary, '[^a-zA-Z0-9]', '', 'g'),32), 
e.object_id, e.content, e.summary,case when e.sort_order > 10000 then e.sort_order/10000 else e.sort_order end, 
e.status, e.create_date, e.last_update
from biomedgps.articles_market a
inner join biomedgps.articles_segment b on a.id=b.market_id 
inner join biomedgps.articles_subsegment c on c.segment_id=b.id
inner join biomedgps.articles_article_subsegments d on c.id=d.subsegment_id
inner join biomedgps.articles_article e on d.article_id=e.id
where e.content_type_id=8 and e.status='P'; --only pushed attributes, there are historical records in here


-- company section data normalization:
update biomedgps.articles_segment set name='Wound Mgmt - Std of Care' where name='Wound Management - Std of Care';

-- company_section_xr -- note the 'null' condition here excludes one record, the "Market (Training)" company has an orphan section.
insert into custom.biomedgps_company_section (company_section_xr_id, section_id, company_id, create_dt)
select replace(newid(),'-',''),coalesce(s.section_id,s2.section_id), f.company_id, current_timestamp
from biomedgps.articles_market a
inner join biomedgps.articles_segment b on a.id=b.market_id 
inner join biomedgps.articles_subsegment c on c.segment_id=b.id
inner join biomedgps.vcompany_subsegment f on f.subsegment_id=c.id
left join custom.biomedgps_section s on c.name=s.section_nm and s.section_id like '%MASTER%'
left join custom.biomedgps_section s2 on b.name=s2.section_nm and s2.section_id like '%MASTER%'
where  c.visible='true' and coalesce(s.section_id,s2.section_id,null) is not null;


--from dev
--the company's attributes
  select distinct aa.summary, ass.name, am.name, aseg.name, aa.sort_order
  from biomedgps.articles_article aa
  join biomedgps.articles_article_subsegments aas on aas.article_id = aa.id
  join biomedgps.articles_subsegment ass on ass.id = aas.subsegment_id
  join biomedgps.articles_market am on ass.market_id = am.id
  join biomedgps.articles_segment aseg on ass.segment_id = aseg.id
  where aa.object_id = 5448 and aa.content_type_id = 8 and aa.status in ('p','e') order by sort_order;



--the company's products - display on the /companies/xyz/ page 
select distinct p.*,seg.name as segname from biomedgps.products_product p
		join biomedgps.products_strategicpartner sp on sp.product_id=p.id and sp.partner_id=5448
		join biomedgps.vproduct_subsegment ps on ps.product_id=p.id and ps.subsegment_id != 5448
		join biomedgps.articles_subsegment ss on ps.subsegment_id=ss.id
		join biomedgps.articles_segment seg on seg.id=ss.segment_id
		where p.id != 5448 and p.status in ('p','e')
		order by seg.name, p.name;
  


-- Gap Analysis

--level 1
--the GA root node for now...keeps a separate hierarchy until we can verify they're safe to consolidate
insert into custom.biomedgps_section (section_id, parent_id, section_nm, solr_token_txt, order_no, create_dt)
values ('GAP_ANALYSIS_ROOT',null,'Gap Analysis','GapAnalysisRoot',null,current_timestamp);

--level 2's
insert into custom.biomedgps_section (section_id, parent_id, section_nm, solr_token_txt, order_no, create_dt) values 
('GA_ORTHOGPS_ROOT','GAP_ANALYSIS_ROOT','OrthoGPS','OrthoGPS',5,current_timestamp),
('GA_WOUNDGPS_ROOT','GAP_ANALYSIS_ROOT','WoundGPS','WoundGPS',10,current_timestamp),
('GA_REGENGPS_ROOT','GAP_ANALYSIS_ROOT','RegenGPS','RegenGPS',15,current_timestamp),
('GA_NEUROGPS_ROOT','GAP_ANALYSIS_ROOT','NeuroGPS','NeuroGPS',20,current_timestamp);

-- level 3's
insert into custom.biomedgps_section (section_id, parent_id, section_nm, solr_token_txt, order_no, create_dt)
select 'GAP_ANALYSIS_L3_' + cast(id as varchar),
case branch when 'OrthoGPS' then 'GA_ORTHOGPS_ROOT' when 'WoundGPS' then 'GA_WOUNDGPS_ROOT' else null end, name, 
name, sort_order,current_timestamp from biomedgps.gap_analysis_gamarket;

-- level 4's
insert into custom.biomedgps_section (section_id, parent_id, section_nm, solr_token_txt, order_no, create_dt)
select 'GAP_ANALYSIS_L4_' + cast(a.id as varchar), 'GAP_ANALYSIS_L3_' + cast(a.market_id as varchar), a.name, 
a.name, a.sort_order,current_timestamp 
from biomedgps.gap_analysis_gasection a 
inner join biomedgps.gap_analysis_gamarket b on a.market_id=b.id;
where a.id != 2; --2 is a duplicate (and empty) 'Clavicle' entry


--level 5's
/*
--helper queries to generate the below manual inserts
select * from biomedgps.gap_analysis_gasection where length(subheaders) > 0;
select * from biomedgps.gap_analysis_gacolumn where section_id=41 order by sort_order;
delete from custom.biomedgps_section where section_id like 'GAP_ANALYSIS_L5_7_%';
*/
--subheaders manually transposed from json in the subheader field
insert into custom.biomedgps_section (section_id, parent_id, section_nm, solr_token_txt, order_no, create_dt) values 
--fusion
('GAP_ANALYSIS_L5_7_CF','GAP_ANALYSIS_L4_7','Cervical Fixation','Cervical Fixation',5,current_timestamp),
('GAP_ANALYSIS_L5_7_TF','GAP_ANALYSIS_L4_7','T/L Fixation','T/L Fixation',10,current_timestamp),
('GAP_ANALYSIS_L5_7_MIS','GAP_ANALYSIS_L4_7','MIS Fusion','MIS Fusion',15,current_timestamp),
('GAP_ANALYSIS_L5_7_INT','GAP_ANALYSIS_L4_7','Interbody','Interbody',20,current_timestamp),
--soft tissue
('GAP_ANALYSIS_L5_15_TR','GAP_ANALYSIS_L4_15','Tendon Replacement','Tendon Replacement',5,current_timestamp),
('GAP_ANALYSIS_L5_15_AUG','GAP_ANALYSIS_L4_15','Augmentation','Augmentation',10,current_timestamp),
--arthroplasty
('GAP_ANALYSIS_L5_22_ANK','GAP_ANALYSIS_L4_22','Ankle','Ankle',5,current_timestamp),
--total knee
('GAP_ANALYSIS_L5_34_PRI','GAP_ANALYSIS_L4_34','Primary','Primary',5,current_timestamp),
('GAP_ANALYSIS_L5_34_REV','GAP_ANALYSIS_L4_34','Revision','Revision',5,current_timestamp),
--external devices
('GAP_ANALYSIS_L5_40_NPWT','GAP_ANALYSIS_L4_40','NPWT','NPWT',5,current_timestamp),
--wound biologics
('GAP_ANALYSIS_L5_41_SKIN','GAP_ANALYSIS_L4_41','Skin/ Dermal','Skin/ Dermal',5,current_timestamp);

--update the gacolumns with the parent_id values we need to tie them to
--fusion
update biomedgps.gap_analysis_gacolumn set extra_sql='GAP_ANALYSIS_L5_7_CF' where id in (18,19);
update biomedgps.gap_analysis_gacolumn set extra_sql='GAP_ANALYSIS_L5_7_TF' where id in (20,21);
update biomedgps.gap_analysis_gacolumn set extra_sql='GAP_ANALYSIS_L5_7_MIS' where id in (22,26);
update biomedgps.gap_analysis_gacolumn set extra_sql='GAP_ANALYSIS_L5_7_INT' where id in (23,149);
update biomedgps.gap_analysis_gacolumn set extra_sql='GAP_ANALYSIS_L4_7' where id in (24,25); --no step-parent
--soft tissue
update biomedgps.gap_analysis_gacolumn set extra_sql='GAP_ANALYSIS_L5_15_TR' where id in (46,47);
update biomedgps.gap_analysis_gacolumn set extra_sql='GAP_ANALYSIS_L5_15_AUG' where id in (112,160);
--arthroplasty
update biomedgps.gap_analysis_gacolumn set extra_sql='GAP_ANALYSIS_L5_22_ANK' where id in (64,65,66,72);
update biomedgps.gap_analysis_gacolumn set extra_sql='GAP_ANALYSIS_L4_22' where id=67; --no step-parent
--total knee
update biomedgps.gap_analysis_gacolumn set extra_sql='GAP_ANALYSIS_L5_34_PRI' where id in (115,116);
update biomedgps.gap_analysis_gacolumn set extra_sql='GAP_ANALYSIS_L5_34_REV' where id in (117,118);
--external devices
update biomedgps.gap_analysis_gacolumn set extra_sql='GAP_ANALYSIS_L4_40' where id in (138,139,140); --no step-parent
update biomedgps.gap_analysis_gacolumn set extra_sql='GAP_ANALYSIS_L5_40_NPWT' where id in (135,136);
--wound biologics
update biomedgps.gap_analysis_gacolumn set extra_sql='GAP_ANALYSIS_L4_41' where id in (141,145); --no step-parent
update biomedgps.gap_analysis_gacolumn set extra_sql='GAP_ANALYSIS_L5_41_SKIN' where id in (142,143,144);

--insert the GA columns
insert into custom.biomedgps_ga_column (ga_column_id, section_id, column_nm, button_txt, 
special_rules_txt, order_no, create_dt) select 'GA_COLUMN_' + cast(id as varchar), 
case when length(trim(extra_sql)) = 0 or extra_sql like '%product_id%' then 'GAP_ANALYSIS_L4_'+cast(section_id as varchar) else extra_sql end, 
name, button, case when length(trim(extra_sql)) = 0 or extra_sql like 'GAP_ANALYSIS%' then null else extra_sql end, 
sort_order,current_timestamp from biomedgps.gap_analysis_gacolumn;

-- GA column attribute XR - classifications - references to product attributues
insert into custom.biomedgps_ga_column_attribute_xr (column_attribute_xr_id, ga_column_id, attribute_id, create_dt)
select 'CLASSIFICATION_'+cast(id as varchar), 'GA_COLUMN_'+cast(gacolumn_id as varchar), 'CLASSIFICATION_'+cast(classification_id as varchar), 
current_timestamp from biomedgps.gap_analysis_gacolumn_classifications;

-- GA column attribute XR - markets - references to product attributues
insert into custom.biomedgps_ga_column_attribute_xr (column_attribute_xr_id, ga_column_id, attribute_id, create_dt)
select 'MARKET_'+cast(id as varchar), 'GA_COLUMN_'+cast(gacolumn_id as varchar), 'MARKET_'+cast(targetmarket_id as varchar), 
current_timestamp from biomedgps.gap_analysis_gacolumn_targetmarkets;

-- GA column attribute XR - indications - references to product attributues
insert into custom.biomedgps_ga_column_attribute_xr (column_attribute_xr_id, ga_column_id, attribute_id, create_dt)
select 'INDICATION_'+cast(id as varchar), 'GA_COLUMN_'+cast(gacolumn_id as varchar), 'INDICATION_'+cast(indication_id as varchar), 
current_timestamp from biomedgps.gap_analysis_gacolumn_indications;

-- GA column attribute XR - methods (approaches) - references to product attributues
insert into custom.biomedgps_ga_column_attribute_xr (column_attribute_xr_id, ga_column_id, attribute_id, create_dt)
select 'METHOD_'+cast(id as varchar), 'GA_COLUMN_'+cast(gacolumn_id as varchar), 'METHOD_'+cast(method_id as varchar), 
current_timestamp from biomedgps.gap_analysis_gacolumn_methods;

-- GA column attribute XR - technologies - references to product attributues
insert into custom.biomedgps_ga_column_attribute_xr (column_attribute_xr_id, ga_column_id, attribute_id, create_dt)
select 'TECHNOLOGY_'+cast(id as varchar), 'GA_COLUMN_'+cast(gacolumn_id as varchar), 'TECHNOLOGY_'+cast(technology_id as varchar), 
current_timestamp from biomedgps.gap_analysis_gacolumn_technologies;



-- financial dashboard


--master FD hierarchy - will also become site's master hierarchy
insert into custom.biomedgps_section (section_id, parent_id, section_nm, solr_token_txt, order_no, create_dt)
SELECT replace('MASTER_'+cast(id as varchar),'MASTER_2000','MASTER_ROOT'), 
	(SELECT replace('MASTER_'+cast(id as varchar),'MASTER_2000','MASTER_ROOT') 
           FROM biomedgps.nodes_node t2 
           WHERE t2.lt < t1.lt AND t2.rt > t1.rt and t1.tree_id = t2.tree_id
           ORDER BY t2.rt-t1.rt asc
           limit 1), name, left(name, 30),lt, current_timestamp
FROM biomedgps.nodes_node t1
where t1.tree_id=2000
ORDER BY rt-lt desc;

--delete orphan revenue records - 5 company-side and 109 hierarchy-side
delete from biomedgps.financials_revenue 
where id in (
	select a.id from biomedgps.financials_revenue a 
	left join biomedgps.companies_company b on a.company_id=b.id where b.id is null
  union
	select a.id from biomedgps.financials_revenue a
	left join custom.biomedgps_section b on 
	'MASTER_'+cast(a.source_node_id as varchar) = b.section_id where b.section_id is null);


--2012 revenues
insert into custom.biomedgps_fd_revenue (revenue_id, company_id, section_id, region_cd, 
year_no, q1_no, q2_no, q3_no, q4_no, create_dt)
select '2012_'+cast(id as varchar), company_id, 'MASTER_'+cast(a.source_node_id as varchar), region,
2012,q2012_1,q2012_2,q2012_3,q2012_4,current_timestamp from biomedgps.financials_revenue a;

--2013 revenues
insert into custom.biomedgps_fd_revenue (revenue_id, company_id, section_id, region_cd, 
year_no, q1_no, q2_no, q3_no, q4_no, create_dt)
select '2013_'+cast(id as varchar), company_id, 'MASTER_'+cast(a.source_node_id as varchar), region,
2013,q2013_1,q2013_2,q2013_3,q2013_4,current_timestamp from biomedgps.financials_revenue a;

--2014 revenues
insert into custom.biomedgps_fd_revenue (revenue_id, company_id, section_id, region_cd, 
year_no, q1_no, q2_no, q3_no, q4_no, create_dt)
select '2014_'+cast(id as varchar), company_id, 'MASTER_'+cast(a.source_node_id as varchar), region, 
2014,q2014_1,q2014_2,q2014_3,q2014_4,current_timestamp from biomedgps.financials_revenue a;

--2015 revenues
insert into custom.biomedgps_fd_revenue (revenue_id, company_id, section_id, region_cd, 
year_no, q1_no, q2_no, q3_no, q4_no, create_dt)
select '2015_'+cast(id as varchar), company_id, 'MASTER_'+cast(a.source_node_id as varchar), region, 
2015,q2015_1,q2015_2,q2015_3,q2015_4,current_timestamp from biomedgps.financials_revenue a;

--2016 revenues
insert into custom.biomedgps_fd_revenue (revenue_id, company_id, section_id, region_cd, 
year_no, q1_no, q2_no, q3_no, q4_no, create_dt)
select '2016_'+cast(id as varchar), company_id, 'MASTER_'+cast(a.source_node_id as varchar), region, 
2016,q2016_1,q2016_2,q2016_3,q2016_4,current_timestamp from biomedgps.financials_revenue a;

--2017 revenues
insert into custom.biomedgps_fd_revenue (revenue_id, company_id, section_id, region_cd, 
year_no, q1_no, q2_no, q3_no, q4_no, create_dt)
select '2017_'+cast(id as varchar), company_id, 'MASTER_'+cast(a.source_node_id as varchar), region, 
2017,q2017_1,q2017_2,q2017_3,q2017_4,current_timestamp from biomedgps.financials_revenue a;

--delete footnotes tied to removed companies (0) and hierarchy sections (4) - they can never appear on the website anyways
delete from biomedgps.financials_revenuenote 
where id in (
	select a.id from biomedgps.financials_revenuenote a 
	left join biomedgps.companies_company b on a.object_id=b.id where b.id is null and a.content_type_id=8
  union
	select a.id from biomedgps.financials_revenuenote a
	left join custom.biomedgps_section b on 
	'MASTER_'+cast(a.source_node_id as varchar) = b.section_id where b.section_id is null);

--insert FD footnotes
insert into custom.biomedgps_fd_revenue_footnote (footnote_id, region_cd, footnote_txt, 
expiration_dt, create_dt, section_id, company_id)
select id,region,content,expiration,CURRENT_TIMESTAMP,'MASTER_'+cast(source_node_id as varchar),
object_id from biomedgps.financials_revenuenote where length(trim(content)) > 0 
and content_type_id=8 and content not like '%test%'; --ignoring content_type_id=143, node bindings.  Seemingly deprecated.

--insert FD scenarios
insert into custom.biomedgps_fd_scenario (scenario_id, user_id, team_id, scenario_nm, status_flg, refresh_dt, create_dt, update_dt)
select id, owner_id, team_id, name, upper(status), last_refresh, current_timestamp, last_changed  from biomedgps.financials_revenueoverlay
where cast(team_id as varchar) in (select team_id from custom.biomedgps_team);

--2012 FD scenario overlay data
insert into custom.biomedgps_fd_scenario_overlay (overlay_id, company_id, scenario_id, 
revenue_id, year_no, q1_no, q2_no, q3_no, q4_no, create_dt, update_dt)
select '2012_'+cast(id as varchar), a.company_id, overlay_id, b.revenue_id, 
2012, q2012_1, q2012_2,q2012_3,q2012_4,current_timestamp, last_changed 
from biomedgps.financials_overlaydata a
inner join custom.biomedgps_fd_revenue b on a.region=b.region_cd and cast(a.company_id as varchar)=b.company_id 
and b.year_no=2012 and 'MASTER_'+cast(a.source_node_id as varchar)=b.section_id
where overlay_id is not null
and (q2012_1 is not null or q2012_2 is not null or q2012_3 is not null or q2012_4 is not null)
and cast(overlay_id as varchar) in (select scenario_id from custom.biomedgps_fd_scenario);

--2013 FD scenario overlay data
insert into custom.biomedgps_fd_scenario_overlay (overlay_id, company_id, scenario_id, 
revenue_id, year_no, q1_no, q2_no, q3_no, q4_no, create_dt, update_dt)
select '2013_'+cast(id as varchar), a.company_id, overlay_id, b.revenue_id, 
2013, q2013_1, q2013_2,q2013_3,q2013_4,current_timestamp, last_changed 
from biomedgps.financials_overlaydata a
inner join custom.biomedgps_fd_revenue b on a.region=b.region_cd and cast(a.company_id as varchar)=b.company_id 
and b.year_no=2013 and 'MASTER_'+cast(a.source_node_id as varchar)=b.section_id
where overlay_id is not null
and (q2013_1 is not null or q2013_2 is not null or q2013_3 is not null or q2013_4 is not null)
and cast(overlay_id as varchar) in (select scenario_id from custom.biomedgps_fd_scenario);

--2014 FD scenario overlay data
insert into custom.biomedgps_fd_scenario_overlay (overlay_id, company_id, scenario_id, 
revenue_id, year_no, q1_no, q2_no, q3_no, q4_no, create_dt, update_dt)
select '2014_'+cast(id as varchar), a.company_id, overlay_id, b.revenue_id, 
2014, q2014_1, q2014_2,q2014_3,q2014_4,current_timestamp, last_changed 
from biomedgps.financials_overlaydata a
inner join custom.biomedgps_fd_revenue b on a.region=b.region_cd and cast(a.company_id as varchar)=b.company_id 
and b.year_no=2014 and 'MASTER_'+cast(a.source_node_id as varchar)=b.section_id
where overlay_id is not null
and (q2014_1 is not null or q2014_2 is not null or q2014_3 is not null or q2014_4 is not null)
and cast(overlay_id as varchar) in (select scenario_id from custom.biomedgps_fd_scenario);

--2015 FD scenario overlay data
insert into custom.biomedgps_fd_scenario_overlay (overlay_id, company_id, scenario_id, 
revenue_id, year_no, q1_no, q2_no, q3_no, q4_no, create_dt, update_dt)
select '2015_'+cast(id as varchar), a.company_id, overlay_id, b.revenue_id, 
2015, q2015_1, q2015_2,q2015_3,q2015_4,current_timestamp, last_changed 
from biomedgps.financials_overlaydata a
inner join custom.biomedgps_fd_revenue b on a.region=b.region_cd and cast(a.company_id as varchar)=b.company_id 
and b.year_no=2015 and 'MASTER_'+cast(a.source_node_id as varchar)=b.section_id
where overlay_id is not null
and (q2015_1 is not null or q2015_2 is not null or q2015_3 is not null or q2015_4 is not null)
and cast(overlay_id as varchar) in (select scenario_id from custom.biomedgps_fd_scenario);

--2016 FD scenario overlay data
insert into custom.biomedgps_fd_scenario_overlay (overlay_id, company_id, scenario_id, 
revenue_id, year_no, q1_no, q2_no, q3_no, q4_no, create_dt, update_dt)
select '2016_'+cast(id as varchar), a.company_id, overlay_id, b.revenue_id, 
2016, q2016_1, q2016_2,q2016_3,q2016_4,current_timestamp, last_changed 
from biomedgps.financials_overlaydata a
inner join custom.biomedgps_fd_revenue b on a.region=b.region_cd and cast(a.company_id as varchar)=b.company_id 
and b.year_no=2016 and 'MASTER_'+cast(a.source_node_id as varchar)=b.section_id
where overlay_id is not null
and (q2016_1 is not null or q2016_2 is not null or q2016_3 is not null or q2016_4 is not null)
and cast(overlay_id as varchar) in (select scenario_id from custom.biomedgps_fd_scenario);

--2017 FD scenario overlay data
insert into custom.biomedgps_fd_scenario_overlay (overlay_id, company_id, scenario_id, 
revenue_id, year_no, q1_no, q2_no, q3_no, q4_no, create_dt, update_dt)
select '2017_'+cast(id as varchar), a.company_id, overlay_id, b.revenue_id, 
2017, q2017_1, q2017_2,q2017_3,q2017_4,current_timestamp, last_changed 
from biomedgps.financials_overlaydata a
inner join custom.biomedgps_fd_revenue b on a.region=b.region_cd and cast(a.company_id as varchar)=b.company_id 
and b.year_no=2017 and 'MASTER_'+cast(a.source_node_id as varchar)=b.section_id
where overlay_id is not null
and (q2017_1 is not null or q2017_2 is not null or q2017_3 is not null or q2017_4 is not null)
and cast(overlay_id as varchar) in (select scenario_id from custom.biomedgps_fd_scenario);


--delete rows for Mechanical Thrombectomy - per Mike - data was all zeros - 18 records as of 3/10/17
delete from custom.biomedgps_fd_revenue where section_id = 'MASTER_35023';


-- links 

-- link types
insert into custom.biomedgps_link_type (link_type_id, name_txt, order_no, create_dt)
select id,linktype, default_order,current_timestamp from biomedgps.links_linktype;

--company links
insert into custom.biomedgps_link (link_id, link_type_id, company_id, name_txt, url_txt, 
archive_flg, check_dt, check_status_no, create_dt) select id, linktype_id, object_id, 
description, url, case when is_archived = 1 then 1 else null end, last_check,  case when broken='true' 
then 404 else 200 end, current_timestamp from biomedgps.links_link where content_type_id=8;




-- accounts


--select * from core.state where country_cd='GB' order by state_nm
--select * from biomedgps.profiles_account where len(state)>5;

--update bad country values
update biomedgps.profiles_account set country='US' where country in ('United States','United States of America','USA');
update biomedgps.profiles_account set country='IS' where country='Iceland';
update biomedgps.profiles_account set country='GB' where country='United Kingdom';
update biomedgps.profiles_account set country='DK' where country='Denmark';
update biomedgps.profiles_account set country='IT' where country='Italy';
update biomedgps.profiles_account set country='SE' where country='Sweden';
update biomedgps.profiles_account set country='DE' where country='Germany ' or country='Germany';
update biomedgps.profiles_account set country='FR' where country='France';

-- update bad state values
UPDATE biomedgps.profiles_account SET state = b.state_cd 
FROM core.state b WHERE b.country_cd=biomedgps.profiles_account.country and b.state_nm=biomedgps.profiles_account.state;
UPDATE biomedgps.profiles_account SET state='MA' where state in ('Massachusettes','Massachsettes');
UPDATE biomedgps.profiles_account SET state='CA' where state='California';
UPDATE biomedgps.profiles_account SET state='LV' where state='Berkshire' and country='GB';

-- insert accounts
insert into custom.biomedgps_account (account_id, company_id, account_nm, start_dt, expiration_dt, 
owner_profile_id, address_txt, address2_txt, city_nm, state_cd, zip_cd, country_cd, status_no, create_dt, update_dt)
select id, company_id, name, coalesce(start_date, '2000-01-01 00:00:00'), expiration, 
account_manager_id, address1, address2, city, state, zip_code, country,upper(status),
CURRENT_TIMESTAMP,modification_timestamp from biomedgps.profiles_account;


--load user records -- Java process
get details from Dave.


-- replace owner_profile_id with a valid PROFILE_ID
UPDATE custom.biomedgps_account SET owner_profile_id = b.wc_profile_id 
from biomedgps.profiles_user b
WHERE b.id=cast(custom.biomedgps_account.owner_profile_id as int);

--IMPORTANT, run these in order! - set type & status by separating their single status column 
update custom.biomedgps_account set type_id=1,status_no='A' where upper(status_no)='A'; --active
update custom.biomedgps_account set type_id=2,status_no='A' where upper(status_no)='S'; --staff
update custom.biomedgps_account set type_id=3,status_no='A' where upper(status_no)='T'; --trial
update custom.biomedgps_account set type_id=4,status_no='A' where upper(status_no)='U'; --updates

/** Dave's script runs the user table population 100%

-- insert the users
insert into custom.biomedgps_user (user_id, profile_id, account_id, status_cd, expiration_dt, create_dt)
select id,wc_profile_id, account_id, upper(status), expiration, date_joined 
from biomedgps.profiles_user where wc_profile_id is not null;

-- update user fields missed by the Java import - run this instead of the above insert
update custom.biomedgps_user
set account_id=b.account_id, expiration_dt=b.expiration
from biomedgps.profiles_user b where custom.biomedgps_user.profile_id=b.wc_profile_id;

*/

-- promote status=Staff to role=Staff
update profile_role a set role_id='3eef678eb39e87277f000101dfd4f140'
from custom.biomedgps_user b 
where a.profile_id=b.profile_id and a.site_id='BMG_SMARTTRAK_1' and b.status_cd='S';

-- change status=Staff to status=Active
update custom.biomedgps_user set status_cd='A' where status_cd='S';

-- insert the teams
insert into custom.biomedgps_team (team_id, account_id, team_nm, default_flg, private_flg, create_dt)
select id,account_id, name, 0, is_private, current_timestamp from biomedgps.profiles_team where account_id is not null;

-- set default teams within the accounts
update custom.biomedgps_team set default_flg=1 from biomedgps.profiles_account b
where cast(b.default_team_id as varchar)=custom.biomedgps_team.team_id and custom.biomedgps_team.account_id=cast(b.id as varchar);

--purge orphan team member records
delete from biomedgps.profiles_team_users where user_id not in (select id from biomedgps.profiles_user);

-- insert team members
insert into custom.biomedgps_user_team_xr (user_team_xr_id, team_id, user_id, create_dt)
select a.id,team_id, user_id, CURRENT_TIMESTAMP from biomedgps.profiles_team_users a 
	inner join biomedgps.profiles_team b on a.team_id=b.id and b.account_id is not null;



-- notes


--company notes - for teams we imported
insert into custom.biomedgps_note (note_id, user_id, team_id, note_nm, note_txt, file_path_txt, expiration_dt, create_dt, update_dt, company_id)
select id, creator_id, team_id, title, content, attachment, expiration, create_date, last_update, object_id from biomedgps.notes_note
where content_type_id=8 and cast(team_id as varchar) in (select team_id from custom.biomedgps_team);

-- company attribute notes - for teams we imported - tie the companyId & attributeId
insert into custom.biomedgps_note (note_id, user_id, team_id, note_nm, note_txt, file_path_txt, 
expiration_dt, create_dt, update_dt, company_id, attribute_id)
select n.id, n.creator_id, n.team_id, n.title, n.content, n.attachment, n.expiration, n.create_date, 
n.last_update,e.object_id, left(cast(c.id as varchar)+regexp_replace(e.summary, '[^a-zA-Z0-9]', '', 'g'),32) 
from biomedgps.articles_market a
inner join biomedgps.articles_segment b on a.id=b.market_id 
inner join biomedgps.articles_subsegment c on c.segment_id=b.id
inner join biomedgps.articles_article_subsegments d on c.id=d.subsegment_id
inner join biomedgps.articles_article e on d.article_id=e.id
inner join biomedgps.notes_note n on e.id=n.object_id and n.content_type_id=22
where e.content_type_id=8 and cast(team_id as varchar) in (select team_id from custom.biomedgps_team);



-- products


-- products tied to companies that exist
insert into custom.biomedgps_product (product_id, parent_id, company_id, product_nm, short_nm, 
alias_nm, order_no, author_profile_id, status_no, create_dt, update_dt)
select id, null, company_id, name, short_name,aka,case when sort_order >1000 then 1000 else sort_order end, 
author_id, upper(status), create_date, last_update 
from biomedgps.products_product where company_id in (select id from biomedgps.companies_company);

-- replace author_profile_id (user_id) with actual profile_id values
update custom.biomedgps_product set author_profile_id=b.profile_id
from custom.biomedgps_user b where custom.biomedgps_product.author_profile_id=b.user_id;

--flush author_profile_ids inditing users that no longer exist
update custom.biomedgps_product set author_profile_id=null 
where author_profile_id not in (select profile_id from custom.biomedgps_user) 
	and author_profile_id not in (select user_id from custom.biomedgps_user);

--populate URL aliases
update custom.biomedgps_product set url_alias_txt=lower(regexp_replace(replace(trim(short_nm),' ','-'), '[^a-zA-Z0-9_-]', '', 'g'));

-- fix duplicate URL aliases by prefixing the dups with their product_id
update custom.biomedgps_product set url_alias_txt=cast(product_id as varchar)+'-'+url_alias_txt
where url_alias_txt in (
select lower(regexp_replace(replace(product_nm,' ','-'), '[^a-zA-Z0-9_-]', '', 'g'))
from custom.biomedgps_product 
group by lower(regexp_replace(replace(product_nm,' ','-'), '[^a-zA-Z0-9_-]', '', 'g'))
having count(*) > 1);

--product partner types (now called alliances on the back-end)
insert into custom.biomedgps_alliance_type (alliance_type_id, type_nm, section_cd, create_dt)
select 'PROD_'+cast(id as varchar), partner_type, 'PRODUCT', current_timestamp from biomedgps.products_partnertype;

--product alliance (partners) - for the companies and product we imported
insert into custom.biomedgps_product_alliance_xr (product_alliance_xr_id, alliance_type_id, 
company_id, product_id, reference_txt, order_no, GA_DISPLAY_FLG, create_dt)
select id, 'PROD_'+cast(partner_type_id as varchar), partner_id, product_id, reference, sort_order, 
case when hide_from_ga='true' then -1 when force_in_ga='true' then 1 else 0 end, current_timestamp 
from biomedgps.products_strategicpartner
where cast(product_id as varchar) in (select product_id from custom.biomedgps_product) 
and cast(partner_id as varchar) in (select company_id from custom.biomedgps_company);

-- product attributes

--insert top level product attributes, which feed the left column & Product Explorer
insert into custom.biomedgps_product_attribute 
(attribute_id, parent_id, attribute_nm, active_flg, type_cd, order_no, create_dt) values
('LINK',null,'Link',1,'LINK',1,current_timestamp),
('GRAPH',null,'Graphic',1,'GRAPH',2,current_timestamp),
('DETAILS_ROOT',null,'Product Details',1,null,5,current_timestamp),
('INDICATION','DETAILS_ROOT','Indication',1,null,8,current_timestamp),
('CLASSIFICATION','DETAILS_ROOT','Classification',1,null,8,current_timestamp),
('TECHNOLOGY','DETAILS_ROOT','Technology',1,null,8,current_timestamp),
('MARKET','DETAILS_ROOT','Target Market',1,null,8,current_timestamp),
('APPROACH','DETAILS_ROOT','Approach',1,null,8,current_timestamp);

-- insert product attribute "methods" (AKA Approaches)
insert into custom.biomedgps_product_attribute (attribute_id, parent_id, attribute_nm, active_flg, order_no, type_cd, create_dt)
select 'METHOD_'+cast(id as varchar), 'APPROACH', method,1,id,null,current_timestamp from biomedgps.products_method;

-- insert product attribute "target markets"
insert into custom.biomedgps_product_attribute (attribute_id, parent_id, attribute_nm, active_flg, order_no, type_cd, create_dt)
select 'MARKET_'+cast(id as varchar), 'MARKET', market,1,id,null,current_timestamp from biomedgps.products_targetmarket;

-- insert product attribute "technology"
insert into custom.biomedgps_product_attribute (attribute_id, parent_id, attribute_nm, abbr_nm, active_flg, order_no, type_cd, create_dt)
select 'TECHNOLOGY_'+cast(id as varchar), 'TECHNOLOGY', technology,case when short=technology then null else short end,
1,id,null,current_timestamp from biomedgps.products_technology;

-- insert product attribute "classification"
insert into custom.biomedgps_product_attribute (attribute_id, parent_id, attribute_nm, abbr_nm, active_flg, order_no, type_cd, create_dt)
select 'CLASSIFICATION_'+cast(id as varchar), 'CLASSIFICATION', classification,case when short=classification then null else short end,
1,id,null,current_timestamp from biomedgps.products_classification;

-- insert product attribute "indication"
insert into custom.biomedgps_product_attribute (attribute_id, parent_id, attribute_nm, abbr_nm, active_flg, order_no, type_cd, create_dt)
select 'INDICATION_'+cast(id as varchar), 'INDICATION', indication,case when short=indication then null else short end,
1,id,null,current_timestamp from biomedgps.products_indication;

-- insert product attribute XRs from method (Approach) table - for the products we imported
insert into custom.biomedgps_product_attribute_xr (product_attribute_id, attribute_id, product_id, create_dt)
select 'METHOD_'+cast(id as varchar),'METHOD_'+cast(method_id as varchar), product_id, current_timestamp 
from biomedgps.products_product_method where cast(product_id as varchar) in (select product_id from custom.biomedgps_product)
and 'METHOD_'+cast(method_id as varchar) in (select attribute_id from custom.biomedgps_product_attribute);

-- insert product attribute XRs from market table - for the products we imported
insert into custom.biomedgps_product_attribute_xr (product_attribute_id, attribute_id, product_id, create_dt)
select 'MARKET_'+cast(id as varchar),'MARKET_'+cast(targetmarket_id as varchar), product_id, current_timestamp 
from biomedgps.products_product_target_market where cast(product_id as varchar) in (select product_id from custom.biomedgps_product)
and 'MARKET_'+cast(targetmarket_id as varchar) in (select attribute_id from custom.biomedgps_product_attribute);

-- insert product attribute XRs from technology table - for the products we imported
insert into custom.biomedgps_product_attribute_xr (product_attribute_id, attribute_id, product_id, create_dt)
select 'TECHNOLOGY_'+cast(id as varchar),'TECHNOLOGY_'+cast(technology_id as varchar), product_id, current_timestamp 
from biomedgps.products_product_technology where cast(product_id as varchar) in (select product_id from custom.biomedgps_product);

-- insert product attribute XRs from classifications table - for the products we imported
insert into custom.biomedgps_product_attribute_xr (product_attribute_id, attribute_id, product_id, create_dt)
select 'CLASSIFICATION_'+cast(id as varchar),'CLASSIFICATION_'+cast(classification_id as varchar), product_id, current_timestamp 
from biomedgps.products_product_classification where cast(product_id as varchar) in (select product_id from custom.biomedgps_product);

-- insert product attribute XRs from indications table - for the products we imported
insert into custom.biomedgps_product_attribute_xr (product_attribute_id, attribute_id, product_id, create_dt)
select 'INDICATION_'+cast(id as varchar),'INDICATION_'+cast(indication_id as varchar), product_id, current_timestamp 
from biomedgps.products_product_indication where cast(product_id as varchar) in (select product_id from custom.biomedgps_product);

-- PRODUCT ATTRIBUTES FROM CONTENT HIERARCHY

-- add a root node for content
insert into custom.biomedgps_product_attribute 
(attribute_id, parent_id, attribute_nm, active_flg, type_cd, order_no, create_dt) values
('CONTENT_ROOT',null,'Product Content',1,null,1,current_timestamp);


-- product attributes level 1 - from their market level
insert into custom.biomedgps_product_attribute 
(attribute_id, parent_id, attribute_nm, active_flg, type_cd, order_no, create_dt)
select distinct 'LVL1_'+cast(a.id as varchar),'CONTENT_ROOT', trim(a.name),
case when a.visible then 1 else 0 end,'HTML',a.default_order, current_timestamp 
from biomedgps.articles_market a
inner join biomedgps.articles_segment b on a.id=b.market_id 
inner join biomedgps.articles_subsegment c on c.segment_id=b.id
inner join biomedgps.articles_article_subsegments d on c.id=d.subsegment_id
inner join biomedgps.articles_article e on d.article_id=e.id
where e.content_type_id=16;

-- product attributes level 2 - from their segment level
insert into custom.biomedgps_product_attribute 
(attribute_id, parent_id, attribute_nm, active_flg, type_cd, order_no, create_dt)
select distinct 'LVL2_'+cast(b.id as varchar),'LVL1_'+cast(a.id as varchar), trim(b.name),
case when b.visible then 1 else 0 end,'HTML',b.default_order, current_timestamp 
from biomedgps.articles_market a
inner join biomedgps.articles_segment b on a.id=b.market_id 
inner join biomedgps.articles_subsegment c on c.segment_id=b.id
inner join biomedgps.articles_article_subsegments d on c.id=d.subsegment_id
inner join biomedgps.articles_article e on d.article_id=e.id
where e.content_type_id=16;

-- product attributes level 3 (sub-headings) - from their sub-segment level
insert into custom.biomedgps_product_attribute 
(attribute_id, parent_id, attribute_nm, active_flg, type_cd, order_no, create_dt)
select distinct 'LVL3_'+cast(c.id as varchar),'LVL2_'+cast(b.id as varchar), trim(c.name),
case when c.visible then 1 else 0 end,'HTML',c.default_order, current_timestamp 
from biomedgps.articles_market a
inner join biomedgps.articles_segment b on a.id=b.market_id 
inner join biomedgps.articles_subsegment c on c.segment_id=b.id
inner join biomedgps.articles_article_subsegments d on c.id=d.subsegment_id
inner join biomedgps.articles_article e on d.article_id=e.id
where e.content_type_id=16;

-- data cleanup for product attribute names
update biomedgps.articles_article set summary='Recalls' 
	where summary='Recall' and content_type_id=16;
update biomedgps.articles_article set summary='Auditor''s Notes: Do Not Publish' 
	where summary in ('Auditor''s Notes - Do Not Publish','Auditor''s Notes','Auditor''s Note','Auditor''s notes','Auditor Note','Auditor''s Note') and content_type_id=16;
update biomedgps.articles_article set summary='Analyst''s Notes: Do Not Publish' 
	where summary in ('Analyst''s Notes','Analysts Notes','Analyst''s Notes:','Analyst''s Notes - Do Not Publish','ANALYST''S NOTE') and content_type_id=16;
update biomedgps.articles_article set summary='Clinical Update / Recent Studies' 
	where summary in ('Clinical Update & Recent Studies','Clinical Update','Clinical Updates','Clinical Updatehttps://www.smarttrak.net/products/5193/#') and content_type_id=16;
update biomedgps.articles_article set summary='Indications' 
	where summary='Indication' and content_type_id=16;
update biomedgps.articles_article set summary='Strategic Alliances' 
	where summary='Strategic Alliance' and content_type_id=16;
update biomedgps.articles_article set summary='Reimbursements' 
	where summary='Reimbursement' and content_type_id=16;
update biomedgps.articles_article set summary='Sales & Distribution' 
	where summary='Sales and Distribution' and content_type_id=16;
update biomedgps.articles_article set summary='Patents' 
	where summary='Patent' and content_type_id=16;
update biomedgps.articles_article set summary='New Article' 
	where lower(summary)='new_article' and content_type_id=16;
update biomedgps.articles_article set summary='Patents' 
	where summary='Patent' and content_type_id=16;
update biomedgps.articles_article set summary='Clinical Update' 
	where summary='Clinical Update for Flivasorb' and content_type_id=16;
update biomedgps.articles_article set summary='Pricing' 
	where summary in ('Pricing (2010)','Pricing **MAY NEED TO ADD THIS ARTICLE**') and content_type_id=16;
update biomedgps.articles_article set summary='Pricing & Reimbursement' 
	where summary in ('Pricing/Reimbursement','Pricing and Reimbursement') and content_type_id=16;
update biomedgps.articles_article set summary='Product Description -Stroke' 
	where summary in ('Product Description- Stroke','Product Description-Stroke') and content_type_id=16;
update biomedgps.articles_article set summary='Product Description' 
	where summary='Product Desciption' and content_type_id=16;
update biomedgps.articles_article set summary='Indications for Use' 
	where summary='Indication' and content_type_id=16;
update biomedgps.articles_article set summary='Product Description' 
	where summary='Product Description ** NEED TO SWITCH TO LIGAMENT REPAIR WHEN SEGMENT AVAIL***' and content_type_id=16;
update biomedgps.articles_article set summary='Summary' 
	where summary='This is a summary for Test 2 [DEMO DATA] Market' and content_type_id=16;
update biomedgps.articles_article set summary='Recent Studies' 
	where summary='Recent studies' and content_type_id=16;
update biomedgps.articles_article set summary='Editor''s Notes: Do Not Publish' 
	where summary='Do Not Publish - Editor''s Notes' and content_type_id=16;
update biomedgps.articles_article set summary='Clinical Update / Recent Studies' 
	where summary in ('Recent Studies','Clinical Update','Recent Studies /Clinical Update') and content_type_id=16;
	
	
-- product attributes level 4 (the names) - from their article summary
insert into custom.biomedgps_product_attribute 
(attribute_id, parent_id, attribute_nm, active_flg, type_cd, order_no, create_dt)
select distinct left(cast(c.id as varchar)+regexp_replace(e.summary, '[^a-zA-Z0-9]', '', 'g'),32),
'LVL3_'+cast(c.id as varchar), trim(e.summary), 1,'HTML',
case when min(e.sort_order) > 10000 then min(e.sort_order)/10000 else min(e.sort_order) end, current_timestamp 
from biomedgps.articles_market a
inner join biomedgps.articles_segment b on a.id=b.market_id 
inner join biomedgps.articles_subsegment c on c.segment_id=b.id
inner join biomedgps.articles_article_subsegments d on c.id=d.subsegment_id
inner join biomedgps.articles_article e on d.article_id=e.id
where e.content_type_id=16 
group by cast(c.id as varchar)+regexp_replace(e.summary, '[^a-zA-Z0-9]', '', 'g'),'LVL3_'+cast(c.id as varchar), trim(e.summary)
order by  left(cast(c.id as varchar)+regexp_replace(e.summary, '[^a-zA-Z0-9]', '', 'g'),32);

-- product attribute _xr
insert into custom.biomedgps_product_attribute_xr (product_attribute_id, attribute_id, 
product_id, value_txt, title_txt, order_no, status_no, create_dt, update_dt)
select replace(newid(),'-',''),left(cast(c.id as varchar)+regexp_replace(e.summary, '[^a-zA-Z0-9]', '', 'g'),32), 
e.object_id, e.content, e.summary,case when e.sort_order > 10000 then e.sort_order/10000 else e.sort_order end, 
e.status, e.create_date, e.last_update
from biomedgps.articles_market a
inner join biomedgps.articles_segment b on a.id=b.market_id 
inner join biomedgps.articles_subsegment c on c.segment_id=b.id
inner join biomedgps.articles_article_subsegments d on c.id=d.subsegment_id
inner join biomedgps.articles_article e on d.article_id=e.id
where e.content_type_id=16 and cast(e.object_id as varchar) in (select product_id from custom.biomedgps_product)
and c.id in (select subsegment_id from biomedgps.vproduct_subsegment) and e.status = 'P';

--product modulesets
insert into custom.biomedgps_product_moduleset (moduleset_id, set_nm, create_dt)
select id, name, current_timestamp from biomedgps.products_moduleset;

-- product moduleset XR for classifications - for modulesets we imported
insert into custom.biomedgps_product_moduleset_xr (product_moduleset_id, moduleset_id, attribute_id, create_dt)
select 'CLASSIFICATION_'+cast(id as varchar), moduleset_id, 'CLASSIFICATION_'+cast(classification_id as varchar), CURRENT_TIMESTAMP 
from biomedgps.products_moduleset_classifications 
where cast(moduleset_id as varchar) in (select moduleset_id from custom.biomedgps_product_moduleset);

-- product moduleset XR for indications - for modulesets we imported
insert into custom.biomedgps_product_moduleset_xr (product_moduleset_id, moduleset_id, attribute_id, create_dt)
select 'INDICATION_'+cast(id as varchar), moduleset_id, 'INDICATION_'+cast(indication_id as varchar), CURRENT_TIMESTAMP 
from biomedgps.products_moduleset_indications 
where cast(moduleset_id as varchar) in (select moduleset_id from custom.biomedgps_product_moduleset);

-- product moduleset XR for technologies - for modulesets we imported
insert into custom.biomedgps_product_moduleset_xr (product_moduleset_id, moduleset_id, attribute_id, create_dt)
select 'TECHNOLOGY_'+cast(id as varchar), moduleset_id, 'TECHNOLOGY_'+cast(technology_id as varchar), CURRENT_TIMESTAMP 
from biomedgps.products_moduleset_technologies 
where cast(moduleset_id as varchar) in (select moduleset_id from custom.biomedgps_product_moduleset);

-- product moduleset XR for technologies - for modulesets we imported
insert into custom.biomedgps_product_moduleset_xr (product_moduleset_id, moduleset_id, attribute_id, create_dt)
select 'MARKET_'+cast(id as varchar), moduleset_id, 'MARKET_'+cast(targetmarket_id as varchar), CURRENT_TIMESTAMP 
from biomedgps.products_moduleset_target_markets 
where cast(moduleset_id as varchar) in (select moduleset_id from custom.biomedgps_product_moduleset);

-- product moduleset XR for approaches (methods) - for modulesets we imported
insert into custom.biomedgps_product_moduleset_xr (product_moduleset_id, moduleset_id, attribute_id, create_dt)
select 'METHOD_'+cast(id as varchar), moduleset_id, 'METHOD_'+cast(method_id as varchar), CURRENT_TIMESTAMP 
from biomedgps.products_moduleset_methods 
where cast(moduleset_id as varchar) in (select moduleset_id from custom.biomedgps_product_moduleset);


--product notes - for teams we imported
insert into custom.biomedgps_note (note_id, user_id, team_id, note_nm, note_txt, file_path_txt, expiration_dt, create_dt, update_dt, product_id)
select id, creator_id, team_id, title, content, attachment, expiration, create_date, last_update, object_id from biomedgps.notes_note
where content_type_id=16 and cast(team_id as varchar) in (select team_id from custom.biomedgps_team);

-- product attribute notes - for teams we imported - tie the productId & attributeId
select n.id, n.creator_id, n.team_id, n.title, n.content, n.attachment, n.expiration, n.create_date, 
n.last_update,e.object_id, left(cast(c.id as varchar)+regexp_replace(e.summary, '[^a-zA-Z0-9]', '', 'g'),32) as attribute_id
into temporary notestemp
from biomedgps.articles_market a
inner join biomedgps.articles_segment b on a.id=b.market_id 
inner join biomedgps.articles_subsegment c on c.segment_id=b.id
inner join biomedgps.articles_article_subsegments d on c.id=d.subsegment_id
inner join biomedgps.articles_article e on d.article_id=e.id and e.content_type_id=16
inner join biomedgps.notes_note n on e.id=n.object_id and n.content_type_id=22
where cast(team_id as varchar) in (select team_id from custom.biomedgps_team);

--delete duplicate note bindings
delete from notestemp where ctid not in (select min(ctid) from notestemp group by id);

-- into product attribute notes
-- product attribute notes - for teams we imported - tie the productId & attributeId
insert into custom.biomedgps_note (note_id, user_id, team_id, note_nm, note_txt, file_path_txt, 
expiration_dt, create_dt, update_dt, product_id, attribute_id)
select id, creator_id, team_id, title, content, attachment, expiration, create_date, 
last_update,object_id, attribute_id from notestemp;

--drop the notes temp table.
drop table notestemp;

-- product_section_xr -- for product we imported
insert into custom.biomedgps_product_section (product_section_xr_id, section_id, product_id, create_dt)
select replace(newid(),'-',''),coalesce(s.section_id,s2.section_id), f.product_id, current_timestamp
from biomedgps.articles_market a
inner join biomedgps.articles_segment b on a.id=b.market_id 
inner join biomedgps.articles_subsegment c on c.segment_id=b.id
inner join biomedgps.vproduct_subsegment f on f.subsegment_id=c.id
left join custom.biomedgps_section s on c.name=s.section_nm and s.section_id like '%MASTER%'
left join custom.biomedgps_section s2 on b.name=s2.section_nm and s2.section_id like '%MASTER%'
where  c.visible='true' and coalesce(s.section_id,s2.section_id,null) is not null
and cast(f.product_id as varchar) in (select product_id from custom.biomedgps_product);


--product regulatory path
insert into custom.biomedgps_regulatory_path (path_id, path_nm, create_dt)
select id, path, current_timestamp from biomedgps.products_path;

--product regulatory status
insert into custom.biomedgps_regulatory_status (status_id, status_nm, create_dt)
select id, status, current_timestamp from biomedgps.products_milestonestatus;

--product regulatory region
insert into custom.biomedgps_regulatory_region (region_id, region_nm, create_dt)
select id, country, current_timestamp from biomedgps.products_pathcountry;

--product regulatory - for the products we imported
insert into custom.biomedgps_product_regulatory (regulatory_id, region_id, path_id, product_id, 
status_id, status_dt, intro_dt, reference_txt, create_dt)
select id, path_country_id, path_id, product_id, status_id, status_date, intro_date, reference, current_timestamp 
from biomedgps.products_regulatorymilestone 
where cast(product_id as varchar) in (select product_id from custom.biomedgps_product);



-- markets

--insert markets - all levels
insert into custom.biomedgps_market (market_id, parent_id, market_nm, short_nm, alias_nm, status_no, order_no, create_dt, update_dt)
select id,null,name, short_name, aka, upper(status), sort_order, create_date, last_update 
from biomedgps.marketreports_marketreport where lower(status) != 'd'; --do not migrate deleted markets? Ask Mike.

-- assign market regions - was not present in the old database
update custom.biomedgps_market set region_cd='FR' where market_nm like '%France%';
update custom.biomedgps_market set region_cd='DE' where market_nm like '%Germany%';
update custom.biomedgps_market set region_cd='WW' where market_nm like '%WW%' or market_nm  like '%Worldwide%';
update custom.biomedgps_market set region_cd='IT' where market_nm like '%Italy%';
update custom.biomedgps_market set region_cd='ES' where market_nm like '%Spain%';
update custom.biomedgps_market set region_cd='GB' where market_nm like '%UK%';
update custom.biomedgps_market set region_cd='US' where region_cd is null and market_nm like '%US%';


--insert markets - level 1 - these must be manually mapped from the website
/*
insert into custom.biomedgps_market (market_id, parent_id, market_nm, short_nm, alias_nm, status_no, order_no, create_dt, update_dt)
select id, ,name, short_name, aka, upper(status), sort_order, create_date, last_update from biomedgps.marketreports_marketreport 
where indent=1 and lower(status) != 'd' and id in ();
*/

-- market_section_xr -- for markets we imported
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt)
select replace(newid(),'-',''),coalesce(s.section_id,s2.section_id), f.marketreport_id, current_timestamp
from biomedgps.articles_market a
inner join biomedgps.articles_segment b on a.id=b.market_id 
inner join biomedgps.articles_subsegment c on c.segment_id=b.id
inner join biomedgps.vreport_subsegment f on f.subsegment_id=c.id
left join custom.biomedgps_section s on c.name=s.section_nm and s.section_id like '%MASTER%'
left join custom.biomedgps_section s2 on b.name=s2.section_nm and s2.section_id like '%MASTER%'
where c.visible='true' and coalesce(s.section_id,s2.section_id,null) is not null
and cast(f.marketreport_id as varchar) in (select market_id from custom.biomedgps_market);

--markets that require manual hierarchy placement (34)
select * from custom.biomedgps_market a left outer join custom.biomedgps_market_section b on a.market_id=b.market_id where b.market_id is null;


-- market attributes level 1 - from their market level
insert into custom.biomedgps_market_attribute 
(attribute_id, parent_id, attribute_nm, active_flg, type_cd, order_no, create_dt)
select distinct 'LVL1_'+cast(a.id as varchar),null, trim(a.name),
case when a.visible then 1 else 0 end,'HTML',a.default_order, current_timestamp 
from biomedgps.articles_market a
inner join biomedgps.articles_segment b on a.id=b.market_id 
inner join biomedgps.articles_subsegment c on c.segment_id=b.id
inner join biomedgps.articles_article_subsegments d on c.id=d.subsegment_id
inner join biomedgps.articles_article e on d.article_id=e.id
where e.content_type_id=62;

-- market attributes level 2 - from their segment level
insert into custom.biomedgps_market_attribute 
(attribute_id, parent_id, attribute_nm, active_flg, type_cd, order_no, create_dt)
select distinct 'LVL2_'+cast(b.id as varchar),'LVL1_'+cast(a.id as varchar), trim(b.name),
case when b.visible then 1 else 0 end,'HTML',b.default_order, current_timestamp 
from biomedgps.articles_market a
inner join biomedgps.articles_segment b on a.id=b.market_id 
inner join biomedgps.articles_subsegment c on c.segment_id=b.id
inner join biomedgps.articles_article_subsegments d on c.id=d.subsegment_id
inner join biomedgps.articles_article e on d.article_id=e.id
where e.content_type_id=62;

-- market attributes level 3 (sub-headings) - from their sub-segment level
insert into custom.biomedgps_market_attribute 
(attribute_id, parent_id, attribute_nm, active_flg, type_cd, order_no, create_dt)
select distinct 'LVL3_'+cast(c.id as varchar),'LVL2_'+cast(b.id as varchar), trim(c.name),
case when c.visible then 1 else 0 end,'HTML',c.default_order, current_timestamp 
from biomedgps.articles_market a
inner join biomedgps.articles_segment b on a.id=b.market_id 
inner join biomedgps.articles_subsegment c on c.segment_id=b.id
inner join biomedgps.articles_article_subsegments d on c.id=d.subsegment_id
inner join biomedgps.articles_article e on d.article_id=e.id
where e.content_type_id=62;

-- data cleanup for market names
update biomedgps.articles_article set summary='New Article' 
	where summary='New_Article' and content_type_id=62;

-- market attributes level 4 (the names) - from their article summary
insert into custom.biomedgps_market_attribute 
(attribute_id, parent_id, attribute_nm, active_flg, type_cd, order_no, create_dt)
select distinct left(cast(c.id as varchar)+regexp_replace(e.summary, '[^a-zA-Z0-9]', '', 'g'),32),
'LVL3_'+cast(c.id as varchar), trim(e.summary), 1,'HTML',
case when min(e.sort_order) > 10000 then min(e.sort_order)/10000 else min(e.sort_order) end, current_timestamp 
from biomedgps.articles_market a
inner join biomedgps.articles_segment b on a.id=b.market_id 
inner join biomedgps.articles_subsegment c on c.segment_id=b.id
inner join biomedgps.articles_article_subsegments d on c.id=d.subsegment_id
inner join biomedgps.articles_article e on d.article_id=e.id
where e.content_type_id=62 
group by cast(c.id as varchar)+regexp_replace(e.summary, '[^a-zA-Z0-9]', '', 'g'),'LVL3_'+cast(c.id as varchar), trim(e.summary)
order by  left(cast(c.id as varchar)+regexp_replace(e.summary, '[^a-zA-Z0-9]', '', 'g'),32);

-- market attribute _xr - for the markets we imported
insert into custom.biomedgps_market_attribute_xr (market_attribute_id, attribute_id, 
market_id, value_txt, order_no, status_no, create_dt, update_dt)
select replace(newid(),'-',''),left(cast(c.id as varchar)+regexp_replace(e.summary, '[^a-zA-Z0-9]', '', 'g'),32), 
e.object_id, e.content, case when e.sort_order > 10000 then e.sort_order/10000 else e.sort_order end, 
upper(e.status), e.create_date, e.last_update
from biomedgps.articles_market a
inner join biomedgps.articles_segment b on a.id=b.market_id 
inner join biomedgps.articles_subsegment c on c.segment_id=b.id
inner join biomedgps.articles_article_subsegments d on c.id=d.subsegment_id
inner join biomedgps.articles_article e on d.article_id=e.id
where e.content_type_id=62 and upper(e.status)='P'
and cast(e.object_id as varchar) in (select market_id from custom.biomedgps_market);


--market notes - for teams we imported
insert into custom.biomedgps_note (note_id, user_id, team_id, note_nm, note_txt, file_path_txt, expiration_dt, create_dt, update_dt, market_id)
select id, creator_id, team_id, title, content, attachment, expiration, create_date, last_update, object_id from biomedgps.notes_note
where content_type_id=62 and cast(team_id as varchar) in (select team_id from custom.biomedgps_team);

-- market attribute notes - for teams we imported - tie the productId & attributeId
insert into custom.biomedgps_note (note_id, user_id, team_id, note_nm, note_txt, file_path_txt, 
expiration_dt, create_dt, update_dt, market_id, attribute_id)
select n.id, n.creator_id, n.team_id, n.title, n.content, n.attachment, n.expiration, n.create_date, 
n.last_update,e.object_id, left(cast(c.id as varchar)+regexp_replace(e.summary, '[^a-zA-Z0-9]', '', 'g'),32) as attribute_id
from biomedgps.articles_market a
inner join biomedgps.articles_segment b on a.id=b.market_id 
inner join biomedgps.articles_subsegment c on c.segment_id=b.id
inner join biomedgps.articles_article_subsegments d on c.id=d.subsegment_id
inner join biomedgps.articles_article e on d.article_id=e.id and e.content_type_id=62
inner join biomedgps.notes_note n on e.id=n.object_id and n.content_type_id=22
where cast(team_id as varchar) in (select team_id from custom.biomedgps_team);



-- updates

--revision from Billy.  Need to turn type into an int before insertion
update biomedgps.updates_update set update_type = 15 where update_type in ('rev', 'Revenue');

-- market updates - bind all to Kevin Hicks since we don't otherwise know who created them
insert into custom.biomedgps_update 
(update_id, market_id, title_txt, type_cd, message_txt, twitter_txt, publish_dt, status_cd, tweet_flg, creator_profile_id, create_dt)
select id, object_id, subject, cast(update_type as int), display, tweet_message, date, upper(status), 
case when enable_tweet ='true' then 1 else 0 end, 'c0a802414bf056a746696d0df7dc039e', create_date
from biomedgps.updates_update where content_type_id=62;

-- company updates - for the companies we imported - bind all to Kevin Hicks since we don't otherwise know who created them
insert into custom.biomedgps_update 
(update_id, company_id, title_txt, type_cd, message_txt, twitter_txt, publish_dt, status_cd, tweet_flg, creator_profile_id, create_dt)
select id, object_id, subject, cast(update_type as int), display, tweet_message, date, upper(status), 
case when enable_tweet ='true' then 1 else 0 end, 'c0a802414bf056a746696d0df7dc039e', create_date
from biomedgps.updates_update where content_type_id=8 
and (object_id is null or cast(object_id as varchar) in (select company_id from custom.biomedgps_company));

-- product updates - for the products we imported - bind all to Kevin Hicks since we don't otherwise know who created them
insert into custom.biomedgps_update 
(update_id, product_id, title_txt, type_cd, message_txt, twitter_txt, publish_dt, status_cd, tweet_flg, creator_profile_id, create_dt)
select id, object_id, subject, cast(update_type as int), display, tweet_message, date, upper(status), 
case when enable_tweet ='true' then 1 else 0 end, 'c0a802414bf056a746696d0df7dc039e', create_date
from biomedgps.updates_update where content_type_id=16 
and (object_id is null or cast(object_id as varchar) in (select product_id from custom.biomedgps_product));

-- notes not attached to companies, products, or markets - bind to Kevin Hicks since we don't otherwise know who created them
insert into custom.biomedgps_update 
(update_id, title_txt, type_cd, message_txt, twitter_txt, publish_dt, status_cd, tweet_flg, creator_profile_id, create_dt)
select id, subject, cast(update_type as int), display, tweet_message, date, upper(status), case when enable_tweet ='true' then 1 else 0 end, 'c0a802414bf056a746696d0df7dc039e', create_date
from biomedgps.updates_update where content_type_id is null;

-- update_section_xr - for the updates we imported above
insert into custom.biomedgps_update_section (update_section_xr_id, create_dt, section_id, update_id)
select replace(newid(),'-',''), b.date, s.section_id, a.update_id 
from biomedgps.updates_update_nodes a
inner join biomedgps.updates_update b on a.update_id=b.id
left join custom.biomedgps_section s on 'MASTER_'+cast(a.node_id as varchar)=s.section_id and s.section_id like '%MASTER%'
where cast(a.update_id as varchar) in (select update_id from custom.biomedgps_update);





-- Commentary / Insights
/*
--blog (commentary) categories
insert into core.blog_category (blog_category_id, organization_id, category_nm, category_url, create_dt)
select 'BMG_SMARTTRAK_'+cast(id as varchar), 'BMG_SMARTTRAK', name, short_name, current_timestamp 
from  biomedgps.updates_category;

--blog (commentary) tags
insert into core.blog_category (blog_category_id, organization_id, category_nm, category_url, create_dt)
select 'BMG_SMARTTRAK_TAG_'+cast(id as varchar), 'BMG_SMARTTRAK', name, slug, current_timestamp 
from  biomedgps.commentaries_commentarytype;

--blog (commentary) authors
 need to create profiles for users first.

--blog (commentary) blogs
--bloggerID set to Mike for now: select * from core.blogger where organization_id='BMG_SMARTTRAK'
insert into core.blog (blog_id, action_id, blogger_id, title_nm, blog_txt, blog_url, approval_flg, publish_dt, 
create_dt, update_dt, images_flg, video_flg, short_desc_txt, browser_title, thumb_url, blog_group_id) 
select 'BMG_SMARTTRAK_'+cast(id as varchar), 'eb19b0e489ade9bd7f000101577715c8', 'c0a802418dc074dcc388278b38e946b4', 
title, coalesce(display,'') + '<div class="new-sidebar-wrapper">'+coalesce(side_display,'')+'</div>', null, 1, 
publish_date, create_date, last_update, null, null, null, title, null, null from biomedgps.commentaries_commentary;

--blog (commentary) category _XRs
insert into core.blog_category_xr (create_dt, blog_category_id, blog_id, tag_flg)
select current_timestamp, 'BMG_SMARTTRAK_'+cast(c.id as varchar), 'BMG_SMARTTRAK_'+cast(a.id as varchar), 0
from biomedgps.commentaries_commentary a
inner join biomedgps.commentaries_commentary_categories b on a.id=b.commentary_id
inner join biomedgps.updates_category c on b.category_id=c.id;

--blog (commentary) tag _XRs
insert into core.blog_category_xr (create_dt, blog_category_id, blog_id, tag_flg)
select current_timestamp, 'BMG_SMARTTRAK_TAG_'+cast(b.id as varchar), 'BMG_SMARTTRAK_'+cast(a.id as varchar), 1
from biomedgps.commentaries_commentary a
inner join biomedgps.commentaries_commentarytype b on a.commentary_type_id=b.id
*/

insert into custom.biomedgps_insight (insight_id, creator_profile_id, title_txt, type_cd, abstract_txt, 
byline_txt, content_txt, side_content_txt, featured_flg, featured_image_txt, status_cd, order_no, 
publish_dt, create_dt, update_dt)
select cast(id as varchar), b.profile_id, title, commentary_type_id, abstract, by_line, content, side_display,0,null,upper(status), sort_order, publish_date, create_date, last_update 
from biomedgps.commentaries_commentary a
inner join custom.biomedgps_user b on cast(a.author_id as varchar)=b.user_id;

--insert insights with no author, attached to mikes "SmartTRAK Analyst" user profile.  Get profileId from the database first.
insert into custom.biomedgps_insight (insight_id, creator_profile_id, title_txt, type_cd, abstract_txt, 
byline_txt, content_txt, side_content_txt, featured_flg, featured_image_txt, status_cd, order_no, 
publish_dt, create_dt, update_dt)
select cast(id as varchar), 'dfdda0aeafbee655c0a8024ab7754686', title, commentary_type_id, abstract, by_line, content, side_display,0,null,
upper(status), sort_order, publish_date, create_date, last_update 
from biomedgps.commentaries_commentary a
left join custom.biomedgps_user b on cast(a.author_id as varchar)=b.user_id
where b.user_id is null;


-- insight section _XR - for the insights we imported
insert into custom.biomedgps_insight_section (insight_section_xr_id, insight_id, section_id, create_dt)
select replace(newid(),'-',''),a.commentary_id, 'MASTER_' + cast(b.node_id as varchar), current_timestamp 
from biomedgps.commentaries_commentary_categories a
inner join biomedgps.updates_category b on a.category_id=b.id
where b.node_id is not null and cast(a.commentary_id as varchar) in (select insight_id from custom.biomedgps_insight);




-- fix solr_token_txt on the section table

-- add a function to generate random strings - this can be deleted (if desired) after applying random values to _section.solr_token_txt
Create or replace function random_string(length integer) returns text as
$$
declare
  chars text[] := '{0,1,2,3,4,5,6,7,8,9,A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z,a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z}';
  result text := '';
  i integer := 0;
begin
  if length < 0 then
    raise exception 'Given length cannot be less than 0';
  end if;
  for i in 1..length loop
    result := result || chars[1+random()*(array_length(chars, 1)-1)];
  end loop;
  return result;
end;
$$ language plpgsql;

-- apply random values to section levels
update custom.biomedgps_section set solr_token_txt=random_string(5);

--verify random values
select * from custom.biomedgps_section;
select solr_token_txt, count(*) from custom.biomedgps_section group by solr_token_txt having count(*) > 1;


-- update section FD publish-date data
update custom.biomedgps_section as a 
set fd_pub_yr=cast(substr(b.published_through,2,4) as int), fd_pub_qtr=cast(right(b.published_through,1) as int)
from biomedgps.nodes_node b where 'MASTER_'+cast(b.id as varchar)=a.section_id;


-- user & account permissions


--account FD flag
update custom.biomedgps_account as a set fd_auth_flg=1
from biomedgps.profiles_account_permissions b where cast(b.account_id as varchar)=a.account_id and b.permission_id=541;

--account GA flag
update custom.biomedgps_account as a set ga_auth_flg=1
from biomedgps.profiles_account_permissions b where cast(b.account_id as varchar)=a.account_id and b.permission_id=596;

--user FD flag
update custom.biomedgps_user as a set fd_auth_flg=1
from biomedgps.profiles_user_user_permissions b where cast(b.user_id as varchar)=a.user_id and b.permission_id=541;

--user GA flag
update custom.biomedgps_user as a set ga_auth_flg=1
from biomedgps.profiles_user_user_permissions b where cast(b.user_id as varchar)=a.user_id and b.permission_id=596;

--create a temporary table for L4 sections, so we don't have to hunt for them each time
select section_id, section_nm into temporary l4sections from custom.biomedgps_section where parent_id in ( --4
	select section_id from custom.biomedgps_section where parent_id in ( --3
		select section_id from custom.biomedgps_section where  --2
			parent_id='MASTER_ROOT')); --1

-- Insert an ACL for every account for every 4th level section.  
-- We'll update these with the legacy data, then delete any that we don't need at the end
insert into custom.biomedgps_account_acl (account_acl_id, section_id, account_id, create_dt)
select replace(newid(),'-',''),section_id, account_id, current_timestamp from custom.biomedgps_account a join l4sections b on 1=1;

-- update the FD flag on the account ACLs
update custom.biomedgps_account_acl as a set fd_no=1
FROM biomedgps.nodes_node t1
left join custom.biomedgps_section sec on replace('MASTER_'+cast(t1.source_node_id as varchar),'MASTER_2000','MASTER_ROOT')=sec.section_id
inner join biomedgps.nodes_accounttree nact on t1.tree_id=nact.tree_id
where replace('MASTER_'+cast(t1.source_node_id as varchar),'MASTER_2000','MASTER_ROOT') 
	in (select section_id from l4sections)
and a.account_id=cast(nact.account_id as varchar) 
and a.section_id=replace('MASTER_'+cast(t1.source_node_id as varchar),'MASTER_2000','MASTER_ROOT')
and t1.hidden=false;

-- update the Upd flag on the account ACLs
update custom.biomedgps_account_acl as a set updates_no=1
FROM biomedgps.profiles_account_update_nodes b
inner join custom.biomedgps_section sec on replace('MASTER_'+cast(b.node_id as varchar),'MASTER_2000','MASTER_ROOT')=sec.section_id
where replace('MASTER_'+cast(b.node_id as varchar),'MASTER_2000','MASTER_ROOT') in (select section_id from l4sections)
and a.account_id=cast(b.account_id as varchar) and a.section_id=replace('MASTER_'+cast(b.node_id as varchar),'MASTER_2000','MASTER_ROOT');

-- update the GA flag on the account ACLs
update custom.biomedgps_account_acl as a set ga_no=1
FROM biomedgps.profiles_account_gap_analysis_nodes b
inner join custom.biomedgps_section sec on replace('MASTER_'+cast(b.node_id as varchar),'MASTER_2000','MASTER_ROOT')=sec.section_id
where replace('MASTER_'+cast(b.node_id as varchar),'MASTER_2000','MASTER_ROOT') in (select section_id from l4sections)
and a.account_id=cast(b.account_id as varchar) and a.section_id=replace('MASTER_'+cast(b.node_id as varchar),'MASTER_2000','MASTER_ROOT');

-- update the browse ("Prof") flag on the account ACLs
update custom.biomedgps_account_acl as x set browse_no=1
FROM biomedgps.profiles_account_subsegments_master a
inner join biomedgps.nodes_node b on a.subsegment_id=b.subsegment_id and b.tree_id=2000
inner join custom.biomedgps_section sec on replace('MASTER_'+cast(b.source_node_id as varchar),'MASTER_2000','MASTER_ROOT')=sec.section_id
where replace('MASTER_'+cast(b.source_node_id as varchar),'MASTER_2000','MASTER_ROOT') in (select section_id from l4sections)
and x.account_id=cast(a.account_id as varchar) and x.section_id=replace('MASTER_'+cast(b.source_node_id as varchar),'MASTER_2000','MASTER_ROOT');

--cleanup
drop table l4sections;
delete from custom.biomedgps_account_acl where updates_no is null and browse_no is null and fd_no is null and ga_no is null;



-- user favorites
insert into profile_favorite (profile_id, uri_txt, type_cd, rel_id, site_id, create_dt)
select b.profile_id, 
case when content_type_id = 62 then '/markets/qs/' + cast(object_id as varchar)
 when content_type_id = 16 then '/products/qs/' + cast(object_id as varchar)
 when content_type_id = 8 then '/companies/qs/' + cast(object_id as varchar) end,
case when content_type_id = 62 then 'MARKET'
 when content_type_id = 16 then 'PRODUCT'
 when content_type_id = 8 then 'COMPANY' end, object_id,'BMG_SMARTTRAK_1',max(last_update) 
from biomedgps.favorites_favorite a
inner join custom.biomedgps_user b on cast(a.author_id as varchar)=b.user_id
left join custom.biomedgps_market m on a.content_type_id=62 and cast (a.object_id as varchar)=m.market_id
left join custom.biomedgps_company c on a.content_type_id=8 and cast (a.object_id as varchar)=c.company_id
left join custom.biomedgps_product p on a.content_type_id=16 and cast (a.object_id as varchar)=p.product_id
where m.market_id is not null or p.product_id is not null or c.company_id is not null
group by b.profile_id, 
case when content_type_id = 62 then '/markets/qs/' + cast(object_id as varchar)
 when content_type_id = 16 then '/products/qs/' + cast(object_id as varchar)
 when content_type_id = 8 then '/companies/qs/' + cast(object_id as varchar)
end, 
case when content_type_id = 62 then 'MARKET'
 when content_type_id = 16 then 'PRODUCT'
 when content_type_id = 8 then 'COMPANY' end, object_id;


-- pageviews

/*
--pageviews?
select * from site where organization_id='BMG_SMARTTRAK';
select * from page where site_id='BMG_SMARTTRAK_1'; --need these values below
select * from biomedgps.favorites_breadcrumb 
where object_id is null 
and page_name not like 'Financial %' 
and page_name !='Home' 
and page_name not like 'SmartSearch%'
and page_name !='Product Roster'
and page_name not like 'Company%'
and page_name !='Market Roster'
and page_name !='View Market Report'
and page_name not like 'Updates%'
and page_name not like 'Account Management%'
and page_name !='Gap Analysis'
and page_name not like '%/products/explore/%'
and page_name !='Product Explorer'
and page_name not like 'Full SmartSearch %'
and page_name not like 'Add %'
and page_name not like 'Edit %'
and page_name not like 'Help %'
and page_name not like 'Password %'
and page_name not like 'Commentary %'
and page_name not like 'Commentaries:%'
order by time_viewed desc;
*/

-- page views for the 3 known sections of the site.
select * from page where site_id='BMG_SMARTTRAK_1'; --need these values below
insert into core.pageview_user (pageview_user_id, site_id, profile_id, session_id, page_id, request_uri_txt, visit_dt, create_dt)
select 'BMG_SMARTTRAK_'+cast(id as varchar), 'BMG_SMARTTRAK_1', b.profile_id, session,
-- inject pageIds
case when content_type_id = 62 then 'c0a80241657fbc6d312081cff3ae969a'
 when content_type_id = 16 then 'c0a8024165807f37308c38cbf36b3ede'
 when content_type_id = 8 then 'c0a8024165801753a90aa0323f30b386' end,
-- build URLs
 case when content_type_id = 62 then '/markets/qs/' + cast(object_id as varchar)
 when content_type_id = 16 then '/products/qs/' + cast(object_id as varchar)
 when content_type_id = 8 then '/companies/qs/' + cast(object_id as varchar) end,time_viewed, current_timestamp
from biomedgps.favorites_breadcrumb a
inner join custom.biomedgps_user b on cast(a.viewer_id as varchar)=b.user_id
where a.object_id is not null and a.content_type_id in (8,16,62);

--homepage visits
insert into core.pageview_user (pageview_user_id, site_id, profile_id, session_id, page_id, request_uri_txt, visit_dt, create_dt)
select 'BMG_SMARTTRAK_'+cast(id as varchar), 'BMG_SMARTTRAK_1', b.profile_id, session, 'c0a802418db3207c71504702fd94efd5','/',time_viewed, current_timestamp
from biomedgps.favorites_breadcrumb a
inner join custom.biomedgps_user b on cast(a.viewer_id as varchar)=b.user_id
where a.object_id is null and page_name='Home';

--FD visits
insert into core.pageview_user (pageview_user_id, site_id, profile_id, session_id, page_id, request_uri_txt, visit_dt, create_dt)
select 'BMG_SMARTTRAK_'+cast(id as varchar), 'BMG_SMARTTRAK_1', b.profile_id, session, 'c0a8024165822e699829c4be3bbda67f','/tools/financial',time_viewed, current_timestamp
from biomedgps.favorites_breadcrumb a
inner join custom.biomedgps_user b on cast(a.viewer_id as varchar)=b.user_id
where a.object_id is null and page_name like 'Financial %';

--search visits
insert into core.pageview_user (pageview_user_id, site_id, profile_id, session_id, page_id, request_uri_txt, query_str_txt, visit_dt, create_dt)
select 'BMG_SMARTTRAK_'+cast(id as varchar), 'BMG_SMARTTRAK_1', b.profile_id, session, '8c3972d994743e800a0014215e515685','/search', 
'searchData=' + replace(page_name,'SmartSearch term = ',''),time_viewed, current_timestamp
from biomedgps.favorites_breadcrumb a
inner join custom.biomedgps_user b on cast(a.viewer_id as varchar)=b.user_id
where a.object_id is null and page_name like 'SmartSearch%';

--company landing page visits
insert into core.pageview_user (pageview_user_id, site_id, profile_id, session_id, page_id, request_uri_txt, visit_dt, create_dt)
select 'BMG_SMARTTRAK_'+cast(id as varchar), 'BMG_SMARTTRAK_1', b.profile_id, session, 'c0a8024165801753a90aa0323f30b386','/companies', time_viewed, current_timestamp
from biomedgps.favorites_breadcrumb a
inner join custom.biomedgps_user b on cast(a.viewer_id as varchar)=b.user_id
where a.object_id is null and page_name like 'Company%';

--product landing page visits
insert into core.pageview_user (pageview_user_id, site_id, profile_id, session_id, page_id, request_uri_txt, visit_dt, create_dt)
select 'BMG_SMARTTRAK_'+cast(id as varchar), 'BMG_SMARTTRAK_1', b.profile_id, session, 'c0a8024165807f37308c38cbf36b3ede','/products', time_viewed, current_timestamp
from biomedgps.favorites_breadcrumb a
inner join custom.biomedgps_user b on cast(a.viewer_id as varchar)=b.user_id
where a.object_id is null and page_name='Product Roster';

--market landing page visits
insert into core.pageview_user (pageview_user_id, site_id, profile_id, session_id, page_id, request_uri_txt, visit_dt, create_dt)
select 'BMG_SMARTTRAK_'+cast(id as varchar), 'BMG_SMARTTRAK_1', b.profile_id, session, 'c0a80241657fbc6d312081cff3ae969a','/markets', time_viewed, current_timestamp
from biomedgps.favorites_breadcrumb a
inner join custom.biomedgps_user b on cast(a.viewer_id as varchar)=b.user_id
where a.object_id is null and (page_name='Market Roster' or page_name='View Market Report');

--Gap Analysis page visits
insert into core.pageview_user (pageview_user_id, site_id, profile_id, session_id, page_id, request_uri_txt, visit_dt, create_dt)
select 'BMG_SMARTTRAK_'+cast(id as varchar), 'BMG_SMARTTRAK_1', b.profile_id, session, 'c0a802416581c8ef84bec769d36bec89','/tools/analysis', time_viewed, current_timestamp
from biomedgps.favorites_breadcrumb a
inner join custom.biomedgps_user b on cast(a.viewer_id as varchar)=b.user_id
where a.object_id is null and page_name='Gap Analysis';

--Product Explorer page visits
insert into core.pageview_user (pageview_user_id, site_id, profile_id, session_id, page_id, request_uri_txt, visit_dt, create_dt)
select 'BMG_SMARTTRAK_'+cast(id as varchar), 'BMG_SMARTTRAK_1', b.profile_id, session, 'c0a802416581638746748ef07a7221e9','/tools/explorer', time_viewed, current_timestamp
from biomedgps.favorites_breadcrumb a
inner join custom.biomedgps_user b on cast(a.viewer_id as varchar)=b.user_id
where a.object_id is null and (page_name like '%/products/explore/%' or page_name='Product Explorer');

--delete pageviews tied to inactive accounts that are older than 1/1/2016 - Lisa Mahan approved this to prune the data
delete from core.pageview_user where profile_id in (
	select profile_id from custom.biomedgps_user where account_id in 
		(select account_id from custom.biomedgps_account where status_no='I')
) and visit_dt < '2016-01-01';



-- add the Staff role to all page, layouts, and _modules
insert into PAGE_ROLE (PAGE_ID, ROLE_ID, CREATE_DT, PAGE_ROLE_ID)
select p.PAGE_ID, '3eef678eb39e87277f000101dfd4f140',GETDATE(), replace(newid(),'-','') from core.PAGE p 
left outer join core.PAGE_ROLE pr1 on p.PAGE_ID=pr1.PAGE_ID and pr1.ROLE_ID='10'
left outer join core.PAGE_ROLE pr2 on p.PAGE_ID=pr2.PAGE_ID and pr2.ROLE_ID='3eef678eb39e87277f000101dfd4f140'
where p.SITE_ID='BMG_SMARTTRAK_1' and pr1.page_role_id is not null and pr2.page_role_id is null;

insert into PAGE_ROLE (PAGE_ID, ROLE_ID, CREATE_DT, PAGE_ROLE_ID)
select p.PAGE_ID, '3eef678eb39e87277f000101dfd4f140',GETDATE(), replace(newid(),'-','') from core.PAGE p 
left outer join core.PAGE_ROLE pr1 on p.PAGE_ID=pr1.PAGE_ID and pr1.ROLE_ID='10'
left outer join core.PAGE_ROLE pr2 on p.PAGE_ID=pr2.PAGE_ID and pr2.ROLE_ID='100'
where p.SITE_ID='BMG_SMARTTRAK_2' and pr1.page_role_id is not null and pr2.page_role_id is null;

insert into PAGE_MODULE_ROLE (PAGE_MODULE_ROLE_ID, ROLE_ID, CREATE_DT, PAGE_MODULE_ID)
select replace(newid(),'-',''), '3eef678eb39e87277f000101dfd4f140', GETDATE(), pm.page_module_id  from PAGE_MODULE pm
left outer join core.PAGE_MODULE_ROLE pr1 on pm.PAGE_MODULE_ID=pr1.PAGE_MODULE_ID and pr1.ROLE_ID='10'
left outer join core.PAGE_MODULE_ROLE pr2 on pm.PAGE_MODULE_ID=pr2.PAGE_MODULE_ID and pr2.ROLE_ID='3eef678eb39e87277f000101dfd4f140'
left join PAGE p on p.PAGE_ID = pm.PAGE_ID
where p.SITE_ID='BMG_SMARTTRAK_1' and pr1.page_module_role_id is not null and pr2.page_module_role_id is null;

insert into PAGE_MODULE_ROLE (PAGE_MODULE_ROLE_ID, ROLE_ID, CREATE_DT, PAGE_MODULE_ID)
select replace(newid(),'-',''), '3eef678eb39e87277f000101dfd4f140', GETDATE(), pm.page_module_id  from PAGE_MODULE pm
left outer join core.PAGE_MODULE_ROLE pr1 on pm.PAGE_MODULE_ID=pr1.PAGE_MODULE_ID and pr1.ROLE_ID='10'
left outer join core.PAGE_MODULE_ROLE pr2 on pm.PAGE_MODULE_ID=pr2.PAGE_MODULE_ID and pr2.ROLE_ID='100'
left join PAGE p on p.PAGE_ID = pm.PAGE_ID
where p.SITE_ID='BMG_SMARTTRAK_2' and pr1.page_module_role_id is not null and pr2.page_module_role_id is null;



--- fix market->section bindings.  Mike created these manually:
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_10815','6559',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_8374','6562',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_18689','6529',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_278','479',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_353','6584',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_21221','6588',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_32111','6603',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_34117','6614',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_34872','6615',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_34268','6619',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_34268','6620',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_34419','6621',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_29347','6623',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_32259','6639',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_8374','6564',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_330','6570',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_318','6572',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_21405','6579',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_338','6581',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_326','6591',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_44830','6641',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_18689','6533',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_328','6540',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_289','6544',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_318','6574',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_338','6583',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_32259','6637',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_328','4136',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_8374','6563',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_338','6582',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_326','6590',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_326','6592',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_21405','6599',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_32259','6604',CURRENT_TIMESTAMP);


-- company audit data
insert into custom.biomedgps_audit_log (audit_log_id, company_id, auditor_profile_id, status_cd, start_dt, complete_dt, update_dt)
select a.id, a.object_id, b.profile_id, upper(a.status), a.start_date, a.end_date,coalesce(a.end_date, a.start_date, current_timestamp) 
from biomedgps.audit_audit a
inner join custom.biomedgps_user b on cast(a.auditor_id as varchar)=b.user_id
inner join custom.biomedgps_company c on cast(a.object_id as varchar)=c.company_id
where a.content_type_id=8;
--check for any other audit types, there shouldn't be any though;
select * from biomedgps.audit_audit where content_type_id != 8;



-- convert markup -- Java process
Need to run com.smt.sitebuilder.db.MarkupConverter from command line to convert the markup stored in the database to HTML.


-- place binary images and media

-- apache rules for binary and media




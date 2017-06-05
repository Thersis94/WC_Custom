
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
select id, id, name, address, address1, city, state, zip, coalesce(country_cd,'US'), phone, phone_800, 
0, create_date, last_update FROM biomedgps.companies_company a
left join core.country b on a.country_id=b.country_nm;


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
inner join biomedgps.gap_analysis_gamarket b on a.market_id=b.id
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




-- accounts


--select * from core.state where country_cd='GB' order by state_nm
--select * from biomedgps.profiles_account where len(state)>5;

--update bad country values
update biomedgps.profiles_account set country='US' where country in ('Unitednnnn States','United States','United States of America','USA');
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

--delete bogus accounts from the source tables
delete from biomedgps.profiles_user where username in ('exp_abigail.freigang','X47XXXabridges@upperoptions.com','X6XXXabridges@upperoptions.com','expbVM_exp_acornell@its.jnj.coexp',
'acornell@its.jnj.comexp','exp_acornell@its.jnj.comexp','exp_ada.au','exp_adenti1@its.jnj.com37exp','adenti1@its.jnj.comexp',
'exp_AGangul2@ITS.JNJ.comexp','exp_agangul2@its.jnj.com38exp','X80Xajford@medline.com','alexander.skinner',
'exp_alexander.skinner@kci1.com','X22Xalexander.skinner@kci1.com','exp_alex.drigan@ferring.com',
'exp_andrea.cardenas@kci1.com95','exp_andrea.sheehan@arthrex','exp_andrea.sheehan@arthrex.com','exp_andrea.sheehan',
'exp_aura@biomedgps.com70','aura@biomedgps.com','X48XXawest@carmellrx.com','X86XXawest@carmellrx.com',
'exp_awohl@rtix.com','exp_bb@cap-partner.eu','exp_bcarlson@orthosensor.com','Becky.Rutland@Arthrex.com','exp_becky.rutland@arthrex.com6',
'exp_ben.burnham@stryker.com','exp_bernard.difrancesco78','exp_bernard.difrancesco','exp_bnichols@nuvasive.com72',
'exp_bnichols@nuvasive.com','exp_Bob.Mcnamara','expVYE_exp_bobmcnamara@ldrspin','exp_bobmcnamara@ldrspine.com',
'X97XXXbobp@dfineinc.com','X85XBobp@dfineinc.com','exp_bordeaux.jean@','exp_bordeaux.jean','X80XXXbrandon.henry@rbccm.com',
'X30XBrandon.Roller@Arthrex.com','X40XBrandon.Roller@Arthrex.com','X58XBrandon.Roller@Arthrex.com','exp_brian.mckinnon',
'X46Xbrian.reed@aesculap.com','X71Xbrian.reed@aesculap.com','exp_burgess.ian','exp_burgess.ian@synthes.com','exp_bethwalters',
'exphAU_exp_bwolfenson@dermasci','exp_bwolfenson@dermasciences.c','exp_caker1@its.jnj.com73exp','exp_caker181',
'cathleen@biomedgps.com89','X47XXEXPcblakely@rtix.com','exp_cclupper@its.jnj.comexp','exp_cclupper','X60Xceckh42169@aol.com',
'X93Xceckh42169@aol.com','X2XXXceckh42169@aol.com','exp_cgumera','X79XXXEXPcgumera@wlgore.com','exp_charliegilbride@ldrspine.c',
'exp_Chelsea.boyte@acumed.net','exp_ex_trchris.chapman@exac.co','X74XXXchris.chapman@exac.com','exp_chris@mahan9group.com',
'exp_ex_trchris.nader@aesculap.','exp_chrispangman@orthofix.com','exp_chrisv','X57XXXEXPchris.valois@zimmer.com','X34Xchuck.williams@medtronic.com',
'exp_ex_trchuck.williams@medtro','X94Xcindy.obrecht@arthrex.com','exp_cindy.obrecht@arthrex.com','X23Xcjennewine@alphatecspine.com','X46Xcjennewine@alphatecspine.com','exp_ex_trckoren@innovationmedi',
'X60XXXckoren@innovationmedical.com','exp_ex_trckuliga@spinewave.com','X85Xckuliga@spinewave.com','exp_clareen.steve','exp_clareen.steve@synthes.com','exp_clloegering1@mmm.com','exp_ex_trcluetto@meridian-med.','X53Xcluetto@meridian-med.com','exp_CODell','X62XXXEXPCodell@organo.com','exp_coffman.katherine@synthes.','exp_craig.kennedy@systagenix.c',
'exp_ex_trexpwyT_exp_craig.kenn','exp_ex_trcritter@misonix.com','X79XXXcritter@misonix.com','exp_cwagne17','exp_cwagne17@dpyus.jnj.comexp',
'exp_ex_trdan.hann@biomet.com','X96Xdan.hann@biomet.com','exp_ex_trdanielle.petrow@aptar','X19XXXdanielle.petrow@aptar.com','exp_daniel.ludwig','exp_Daniel.Ludwig@stryker.com',
'X41Xdan.williamson@biomet.com','X76Xdan.williamson@biomet.com','exp_david.evans@smith-nephew.c','X34XXXdavid.mekeel@smith-nephew.com','exp_David.Nolan@zimmer.com','exp_ex_trdavis','X10Xdavis@activelifescientific','exp_dbanks@orthohelix.com','exp_ex_trexpQgH_exp_dbanks@ort','exp_dbeaubien@mmm.com',
'exp_dcoppes@its.jnj.comexp','exp_ddemski@globusmedical.com','exp_ex_trddemski@globusmedical','exp_dducharme@orthohelix.com','exp_dducharme@orthohelix.com70','exp_ex_trexp8A6_exp_dducharme@','X42Xdducharme@orthohelix.com','exp_ddufour','X63Xdedgar3@its.jnj.com','exp_Devan.Ball@zimmer.com19','X89XXdidier.t@nlt-spine.com','exp_ex_trdineen.zimmer@bbraun.','exp_dineen.zimmer@bbraun.com','X42Xdkuyper@alphatecspine.com','X48Xdkuyper@alphatecspine.com','exp_deniseluciano',
'exp_d.mills@dallenmedical.com2','exp_d.mills@dallenmedical.com','X83Xdonna.moats@smith-nephew.com','exp_ex_trdschmierer@osteomed.c','exp_dstoller','exp_dstoller@its.jnj.comexp','exp_ex_trdwhite@allosource.org','exp_dwhite@allosource.org63','exp_dwhite@allosource.org',
'exp_ex_treanapliotis@merete.de','exp_eanapliotis@merete.de70','exp_eileendunne','exp_efender','X41XEXPefender@vertiflexspine.com','exp_ex_trexpVWw_exp_elaine.f.s','exp_elaine.f.sebak@medtronic.c','exp_elin.almegren','exp_ex_tremccloy@accutektestin','X44Xemccloy@accutektesting.com','X78XEric.Dremel@amportho.com','X73Xeric.goslau@l5partners.com','X21XXXeric.goslau@L5partners.com','X97Xfrhaley@mmm.com','exp_ex_trgary.l.green@medtroni','X56XXXgary.l.green@medtronic.com','exp_gates.juston@synthes.com','expunL_exp_gates.juston@synthe','exp_ex_trinfo@barrx.com','exp_ex_trgbarrett@barrx.com','exp_ex_trexpM4j_exp_gdericks@e','exp_gdericks@eesus.jnj.comexp','X58XXXgeorge_oram@mtf.org','exp_glen.sokaloski','exp_glen.sokaloski@ferring.com','X82XXXgorkem@medistgroup.com','exp_grobinson@kirchnergroup.co','X82XHartmansg@aol.com','X85XHartmansg@aol.com','X58Xhdavies@greatbatchmedical.com','exp_ex_trhdavies@greatbatchmed','exp_hearn.jim@synthes.com','exp_hearn.jim','X59XXhollytshaw@gmail.com','exp_ex_trhollytshaw@gmail.com','X36Xh.oonishi@teijin.co.jp','X81XXh.oonishi@teijin.co.jp','exp_htang2','exp_htang','exp_htang2@its.jnj.com73exp','exp_ian.dawson','exp_ian.dawson@smith-nephew.co','exp_ilka.bijoux','exp_isira@mmm.com','exp_ex_trexpRnn_exp_isira@mmm.','exp_ex_trjacob.haskins@exac.co','X80XXXjacob.haskins@exac.com',
'X36XXXjamey.rottman@zimmer.com','exp_jason.fowler','exp_jay.sachnoff','exp_jay.sachnoff@ferring.com','X64Xjbapst@k2m.com','jbonitat@its.jnj.comexp','exp_jbonitat@its.jnj.comexp','exp_ex_trjburgessts@gmail.com','exp_jburgessts@gmail.com20','exp_jcash@dpyus.jnj.comexp','exp_jcash@dpyus.jnjexp','exp_jdgordon@orthosensor.com92','X53Xjdl@riverstreetmgt.com','X13Xjdl@riverstreetmgt.com65','exp_ex_trjdl@riverstreetmgt.co','X61Xjean-marc.ferrier@graftys.com','X30Xjean-marc.ferrier@graftys.com','X0XXXjed.white@kenseynash.com','X17Xjed.white@kenseynash.com64','X5XXjeffrey.a.husak@medtronic.com','X84Xjeffrey.a.husak@medtronic.com','X23Xjeffrey.scifert@medtronic.com','exp_ex_trjeffrey.scifert@medtr','X45Xjeffrey.scifert@medtronic.com','X39Xjeff.stebbins@biomet.com','X9XXXjeff.stebbins@biomet.com','exp_jeff.stebbins@biomet.com26','exp_jeff.stebbins@biomet.com44','exp_jenn.davis','exp_jenn.davis@acumed.net','exp_jennifer.gotto@bbraun.com4','exp_jennifer.gotto@bbraun.com','exp_ex_trjennifer.grasso@biome','exp_ex_trjgannoe@extremitymedi','jgannoe@extremitymedical.com','jgiroux@pivotmedical.com35','exp_jgiroux@pivotmedical.com','X42Xjjohnson@amedica.com','exp_ex_trjoconnor@accelalox.co','X47Xjoconnor@accelalox.com','X45Xjoel.pickering@systagenix.com','exp_ex_trjoel.pickering@systag','exp_joeross@ldrspine.com','X16XXXjoeross@ldrspine.com','exp_john.broughton@medela.com','expagE_exp_john.broughton@mede','exp_ex_trjohn.gauger@coringrou','X41Xjohn.gauger@coringroup.com','X73Xjohn.gauger@coringroup.com','X75Xjohn.gotzon@aesculap.com','exp_ex_trjohn.gotzon@aesculap.','exp_ex_trjohn.love@aesculap.co','X95Xjohn.love@aesculap.com','X50XJohn.sparacio@smith-nephew.com','exp_john.sparacio@smith-nephew','expVRf_exp_john.sparacio@smith','exp_ex_trjohn@wintherix.com','X83Xjohn@wintherix.com','X55Xjong.lee@conformis.com','exp_ex_trjong.lee@conformis.co',
'exp_ex_trjon.peacock@sonosite.','X56XXXjon.peacock@sonosite.com','exp_jonwerner@acell.com','expk9a_exp_joseph.pizzurro@exa','joseph.pizzurro@exac.com',
'exp_ex_trjosh@3dmedicalconcept','X9Xjosh@3dmedicalconcepts.com','exp_ex_trjpetricek@biomimetics','X18XXXjpetricek@biomimetics.com','exp_jrgannon@shire.com','exp_jrgannon@shire.com19','exp_jenniferRichter','exp_jrichter','exp_jspurgeo@its.jnj.comexp','expgYq_exp_JSpurgeo@its.jnj.coexp','exp_janetstewart','exp_jstewart1@its.jnj.com8exp','exp_jeffreytolonen','exp_jeff.tolonen','X66Xjulian@pivotmedical.com','X44Xjulie@orthoworld.com','X97XXXjulie@orthoworld.com','X26XXXjulie@orthoworld.com','X45Xjulie.tracy@wmt.com','exp_julie.tracy@wmt.com','exp_justin.gjokaj','exp_justin.gjokaj@ferring.com','X21Xjwhite@wmt.com','exp_karl.erikson','X41XXXkathleendschaum@bellsouth.com','X37XXkathleendschaum@bellsouth.com','kaytie.brown@baxsurg.com','X38Xkdieselman@k2m.com','user9','demospine','exp_kevin@biomedgps.com','exp_kevin.bolduc@stryker.com','expVVu_exp_kevin.bolduc@stryke','user8','k.e.v.i.nhicks@gmail.com','updates','exp_kevinhicks@mac.com35','exp_kgallo','exp_kgallo1@its.jnj.comexp','exp_kim.parkins@stryker.com77','X22Xkirk.grayam@kci1.com','exp_kirk.grayam@kci1.com','exp_kkinna','expyg5_exp_kkinna','exp_kmcgrat1@dpyus.jnj.comexp','exp_kmcgrat1@dpyuscom','exp_kplancher@plancherortho.co','X14Xktune@tm-partners.com72','X74Xktune@tm-partners.com','exp_ex_trkurt_t_johnson@baxter','kurt_t_johnson@baxter.com','exp_kvalentine@nuvasive.com67','exp_lauren.jordan@ferring.com3','X17XXXLayton@parcelllabs.com',
'exp_layton@parcelllabs.com','X29XXXlgarrett@rtix.com','X80XLindsey.Hall@arthrex.com','exp_Lindsey.Hall@arthrex.com','exp_lindsey.hall','exp_ex_trlisa-ann.underwood@kc','X58Xlisa-ann.underwood@kcc.com','X46Xlisa@biomedgps.com','exp_llapierre@gmail.com','exp_llehmull@its.jnj.comexp','exp_ex_trexpzwp_exp_llehmull@i','X58Xlora.fusco@medtronic.com',
'exp_ex_trlora.fusco@medtronic.','X87Xlora.fusco@medtronic.com','exp_exp_louie.vogtjr@zimmer.co','X12XXXuser10','X51XXXbrent','X22XXXchris','X88XXXuser14','X86XXXuser15','X65XXXuser16','exp_podonnell','X24Xglennhealey','X36XXXsteve','X82XXXstevestaff','X75XXXroyogle','X65XXXJSchiaparelli@cinci.rr.c','X23XXXphilkuhn','X24XXXtlangenderfer','X4XXXjamesclagett','X23XXXtrainer1','X12XXXkevinRTI','X23XXXrcarew2747','X40XXXgeoffreyfournie','X98XXXgaryvivian','X40XXXtracy','expsX9_exp_chelsea.boyte@acume','X61XXXjames.lavan','X36XXXjandeaton1','X51XXXdemoall','X8XXXchrisprime','X27XXXtomkonopka','X45XXXjesushernandez','X70XXXgeorgeayd','X57XXXuser1','X47XXXuser2','X64XXXuser3','X93XXXdmatus','exp_Linda.Smyth','X0Xmichelle.lamory','X85XXXEXPStryker','X80XXXuser4','X92XXXuser5','X24XXXuser6','X21XXXjdrost','X60XXXadamhayden','exp_terri.kapur','X1XXXlanemajor','X50XXXjimbapst','X96XXXtsmith','X58XXXjbelleville','X1XXXjbamis','X30XXXbmorgan','X51XXXgregkowalczyk','X9XXXrob.brown@biomedgps.com','X89XXXnickcotton','X52XXXbartgaskins','XXjbowden@hmpcommunications.','X35XXXchrisolig','X88XXXjohngauger','X88XXXyesisevilla','X22XXXbrian','X9XXXcarriestout','X51XXXalanhomer','exp_ex_trwboren@biomet.com','X18XXXemmitt','X64XXXericjania1','X72XXXsolomonnotik','X25XXXtodd','X10Xtracy.martellotta','expysH_exp_gcorraro@spinewave.','X72XXXAAOS','X82XXXrichard.lanigan','X59Xashleyp','X59Xneil.wintebottom','X89XXXJIMPAP','X88XXXJeff.Jenkins@plantemoran','X58XXXkevi.','X91XXXTrung_Pham','X97XXXmari','X45XXXwilliamplovanic','X52XXXlanemajor1','X2XXXcurtyocam','X85XXXjohnbrunelle','X96XXXjuliegustafson','X22XXXnancy','X59XXXjsarosy','X51XXXdanielomahony','X59Xashley.wohl','X86XXXjaylawson','X94XXXtoddharrington','exp_X75XXXlindsey','X95XXXkarengyongyosi','X60XXXkristineilaria','X24XXXpaul','X82XXXtest2','X73XXXspineview','X38XXXtmcleer','X62XXXjohnsmith','exp_exp_bill.benavitz@arthrex.','X11XXXjillschiaparelli','X86XXXmary annprunier','exp2E9_exp_matthew.abernethy@b','X56XXXtommytriallast','expw6G_exp_verena.harms@bsnmed','exp_exp_Email1',
'exp_pkrekel@eesus.jnj.comexp','exp7uz_exp_lindsay.ratterman@s','expjms_exp_yolanda.shepard@str','expq5E_exp_hwalthall@nutechmed','exp_exp_exp_lauren.venekas@str',
'expNuU_exp_verena.harms@bsnmed','expDQV_exp_ack@implantec.ch','X59XXXdavidcossaboon','expTQ7_exp_sstanton@globusmedi','X2XXXtrialuser','expC4C_exp_kevin@biomedgps.com',
'X1XXXgilpeterson','X8XXdwertzberger','X0XXXjandeaton','X97XXXstuharman','exp_Filippo.Secchi27','expCbV_exp_sstanton@globusmedi','X14XXlynn.tarkington','X87XXXnelsoncooke',
'X76Xtheresa.hanisko','X70Xkirt.stephenson','X44XXXcelestebrooks','X60XXXjeannevirca','X67XXXlucievan de steeg','X35Xhitoshi.mizuno','X55XXXrobevans','X77XXXpatrickgray','X70Xhector.torres','X22XXXjimhart','X77XXXtimnash',
'X3Xthien.doan@exac.com','X76XXXtom','X76XDIYAR.AMIN','X71Xjeffery.cole','X34Xrsmestad','X5Xfelix_pustilnik',
'X22XXXderekharper','X29Xjeanpierre.desmarais','X78XXXdanmurray','X27XXXrebeccamildrew','X92XMiguelFranco',
'X4Xlaura@noblescommunications.','X15XXXdaffyduck','exp_exp_Email','X2Xlaurie_lucarelli','X10Xtheresa.hanisko38',
'X17Xrichard.lanigan39','X17Xsteven.sanderson','X36Xguenther','XXJayHouserXXX','X93Xsjcadotte','X20Xsjcadotte4',
'X44Xdaniel.e.shoemaker','X97Xjamiemurdock','X58Xpamela','drew.josephson@arthrex.com','exp_Susan.paquette',
'exp_ex_trmarcv@vbllc.com','X70Xmarcv@vbllc.com','exp_mark.nemec','exp_Mark.Nemec@stryker.com','expJwU_exp_markrichards@ldrspi',
'exp_markrichards@ldrspine.com','exp_ex_trmatt.federico@bsnmedi','X54Xmatt.federico@bsnmedical.com',
'X85Xmatt.federico@bsnmedical.com','exp_ex_trexppB4_exp_matthew.ab','exp_matthew.abernethy@biomet.c',
'exp_ex_trmatthew.dodds@citi.co','X10Xmatthew.dodds@citi.com','X94Xmbutler@lifespine.com','exp_mcgill.logan@synthes.com92',
'exp_ex_trmcouncil@medicalmetri','X54Xmcouncil@medicalmetrics.com','exp_mdeschne@gmail.com','exp_mdeschne@gmail.com32','exp_megan.larch@stryker.com54',
'exp_melissa.troop@ferring.com','X84Xmembery@invibio.com','X71Xmfrancois@alphatecspine.com','X31XXXm.hall@biocrossroads.com','X41Xm.hall@biocrossroads.com','exp_ex_trm.hall@biocrossroads.',
'exp_mhanes@dpyus.jnjexp','exp_mhanes@dpyus.jnj.comexp','exp_mhanzo@shire.com','exp_ex_trexpZzA_exp_mhanzo@shi','exp_michaelbaur@me.com','expkmm_exp_michaelbaur@me.com',
'exp_michael.karnes@arthrex.com','Michael.Karnes@arthrex.com','exp_michael.karnes','X36Xmichael.szachta@zimmer.com','X12Xmichael.warman@mesoblast.c','exp_ex_trmichael.warman@mesobl',
'michellegammon@orthofix.com','expcfz_exp_mike.hanlin@exac.co','Mike.hanlin@exac.com','X70Xmike.p.smith@me.com','X5Xmike.p.smith@me.com','X63Xmmusick@customspine.com',
'exp_ex_trmmusick@customspine.c','exp_ex_trmrowe@imeconcepts.com','X36Xmrowe@imeconcepts.com','X50XXXMScanlo3@its.jnj.comexp','exp_mscanlo3@its.jnj.comexp','exp_ex_trmsherman@kenseynash.c',
'X38Xmsherman@kenseynash.com','exp_mstead@steadmed.com','exp_mtalley@wmt.com','exp_ex_trmuschlg@ccf.org','X42XXmuschlg@ccf.org','nancy@picdexcellence.com',
'exp_nicole.westin@stryker.com6','X93XXXnkdieselman@hotmail.com','X87Xnkdieselman@hotmail.com','exp_ex_trnness@allosource.org','exp_ex_troded.eshel@exac.com','X30Xoded.eshel@exac.com',
'exp_paulbryant','exp_Paul','X3XXXsue','exp_paul.maccini@ferring.com','X50Xpernilla@woodwelding.com','X96Xpernilla@woodwelding.com','exp_peter.denove@arthrex.com25','Peter.Denove@Arthrex.com','X9XPeter.Denove@arthrex.com','exp_peter.denove','exp_peter.vansyckle98','exp_phil','X24Xphilkuhn@orthofix.com','exp_Phil@Mundymed.com41','exp_Phil@Mundymed.com52','exp_Phil@Mundymed.com','X96Xp.lempereur@spineguard.com','X50Xp.lempereur@spineguard.com','exp_pmcnees@kirchnergroup.com','exp_pmenon','exp_ex_trexpGfX_exp_pmiles@nuv','exp_ex_trpreynolds@spinewave.c','exp_preynolds@spinewave.com66','exp_ptodonnell@comcast.net','exp_ptodonnell','exp_qblackford@nuvasive.com83','exp_qblackford@nuvasive.com24','exp_randy.sessler','raylinovitz@orthofix.com','exp_ex_trrgreiber@lifespine.co','X55Xrgreiber@lifespine.com','X15XXXRhershman@mac.com','X21XXXRhershman@mac.com','exp_randyh','rick.andrews@exac.com','exp_rick.andrews@exac.com','exp_rick.swaim@wright.com','rj.choinski@arthrex.com','exp_rj.choinski@arthrex.com','exp_rj.choinski','expKEu_exp_rkilburn@dpyus.jnj.exp','exp_rkilburn@dpyus.jnj.comexp','X70Xrobyn.whalen@molnlycke.com','X50Xrodger.fedigan@osseon.com','exp_ex_trrodger.fedigan@osseon','X77Xrodney.lanctot@medtronic.com','exp_ex_trrodney.lanctot@medtro','exp_ronald_vitales@baxter.com','X78XXXrose.vella@ppdi.com','exp_ex_trrose.vella@ppdi.com','exp_ex_trrpowers@nuvasive.com','X57Xrpowers@nuvasive.com','exp_ex_trrrusso@endomedix.com','X42Xrrusso@endomedix.com','expTBZ_exp_rsavage@mimedx.com','exp_RSinha','X2XXrsinha@organo.com','rspencer@mimedx.com','exp_rspencer@mimedx.com','exp_russ.a.johnson','exp_ryan.marshall@arthrex.com5','Ryan.Marshall@arthrex.com','exp_Sabrina.McDaniell@aesculap','X56Xsahin_soysal@yahoo.com',
'X42Xsahin_soysal@yahoo.com','exp_sascha.haas@de.lrmed.com','expCe4_exp_sascha.haas@de.lrme','exp_chrisscholl','X11Xsdoyle@etexcorp.com54','X89Xsdoyle@etexcorp.com','exp_sdthomas@mmm.com','exp_ex_trexpJUP_exp_sdthomas@m','X61Xsdthomas@mmm.com','X53Xsean.luland@integralife.com','X16Xsergiof@webmpt.com','exp_ex_trsergiof@webmpt.com','X39Xsfox@deroyal.com','exp_ex_trsfox@deroyal.com','X16Xshah@domainvc.com','exp_ex_trshah@domainvc.com','exp_shannon.cummings@wmt.com','exp_shawn.kroll@stryker.com','exp_sheldon.matt@synthes.com','exp_sheldon.matt','X88Xshon.steger@biomet.com','X11Xshon.steger@biomet.com32','exp_ex_trshon.steger@biomet.co','exp_ex_trshuiwong@sbcglobal.ne','X85Xshuiwong@sbcglobal.net','exp_shung','expLxu_exp_shung','exp_singhatat.wamis','exp_singhatat.wamis@synthes.co','exp_siravo.mark','exp_siravo.mark@synthes.com','exp_SLynch','X12XEXPslynch@vertiflexspine.c','exp_ex_trsnair@affinergy.com','X53Xsnair@affinergy.com','X1XXXsoeds@soelim.com','X67Xsoeds@soelim.com','X45Xsoeds@soelim.com','X68Xsoeds@soelim.com','X47XSpinegent@comcast.net','X80XSpinegent@comcast.net','exp_randyryan1','X80XXXsryan004@comcast.net','X63Xssevick@yahoo.com','exp_ex_trexpdKu_exp_sstanton@g','exp_sstanton@globusmedical.com','expUya_exp_sstanton@globusmedi','exp_stephen_czick@baxter.com',
'exp_ex_trstephenfloe@yahoo.com','X22XXstephenfloe@yahoo.com','exp_steve.atlay','X12XXXsteve.buldiger@biomet.co','exp_ex_trsteve.buldiger@biomet','steve.szabo@exac.com','exp_ex_trsteve.szabo@exac.com','X79Xsusan.a.sexton@medtronic.com','X95Xsusan.a.sexton@medtronic.com',
'exp_susan@biomedgps.com','X38Xsusan@spinalmodulation.com','X96Xsusan@spinalmodulation.com','exp_ex_trsylvias@vilex.com','X30XXXsylvias@vilex.com','X79Xtalleman@deroyal.com','X50Xtalleman@deroyal.com','exp_ex_trtcahill@tornier.com','X62Xtcahill@tornier.com','exp_tczartos@dpyus.jnj.comexp',
'exp_tczartos','exp_terri_riley@mtf.org','X35XXXterri_riley@mtf.org','expYR3_exp_tflynn2@dpyus.jnj.cexp','exp_tflynn2@dpyus.jnj.comexp','exp_tforney@dpyus.jnj.comexp',
'expp6G_exp_tforney@dpyus.jnj.cexp','exp_ex_trtfournier@neurotherm.','X80XXXtfournier@neurotherm.com','X9XXtgarcia@makosurgical.com','exp_ex_trtgarcia@makosurgical.','exp_mini','X64Xthomas.hur@aesculap.com',
'exp_ex_trthomas.hur@aesculap.c','X95Xtimothy.davidson@genzyme.com','exp_ex_trtimothy.davidson@genz','X12Xtliska@alphatecspine.com82','X28Xtliska@alphatecspine.com','X55Xtliska@alphatecspine.com','X92Xtliska@alphatecspine.com','exp_todd.earhart@arthrex.com',
'exp_todd.earhart','X39XXXtroueche@biosetinc.com','exp_ttucker@mimedx.com','exp_tyler.palmer','expkgN_exp_tyler.palmer','exp_ulrike.bitzer@hartmann.inf','X55Xverena.harms@bsnmedical.com','exp_verena.harms@bsnmedical.co','X74XXXverena.harms@bsnmedical.com',
'exp_victoria.ball@hollister.co','exp_wehrli.miriam','exp_wehrli.miriam@synthes.com','X41XXXwgitt@k2m.com','X51Xwgitt@k2m.com','X13Xwgitt@k2m.com49','X21Xwona.smith@olympusbiotech.com',
'exp_ylisda@its.jnj.comexp','exp_ylisda@its.jnj.com18exp','exp_ex_trymonovoukas@teibio.co','exp_elmarzubriggen','X50XXXstephanseiler','X67XXXianellis','X65XXXalanhorner','X6XXXnickpsaltos');


--load user records -- Java process
get details from Dave.

select * from custom.biomedgps_account;
-- replace owner_profile_id with a valid PROFILE_ID
/* replaced below
UPDATE custom.biomedgps_account SET owner_profile_id = b.wc_profile_id 
from biomedgps.profiles_user b
WHERE b.id=cast(custom.biomedgps_account.owner_profile_id as int);
*/
-- replace owner_profile_id with a valid PROFILE_ID
UPDATE custom.biomedgps_account SET owner_profile_id = b.profile_id 
from custom.biomedgps_user b
WHERE b.user_id=custom.biomedgps_account.owner_profile_id;

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

-- promote status=Staff to role=Staff
update profile_role a set role_id='3eef678eb39e87277f000101dfd4f140'
from custom.biomedgps_user b 
where a.profile_id=b.profile_id and a.site_id='BMG_SMARTTRAK_1' and b.status_cd='S';

*/

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
select a.id,team_id, a.user_id, CURRENT_TIMESTAMP from biomedgps.profiles_team_users a 
	inner join biomedgps.profiles_team b on a.team_id=b.id and b.account_id is not null
	inner join custom.biomedgps_user c on cast(a.user_id as varchar)=c.user_id;

-- flag account owners
update custom.biomedgps_user a set acct_owner_flg=1
from biomedgps.profiles_user_user_permissions b where permission_id=600 and a.user_id=cast(b.user_id as varchar);



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

--insert FD scenarios for teams
insert into custom.biomedgps_fd_scenario (scenario_id, user_id, team_id, scenario_nm, status_flg, refresh_dt, create_dt, update_dt)
select id, owner_id, team_id, name, upper(status), last_refresh, current_timestamp, last_changed  from biomedgps.financials_revenueoverlay
where cast(team_id as varchar) in (select team_id from custom.biomedgps_team);

--insert FD scenarios for users (private)
insert into custom.biomedgps_fd_scenario (scenario_id, user_id, team_id, scenario_nm, status_flg, refresh_dt, create_dt, update_dt)
select id, owner_id, null, name, upper(status), last_refresh, current_timestamp, last_changed  from biomedgps.financials_revenueoverlay
where cast(owner_id as varchar) in (select user_id from custom.biomedgps_user) and team_id=1; --1=no team

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
--per Skype note on 3/27, do not delete these 18 records anymore.  They no longer contain all zeros in the legacy data.
--delete from custom.biomedgps_fd_revenue where section_id = 'MASTER_35023';


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


-- notes


--company notes - for teams we imported
insert into custom.biomedgps_note (note_id, user_id, team_id, note_nm, note_txt, file_path_txt, expiration_dt, create_dt, update_dt, company_id)
select id, creator_id, team_id, title, content, attachment, expiration, create_date, last_update, object_id from biomedgps.notes_note
where content_type_id=8 and cast(team_id as varchar) in (select team_id from custom.biomedgps_team)
and cast(creator_id as varchar) in (select user_id from custom.biomedgps_user);

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
('LVL1_LINK',null,'Link',1,'LINK',1,current_timestamp),
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
where content_type_id=16 and cast(team_id as varchar) in (select team_id from custom.biomedgps_team)
and cast(creator_id as varchar) in (select user_id from custom.biomedgps_user);

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
last_update,object_id, attribute_id from notestemp where cast(creator_id as varchar) in (select user_id from custom.biomedgps_user);


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

update custom.biomedgps_regulatory_status set STATUS_TXT='DISCONTINUED' where status_id in ('10','21','2','42');
update custom.biomedgps_regulatory_status set STATUS_TXT='APPROVED' where status_id in ('9','12','13','14','43');
update custom.biomedgps_regulatory_status set STATUS_TXT='IN_DEVELOPMENT' 
	where status_id in ('5','6','7','8','11','15','17','18','22','23','24','25','27','28','29','30','31','32','33','34','35','36','37','40');

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



--- fix market->section bindings.  Mike created these manually:
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_10815','6559',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_18689','6529',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_278','479',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_353','6584',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_21221','6588',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_32111','6603',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_34268','6619',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_34268','6620',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_34419','6621',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_29347','6623',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_32259','6639',CURRENT_TIMESTAMP);
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
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_338','6582',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_326','6590',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_21405','6599',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_32259','6604',CURRENT_TIMESTAMP);
insert into custom.biomedgps_market_section (market_section_xr_id, section_id, market_id, create_dt) values (replace(newid(),'-',''),'MASTER_37874','6629',CURRENT_TIMESTAMP);

--report markets not tied to sections to Mike:
select * from custom.biomedgps_market a left join custom.biomedgps_market_section b on a.market_id=b.market_id where b.market_id is null order by a.market_id;


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

-- updates not attached to companies, products, or markets - bind to Kevin Hicks since we don't otherwise know who created them
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


--update Update titles tied to companies having the company_nm in the title
update custom.biomedgps_update 
set title_txt=regexp_replace(title_txt,'^'+c.company_nm,'')
from custom.biomedgps_company c
where custom.biomedgps_update.company_id=c.company_id

--update Update titles tied to products having the company_nm in the title
update custom.biomedgps_update 
set title_txt=regexp_replace(title_txt,'^'+c.company_nm,'')
from custom.biomedgps_company c
inner join custom.biomedgps_product d on d.company_id=c.company_id
where custom.biomedgps_update.product_id=d.product_id;



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

--user Mkt flag
update custom.biomedgps_user as a set mkt_auth_flg=1
from biomedgps.profiles_user_user_permissions b where cast(b.user_id as varchar)=a.user_id and b.permission_id=642;


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


--grandfather in some test accounts to have market access - should be 18
update custom.biomedgps_account 
set mkt_auth_flg=1
where account_nm in ('ACME','BioMedGPS, LLC','Byline Only','C. Blakely','Demo, LLC',
'DemoAll','DemoSpine','DemoWound','Eric Jania','EU Demo','EU Tim Jeavons','Kathleen Schaum','Kevin''s Accounts','Neuro Markets','SmartTRAK-Complimentary','Steve Pinto','Thea Accounts');

-- account notes
insert into custom.biomedgps_note (note_id, user_id, account_id, note_nm, note_txt, file_path_txt, expiration_dt, create_dt, update_dt)
select id, creator_id, object_id, title, content, attachment, expiration, create_date, last_update from biomedgps.notes_note
where content_type_id=95 and cast(object_id as varchar) in (select account_id from custom.biomedgps_account);


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
select 'BMG_SMARTTRAK_'+cast(id as varchar), 'BMG_SMARTTRAK_1', b.profile_id, session, '270d08c0adcfac1ec0a8024a750a1b36','/search', 
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


-- company audit data
insert into custom.biomedgps_audit_log (audit_log_id, company_id, auditor_profile_id, status_cd, start_dt, complete_dt, update_dt)
select a.id, a.object_id, b.profile_id, upper(a.status), a.start_date, a.end_date,coalesce(a.end_date, a.start_date, current_timestamp) 
from biomedgps.audit_audit a
inner join custom.biomedgps_user b on cast(a.auditor_id as varchar)=b.user_id
inner join custom.biomedgps_company c on cast(a.object_id as varchar)=c.company_id
where a.content_type_id=8;
--check for any other audit types, there shouldn't be any though;
select * from biomedgps.audit_audit where content_type_id != 8;


--delete the VascularGPS section, hierarchically
delete from custom.biomedgps_section where section_id='MASTER_34117';


--REQ_FOR_SORTING alliances and alliance type:
DELETE from custom.biomedgps_product_alliance_XR where alliance_type_id = 'PROD_6';
DELETE from custom.biomedgps_alliance_type where alliance_type_id = 'PROD_6';

--insert solr synonyms
insert into core.solr_synonym (solr_synonym_id, solr_collection_id, left_term, synonym_list, index_flg, create_dt)
select replace(newid(),'-',''),'SB_COLLECTION', null,phrase1+','+string_agg(phrase2,','),1,current_timestamp from biomedgps.search_synonym
group by phrase1;

--delete duplicate product attributes after flattening their hierarchcal structure
delete from custom.biomedgps_product_attribute_xr a
where a.ctid in (select min(ctid) from custom.biomedgps_product_attribute_xr b 
	where length(title_txt) > 0 and length(value_txt)>0
	group by product_id, title_txt, value_txt, create_dt having count(*) > 1);



-- NOTE FILES --
-- rename some linux files:
mv users/timreed/OFIX_Non-deal_Road_ShowTake-aways_-_William_Plovanic_-_Canaccord_09082011.pdf users/timreed/Canaccord_09082011.pdf
mv users/aura/CBS-2013-Summary-of-Findings-Costs-of-Importation-of-Musculoskeletal-Allografts-and-Acellular-Dermal-Matrix_1.pdf users/aura/Acellular-Dermal-Matrix_1.pdf
mv users/aura/CBS-2013-Summary-of-Findings-Costs-of-Importation-of-Musculoskeletal-Allografts-and-Acellular-Dermal-Matrix_1_2.pdf users/aura/Acellular-Dermal-Matrix_1_2.pdf
mv users/aura/CBS-2013-Summary-of-Findings-Costs-of-Importation-of-Musculoskeletal-Allografts-and-Acellular-Dermal-Matrix_1_1.pdf users/aura/Acellular-Dermal-Matrix_1_1.pdf
mv users/user7/A-SmartTRAK_is_the_first_Business_Intelligence_Hub_for_the_life_science_industry_1.docx users/user7/life_science_industry_1.docx
mv users/KRusnack@Organo.com/OAS-90DayGlobal-Release_D1R-Client-13JAN12_4_-_FINAL__APPROVED_FOR_DISTRIBUTION_25jan12.pdf users/KRusnack@Organo.com/FINAL__APPROVED_FOR_DISTRIBUTION_25jan12.pdf 

-- flush values with no file extension.
update custom.biomedgps_note set file_path_txt=null where strpos(file_path_txt,'.') < 1;
--rename the files we moved (in linux) above
update custom.biomedgps_note set file_path_txt=replace(file_path_txt,'OFIX_Non-deal_Road_ShowTake-aways_-_William_Plovanic_-_Canaccord_09082011.pdf','Canaccord_09082011.pdf') where file_path_txt like '%OFIX_Non-deal_Road_ShowTake-aways_-_William_Plovanic_-_Canaccord_09082011.pdf';
update custom.biomedgps_note set file_path_txt=replace(file_path_txt,'CBS-2013-Summary-of-Findings-Costs-of-Importation-of-Musculoskeletal-Allografts-and-Acellular-Dermal-Matrix_1.pdf','Acellular-Dermal-Matrix_1.pdf') where file_path_txt like '%CBS-2013-Summary-of-Findings-Costs-of-Importation-of-Musculoskeletal-Allografts-and-Acellular-Dermal-Matrix_1.pdf';
update custom.biomedgps_note set file_path_txt=replace(file_path_txt,'CBS-2013-Summary-of-Findings-Costs-of-Importation-of-Musculoskeletal-Allografts-and-Acellular-Dermal-Matrix_1_2.pdf','Acellular-Dermal-Matrix_1_2.pdf') where file_path_txt like '%CBS-2013-Summary-of-Findings-Costs-of-Importation-of-Musculoskeletal-Allografts-and-Acellular-Dermal-Matrix_1_2.pdf';
update custom.biomedgps_note set file_path_txt=replace(file_path_txt,'CBS-2013-Summary-of-Findings-Costs-of-Importation-of-Musculoskeletal-Allografts-and-Acellular-Dermal-Matrix_1_1.pdf','Acellular-Dermal-Matrix_1_1.pdf') where file_path_txt like '%CBS-2013-Summary-of-Findings-Costs-of-Importation-of-Musculoskeletal-Allografts-and-Acellular-Dermal-Matrix_1_1.pdf';
update custom.biomedgps_note set file_path_txt=replace(file_path_txt,'A-SmartTRAK_is_the_first_Business_Intelligence_Hub_for_the_life_science_industry_1.docx','life_science_industry_1.docx') where file_path_txt like '%A-SmartTRAK_is_the_first_Business_Intelligence_Hub_for_the_life_science_industry_1.docx';

update custom.biomedgps_note set file_path_txt=replace(file_path_txt,'OAS-90DayGlobal-Release_D1R-Client-13JAN12_4_-_FINAL__APPROVED_FOR_DISTRIBUTION_25jan12.pdf','FINAL__APPROVED_FOR_DISTRIBUTION_25jan12.pdf') where file_path_txt like '%FINAL__APPROVED_FOR_DISTRIBUTION_25jan12.pdf';

--check for lengths > 97 chars - if found, move the file ("mv") and run an above update query for it.
select length(file_path_txt),file_path_txt from custom.biomedgps_note 
where file_path_txt is not null and length(file_path_txt) > 97 order by length(file_path_txt) desc;


--create a temp table with our separated file name and path
create temporary table note_temp as select (regexp_split_to_array(file_path_txt,'/'))[array_upper((regexp_split_to_array(file_path_txt,'/')),1)] as file_nm, 
(regexp_split_to_array(file_path_txt,'\.'))[array_upper((regexp_split_to_array(file_path_txt,'\.')),1)] as ext, * 
from custom.biomedgps_note where file_path_txt is not null;

--ensure we have all file types accounted for:
select * from note_temp a left join file_type b on lower(a.ext)=b.file_type_cd where b.file_type_cd is null;

-- insert the note files into profile_document's reponsitory, which is what serves them to the browser
insert into profile_document (profile_document_id, action_id, feature_id, organization_id, profile_id, file_nm, file_type_cd, file_path_url, create_dt)
select replace(newid(),'-',''),'86081535ab51f7afc0a8024a38334aaf',a.note_id,'BMG_SMARTTRAK',b.profile_id,
replace(a.file_nm,'.'+ext,'')+'--'+a.file_nm, lower(ext), '/'+replace(a.file_path_txt,a.file_nm,''), a.create_dt 
from note_temp a inner join custom.biomedgps_user b on a.user_id=b.user_id where a.file_path_txt is not null;

--drop temp table
drop table note_temp;

-- insight images (append to left column)

--cleanup some typos in image names before we import them into insights
update biomedgps.images_image set name=replace(initcap(name),'Us','US') where name like 'FIgure%';
update biomedgps.images_image set name='Figure 1' where name='Figure !';
/**
--if we're expirimenting, create a backup of the table
create table custom.biomedgps_insight2 as select * from custom.biomedgps_insight;

--query to restore a record from the backup:
update custom.biomedgps_insight a
set content_txt=b.content_txt
from custom.biomedgps_insight2 b
where a.insight_id=b.insight_id and b.insight_id='173';
*/


-- ATTACHMENTS --

/*
--attachments in the various sections
select content_type_id, count(*), b.name, max(a.object_id)
from biomedgps.attachments_attachment a
join biomedgps.django_content_type b on a.content_type_id=b.id
group by content_type_id,b.name;
*/

-- market attachments

--create the attribute for attachments:
insert into custom.biomedgps_market_attribute (attribute_id, parent_id, attribute_nm, active_flg, order_no, type_cd, create_dt) values ('LVL1_ATTACH',null,'Attachments',1,1,'ATTACH',current_timestamp);
--insert the data
insert into custom.biomedgps_market_attribute_xr (market_attribute_id, market_id, 
attribute_id, create_dt, order_no, status_no, title_txt, value_txt, value_1_txt)
select cast(object_id as varchar)+'_ATTACH_'+cast(id as varchar),object_id,'LVL1_ATTACH',
last_update,"order",'P',name,'/secBinary/org/BMG_SMARTTRAK/'+attachment, description
from biomedgps.attachments_attachment where content_type_id=62 and length(name) > 0;


--company attachments

--getters to verify inserts
select * from custom.biomedgps_company_attribute where attribute_id like '%ATT%';
select * from custom.biomedgps_company_attribute_xr where attribute_id like '%ATT%';

--create the attachment root node
insert into custom.biomedgps_company_attribute (attribute_id, parent_id, attribute_nm, active_flg, display_order_no, type_nm, create_dt) 
values ('LVL1_ATTACH',null,'Attachments',1,1,'ATTACH',current_timestamp);

--create the attachments 'archives' lvl2 node
insert into custom.biomedgps_company_attribute (attribute_id, parent_id, attribute_nm, active_flg, display_order_no, type_nm, create_dt) 
values ('LVL2_ATTACH_ARCH','LVL1_ATTACH','Archived Attachments',1,1,'ATTACH',current_timestamp);

--create the attachments archives sub-folders based on the data we need to store
insert into custom.biomedgps_company_attribute (attribute_id, parent_id, attribute_nm, active_flg, display_order_no, type_nm, create_dt) 
select distinct 'LVL3_ATTACH_ARCH_'+cast(archive_folder as varchar),'LVL2_ATTACH_ARCH',archive_folder,1,
abs(2020-cast(archive_folder as integer)),'ATTACH',current_timestamp 
from biomedgps.attachments_attachment where content_type_id=8 and length(archive_folder) > 0 and is_archived=1
group by archive_folder order by archive_folder;

--create the attachments archives 'misc' lvl3 node
insert into custom.biomedgps_company_attribute (attribute_id, parent_id, attribute_nm, active_flg, display_order_no, type_nm, create_dt) 
values ('LVL3_ATTACH_MISC','LVL2_ATTACH_ARCH','Misc.',1,50,'ATTACH',current_timestamp);

--these are the records we need to insert:
select * from biomedgps.attachments_attachment where content_type_id=8 and length(name) > 0;

-- insert top level company attachments
insert into custom.biomedgps_company_attribute_xr (company_attribute_id, company_id, attribute_id, create_dt, order_no, status_no, title_txt, value_txt, alt_title_txt)
select cast(object_id as varchar)+'_ATTACH_'+cast(id as varchar),object_id,'LVL1_ATTACH',last_update,"order",'P',name,'/secBinary/org/BMG_SMARTTRAK/'+attachment,description 
from biomedgps.attachments_attachment where content_type_id=8 and length(name) > 0 and is_archived=0;

-- insert 'misc' company attachments (no archive _folder)
insert into custom.biomedgps_company_attribute_xr (company_attribute_id, company_id, attribute_id, create_dt, order_no, status_no, title_txt, value_txt, alt_title_txt)
select cast(object_id as varchar)+'_ATTACH_'+cast(id as varchar),object_id,'LVL3_ATTACH_MISC',last_update,"order",'P',name,'/secBinary/org/BMG_SMARTTRAK/'+attachment,description 
from biomedgps.attachments_attachment where content_type_id=8 and length(name) > 0 and is_archived=1 and length(archive_folder) = 0;

--insert archived company attachments, using archive_folder
insert into custom.biomedgps_company_attribute_xr (company_attribute_id, company_id, attribute_id, create_dt, order_no, status_no, title_txt, value_txt, alt_title_txt)
select cast(object_id as varchar)+'_ATTACH_'+cast(id as varchar),object_id,'LVL3_ATTACH_ARCH_'+archive_folder,last_update,"order",'P',name,'/secBinary/org/BMG_SMARTTRAK/'+attachment,description 
from biomedgps.attachments_attachment where content_type_id=8 and length(name) > 0 and is_archived=1 and length(archive_folder) > 0;


--product attachments

--getters to verify inserts
select * from custom.biomedgps_product_attribute where attribute_id like '%ATT%';
select * from custom.biomedgps_product_attribute_xr where attribute_id like '%ATT%';

--create the attachment root node
insert into custom.biomedgps_product_attribute (attribute_id, parent_id, attribute_nm, active_flg, order_no, type_cd, create_dt) 
values ('LVL1_ATTACH',null,'Attachments',1,1,'ATTACH',current_timestamp);

--the records we need to insert:
select * from biomedgps.attachments_attachment where content_type_id=16 order by object_id;

-- insert top level product attachments
insert into custom.biomedgps_product_attribute_xr (product_attribute_id, product_id, attribute_id, create_dt, order_no, 
status_no, title_txt, value_txt, alt_title_txt)
select cast(object_id as varchar)+'_ATTACH_'+cast(id as varchar),object_id,'LVL1_ATTACH',last_update,"order",'P',
case when length(name)=0 then (regexp_split_to_array(attachment,'/'))[array_upper((regexp_split_to_array(attachment,'/')),1)] else name end,
'/secBinary/org/BMG_SMARTTRAK/'+attachment,description 
from biomedgps.attachments_attachment where content_type_id=16 and is_archived=0
and cast(object_id as varchar) in (select product_id from custom.biomedgps_product);

--be sure we didn't pick up some new records that are archived or nested
select * from biomedgps.attachments_attachment where content_type_id=16 and (is_archived=1 or length(archive_folder) > 0);


-- LINKS --

--market links

--getters to verify inserts
select * from custom.biomedgps_market_attribute where attribute_id like '%LINK%';
select * from custom.biomedgps_market_attribute_xr where attribute_id like '%LINK%';

--create the links root node
insert into custom.biomedgps_market_attribute (attribute_id, parent_id, attribute_nm, active_flg, order_no, type_cd, create_dt) 
values ('LVL1_LINK',null,'Links',1,1,'LINK',current_timestamp);

--create the links 'archives' lvl2 node
insert into custom.biomedgps_market_attribute (attribute_id, parent_id, attribute_nm, active_flg, order_no, type_cd, create_dt) 
values ('LVL2_LINK_ARCH','LVL1_LINK','Archived Links',1,1,'LINK',current_timestamp);

--create the links archives sub-folders based on the data we need to store
insert into custom.biomedgps_market_attribute (attribute_id, parent_id, attribute_nm, active_flg, order_no, type_cd, create_dt) 
select distinct 'LVL3_LINK_ARCH_'+cast(archive_folder as varchar),'LVL2_LINK_ARCH',archive_folder,1,
abs(2020-cast(archive_folder as integer)),'LINK',current_timestamp 
from biomedgps.links_link where content_type_id=62 and length(archive_folder) > 0 and is_archived=1
group by archive_folder order by archive_folder;

--create the links archives 'misc' lvl3 node
insert into custom.biomedgps_market_attribute (attribute_id, parent_id, attribute_nm, active_flg, order_no, type_cd, create_dt) 
values ('LVL3_LINK_MISC','LVL2_LINK_ARCH','Misc.',1,50,'LINK',current_timestamp);

--these are the records we need to insert:
select b.linktype, a.*,b.* from biomedgps.links_link a inner join biomedgps.links_linktype b on a.linktype_id=b.id 
where content_type_id=62 order by object_id;

-- insert top level market links
insert into custom.biomedgps_market_attribute_xr (market_attribute_id, market_id, attribute_id, create_dt, order_no, status_no, title_txt, value_txt, value_1_txt)
select cast(object_id as varchar)+'_LINK_'+cast(a.id as varchar),object_id,'LVL1_LINK',last_check,sort_order,'P',
description,regexp_replace(url,'https://www.smarttrak.net/([a-z]+)/([0-9]+)/','/\1/qs/\2'),null 
from biomedgps.links_link a
inner join biomedgps.links_linktype b on a.linktype_id=b.id
where content_type_id=62 and is_archived=0 and cast(object_id as varchar) in (select market_id from custom.biomedgps_market);

-- insert 'misc' market links (no archive _folder)
insert into custom.biomedgps_market_attribute_xr (market_attribute_id, market_id, attribute_id, create_dt, order_no, status_no, title_txt, value_txt, value_1_txt)
select cast(object_id as varchar)+'_LINK_'+cast(a.id as varchar),object_id,'LVL3_LINK_MISC',last_check,sort_order,'P',
description,regexp_replace(url,'https://www.smarttrak.net/([a-z]+)/([0-9]+)/','/\1/qs/\2'),null 
from biomedgps.links_link a
inner join biomedgps.links_linktype b on a.linktype_id=b.id
where content_type_id=62 and is_archived=1 and length(archive_folder) = 0 and cast(object_id as varchar) in (select market_id from custom.biomedgps_market);

--insert archived market links, using archive_folder
insert into custom.biomedgps_market_attribute_xr (market_attribute_id, market_id, attribute_id, create_dt, order_no, status_no, title_txt, value_txt, value_1_txt)
select cast(object_id as varchar)+'_LINK_'+cast(a.id as varchar),object_id,'LVL3_LINK_ARCH_'+archive_folder,last_check,sort_order,'P',
description,regexp_replace(url,'https://www.smarttrak.net/([a-z]+)/([0-9]+)/','/\1/qs/\2'),null 
from biomedgps.links_link a
inner join biomedgps.links_linktype b on a.linktype_id=b.id
where content_type_id=62 and is_archived=1 and length(archive_folder) > 0 and cast(object_id as varchar) in (select market_id from custom.biomedgps_market);


--company links

--getters to verify inserts
select * from custom.biomedgps_company_attribute where attribute_id like '%LINK%';
select * from custom.biomedgps_company_attribute_xr where attribute_id like '%LINK%';

--create the links 'archives' lvl2 node
insert into custom.biomedgps_company_attribute (attribute_id, parent_id, attribute_nm, active_flg, display_order_no, type_nm, create_dt) 
values ('LVL2_LINK_ARCH','LVL1_LINK','Archived Links',1,1,'LINK',current_timestamp);

--create the links archives sub-folders based on the data we need to store
insert into custom.biomedgps_company_attribute (attribute_id, parent_id, attribute_nm, active_flg, display_order_no, type_nm, create_dt) 
select distinct 'LVL3_LINK_ARCH_'+cast(archive_folder as varchar),'LVL2_LINK_ARCH',archive_folder,1,
abs(2020-cast(archive_folder as integer)),'LINK',current_timestamp 
from biomedgps.links_link where content_type_id=8 and length(archive_folder) > 0 and is_archived=1
group by archive_folder order by archive_folder;

--create the links archives 'misc' lvl3 node
insert into custom.biomedgps_company_attribute (attribute_id, parent_id, attribute_nm, active_flg, display_order_no, type_nm, create_dt) 
values ('LVL3_LINK_MISC','LVL2_LINK_ARCH','Misc.',1,50,'LINK',current_timestamp);

--these are the records we need to insert:
select b.linktype, a.*,b.* from biomedgps.links_link a inner join biomedgps.links_linktype b on a.linktype_id=b.id 
where content_type_id=8 and object_id=9 order by object_id;

-- insert top level company links
insert into custom.biomedgps_company_attribute_xr (company_attribute_id, company_id, attribute_id, create_dt, order_no, status_no, title_txt, value_txt, alt_title_txt)
select cast(object_id as varchar)+'_LINK_'+cast(a.id as varchar),object_id,'LVL1_LINK',last_check,sort_order,'P',
linktype,url,case when description != b.linktype and length(description) > 0 then description else null end 
from biomedgps.links_link a
inner join biomedgps.links_linktype b on a.linktype_id=b.id
where content_type_id=8 and is_archived=0;

-- insert 'misc' company links (no archive _folder)
insert into custom.biomedgps_company_attribute_xr (company_attribute_id, company_id, attribute_id, create_dt, order_no, status_no, title_txt, value_txt, alt_title_txt)
select cast(object_id as varchar)+'_LINK_'+cast(a.id as varchar),object_id,'LVL3_LINK_MISC',last_check,sort_order,'P',
linktype,url,case when description != b.linktype and length(description) > 0 then description else null end 
from biomedgps.links_link a
inner join biomedgps.links_linktype b on a.linktype_id=b.id
where content_type_id=8 and is_archived=1 and length(archive_folder) = 0;

--insert archived company links, using archive_folder
insert into custom.biomedgps_company_attribute_xr (company_attribute_id, company_id, attribute_id, create_dt, order_no, status_no, title_txt, value_txt, alt_title_txt)
select cast(object_id as varchar)+'_LINK_'+cast(a.id as varchar),object_id,'LVL3_LINK_ARCH_'+archive_folder,last_check,sort_order,'P',
linktype,url,case when description != b.linktype and length(description) > 0 then description else null end 
from biomedgps.links_link a
inner join biomedgps.links_linktype b on a.linktype_id=b.id
where content_type_id=8 and is_archived=1 and length(archive_folder) > 0;


-- product links --

--getters to verify inserts
select * from custom.biomedgps_product_attribute where attribute_id like '%LINK%';
select * from custom.biomedgps_product_attribute_xr where attribute_id like '%LINK%';

--create the links 'archives' lvl2 node
insert into custom.biomedgps_product_attribute (attribute_id, parent_id, attribute_nm, active_flg, order_no, type_cd, create_dt) 
values ('LVL2_LINK_ARCH','LVL1_LINK','Archived Links',1,1,'LINK',current_timestamp);

--create the links archives sub-folders based on the data we need to store
insert into custom.biomedgps_product_attribute (attribute_id, parent_id, attribute_nm, active_flg, order_no, type_cd, create_dt) 
select distinct 'LVL3_LINK_ARCH_'+cast(archive_folder as varchar),'LVL2_LINK_ARCH',archive_folder,1,
abs(2020-cast(archive_folder as integer)),'LINK',current_timestamp 
from biomedgps.links_link where content_type_id=16 and length(archive_folder) > 0 and is_archived=1
group by archive_folder order by archive_folder;

--create the links archives 'misc' lvl3 node
insert into custom.biomedgps_product_attribute (attribute_id, parent_id, attribute_nm, active_flg, order_no, type_cd, create_dt) 
values ('LVL3_LINK_MISC','LVL2_LINK_ARCH','Misc.',1,50,'LINK',current_timestamp);

--these are the records we need to insert:
select b.linktype, a.*,b.* from biomedgps.links_link a inner join biomedgps.links_linktype b on a.linktype_id=b.id 
where content_type_id=16 and object_id=110487 order by object_id;

-- insert top level product links
insert into custom.biomedgps_product_attribute_xr (product_attribute_id, product_id, attribute_id, create_dt, order_no, status_no, title_txt, value_txt, alt_title_txt)
select cast(object_id as varchar)+'_LINK_'+cast(a.id as varchar),object_id,'LVL1_LINK',last_check,sort_order,'P',
linktype,url,case when description != b.linktype and length(description) > 0 then description else null end 
from biomedgps.links_link a
inner join biomedgps.links_linktype b on a.linktype_id=b.id
where content_type_id=16 and is_archived=0 and cast(object_id as varchar) in (select product_id from custom.biomedgps_product);

-- insert 'misc' product links (no archive _folder)
insert into custom.biomedgps_product_attribute_xr (product_attribute_id, product_id, attribute_id, create_dt, order_no, status_no, title_txt, value_txt, alt_title_txt)
select cast(object_id as varchar)+'_LINK_'+cast(a.id as varchar),object_id,'LVL3_LINK_MISC',last_check,sort_order,'P',
linktype,url,case when description != b.linktype and length(description) > 0 then description else null end 
from biomedgps.links_link a
inner join biomedgps.links_linktype b on a.linktype_id=b.id
where content_type_id=16 and is_archived=1 and length(archive_folder) = 0 and cast(object_id as varchar) in (select product_id from custom.biomedgps_product);

--insert archived product links, using archive_folder
insert into custom.biomedgps_product_attribute_xr (product_attribute_id, product_id, attribute_id, create_dt, order_no, status_no, title_txt, value_txt, alt_title_txt)
select cast(object_id as varchar)+'_LINK_'+cast(a.id as varchar),object_id,'LVL3_LINK_ARCH_'+archive_folder,last_check,sort_order,'P',
linktype,url,case when description != b.linktype and length(description) > 0 then description else null end 
from biomedgps.links_link a
inner join biomedgps.links_linktype b on a.linktype_id=b.id
where content_type_id=16 and is_archived=1 and length(archive_folder) > 0 and cast(object_id as varchar) in (select product_id from custom.biomedgps_product);


-- support tickets --

--delete from core.support_ticket where ticket_id like 'SMARTTRAK_%';
--delete from core.support_activity where ticket_id like 'SMARTTRAK_%';

-- insert 90 days worth of support tickets
insert into core.support_ticket (ticket_id, ticket_no, organization_id, reporter_id, assigned_id, 
status_cd, notify_flg, desc_txt, referrer_url, create_dt, update_dt)
select 'SMARTTRAK_'+cast(id as varchar),id,'BMG_SMARTTRAK',b.profile_id,c.profile_id,
case when status='resolved' then 3 
	 when status='assigned' then 1 
	 when status='ongoing' then 5 
	 else 0 end,
0,description,regexp_replace(page,'^https?://(www\.)?smarttrak\.net',''),incident_date,last_update
from biomedgps.help_ticket a
inner join custom.biomedgps_user b on cast(a.creator_id as varchar)=b.user_id
left join custom.biomedgps_user c on cast(a.assigned_to_id as varchar)=c.user_id
where create_date > (current_date - interval '90 days');

-- insert ledger entries from the old system - for the tickets we imported
insert into core.support_activity (activity_id, ticket_id, profile_id, desc_txt, create_dt)
select 'SMARTTRAK_'+cast(ticket_id as varchar)+'_'+cast(id as varchar),'SMARTTRAK_'+cast(ticket_id as varchar),b.profile_id,entry,a.created
from biomedgps.help_ticketlog a
inner join custom.biomedgps_user b on cast(a.owner_id as varchar)=b.user_id
where 'SMARTTRAK_'+cast(ticket_id as varchar) in (select ticket_id from core.support_ticket);

--create temp table of ticket activities
create temporary table maxactivity as select max(activity_id) as id,ticket_id from core.support_activity group by ticket_id;
select * from maxactivity;

-- insert a ledger entry for ticket assignment for tickets that don't have at least one activity already
insert into core.support_activity (activity_id, ticket_id, profile_id, desc_txt, create_dt)
select 'SMARTTRAK_'+cast(a.id as varchar)+'_1','SMARTTRAK_'+cast(id as varchar),b.profile_id,'Ticket Assigned',a.create_date
from biomedgps.help_ticket a
inner join custom.biomedgps_user b on cast(a.assigned_to_id as varchar)=b.user_id
where a.assigned_to_id is not null and 'SMARTTRAK_'+cast(id as varchar) not in (select ticket_id from maxactivity)
and 'SMARTTRAK_'+cast(a.id as varchar) in (select ticket_id from core.support_ticket);

--drop the temp table
drop table maxactivity;


--move minutes/cost values from the ticket over to the last activity on the ticket
create temporary table maxactivity as select max(activity_id) as id,ticket_id from core.support_activity group by ticket_id;
select * from maxactivity where ticket_id='SMARTTRAK_1108';

update core.support_activity a
set cost_no=b.additional_costs, effort_no=case when b.time_worked_value = 0 then b.minutes_worked else b.time_worked_value end
from biomedgps.help_ticket b
inner join maxactivity c on 'SMARTTRAK_'+cast(b.id as varchar)=c.ticket_id
where c.id=a.activity_id;

--drop the temp table
drop table maxactivity;

--insert staff comments as internal activity ledger entries
insert into core.support_activity (activity_id, ticket_id, profile_id, desc_txt, internal_flg,create_dt)
select 'SMARTTRAK_'+cast(ticket_id as varchar)+'_int_'+cast(id as varchar),'SMARTTRAK_'+cast(ticket_id as varchar),b.profile_id,staff_comments,1,a.created
from biomedgps.help_ticketlog a
inner join custom.biomedgps_user b on cast(a.owner_id as varchar)=b.user_id
where length(staff_comments)>0 and 'SMARTTRAK_'+cast(ticket_id as varchar) in (select ticket_id from core.support_ticket);

-- ticket attachments 

--create a temp table with our separated file name and path
create temporary table attach_temp as select (regexp_split_to_array(attachment,'/'))[array_upper((regexp_split_to_array(attachment,'/')),1)] as file_nm, 
(regexp_split_to_array(attachment,'\.'))[array_upper((regexp_split_to_array(attachment,'\.')),1)] as ext, * 
from biomedgps.attachments_attachment where length(attachment)>0 and content_type_id=65;

--ensure we have all file types accounted for:
select * from attach_temp a left join file_type b on lower(a.ext)=b.file_type_cd where b.file_type_cd is null;
--find ticket action
select * from sb_action where module_type_id='BMG_TICKET';

--insert ticket attachments into the profile_document table
insert into profile_document (profile_document_id, action_id, feature_id, organization_id, profile_id, file_nm, file_type_cd, file_path_url, create_dt)
select 'TKT_'+cast(object_id as varchar)+'_'+cast(id as varchar),'12e9b159ab5164ffc0a8024a2b9a26e3','SMARTTRAK_'+cast(object_id as varchar),'BMG_SMARTTRAK',
b.profile_id, replace(a.file_nm,'.'+ext,'')+'--'+a.file_nm, lower(ext), '/'+replace(a.attachment,a.file_nm,''), a.last_update 
from attach_temp a
inner join custom.biomedgps_user b on cast(a.owner_id as varchar)=b.user_id
where 'SMARTTRAK_'+cast(object_id as varchar) in (select ticket_id from core.support_ticket);


-- create a binder between ticket attachments and the ticket
insert into core.support_attachment_xr (attachment_xr_id, ticket_id, profile_document_id, create_dt,desc_txt)
select replace(newid(),'-',''),'SMARTTRAK_'+cast(object_id as varchar),'TKT_'+cast(object_id as varchar)+'_'+cast(id as varchar),a.last_update,case when length(a.name)>0 then a.name else a.file_nm end
from attach_temp a inner join custom.biomedgps_user b on cast(a.owner_id as varchar)=b.user_id
where 'SMARTTRAK_'+cast(object_id as varchar) in (select ticket_id from core.support_ticket);

drop table attach_temp;



--fix overlapping product attributes - per Eric

--awaiting new queries



--run GRID_04132017_1155.sql

--ryan fix for profile_document file names:
-- updates historical file names to match new notes db storage patterns.  
UPDATE profile_document pd
SET file_nm = i.newFileNm, file_path_url = i.newFilePath 
FROM (
    select 
    profile_document_id, 
	substring(file_nm from (position('--' in file_nm)+2))as newFileNm,
	file_path_url + substring(file_nm from (position('--' in file_nm)+2)) as newFilePath
	from profile_document
	where file_nm like '%--%'
	) i
WHERE i.profile_document_id = pd.profile_document_id;


UPDATE custom.biomedgps_section set section_nm = 'Select Market' where section_id = 'MASTER_ROOT';

delete from custom.biomedgps_market_section where market_id = '56' and section_id ='MASTER_4337';
delete from custom.biomedgps_market_section where market_id = '6549' and section_id ='MASTER_338';
delete from custom.biomedgps_market_section where market_id = '6601' and section_id ='MASTER_37874';
delete from custom.biomedgps_market_section where market_id = '6607' and section_id ='MASTER_37715';
delete from custom.biomedgps_market_section where market_id = '6609' and section_id ='MASTER_38033';
delete from custom.biomedgps_market_section where market_id = '6612' and section_id ='MASTER_38192';
delete from custom.biomedgps_market_section where market_id = '6613' and section_id ='MASTER_38351';
delete from custom.biomedgps_market_section where market_id = '6617' and section_id ='MASTER_40244';
delete from custom.biomedgps_market_section where market_id = '6625' and section_id ='MASTER_40081';
delete from custom.biomedgps_market_section where market_id = '6627' and section_id ='MASTER_29899';
delete from custom.biomedgps_market_section where market_id = '6628' and section_id ='MASTER_30037';
delete from custom.biomedgps_market_section where market_id = '6629' and section_id ='MASTER_29623';
delete from custom.biomedgps_market_section where market_id = '6630' and section_id ='MASTER_29761';
delete from custom.biomedgps_market_section where market_id = '6631' and section_id ='MASTER_30175';


-- 
--Change all "Regulatory"(8) , "Reimbursement"(7), "Healthcare Reform"(13), 
-- "Intellectual Property"(14), "Compliance"(11) to Deleted status
--
update custom.biomedgps_insight
set status_cd = 'D', update_dt = now()
where type_cd in ('8', '7', '13', '14', '11');


-- HubSpot domain key

-- create Hubspot embed external site key for domain key use.
insert into external_site_key (external_site_key_id, key_nm, key_cd, key_type_id, tag_txt, create_dt)
values ('HUBSPOT_EMBED','Hubspot','hs-script-loader','INFO','<!-- HubSpot Embed Code --> <script type="text/javascript" id="${key}" async defer src="//js.hs-scripts.com/${value}.js"></script>',getdate());

-- insert domain key for Smarttrak in sb-uat
insert into site_key_xr (site_key_xr_id,key_txt,site_alias_id,external_site_key_id,create_dt)
values (lower(replace(newid(),'-','')),'3044256','c0a802418db21c85da5c8f1def9d70b3','HUBSPOT_EMBED',getdate());


-- convert markup -- Java process
java com.smt.sitebuilder.db.LegacyMarkdownConverter

-- to merge images into Insight's left column, main content, and insert Attachments.
-- run the above class in two modes (less risk); once for graphics and once for attachments.
java com.smt.sitebuilder.db.LegacyImageInserter

-- import legacy emaillog -- Java process
-- run this absolutely last, it takes over an hour and only serves to populate a legacy report
java com.smt.sitebuilder.db.LegacyEmailImporter from command line.

run solr indexers

-- place binary images and media

-- apache rules for binary and media


-- database validation
SELECT schemaname,relname,n_live_tup  FROM pg_stat_user_tables where schemaname='biomedgps' ORDER BY schemaname,relname;
SELECT schemaname,relname,n_live_tup  FROM pg_stat_user_tables where relname like 'biomedgps_%' ORDER BY n_live_tup desc, schemaname,relname;

-- (this was staging after last import, 4-14-2017)
custom	biomedgps_product_attribute_xr	119105
custom	biomedgps_update_section	57120
custom	biomedgps_update	46680
custom	biomedgps_grid_detail	21089
custom	biomedgps_fd_revenue	15900
custom	biomedgps_company_attribute_xr	14246
custom	biomedgps_product_regulatory	10147
custom	biomedgps_fd_scenario_overlay	7927
custom	biomedgps_product_section	7841
custom	biomedgps_product	7257
custom	biomedgps_user	6111
custom	biomedgps_account_acl	3671
custom	biomedgps_company_section	2409
custom	biomedgps_company	2396
custom	biomedgps_company_location	2396
custom	biomedgps_grid	2163
custom	biomedgps_note	2010
custom	biomedgps_product_moduleset_xr	1487
custom	biomedgps_market_attribute_xr	1342
custom	biomedgps_product_attribute	1033
custom	biomedgps_company_alliance_xr	821
custom	biomedgps_user_team_xr	736
custom	biomedgps_product_alliance_xr	706
custom	biomedgps_company_investor	399
custom	biomedgps_team	366
custom	biomedgps_insight_section	359
custom	biomedgps_ga_column_attribute_xr	334
custom	biomedgps_fd_scenario	332
custom	biomedgps_insight	327
custom	biomedgps_section	286
custom	biomedgps_audit_log	216
custom	biomedgps_account	207
custom	biomedgps_market	145
custom	biomedgps_market_section	142
custom	biomedgps_fd_revenue_footnote	138
custom	biomedgps_ga_column	137
custom	biomedgps_company_attribute	103
custom	biomedgps_regulatory_path	46
custom	biomedgps_regulatory_status	37
custom	biomedgps_stock_exchange	24
custom	biomedgps_regulatory_region	23
custom	biomedgps_alliance_type	21
custom	biomedgps_product_moduleset	14
custom	biomedgps_chart_type	11
custom	biomedgps_market_attribute	8
custom	biomedgps_grid_detail_type	5
custom	biomedgps_grid_type	4
custom	biomedgps_explorer_query	0
custom	biomedgps_ga_savestate	0
custom	biomedgps_link	0


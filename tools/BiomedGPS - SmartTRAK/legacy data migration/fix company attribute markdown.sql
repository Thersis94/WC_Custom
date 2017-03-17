
-- company attributes

delete from custom.BIOMEDGPS_COMPANY_ATTRIBUTE_XR;

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
where e.content_type_id=8 and upper(e.status)='P';

--fix a damaged record - could not fix on legacy/live site:
update custom.biomedgps_company_attribute_xr set value_txt=replace(value_txt,'**Nov88','**Nov**') where company_id='9' and attribute_id='13RecentCommentary';

-- run the script --

--check for unfixed data
select * from custom.biomedgps_company_attribute_xr where value_txt like '%##%';
select * from custom.biomedgps_company_attribute_xr where value_txt like '%---%';
select * from custom.biomedgps_company_attribute_xr where value_txt like '%**%'; --should be one, **Nov88 ...false positive
select * from custom.biomedgps_company_attribute_xr where value_txt like '%](http%';


-- product attributes

delete from custom.BIOMEDGPS_PRODUCT_ATTRIBUTE_XR;

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

-- run the script --

--check for unfixed data
select product_id, attribute_id, * from custom.biomedgps_PRODUCT_attribute_xr where value_txt like '%##%';
select product_id, attribute_id, * from custom.biomedgps_PRODUCT_attribute_xr where value_txt like '%---%'; -- 5 false positives
select product_id, attribute_id, * from custom.biomedgps_PRODUCT_attribute_xr where value_txt like '%**%'; -- 1 false positive
select product_id, attribute_id, * from custom.biomedgps_PRODUCT_attribute_xr where value_txt like '%](http%';


-- markets ---

delete from custom.BIOMEDGPS_MARKET_ATTRIBUTE_XR;

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

-- run the script --

--check for unfixed data
select * from custom.biomedgps_MARKET_attribute_xr where value_txt like '%##%';
select * from custom.biomedgps_MARKET_attribute_xr where value_txt like '%---%';
select * from custom.biomedgps_MARKET_attribute_xr where value_txt like '%**%';
select * from custom.biomedgps_MARKET_attribute_xr where value_txt like '%](http%';


-- insights --

--reset the data
update custom.biomedgps_insight a
set abstract_txt=b.abstract, content_txt=b.content
from biomedgps.commentaries_commentary b where a.insight_id=cast(b.id as varchar)

-- run the script --

--check for unfixed data
select * from custom.biomedgps_insight where content_txt like '%##%';
select * from custom.biomedgps_insight where content_txt like '%---%';
select * from custom.biomedgps_insight where content_txt like '%**%';
select * from custom.biomedgps_insight where content_txt like '%](http%';
select * from custom.biomedgps_insight where abstract_txt like '%##%';
select * from custom.biomedgps_insight where abstract_txt like '%---%';
select * from custom.biomedgps_insight where abstract_txt like '%**%';
select * from custom.biomedgps_insight where abstract_txt like '%](http%';


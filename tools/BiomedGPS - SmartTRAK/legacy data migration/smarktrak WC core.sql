/**
 * WC widget registrations & core data changes
 **/
 

 -- FD
 insert into MODULE_TYPE (MODULE_TYPE_ID, ORGANIZATION_ID, MODULE_TYPE_NM, CLASS_NM, INDEXER_CLASS_NM, ADMIN_FLG, SERVICE_URL, FACADE_FLG, GLOBAL_ADMIN_FLG, DISPLAY_FLG, CREATE_DT, REPORT_FLG, CACHEABLE_FLG, CLONABLE_FLG, APPROVABLE_FLG, WIDGET_CATEGORY_ID) values ('SMARTTRAK_FINANCIAL_DASH', 'BMG_SMARTTRAK', 'SmartTRAK Financial Dashboard', 'com.biomed.smarttrak.fd.FinancialDashAction', '', 0, '', 0, 0, 1, getdate(), 0, 0, 0, 0, 'CUSTOM');

insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('7f00010194f5681928a08fb0200ee9e9', 'SMARTTRAK_FINANCIAL_DASH', '/custom/biomed/smarttrak/FinancialDash/index.jsp', 'Main', getdate(), '', null, 0, 1);
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('fedec22e7cc2f0807f000101ba965808', 'SMARTTRAK_FINANCIAL_DASH', '/custom/biomed/smarttrak/FinancialDash/nonAdmin.jsp', 'Non-Admin', getdate(), '', null, 0, 1);


-- Notes action
insert into MODULE_TYPE (MODULE_TYPE_ID, ORGANIZATION_ID, MODULE_TYPE_NM, CLASS_NM, INDEXER_CLASS_NM, ADMIN_FLG, SERVICE_URL, FACADE_FLG, GLOBAL_ADMIN_FLG, DISPLAY_FLG, CREATE_DT, REPORT_FLG, CACHEABLE_FLG, CLONABLE_FLG, APPROVABLE_FLG, WIDGET_CATEGORY_ID) values ('BIOMED_NOTE', 'BMG_SMARTTRAK', 'Biomed SmartTRAK Note', 'com.biomed.smarttrak.action.NoteAction', '', 0, '', 0, 0, 1, getdate(), 0, 0, 0, 0, 'CONTACT_SERVICES');
insert into organization_module values
(LOWER(REPLACE(NEWID(),'-' ,'')),'BMG_SMARTTRAK','BIOMED_NOTE', now() );

--Notes form view
INSERT INTO core.module_display
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('dfc0e193v68e0a5c7f000101d7432c10', 'BIOMED_NOTE', '/custom/biomed/smarttrak/NOTE/form.jsp', 'form', getdate(), '', null, 0, 0);
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('ca42c5941fa775d97f0001015571dc03', 'BIOMED_NOTE', '/custom/biomed/smarttrak/NOTE/list.jsp', 'list', getdate(), '', null, 0, 0);
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('3cfe3849247fc8407f000101fe658d65', 'BIOMED_NOTE', '/custom/biomed/smarttrak/NOTE/index.jsp', 'main', getdate(), '', null, 0, 1);

--may need to create an instance of the widget here - Ryan omitted.

-- reg ajax action
INSERT INTO core.ajax_module
(ajax_module_id, ajax_nm, site_id, action_id, status_cd, 
create_dt, update_dt, module_display_id)
values
('a292dc70v6a7044f7f000101b1b54916', 'noteAction', 'BMG_SMARTTRAK_1', '3e8944afv6a623cd7f00010177d8e14a', 1, 
'2017-02-03 18:04:53.328', '2017-02-03 18:23:09.368', 'dfc0e193v68e0a5c7f000101d7432c10');

INSERT INTO core.ajax_module_role_xr
(ajax_module_role_xr_id, ajax_module_id, role_id, create_dt)
VALUES('53aa38e8v6b7be477f000101a20a7653', 'a292dc70v6a7044f7f000101b1b54916', '10', '2017-02-03 18:23:09.511');
INSERT INTO core.ajax_module_role_xr
(ajax_module_role_xr_id, ajax_module_id, role_id, create_dt)
VALUES('8afc1eb1v6b7be477f000101a20a7653', 'a292dc70v6a7044f7f000101b1b54916', '3eef678eb39e87277f000101dfd4f140', '2017-02-03 18:23:09.511');
INSERT INTO core.ajax_module_role_xr
(ajax_module_role_xr_id, ajax_module_id, role_id, create_dt)
VALUES('7cf57300v6b7be477f000101a20a7653', 'a292dc70v6a7044f7f000101b1b54916', '100', '2017-02-03 18:23:09.511');


/*email campaign widget action */
INSERT INTO module_type(module_type_id, organization_id, module_type_nm, class_nm, admin_flg, facade_flg, global_admin_flg,
display_flg, create_dt, report_flg, cacheable_flg, clonable_flg, approvable_flg, widget_category_id, indexer_class_nm)
VALUES('EMAIL_CAMPAIGN_WIDGET', null, 'Email Campaign Widget', 'com.smt.sitebuilder.action.emailcampaign.CampaignInstanceAssociativeAction', 1, 0, 0,
0, getDate(), 0, 0, 0, 0, 'UNALLOCATED', null);


insert into core.INDEX_TYPE (INDEX_TYPE_CD, TYPE_NM, CREATE_DT, SOLR_COLLECTION_ID)
values ('BIOMEDGPS_PRODUCT', 'SmartTRAK Products', current_timestamp, 'SB_COLLECTION'),
('BIOMEDGPS_MARKET', 'SmartTRAK Markets', current_timestamp, 'SB_COLLECTION'),
('BIOMEDGPS_COMPANY', 'SmartTRAK Companies', current_timestamp, 'SB_COLLECTION'),
('BIOMED_UPDATE','SmartTRAK Updates', current_timestamp, 'SB_COLLECTION'),
('BIOMED_INSIGHT','SmartTRAK Insights', current_timestamp, 'SB_COLLECTION');

--quick links
insert into MODULE_TYPE (MODULE_TYPE_ID, ORGANIZATION_ID, MODULE_TYPE_NM, CLASS_NM, INDEXER_CLASS_NM, ADMIN_FLG, SERVICE_URL, FACADE_FLG, GLOBAL_ADMIN_FLG, DISPLAY_FLG, CREATE_DT, REPORT_FLG, CACHEABLE_FLG, CLONABLE_FLG, APPROVABLE_FLG, WIDGET_CATEGORY_ID) values ('QUICK_LINKS', 'BMG_SMARTTRAK', 'SmartTRAK QuickLinks', 'com.biomed.smarttrak.action.QuickLinksAction', '', 0, '', 0, 0, 1, getdate(), 0, 0, 0, 0, 'CUSTOM');

--quick links view
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('7209a00b4d6e19f47f000101ddd7bfaa', 'QUICK_LINKS', '/custom/biomed/smarttrak/USER/quick_links/quick_links.jsp', 'Main', getdate(), '', 'BMG_SMARTTRAK', 0, 1);

--inserting Insights Solr Action (sb_action)
INSERT INTO core.sb_action
(action_id, organization_id, module_type_id, action_nm, action_desc, create_dt, update_dt, attrib1_txt, attrib2_txt, action_url, intro_txt, pending_sync_flg, action_group_id, create_by_id, update_by_id)
values
('dc2a2a516eab4b68bbc64c6057fe4c5e', 'BMG_SMARTTRAK', 'BIOMED_INSIGHTS', 'Featured Insights', 'Featured insights', now(), NULL, '84ca6fd6823f582c7f0001016eca4ac2', NULL, NULL, NULL, 0, 'dc2a2a516eab4b68bbc64c6057fe4c5e', 'c0a80241f1b1253e6b1c166b380c1143', NULL),('ce810fd38fecf1937f0001013d945ef4', 'BMG_SMARTTRAK', 'BIOMED_INSIGHTS', 'Biomed Insight Wrapper test', 'Wrapper Test', '2017-01-11 16:46:27.347', NULL, 'c8511f927166316f7f000101b4b5e712', NULL, NULL, NULL, 0, 'ce810fd38fecf1937f0001013d945ef4', 'c0a80241f1b1253e6b1c166b380c1143', NULL);

--inserting Insights solr action
INSERT INTO core.solr_action
(solr_action_id, organization_id, action_id, response_no, display_results_flg, retrieve_facet_flg, facet_sort_txt, field_sort_txt, sort_dir_txt, create_dt, update_dt, solr_collection_id, keyword_modify_no, acl_type_no)
VALUES('e0599619823f597d7f000101b66dee29', 'BMG_SMARTTRAK', '84ca6fd6823f582c7f0001016eca4ac2', 15, 1, 0, '', 'updateDate', 'desc', '2017-02-27 18:04:33.772', NULL, 'SB_COLLECTION', 0, 0),
('7879abd6716631a97f0001017bbad9ac', 'BMG_SMARTTRAK', 'c8511f927166316f7f000101b4b5e712', 15, 1, 1, 'index', 'updateDate', 'desc', '2017-02-24 11:33:27.243', NULL, 'SB_COLLECTION', 5, 0);

--fields for Insights solr search widgets
INSERT INTO core.solr_field
(solr_field_id, solr_field_type_cd, solr_action_id, field_cd, value_txt, boolean_type_cd, create_dt)
VALUES('50cc22d8823b2ecd7f0001016f43baa5', 'FACET', '7879abd6716631a97f0001017bbad9ac', 'hierarchy', NULL, NULL, '2017-02-27 18:00:01.101'),
('20ca9217823b2f017f000101e7f8f909', 'BOOST', '7879abd6716631a97f0001017bbad9ac', 'title', '', NULL, '2017-02-27 18:00:01.152'),
('80d623d4823b2f347f0001012ce19191', 'BOOST', '7879abd6716631a97f0001017bbad9ac', 'hierarchy', '', NULL, '2017-02-27 18:00:01.204'),
('84a800f9823b2f697f000101e03dc7f2', 'BOOST', '7879abd6716631a97f0001017bbad9ac', 'firstNm_s', '', NULL, '2017-02-27 18:00:01.257'),
('dfb17454823b2fa47f0001011aee92cc', 'BOOST', '7879abd6716631a97f0001017bbad9ac', 'lastNm_s', '', NULL, '2017-02-27 18:00:01.316'),
('3068120e823b2fd97f000101f5a01cc4', 'BOOST', '7879abd6716631a97f0001017bbad9ac', 'summary', '', NULL, '2017-02-27 18:00:01.369'),
('ab4baacf828505be7f000101394a945a', 'FILTER', 'e0599619823f597d7f000101b66dee29', 'featuredFlg_i', '1', 'AND', '2017-02-27 19:20:40.253');

-- solr action_index for Insights solr action
INSERT INTO core.solr_action_index
(action_index_id, solr_action_id, index_type_cd, create_dt)
VALUES('b3927f8c823b30397f0001012c2194a6', '7879abd6716631a97f0001017bbad9ac', 'BIOMED_INSIGHT', NULL),
('f2e808668285063c7f00010130d588a0', 'e0599619823f597d7f000101b66dee29', 'BIOMED_INSIGHT', NULL);

--login view
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('b3ec66a43a8361850a001421acb0e9f3', 'LOGIN', '/user/bootstrap-login/index.jsp', 'Bootstrap Login Form', getdate(), '', null, 0, 1);

--site search view
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('88a4206b471335600a00142166b0a69b', 'SOLR_SEARCH', '/custom/biomed/smarttrak/search/index.jsp', 'Smarttrak Site Search', getdate(), '', 'BMG_SMARTTRAK', 0, 0);


--admin controller
insert into MODULE_TYPE (MODULE_TYPE_ID, ORGANIZATION_ID, MODULE_TYPE_NM, CLASS_NM, INDEXER_CLASS_NM, ADMIN_FLG, SERVICE_URL, FACADE_FLG, GLOBAL_ADMIN_FLG, DISPLAY_FLG, CREATE_DT, REPORT_FLG, CACHEABLE_FLG, CLONABLE_FLG, APPROVABLE_FLG, WIDGET_CATEGORY_ID) values ('BIOMED_ADMIN_CONTROLLER', 'BMG_SMARTTRAK', 'SmartTRAK Admin Controller', 'com.biomed.smarttrak.action.AdminControllerAction', '', 0, '', 0, 0, 0, getdate(), 0, 0, 0, 0, 'ADMINISTRATION');

insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('3e3b2bc829da4d3298be02e9930ff9e5', 'BIOMED_ADMIN_CONTROLLER', '/custom/biomed/smarttrak/admin/index.jsp', 'Default View', getdate(), '', null, 0, 0);

--gap analysis
insert into MODULE_TYPE (MODULE_TYPE_ID, ORGANIZATION_ID, MODULE_TYPE_NM, CLASS_NM, INDEXER_CLASS_NM, ADMIN_FLG, SERVICE_URL, FACADE_FLG, GLOBAL_ADMIN_FLG, DISPLAY_FLG, CREATE_DT, REPORT_FLG, CACHEABLE_FLG, CLONABLE_FLG, APPROVABLE_FLG, WIDGET_CATEGORY_ID) values ('GAP_ANALYSIS', 'BMG_SMARTTRAK', 'SmartTRAK Gap Analysis', 'com.biomed.smarttrak.action.GapAnalysisAction', '', 0, '', 0, 0, 1, getdate(), 0, 0, 0, 0, 'CUSTOM');
-- view
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('8d0525acd15b9e540a001413b29311f2', 'GAP_ANALYSIS', '/custom/biomed/smarttrak/GAP_ANALYSIS/index.jsp', 'Default Gap Analysis View', getdate(), '', 'BMG_SMARTTRAK', 0, 1);

--companies page
insert into MODULE_TYPE (MODULE_TYPE_ID, ORGANIZATION_ID, MODULE_TYPE_NM, CLASS_NM, INDEXER_CLASS_NM, ADMIN_FLG, SERVICE_URL, FACADE_FLG, GLOBAL_ADMIN_FLG, DISPLAY_FLG, CREATE_DT, REPORT_FLG, CACHEABLE_FLG, CLONABLE_FLG, APPROVABLE_FLG, WIDGET_CATEGORY_ID) values ('BIOMED_COMPANY', 'BMG_SMARTTRAK', 'SmartTRAK Companies', 'com.biomed.smarttrak.action.CompanyAction', '', 0, '', 0, 0, 1, getdate(), 0, 0, 0, 0, 'CUSTOM');

insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('fc15aa213824a9610a00141de19d048b', 'BIOMED_COMPANY', '/custom/biomed/smarttrak/companies/index.jsp', 'Default', getdate(), '', null, 0, 1);
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('81284d873d13246b0a00141dd8ede818', 'BIOMED_COMPANY', '/custom/biomed/smarttrak/companies/companies-response.jsp', 'Response Table', getdate(), '', null, 0, 0);
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('466ce27787baabe50a00141d305a4167', 'BIOMED_COMPANY', '/custom/biomed/smarttrak/facet-hierarchy.jsp', 'Hierarchy Return', getdate(), '', null, 0, 0);

-- grid display
insert into MODULE_TYPE (MODULE_TYPE_ID, ORGANIZATION_ID, MODULE_TYPE_NM, CLASS_NM, INDEXER_CLASS_NM, ADMIN_FLG, SERVICE_URL, FACADE_FLG, GLOBAL_ADMIN_FLG, DISPLAY_FLG, CREATE_DT, REPORT_FLG, CACHEABLE_FLG, CLONABLE_FLG, APPROVABLE_FLG, WIDGET_CATEGORY_ID) values ('GRID_DISPLAY_ACTION', 'BMG_SMARTTRAK', 'SmartTRAK Grid Display', 'com.biomed.smarttrak.action.GridDisplayAction', '', 0, '', 0, 0, 1, getdate(), 0, 0, 0, 0, 'UNALLOCATED');
Widget Display Creation
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('38bf8c228210c7690a001a75495bc032', 'GRID_DISPLAY_ACTION', '/none', 'Empty View for AMID', getdate(), '', 'BMG_SMARTTRAK', 0, 0);
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('e209fd8d8213a78a0a001a756fab1937', 'GRID_DISPLAY_ACTION', '/none', 'JSON View', getdate(), '', 'BMG_SMARTTRAK', 0, 0);
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('dd8cf2f68214552c0a001a752a49bccf', 'GRID_DISPLAY_ACTION', '/none', 'JSON View', getdate(), '', null, 0, 0);

--insights
insert into MODULE_TYPE (MODULE_TYPE_ID, ORGANIZATION_ID, MODULE_TYPE_NM, CLASS_NM, INDEXER_CLASS_NM, ADMIN_FLG, SERVICE_URL, FACADE_FLG, GLOBAL_ADMIN_FLG, DISPLAY_FLG, CREATE_DT, REPORT_FLG, CACHEABLE_FLG, CLONABLE_FLG, APPROVABLE_FLG, WIDGET_CATEGORY_ID) values ('BIOMED_INSIGHTS', 'BMG_SMARTTRAK', 'SmartTRAK Insights', 'com.biomed.smarttrak.action.InsightAction', '', 0, '', 0, 0, 1, getdate(), 0, 0, 0, 0, 'CONTENT_MANAGEMENT');
Widget Display Creation
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('b7542e386ef5ab787f000101e37ad6d6', 'BIOMED_INSIGHTS', '/custom/biomed/smarttrak/insights/index.jsp', 'default insights view', getdate(), '', null, 0, 1);
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('ec82315e8276a03b7f000101e6ba202d', 'BIOMED_INSIGHTS', '/custom/biomed/smarttrak/insights/featured.jsp', 'featured', getdate(), '', null, 0, 0);


--markets
insert into MODULE_TYPE (MODULE_TYPE_ID, ORGANIZATION_ID, MODULE_TYPE_NM, CLASS_NM, INDEXER_CLASS_NM, ADMIN_FLG, SERVICE_URL, FACADE_FLG, GLOBAL_ADMIN_FLG, DISPLAY_FLG, CREATE_DT, REPORT_FLG, CACHEABLE_FLG, CLONABLE_FLG, APPROVABLE_FLG, WIDGET_CATEGORY_ID) values ('BIOMEDGPS_MARKET', 'BMG_SMARTTRAK', 'SmartTRAK Markets', 'com.biomed.smarttrak.action.MarketAction', '', 0, '', 0, 0, 1, getdate(), 0, 0, 0, 0, 'CUSTOM');
Widget Display Creation
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('9c2dbe00332270ca0a00141d8364570d', 'BIOMEDGPS_MARKET', '/custom/biomed/smarttrak/markets/index.jsp', 'Default', getdate(), '', null, 0, 1);

--my teams
insert into MODULE_TYPE (MODULE_TYPE_ID, ORGANIZATION_ID, MODULE_TYPE_NM, CLASS_NM, INDEXER_CLASS_NM, ADMIN_FLG, SERVICE_URL, FACADE_FLG, GLOBAL_ADMIN_FLG, DISPLAY_FLG, CREATE_DT, REPORT_FLG, CACHEABLE_FLG, CLONABLE_FLG, APPROVABLE_FLG, WIDGET_CATEGORY_ID) values ('BIOMED_MY_TEAMS', 'BMG_SMARTTRAK', 'SmartTRAK My Teams', 'com.biomed.smarttrak.action.MyTeamsAction', '', 0, '', 0, 0, 1, getdate(), 0, 0, 0, 0, 'CUSTOM');
--Widget Display Creation
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('6f294e5b80f62d190a001421c247c83d', 'BIOMED_MY_TEAMS', '/custom/biomed/smarttrak/my-teams/index.jsp', 'Default', getdate(), '', null, 0, 1);

--product explorer
insert into MODULE_TYPE (MODULE_TYPE_ID, ORGANIZATION_ID, MODULE_TYPE_NM, CLASS_NM, INDEXER_CLASS_NM, ADMIN_FLG, SERVICE_URL, FACADE_FLG, GLOBAL_ADMIN_FLG, DISPLAY_FLG, CREATE_DT, REPORT_FLG, CACHEABLE_FLG, CLONABLE_FLG, APPROVABLE_FLG, WIDGET_CATEGORY_ID) values ('BIOMED_PRODUCT_EXPLORER', 'BMG_SMARTTRAK', 'SmartTRAK Product Explorer', 'com.biomed.smarttrak.action.ProductExplorer', '', 0, '', 0, 0, 1, getdate(), 0, 0, 0, 0, 'CUSTOM');
--Widget Display Creation
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('f1900ae9498955140a00141df9d620a1', 'BIOMED_PRODUCT_EXPLORER', '/custom/biomed/smarttrak/product_explorer/products.jsp', 'Ajax Response', getdate(), '', null, 0, 0);
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('f8ee95184988f21e0a00141d1697e766', 'BIOMED_PRODUCT_EXPLORER', '/custom/biomed/smarttrak/product_explorer/index.jsp', 'Default', getdate(), '', null, 0, 1);
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('6cfb3e6d8b5ff0800a00141d575bd83b', 'BIOMED_PRODUCT_EXPLORER', '/custom/biomed/smarttrak/facet-hierarchy.jsp', 'Hierarchy Response', getdate(), '', null, 0, 0);

--products
insert into MODULE_TYPE (MODULE_TYPE_ID, ORGANIZATION_ID, MODULE_TYPE_NM, CLASS_NM, INDEXER_CLASS_NM, ADMIN_FLG, SERVICE_URL, FACADE_FLG, GLOBAL_ADMIN_FLG, DISPLAY_FLG, CREATE_DT, REPORT_FLG, CACHEABLE_FLG, CLONABLE_FLG, APPROVABLE_FLG, WIDGET_CATEGORY_ID) values ('BIOMED_PRODUCT', 'BMG_SMARTTRAK', 'SmartTRAK Products', 'com.biomed.smarttrak.action.ProductAction', '', 0, '', 0, 0, 1, getdate(), 0, 0, 0, 0, 'CUSTOM');
--Widget Display Creation
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('effdc8f742ab28160a00141d17669c2c', 'BIOMED_PRODUCT', '/custom/biomed/smarttrak/products/product-response.jsp', 'Response Table', getdate(), '', null, 0, 0);
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('63912d4842aad48b0a00141de7514fdd', 'BIOMED_PRODUCT', '/custom/biomed/smarttrak/products/index.jsp', 'Default', getdate(), '', null, 0, 1);
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('efa986368b19700e0a00141da2f5a27b', 'BIOMED_PRODUCT', '/custom/biomed/smarttrak/facet-hierarchy.jsp', 'Facet Hierarchy', getdate(), '', null, 0, 0);

--support ticket
insert into MODULE_TYPE (MODULE_TYPE_ID, ORGANIZATION_ID, MODULE_TYPE_NM, CLASS_NM, INDEXER_CLASS_NM, ADMIN_FLG, SERVICE_URL, FACADE_FLG, GLOBAL_ADMIN_FLG, DISPLAY_FLG, CREATE_DT, REPORT_FLG, CACHEABLE_FLG, CLONABLE_FLG, APPROVABLE_FLG, WIDGET_CATEGORY_ID) values ('BMG_TICKET', 'BMG_SMARTTRAK', 'SmartTRAK Ticket System', 'com.biomed.smarttrak.admin.SupportFacadeAction', '', 0, '', 0, 0, 1, getdate(), 0, 0, 0, 0, 'CONTACT_SERVICES');
--Widget Display Creation
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('622842be853c03ba0a0014134aa9c1b2', 'BMG_TICKET', '/custom/biomed/smarttrak/admin/support/index.jsp', 'Management Views', getdate(), '', 'BMG_SMARTTRAK', 0, 1);

--updates
insert into MODULE_TYPE (MODULE_TYPE_ID, ORGANIZATION_ID, MODULE_TYPE_NM, CLASS_NM, INDEXER_CLASS_NM, ADMIN_FLG, SERVICE_URL, FACADE_FLG, GLOBAL_ADMIN_FLG, DISPLAY_FLG, CREATE_DT, REPORT_FLG, CACHEABLE_FLG, CLONABLE_FLG, APPROVABLE_FLG, WIDGET_CATEGORY_ID) values ('BIOMED_UPDATES', 'BMG_SMARTTRAK', 'SmartTRAK Updates', 'com.biomed.smarttrak.action.UpdatesAction', '', 0, '', 0, 0, 1, getdate(), 0, 0, 0, 0, 'CONTENT_MANAGEMENT');
--Widget Display Creation
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('7109a5df491f3fc30a001413e1576dc9', 'BIOMED_UPDATES', '/custom/biomed/smarttrak/updates/index.jsp', 'Default Updates View', getdate(), '', 'BMG_SMARTTRAK', 0, 1);

--ask analysist contact us view
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('3948895c62d2afe20a0014135ca5ec64', 'CONTACT', '/contact/bootstrap-embed.jsp', 'Ask An Analyst Jsp', getdate(), '', 'BMG_SMARTTRAK', 0, 0);

--content hierarchy - used via ajax in /manage
insert into MODULE_TYPE (MODULE_TYPE_ID, ORGANIZATION_ID, MODULE_TYPE_NM, CLASS_NM, INDEXER_CLASS_NM, ADMIN_FLG, SERVICE_URL, FACADE_FLG, GLOBAL_ADMIN_FLG, DISPLAY_FLG, CREATE_DT, REPORT_FLG, CACHEABLE_FLG, CLONABLE_FLG, APPROVABLE_FLG, WIDGET_CATEGORY_ID) values ('CONTENT_HIERARCHY', 'BMG_SMARTTRAK', 'SmartTRAK Section Hierarchy', 'com.biomed.smarttrak.admin.SectionHierarchyAction', '', 0, '', 0, 0, 1, getdate(), 0, 0, 0, 0, 'ADMINISTRATION');
--Widget Display Creation
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('0a001413856e81cf66158582b9fd1ca2', 'CONTENT_HIERARCHY', '/custom/biomed/smarttrak/CONTENT_HIERARCHY/index.jsp', 'Default View', getdate(), '', 'BMG_SMARTTRAK', 0, 1);

--registration view
insert into MODULE_DISPLAY (MODULE_DISPLAY_ID, MODULE_TYPE_ID, DISPLAY_PAGE_NM, DISPLAY_DESC, CREATE_DT, STYLESHEET_TXT, ORGANIZATION_ID, INDEXABLE_FLG, DEFAULT_FLG) values ('83346fb948ec46830a00142196d99c41', 'REGISTRATION', '/custom/biomed/smarttrak/registration/index.jsp', 'SmartTRAK My Account', getdate(), '', 'BMG_SMARTTRAK', 0, 0);

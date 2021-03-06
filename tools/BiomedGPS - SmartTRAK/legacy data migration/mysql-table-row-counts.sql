 SELECT "addresses_address" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`addresses_address` UNION                                                     
 SELECT "articles_article" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`articles_article` UNION                                                       
 SELECT "articles_article_subsegments" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`articles_article_subsegments` UNION                               
 SELECT "articles_attachment" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`articles_attachment` UNION                                                 
 SELECT "articles_grid" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`articles_grid` UNION                                                             
 SELECT "articles_gridrow" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`articles_gridrow` UNION                                                       
 SELECT "articles_historicalarticle" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`articles_historicalarticle` UNION                                   
 SELECT "articles_image" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`articles_image` UNION                                                           
 SELECT "articles_market" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`articles_market` UNION                                                         
 SELECT "articles_marketorder" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`articles_marketorder` UNION                                               
 SELECT "articles_prettycontent" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`articles_prettycontent` UNION                                           
 SELECT "articles_review" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`articles_review` UNION                                                         
 SELECT "articles_segment" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`articles_segment` UNION                                                       
 SELECT "articles_segmentorder" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`articles_segmentorder` UNION                                             
 SELECT "articles_subsegment" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`articles_subsegment` UNION                                                 
 SELECT "articles_subsegmentorder" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`articles_subsegmentorder` UNION                                       
 SELECT "attachments_attachment" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`attachments_attachment` UNION                                           
 SELECT "audit_audit" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`audit_audit` UNION                                                                 
 SELECT "auth_group" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`auth_group` UNION                                                                   
 SELECT "auth_group_permissions" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`auth_group_permissions` UNION                                           
 SELECT "auth_message" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`auth_message` UNION                                                               
 SELECT "auth_permission" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`auth_permission` UNION                                                         
 SELECT "breadcrumbs" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`breadcrumbs` UNION                                                                 
 SELECT "bulletins_bulletin" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`bulletins_bulletin` UNION                                                   
 SELECT "category_nodes" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`category_nodes` UNION                                                           
 SELECT "celery_taskmeta" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`celery_taskmeta` UNION                                                         
 SELECT "celery_tasksetmeta" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`celery_tasksetmeta` UNION                                                   
 SELECT "charts_chart" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`charts_chart` UNION                                                               
 SELECT "charts_series" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`charts_series` UNION                                                             
 SELECT "commentaries_commentary" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`commentaries_commentary` UNION                                         
 SELECT "commentaries_commentary_categories" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`commentaries_commentary_categories` UNION                   
 SELECT "commentaries_commentarytype" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`commentaries_commentarytype` UNION                                 
 SELECT "commentaries_microblog" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`commentaries_microblog` UNION                                           
 SELECT "commentaries_panel" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`commentaries_panel` UNION                                                   
 SELECT "commentaries_panelitem" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`commentaries_panelitem` UNION                                           
 SELECT "companies_alliance" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`companies_alliance` UNION                                                   
 SELECT "companies_alliancetype" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`companies_alliancetype` UNION                                           
 SELECT "companies_company" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`companies_company` UNION                                                     
 SELECT "companies_company_investors" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`companies_company_investors` UNION                                 
 SELECT "companies_country" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`companies_country` UNION                                                     
 SELECT "companies_currency" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`companies_currency` UNION                                                   
 SELECT "companies_gapanalysis" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`companies_gapanalysis` UNION                                             
 SELECT "companies_historicalcompany" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`companies_historicalcompany` UNION                                 
 SELECT "companies_soft_tissue_port" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`companies_soft_tissue_port` UNION                                   
 SELECT "companies_spineport" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`companies_spineport` UNION                                                 
 SELECT "companies_stockexchange" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`companies_stockexchange` UNION                                         
 SELECT "contacts_contact" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`contacts_contact` UNION                                                       
 SELECT "contacts_contactgroup" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`contacts_contactgroup` UNION                                             
 SELECT "contacts_contactgroup_contacts" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`contacts_contactgroup_contacts` UNION                           
 SELECT "customarticles_customarticle" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`customarticles_customarticle` UNION                               
 SELECT "customarticles_customtemplate" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`customarticles_customtemplate` UNION                             
 SELECT "django_admin_log" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`django_admin_log` UNION                                                       
 SELECT "django_content_type" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`django_content_type` UNION                                                 
 SELECT "django_session" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`django_session` UNION                                                           
 SELECT "django_site" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`django_site` UNION                                                                 
 SELECT "djcelery_crontabschedule" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`djcelery_crontabschedule` UNION                                       
 SELECT "djcelery_intervalschedule" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`djcelery_intervalschedule` UNION                                     
 SELECT "djcelery_periodictask" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`djcelery_periodictask` UNION                                             
 SELECT "djcelery_periodictasks" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`djcelery_periodictasks` UNION                                           
 SELECT "djcelery_taskstate" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`djcelery_taskstate` UNION                                                   
 SELECT "djcelery_workerstate" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`djcelery_workerstate` UNION                                               
 SELECT "docs_doc" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`docs_doc` UNION                                                                       
 SELECT "docs_docsection" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`docs_docsection` UNION                                                         
 SELECT "docs_historicaldoc" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`docs_historicaldoc` UNION                                                   
 SELECT "emaillog_emailbreadcrumb" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`emaillog_emailbreadcrumb` UNION                                       
 SELECT "emaillog_emaillog" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`emaillog_emaillog` UNION                                                     
 SELECT "embeds_embed" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`embeds_embed` UNION                                                               
 SELECT "embeds_grid" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`embeds_grid` UNION                                                                 
 SELECT "embeds_gridrow" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`embeds_gridrow` UNION                                                           
 SELECT "events_event" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`events_event` UNION                                                               
 SELECT "events_eventtype" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`events_eventtype` UNION                                                       
 SELECT "events_organizer" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`events_organizer` UNION                                                       
 SELECT "favorites_breadcrumb" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`favorites_breadcrumb` UNION                                               
 SELECT "favorites_favorite" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`favorites_favorite` UNION                                                   
 SELECT "favorites_saveitem" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`favorites_saveitem` UNION                                                   
 SELECT "favorites_saveset" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`favorites_saveset` UNION                                                     
 SELECT "financials_insight" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`financials_insight` UNION                                                   
 SELECT "financials_overlaycache" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`financials_overlaycache` UNION                                         
 SELECT "financials_overlaydata" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`financials_overlaydata` UNION                                           
 SELECT "financials_quarterinfo" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`financials_quarterinfo` UNION                                           
 SELECT "financials_revenue" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`financials_revenue` UNION                                                   
 SELECT "financials_revenue_audit" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`financials_revenue_audit` UNION                                       
 SELECT "financials_revenuenote" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`financials_revenuenote` UNION                                           
 SELECT "financials_revenueoverlay" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`financials_revenueoverlay` UNION                                     
 SELECT "financials_revenuerow" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`financials_revenuerow` UNION                                             
 SELECT "followups_followup" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`followups_followup` UNION                                                   
 SELECT "fp_temp" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`fp_temp` UNION                                                                         
 SELECT "fpc_temp" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`fpc_temp` UNION                                                                       
 SELECT "frtemp" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`frtemp` UNION                                                                           
 SELECT "gap_analysis_gacolumn" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`gap_analysis_gacolumn` UNION                                             
 SELECT "gap_analysis_gacolumn_classifications" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`gap_analysis_gacolumn_classifications` UNION             
 SELECT "gap_analysis_gacolumn_indications" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`gap_analysis_gacolumn_indications` UNION                     
 SELECT "gap_analysis_gacolumn_methods" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`gap_analysis_gacolumn_methods` UNION                             
 SELECT "gap_analysis_gacolumn_subsegments" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`gap_analysis_gacolumn_subsegments` UNION                     
 SELECT "gap_analysis_gacolumn_targetmarkets" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`gap_analysis_gacolumn_targetmarkets` UNION                 
 SELECT "gap_analysis_gacolumn_technologies" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`gap_analysis_gacolumn_technologies` UNION                   
 SELECT "gap_analysis_gacompanystatus" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`gap_analysis_gacompanystatus` UNION                               
 SELECT "gap_analysis_galayout" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`gap_analysis_galayout` UNION                                             
 SELECT "gap_analysis_gamarket" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`gap_analysis_gamarket` UNION                                             
 SELECT "gap_analysis_gasection" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`gap_analysis_gasection` UNION                                           
 SELECT "gap_analysis_gausercompanyinfo" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`gap_analysis_gausercompanyinfo` UNION                           
 SELECT "help_category" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`help_category` UNION                                                             
 SELECT "help_emailtip" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`help_emailtip` UNION                                                             
 SELECT "help_helpqueue" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`help_helpqueue` UNION                                                           
 SELECT "help_helpqueue_members" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`help_helpqueue_members` UNION                                           
 SELECT "help_question" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`help_question` UNION                                                             
 SELECT "help_ticket" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`help_ticket` UNION                                                                 
 SELECT "help_ticketlog" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`help_ticketlog` UNION                                                           
 SELECT "images_image" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`images_image` UNION                                                               
 SELECT "leads_lead" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`leads_lead` UNION                                                                   
 SELECT "leads_lead_categories" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`leads_lead_categories` UNION                                             
 SELECT "leads_opportunity" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`leads_opportunity` UNION                                                     
 SELECT "leads_sflink" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`leads_sflink` UNION                                                               
 SELECT "leads_sfref" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`leads_sfref` UNION                                                                 
 SELECT "leads_sfref_divisions" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`leads_sfref_divisions` UNION                                             
 SELECT "library_numericalreference" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`library_numericalreference` UNION                                   
 SELECT "links_link" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`links_link` UNION                                                                   
 SELECT "links_linktype" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`links_linktype` UNION                                                           
 SELECT "marketreports_marketreport" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`marketreports_marketreport` UNION                                   
 SELECT "marketreports_reportgroup" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`marketreports_reportgroup` UNION                                     
 SELECT "newprod" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`newprod` UNION                                                                         
 SELECT "nodes_accounttree" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`nodes_accounttree` UNION                                                     
 SELECT "nodes_node" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`nodes_node` UNION                                                                   
 SELECT "nodes_tree" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`nodes_tree` UNION                                                                   
 SELECT "notes_note" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`notes_note` UNION                                                                   
 SELECT "photologue_gallery" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`photologue_gallery` UNION                                                   
 SELECT "photologue_gallery_photos" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`photologue_gallery_photos` UNION                                     
 SELECT "photologue_galleryupload" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`photologue_galleryupload` UNION                                       
 SELECT "photologue_photo" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`photologue_photo` UNION                                                       
 SELECT "photologue_photoeffect" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`photologue_photoeffect` UNION                                           
 SELECT "photologue_photosize" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`photologue_photosize` UNION                                               
 SELECT "photologue_watermark" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`photologue_watermark` UNION                                               
 SELECT "products_anatomy" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_anatomy` UNION                                                       
 SELECT "products_classification" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_classification` UNION                                         
 SELECT "products_explorerquery" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_explorerquery` UNION                                           
 SELECT "products_indication" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_indication` UNION                                                 
 SELECT "products_method" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_method` UNION                                                         
 SELECT "products_milestonestatus" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_milestonestatus` UNION                                       
 SELECT "products_moduleset" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_moduleset` UNION                                                   
 SELECT "products_moduleset_classifications" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_moduleset_classifications` UNION                   
 SELECT "products_moduleset_indications" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_moduleset_indications` UNION                           
 SELECT "products_moduleset_methods" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_moduleset_methods` UNION                                   
 SELECT "products_moduleset_target_markets" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_moduleset_target_markets` UNION                     
 SELECT "products_moduleset_technologies" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_moduleset_technologies` UNION                         
 SELECT "products_partnertype" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_partnertype` UNION                                               
 SELECT "products_path" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_path` UNION                                                             
 SELECT "products_pathcountry" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_pathcountry` UNION                                               
 SELECT "products_product" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_product` UNION                                                       
 SELECT "products_product_articles" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_product_articles` UNION                                     
 SELECT "products_product_classification" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_product_classification` UNION                         
 SELECT "products_product_indication" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_product_indication` UNION                                 
 SELECT "products_product_method" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_product_method` UNION                                         
 SELECT "products_product_target_market" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_product_target_market` UNION                           
 SELECT "products_product_technology" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_product_technology` UNION                                 
 SELECT "products_producttype" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_producttype` UNION                                               
 SELECT "products_profile" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_profile` UNION                                                       
 SELECT "products_regulatoryinternational" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_regulatoryinternational` UNION                       
 SELECT "products_regulatorymilestone" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_regulatorymilestone` UNION                               
 SELECT "products_regulatorypath" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_regulatorypath` UNION                                         
 SELECT "products_regulatorystatus" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_regulatorystatus` UNION                                     
 SELECT "products_strategicpartner" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_strategicpartner` UNION                                     
 SELECT "products_targetmarket" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_targetmarket` UNION                                             
 SELECT "products_technology" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`products_technology` UNION                                                 
 SELECT "profile_account_primary_contacts" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profile_account_primary_contacts` UNION                       
 SELECT "profiles_account" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_account` UNION                                                       
 SELECT "profiles_account_custom_companies" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_account_custom_companies` UNION                     
 SELECT "profiles_account_custom_products" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_account_custom_products` UNION                       
 SELECT "profiles_account_gap_analysis_nodes" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_account_gap_analysis_nodes` UNION                 
 SELECT "profiles_account_metrics" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_account_metrics` UNION                                       
 SELECT "profiles_account_permissions" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_account_permissions` UNION                               
 SELECT "profiles_account_subsegments" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_account_subsegments` UNION                               
 SELECT "profiles_account_subsegments_master" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_account_subsegments_master` UNION                 
 SELECT "profiles_account_update_nodes" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_account_update_nodes` UNION                             
 SELECT "profiles_accountoptional" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_accountoptional` UNION                                       
 SELECT "profiles_color" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_color` UNION                                                           
 SELECT "profiles_colorpallet" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_colorpallet` UNION                                               
 SELECT "profiles_division" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_division` UNION                                                     
 SELECT "profiles_historicalaccount" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_historicalaccount` UNION                                   
 SELECT "profiles_historicaluser" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_historicaluser` UNION                                         
 SELECT "profiles_industry" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_industry` UNION                                                     
 SELECT "profiles_jobcategory" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_jobcategory` UNION                                               
 SELECT "profiles_joblevel" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_joblevel` UNION                                                     
 SELECT "profiles_online" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_online` UNION                                                         
 SELECT "profiles_profile_divisions" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_profile_divisions` UNION                                   
 SELECT "profiles_savedsearch" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_savedsearch` UNION                                               
 SELECT "profiles_team" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_team` UNION                                                             
 SELECT "profiles_team_managers" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_team_managers` UNION                                           
 SELECT "profiles_team_updates" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_team_updates` UNION                                             
 SELECT "profiles_team_users" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_team_users` UNION                                                 
 SELECT "profiles_user" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_user` UNION                                                             
 SELECT "profiles_user_divisions" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_user_divisions` UNION                                         
 SELECT "profiles_user_groups" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_user_groups` UNION                                               
 SELECT "profiles_user_metrics" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_user_metrics` UNION                                             
 SELECT "profiles_user_update_nodes" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_user_update_nodes` UNION                                   
 SELECT "profiles_user_user_permissions" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_user_user_permissions` UNION                           
 SELECT "profiles_useroptional" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`profiles_useroptional` UNION                                             
 SELECT "revenue_accountreceivable" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`revenue_accountreceivable` UNION                                     
 SELECT "revenue_booking" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`revenue_booking` UNION                                                         
 SELECT "revenue_goalgrouping" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`revenue_goalgrouping` UNION                                               
 SELECT "revenue_historicmetrics" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`revenue_historicmetrics` UNION                                         
 SELECT "revenue_monthlyrevenue" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`revenue_monthlyrevenue` UNION                                           
 SELECT "revenue_newbusiness" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`revenue_newbusiness` UNION                                                 
 SELECT "revenue_renewalinformation" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`revenue_renewalinformation` UNION                                   
 SELECT "revenue_revenuegoal" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`revenue_revenuegoal` UNION                                                 
 SELECT "revenue_saasbooking" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`revenue_saasbooking` UNION                                                 
 SELECT "revenue_saasrevenue" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`revenue_saasrevenue` UNION                                                 
 SELECT "scheduled_jobs_emailsegment" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`scheduled_jobs_emailsegment` UNION                                 
 SELECT "scheduled_jobs_task" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`scheduled_jobs_task` UNION                                                 
 SELECT "scrapbooks_scrap" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`scrapbooks_scrap` UNION                                                       
 SELECT "scrapbooks_scrapbook" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`scrapbooks_scrapbook` UNION                                               
 SELECT "scrapbooks_scrapbook_groups" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`scrapbooks_scrapbook_groups` UNION                                 
 SELECT "scrapbooks_scrapbook_subscriber" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`scrapbooks_scrapbook_subscriber` UNION                         
 SELECT "scrapbooks_scrapbookorder" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`scrapbooks_scrapbookorder` UNION                                     
 SELECT "search_synonym" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`search_synonym` UNION                                                           
 SELECT "site_stats" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`site_stats` UNION                                                                   
 SELECT "smt_articles_article" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`smt_articles_article` UNION                                               
 SELECT "smt_articles_historicalarticle" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`smt_articles_historicalarticle` UNION                           
 SELECT "south_migrationhistory" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`south_migrationhistory` UNION                                           
 SELECT "state" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`state` UNION                                                                             
 SELECT "temp_act" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`temp_act` UNION                                                                       
 SELECT "temp_commentary" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`temp_commentary` UNION                                                         
 SELECT "temp_grid" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`temp_grid` UNION                                                                     
 SELECT "updates_aliasword" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`updates_aliasword` UNION                                                     
 SELECT "updates_category" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`updates_category` UNION                                                       
 SELECT "updates_category_users" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`updates_category_users` UNION                                           
 SELECT "updates_categoryorder" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`updates_categoryorder` UNION                                             
 SELECT "updates_history" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`updates_history` UNION                                                         
 SELECT "updates_update" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`updates_update` UNION                                                           
 SELECT "updates_update_categories" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`updates_update_categories` UNION                                     
 SELECT "updates_update_nodes" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`updates_update_nodes` UNION                                               
 SELECT "vcompany_subsegment" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`vcompany_subsegment` UNION                                                 
 SELECT "videos_video" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`videos_video` UNION                                                               
 SELECT "vproduct_subsegment" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`vproduct_subsegment` UNION                                                 
 SELECT "vreport_subsegment" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`vreport_subsegment` UNION                                                   
 SELECT "welcome_authtoken" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`welcome_authtoken` UNION                                                     
 SELECT "wound_woundcontact" AS table_name, COUNT(*) AS exact_row_count FROM `biomedgps`.`wound_woundcontact`

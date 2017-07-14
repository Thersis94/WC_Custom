-- replace any pre-existing pipes
update biomedgps.profiles_user set address = replace(address,'|',',') where address like '%|%';
update biomedgps.profiles_user set company = replace(company,'|',',') where company like '%|%';
update biomedgps.profiles_user set title = replace(title,'|',',') where title like '%|%';

-- replace special chars in quick notes and company
update biomedgps.profiles_user set quick_notes = regexp_replace(quick_notes, E'[\\n\\r\\f\\t\\v\\a\\b\\e\\0]+', ';;', 'g');
update biomedgps.profiles_user set quick_notes = replace(quick_notes,'"','');

update biomedgps.profiles_user set company = regexp_replace(company, E'[\\n\\r\\f\\t\\v\\a\\b\\e\\0]+', '', 'g');
update biomedgps.profiles_user set company = replace(company,'"','');

update biomedgps.profiles_user set first_name = replace(first_name,'"','');

update biomedgps.profiles_user set address1 = '2132 LS', city = 'Hoofddorp' where city = '2132 LS Hoofddorp';
update biomedgps.profiles_user set address1 = '2132 LS', city = 'Hoofddorp' where city = '2132LS Hoofddorp';

update biomedgps.profiles_user set state = 'CO' where state = ' Colorado';
update biomedgps.profiles_user set state = 'CO' where state = 'Colorado';
update biomedgps.profiles_user set state = 'AL' where state = 'Alabama';
update biomedgps.profiles_user set state = 'AZ' where state = 'Arizona';
update biomedgps.profiles_user set address = 'Norcross', city = 'Atlanta', state = 'GA' where state = 'Atlanta';
update biomedgps.profiles_user set state = 'CA' where state = 'Ca';
update biomedgps.profiles_user set state = 'CA' where state = 'California';
update biomedgps.profiles_user set state = 'FL' where state = 'Florida';
update biomedgps.profiles_user set state = 'GA' where state = 'Georgia';
update biomedgps.profiles_user set state = 'IL' where state = 'Illinois';
update biomedgps.profiles_user set state = 'IN' where state = 'Indiana';
update biomedgps.profiles_user set state = 'KS' where state = 'Kansas';
update biomedgps.profiles_user set state = 'MD' where state = 'Maryland';
update biomedgps.profiles_user set state = 'MA' where state = 'Massachusettes';
update biomedgps.profiles_user set state = 'MA' where state = 'Massachusetts';
update biomedgps.profiles_user set state = 'MI' where state = 'Michigan';
update biomedgps.profiles_user set state = 'MN' where state = 'Minnesota';
update biomedgps.profiles_user set state = 'MN' where state = 'Minnesotta';
update biomedgps.profiles_user set state = 'MO' where state = 'Missouri';
update biomedgps.profiles_user set state = 'MT' where state = 'Montana';
update biomedgps.profiles_user set state = 'NJ' where state = 'New Jersey';
update biomedgps.profiles_user set state = 'NY' where state = 'New York';
update biomedgps.profiles_user set state = 'NY' where state = 'NY - New York';
update biomedgps.profiles_user set state = 'NC' where state = 'North Carolina';
update biomedgps.profiles_user set state = 'OH' where state = 'Ohio';
update biomedgps.profiles_user set state = 'OR' where state = 'Oregon';
update biomedgps.profiles_user set state = 'PA' where state = 'Pennsylvania';
update biomedgps.profiles_user set state = 'SC' where state = 'South Carolina';
update biomedgps.profiles_user set state = 'TN' where state = 'Tennessee';
update biomedgps.profiles_user set state = 'TX' where state = 'Texas';
update biomedgps.profiles_user set state = 'UT' where state = 'Utah';
update biomedgps.profiles_user set state = 'OH' where city = 'Minneapolis' and state = 'USA';
update biomedgps.profiles_user set state = 'VA' where state = 'Virgina';
update biomedgps.profiles_user set state = 'VA' where state = 'Virginai';
update biomedgps.profiles_user set state = 'VA' where state = 'Virginia';
update biomedgps.profiles_user set state = 'VA' where state = 'Virginia ';
update biomedgps.profiles_user set state = 'CA' where state = 'California ';
update biomedgps.profiles_user set state = 'CT' where state = 'Connecticut';
update biomedgps.profiles_user set state = '' where state = 'England';
update biomedgps.profiles_user set state = 'GA' where state = 'Georgia  ';
update biomedgps.profiles_user set state = '', city = 'Helsinki' where state = 'Helsinki';
update biomedgps.profiles_user set state = '', city = 'Hermosa Beach' where state = 'Hermosa Beach';
update biomedgps.profiles_user set state = 'HT' where state = 'Hertfordshire' and country = 'GB';
update biomedgps.profiles_user set state = 'HT' where state = 'Herts' and country = 'GB';

update biomedgps.profiles_user set state = '' where state = 'Israe';
update biomedgps.profiles_user set state = 'KS' where state = 'Kansas ';
update biomedgps.profiles_user set state = 'LA' where state = 'Lancashire';
update biomedgps.profiles_user set state = 'NT' where state = 'Nottinghamshire';
update biomedgps.profiles_user set state = 'ON' where state = 'Ontario';
update biomedgps.profiles_user set state = 'YK' where state = 'South Yorks' and country = 'GB';
update biomedgps.profiles_user set state = 'AN', city = 'Turnhout' where state = 'Turnhout';
update biomedgps.profiles_user set state = 'BW', city = 'Weinheim' where state = 'Weinheim';
update biomedgps.profiles_user set state = 'WS' where state = 'West Sussex';
update biomedgps.profiles_user set state = 'FO' where state = 'Bedfordshire';
update biomedgps.profiles_user set state = 'RG' where state = 'Berkshire';
update biomedgps.profiles_user set state = 'UT', city = 'Bilthoven' where state = 'Bilthoven';

update biomedgps.profiles_user set country = 'AU' where country = 'Australia';
update biomedgps.profiles_user set country = 'DK' where country = 'Denmark';
update biomedgps.profiles_user set country = 'DE' where country = 'Germany';
update biomedgps.profiles_user set country = 'DE' where country = 'Germany ';
update biomedgps.profiles_user set country = 'GB' where country = 'Great Britain';
update biomedgps.profiles_user set country = 'IS' where country = 'Iceland';
update biomedgps.profiles_user set country = 'IL' where country = 'Israel';
update biomedgps.profiles_user set country = 'NL' where country = 'Netherlands';
update biomedgps.profiles_user set country = 'SG' where country = 'Singapore';
update biomedgps.profiles_user set country = 'SE' where country = 'Sweden';
update biomedgps.profiles_user set country = 'CH' where country = 'Switzerland';
update biomedgps.profiles_user set country = 'NL' where country = 'The Netherlands';
update biomedgps.profiles_user set country = 'GB' where country = 'United Kingdom';
update biomedgps.profiles_user set country = 'GB' where country = 'UK';
update biomedgps.profiles_user set country = 'US' where country = 'United States';
update biomedgps.profiles_user set country = 'US' where country = 'United Statess';
update biomedgps.profiles_user set country = 'US' where country = 'united states';
update biomedgps.profiles_user set country = 'US' where country = 'United States of America';
update biomedgps.profiles_user set country = 'US' where country = 'USA';
update biomedgps.profiles_user set country = 'IE' where country = 'Ireland';
update biomedgps.profiles_user set country = 'BR' where country = 'Brazil';
update biomedgps.profiles_user set country = 'IT' where country = 'Italy';
update biomedgps.profiles_user set country = 'ES' where country = 'Spain';
update biomedgps.profiles_user set country = 'GB' where country = 'Unitd Kingdom';
update biomedgps.profiles_user set country = 'GB' where country = 'United Kingdom';
update biomedgps.profiles_user set country = 'FR' where country = 'France';





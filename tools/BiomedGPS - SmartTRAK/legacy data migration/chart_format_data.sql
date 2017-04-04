
update custom.biomedgps_grid_detail 
set value_1_txt = 
case
	when decimal_display_no = 1 then to_char(cast(regexp_replace(value_1_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999d0')
	when decimal_display_no = 2 then to_char(cast(regexp_replace(value_1_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999d00')
	else to_char(cast(regexp_replace(value_1_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999') 
end 
from custom.biomedgps_grid a 
where a.grid_id = biomedgps_grid_detail.grid_id
and a.grid_id like 'CHART%';

update custom.biomedgps_grid_detail 
set value_2_txt = 
case
	when decimal_display_no = 1 then to_char(cast(regexp_replace(value_2_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999d0')
	when decimal_display_no = 2 then to_char(cast(regexp_replace(value_2_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999d00')
	else to_char(cast(regexp_replace(value_2_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999') 
end 
from custom.biomedgps_grid a 
where a.grid_id = biomedgps_grid_detail.grid_id
and a.grid_id like 'CHART%';

update custom.biomedgps_grid_detail 
set value_3_txt = 
case
	when decimal_display_no = 1 then to_char(cast(regexp_replace(value_3_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999d0')
	when decimal_display_no = 2 then to_char(cast(regexp_replace(value_3_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999d00')
	else to_char(cast(regexp_replace(value_3_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999') 
end 
from custom.biomedgps_grid a 
where a.grid_id = biomedgps_grid_detail.grid_id
and a.grid_id like 'CHART%';

update custom.biomedgps_grid_detail 
set value_4_txt = 
case
	when decimal_display_no = 1 then to_char(cast(regexp_replace(value_4_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999d0')
	when decimal_display_no = 2 then to_char(cast(regexp_replace(value_4_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999d00')
	else to_char(cast(regexp_replace(value_4_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999') 
end 
from custom.biomedgps_grid a 
where a.grid_id = biomedgps_grid_detail.grid_id
and a.grid_id like 'CHART%';

update custom.biomedgps_grid_detail 
set value_5_txt = 
case
	when decimal_display_no = 1 then to_char(cast(regexp_replace(value_5_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999d0')
	when decimal_display_no = 2 then to_char(cast(regexp_replace(value_5_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999d00')
	else to_char(cast(regexp_replace(value_5_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999') 
end 
from custom.biomedgps_grid a 
where a.grid_id = biomedgps_grid_detail.grid_id
and a.grid_id like 'CHART%';

update custom.biomedgps_grid_detail 
set value_6_txt = 
case
	when decimal_display_no = 1 then to_char(cast(regexp_replace(value_6_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999d0')
	when decimal_display_no = 2 then to_char(cast(regexp_replace(value_6_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999d00')
	else to_char(cast(regexp_replace(value_6_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999') 
end 
from custom.biomedgps_grid a 
where a.grid_id = biomedgps_grid_detail.grid_id
and a.grid_id like 'CHART%';

update custom.biomedgps_grid_detail 
set value_7_txt = 
case
	when decimal_display_no = 1 then to_char(cast(regexp_replace(value_7_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999d0')
	when decimal_display_no = 2 then to_char(cast(regexp_replace(value_7_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999d00')
	else to_char(cast(regexp_replace(value_7_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999') 
end 
from custom.biomedgps_grid a 
where a.grid_id = biomedgps_grid_detail.grid_id
and a.grid_id like 'CHART%';

update custom.biomedgps_grid_detail 
set value_8_txt = 
case
	when decimal_display_no = 1 then to_char(cast(regexp_replace(value_8_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999d0')
	when decimal_display_no = 2 then to_char(cast(regexp_replace(value_8_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999d00')
	else to_char(cast(regexp_replace(value_8_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999') 
end 
from custom.biomedgps_grid a 
where a.grid_id = biomedgps_grid_detail.grid_id
and a.grid_id like 'CHART%';

update custom.biomedgps_grid_detail 
set value_9_txt = 
case
	when decimal_display_no = 1 then to_char(cast(regexp_replace(value_9_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999d0')
	when decimal_display_no = 2 then to_char(cast(regexp_replace(value_9_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999d00')
	else to_char(cast(regexp_replace(value_9_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999') 
end 
from custom.biomedgps_grid a 
where a.grid_id = biomedgps_grid_detail.grid_id
and a.grid_id like 'CHART%';

update custom.biomedgps_grid_detail 
set value_10_txt = 
case
	when decimal_display_no = 1 then to_char(cast(regexp_replace(value_10_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999d0')
	when decimal_display_no = 2 then to_char(cast(regexp_replace(value_10_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999d00')
	else to_char(cast(regexp_replace(value_10_txt, '[^0-9.]+', '', 'g') as numeric), 'FM9G999G999G999G999G999G999G999G999G999G999G999') 
end 
from custom.biomedgps_grid a 
where a.grid_id = biomedgps_grid_detail.grid_id
and a.grid_id like 'CHART%';

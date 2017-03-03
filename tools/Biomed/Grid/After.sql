insert into biomedgps_grid_detail_type values 
('DATA', 'Chartable Grid Data', 'bs-data', now()),
('UNCHARTED_DATA', 'Non-Chartable Grid Data', 'bs-nochart-data', now()),
('HEADING', 'Heading Row', 'bs-heading', now()),
('SUB_TOTAL', 'Sub-Total Row', 'bs-sub-total', now()),
('TOTAL', 'Total Row', 'bs-total', now());

/** Need this for the widget register for grid charts **/
insert into sb_action (action_id, organization_id, module_type_id, action_nm, create_dt)
values ('7675rhfhrht', 'BMG_SMARTTRAK', 'GRID_DISPLAY_ACTION', 'Placeholder for Grids', null);
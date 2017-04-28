select date(check_dt), count(*) from custom.biomedgps_link group by date(check_dt) order by date(check_dt);

select * from custom.biomedgps_link where create_dt < '2017-04-16' limit 6200;

update custom.biomedgps_link set check_dt='2017-04-04' where link_id in (select link_id from custom.biomedgps_link where create_dt between '2017-04-15' and '2017-04-16' limit 6800);
update custom.biomedgps_link set check_dt='2017-04-05' where link_id in (select link_id from custom.biomedgps_link where create_dt between '2017-04-15' and '2017-04-16' limit 6800);
update custom.biomedgps_link set check_dt='2017-04-06' where link_id in (select link_id from custom.biomedgps_link where create_dt between '2017-04-15' and '2017-04-16' limit 6800);
update custom.biomedgps_link set check_dt='2017-04-07' where link_id in (select link_id from custom.biomedgps_link where create_dt between '2017-04-15' and '2017-04-16' limit 6800);
update custom.biomedgps_link set check_dt='2017-04-08' where link_id in (select link_id from custom.biomedgps_link where create_dt between '2017-04-15' and '2017-04-16' limit 6800);
update custom.biomedgps_link set check_dt='2017-04-09' where link_id in (select link_id from custom.biomedgps_link where create_dt between '2017-04-15' and '2017-04-16' limit 6800);
update custom.biomedgps_link set check_dt='2017-04-10' where link_id in (select link_id from custom.biomedgps_link where create_dt between '2017-04-15' and '2017-04-16' limit 6800);
update custom.biomedgps_link set check_dt='2017-04-11' where link_id in (select link_id from custom.biomedgps_link where create_dt between '2017-04-15' and '2017-04-16' limit 6800);
update custom.biomedgps_link set check_dt='2017-04-12' where link_id in (select link_id from custom.biomedgps_link where create_dt between '2017-04-15' and '2017-04-16' limit 6800);
update custom.biomedgps_link set check_dt='2017-04-13' where link_id in (select link_id from custom.biomedgps_link where create_dt between '2017-04-15' and '2017-04-16' limit 6800);
update custom.biomedgps_link set check_dt='2017-04-14' where link_id in (select link_id from custom.biomedgps_link where create_dt between '2017-04-15' and '2017-04-16' limit 6800);
update custom.biomedgps_link set check_dt='2017-04-16' where link_id in (select link_id from custom.biomedgps_link where create_dt between '2017-04-15' and '2017-04-16' limit 6800);
update custom.biomedgps_link set check_dt='2017-04-17' where link_id in (select link_id from custom.biomedgps_link where create_dt between '2017-04-15' and '2017-04-16' limit 6800);

update custom.biomedgps_link set check_dt='2017-04-15';

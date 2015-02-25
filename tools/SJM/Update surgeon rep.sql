update sitebuilder_custom.dbo.ans_surgeon
set sales_rep_id = cast(b.repobjectid as varchar(32))
from sitebuilder_custom.dbo.ans_surgeon a
inner join obj_phytemp b
on a.surgeon_id = b.objectid 
where cast(repobjectid as varchar(32)) in (select sales_rep_id from sitebuilder_custom.dbo.ans_sales_rep)

package com.depuy.forefront.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.depuy.forefront.action.vo.ActionPlanVO;
import com.depuy.forefront.action.vo.ListItemVO;
import com.depuy.forefront.action.vo.MyActionPlanVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

public class MyActionPlanAction extends SBActionAdapter{
	
	public MyActionPlanAction() {
	}
	
	public MyActionPlanAction(ActionInitVO ai) {
		super(ai);
	}

	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("Beginning retrieve for MyActionPlan");
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		MyActionPlanVO apVo = null;
		
		//all lookups will be done using siteId from the _inst table.
		//this allows us to re-use the same portlet from the shared org for all implementations
		
		StringBuilder sql = new StringBuilder();
		sql.append("select *, d.url_alias_txt as 'stage_url_alias', e.hospital_inst_id as 'ap_hosp_inst' ");
		sql.append("from ").append(customDb).append("FOREFRONT_HOSPITAL a ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_HOSPITAL_INST b on a.hospital_id=b.hospital_id and b.site_id=? ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_PROGRAM c on b.program_id=c.program_id ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_STAGE d on c.program_id=d.program_id ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_ACTION_PLAN e on d.stage_id=e.stage_id and (e.hospital_inst_id is null or e.hospital_inst_id=b.hospital_inst_id) ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_ACTION_PLAN_XR f on e.action_plan_id=f.action_plan_id ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_LIST_ITEM g on f.list_item_id=g.list_item_id ");
		sql.append("order by d.surgery_week_no, e.hospital_inst_id desc, f.order_no, g.list_nm");
		log.debug(sql);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, site.getSiteId());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (apVo == null)
					apVo = new MyActionPlanVO(rs);
				
				ActionPlanVO stageVo = null;
				if (apVo.containsStage(rs.getString("stage_id"))) {
					stageVo = apVo.getStage(rs.getString("stage_id"));
				} else {
					stageVo = new ActionPlanVO(rs); //ActionPlanVO extends StageVO
					stageVo.setUrlAliasText(rs.getString("stage_url_alias"));
					stageVo.setHospitalInstId(StringUtil.checkVal(rs.getString("ap_hosp_inst")));
				}
				
				//add the listItem to the Stage/ActionPlan
				//if the stage is custom, only include customized Items (generic...only generic items)
				if (stageVo.getHospitalInstId().equals(StringUtil.checkVal(rs.getString("ap_hosp_inst")))) 
					stageVo.addItem(new ListItemVO(rs)); //ListItemVO contains the _xr variables
				
				//add the stage
				apVo.addStage(stageVo);
			}
			
		} catch (SQLException sqle) {
			log.error("could not load MyActionPlan", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		if (apVo == null) apVo = new MyActionPlanVO();
		log.debug("loaded " + apVo.getStages().size() + " stages");
		
		//Set Wrapper in the actionData for the view.
		mod.setActionData(apVo);
		mod.setCacheGroups(new String[] { AdminFacadeAction.CACHE_GROUP, site.getSiteId(), mod.getModuleType() });
		log.debug(StringUtil.getToString(mod.getCacheGroups(), false, true, ","));
	}
	
}

package com.depuy.forefront.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.depuy.forefront.action.vo.MyTreatCalVO;
import com.depuy.forefront.action.vo.TreatCalVO;
import com.depuy.forefront.action.vo.TreatCalItemVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

public class MyTreatCalAction extends SBActionAdapter {
	
	public MyTreatCalAction() {
	}
	
	public MyTreatCalAction(ActionInitVO ai) {
		super(ai);
	}

	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("Beginning retrieve for MyTreatmentCalendar");
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		MyTreatCalVO tcVo = null;
		
		//all lookups will be done using siteId from the _inst table.
		//this allows us to re-use the same portlet from the shared org for all implementations
		
		StringBuilder sql = new StringBuilder();
		sql.append("select *, d.url_alias_txt as 'stage_url_alias', g.summary_txt as 'item_summary_txt', e.hospital_inst_id as 'cal_hosp_inst' ");
		sql.append("from ").append(customDb).append("FOREFRONT_HOSPITAL a ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_HOSPITAL_INST b on a.hospital_id=b.hospital_id and b.site_id=? ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_PROGRAM c on b.program_id=c.program_id ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_STAGE d on c.program_id=d.program_id ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_TREAT_CAL e on d.stage_id=e.stage_id and (e.hospital_inst_id is null or e.hospital_inst_id=b.hospital_inst_id) ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_TREAT_CAL_XR f on e.treat_cal_id=f.treat_cal_id ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_TREAT_CAL_ITEM g on f.treat_cal_item_id=g.treat_cal_item_id ");
		sql.append("order by d.surgery_week_no, e.hospital_inst_id desc, f.order_no, g.entry_nm");
		log.debug(sql);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, site.getSiteId());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (tcVo == null)
					tcVo = new MyTreatCalVO(rs);
				
				TreatCalVO stageVo = null;
				if (tcVo.containsStage(rs.getString("stage_id"))) {
					stageVo = tcVo.getStage(rs.getString("stage_id"));
				} else {
					stageVo = new TreatCalVO(rs); //ActionPlanVO extends StageVO
					stageVo.setUrlAliasText(rs.getString("stage_url_alias"));
					stageVo.setHospitalInstId(StringUtil.checkVal(rs.getString("cal_hosp_inst")));
				}
				
				//add the listItem to the Stage/ActionPlan
				//if the stage is custom, only include customized Items (generic...only generic items)
				if (rs.getString("treat_cal_item_id") != null && 
						stageVo.getHospitalInstId().equals(StringUtil.checkVal(rs.getString("cal_hosp_inst")))) 
					stageVo.addItem(new TreatCalItemVO(rs)); //TreatCalItemVO contains the _xr variables
				
				//add the stage
				tcVo.addStage(stageVo);
			}
			
		} catch (SQLException sqle) {
			log.error("could not load MyActionPlan", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		if (tcVo == null) tcVo = new MyTreatCalVO();
		log.debug("loaded " + tcVo.getStages().size() + " stages");
		
		//Set Wrapper in the actionData for the view.
		mod.setActionData(tcVo);
		mod.setCacheGroups(new String[] { AdminFacadeAction.CACHE_GROUP, site.getSiteId(), mod.getModuleType() });
	}
}

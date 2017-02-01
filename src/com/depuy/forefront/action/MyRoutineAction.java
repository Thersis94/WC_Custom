package com.depuy.forefront.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Enumeration;

import com.depuy.forefront.action.vo.ExerciseAttributeVO;
import com.depuy.forefront.action.vo.ExerciseIntensityVO;
import com.depuy.forefront.action.vo.MyRoutineVO;
import com.depuy.forefront.action.vo.RoutineVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;


public class MyRoutineAction extends SBActionAdapter {
	
	public MyRoutineAction() {
	}
	
	public MyRoutineAction(ActionInitVO ai) {
		super(ai);
	}

	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("Beginning retrieve for MyRoutine");
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		MyRoutineVO rVo = null;
		
		//all lookups will be done using siteId from the _inst table.
		//this allows us to re-use the same portlet from the shared org for all implementations
		
		StringBuilder sql = new StringBuilder();
		sql.append("select *, d.url_alias_txt as 'stage_url_alias', e.hospital_inst_id as 'rout_hosp_inst' ");
		sql.append("from ").append(customDb).append("FOREFRONT_HOSPITAL a ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_HOSPITAL_INST b on a.hospital_id=b.hospital_id and b.site_id=? ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_PROGRAM c on b.program_id=c.program_id ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_STAGE d on c.program_id=d.program_id ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_ROUTINE e on d.stage_id=e.stage_id and (e.hospital_inst_id is null or e.hospital_inst_id=b.hospital_inst_id)  ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_EXERCISE_ROUTINE_XR f on e.routine_id=f.routine_id ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_EXERCISE_INTENSITY g on f.exercise_intensity_id=g.exercise_intensity_id ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_EXERCISE h on g.exercise_id=h.exercise_id ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_EXERCISE_ATTRIBUTE i on g.exercise_intensity_id=i.exercise_intensity_id ");
		sql.append("order by d.surgery_week_no, e.hospital_inst_id desc, f.order_no, h.exercise_nm");
		log.debug(sql);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, site.getSiteId());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (rVo == null)
					rVo = new MyRoutineVO(rs);
				
				//load the stage/Routine
				RoutineVO stageVo = null;
				if (rVo.containsStage(rs.getString("stage_id"))) {
					stageVo = rVo.getStage(rs.getString("stage_id"));
				} else {
					stageVo = new RoutineVO(rs); //ActionPlanVO extends StageVO
					stageVo.setUrlAliasText(rs.getString("stage_url_alias"));
					stageVo.setHospitalInstId(StringUtil.checkVal(rs.getString("rout_hosp_inst")));
				}
				

				//if the stage is custom, only include customized Items (generic...only generic items)
				if (stageVo.getHospitalInstId().equals(StringUtil.checkVal(rs.getString("rout_hosp_inst")))) { 
					
					//load the Exercise/Intensity
					ExerciseIntensityVO exerVo = null;
					if (stageVo.containsExercise(rs.getString("exercise_intensity_id"))) {
						exerVo = stageVo.getExercise(rs.getString("exercise_intensity_id"));
					} else {
						exerVo = new ExerciseIntensityVO(rs);
					}
					
					//add the attribute to the Exercise
					ExerciseAttributeVO attrVo = new ExerciseAttributeVO(rs);
					exerVo.addListAttribute(attrVo);
					
					//add the Exercise to the stage
					stageVo.addExercise(exerVo);
				}
				
				//add the Stage to the Program
				rVo.addStage(stageVo);
			}
			
		} catch (SQLException sqle) {
			log.error("could not load MyActionPlan", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		if (rVo == null) rVo = new MyRoutineVO();
		log.debug("loaded " + rVo.getStages().size() + " stages");
		
		//Set Wrapper in the actionData for the view.
		mod.setActionData(rVo);
		mod.setCacheGroups(new String[] { AdminFacadeAction.CACHE_GROUP, site.getSiteId(), mod.getModuleType() });
	}
	
	
	@SuppressWarnings("rawtypes")
	public void build(ActionRequest req) throws ActionException {
		StringBuilder sql = new StringBuilder();
		sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("FOREFRONT_EXERCISE_LOG (ATTRIBUTE_ID, PROFILE_ID, STAGE_ID, ");
		sql.append("VALUE_TXT, SET_NO, CREATE_DT, WORKOUT_DT, EXERCISE_LOG_ID) ");
		sql.append("values(?,?,?,?,?,?,?,?)");
		
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		if (user == null) throw new ActionException("cannot log transaction, user is not logged in");
		
		Date workoutDt = Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("workoutDt"));
		if (workoutDt == null) workoutDt = new Date();
		
		log.debug(req.getQueryString());
		Enumeration e = req.getParameterNames();
		
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sql.toString());
			
			while (e.hasMoreElements()) {
				String paramNm = (String) e.nextElement();
				if (paramNm.startsWith("attr_") && req.getParameter(paramNm).length() > 0) {
						int lastDelim = paramNm.lastIndexOf("_");
						
						ps.setString(1, paramNm.substring(lastDelim+1));
						ps.setString(2, user.getProfileId());
						ps.setString(3, req.getParameter("stageId"));
						ps.setString(4, req.getParameter(paramNm));
						ps.setInt(5, Convert.formatInteger(paramNm.substring(paramNm.indexOf("_"), lastDelim),1));
						ps.setTimestamp(6, Convert.getCurrentTimestamp());
						ps.setDate(7, Convert.formatSQLDate(workoutDt));
						ps.setString(8, new UUIDGenerator().getUUID());
						//log.debug("added to batch: " + paramNm + "=" + req.getParameter(paramNm));
						ps.addBatch();
				}
			}
			ps.executeBatch();
			
		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
			try { ps.close(); } catch (Exception ex) {}
		}
	
	}
}

package com.depuy.forefront.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

import com.depuy.forefront.action.vo.ExerciseIntensityVO;
import com.depuy.forefront.action.vo.MyProgressVO;
import com.depuy.forefront.action.vo.MyProgressVO.DetailVO;
import com.depuy.forefront.action.vo.MyRoutineVO;
import com.depuy.forefront.action.vo.RoutineVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

public class MyProgressAction extends SBActionAdapter {
	
	public MyProgressAction() {
	}
	
	public MyProgressAction(ActionInitVO ai) {
		super(ai);
	}

	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("Beginning retrieve for MyProgress");
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		Boolean isPreSurgery = req.hasParameter("preSurgery");
		Date surgeryDt = (Date)req.getSession().getAttribute("surgeryDate");
		if (surgeryDt == null) surgeryDt = new Date();
		
		//get the routine for this program
		MyRoutineAction mra = new MyRoutineAction(actionInit);
		mra.setAttributes(attributes);
		mra.setDBConnection(dbConn);
		mra.retrieve(req);
		mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		MyRoutineVO rVo = (MyRoutineVO) mod.getActionData();

		//loop the routine into a list of ExerciseIds that we need to report on.
		//each Exercise will populate 7 days worth of reporting data; for each stage/intensity its bound at.
		Map<String, MyProgressVO> exerciseMap = new HashMap<String, MyProgressVO>();
		for (RoutineVO stage : rVo.getStages()) {
			for (ExerciseIntensityVO eVo : stage.getExercises()) {
				if ((isPreSurgery && stage.getSurgeryWeekNo() < 0) || (!isPreSurgery && stage.getSurgeryWeekNo() > 0)) {
					MyProgressVO vo = exerciseMap.get(eVo.getExerciseId());
					if (vo == null)
						vo = new MyProgressVO(eVo.getExerciseName());
					
					vo.addStage(eVo.getStage().getSurgeryWeekNo(), eVo.getReqSetsNo());
					exerciseMap.put(eVo.getExerciseId(), vo);
				}
			}
		}
		
		//load the _LOG data for these exercises (metrics recorded for this user)
		StringBuilder sql = new StringBuilder();
		sql.append("select a.exercise_id, a.exercise_nm, b.req_sets_no, ");
		sql.append("abs(datediff(dd,d.workout_dt,?)) as 'day_offset', count(d.value_txt) as 'entries' ");
		sql.append("from ").append(customDb).append("FOREFRONT_EXERCISE a inner join ");
		sql.append(customDb).append("FOREFRONT_EXERCISE_INTENSITY b on a.exercise_id=b.exercise_id ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_EXERCISE_ATTRIBUTE c on b.exercise_intensity_id=c.exercise_intensity_id ");
		sql.append("left outer join ").append(customDb).append("FOREFRONT_EXERCISE_LOG d on c.attribute_id=d.attribute_id and d.profile_id=? ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_STAGE e on d.stage_id=e.stage_id and e.surgery_week_no ").append((isPreSurgery ? "< 0" : "> 0"));
		sql.append(" where c.label_txt in ('# REPS','Distance') "); //for now, this is the best gauge of "is this exercise complete"
		sql.append("group by a.exercise_id, a.exercise_nm, b.req_sets_no, d.workout_dt");
		log.debug(sql + user.getProfileId() + surgeryDt);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setDate(1, Convert.formatSQLDate(surgeryDt));
			ps.setString(2, user.getProfileId());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				MyProgressVO vo = exerciseMap.get(rs.getString("exercise_id"));
				if (vo == null) {
					log.error("THIS SHOULD NEVER BE REACHED! exerciseId=" + rs.getString("exercise_id"));
					continue;
				}				
				int offset = rs.getInt("day_offset")-1;
				if (isPreSurgery) offset = Math.abs(28-offset)-1;
				
				vo.setRepsCompleted(offset, rs.getInt("entries"));
				
				exerciseMap.put(rs.getString("exercise_id"), vo);
			}
			
		} catch (SQLException sqle) {
			log.error("could not load MyProgress", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
				
		//Set Wrapper in the actionData for the view.
		log.debug(exerciseMap.size());
		List<MyProgressVO> l = new ArrayList<MyProgressVO>(exerciseMap.values());
		Collections.sort(l, new MyProgressComparator());
		mod.setActionData(l);
		
		//iterate and assemble the "Overall Progress" graph
		MyProgressVO overall = new MyProgressVO(null);  //name is hard-coded in the View
		
		for (MyProgressVO vo : l) {  //loop the 28 days of data for each of the Exercises
			Map<Integer, DetailVO> stats = vo.getCompleted();
			for (Integer x : stats.keySet()) {
				DetailVO mStats = overall.getDetails(x) != null ? overall.getDetails(x) : overall.new DetailVO(x,0,0);
				DetailVO xStats = stats.get(x);
				if (xStats == null) continue; //nothing to update!
				
				overall.setCompletedIdx(x, Convert.formatInteger(mStats.reqSetsNo) + Convert.formatInteger(xStats.reqSetsNo), Convert.formatInteger(mStats.setsCompleted) + Convert.formatInteger(xStats.setsCompleted));
			}
		}
		req.setAttribute("overall", overall);
		
	}
	
	/*
	 * simple String Comparator that returns the alphabetical order of VOs by exercise name
	 */
	public class MyProgressComparator implements Comparator<MyProgressVO> {
		public int compare(MyProgressVO o1, MyProgressVO o2) {
			return o1.getExerciseName().compareTo(o2.getExerciseName());
		}
		
	}
	
}

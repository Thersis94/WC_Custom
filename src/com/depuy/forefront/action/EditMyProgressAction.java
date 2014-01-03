package com.depuy.forefront.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Comparator;
import java.util.List;

import com.depuy.forefront.action.vo.ExerciseAttributeVO;
import com.depuy.forefront.action.vo.MyProgressVO;
import com.depuy.forefront.action.vo.MyRoutineVO;
import com.depuy.forefront.action.vo.RoutineVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

public class EditMyProgressAction extends SBActionAdapter {
	 public final static int MILLIS_IN_DAY = 1000 * 60 * 60 * 24;  

	public EditMyProgressAction() {
	}
	
	public EditMyProgressAction(ActionInitVO ai) {
		super(ai);
	}
	
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("Beginning build for MyProgress");
		
		/*
		 * Prepare the date for the MyRoutineAction, Store old format for use later.
		 */
		String oldWorkoutdt = (String)req.getParameter("workoutDt");
		Date currentDt = Convert.formatDate("MM/dd/yyyy", oldWorkoutdt);
		log.debug("Formatted date: " + Convert.formatDate(currentDt, Convert.DATE_SLASH_PATTERN));
		req.setParameter("workoutDt", Convert.formatDate(currentDt, Convert.DATE_SLASH_PATTERN));
		
		/*
		 * Forward to MyRoutineAction
		 */
		SBActionAdapter sb = new MyRoutineAction(this.actionInit);
		sb.setDBConnection(dbConn);
		sb.setAttributes(attributes);
		sb.build(req);
		
		/*
		 * Redirect with old date format so retrieve returns current page.
		 */
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		this.sendRedirect(page.getFullPath() + "?workoutDt=" + oldWorkoutdt, null, req);
		
	}

	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("Beginning retrieve for MyProgress");
		
		//Initialise local variables
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		String dir = StringUtil.checkVal(req.getParameter("direction"));
		//log.debug((String)req.getParameter("workoutDt") + " | " + (Date)req.getSession().getAttribute("surgeryDate"));
		
		//Initialize surgery Date variable for comparison
		Date surgeryDt = (Date)req.getSession().getAttribute("surgeryDate");
		if (surgeryDt == null) surgeryDt = new Date();
		
		//Initialise current Date off form for our relationship to the surgery Date
		Date currentDt = Convert.formatDate("MM/dd/yyyy", (String)req.getParameter("workoutDt"));
		if (currentDt == null) currentDt = new Date();
		log.debug("formatted: " + currentDt + " " + surgeryDt);
		log.debug(Convert.formatDate(currentDt, Convert.DATE_SLASH_PATTERN));
		
		//If we're navigating the dates, check the direction parameter and adjest current date accordingly.
		if(dir != null && dir.equals("prev")){
			currentDt.setTime(currentDt.getTime() - MILLIS_IN_DAY);
		} else if(dir != null && dir.equals("next")){
			currentDt.setTime(currentDt.getTime() + MILLIS_IN_DAY);
		}
		//set the new date on the request
		req.setParameter("workoutDt", Convert.formatDate(currentDt, Convert.DATE_SLASH_PATTERN));
		
		//get the routine for this program
		MyRoutineAction mra = new MyRoutineAction(actionInit);
		mra.setAttributes(attributes);
		mra.setDBConnection(dbConn);
		mra.retrieve(req);
		mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		MyRoutineVO rVo = (MyRoutineVO) mod.getActionData();
		
		//Get the Proper Routine
		RoutineVO v = (RoutineVO) getStage(rVo, currentDt, surgeryDt);
		
		//load the _LOG data for these exercises (metrics recorded for this user)
		StringBuilder sql = new StringBuilder();
		sql.append("select a.EXERCISE_ID, b.EXERCISE_INTENSITY_ID, ");
		sql.append("c.ATTRIBUTE_ID, d.VALUE_TXT, d.SET_NO, max(d.CREATE_DT) as CREATE_DT ");
		sql.append("from ").append(customDb).append("FOREFRONT_EXERCISE a inner join ");
		sql.append(customDb).append("FOREFRONT_EXERCISE_INTENSITY b on a.exercise_id=b.exercise_id ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_EXERCISE_ATTRIBUTE c on b.exercise_intensity_id=c.exercise_intensity_id ");
		sql.append("left outer join ").append(customDb).append("FOREFRONT_EXERCISE_LOG d on c.attribute_id=d.attribute_id and d.profile_id=? ");
		sql.append("inner join ").append(customDb).append("FOREFRONT_STAGE e on d.stage_id=e.stage_id ");
		sql.append(" where d.workout_dt = ? "); //for now, this is the best gauge of "is this exercise complete"
		sql.append("group by a.EXERCISE_ID, b.EXERCISE_INTENSITY_ID, c.ATTRIBUTE_ID, d.VALUE_TXT, d.SET_NO, d.CREATE_DT ");
		sql.append("order by SET_NO, create_dt desc");
		log.debug(sql + " | " + user.getProfileId() + " | " + currentDt);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, user.getProfileId());
			ps.setDate(2, Convert.formatSQLDate(currentDt));
			ResultSet rs = ps.executeQuery();
			
			//Load all the log data into Routine view
			while (rs.next()) {
				List<ExerciseAttributeVO> vos = v.getExercise(rs.getString("EXERCISE_INTENSITY_ID")).getListAttributes();
				for(ExerciseAttributeVO vo : vos){
					if(vo.getExerciseAttributeId().equals(rs.getString("ATTRIBUTE_ID"))){
						if(!vo.getValues().containsKey(rs.getInt("SET_NO")))
							vo.getValues().put(rs.getInt("SET_NO"), rs.getString("VALUE_TXT"));
					}
				}
			}
			
		}catch (SQLException sqle) {
			log.error("could not load MyProgress", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
				
		//Set Wrapper in the actionData for the view.
		mod.setActionData(v);
		
	}
	
	public RoutineVO getStage(MyRoutineVO rVo, Date stageDt, Date surgeryDt){
		//dynamically find list midpoint for the offsets.
		int size = rVo.getStages().size()/2;
		
		//store times as long format
		Long surgTime = surgeryDt.getTime();
		Long currentTime = stageDt.getTime();
		
		int diff = calcTimeDiff(surgTime, currentTime);
		
		//Place clamps on high and low to avoid going out of bounds.
		if(diff > size) diff = size;
		if(diff < (-1 * size)) diff = (-1 * size);
		log.debug("The offset is: " + diff);
		return rVo.getStages().get(size + diff);
	}
	
	public static final int calcTimeDiff(Long surgTime, Long currentTime) {
		//difference in days between dates.
		Long d = (currentTime - surgTime) / MILLIS_IN_DAY;
		log.debug("curr=" + currentTime + " surg=" + surgTime + " diff=" + d);
		
		//normalize for weeks.
		Long diff = d / 7;
		
		//check for partial weeks (we want to subract one if before surgery, otherwise keep the same as hospital stay is a week long.
		if((d % 7) != 0)
			diff += (diff < 0) ? -1 : 0;
		
		return diff.intValue();
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

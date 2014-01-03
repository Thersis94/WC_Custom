package com.depuy.forefront.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.depuy.forefront.action.vo.ExerciseIntensityVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

public class ExerciseAssocAction extends SBActionAdapter {

	public ExerciseAssocAction() {
		super();
	}
	public ExerciseAssocAction(ActionInitVO ai) {
		super(ai);
	}
	
	public void delete(SMTServletRequest req) {
	}
	
	
	public void retrieve(SMTServletRequest req) throws ActionException{
		log.debug("Beginning ExerciseAssocAction retrieve");
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String programId = (String) req.getSession().getAttribute(ProgramAction.PROGRAM_ID);
		String hospitalId = StringUtil.checkVal(req.getSession().getAttribute("hospitalId"));
		String routineId = StringUtil.checkVal(req.getParameter("routineId"));
		Map<String, ExerciseIntensityVO> data = new LinkedHashMap<String, ExerciseIntensityVO>();
		
		StringBuilder sb = new StringBuilder();
		String orderBy = "EXERCISE_NM, INTENSITY_LEVEL_NO";
		sb.append("select INTENSITY_LEVEL_NO, b.EXERCISE_INTENSITY_ID, a.EXERCISE_ID, ");
		sb.append("a.EXERCISE_NM, c.EXERCISE_ROUTINE_XR_ID, c.order_no ");
		sb.append("from ").append(customDb).append("FOREFRONT_EXERCISE a ");
		sb.append("inner join ").append(customDb).append("FOREFRONT_EXERCISE_INTENSITY b ");
		sb.append("on a.EXERCISE_ID = b.EXERCISE_ID ");
		if (req.hasParameter("rearrange")) {
			sb.append("inner join ");
			orderBy = "c.order_no, " + orderBy;
		} else {
			sb.append("left outer join ");
		}
		sb.append(customDb).append("FOREFRONT_EXERCISE_ROUTINE_XR c ");
		sb.append("on b.EXERCISE_INTENSITY_ID=c.EXERCISE_INTENSITY_ID and c.ROUTINE_ID=? ");
		sb.append("where a.PROGRAM_ID = ? and (a.HOSPITAL_ID is NULL or a.HOSPITAL_ID = ?) ");
		sb.append("order by ").append(orderBy);
		log.debug(sb);

		PreparedStatement ps = null;
		int i = 1;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(i++, routineId);
			ps.setString(i++, programId);
			ps.setString(i++, hospitalId);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				data.put(rs.getString("exercise_intensity_id"), new ExerciseIntensityVO(rs));
			
		} catch (SQLException sqle) {
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		log.debug("Retrieved " + data.size() + " Exercises");
		mod.setActionData(new ArrayList<ExerciseIntensityVO>(data.values()));
	}
	

	private void updateRoutineAssoc(SMTServletRequest req) throws ActionException {
		log.debug("Beginning RoutineAction update");
		String msg = "Exercise Added to Routine Successfully";
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		ExerciseIntensityVO vo = new ExerciseIntensityVO(req);

		StringBuilder sb = new StringBuilder();
		PreparedStatement ps = null;
		
		//delete all existing routine associations
		sb.append("delete from ").append(customDb).append("FOREFRONT_EXERCISE_ROUTINE_XR where routine_id=?");
		log.debug(sb);
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, vo.getRoutineId());
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			log.error("could not delete old routine exercises", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		
		//loop and insert all as new
		sb = new StringBuilder("insert into ").append(customDb);
		sb.append("FOREFRONT_EXERCISE_ROUTINE_XR (ROUTINE_ID, EXERCISE_INTENSITY_ID, ");
		sb.append("ORDER_NO, CREATE_DT, EXERCISE_ROUTINE_XR_ID) values(?,?,?,?,?)");
		log.debug(sb);
		
		UUIDGenerator uuid = new UUIDGenerator();
		try {
			ps = dbConn.prepareStatement(sb.toString());
			Enumeration<String> names = req.getParameterNames();
			while (names.hasMoreElements()) {
				String s = names.nextElement();
				if (!s.startsWith("intensityId")) continue;
				
				String[] nmArr = s.split("~");
				if (req.getParameter(s).length() == 0) continue;
				
				int orderNo = (nmArr.length > 1) ? Convert.formatInteger(nmArr[1]) : 1;
				
				ps.setString(1, vo.getRoutineId());
				ps.setString(2, req.getParameter(s));
				ps.setInt(3, orderNo);
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				ps.setString(5, uuid.getUUID());
				ps.addBatch();
			}
			ps.executeBatch();
			
		} catch (SQLException sqle) {
			msg = "An error occured while saving the routine.";
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		super.sendRedirect(page.getFullPath() + "?actionType=7", msg, req);
	}
	
	
	
	private void reorderRoutine(SMTServletRequest req) throws ActionException{
		log.debug("Beginning Routine Reorder update");
		String msg = "Routine ReOrdered Successfully";
		List<ExerciseIntensityVO> vos = new ArrayList<ExerciseIntensityVO>();
		int i = 0;
		ExerciseIntensityVO v = getNextUpdate(req, i);
		
		while (v != null) {
			vos.add(v);
			i++;
			v = getNextUpdate(req, i);
			log.debug(i + ": " + v);
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FOREFRONT_EXERCISE_ROUTINE_XR set ORDER_NO = ?, UPDATE_DT = ? where EXERCISE_ROUTINE_XR_ID = ?");
		log.debug(sb);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			for (ExerciseIntensityVO vo : vos) {
				ps.setInt(1, vo.getOrderNo());
				ps.setTimestamp(2, Convert.getCurrentTimestamp());
				ps.setString(3, vo.getExerciseRoutineId());
				ps.addBatch();
			}
			ps.executeBatch();
			
		} catch (SQLException sqle) {
			msg = "A problem occured when adding the routine.";
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}

		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		super.sendRedirect(page.getFullPath() + "?actionType=7", msg, req);
	}
	

	
	private ExerciseIntensityVO getNextUpdate(SMTServletRequest req, int i) {
		String ers = StringUtil.checkVal(req.getParameter("exerciseRoutineId_" + i));
		log.debug(i + " : " + ers);
		if (ers.length() == 0) return null;
		
		req.setParameter("exerciseRoutineId", ers.substring(0, ers.indexOf('~')));
		req.setParameter("orderNo", ers.substring(ers.indexOf('~')));
		
		return new ExerciseIntensityVO(req);
	}
	
	
	/**
	 * when a customized routine is added for a hosp-instance, we want to clone the program's 
	 * default exercise _XR records.  The hospital can still go in and customize their
	 * routine lists, but at least this gives them a place to start.
	 * This method gets called from RoutineAction, when a new, hosp-specific Routine is created.
	 * @param req
	 * @throws ActionException
	 */
	protected void cloneRoutine(String origRoutine, String newRoutine) throws ActionException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("insert into ").append(customDb).append("FOREFRONT_EXERCISE_ROUTINE_XR ");
		sql.append("(EXERCISE_ROUTINE_XR_ID,ROUTINE_ID,EXERCISE_INTENSITY_ID,ORDER_NO,CREATE_DT) ");
		sql.append("select replace(newid(),'-',''),?,EXERCISE_INTENSITY_ID,ORDER_NO,? ");
		sql.append("from ").append(customDb).append("FOREFRONT_EXERCISE_ROUTINE_XR ");
		sql.append("where routine_id=?");
		log.debug(sql);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, newRoutine);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, origRoutine);
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}	
	

	public void build(SMTServletRequest req) throws ActionException {
		if (Boolean.parseBoolean(req.getParameter("reorder"))) {
			reorderRoutine(req);
		} else if (Boolean.parseBoolean(req.getParameter("assoc"))) {
			updateRoutineAssoc(req);
		}
	}
}

package com.depuy.forefront.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.depuy.forefront.action.vo.ExerciseAttributeVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

public class ExerciseAttributeAction extends SBActionAdapter {
	public ExerciseAttributeAction() {
		super();
	}

	public ExerciseAttributeAction(ActionInitVO ai) {
		super(ai);
	}

	public void delete(SMTServletRequest req) {
		return;
	}

	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("Beginning ExerciseAttributeAction retrieve");
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		List<ExerciseAttributeVO> data = new ArrayList<ExerciseAttributeVO>();
		String exerciseId = req.getParameter("exerciseIntensityId");
		
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FOREFRONT_EXERCISE_ATTRIBUTE where EXERCISE_INTENSITY_ID = ?");
		log.debug(sb);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, exerciseId);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				data.add(new ExerciseAttributeVO(rs));
			
		} catch (SQLException sqle) {
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}

		log.debug("Retrieved " + data.size() + "ExerciseAttributes");
		mod.setActionData(data);
	}
	
	private List<ExerciseAttributeVO> loadAttributes(SMTServletRequest req){
		int count= 1;
		List<ExerciseAttributeVO> vos = new ArrayList<ExerciseAttributeVO>();
		ExerciseAttributeVO vo = new ExerciseAttributeVO(req, 0, count);
		
		while(StringUtil.checkVal(vo.getLabelText()).length() > 0) {
			vos.add(vo);
			count++;
			vo = new ExerciseAttributeVO(req, 0, count);
		}
		return vos;
	}

	private void updateExerciseAttribute(SMTServletRequest req) throws ActionException {
		log.debug("Beginning ExerciseAttributeAction update");
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		List<ExerciseAttributeVO> vos = loadAttributes(req);
		
		for (ExerciseAttributeVO vo : vos) {
			StringBuffer sql = new StringBuffer();	

			// Create the statement based upon whether a pkId is present
			if (vo.getExerciseAttributeId() == null || vo.getExerciseAttributeId().length() == 0) {
				sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
				sql.append("FOREFRONT_EXERCISE_ATTRIBUTE (EXERCISE_INTENSITY_ID, LABEL_TXT, ");
				sql.append("UNIT_TXT, HTML_TYPE_NM, DEFAULT_VALUE_TXT, CREATE_DT, ");
				sql.append("ATTRIBUTE_ID) values (?,?,?,?,?,?,?)");
				vo.setExerciseAttributeId(new UUIDGenerator().getUUID());
			} else {
				sql.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
				sql.append("FOREFRONT_EXERCISE_ATTRIBUTE set EXERCISE_INTENSITY_ID=?, LABEL_TXT=?, ");
				sql.append("UNIT_TXT=?, HTML_TYPE_NM=?, DEFAULT_VALUE_TXT=?, UPDATE_DT=? ");
				sql.append("where ATTRIBUTE_ID=?");
			}
			log.debug("FOREFRONT_EXERCISE_ATTRIBUTE Sql= " + sql.toString() + vo.getLabelText());
			
			PreparedStatement ps = null;
			try {
				ps = dbConn.prepareStatement(sql.toString());
				ps.setString(1, vo.getExerciseIntensityId());
				ps.setString(2, vo.getLabelText());
				ps.setString(3, vo.getUnitText());
				ps.setString(4, vo.getHtmlTypeName());
				ps.setString(5, vo.getDefaultValueText());
				ps.setTimestamp(6, Convert.getCurrentTimestamp());
				ps.setString(7, vo.getExerciseAttributeId());
	
				if (ps.executeUpdate() < 1)
					throw new ActionException("No records updated, "+ ps.getWarnings());
	
			} catch (SQLException sqle) {
				log.error(sqle);
				throw new ActionException(sqle);
			} finally {
				try { ps.close(); } catch (Exception e) {}
			}
			
			req.setAttribute("attributeId", vo.getExerciseAttributeId());
		}
		mod.setActionData(vos);	
	}
	
	public void build(SMTServletRequest req) throws ActionException {
		updateExerciseAttribute(req);
	}
}

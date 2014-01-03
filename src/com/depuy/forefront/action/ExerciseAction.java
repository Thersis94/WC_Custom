package com.depuy.forefront.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.depuy.forefront.action.vo.ExerciseAttributeVO;
import com.depuy.forefront.action.vo.ExerciseIntensityVO;
import com.depuy.forefront.action.vo.ExerciseVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.smt.sitebuilder.action.FileLoader;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

public class ExerciseAction extends SBActionAdapter {

	private String msg = null;
	
	public ExerciseAction() {
		super();
	}
	public ExerciseAction(ActionInitVO ai) {
		super(ai);
	}
	
	public void delete(SMTServletRequest req) {
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		msg = (String) getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		String pkId = req.getParameter("delId");

		String[] sql = new String[2];
		sql[0] = "delete from " + customDb + "forefront_exercise_intensity where exercise_id=?";
		sql[1] = "delete from " + customDb + "forefront_exercise where exercise_id=?";
		
		PreparedStatement ps = null;
		try {
			for (String s : sql) {
				ps = dbConn.prepareStatement(s);
				ps.setString(1, pkId);
				ps.execute();
			}
		} catch (SQLException sqle) {
			log.error("could not delete exercise", sqle);
			msg = (String) getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException{
		log.debug("Beginning ExerciseAction retrieve");
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		String programId = (String) req.getSession().getAttribute(ProgramAction.PROGRAM_ID);
		String hospitalId = StringUtil.checkVal(req.getSession().getAttribute("hospitalId"));
		String exerciseId = StringUtil.checkVal(req.getParameter("exerciseId"));
		String exerciseIntensityId = StringUtil.checkVal(req.getParameter("exerciseIntensityId"));
		List<ExerciseVO> vos = new ArrayList<ExerciseVO>();
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(customDb).append("FOREFRONT_EXERCISE a ");
		sb.append("left outer join ").append(customDb).append("FOREFRONT_EXERCISE_INTENSITY b on a.EXERCISE_ID=b.EXERCISE_ID ");
		sb.append("left outer join ").append(customDb).append("FOREFRONT_EXERCISE_ATTRIBUTE c on b.EXERCISE_INTENSITY_ID=c.EXERCISE_INTENSITY_ID ");
		sb.append("left outer join ").append(customDb).append("FOREFRONT_HOSPITAL d on a.hospital_id=d.hospital_id ");
		sb.append("where PROGRAM_ID = ?");
		
		if (hospitalId.length() > 0) {
			sb.append(" and a.HOSPITAL_ID = ?");
		} else {
			sb.append(" and a.HOSPITAL_ID is null");
		}
		
		if (exerciseId.length() > 0)
			sb.append(" and a.EXERCISE_ID = ?");
		
		if (exerciseIntensityId.length() > 0)
			sb.append(" and b.EXERCISE_INTENSITY_ID = ?");
		
		sb.append(" order by a.EXERCISE_NM, b.INTENSITY_LEVEL_NO, c.ATTRIBUTE_ID");
		log.debug(sb);
		
		ExerciseVO eVo = null;
		String exId = "";
		ExerciseIntensityVO iVo = null;			
		PreparedStatement ps = null;
		int i = 1;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(i++, programId);
			if (hospitalId.length() > 0) ps.setString(i++, hospitalId);
			if (exerciseId.length() > 0) ps.setString(i++, exerciseId);
			if (exerciseIntensityId.length() > 0) ps.setString(i++, exerciseIntensityId);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (!exId.equals(rs.getString("EXERCISE_ID"))) {
					if (eVo != null) {
						//add old
						eVo.addIntensityLevel(iVo);
						vos.add(eVo);
					}
					
					//default action for first case.
					eVo = new ExerciseVO(rs);
					iVo = new ExerciseIntensityVO(rs);
					iVo.addListAttribute(new ExerciseAttributeVO(rs));
					exId = eVo.getExerciseId();
					continue;
				}

				if (!iVo.getExerciseIntensityId().equals(rs.getString("EXERCISE_INTENSITY_ID"))) {
					//add old
					eVo.addIntensityLevel(iVo);
					//save new
					iVo = new ExerciseIntensityVO(rs);
				}

				iVo.addListAttribute(new ExerciseAttributeVO(rs));
			}
			//add the dangling record
			if (eVo != null) {
				eVo.addIntensityLevel(iVo);
				vos.add(eVo);
			}
		} catch (SQLException sqle) {
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		log.debug("Retrieved " + vos.size() + " Exercises");
		mod.setActionData(vos);
	}
	
	private String writeFile(SMTServletRequest req, String paramNm) {
		log.debug("starting writeFile");
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);

		StringBuilder fPath =  new StringBuilder((String)getAttribute("pathToBinary"));
		fPath.append(getAttribute("orgAlias")).append(site.getOrganizationId());
		fPath.append("/").append("foreFront/").append(req.getSession().getAttribute(ProgramAction.PROGRAM_ID));
		fPath.append("/").append(paramNm).append("/");

    	FileLoader fl = null;
    	FilePartDataBean fpdb = req.getFile(paramNm);
    	String fileNm = (fpdb != null) ? StringUtil.checkVal(fpdb.getFileName()) : null;
		
    	// Write new file
    	if (fileNm != null && fileNm.length() > 0) {
    		try {
	    		fl = new FileLoader(attributes);
	        	fl.setFileName(fpdb.getFileName());
	        	fl.setPath(fPath.toString());
	        	fl.setRename(Boolean.TRUE);
	    		fl.setOverWrite(Boolean.FALSE);
	        	fl.makeDir(true);
	        	fl.setData(fpdb.getFileData());
	        	fileNm = fl.writeFiles();
	    	} catch (Exception e) {
	    		log.error("Error Writing File", e);
	    	}
	    	log.debug("finished writing file " + fileNm);
	    	
    	} else if (req.getParameter("orig_" + paramNm) != null) {
    		fileNm = req.getParameter("orig_" + paramNm);
    	}
    	
    	fpdb = null;
    	fl = null;
		return fileNm;
	}
	
	private void updateExercise(SMTServletRequest req) throws ActionException{
		log.debug("Beginning Exercise update");
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		ExerciseVO vo = new ExerciseVO(req);

		StringBuilder sb = new StringBuilder();
		Boolean isInsert = !(StringUtil.checkVal(vo.getExerciseId()).length() > 0);
		
		if (isInsert) {
			sb.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sb.append("FOREFRONT_EXERCISE (PROGRAM_ID, HOSPITAL_ID, ");
			sb.append("EXERCISE_NM, THUMBNAIL_IMG, VIDEO_URL, ");
			sb.append("DETAIL_IMG, DETAILED_DESC_TXT, POSTSURG_DESC_TXT, ");
			sb.append("CREATE_DT, EXERCISE_ID) values(?,?,?,?,?,?,?,?,?,?)");
			vo.setExerciseId(new UUIDGenerator().getUUID());
			//used for binding the exerciseIntensity fields.
			req.setParameter("exerciseId", vo.getExerciseId());
		} else {
			sb.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sb.append("FOREFRONT_EXERCISE set PROGRAM_ID = ?, HOSPITAL_ID = ?, ");
			sb.append("EXERCISE_NM = ?, THUMBNAIL_IMG = ?, VIDEO_URL = ?, ");
			sb.append("DETAIL_IMG = ?, DETAILED_DESC_TXT = ?, POSTSURG_DESC_TXT=?, ");
			sb.append("UPDATE_DT = ? where EXERCISE_ID = ?");
		}

		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			int i = 1;
			ps.setString(i++, vo.getProgramId());
			ps.setString(i++, vo.getHospitalId());
			ps.setString(i++, vo.getExerciseName());
			
			if (req.hasParameter("thumbnail") || req.hasParameter("orig_thumbnail")) {
				ps.setString(i++,  writeFile(req, "thumbnail"));
			} else {
				ps.setString(i++, req.getParameter("thumbnailTxt"));
			}
			if (req.hasParameter("video") || req.hasParameter("orig_video")) {
				ps.setString(i++,  writeFile(req, "video"));
			} else {
				ps.setString(i++, req.getParameter("videoTxt"));
			}
			if (req.hasParameter("detail") || req.hasParameter("orig_detail")) {
				ps.setString(i++,  writeFile(req, "detail"));
			} else {
				ps.setString(i++, req.getParameter("detailTxt"));
			}
			ps.setString(i++, vo.getDetailedDescText());
			ps.setString(i++, StringUtil.checkVal(vo.getPostSurgDescText()).trim()); //flush whitespace created by WYSIWYG
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setString(i++, vo.getExerciseId());
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {
			}
		}

		mod.setActionData(vo);
		}
	
	private void updateExerciseIntensity(SMTServletRequest req) throws ActionException {
		log.debug("Beginning ExerciseIntensity update");
		List<ExerciseIntensityVO> vos = loadIntensities(req);
		
		for (ExerciseIntensityVO vo : vos) {
			StringBuilder sb = new StringBuilder();
			Boolean isInsert = StringUtil.checkVal(vo.getExerciseIntensityId()).length() == 0;
			
			if (isInsert) {
				sb.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
				sb.append("FOREFRONT_EXERCISE_INTENSITY (EXERCISE_ID, INTENSITY_LEVEL_NO, ");
				sb.append("REQ_SETS_NO, SHORT_DESC_TXT, CREATE_DT, EXERCISE_INTENSITY_ID) values(?,?,?,?,?,?)");
				vo.setExerciseIntensityId(new UUIDGenerator().getUUID());
				//Used for adding parameters to the Intensity piece.
				req.setParameter("exerciseIntensityId_" + vo.getIntensityLvlNo(), vo.getExerciseIntensityId());
			} else {
				sb.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
				sb.append("FOREFRONT_EXERCISE_INTENSITY set EXERCISE_ID = ?, INTENSITY_LEVEL_NO = ?, ");
				sb.append("REQ_SETS_NO = ?, SHORT_DESC_TXT = ?, UPDATE_DT = ? where EXERCISE_INTENSITY_ID = ?");
			}
	
			PreparedStatement ps = null;
			try {
				ps = dbConn.prepareStatement(sb.toString());
				ps.setString(1, vo.getExerciseId());
				ps.setInt(2, vo.getIntensityLvlNo());
				ps.setInt(3, vo.getReqSetsNo());
				ps.setString(4, vo.getShortDescText());
				ps.setTimestamp(5, Convert.getCurrentTimestamp());
				ps.setString(6, vo.getExerciseIntensityId());
				ps.executeUpdate();
				
			} catch (SQLException sqle) {
				log.error(sqle);
				throw new ActionException(sqle);
			} finally {
				try { ps.close(); } catch (Exception e) {}
			}
			
			updateExerciseAttributes(req, vo.getIntensityLvlNo());
		}

	}
	
	private void updateExerciseAttributes(SMTServletRequest req, int iLvl) throws ActionException {
		log.debug("Beginning ExerciseAttributes update");
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		List<ExerciseAttributeVO> vos = loadAttributes(req, iLvl);
		
		for(ExerciseAttributeVO vo : vos) {
			StringBuffer sql = new StringBuffer();	

			// Create the statement based upon whether a pkId is present
			if (vo.getExerciseAttributeId() == null || vo.getExerciseAttributeId().length() == 0) {
				sql.append("insert into ").append(customDb);
				sql.append("FOREFRONT_EXERCISE_ATTRIBUTE (EXERCISE_INTENSITY_ID, LABEL_TXT, ");
				sql.append("UNIT_TXT, HTML_TYPE_NM, DEFAULT_VALUE_TXT, CREATE_DT, ");
				sql.append("ATTRIBUTE_ID) values (?,?,?,?,?,?,?)");
				vo.setExerciseAttributeId(new UUIDGenerator().getUUID());
			} else {
				sql.append("update ").append(customDb);
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
					throw new ActionException("No records updated, " + ps.getWarnings());
	
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
	
	private List<ExerciseIntensityVO> loadIntensities(SMTServletRequest req){
		List<ExerciseIntensityVO> vos = new ArrayList<ExerciseIntensityVO>();
		
		for(int i = 1; i <= 3; i++) {
			ExerciseIntensityVO vo = new ExerciseIntensityVO(req, i);
			if(vo.getReqSetsNo() > 0)
				vos.add(vo);
		}
		
		log.debug("Returning " + vos.size() + " intensity VO's from the loader.");
		return vos;
	}
	
	private List<ExerciseAttributeVO> loadAttributes(SMTServletRequest req, int iLvl) {
		int count = 1;
		List<ExerciseAttributeVO> vos = new ArrayList<ExerciseAttributeVO>();
		ExerciseAttributeVO vo = new ExerciseAttributeVO(req, iLvl, count);
		
		while (StringUtil.checkVal(vo.getLabelText()).length() > 0) {
			vos.add(vo);
			count++;
			vo = new ExerciseAttributeVO(req, iLvl, count);
		}
		
		log.debug("Returning " + vos.size() + " attribute VO's from the loader.");
		return vos;
	}

	
	public void build(SMTServletRequest req) throws ActionException {
		msg = (String) getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		req.setValidateInput(Boolean.FALSE);
		Boolean addAttr = Boolean.parseBoolean(StringUtil.checkVal(req.getParameter("addAttributes")));
		
		try {
			if (req.hasParameter("delId")) {
				this.delete(req);
			} else if (!addAttr) {
				updateExercise(req);		
			} else {
				updateExerciseIntensity(req);	
			}
		} catch (ActionException ae) {
			log.error("error building exercise", ae);
			msg = (String) getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		}
		
		req.setValidateInput(Boolean.TRUE);
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		this.sendRedirect(page.getFullPath() + "?actionType=" + AdminFacadeAction.EXERCISE_ACTION, msg, req);
	}
}

package com.depuy.forefront.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.depuy.forefront.action.vo.ListItemVO;
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

public class ListAction extends SBActionAdapter {
	
	private String msg = null;
	
	public ListAction() {
		super();
	}
	public ListAction(ActionInitVO ai) {
		super(ai);
	}
	
	public void delete(SMTServletRequest req) {
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		msg = (String) getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		String pkId = req.getParameter("delId");

		StringBuilder sql = new StringBuilder();
		sql.append("delete from ").append(customDb);
		sql.append("forefront_list_item where list_item_id=?");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, pkId);
			ps.execute();
		} catch (SQLException sqle) {
			log.error("could not delete list item", sqle);
			msg = (String) getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}
	
	
	public void retrieve(SMTServletRequest req) throws ActionException{
		log.debug("Beginning ListAction retrieve");
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String programId = (String) req.getSession().getAttribute(ProgramAction.PROGRAM_ID);
		String hospitalId = StringUtil.checkVal(req.getSession().getAttribute("hospitalId"));
		String listItemId = StringUtil.checkVal(req.getParameter("listItemId"));
		List<ListItemVO> data = new ArrayList<ListItemVO>();
		
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FOREFRONT_LIST_ITEM a left outer join ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FOREFRONT_HOSPITAL b on a.hospital_id=b.hospital_id where PROGRAM_ID = ? ");
		if (hospitalId.length() > 0) {
			sb.append("and a.HOSPITAL_ID = ? ");
		} else {
			sb.append("and a.HOSPITAL_ID is null "); 
		}
		if (listItemId.length() > 0) sb.append("and LIST_ITEM_ID = ? ");
		sb.append("order by list_nm");
		log.debug(sb);
		
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sb.toString());
			int i = 1;
			ps.setString(i++, programId);
			if (hospitalId.length() > 0) ps.setString(i++, hospitalId);
			if (listItemId.length() > 0) ps.setString(i++, listItemId);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				data.add(new ListItemVO(rs));

		} catch (SQLException sqle) {
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		//ensure at least one item is returned, so the edit form displays
		if (data.size() == 0 && listItemId.length() > 0)
			data.add(new ListItemVO());
		
		mod.setActionData(data);
	}
	
	private void updateList(SMTServletRequest req) throws ActionException{
		log.debug("Beginning ListAction update");
		msg = "List Item Added Successfully";
		ListItemVO vo = new ListItemVO(req);

		StringBuilder sb = new StringBuilder();
		Boolean isInsert = StringUtil.checkVal(vo.getListItemId()).length() == 0;
		
		if (isInsert) {
			sb.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sb.append("FOREFRONT_LIST_ITEM (PROGRAM_ID, HOSPITAL_ID, ");
			sb.append("ITEM_TYPE_NO, LIST_NM, SUMMARY_TXT, ARTICLE_TXT, ");
			sb.append("THUMB_IMG_URL, DETAIL_IMG_URL, VIDEO_URL, CREATE_DT, ");
			sb.append("EXTERNAL_URL, ");
			sb.append("LIST_ITEM_ID) values(?,?,?,?,?,?,?,?,?,?,?,?)");
			vo.setListItemId(new UUIDGenerator().getUUID());

		} else {
			sb.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sb.append("FOREFRONT_LIST_ITEM set PROGRAM_ID = ?, HOSPITAL_ID = ?, ");
			sb.append("ITEM_TYPE_NO = ?, LIST_NM = ?, SUMMARY_TXT = ?, ");
			sb.append("ARTICLE_TXT = ?, THUMB_IMG_URL = ?, DETAIL_IMG_URL = ?, ");
			sb.append("VIDEO_URL = ?, UPDATE_DT = ?, EXTERNAL_URL=? ");
			sb.append("where LIST_ITEM_ID = ?");
		}

		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, vo.getProgramId());
			ps.setString(2, vo.getHospitalId());
			ps.setInt(3, vo.getItemTypeId());
			ps.setString(4, vo.getListName());
			ps.setString(5, vo.getSummaryText());
			ps.setString(6, vo.getArticleText());
			ps.setString(7, writeFile(req, "thumbImageUrl"));
			ps.setString(8, writeFile(req, "detailImageUrl"));
			ps.setString(9, writeFile(req, "videoUrl"));
			ps.setTimestamp(10, Convert.getCurrentTimestamp());
			ps.setString(11, vo.getExternalUrl());
			ps.setString(12, vo.getListItemId());
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			msg = "A problem occured when adding the List Item.";
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}
	
	public void build(SMTServletRequest req) throws ActionException {
		if (req.hasParameter("delId")) {
			this.delete(req);
		} else {
			req.setValidateInput(Boolean.FALSE);
			updateList(req);
			req.setValidateInput(Boolean.TRUE);
		}

		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		this.sendRedirect(page.getFullPath() + "?actionType=" + AdminFacadeAction.LIST_ITEM_ACTION, msg, req);
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
	    	
    	} else if (req.hasParameter("orig_" + paramNm)) {
    		fileNm = req.getParameter("orig_" + paramNm);
    		
    	} else if (req.hasParameter("alt_" + paramNm)) {
    		fileNm = req.getParameter("alt_" + paramNm);
    	}
    	
    	fpdb = null;
    	fl = null;
		return fileNm;
	}
}

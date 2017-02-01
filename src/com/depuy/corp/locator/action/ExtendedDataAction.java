/**
 * 
 */
package com.depuy.corp.locator.action;

import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.smt.sitebuilder.action.FileLoader;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ExtendedDataAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 18, 2011
 ****************************************************************************/
public class ExtendedDataAction extends SimpleActionAdapter {
	
	//searchType constants
	public static final int LOCATION_SEARCH = 1;
	public static final int HOSPITAL_SEARCH = 2;
	public static final int PHYSICIAN_SEARCH = 3;
	public static final int PATHOLOGY_SEARCH = 4;

	public ExtendedDataAction() {
	}

	public ExtendedDataAction(ActionInitVO arg0) {
		super(arg0);
	}


	public void delete(ActionRequest req) throws ActionException {
		
	}
	
	public void update(ActionRequest req) throws ActionException {
		if (!req.hasParameter("dealerLocationId"))
			throw new ActionException("extData save will fail, no dealerLocationId");
			
		log.debug("saving extended data");
		Boolean isInsert = Convert.formatBoolean(req.getParameter("isInsert"));
		StringBuilder sql = new StringBuilder();
		String dlrLocnId = req.getParameter("dealerLocationId");
				
		if (isInsert) {
			sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DEPUY_CORP_LOCATOR (PHYSICIAN_NM, HOSP_AFFIL_TXT, CASE_DAYS_TXT, ");
			sql.append("HOTELS_TXT,	AIRPORTS_TXT, CREATE_DT, PHOTO_URL, VIDEO_URL, ");
			sql.append("COURSEWORK_URL, ALT_WEBSITE_URL, SPECIALTY_TXT, DEALER_LOCATION_ID) ");
			sql.append("values (?,?,?,?,?,?,?,?,?,?,?,?)");
		} else {
			sql.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DEPUY_CORP_LOCATOR set PHYSICIAN_NM=?, HOSP_AFFIL_TXT=?, ");
			sql.append("CASE_DAYS_TXT=?, HOTELS_TXT=?, AIRPORTS_TXT=?, UPDATE_DT=?, ");
			sql.append("PHOTO_URL=?, VIDEO_URL=?, COURSEWORK_URL=?, ALT_WEBSITE_URL=?, ");
			sql.append("SPECIALTY_TXT=? where DEALER_LOCATION_ID=?");
		}
		log.debug(sql);
		
		int cnt = 0;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("physicianName"));
			ps.setString(2, req.getParameter("hospitalAffiliation"));
			ps.setString(3, req.getParameter("caseDays"));
			ps.setString(4, req.getParameter("hotels"));
			ps.setString(5, req.getParameter("airports"));
			ps.setTimestamp(6, Convert.getCurrentTimestamp());
			ps.setString(7, writeFile(req, "photo"));
			ps.setString(8, writeFile(req, "video"));
			ps.setString(9, writeFile(req, "coursework"));
			ps.setString(10, req.getParameter("alternateUrl"));
			ps.setString(11, req.getParameter("speciality"));
			ps.setString(12, dlrLocnId);
			cnt = ps.executeUpdate();
			
		} catch (SQLException sqle) {
			log.error("error saving extended data", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		//if we mistakenly tried to update a record that doesn't exist, add it!
		if (!isInsert && cnt == 0) {
			req.setParameter("isInsert", "true");
			update(req);
			
		} else if (cnt > 0) {
			//save product associations
			if (req.hasParameter("products")) {
				this.flushProducts(dlrLocnId);
				log.debug("prods=" + req.getParameter("products"));
				this.saveProducts(dlrLocnId, req.getParameter("products").split(","));
			}
			
			//save pathologies associations
			this.flushPathologies(req);

			if (req.hasParameter("pathologies")) {
				log.debug("paths=" + req.getParameter("pathologies"));
				this.savePathologies(req);
			}
		}
	}

	public void retrieve(ActionRequest req) throws ActionException {
		final String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		int sType = Convert.formatInteger(req.getParameter("sType"), 0); //searchType
		String[] dlrs = req.getParameterValues("dealerLocationId");
		String physNm = req.getParameter("physician");
		String[] pathIds = StringUtil.checkVal(req.getParameter("pathology")).split(",");
		
		StringBuilder sql = new StringBuilder("select * from ");
		sql.append(customDb).append("DEPUY_CORP_LOCATOR a ");
		sql.append("left outer join ").append(customDb).append("DEPUY_CORP_PATHOLOGY_XR b ");
		sql.append("on a.dealer_location_id=b.dealer_location_id ");
		sql.append("left outer join ").append(customDb).append("DEPUY_CORP_PATHOLOGY c ");
		sql.append("on b.pathology_id=c.pathology_id ");
		sql.append("left outer join ").append(customDb).append("DEPUY_CORP_PRODUCT_XR d ");
		sql.append("on a.dealer_location_id=d.dealer_location_id ");
		sql.append("left outer join ").append(customDb).append("DEPUY_CORP_PRODUCT e ");
		sql.append("on d.product_id=e.product_id ");
		sql.append("where 1=0 ");
		
		if (dlrs != null && dlrs.length > 0) { //by ID
			sql.append("or a.dealer_location_id in (''");
			for (int x=dlrs.length; x > 0; --x) sql.append(",?");
			sql.append(")");
		} else if (sType == PHYSICIAN_SEARCH) {
			sql.append("or physician_nm like ?");
			
		} else if (sType == PATHOLOGY_SEARCH) {
			if (pathIds == null) pathIds = new String[0];
			sql.append("or c.pathology_id in (''");
			for (int x=pathIds.length; x > 0; --x) sql.append(",?");
			sql.append(")");
			
		}
		
		log.debug(sql);
		Map<String, DePuyCorpLocationVO> data = new HashMap<String, DePuyCorpLocationVO>();
		DePuyCorpLocationVO vo = null;
		PreparedStatement ps = null;
		int i = 0;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			
			if (dlrs != null && dlrs.length > 0) { //by ID
				for (String s : dlrs)
					ps.setString(++i, s);
			} else if (sType == PHYSICIAN_SEARCH) {
					ps.setString(++i, "%" + physNm + "%");
			} else if (sType == PATHOLOGY_SEARCH) {
				for (String s : pathIds)
					ps.setString(++i, s);
			}
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (data.containsKey(rs.getString("dealer_location_id"))) {
					vo = data.get(rs.getString("dealer_location_id"));
				} else {
					vo = new DePuyCorpLocationVO(rs);
				}
				
				//add this product; the Map is duplicate-safe
				if (!vo.getProducts().containsKey(rs.getString("product_id")))
					vo.addProduct(rs.getString("product_id"), rs.getString("product_nm"));
				
				//add this pathology; the Map is duplicate-safe
				//if (!vo.getPathologies().containsKey(rs.getString("pathology_id")))
				//	vo.addPathology(rs.getString("pathology_id"), rs.getString("parent_id"), rs.getString("pathology_nm"));
				
				data.put(rs.getString("dealer_location_id"), vo);
			}
			
		} catch (SQLException sqle) {
			log.error("error retrieving extended data", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		//req.setAttribute("extDataMap", data);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		mod.setActionData(data);
	}
	
	
	private void flushPathologies(ActionRequest req) throws ActionException {
		SimpleActionAdapter saa = new PathologiesAction(this.actionInit);
		saa.setAttributes(attributes);
		saa.setDBConnection(dbConn);
		saa.delete(req);
	}
	
	private void flushProducts(String dealerLocationId) {
		StringBuilder sql = new StringBuilder();
		sql.append("delete from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_CORP_PRODUCT_XR where dealer_location_id=?");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, dealerLocationId);
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("could not delete products", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}
	
	private void saveProducts(String dlrLocnId, String[] productIds) {
		if (productIds == null || productIds.length == 0) return;
		
		StringBuilder sql = new StringBuilder();
		sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_CORP_PRODUCT_XR (product_xr_id, dealer_location_id, ");
		sql.append("product_id, create_dt) values (?,?,?,?)");

		UUIDGenerator uuid = new UUIDGenerator();
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			for (String p : productIds) {
				if (p == null || p.length() == 0) continue;
				ps.setString(1, uuid.getUUID());
				ps.setString(2, dlrLocnId);
				ps.setString(3, p);
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.debug(cnt.length + " products saved for " + dlrLocnId);
			
		} catch (SQLException sqle) {
			log.error("could not save products", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}
	
	private void savePathologies(ActionRequest req) throws ActionException {
		SimpleActionAdapter saa = new PathologiesAction(this.actionInit);
		saa.setAttributes(attributes);
		saa.setDBConnection(dbConn);
		saa.update(req);
	}
	
	
	private String writeFile(ActionRequest req, String paramNm) {
		log.debug("starting writeFile");
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);

		StringBuilder fPath =  new StringBuilder((String)getAttribute("pathToBinary"));
		fPath.append(getAttribute("orgAlias")).append(site.getOrganizationId());
		fPath.append("/").append("corpLocFiles").append("/").append(paramNm).append("/");

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
	    	log.debug("finished write");
	    	
    	} else if (req.getParameter("orig_" + paramNm) != null) {
    		fileNm = req.getParameter("orig_" + paramNm);
    	}
    	
    	fpdb = null;
    	fl = null;
		return fileNm;
	}

}

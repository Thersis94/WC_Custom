package com.ansmed.sb.physician;

// JDK 1.5.0
import java.util.List;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

// SMT Base Libs 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.FileException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.databean.FilePartDataBean;

// SB Libs
import com.smt.sitebuilder.action.FileLoader;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: PhysicianDocumentAction.java</p>
 <p>Description: <b/>Manages the information and docuemnts for a given physician</p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 1.0
 @since Oct 18, 2007
 Last Updated:
 ***************************************************************************/

public class PhysicianDocumentAction extends SBActionAdapter {
	public static final String DOCUMENT_DATA = "documentData";
	
	/**
	 * 
	 */
	public PhysicianDocumentAction() {
	}

	/**
	 * @param actionInit
	 */
	public PhysicianDocumentAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		if(Convert.formatBoolean(req.getParameter("deleteEle"))) {
			delete(req);
		} else {
			update(req);
		}
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		log.debug("deleting document");
		String message = "Successfully deleted file";
		String schema = (String) this.getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuffer sql = new StringBuffer();
		sql.append("delete from ").append(schema).append("ans_document ");
		sql.append("where document_id = ?");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("documentId"));
			if(ps.executeUpdate() < 1) {
				message = "Unable to delete the document";
			}
			
			try {
				this.deleteFiles(req);
			} catch(Exception e) {}
		} catch(SQLException sqle) {
			log.error("Error deleting ans physician documents", sqle);
			message = "Unable to delete the document";
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		req.setAttribute(PhysicianDataFacadeAction.ANS_PHYSICIAN_MESSAGE, message);
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("retrieving document ...");
		String schema = (String) this.getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuffer sql = new StringBuffer();
		sql.append("select a.*, type_nm from ").append(schema).append("ans_document a ");
		sql.append("inner join ").append(schema).append("ans_lu_document_type b ");
		sql.append("on a.document_type_id = b.document_type_id ");
		sql.append("where surgeon_id = ? order by document_nm ");
		log.debug("Document SQL: " + sql + "|" + req.getParameter("surgeonId"));
		
		PreparedStatement ps = null;
		List<DocumentVO> docs = new ArrayList<DocumentVO>();
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("surgeonId"));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				docs.add(new DocumentVO(rs));
			}
		} catch(SQLException sqle) {
			log.error("Error retrieving ans physician documents", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		// Add the data to the request object
		req.setAttribute(DOCUMENT_DATA, docs);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		log.debug("Updating document");
		String message = "You have successfully added a document";
		String schema = (String) this.getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuffer sql = new StringBuffer();
		sql.append("insert into ").append(schema).append("ans_document ");
		sql.append("(document_id, document_type_id, surgeon_id, document_nm, ");
		sql.append("create_dt) values(?,?,?,?,?)");
		
		PreparedStatement ps = null;
		DocumentVO vo = new DocumentVO(req);

		try {
			// Write the file to the system
			String fileName = writeFile(req);
			
			// Update the database
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, vo.getDocumentTypeId());
			ps.setString(3, vo.getSurgeonId());
			ps.setString(4, fileName);
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			
			if(ps.executeUpdate() < 1) {
				message = "Unable to add a document";
			}
			
		} catch(Exception sqle) {
			log.error("Error adding ans physician documents", sqle);
			message = "Unable to add a document";
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		req.setAttribute(PhysicianDataFacadeAction.ANS_PHYSICIAN_MESSAGE, message);
	}
	
	/**
	 * Deletes a physician document off the file system
	 * @param req
	 * @throws FileException
	 */
	protected void deleteFiles(ActionRequest req) 
	throws FileException {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		String orgAlias = (String)getAttribute("orgAlias");
		String binAlias = (String)getAttribute("pathToBinary");
		StringBuffer path = new StringBuffer();
		path.append(binAlias).append(orgAlias).append(site.getOrganizationId());
		path.append("/").append(site.getSiteId()).append("/images/module/phys_docs/");
		path.append(req.getParameter("documentName"));
		log.debug("Deleted File: " + path.toString());
		
		// Delete the file
		FileLoader fl = new FileLoader(attributes);
		try {
			fl.deleteFile(path.toString());
		} catch (Exception e) {
			log.error("Error delting phys doc form file system", e);
			throw new FileException("Unable to delete file", e);
		}
		
	}
	
	/**
	 * Writes the file to the system and returns the file name
	 * @param req
	 * @return
	 */
	protected String writeFile(ActionRequest req) 
	throws ActionException  {
		FilePartDataBean fpdb = req.getFile("documentName");
		if (fpdb == null) return null;
		
		// Build the directory path
		String fileName = req.getParameter("surgeonId") + "_" + fpdb.getFileName();
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		String orgAlias = (String)getAttribute("orgAlias");
		String binAlias = (String)getAttribute("pathToBinary");
		StringBuffer path = new StringBuffer();
		path.append(binAlias).append(orgAlias).append(site.getOrganizationId());
		path.append("/").append(site.getSiteId()).append("/images/module/phys_docs/");
		log.debug("Doc Path: " + path);
		
		// Write the file to the SMB File Share
		FileLoader fl = new FileLoader(attributes);
		try {
			// Write the file and get the file name for renaming
			fileName = fl.writeFiles(fpdb.getFileData(), path.toString(), fileName, Boolean.TRUE, Boolean.FALSE);
		} catch(Exception e) {
			log.error("Unable to write surgeon data file", e);
			throw new ActionException("Unable to write surgeon data file", e);
		}
		log.debug("Something goes here");
		return fileName;
	}

}

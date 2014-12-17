package com.sjm.corp.locator.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.dealer.DealerInfoAction;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;
import com.smt.sitebuilder.action.dealer.DealerVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: BatchClinicUpdaterAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Allows do a batch upload of clinics of a particular type or
 * get a list of all clinics of that type.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Dec 17, 2014<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class BatchClinicUpdaterAction extends SBActionAdapter {
	
	public BatchClinicUpdaterAction () {
		
	}
	
	public BatchClinicUpdaterAction(ActionInitVO ai) {
		super(ai);
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("Retrieve");
		super.retrieve(req);
	}
	
	public void build(SMTServletRequest req) throws ActionException  {
		log.debug("Building");
	    	ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
	    	int clinicTypeId = Convert.formatInteger((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
	    	
	    	// Check if we are doing an insert or an export
	    	if (Convert.formatBoolean(req.getParameter("dealerImport"))) {
	    		updateClinics(req, clinicTypeId);
		} else {
			exportClinics(req, clinicTypeId);
		}
	}

	/**
	 * Insert all the new clinics listed in the uploaded document using the DealerInfoAction
	 * @param req
	 * @param clinicTypeId
	 * @throws ActionException
	 */
	private void updateClinics(SMTServletRequest req, int clinicTypeId) throws ActionException {
    		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
    		
    		// Delete all clinics in the database so as to start fresh with the up to date list of clinics
		flushClinics(clinicTypeId);
		
		// Set up the dealerInforAction and have it handle the upload for us
		SMTActionInterface dia = new DealerInfoAction(actionInit);
		dia.setDBConnection(dbConn);
		dia.setAttributes(attributes);
		req.setParameter("dealerImport", "true");
    		req.setParameter("organizationId", site.getOrganizationId());
		dia.update(req);
		
		req.setParameter("msg", "Dealers successfully replaced.");
	}

	/**
	 * Create a report of all the clinics of the supplied type
	 * @param req
	 * @param clinicTypeId
	 */
	private void exportClinics(SMTServletRequest req, int clinicTypeId) {
		List<DealerVO> clinics = getClinicList(clinicTypeId);
		AbstractSBReportVO rpt = new ClinicReportVO();
		rpt.setData(clinics);
		rpt.setFileName("ClinicReport.csv");
		
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
		log.debug("Done building");
		
	}

	/**
	 * Create a list of all clinics with the type specified in the module data
	 * @param clinicTypeId
	 * @return
	 */
	private List<DealerVO> getClinicList(int clinicTypeId) {
		StringBuilder sql = new StringBuilder(125);
		
		sql.append("SELECT * FROM DEALER_LOCATION dl left join DEALER d ");
		sql.append("on d.DEALER_ID = dl.DEALER_ID WHERE d.DEALER_TYPE_ID = ?");
		log.debug(sql + "|" + clinicTypeId);
		
		PreparedStatement ps = null;
		List<DealerVO> data = new ArrayList<DealerVO>();
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, clinicTypeId);
			
			ResultSet rs = ps.executeQuery();
			String lastDealer = "";
			DealerVO vo = null;
			while (rs.next()) {
				if (!lastDealer.equals(rs.getString("DEALER_ID"))) {
					if (vo != null) data.add(vo);
					vo = new DealerVO(rs);
					vo.addLocation(new DealerLocationVO(rs));
				}
				vo.addLocation(new DealerLocationVO(rs));
			}
			
			// Add the dangling record if it exists
			if (vo != null) data.add(vo);
			
		} catch(SQLException e) {
			log.error("Could not get clinic infromation related to dealer type id " + clinicTypeId, e);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		return data;
	}

	/**
	 * Delete all clinics of a particular type so that we can reinsert everything from scratch
	 * @param clinicTypeId
	 */
	private void flushClinics(int clinicTypeId) {
		String sql = "DELETE DEALER WHERE DEALER_TYPE_ID = ?";
		log.debug(sql + "|" + clinicTypeId);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql);
			ps.setInt(1, clinicTypeId);
			
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Could not delete clinics with dealer type id " + clinicTypeId, e);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}
	
	public void delete(SMTServletRequest req) throws ActionException {
		log.debug("Delete");
		super.delete(req);		
	}
	
	public void list(SMTServletRequest req) throws ActionException  {
		log.debug("List");
		super.list(req);
	}
	
	public void update(SMTServletRequest req) throws ActionException  {
		log.debug("Update");
		super.update(req);
	}

}

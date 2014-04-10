package com.ansmed.sb.patienttracker;

// JDK 1.6 libs
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

// SMB Baselibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.StringUtil;

// SiteBuilder II libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.contact.ContactAction;
import com.smt.sitebuilder.action.contact.ContactDataContainer;
import com.smt.sitebuilder.action.contact.ContactDataModuleVO;
import com.smt.sitebuilder.action.contact.ContactVO;
import com.smt.sitebuilder.action.tracker.TrackerDataContainer;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
* <b>Title</b>PatientContactRetriever.java<p/>
* <b>Description: </b>Retrieves Contact Us form structure and contact submittal data for 
* the given contact action ID and contact submittal id. 
* <p/>
* <b>Copyright:</b> Copyright (c) 2011<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since Oct 28, 2011
* <b>Changes: </b>
* 10/28/2011: DBargerhuff; created class
****************************************************************************/
public class PatientContactRetriever extends SBActionAdapter {
	
	public PatientContactRetriever() {
		super();
	}

	public PatientContactRetriever(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("PatientContactDataRetriever retrieve...");
		String contactFormId = StringUtil.checkVal(req.getAttribute("sourceFormId"));
		String contactSubmittalId = StringUtil.checkVal(req.getAttribute("sourceSubmittalId"));
				
		// retrieve contact form
		String oldActionId = req.getParameter("actionId");
		req.setParameter("actionId", contactFormId, true);
		
		SMTActionInterface sai = new ContactAction(actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		sai.retrieve(req);

		TrackerDataContainer tdc = new TrackerDataContainer();
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		ContactVO cvo = (ContactVO) mod.getActionData();
		tdc.setContactForm(cvo);
		
		// retrieve the contact data
		ContactDataContainer cdc = this.retrieveContactData(req, contactFormId, contactSubmittalId);
		
		// set data on tracker container
		tdc.setContactData(cdc);
		// set container on module
		mod.setActionData(tdc);
		req.setAttribute(Constants.MODULE_DATA, mod);
		// reset actionId param
		req.setParameter("actionId", oldActionId, true);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException { }
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	private ContactDataContainer retrieveContactData(SMTServletRequest req, String contactFormId, String contactSubmittalId) 
		throws ActionException {
		log.debug("Starting retrieveContactData...");
		ContactDataContainer cdc = new ContactDataContainer();
		
		// retrieve the fields associated to this contact form and the 
		// core data (profile info, action name, etc ..)
		this.getFields(contactFormId, cdc);
		
		// Build the SQL Statement
		StringBuffer sql = new StringBuffer();
		sql.append("select a.contact_submittal_id, a.contact_field_id, value_txt, ");
		sql.append("data_enc_flg, profile_id, b.create_dt, b.site_id, c.html_type_id ");
		sql.append("from contact_data a inner join contact_submittal b ");
		sql.append("on a.contact_submittal_id = b.contact_submittal_id ");
		sql.append("inner join contact_field c on a.contact_field_id=c.contact_field_id ");
		sql.append("where b.action_id = ? and b.contact_submittal_id = ? ");
		sql.append("order by a.contact_submittal_id, a.contact_field_id");
		
		log.debug("Patient tracker contact data SQL: " + sql.toString());
		
		int i = 0;
		PreparedStatement ps = null;
		List<String> profileIds = new ArrayList<String>();
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(++i, contactFormId);
			ps.setString(++i, contactSubmittalId);
			
			ResultSet rs = ps.executeQuery();
			String csId = "";
			ContactDataModuleVO vo = null;
			String encKey = (String) getAttribute(Constants.ENCRYPT_KEY);
			StringEncrypter se = new StringEncrypter(StringEncrypter.DESEDE_ENCRYPTION_SCHEME, encKey);
			StringEncoder ser = new StringEncoder();
			while(rs.next()) {
				String newCsId = rs.getString(1); 
				profileIds.add(rs.getString("profile_id"));
				
				if (!csId.equals(newCsId)) {
					if (vo != null)	cdc.addResponse(vo);

					vo = new ContactDataModuleVO();
					vo.setData(rs);
				}

				String contactFieldId = rs.getString(2);
				String val = rs.getString(3);
				// if the data is encrypted, decrypt it first
				if (rs.getInt(4) == 1 && val != null && val.length() > 0)
					val = se.decrypt(val);
				//decode reserved characters
				val = decodeValue(ser, val);
				// log.debug("val: " + val);
				vo.addExtData(contactFieldId, val, rs.getInt("html_type_id"));
				// Reset the ids for comparison and increment the counter
				csId = newCsId;
			}

			// add the dangling record
			if (vo != null) cdc.addResponse(vo);
			log.debug("response list size: " + cdc.getData().size());			
			
		} catch (Exception e) {
			log.error("Error retrieving patient source contact data: ", e);
			throw new ActionException("Error retrieving patient source contact data: ", e);
		} finally {
	        try { ps.close(); } catch(Exception e) {}
		}
		return cdc;
	}
	
	/**
	 * Retrieves a list of fields for a given Contact Us Form
	 * @param contactId
	 * @throws ActionException
	 */
	private void getFields(String contactId, ContactDataContainer cdc) 
	throws ActionException {
		StringBuffer sql = new StringBuffer();
		sql.append("select a.contact_field_id, field_nm ");
		sql.append("from contact_field a inner join contact_assoc b ");
		sql.append("on a.contact_field_id = b.contact_field_id ");
		sql.append("inner join contact c on c.action_id = b.action_id ");
		sql.append("where c.action_id = ? and a.organization_id is not null ");
		sql.append("order by c.action_id, b.order_no, field_nm");
		log.info("Source form field retrieval SQL: " + sql + " | " + contactId);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, contactId);
			ResultSet rs = ps.executeQuery();
			
			while (rs.next())
				cdc.addField(rs.getString(1), rs.getString(2));
			
		} catch (Exception e) {
			log.error("Error retrieving patient source contact field data: ", e);
			throw new ActionException("Error retrieving patient source contact field data: ", e);
		} finally {
	        try { ps.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Loops through value twice to decode &amp; and then &nnnn/&#nnnn; values.
	 * @param se
	 * @param val
	 * @return
	 */
	private String decodeValue(StringEncoder se, String val) {
		for (int i = 0; i < 2; i++) {
			val = se.decodeValue(val);
		}
		return val;
	}
	
}

package com.ansmed.sb.patienttracker;

// JDK 1.6 libs
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Log4j
import org.apache.log4j.Logger;

// SMB Baselibs 2.0
import com.siliconmtn.db.pool.SMTDBConnection;

// WC libs
import com.smt.sitebuilder.action.contact.ContactDataContainer;
import com.smt.sitebuilder.action.contact.ContactDataModuleVO;
import com.smt.sitebuilder.action.contact.ContactFieldVO;
import com.smt.sitebuilder.action.contact.ContactVO;
import com.smt.sitebuilder.action.tracker.TrackerDataContainer;

/****************************************************************************
* <b>Title</b>PatientContactDataRetriever.java<p/>
* <b>Description: </b>Retrieves Contact Us form structure and contact submittal data for 
* the given contact action ID and contact submittal id. 
* <p/>
* <b>Copyright:</b> Copyright (c) 2011<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since Oct 12, 2012
* <b>Changes: </b>
* 10/12/2012: DBargerhuff; created class
****************************************************************************/
public class PatientContactDataRetriever {
	
	private Logger log = Logger.getLogger(PatientContactDataRetriever.class);
	private SMTDBConnection dbConn = null;
	private String contactSubmittalId;
	private List<String> contactSubmittalIds;
	
	public PatientContactDataRetriever() {
		contactSubmittalIds = new ArrayList<String>();
	}

	public TrackerDataContainer retrievePatientContactData() throws SQLException {
		log.debug("retrieving patient contact source data...");
		// retrieve the source Contact Us form
		log.debug("contact submittalId: " + contactSubmittalId);
		TrackerDataContainer tdc = new TrackerDataContainer();
		this.retrieveContactData(tdc);
		this.retrieveContactFields(tdc.getContactForm());
		return tdc;
	}
	
	/**
	 * Retrieves the Contact Us form (minus the fields) and the user-submitted data and adds both 
	 * to the tracker data container.
	 * @param tdc
	 * @throws SQLException
	 */
	private void retrieveContactData(TrackerDataContainer tdc) throws SQLException {
		StringBuffer sql = new StringBuffer();
		sql.append("select a.action_id, a.action_nm, b.organization_id, b.contact_nm, b.response_txt, ");
		sql.append("b.header_txt, b.collection_txt, b.permission_flg, b.profile_flg, ");
		sql.append("b.email_address_txt, b.create_dt, d.contact_field_id, d.value_txt ");
		sql.append("from sb_action a inner join contact b on a.action_id = b.action_id ");
		sql.append("inner join contact_submittal c on b.action_id = c.action_id ");
		sql.append("inner join contact_data d on c.contact_submittal_id = d.contact_submittal_id ");
		sql.append("where c.contact_submittal_id = ?");
		log.debug("contact data retrieval SQL: " + sql.toString());
		
		PreparedStatement ps = null;
		ContactDataContainer cdc = null;
		ContactDataModuleVO vo = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, contactSubmittalId);
			ResultSet rs = ps.executeQuery();
			cdc = new ContactDataContainer();
			vo = new ContactDataModuleVO();
			int count = 1;
			while (rs.next()) {
				if (count == 1) { // get the Contact form data on first pass.
					ContactVO cvo = new ContactVO();
					cvo.setData(rs);
					cvo.setActionName(rs.getString("action_nm"));
					tdc.setContactForm(cvo);
					count++;
				}
				// retrieve response data
				vo.addExtData(rs.getString("contact_field_id"), rs.getString("value_txt"), 0);
				log.debug("field/value: " + rs.getString("contact_field_id") + "/" + rs.getString("value_txt"));
			}
			cdc.addResponse(vo);
			tdc.setContactData(cdc);
		} catch (Exception e) {
			log.error("Error retrieving patient source contact form and submittal data: ", e);
			throw new SQLException("Error retrieving patient source contact form and submittal data: ", e);
		} finally {
	        try { ps.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Retrieves a list of fields for a given Contact Us Form
	 * @param cvo
	 * @throws SQLException
	 */
	private void retrieveContactFields(ContactVO cvo) throws SQLException {
		if (cvo == null) {
			log.debug("ContactVO is null, returning");
			return;
		}
		log.debug("Retrieving contact form fields...using contact action ID: " + cvo.getActionId());
		StringBuffer sql = new StringBuffer();
		sql.append("select a.contact_field_id, field_nm, order_no, required_flg, ");
		sql.append("html_type_id, encrypt_flg, wrap_question_flg ");
		sql.append("from contact_field a inner join contact_assoc b ");
		sql.append("on a.contact_field_id = b.contact_field_id ");
		sql.append("inner join contact c on c.action_id = b.action_id ");
		sql.append("where c.action_id = ? and a.organization_id is not null ");
		sql.append("order by c.action_id, b.order_no, field_nm");
		log.debug("Source form field retrieval SQL: " + sql + " | " + cvo.getActionId());
				
		PreparedStatement ps = null;
		ContactFieldVO field = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, cvo.getActionId());
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				field = new ContactFieldVO();
				field.setData(rs);
				cvo.addField(field);
			}
		} catch (Exception e) {
			log.error("Error retrieving patient source contact field data: ", e);
			throw new SQLException("Error retrieving patient source contact field data: ", e);
		} finally {
	        try { ps.close(); } catch (Exception e) {}
		}
	}

	/**
	 * @param dbConn the dbConn to set
	 */
	public void setDbConn(SMTDBConnection dbConn) {
		this.dbConn = dbConn;
	}

	/**
	 * @param contactSubmittalId the contactSubmittalId to set
	 */
	public void setContactSubmittalId(String contactSubmittalId) {
		this.contactSubmittalId = contactSubmittalId;
	}

	/**
	 * @return the contactSubmittalIds
	 */
	public List<String> getContactSubmittalIds() {
		return contactSubmittalIds;
	}

	/**
	 * @param contactSubmittalIds the contactSubmittalIds to set
	 */
	public void setContactSubmittalIds(List<String> contactSubmittalIds) {
		this.contactSubmittalIds = contactSubmittalIds;
	}
	
}

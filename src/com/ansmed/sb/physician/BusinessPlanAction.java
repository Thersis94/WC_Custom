package com.ansmed.sb.physician;

// JDK 1.5.0
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

// SMT Base Libs 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// SB
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

// SB ANS Medical Libs
import com.ansmed.sb.util.calendar.InvalidCalendarException;
import com.ansmed.sb.util.calendar.SJMBusinessCalendar;

/*****************************************************************************
 <p><b>Title</b>: BusinessPlanAction.java</p>
 <p>Description: <b/></p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 1.0
 @since Sep 12, 2007
 Last Updated:
 ***************************************************************************/

public class BusinessPlanAction extends SBActionAdapter {
	
	public static final String ANS_SJM_NUMBER_USED = "sjmNumberUsed";

	/**
	 * 
	 */
	public BusinessPlanAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public BusinessPlanAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("Building business plan...");
		String schema = (String)getAttribute("customDbSchema");
		String message = "You have successfully updated the business plan information.";
		String errMsg = "You cannot update business plan data for a non-current quarter.";
		int bpYear = 0;
		int bpQuarter = 0;
		
		// Process quarter/year
		bpYear = Convert.formatInteger(req.getParameter("bpYear"));
		bpQuarter = Convert.formatInteger(req.getParameter("bpQuarter"));
		log.debug("Checking for current year/quarter: " + bpQuarter + "|" + bpYear);
		
		//retrieve the current SJM business calendar
		SJMBusinessCalendar bc = null;
		try {
			bc = new SJMBusinessCalendar();
		} catch (InvalidCalendarException ice) {
			log.error("Error retrieving SJM business calendar.", ice);
			throw new ActionException("Error retrieving business calendar.",ice);
		}

		
		if (bpYear != bc.getCurrentYear()) {
			log.debug("Blocking attempt to update non-current year.");
			req.setAttribute(PhysicianDataFacadeAction.ANS_PHYSICIAN_MESSAGE, errMsg);
			return;
		} else {
			if (bpQuarter != bc.getCurrentQuarter()) {
				log.debug("Blocking attempt to update non-current quarter.");
				req.setAttribute(PhysicianDataFacadeAction.ANS_PHYSICIAN_MESSAGE, errMsg);
				return;
			}
		}
		
		// Delete the existing records before writing new records.
		delete(req);
		
		//Update the batch files
		Map<String, Boolean> data = this.getFieldVals();
		StringBuffer sql = new StringBuffer();
		sql.append("insert into ").append(schema).append("ans_xr_surgeon_busplan ");
		sql.append("(surgeon_busplan_id, surgeon_id, business_plan_id, selected_flg, ");
		sql.append("value_txt, bp_year_no, bp_quarter_no, create_dt ) values (?,?,?,?,?,?,?,?)");

		PreparedStatement ps = null;
		try {
			dbConn.setAutoCommit(true);
			ps = dbConn.prepareStatement(sql.toString());
			Enumeration<String> e = req.getParameterNames();
			while(e.hasMoreElements()){
				String key = e.nextElement();
				log.info("key: " + key);
				String[] value = req.getParameterValues(key);
				if (key != null && data.containsKey(key)) {
					Object[] parsedData = this.processVals(req, key, value);
					ps = dbConn.prepareStatement(sql.toString());
					ps.setString(1, new UUIDGenerator().getUUID());
					ps.setString(2, req.getParameter("surgeonId"));
					ps.setString(3, key);
					ps.setInt(4, (Integer)parsedData[0]);
					ps.setString(5, (String)parsedData[1]);
					ps.setInt(6, bpYear);
					ps.setInt(7, bpQuarter);
					ps.setTimestamp(8, Convert.getCurrentTimestamp());
					ps.executeUpdate();
					
					//log.info(req.getParameter("surgeonId") + "|" + key + "|" + parsedData[0] + "|" + parsedData[1] + "|" + Convert.formatInteger(req.getParameter("bpYear")));
				}
			}
		} catch(SQLException sqle) {
			sqle.printStackTrace();
			log.error("Error updating physician business plan.", sqle);
			message = "Error updating physician business plan.";
		} finally {
		}
		try {
			ps.close();
		} catch(Exception e) {}
		
		// add the physician's ranking.
		ps = null;
		sql = new StringBuffer();
		
		sql.append("update ").append(schema).append("ans_surgeon set rank_no = ? ");
		sql.append("where surgeon_id = ?");
		
		Integer rank = Convert.formatInteger(req.getParameter("rank"));
		
		log.debug("Rank update SQL: " + sql.toString());
		log.debug("Parameters: " + rank + " | " + req.getParameter("surgeonId"));
		
		try {
			dbConn.setAutoCommit(true);
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(1, rank);
			ps.setString(2, req.getParameter("surgeonId"));
			ps.execute();
			
		} catch(SQLException sqle) {
			sqle.printStackTrace();
			log.error("Error updating physician ranking.", sqle);
			message = "Error updating the physician business plan.";
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		// Add the message to the req object
		req.setAttribute(PhysicianDataFacadeAction.ANS_PHYSICIAN_MESSAGE, message);
	}
	
	/**
	 * Parse the returned data.
	 * @param req
	 * @param key
	 * @param data
	 * @return
	 */
	private Object[] processVals(ActionRequest req, String key, String[] data) {
		Object[] vals = new Object[2];
		
		// Loop the array and process the data
		Integer selected = 0;
		String value = null;
		//log.debug("Data: " + data[0] );
		for (int i = 0; i < data.length; i++) {
			Boolean bool = Convert.formatBoolean(data[0]);
			if (bool) selected = 1;
			value = data[i];
		}
		
		// If data is passed, the check box must be selected
		if (selected == 0 && StringUtil.checkVal(value).length() > 0) {
			selected = 1;
		}
		
		// Add the elements to the array
		vals[0] = selected;
		
		if (key.equalsIgnoreCase("physanlrevgoal")) {
			// Remove 'currency' formatting that may have been applied to this String.
			if (StringUtil.checkVal(value).length() > 0) {
				log.debug("key/value START: " + key + "/" + value);
				String temp = new String(value);
				int index = -1;
				// Remove the decimal or 'cents'
				if ((index = temp.indexOf(".")) > -1) {
					temp = temp.substring(0,index);
				}
				temp = StringUtil.replace(temp, "$", "");
				temp = StringUtil.replace(temp, ",", "");
				log.debug("key/value - END: " + key + "/" + temp);
				vals[1] = temp;
			}
		} else {
			vals[1] = value;	
		}
		
		log.info(selected + "|" + value);
		return vals;
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		String schema = (String)getAttribute("customDbSchema");
		StringBuffer del = new StringBuffer();
		del.append("delete from ").append(schema).append("ans_xr_surgeon_busplan ");
		del.append("where surgeon_id = ? and bp_quarter_no = ? and bp_year_no = ? ");
		log.info("Bus Plan Delete SQL: " + del + req.getParameter("surgeonId") + "|" + req.getParameter("bpQuarter") + "|" + req.getParameter("bpYear"));
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(del.toString());
			ps.setString(1, req.getParameter("surgeonId"));
			ps.setInt(2, Convert.formatInteger(req.getParameter("bpQuarter")));
			ps.setInt(3, Convert.formatInteger(req.getParameter("bpYear")));
			
			// Execute the delete
			ps.executeUpdate();
		} catch(SQLException sqle) {
			log.error("Error deleting physician's business plan data.", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("Retrieving Business Plan");
		// bpPlanType set to 2 to ensure business plan data query is successful.
		int bpPlanType = 2;
		int bpQuarter = Convert.formatInteger(req.getParameter("bpQuarter")).intValue();
		int bpYear = Convert.formatInteger(req.getParameter("bpYear")).intValue();
		boolean isCurrentQuarter = false;
		
		int currQuarter = 0;
		int currYear = 0;
		
		// See if we need to process quarter/year
		SJMBusinessCalendar bc = null;
		try {
			bc = new SJMBusinessCalendar();
		} catch (InvalidCalendarException ice) {
			log.error("Error retrieving SJM business calendar. ", ice);
			throw new ActionException("Error retrieving business calendar.",ice);
		}
		
		// get the current quarter/year values
		currYear = bc.getCurrentYear();
		currQuarter = bc.getCurrentQuarter();
		
		// check the quarter/year values passed in to determine if it's the 
		// current quarter.
		if (bpQuarter > 0 && bpYear > 0) {
			// Check for current quarter.
			if ((bpQuarter == currQuarter) && (bpYear == currYear)) {
				isCurrentQuarter = true;
			}
		} else { // If quarter or year are blank, use current quarter/year.
			bpQuarter = currQuarter;
			bpYear = currYear;
			isCurrentQuarter = true;
		}
		
		String surgeonId = req.getParameter("surgeonId");
		
		String schema = (String)getAttribute("customDbSchema");
		StringBuffer sql = new StringBuffer();
		sql.append("select a.*, b.field_nm from (");
		sql.append(schema).append("ans_xr_surgeon_busplan a inner join ");
		sql.append(schema).append("ans_business_plan b ");
		sql.append("on a.business_plan_id = b.business_plan_id) left outer join ");
		sql.append(schema).append("ans_bp_category c ");
		sql.append("on b.category_id = c.category_id ");
		sql.append("where surgeon_id = ? and c.category_type_id = ? ");
		sql.append("and bp_quarter_no = ? and bp_year_no = ? ");
		
		log.debug("Business plan SQL: " + sql.toString());
		log.debug("SQL params: " + surgeonId + "|" + bpPlanType + "|" + bpQuarter + "|" + bpYear);
		
		PreparedStatement ps = null;
		Map<String, BusinessPlanVO> data = new HashMap<String, BusinessPlanVO>();
		//Integer maxBpYear = 0;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, surgeonId);
			ps.setInt(2, bpPlanType);
			ps.setInt(3, bpQuarter);
			ps.setInt(4, bpYear);
						
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				data.put(rs.getString("business_plan_id"),new BusinessPlanVO(rs));
			}
		} catch(SQLException sqle) {
			log.error("Error retrieving physician's business plan data.", sqle);
		}
		
		// Get the physician's actuals for the previous year
		Map<String,Integer> actuals = new HashMap<String,Integer>(); 
		ps = null;
		sql = new StringBuffer();
		sql.append("select quarter_no, trial_no, ipg_no + rc_ipg_no as 'perms_no', ");
		sql.append("eol_ipg_no + eol_ipg_a_no as 'revisions_no', physician_dollar_no from ");
		sql.append(schema).append("ans_physician_actual where surgeon_id = ? ");
		sql.append("order by quarter_no, month_no");
		
		log.debug("Surgeon actuals SQL: " + sql.toString() + " | " + surgeonId);
		
		int prevTrialsQ1 = 0;
		int prevTrialsQ2 = 0;
		int prevTrialsQ3 = 0;
		int prevTrialsQ4 = 0;
		int prevPermsQ1 = 0;
		int prevPermsQ2 = 0;
		int prevPermsQ3 = 0;
		int prevPermsQ4 = 0;
		int revTotal = 0;
		Float physDollars = 0.00f;
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, surgeonId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				
				// Get quarterly data
				switch (rs.getInt("quarter_no")) {
				case 1:
					prevTrialsQ1 = prevTrialsQ1 + rs.getInt("trial_no");
					prevPermsQ1 = prevPermsQ1 + rs.getInt("perms_no");
					break;
				case 2:
					prevTrialsQ2 = prevTrialsQ2 + rs.getInt("trial_no");
					prevPermsQ2 = prevPermsQ2 + rs.getInt("perms_no");
					break;
				case 3:
					prevTrialsQ3 = prevTrialsQ3 + rs.getInt("trial_no");
					prevPermsQ3 = prevPermsQ3 + rs.getInt("perms_no");
					break;
				case 4:
					prevTrialsQ4 = prevTrialsQ4 + rs.getInt("trial_no");
					prevPermsQ4 = prevPermsQ4 + rs.getInt("perms_no");
					break;
				default:
					break;
				}
				
				// Get revision totals
				revTotal = revTotal + rs.getInt("revisions_no");
				physDollars = rs.getFloat("physician_dollar_no");
			}
			
			
		} catch (SQLException sqle) {
			log.error("Error retrieving physician's actuals data.", sqle);
		}
		
		try {
			ps.close();
		} catch(Exception e) {}
		
		//Put the data on the map.
		actuals.put("prevTrialsQ1", prevTrialsQ1);
		actuals.put("prevTrialsQ2", prevTrialsQ2);
		actuals.put("prevTrialsQ3", prevTrialsQ3);
		actuals.put("prevTrialsQ4", prevTrialsQ4);
		actuals.put("prevPermsQ1", prevPermsQ1);
		actuals.put("prevPermsQ2", prevPermsQ2);
		actuals.put("prevPermsQ3", prevPermsQ3);
		actuals.put("prevPermsQ4", prevPermsQ4);
		actuals.put("prevTrialsTotal", prevTrialsQ1 + prevTrialsQ2 + prevTrialsQ3 + prevTrialsQ4);
		actuals.put("prevRevTotal", revTotal);
		actuals.put("prevPermsTotal", prevPermsQ1 + prevPermsQ2 + prevPermsQ3 + prevPermsQ4);
		actuals.put("prevPhysDollars", physDollars.intValue());
		
		// Store the retrieved data in the ModuleVO.actionData and replace into
		// the Collection
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		mod.setActionData(data);
		attributes.put(Constants.MODULE_DATA, mod);
		
		req.setAttribute("bpQuarter", bpQuarter);
		req.setAttribute("bpYear", bpYear);
		req.setAttribute("isCurrentQuarter", isCurrentQuarter);
		
		// If plan data for the requested quarter/year, calculate totals. 
		int pTotal = 0;
		int tTotal = 0;
		int permsFactor = 0;
		int trialsFactor = 0;
		int ptlFactor = 0;
		int rank = 0;

		if (data != null && !data.isEmpty()) {			
			// Calculate the totals, factors, and ranking.
			pTotal =0;
			pTotal += getDataEle(data.get("permQ1"));
			pTotal += getDataEle(data.get("permQ2"));
			pTotal += getDataEle(data.get("permQ3"));
			pTotal += getDataEle(data.get("permQ4"));
			
			tTotal =0;
			tTotal += getDataEle(data.get("trialQ1"));
			tTotal += getDataEle(data.get("trialQ2"));
			tTotal += getDataEle(data.get("trialQ3"));
			tTotal += getDataEle(data.get("trialQ4"));
			
			permsFactor = getFactorValue(pTotal);
			trialsFactor = getFactorValue(tTotal);
			// Use getFactorValue method if totalAnnual select list is used in business_plan.jsp 
			ptlFactor = getDataEle(data.get("implantClass"));
			int focus = getDataEle(data.get("focusFactor"));
		
			// Check for null in case rep initially saves partial business plan data.
			if (data.get("physicianPerforms")!= null) {
				String performs = data.get("physicianPerforms").getValueText();
				boolean perms = false;
				if (performs.toLowerCase().indexOf("perms") > -1) perms = true;
				// Calculate rank			
				if (perms) {
					//use the perms factor value
					rank = (permsFactor + ptlFactor) * focus;
				} else {
					//use the trials factor value
					rank = (trialsFactor + ptlFactor) * focus;
				}
				
				// Calculate units used based on actual numbers
				String sjmMedical = StringUtil.checkVal(data.get("sjmMedical").getValueText());
				
				// If no percentage is specified for SJM Medical products, skip calculation
				if (sjmMedical.length() > 0) {
					// If no actuals exist for this physician, skip the calculation.
					if (! actuals.isEmpty()) {
						int sjmUsed = 0;
												
						if (perms) { // use the perms value
							sjmUsed = actuals.get("prevPermsTotal").intValue();
						} else { // use trials value
							sjmUsed = actuals.get("prevTrialsTotal").intValue();
						}
						// If SJM actuals are 0, skip the calculation.
						if (sjmUsed > 0) {
							Double calculatedTotal = 0.0;
							Double medUsed = 0.0;
							Double bosUsed = 0.0;
							Double unkUsed = 0.0;
							
							/* Total is the overall total number of implants a surgeon performs, 
							 * regardless of brand (SJM, Medtronic, etc.). It is calculated
							 * by using the number of SJM implants performed and the percentage
							 * of the total represented by the number of SJM implants performed.
							 * For example, if a physician performs 20 SJM implants and that 
							 * number represents 50% of all the implants performed by that surgeon,
							 * then the total number of implants is 20/.5 = 40 implants.*/
							calculatedTotal = sjmUsed/(Convert.formatDouble(sjmMedical));
							
							//Remaining numbers are calculated using the computed total.
							medUsed = calculatedTotal * (Convert.formatDouble(StringUtil.checkVal(data.get("medtronic").getValueText())));
							bosUsed = calculatedTotal * (Convert.formatDouble(StringUtil.checkVal(data.get("bostonScientific").getValueText())));
							unkUsed = calculatedTotal * (Convert.formatDouble(StringUtil.checkVal(data.get("unknown").getValueText())));
							
							// Put the data on the map
							actuals.put("sjmUsed", sjmUsed);
							actuals.put("medUsed", medUsed.intValue());
							actuals.put("bosUsed", bosUsed.intValue());
							actuals.put("unkUsed", unkUsed.intValue());
							actuals.put("calculatedTotal", calculatedTotal.intValue());
						}
					}
				}
			}
		}
		
		// Add the data elements to the request object
		req.setAttribute("actuals", actuals);
		req.setAttribute("permTotal", pTotal);
		req.setAttribute("trialTotal", tTotal);
		req.setAttribute("pFactor", permsFactor);
		req.setAttribute("tFactor", trialsFactor);
		req.setAttribute("ptlFactor", ptlFactor);
		req.setAttribute("rank", rank);

	}
	
	/**
	 * Calculates the int value from the value text
	 * @param vo
	 * @return
	 */
	private int getDataEle(BusinessPlanVO vo) {
		if (vo == null) return 0;
		
		Integer val = Convert.formatInteger(vo.getValueText());
		return val.intValue();
	}

	
	/**
	 * Retrieves the field ID and whether the field has a check box element
	 * @return
	 */
	private Map<String, Boolean> getFieldVals() {
		String schema = (String)getAttribute("customDbSchema");
		StringBuffer sql = new StringBuffer();
		sql.append("select business_plan_id, select_flg from ").append(schema);
		sql.append("ans_business_plan");
		log.info("BP Mapping: " + sql);
		
		Map<String, Boolean> data = new TreeMap<String, Boolean>();
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				data.put(rs.getString(1), Convert.formatBoolean(rs.getInt(2)));
			}
		} catch(SQLException sqle) {
			log.error("Error retrieving business plan fields", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		return data;
	}
	
	/**
	 * Calculates the business factor number based on the int passed in.
	 * @param num
	 * @return
	 */
	private int getFactorValue(int num) {
		
		if (num < 13) {
			return 1;
		} else if (num > 12 && num < 25) {
			return 2;
		} else if (num > 24 && num < 35) {
			return 3;
		} else {
			return 4;
		}
	}

}

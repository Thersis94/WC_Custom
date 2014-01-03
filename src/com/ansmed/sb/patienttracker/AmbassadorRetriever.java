package com.ansmed.sb.patienttracker;

// JDK 1.6
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;

// SB Base libs
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;

// Sitebuilder II libs
import com.smt.sitebuilder.action.tracker.AssigneeManager;
import com.smt.sitebuilder.action.tracker.data.ProfileNameComparator;
import com.smt.sitebuilder.action.tracker.vo.AssigneeVO;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerFacade;
import com.smt.sitebuilder.data.vo.FormTransactionVO;
import com.smt.sitebuilder.data.vo.GenericQueryVO;
import com.smt.sitebuilder.data.vo.QueryParamVO;
import com.smt.sitebuilder.data.vo.GenericQueryVO.Operator;

/****************************************************************************
* <b>Title</b>AmbassadorRetriever.java<p/>
* <b>Description: </b>Utility class for retrieving ambassador data (base records and 'black box' data).
* Data retrieval is filtered by the status of flags set by the calling class.
* <p/>
* <b>Copyright:</b> Copyright (c) 2011<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since Aug 18, 2011
* <b>Changes: </b>
* Oct 15, 2012: DBargerhuff; updated business rules per Phase3 #3002
****************************************************************************/
public class AmbassadorRetriever {
	
	public static final String FORM_ID = "c0a80237661feb561b100221e0a9c7f6";
	public static final String DAILY_AVAILABILITY_FIELD_ID = "c0a802419eef92a7e01d18d8f424f76";
	public static final String ORGANIZATION_ID = "SJM_AMBASSADORS";
	public static final Integer STATUS_ACTIVE = 10;
	protected final Logger log = Logger.getLogger(getClass());
	private GenericQueryVO query = null;
	private SMTDBConnection dbConn = null;
	private Map<String, Object> attributes = null;
	private Integer status = STATUS_ACTIVE;
	private Integer ambassadorType = new Integer(0);
	private boolean checkAssignmentLimit = false;
	private boolean ignoreDailyAvailability = false;
	
	/**
	 * 
	 */
	public AmbassadorRetriever() {	
		attributes = new HashMap<String, Object>();
	}

	/**
	 * Retrieves list of ambassadors including base record data, extended data, and profile data.  Default behavior
	 * is to retrieve all ambassadors with a status of 'active'.
	 * @return
	 * @throws SQLException
	 * @throws DatabaseException
	 */
	public List<AssigneeVO> retrieveAmbassadors() throws SQLException {
		Map<String, Integer> todaysNewCount = new HashMap<String, Integer>(); 
		this.retrieveTodaysAssignmentCount(todaysNewCount);
		List<AssigneeVO> ambs = this.retrieveBaseRecords(todaysNewCount);
		log.debug("ambassador base records retrieved: " + ambs.size());
		List<AssigneeVO> returnList = null;
		if (ambs.size() > 0) {
			if (query == null) this.buildDefaultQuery(ambs);
			returnList = this.retrieveExtendedData(ambs);
			if (returnList != null && returnList.size() > 0) {
				try {
					mergeProfileData(returnList);
				} catch (DatabaseException de) {
					log.error("Error merging base records, and profile data, ", de);
				}
				Collections.sort(returnList, new ProfileNameComparator());
			}
		}
		return returnList;
	}
	
	/**
	 * Retrieves base records for ambassadors
	 * @return
	 * @throws SQLException
	 */
	private List<AssigneeVO> retrieveBaseRecords(Map<String, Integer> todaysCount) throws SQLException {
		log.debug("retrieving base records...");
		StringBuffer sql = new StringBuffer();
		sql.append("select a.assignee_id, a.organization_id, a.assignee_type_id, a.pt_status_id, ");
		sql.append("a.assignee_profile_id, a.curr_assign_no, a.max_assign_no, a.max_daily_assign_no, ");
		sql.append("a.form_submittal_id, a.rank_no, a.create_dt from pt_assignee a "); 
		sql.append("where 1 = 1 ");
		sql.append("and a.organization_id = ? ");
		sql.append("and a.pt_status_id = ? ");
		if (ambassadorType > 0) {
			if (ambassadorType >= AssigneeManager.MIN_ADMIN_TYPE_ID) {
				sql.append("and a.assignee_type_id >= ? ");
			} else {
			sql.append("and a.assignee_type_id = ? ");
			}
		}
		
		log.debug("ambassador retrieve SQL: " + sql.toString());
		log.debug("organization ID | status | ambassadorType: " + ORGANIZATION_ID + " | " + status + " | " + ambassadorType);
		
		List<AssigneeVO> lavo = new ArrayList<AssigneeVO>();
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, ORGANIZATION_ID);
			ps.setInt(2, status);
			if (ambassadorType > 0) {
				if (ambassadorType >= AssigneeManager.MIN_ADMIN_TYPE_ID) {
					ps.setInt(3, AssigneeManager.MIN_ADMIN_TYPE_ID);
				} else {
					ps.setInt(3, ambassadorType);
				}
			}
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				AssigneeVO avo = new AssigneeVO();
				avo.setData(rs);
				// filter out non-admin ambassadors who have limited out on assignments (either daily total allowed or total allowed)
				if (checkAssignmentLimit && avo.getTypeId() < AssigneeManager.MIN_ADMIN_TYPE_ID) {
					/*
					 * Business rules as of 10-12-2012
					 * 	1. if not an ambassador admin and:
					 * 2. current number of assignments < assignment limit and:
					 * 3. no 'new' assignments have been assigned to this ambassador today or 
					 * the number of 'new' assignments assigned to this ambassador today < max 'new' daily assignment limit, then
					 * add this ambassador to the pool of 'available' ambassadors (//PHASE3 related to Item: #3002)
					 */
					if (avo.getCurrAssignmentNumber() < avo.getMaxAssignmentNumber()) {
						// has not reached total max #, check for daily limit
						//log.debug("assigneeId/newCount/maxNewCount: " + avo.getAssigneeId() + "/" + todaysCount.get(avo.getAssigneeId()) + "/" + avo.getMaxDailyAssignmentNumber());
						if (todaysCount.get(avo.getAssigneeId()) == null ||  
								(todaysCount.get(avo.getAssigneeId()) < avo.getMaxDailyAssignmentNumber())) {
							// has not reached daily limit, so add to list.
							lavo.add(avo);
						}
					}
				} else {
					lavo.add(avo);
				}
			}
		} catch (SQLException sqle) {
			throw new SQLException(sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) { }
		}
		return lavo;
	}
	
	/**
	 * Retrieves a count of the number of new assignments that have been assigned 'today'
	 * for each ambassador.
	 * @return
	 */
	private void retrieveTodaysAssignmentCount(Map<String, Integer> todaysCount) {
		log.debug("retrieving todays 'new' assignment count...");
		StringBuilder s = new StringBuilder();
		s.append("select assignee_id from PT_ASSIGNMENT ");
		s.append("where ASSIGN_DT >= ? order by assignee_id");
		PreparedStatement ps = null;
		Date today = Convert.formatStartDate(new Date());
		log.debug("today's count SQL: " + s.toString() + " | " + Convert.formatSQLDate(today));
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setDate(1, Convert.formatSQLDate(today));
			ResultSet rs = ps.executeQuery();
			String prev = "";
			String curr = null;
			int count = 0;
			while (rs.next()) {
				curr = rs.getString("assignee_id");
				if (curr.equals(prev)) {
					count++;
				} else {
					if (count == 0) {
						count++;
					} else {
						todaysCount.put(prev, count); // save the previous
						count = 1; // set the count to reflect current
					}
				}
				prev = curr;
			}
			// pick up the dangling record
			todaysCount.put(curr, count);
		} catch (SQLException sqle) {
			log.error("Error retrieving today's assignment count, ", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
		}
		log.debug("size of today's 'new' assignment count map: " + todaysCount.size());
	}

	/**
	 * Retrieves extended data from the black box and returns a list of assignee VOs that have
	 * extended data
	 * @param ambs
	 * @return
	 */
	private List<AssigneeVO> retrieveExtendedData(List<AssigneeVO> ambs) {
		// if nothing to look up, return
		if (ambs == null || ambs.isEmpty()) return null;
		log.debug("retrieving extended data, initial list size is: " + ambs.size());
		
		DataContainer dc = new DataContainer();
		dc.setQuery(query);
		DataManagerFacade dfm = new DataManagerFacade(attributes, dbConn);
		dc = dfm.loadTransactions(dc);
		Map<String, FormTransactionVO> aData = dc.getTransactions();
		log.debug("transactions retrieved: " + (dc.getTransactions() != null ? dc.getTransactions().size() : null));
		
		List<AssigneeVO> returnList = null;
		// Merge responses with the AssigneeVOs; keep only the VOs that have transactions
		if (aData != null) {
			returnList = new ArrayList<AssigneeVO>();
			log.debug("merging transaction data");
			for (String s : aData.keySet()) {
				for (AssigneeVO as : ambs) {
					if (s.equalsIgnoreCase(as.getSubmittalId())) {
						as.setTransaction(aData.get(s));
						// add to the return list if we have transaction data
						returnList.add(as);
					}
				}
			}
		}
		return returnList;
	}

	/**
	 * Merges profile data with base/extended data
	 * @param ambs
	 */
	private void mergeProfileData(List<AssigneeVO> ambs) throws DatabaseException {
		// retrieve profiles, merge with patients
		if (ambs == null || ambs.isEmpty()) return;
		List<String> profileIds = new ArrayList<String>();
		for (AssigneeVO avo : ambs) {
			profileIds.add(avo.getAssigneeProfileId());
		}
		
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		Map<String, UserDataVO> profiles = new HashMap<String, UserDataVO>();
		try {
			profiles = pm.searchProfileMap(dbConn, profileIds);
		} catch (DatabaseException de) {
			throw new DatabaseException();
		}
		for (AssigneeVO avo : ambs) {
			avo.setData(profiles.get(avo.getProfileId()).getDataMap());
		}
	}
		
	/**
	 * Builds query object with two query params: one that specifies the ambassador availability field 
	 * and a value representing "today", the other that specifies the form submittal ids of the 
	 * ambassadors in the list passed in.
	 * @param ambs
	 */
	private void buildDefaultQuery(List<AssigneeVO> ambs) {
		query = new GenericQueryVO(FORM_ID);
		query.setOrganizationId(ORGANIZATION_ID);
		
		if (! ignoreDailyAvailability) {
			// set up param for filtering by today's name value
			Calendar cal = GregorianCalendar.getInstance();
			String today = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, new Locale("US"));
			log.debug("searching for ambassadors available 'today': " + today);
			QueryParamVO q1 = new QueryParamVO(DAILY_AVAILABILITY_FIELD_ID, false);
			q1.setOperator(Operator.in);
			q1.setValues(new String[] {today});
			query.addConditional(q1);
		}
		
		// filter based on form submittal IDs
		//loop the patients to retrieve each assignee's formSubmittalId
		List<String> ids = new ArrayList<String>();
		for (AssigneeVO as : ambs) {
			ids.add(as.getSubmittalId());
		}
		QueryParamVO q2 = new QueryParamVO();
		q2.setColumnNm(GenericQueryVO.ColumnName.FORM_SUBMITTAL_ID);
		q2.setOperator(GenericQueryVO.Operator.in);
		q2.setValues(ids.toArray(new String[0]));
		query.addConditional(q2);
	}
	
	public void setQuery(GenericQueryVO query) {
		this.query = query;
	}

	public void setDbConn(SMTDBConnection dbConn) {
		this.dbConn = dbConn;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Integer getStatus() {
		return status;
	}

	public Integer getAmbassadorType() {
		return ambassadorType;
	}

	public void setAmbassadorType(Integer ambassadorType) {
		this.ambassadorType = ambassadorType;
	}

	public void setCheckAssignmentLimit(boolean checkAssignmentLimit) {
		this.checkAssignmentLimit = checkAssignmentLimit;
	}

	/**
	 * @param ignoreDailyAvailability If set to true, causes the query for 
	 * ambassadors to ignore 'today' as a filter when retrieving active ambassadors
	 */
	public void setIgnoreDailyAvailability(boolean ignoreDailyAvailability) {
		this.ignoreDailyAvailability = ignoreDailyAvailability;
	}

}

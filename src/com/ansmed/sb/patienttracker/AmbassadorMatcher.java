package com.ansmed.sb.patienttracker;

// JDK 1.6
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Apache Log4J
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// Sitebuilder II libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.StringUtil;

// SB Base Libs 2.0
import com.smt.sitebuilder.action.tracker.data.AssigneeRankComparator;
import com.smt.sitebuilder.action.tracker.vo.AssigneeVO;
import com.smt.sitebuilder.action.tracker.vo.PatientVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.vo.FormFieldVO;
import com.smt.sitebuilder.data.vo.FormTransactionVO;
import com.smt.sitebuilder.data.vo.FormVO;

/****************************************************************************
* <b>Title</b>AmbassadorMatcher.java<p/>
* <b>Description: Evaluates certain patient attributes against a filtered set of assignees to 
* return the assignee who is the 'best match' to the patient based on the results of the comparison.
* Returns an admin as the assignee if a 'best match' can't be found.</b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2011<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Dave Bargerhuff
* @version 1.0
* @since Mar 21, 2011
* <b>Changes: </b>
* Oct 12, 2012: David Bargerhuff - Phase 3 - modified business rules to add matching on contact method
****************************************************************************/
public class AmbassadorMatcher {
	
	public static final Integer MIN_TYPE_ADMIN = 20;
	protected final Logger log = Logger.getLogger(getClass());
	private PatientVO patient = null;
	private List<AssigneeVO> ambassadors = null;
	private List<String> excludeList = null;
	private Map<String, String> fieldMap = null;
	private FormVO ambassadorForm = null;
	//private Map<String, String> ambassadorFormFieldMap = null;
	private boolean adminMatch = false;
	private SMTDBConnection dbConn = null;
		
	public AmbassadorMatcher() {
		patient = new PatientVO();
		ambassadors = new ArrayList<AssigneeVO>();
		excludeList = new ArrayList<String>();
		loadCommonMatchFieldMap();
	}
	
	
	public static void main(String[] args) {
		PropertyConfigurator.configure("scripts/ans_log4j.properties");
		String driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
		String dbUrl = "jdbc:sqlserver://10.0.80.5:2007";
		String user = "sitebuilder_sb_user";
		String pwd = "sqll0gin";
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put(Constants.ENCRYPT_KEY, "s1l1c0nmtnT3chm0l0g13$JC");
		// get connection
		AmbassadorMatcher am = new AmbassadorMatcher();
		try {
			am.getDBConnection(driver, dbUrl, user, pwd);
		} catch (Exception e) {
			am.log.error("Error getting db connection, " + e);
			System.exit(-1);
		}
		// build a test patient (matchFactor, location of pain, age range, gender)
		PatientVO p = am.buildTestPatient("age", "both", "Over 70", "male", "phone");
		p.setPatientId("dummy");
		am.setPatient(p);
		// retrieve ambs
		AmbassadorRetriever ar = new AmbassadorRetriever();
		ar.setDbConn(am.dbConn);
		ar.setAttributes(attributes);
		ar.setCheckAssignmentLimit(true);
		
		// find match
		try {
			am.setAmbassadors(ar.retrieveAmbassadors());
			am.log.debug("ambassadors retrieved size: " + am.getAmbassadors().size());
		} catch (Exception e) {
			am.log.error("Error e, ", e);
		}
		AssigneeVO avo = am.findAmbassadorMatch();
		if (avo != null) {
			am.log.debug("assigneeId: " + avo.getAssigneeId());
			am.log.debug("name: " + avo.getFirstName() + " " + avo.getLastName());
		} else {
			am.log.debug("avo is null");
		}
		
		am.closeDBConnection();
	}
	
	
	/**
	 * Compares patient to each ambassador to determine best match
	 * @return
	 */
	public AssigneeVO findAmbassadorMatch() {
		AssigneeVO ambassadorMatch = null;
		adminMatch = false;
		if (patient.getPatientId() != null && ! ambassadors.isEmpty()) {
			this.buildExcludeList(patient.getPatientId());
			List<AssigneeVO> admins = new ArrayList<AssigneeVO>();
			List<AssigneeVO> candidates = new ArrayList<AssigneeVO>();
			// find admins
			parseAmbassadors(admins, candidates);
			// evaluate candidates to find a match
			ambassadorMatch = matchAmbassadors(candidates);
			// if not match is found, find an admin match (guaranteed to return a match)
			if (ambassadorMatch == null) ambassadorMatch = matchAdmin(admins);
		}
		return ambassadorMatch;
	}
	
	/**
	 * Compares patient to each ambassador to determine best match
	 * @param patient
	 * @param assignees
	 * @return
	 */
	public AssigneeVO findAmbassadorMatch(PatientVO patient, List<AssigneeVO> ambassadors) {
		setPatient(patient);
		setAmbassadors(ambassadors);
		return findAmbassadorMatch();
	}
	
	/**
	 * Queries the assignment table for all active assignments to this patient.  Ambassador
	 * assignee_id values are added to the exclude list so that automated assignment
	 * doesn't attempt to create a duplicate assignee/patient relationship.
	 * @param patientId
	 */
	private void buildExcludeList(String patientId) {
		log.debug("looking for ambassadors already assigned to this patient...");
		StringBuffer sql = new StringBuffer();
		sql.append("select assignee_id from pt_assignment ");
		sql.append("where patient_id = ? and assignment_status_id < ? ");
		log.debug("exclude list SQL: " + sql.toString());
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, patientId);
			ps.setInt(2, SJMTrackerConstants.STATUS_COMPLETED);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				this.addToExcludeList(rs.getString("assignee_id"));
			}
			log.debug("active assignments to this patient: " + excludeList.size());
		} catch (SQLException sqle) {
			log.error("Error looking for ambassadors already assigned to patient, ", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
		}
	}
	
	/**
	 * Parses ambassadors into admins and candidate lists
	 * @param admins
	 * @param candidates
	 */
	private void parseAmbassadors(List<AssigneeVO> admins, List<AssigneeVO> candidates) {
		for (AssigneeVO a : ambassadors) {
			// if admin, add to list
			if (a.getTypeId() >= MIN_TYPE_ADMIN) {
				admins.add(a);
			} else {
				// if ambassador is not already assigned to the patient (i.e. not in exludeList), add to candidates list
				if (! excludeList.contains(a.getAssigneeId())) { 	candidates.add(a); }
			}
		}
	}
	
	/**
	 * 
	 * @param candidates
	 * @return
	 */
	private AssigneeVO matchAmbassadors(List<AssigneeVO> candidates) {
		log.debug("comparing ambassadors to patient criteria");
		Map<String, AssigneeVO> matches = new HashMap<String,AssigneeVO>();
		Map<String, Integer> scores = new HashMap<String, Integer>();
		AssigneeVO ambassadorMatch = null;
		// build map of patient's data; key set mirrors 'fieldMap' key set
		Map<String, String> patientData = this.parsePatientData(patient);
		//log.debug("matchFactor is: " + patientData.get("matchFactor"));
		// loop the ambassadors and evaluate
		int baseScore = 0;
		log.debug("looking for primary match candidates...");
		for (AssigneeVO amb : candidates) {
			Map<String, String> ambData = this.parseAmbassadorData(amb);
			if (isPrimaryMatch(ambData, patientData)) {
				// compare amb data and patient data to compute raw base score
				baseScore = this.computeBaseScore(ambData, patientData, true);
				// add to score because the amb matched the 'primary factor'
				baseScore += 16;
				// if baseScore is positive, then we have matched on at least one attribute
				scores.put(amb.getAssigneeId(), new Integer(baseScore));
				matches.put(amb.getAssigneeId(), amb);
			}
			// ensure that baseScore is reset to zero.
			baseScore = 0;
		}
		
		// if no matches found, perform 2nd pass, assign closest matching amb.
		if (matches.isEmpty())	{
			log.debug("found no primary match candidates, making second pass...");
			// no 'match factor' match found, second pass
			for (AssigneeVO amb : candidates) {
				Map<String, String> ambData = this.parseAmbassadorData(amb);
				baseScore = this.computeBaseScore(ambData, patientData, false);
				if (baseScore > 0) {
					scores.put(amb.getAssigneeId(), new Integer(baseScore));
					matches.put(amb.getAssigneeId(), amb);
				}
			}	
		}
		
		// 10/12/2012 PHASE3 #3005; if still no matches found, assign next available ambassador
		if (matches.isEmpty()) {
			log.debug("found no matches during second pass...choosing next available ambassador...");
			log.debug("candidates list size: " + candidates.size());
			if (candidates.size() > 0) {
				// sort by rank and by current # of assignments and choose first non-admin in list.
				Collections.sort(candidates, new AssigneeRankComparator());
				// get the last in the list.
				ambassadorMatch = candidates.get(candidates.size() - 1);
			}
		} else { // found matches after 2nd pass, determine match by score.
			ambassadorMatch = this.findAmbassadorByScore(scores, matches);
		}
		//ambassadorMatch = this.findAmbassadorByScore(scores, matches);
		return ambassadorMatch;
	}

	
	/**
	 * Parses certain patient data into a map for use in comparing with ambassador data
	 * @param patient
	 * @return
	 */
	private Map<String, String> parsePatientData(PatientVO patient) {
		log.debug("parsing patient data");
		Map<String, String> patientData = new HashMap<String, String>();
		Map<String, FormFieldVO> pData = patient.getTransaction().getCustomData();
		if (pData == null || pData.isEmpty()) {
			return patientData;
		}
		// set key/val pairs
		for (String key : fieldMap.keySet()) {
			//log.debug("patient key: " + key);
			if (key.equals("spanish")) {
				continue;
			} else if (key.equals("matchFactor")) {
				// set match factor key/val
				patientData.put("matchFactor", this.findMatchFactorKey(pData));
			} else if (key.equals("gender")) {
				patientData.put("gender", patient.getGenderCode());
			} else {
				FormFieldVO vo = pData.get(fieldMap.get(key));
				if (vo != null) {
					if (vo.getResponses() == null || vo.getResponses().isEmpty()) {
						patientData.put(key, null);
					} else {
						patientData.put(key, vo.getResponses().get(0));
					}
				} else {
					patientData.put(key, null);
				}
			}
			//log.debug("value: " + patientData.get(key));
		}
		log.debug("size of patientData map: " + patientData.size());
		return patientData;
	}
	
	
	/**
	 * Determines the 'most important match factor' key for use when attempting
	 * to match an ambassador with the patient.
	 * @param pData
	 * @return
	 */
	private String findMatchFactorKey(Map<String, FormFieldVO> pData) {
		if (pData == null || pData.isEmpty()) return null;
		String matchFactorFieldVal = null;
		// first, determine the most important match factor for this patient.
		FormFieldVO matchFactorField = pData.get(fieldMap.get("matchFactor"));
		if (matchFactorField != null) {
			if (matchFactorField.getResponses() != null) {
				if (StringUtil.checkVal(matchFactorField.getResponses().get(0)).length() > 0) {
					matchFactorFieldVal = matchFactorField.getResponses().get(0).toLowerCase();
					// set the match factor key value
					if (matchFactorFieldVal.equalsIgnoreCase("unknown")) {
						matchFactorFieldVal = null;
					} else {
						for (String key : fieldMap.keySet()) {
							if (key.equals("matchFactor")) continue;
							if (matchFactorFieldVal.contains(key)) {
								matchFactorFieldVal = key;
								break;
							}
						}
					}
				}
			}
		}
		log.debug("patient's matchFactorFieldVal: " + matchFactorFieldVal);
		return matchFactorFieldVal;
	}
	
	/**
	 * Parses ambassador data into a map whose key set mirrors the fieldMap key set.
	 * @param ambassador
	 * @return
	 */
	private Map<String, String> parseAmbassadorData(AssigneeVO ambassador) {
		log.debug("parsing ambassador data");
		Map<String, String> ambData = new HashMap<String, String>();
		Map<String, FormFieldVO> aData = ambassador.getTransaction().getCustomData();
		if (aData == null || aData.isEmpty()) return ambData;
		// set key/val pairs
		for (String key : fieldMap.keySet()) {
			//log.debug("amb key: " + key);
			if (key.equals("matchFactor")) {
				continue;
			} else if (key.equals("gender")) {
				ambData.put("gender", ambassador.getGenderCode());
			} else {
				FormFieldVO vo = aData.get(fieldMap.get(key));
				if (vo != null) {
					if (vo.getResponses() == null || vo.getResponses().isEmpty()) {
						ambData.put(key, null);
					} else {
						ambData.put(key, vo.getResponses().get(0));
					}
				} else {
					ambData.put(key, null);
				}
			}
			//log.debug("value: " + ambData.get(key));
		}
		log.debug("size of ambData map: " + ambData.size());
		return ambData;
	}
	
	/**
	 * Compares specific ambassador and patient form field values to determine a base score
	 * for how well an ambassador matches a patient
	 * @param ambData
	 * @param patientData
	 * @return
	 */
	private int computeBaseScore(Map<String, String> ambData, Map<String, String> patientData, boolean ignorePatientMatchFactor) {
		log.debug("computing base score");
		int baseScore = 0;
		// check for empty ambassador/patient data, filter out empty ambassadors
		if (ambData.isEmpty() || patientData.isEmpty()) return baseScore;
		for (String fmKey : fieldMap.keySet()) {
			//log.debug("scoring key, patientVal/ambVal: " + fmKey + ", " + patientData.get(fmKey) + "/" + ambData.get(fmKey));			
			// skip certain keys
			if (fmKey.equals("matchFactor") || fmKey.equals("spanish")) continue;
			// skip the key that matches the patient's match factor, or not.
			if (fmKey.equals(patientData.get("matchFactor"))) {
				if (ignorePatientMatchFactor) continue;
			}
			if (patientData.get(fmKey) != null) {
				if (fmKey.equals("contactMethod")) {
					if (ambData.get(fmKey) != null) {
						if (ambData.get(fmKey).toLowerCase().indexOf("both") > -1 || 
								patientData.get(fmKey).equalsIgnoreCase(ambData.get(fmKey))) {
							baseScore += 1;
						}	
					}
				} else {
					if (patientData.get(fmKey).equalsIgnoreCase(ambData.get(fmKey))) {
						if (fmKey.equals("pain")) {
							baseScore += 8;
						} else if (fmKey.equals("age")) {
							baseScore += 4;
						} else if (fmKey.equals("gender")) {
							baseScore += 2;
						}
					}
				}
			}
		}
		log.debug("baseScore is: " + baseScore);
		return baseScore;
	}
	
	/**
	 * Determines if ambassador data matches the most important match factor specified by the patient
	 * @param aData
	 * @param pData
	 * @return
	 */
	private boolean isPrimaryMatch(Map<String, String> aData, Map<String, String> pData) {
		boolean isMatch = false;
		String matchFactor = pData.get("matchFactor");
		if (matchFactor == null) {
			// patient didn't specify a most important factor so all ambassadors are considered
			isMatch = true;
		} else {
			// matchFactor is specified
			String ambMatchVal = aData.get(matchFactor);
			if (ambMatchVal != null) {
				if (matchFactor.equals("spanish")) {
					if (ambMatchVal.equalsIgnoreCase("yes")) {
						isMatch = true;
					}				
				} else {
					if (pData.get(matchFactor) != null && (pData.get(matchFactor).equalsIgnoreCase(ambMatchVal))) {
						isMatch = true;
					}
				}
			}
		}
		log.debug("isPrimaryMatch?: " + isMatch);
		return isMatch;
	}
			
	/**
	 * Evaluates scores and rank of each ambassador that matched at least one patient attribute.
	 * @param matchScore
	 * @param matches
	 * @return
	 */
	private AssigneeVO findAmbassadorByScore(Map<String, Integer> scores, Map<String, AssigneeVO> matches) {
		AssigneeVO ambMatch = null;
		log.debug("finding ambassador match by score...");
		// evaluate all matches
		if (! matches.isEmpty()) {
			log.debug("matches map size is: " + matches.size());
			if (matches.size() == 1) {
				for (String k : matches.keySet()) {
					ambMatch = matches.get(k);	
				}
			} else {
				// compare score and rank to find best match
				int maxScore = 0;
				int currScore = 0;
				String maxId = "";
				int maxRank = 0;
				for (String k : matches.keySet()) {
					AssigneeVO a = matches.get(k);
					currScore = scores.get(a.getAssigneeId());
					if (currScore > maxScore) {
						maxScore = currScore;
						maxId = a.getAssigneeId();
						maxRank = a.getRankNumber();
					} else if (currScore == maxScore) {
						if (a.getRankNumber() > maxRank) {
							maxId = a.getAssigneeId();
							maxRank = a.getRankNumber();
						}
					}
					//log.debug("max score/Id/rank: " + maxScore + "/" + maxId + "/" + maxRank);
				}
				ambMatch = matches.get(maxId);
			}
		}
		return ambMatch;
	}
	
	/**
	 * 
	 * @param admins
	 * @return
	 */
	private AssigneeVO matchAdmin(List<AssigneeVO> admins) {
		AssigneeVO adminMatched = null;
		/* *****************
		// TODO return admin match based on admin's geographic proximity to patient
		// 1. get patient's geographical location (or zipcode)
		// 2. do a locator search for surgeons in proximity to patient
		// 3. get area associated with surgeon's rep's region
		// 4. match to admin area
		// 5. return admin assignee
		*/
		// this assigns the admin with the least number of current assignments as the match
		int currMin = 10000;
		for (AssigneeVO admin : admins) {
			if (admin.getCurrAssignmentNumber() < currMin) {
				adminMatched = admin;
			}
		}
		adminMatch = true;
		return adminMatched;
	}

	/**
	 * Map of comparison attributes and their corresponding form field IDs
	 */
	private void loadCommonMatchFieldMap() {
		fieldMap = new HashMap<String, String>();
		fieldMap.put("age", "c0a80237662b11dbb5c0326460296ed1");
		fieldMap.put("pain", "c0a8023766282af6d33fce93e7d2d285");
		fieldMap.put("state", "c0a80241ad8ec5b1902f1fc5b9f6ce42");
		fieldMap.put("spanish", "c0a802416045328a4aa26d13ee7bc227");
		fieldMap.put("gender", "c0a8023766290edfaabaeaee10fd075c");
		fieldMap.put("matchFactor", "c0a80237662d0a7775f2231c9c7c3743");
		fieldMap.put("contactMethod", "c0a8023766250c46418ec22b852e7838");
	}
	
	public PatientVO getPatient() {
		return patient;
	}

	public void setPatient(PatientVO patient) {
		this.patient = patient;
	}

	public List<AssigneeVO> getAmbassadors() {
		return ambassadors;
	}

	public void setAmbassadors(List<AssigneeVO> ambassadors) {
		this.ambassadors = ambassadors;
	}

	public List<String> getExcludeList() {
		return excludeList;
	}

	public void setExcludeList(List<String> excludeList) {
		this.excludeList = excludeList;
	}
	
	public void addToExcludeList(String assigneeId) {
		excludeList.add(assigneeId);
	}

	/**
	 * @return the ambassadorForm
	 */
	public FormVO getAmbassadorForm() {
		return ambassadorForm;
	}

	/**
	 * @param ambassadorForm the ambassadorForm to set
	 */
	public void setAmbassadorForm(FormVO ambassadorForm) {
		this.ambassadorForm = ambassadorForm;
	}

	/**
	 * @return the adminMatch
	 */
	public boolean isAdminMatch() {
		return adminMatch;
	}

	/**
	 * @param adminMatch the adminMatch to set
	 */
	public void setAdminMatch(boolean adminMatch) {
		this.adminMatch = adminMatch;
	}

	/**
	 * @return the dbConn
	 */
	public SMTDBConnection getDbConn() {
		return dbConn;
	}

	/**
	 * @param dbConn the dbConn to set
	 */
	public void setDbConn(SMTDBConnection dbConn) {
		this.dbConn = dbConn;
	}
	
	/**
	 * Gets a db connection
	 * @throws Exception
	 */
	private void getDBConnection(String dbDriver, String dbUrl, String dbUser, String dbPassword) throws Exception {
		log.debug("getting db connection...");
		DatabaseConnection dbc = new DatabaseConnection(dbDriver,dbUrl,dbUser,dbPassword);
		dbConn = new SMTDBConnection(dbc.getConnection());
	}
	
	/**
	 * Closes db connection
	 */
	private void closeDBConnection() {
		if (dbConn != null) {
			try {
				dbConn.close();
			} catch (Exception e) {
				log.error("could not close db connection, " + e);
			}
		}
	}
	
	private PatientVO buildTestPatient(String matchFactor, String pain, String age, String gender, String contactMethod) {
		log.debug("building test patient");
		PatientVO p = new PatientVO();

		Map<String, FormFieldVO> customData = new HashMap<String, FormFieldVO>();
		
		FormFieldVO ffvo = new FormFieldVO();
		ffvo.setFormFieldId(fieldMap.get("matchFactor"));
		ffvo.addResponse(matchFactor);
		customData.put(ffvo.getFormFieldId(), ffvo);
		
		ffvo = new FormFieldVO();
		ffvo.setFormFieldId(fieldMap.get("pain"));
		ffvo.addResponse(pain);
		customData.put(ffvo.getFormFieldId(), ffvo);
		
		ffvo = new FormFieldVO();
		ffvo.setFormFieldId(fieldMap.get("age"));
		ffvo.addResponse(age);
		customData.put(ffvo.getFormFieldId(), ffvo);
		
		ffvo = new FormFieldVO();
		ffvo.setFormFieldId(fieldMap.get("contactMethod"));
		ffvo.addResponse(contactMethod);
		customData.put(ffvo.getFormFieldId(), ffvo);
		
		p.setGenderCode(gender);

		FormTransactionVO fvo = new FormTransactionVO();
		fvo.setCustomData(customData);
		p.setTransaction(fvo);
		return p;
	}
	
}

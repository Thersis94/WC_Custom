package com.depuysynthesinst.lms;

// Java 7
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;


// Apache log4j
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


import com.depuysynthesinst.DSIUserDataVO;
// SMTBaseLibs 2
import com.siliconmtn.action.ActionException;


//LMS SOAP Api
import cfc.DSIResidentsCFCInvocationExceptionException;
import cfc.DSIResidentsStub;
import cfc.DSIResidentsStub.CourseList;
import cfc.DSIResidentsStub.CourseListResponse;
import cfc.DSIResidentsStub.CreateUser;
import cfc.DSIResidentsStub.CreateUserResponse;
import cfc.DSIResidentsStub.Entry1;
import cfc.DSIResidentsStub.Entry2;
import cfc.DSIResidentsStub.GetUserActiveIDbyEmail;
import cfc.DSIResidentsStub.GetUserActiveIDbyEmailResponse;
import cfc.DSIResidentsStub.GetUserHoldingIDbyEmail;
import cfc.DSIResidentsStub.GetUserHoldingIDbyEmailResponse;
import cfc.DSIResidentsStub.JKTest;
import cfc.DSIResidentsStub.JKTestResponse;
import cfc.DSIResidentsStub.Map1;
import cfc.DSIResidentsStub.Map2;
import cfc.DSIResidentsStub.MigrateUser;
import cfc.DSIResidentsStub.MigrateUserResponse;
import cfc.DSIResidentsStub.RegisterUserforCourse;
import cfc.DSIResidentsStub.RegisterUserforCourseResponse;
import cfc.DSIResidentsStub.TotalUserPoints;
import cfc.DSIResidentsStub.TotalUserPointsResponse;
import cfc.DSIResidentsStub.UpdateUser;
import cfc.DSIResidentsStub.UpdateUserResponse;
import cfc.DSIResidentsStub.UserCourseList;
import cfc.DSIResidentsStub.UserCourseListResponse;


/****************************************************************************
 * <b>Title: </b>LMSWSClient.java <p/>
 * <b>Project: </b>DSI-WS2 <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2015<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0<p/>
 * @since May 28, 2015<p/>
 *<b>Changes: </b>
 * May 28, 2015: David Bargerhuff: Created class.
 ****************************************************************************/
public class LMSWSClient {
	
	public static final String CFG_SECURITY_KEY = "dsiTTLMSApiKey"; //from the sb_config file
	
	private static Logger log;
	private String securityKey;
	private DSIResidentsStub dsi;
	private Map<Integer,String> errorCodeMap;
	
	/**
	 * 
	 */
	public LMSWSClient(String securityKey) {
		log = Logger.getLogger(LMSWSClient.class);
		this.securityKey = securityKey;
		initErrorMap();
	}
	
	public static void main (String[] args) {
		String secKeySMT = "183742B231C69E28";
		//String secKeyTest = "SUSR802";
		PropertyConfigurator.configure("scripts/dsi/lms_soap_log4j.properties");
		LMSWSClient tc = new LMSWSClient(secKeySMT);
		
		try {
			log.debug("JKTest return value: " + tc.doJKTest());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	 
	/**
	 * This tests communication to the WebService.  A response value of 1 indicates
	 * success.
	 * @throws RemoteException
	 * @throws DSIResidentsCFCInvocationExceptionException 

	 */
	public double doJKTest() throws ActionException {

		JKTest jTest = new JKTest();
		JKTestResponse jRes = null;
		try {
			// make sure we have a client stub
			checkWSStub();
			
			// test WS 
			jRes = dsi.jKTest(jTest);
		} catch (Exception e) {
			throw new ActionException(e);
		}
		
		// JKTest return value: 1.0
		
		return jRes.get_return();
	}
	
	/**
	 * Creates new user in the LMS based on given information.
	 * @param user
	 * @return TTLMSID of newly created user
	 * @throws ActionException
	 */
	public double createUser(DSIUserDataVO user) throws ActionException {
		
		// build request
		CreateUser cu = new CreateUser();
		cu.setSecurityKey(securityKey);
		cu.setDSI_ID(user.getDsiId()); // required
		cu.setFirstName(user.getFirstName()); // required
		cu.setLastName(user.getLastName()); // required
		cu.setEmail(user.getEmailAddress()); // required
		cu.setCountry(user.getCountryCode());
		cu.setHospital(user.getHospital());
		cu.setSpecialty(user.getSpecialty());
		cu.setProfession(user.getProfession());
		cu.setEligibleProgram(convertBoolean(user.isEligible()));
		cu.setVerified(convertBoolean(user.isVerified()));
		
		// make WS call and get response
		CreateUserResponse cur = null;
		try {
			// make sure we have a client stub
			checkWSStub();
				
			// get response
			cur = dsi.createUser(cu);
		} catch (Exception e) {
			throw new ActionException(e);
		}
		return cur.get_return();
	}

	/**
	 * Updates user in the LMS using the given information. API returns user's TTLMSID.
	 * @param user
	 * @return TTLMSID of update user
	 * @throws ActionException
	 */
	public double updateUser(DSIUserDataVO user) throws ActionException {

		// build request
		UpdateUser uu = new UpdateUser();
		uu.setSecurityKey(securityKey);
		uu.setFirstName(user.getFirstName());
		uu.setLastName(user.getLastName());
		uu.setEmail(user.getEmailAddress());
		uu.setDSI_ID(user.getDsiId());
		uu.setCountry(user.getCountryCode());
		uu.setHospital(user.getHospital());
		uu.setSpecialty(user.getSpecialty());
		uu.setProfession(user.getProfession());
		uu.setEligibleProgram(convertBoolean(user.isEligible()));
		uu.setVerified(convertBoolean(user.isVerified()));
		
		// make WS call and get response
		UpdateUserResponse uur = null;
		try {
			// make sure we have a client stub
			checkWSStub();
				
			// 	get response
			uur = dsi.updateUser(uu);
			log.debug("UpdateUserResponse val: " + uur.get_return());
		} catch (Exception e) {
			throw new ActionException(e);
		}
		return uur.get_return();
	}
	
	/**
	 * Migrates a user's legacy LMS user account to a new account(?).
	 * @param user
	 * @return TTLMSID of migrated user.
	 * @throws ActionException
	 */
	public double migrateUser(DSIUserDataVO user) 
			throws ActionException {
		
		// format request data
		MigrateUser mu = new MigrateUser();
		mu.setSecurityKey(securityKey);
		mu.setDSI_ID(user.getDsiId());
		mu.setSynthesID(user.getSynthesId());
		mu.setFirstName(user.getFirstName());
		mu.setLastName(user.getLastName());
		mu.setEmail(user.getEmailAddress());
		mu.setCountry(user.getCountryCode());
		mu.setHospital(user.getHospital());
		mu.setProfession(user.getProfession());
		mu.setSpecialty(user.getSpecialty());
		mu.setEligibleProgram(convertBoolean(user.isEligible()));
		mu.setVerified(convertBoolean(user.isVerified()));
		
		MigrateUserResponse mur = null;
		try {
			// make sure we have a client stub
			checkWSStub();
		
			// make WS call and get response
			 mur = dsi.migrateUser(mu);

			 // debug
			 log.debug("MigrateUserResponse return val: " + mur.get_return());
		} catch (Exception e) {
			throw new ActionException(e);
		}

		return mur.get_return();
		
	}
	
	/**
	 * Looks up user by email address.  API returns user's TTLMSID.
	 * @param emailAddress
	 * @return JSON structure with:
	 * TTLMSID
	 * SynthesID
	 * Eligible Program
	 * Verified
	 * Error with value of -2 if user does not have a legacy user account
	 * @throws ActionException
	 */
	public Map<Object,Object> getUserActiveIDByEmail(String emailAddress) 
			throws ActionException {

		// build request
		GetUserActiveIDbyEmail gube = new GetUserActiveIDbyEmail();
		gube.setSecurityKey(securityKey);
		gube.setEmail(emailAddress);
		
		// make WS and get response
		GetUserActiveIDbyEmailResponse guder = null;
		Map<Object,Object> ret = new HashMap<>();
		try {
			// make sure we have a client stub
			checkWSStub();
					
			// get response
			guder = dsi.getUserActiveIDbyEmail(gube);
			Map1 m1 = new Map1();
			m1 = guder.get_return();
			log.debug("getUserActiveIDbyEmailResponse raw response: " + m1);

			// debug
			if (m1 != null && m1.getEntry() != null) {
				for (Entry1 e1 : m1.getEntry()) {
					log.debug("key/value: " + e1.getKey() + "|" + e1.getValue());
					ret.put(e1.getKey(),e1.getValue());
				}
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}
		
		return ret;
	}
	
	/**
	 * Performs a lookup to see if this user has a legacy LMS account.
	 * @param emailAddress
	 * @return JSON structure with:
	 * TTLMSID
	 * SynthesID
	 * Eligible Program
	 * Verified
	 * Error with value of -2 if user does not have a legacy user account
	 * @throws ActionException
	 */
	public Map<Object,Object> getUserHoldingIDByEmail(String emailAddress) 
			throws ActionException {

		// build request
		GetUserHoldingIDbyEmail gusi = new GetUserHoldingIDbyEmail();
		gusi.setSecurityKey(securityKey);
		gusi.setEmail(emailAddress);
		
		// make WS and get response
		GetUserHoldingIDbyEmailResponse gusr = null;
		Map<Object,Object> ret = new HashMap<>();
		try {
			// make sure we have a client stub
			checkWSStub();
			
			// make WS call
			gusr = dsi.getUserHoldingIDbyEmail(gusi);
			Map2 m2 = gusr.get_return();
			log.debug("getUserHoldingIDbyEmailResponse raw response: " + m2);
			
			// parse the returned map into a standard Map.
			if (m2 != null && m2.getEntry() != null) {
				for (Entry2 e2 : m2.getEntry()) {
					log.debug("key/value: " + e2.getKey() + "|" + e2.getValue());
					ret.put(e2.getKey(), e2.getValue());
				}
			}
		} catch (Exception e) {
			throw new ActionException(e);
		}

		return ret;
	}
	
	/**
	 * Returns the total number of points for a specified user.
	 * @param user
	 * @return
	 * @throws ActionException
	 */
	public double getTotalUserPoints(String dsiId) 
			throws ActionException {

		// build request
		TotalUserPoints tup = new TotalUserPoints();
		tup.setSecurityKey(securityKey);
		tup.setDSI_ID(dsiId);
		
		// make WS call and get response
		TotalUserPointsResponse tupr = null;
		try {
			// make sure we have a client stub
			checkWSStub();
					
			// get response
			tupr = dsi.totalUserPoints(tup);
			log.debug("TotalUserPoints val: " + tupr.get_return());
		} catch (Exception e) {
			throw new ActionException(e);
		}
		
		return tupr.get_return();
	}
	
	/**
	 * Returns an Object array of JSON-formatted structures containing a list of 
	 * all available courses and user-data related to these courses.  Each element
	 * of the array is comprised of:
	 * C_ID (course ID): 
	 * C_Name (course name):
	 * C_Description (course description):
	 * InDevelopment
	 * BodyRegion
	 * CourseComplete
	 * ForPoints
	 * Points
	 * PointsEarnable
	 * @param user
	 * @return
	 * @throws ActionException
	 */
	public Object[] getUserCourseList(String dsiId) 
			throws ActionException {

		// build request
		UserCourseList ucl = new UserCourseList();
		ucl.setSecurityKey(securityKey);
		ucl.setDSI_ID(dsiId);
		
		// make WS call and get response
		UserCourseListResponse uclr = null;
		try {
			// make sure we have a client stub
			checkWSStub();
		
			// get response
			uclr = dsi.userCourseList(ucl);
			/*
			Object[] courseList = uclr.get_return();
			// debug
			for (Object o : courseList) {
				log.debug("Course: " + o);
			}
			return courseList;
			*/
		} catch (Exception e) {
			throw new ActionException(e);
		}
		return uclr.get_return();
	}
	
	/**
	 * Gets all available courses.
	 * JSON structure is returned which is an Object array (Object[]).  Each element
	 * of the array contains:
	 * Course ID
	 * Course Name
	 * Course Description
	 * InDevelopment
	 * Credits
	 * Specialty ID List
	 * Body Region ID List
	 * @return
	 * @throws ActionException
	 */
	public Object[] getCourseList() throws ActionException {

		// build request
		CourseList cl = new CourseList();
		cl.setSecurityKey(securityKey);
		
		// make WS call and get response.
		CourseListResponse clr = null;
		try {
			// make sure we have a client stub
			checkWSStub();

			// make WS call
			clr = dsi.courseList(cl);
			/*
			log.debug("Logging course list response: ");
			for (Object o : clr.get_return()) {
				log.debug(o);
			}
			log.debug("End of course list response.");
			 */
		} catch (Exception e) {
			throw new ActionException(e);
		}
		return clr.get_return();
	}
	
	/**
	 * Assigns a course to a user.
	 * @param user
	 * @param courseId
	 * @return
	 * @throws ActionException
	 */
	@SuppressWarnings("unused")
	public double registerUserForCourse(String dsiId, double courseId) 
			throws ActionException {

		// build request
		RegisterUserforCourse rufc = new RegisterUserforCourse();
		rufc.setSecurityKey(securityKey);
		rufc.setDSI_ID(dsiId);
		rufc.setC_ID(courseId);
		
		// make call and get response
		RegisterUserforCourseResponse rufcr = null;
		try {
			// make sure we have a client stub
			checkWSStub();
			
			// make the call
			rufcr = dsi.registerUserforCourse(rufc);
		} catch (Exception e) {
			throw new ActionException(e);
		}
		
		log.debug("registerUserforCourseResponse: " + rufcr.get_return());
		return rufcr.get_return();
		
	}
	
	/**
	 * Performs a lookup on the supplied error code and returns the message 
	 * associated with that error code.  If the error code supplied is null or invalid,
	 * a corresponding message is returned.
	 * @param errorCode
	 * @return
	 */
	public String parseErrorCode(Integer errorCode) {
		if (errorCode == null || (errorCode > -1 || errorCode < -6)) {
			return "Invalid error code supplied.";
		}
		return errorCodeMap.get(errorCode);
	}
	
	/**
	 * Initializes the error Map that contains error code key values mapped to 
	 * the key's meaning.
	 */
	private final void initErrorMap() {
		errorCodeMap = new HashMap<>();
		errorCodeMap.put(-1,"Can't find new group (Internal Error)");
		errorCodeMap.put(-2,"Requested user doesn't exist");
		errorCodeMap.put(-3,"Can't find new user (Internal Error)");
		errorCodeMap.put(-4,"User already exists (Based on SynthesID)");
		errorCodeMap.put(-5,"Requested course doesn't exist");
		errorCodeMap.put(-6,"Bad security code");
	}
	
	/**
	 * Checks to see if a client stub has been instantiated prior to a WS call
	 * being made.  If not, a client stub is instantiated.  An exception is thrown
	 * if a client stub cannot be instantiated.
	 * @throws ActionException
	 */
	private void checkWSStub() throws ActionException {
		if (dsi == null) {
			try {
				dsi = new DSIResidentsStub();
			} catch (Exception e) {
				throw new ActionException("Error instantiating LMS WS stub, ", e);
			}
		}
	}
	
	/**
	 * Utility method to convert boolean into specific value.
	 * @param val
	 * @return
	 */
	private double convertBoolean(boolean val) {
		if (val) return 1.0;
		return 0.0;
	}
	
}

package com.depuysynthesinst.lms;

import java.util.List;

import com.depuysynthesinst.DSIUserDataVO;
import com.siliconmtn.security.UserDataVO;

/****************************************************************************
 * <b>Title: </b>TestLMS.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2015<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0<p/>
 * @since Jun 29, 2015<p/>
 *<b>Changes: </b>
 * Jun 29, 2015: David Bargerhuff: Created class.
 ****************************************************************************/
public class TestLMS {

	/**
	 * 
	 */
	public TestLMS() {
	}
	
	public static void main(String[] args) {
		LMSWSClient client = new LMSWSClient("183742B231C69E28");
		
		try {
			//System.out.println("doJKTest: " + client.getUserActiveIDByEmail("jmckain@siliconmtn.com"));
			String email = "depuy3migration@test.com";
			System.out.println(client.getUserHoldingIDByEmail(email));
			System.out.println(client.getUserActiveIDByEmail(email));
			// format a test user VO and set on client.
			DSIUserDataVO du = formatTestUser();
			
			// create user
			//System.out.println("createUser TTLMSID response: " + client.createUser(du));
			
			// update user
			du.setEligible(true);
			du.setVerified(true);
			du.setTtLmsId(163659);
			du.setEmailAddress(email);
			du.setProfileId("c0a8024a906c46e9f68bf2cd5273a381");
			//System.out.println("updateUser TTLMSID response: " + client.updateUser(du));
			
			// get all available courses
//			Object[] allCourses = client.getCourseList();
//			System.out.println("allCourses response: " + Arrays.toString(allCourses));
			
			// get all courses for this user
			List<MyLMSCourseVO> userCourseList = client.getUserCourseList(du.getDsiId());
			for (MyLMSCourseVO vo : userCourseList) {
				System.out.println(vo.getId() + "\t" + vo.isComplete() + "\t" + vo.isInDevelopment() + "\t" + vo.getPointsEarnable() + "\t" + vo.getPOINTS());
			}
			
			
			// get users total points
			System.out.println("total user points response: " + client.getTotalUserPoints(du.getDsiId()));
			/*
			
			// get active user by email address
			System.out.println("get active user ID by email response: " + client.getUserActiveIDByEmail(du.getEmailAddress()));
			
			// check to see if this user has a legacy acct
			System.out.println("get user holding ID by email: " + client.getUserHoldingIDByEmail(du.getEmailAddress()));
			
			// migrate user
			
			DSIUserDataVO mu = formatMigrateUser();
			System.out.println("migrate user response: " + client.migrateUser(mu));
			*/		
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public static DSIUserDataVO formatTestUser() {
		DSIUserDataVO dU = new DSIUserDataVO(new UserDataVO());
		dU.setFirstName("SMT-TestUser-103");
		dU.setLastName("SMTUser103");
		dU.setEmailAddress("smttest103@siliconmtn.com");
		dU.setProfileId("SMT103");
		dU.setCountryCode("US");
		dU.setHospital("SMT Hospital 103");
		dU.setSpecialty("Orthopedic Surgery");
		dU.setProfession("Orthopedics");
		dU.setEligible(false);
		dU.setVerified(false);
		
		return dU;
	}
	
	public static DSIUserDataVO formatMigrateUser() {
		// test user from Jay
		DSIUserDataVO mU = new DSIUserDataVO(new UserDataVO());
		mU.setEmailAddress("test6-residentprogram@synthes.com");
		mU.setSpecialty("Orthopedic Surgery");
		mU.setSynthesId("1031");
		mU.setEligible(true);
		mU.setVerified(true);
		return mU;
	}

}

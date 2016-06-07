package com.depuysynthesinst;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.solr.common.SolrDocument;

import com.siliconmtn.security.UserDataVO;

/****************************************************************************
 * <b>Title</b>: DSIRoleMgr.java<p/>
 * <b>Description: Decouples the business rule decisions for "Role", which is really a reflection
 * of how the user answered the "Profession" question during registration. </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jun 29, 2015
 ****************************************************************************/
public class DSIRoleMgr {

	public DSIRoleMgr() {
		//this constructor is important for JSTL, which needs a concrete object 
		//reference in order to access static methods.
	}

	
	/**
	 * returns true if the user's profession is Resident (RESIDENT)
	 * @param user
	 * @return
	 */
	public boolean isResident(UserDataVO user) {
		return "RESIDENT".equals(DSIUserDataVO.getProfession(user));
	}
	
	/**
	 * returns true if the user's profession is Fellow (FELLOW)
	 * @param user
	 * @return
	 */
	public boolean isFellow(UserDataVO user) {
		return "FELLOW".equals(DSIUserDataVO.getProfession(user));
	}
	
	/**
	 * returns true if the user's profession is Chief Resident (CHIEF)
	 * @param user
	 * @return
	 */
	public boolean isChiefResident(UserDataVO user) {
		return "CHIEF".equals(DSIUserDataVO.getProfession(user));
	}
	
	/**
	 * returns true if the user's profession is Resident Director/ (DIRECTOR)
	 * @param user
	 * @return
	 */
	public boolean isDirector(UserDataVO user) {
		return "DIRECTOR".equals(DSIUserDataVO.getProfession(user));
	}

	/**
	 * returns true if the user's professing is Nurse (NURSE)
	 * @param user
	 * @return
	 */
	public boolean isNurse(UserDataVO user) {
		return "NURSE".equals(DSIUserDataVO.getProfession(user));
	}
	
	/**
	 * returns true if the user's profession is any of the above; meaning they interact with the
	 * Assignments functionality in any way.  Used for displaying menus, etc.
	 * Fellows DO NOT partake in My Assignments, at all.
	 * @param user
	 * @return
	 */
	public boolean isAssgUser(UserDataVO user) {
		return isResident(user) || isChiefResident(user) || (isDirector(user) && isParticipating(user));
	}	
	
	/**
	 * helper method to isAssgUser(); CHEIFs and DIRECTORs can opt NOT to 
	 * partake in My Assignments.
	 * @param user
	 * @return
	 */
	private boolean isParticipating(UserDataVO user) {
		return "yes".equals(user.getAttribute(DSIUserDataVO.RegField.DSI_MY_ASSIGNMENTS.toString()));
	}
	
	/**
	 * returns true if the user has administrative access for managing Assignments
	 * @param user
	 * @return
	 */
	public boolean isAssgAdmin(UserDataVO user) {
		return (isChiefResident(user) || isDirector(user)) && isParticipating(user);
	}
	
	
	/**
	 * returns true if the user is a CHIEF Resident and has chosen to participate in the program (administrative)
	 * @param user
	 * @return
	 */
	public boolean isChiefResidentAdmin(UserDataVO user) {
		return isChiefResident(user) && isParticipating(user);
	}
	
	
	/**
	 * returns true for our students who see 'My Assignments' in the left nav, and 
	 * have a Resident Director listed on their My Profile page
	 * @param user
	 * @return
	 */
	public boolean isAssgStudent(UserDataVO user) {
		return isResident(user) || isChiefResident(user);
	}
	
	
	/**
	 * returns true for anyone authorized to take LMS courses - based on Roles spreadsheet
	 * @param user
	 * @return
	 */
	public boolean isLMSAuthorized(UserDataVO user) {
		return isResident(user) || isFellow(user) || isChiefResident(user) || isDirector(user) || isNurse(user);
	}
	
	
	/**
	 * returns true for users who can access the redemption center
	 * @param user
	 * @return
	 */
	public boolean isRedemptionAuthorized(DSIUserDataVO user) {
		if (user == null) return false;

		//allow all J&J WWID users through
		if (UserDataVO.AuthenticationType.SAML == user.getAuthType()) return true;
		
		//let Resident Directors through
		if (isDirector(user)) return true;

		//block everyone that can't redeem
		if (!isCreditRedeeming(user)) return false;
		
		//graduated Residents & Fellows can't get redeem anymore
		Date d = user.getGraduationDate();
		if (d == null || d.before(Calendar.getInstance().getTime())) return false;
		
		//the only ones left are eligible residents, chief residents, and fellows who've not graduated yet
		return true;
	}
	
	
	/**
	 * returns true for users who can place orders ("cash in points") within the 
	 * redemption center.
	 * "Residents, Chief Residents, and Fellows at Eligible programs"
	 * @param user
	 * @return
	 */
	public boolean isCreditRedeeming(DSIUserDataVO user) {
		return (user != null && (isResident(user) || isFellow(user) || isChiefResident(user)) && user.isEligible() && user.isVerified());
	}

	/**
	 * Returns true for anyone authorized to collect points for the given  course.
	 * Specialty list is provided by Solr for 'this' course.
	 * @param user
	 * @param specialty
	 * @return
	 */
	public boolean isCreditEarning(UserDataVO user, List<String> courseSpecialties) {
		if (user == null || courseSpecialties == null) return false;
		
		//test Profession
		if (!isResident(user) && !isFellow(user) && !isChiefResident(user))
			return false;

		//if they're eligible they can only earn credits for courses in their specialty.
		DSIUserDataVO vo = new DSIUserDataVO(user);
		if (vo.isEligible()) {
			//compare course speciatly to user's specialty
			return courseSpecialties.contains(vo.getSpecialty());
		}
		return false;
	}
	
	
	/**
	 * overloaded above for Solr 
	 * @param user
	 * @param courseSpecialty
	 * @return
	 */
	public boolean  isCreditEarning(UserDataVO user, String courseSpecialty) {
		List<String> specs = new ArrayList<>();
		specs.add(courseSpecialty);
		return  isCreditEarning(user, specs);
	}
	
	
	/**
	 * returns true is the user is authorized to launch the course.  This has no bearing on points of redemption
	 * Is a pass-through method for backwards compatibility.
	 * @param user
	 * @return
	 */
	public boolean isCourseAuthorized(DSIUserDataVO user) {
		return isCourseAuthorized(user, null);
	}


	/**
	 * returns true is the user is authorized to launch the course.  This has no bearing on points of redemption
	 * @param user
	 * @return
	 */
	public boolean isCourseAuthorized(DSIUserDataVO user, SolrDocument course) {
		if (user == null || user.getProfession() == null) return false;

		//allow all J&J WWID users access
		if (UserDataVO.AuthenticationType.SAML == user.getAuthType()) return true;

		//must have a TTLMS ID
		if (user.getTtLmsId() == null || user.getTtLmsId().length() == 0) return false;

		//If the User is a Nurse.
		if(isNurse(user)) {
			/*
			 * Verify that the course isn't null.  Want a separate check here to
			 * prevent accidental skippage and passage through into a potential
			 * true situation later on.
			 */
			if(course != null) {

				/*
				 * Validate that cats isn't null and if Cats contains NURSE,
				 * return true to enable access.
				 */
				if(course.getFieldValue("category") instanceof List) {
					List<?> cats = ((List<?>)course.getFieldValue("category"));
					return cats.contains("NURSE");
				} else {
					return "NURSE".equals(course.getFieldValue("category"));
				}
			}

			//If any of the above cases aren't true for a Nurse, return false.
			return false;
		}

		//this list comes from the ACGME roles - users authorized to launch courses based on their profession
		List<String> approved = new ArrayList<>();
		approved.add("RESIDENT");
		approved.add("FELLOW");
		approved.add("CHIEF");
		approved.add("DIRECTOR");
		approved.add("ALLIED");
		
		return approved.contains(user.getProfession());
	}
}

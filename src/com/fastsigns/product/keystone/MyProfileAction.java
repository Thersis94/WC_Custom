package com.fastsigns.product.keystone;

import java.util.ArrayList;

import com.fastsigns.security.FastsignsSessVO;
import com.fastsigns.security.KeystoneProfileManager;
import com.fastsigns.security.KeystoneUserDataVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractBaseAction;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: MyProfileAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Nov 16, 2012
 ****************************************************************************/
public class MyProfileAction extends AbstractBaseAction {

	public MyProfileAction() {
	}

	public MyProfileAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("preparing to send Profile updates to Keystone");
		String msg = "Update Successful";
		FastsignsSessVO sessVo = (FastsignsSessVO) req.getSession().getAttribute(KeystoneProxy.FRAN_SESS_VO);
		String webId = (String)req.getSession().getAttribute(FastsignsSessVO.FRANCHISE_ID);

		KeystoneProfileManager pm = new KeystoneProfileManager(attributes);
		
		//replace the data in UserDataVO for local display
		KeystoneUserDataVO user = sessVo.getProfile(webId);
		String origUserName = user.getEmailAddress(); //preserve this for the JSON pass
		user.setPhoneNumbers(new ArrayList<PhoneVO>()); //flush phones
		user.setData(req);
		user.addAttribute("origUserName", origUserName);
		
		//load phone #s
		user = this.loadPhoneNumbers(user, req);
		
		//load alt emails
		user = this.loadAltEmails(user, req);
		
		//add division name
		user.addAttribute("divisionName", req.getParameter("divisionName"));
		
		//tell the proxy to submit our data and get a response back
		try {
			user = pm.submitProfileToKeystone(user, req);
			user.setWebId(webId);
			sessVo.addProfile(user);
			req.getSession().setAttribute(KeystoneProxy.FRAN_SESS_VO, sessVo);
		} catch (Exception e) {
			log.error("could not save MyProfile", e);
			msg = e.getMessage();
		}
		
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder url = new StringBuilder(page.getFullPath());
		url.append("?display=profile&msg=").append(msg);
		req.setAttribute(Constants.REDIRECT_REQUEST, true);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}
	
	
	/**
	 * simple util to ensure an ID gets passed to keystone instead of a blank.
	 * blanks will bomb the json object!
	 * if we don't have an id, pass a random String <32 chars long and Keystone 
	 * will see that and know to create a new record in the DB.
	 * @param id
	 * @return
	 */
	public String ensureId(String id) {
		if (id != null && id.length() > 0) return id;
		return RandomAlphaNumeric.generateRandom(10);
	}
	
	
	public KeystoneUserDataVO loadPhoneNumbers(KeystoneUserDataVO user, SMTServletRequest req) {
		//add phone #s and ids
		PhoneVO main = new PhoneVO(PhoneVO.HOME_PHONE, StringUtil.checkVal(req.getParameter("homePhoneNo")), user.getCountryCode());
		main.setPhoneNumberId(ensureId(req.getParameter("homePhoneId")));
		user.addAttribute("homePhoneId", main.getPhoneNumberId());
		user.addPhone(main);

		PhoneVO mobile = new PhoneVO(PhoneVO.MOBILE_PHONE, StringUtil.checkVal(req.getParameter("mobilePhoneNo")), user.getCountryCode());
		mobile.setPhoneNumberId(ensureId(req.getParameter("mobilePhoneId")));
		user.addAttribute("mobilePhoneId", mobile.getPhoneNumberId());
		user.addPhone(mobile);

		PhoneVO work = new PhoneVO(PhoneVO.WORK_PHONE, StringUtil.checkVal(req.getParameter("workPhoneNo")), user.getCountryCode());
		work.setPhoneNumberId(ensureId(req.getParameter("workPhoneId")));
		user.addAttribute("workPhoneId", work.getPhoneNumberId());
		user.addPhone(work);
		
		return user;
	}
	
	
	public KeystoneUserDataVO loadAltEmails(KeystoneUserDataVO user, SMTServletRequest req) {
		//add alt emails
		user.addAttribute("altEmail1", req.getParameter("altEmail1"));
		user.addAttribute("altEmail1Id", ensureId(req.getParameter("altEmail1Id")));
		
		user.addAttribute("altEmail2", req.getParameter("altEmail2"));
		user.addAttribute("altEmail2Id", ensureId(req.getParameter("altEmail2Id")));
		
		return user;
	}
	
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		FastsignsSessVO sessVo = (FastsignsSessVO) req.getSession().getAttribute(KeystoneProxy.FRAN_SESS_VO);
		String webId = (String)req.getSession().getAttribute(FastsignsSessVO.FRANCHISE_ID);
		
		//give the user the profile that matches the Store they're on (to edit)
		super.putModuleData(sessVo.getProfile(webId));
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#copy(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void copy(SMTServletRequest req) throws ActionException {}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void delete(SMTServletRequest req) throws ActionException {
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.SMTActionInterface#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
	}

}

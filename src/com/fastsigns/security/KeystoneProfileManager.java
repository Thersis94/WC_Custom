package com.fastsigns.security;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.fastsigns.action.franchise.vo.FranchiseVO;
import com.fastsigns.product.keystone.KeystoneProxy;
import com.fastsigns.product.keystone.parser.KeystoneDataParser;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: KeystoneProfileManager.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 22, 2013
 ****************************************************************************/
public class KeystoneProfileManager {

	private Map<String, Object> attributes = new HashMap<String, Object>();
	protected static Logger log = Logger.getLogger(KeystoneProfileManager.class);
	
	public KeystoneProfileManager() {
	}
	
	public KeystoneProfileManager(Map<String, Object> attribs) {
		this.attributes = attribs;
	}
	
	
	/**
	 * reusable method that turns the Keystone "entity" JSON into a UserDataVO
	 * @param entity
	 * @return
	 */
	public KeystoneUserDataVO loadProfileFromEntityJSON(JSONObject entity) 
			throws InvalidDataException {
		KeystoneUserDataVO user = new KeystoneUserDataVO();
		try {
			user.setAccountId(entity.optString("accounts_id"));
	
			//build the UserDataVO
			user.setProfileId(entity.optString("users_id"));
			user.setAuthenticationId(entity.optString("user_login_id"));
			user.setFirstName(entity.optString("first_name"));
			user.setLastName(entity.optString("last_name"));
			user.setEmailAddress(entity.optString("username"));
			user.addAttribute("companyName", entity.optString("company_name"));
			if (!"null".equalsIgnoreCase(entity.optString("division_name")))
				user.addAttribute("divisionName", entity.optString("division_name"));
			user.addAttribute("taxExempt", entity.optString("tax_exempt"));
			
			//needed at our custom FsKeystoneRoleModule
			//not fully implemented as of 02/21/2013; waiting for support on Keystone's side. - JM
			//user.addAttribute("roleId", entity.optString("role_id"));
			//user.addAttribute("rolePermissionId", entity.optString("role_permission_id"));
			//user.addAttribute("active", entity.optString("active"));
		
			//parse addresses (we only need one)
			JSONArray addrs = JSONArray.fromObject(entity.get("addresses"));
			int x = 0;
			for (x=0; x < addrs.size(); x++) {
				JSONObject addr = addrs.getJSONObject(x);
				//only save the primary address, or the last address (if o primary)
				if (addr.optInt("primary_address") != 1 && x < addrs.size()-1) continue;
				user.setAddress(StringUtil.checkVal(addr.optString("address_1")));
				if(!addr.optString("address_2").equals("null"))
					user.setAddress2(StringUtil.checkVal(addr.optString("address_2")));
				user.setCity(StringUtil.checkVal(addr.optString("city")));
				user.setState(StringUtil.checkVal(addr.optString("state_code")));
				user.setZipCode(StringUtil.checkVal(addr.optString("zipcode")));
				user.setCountryCode(StringUtil.checkVal(addr.optString("country_code")));
				user.addAttribute("addressId", StringUtil.checkVal(addr.optString("address_id")));
				break;
			}
			
			//parse phone#s
			JSONArray phones = JSONArray.fromObject(entity.get("phones"));
			for (x=0; x < phones.size(); x++) {
				JSONObject ph = phones.getJSONObject(x);
				log.debug("found phone: " + ph.toString());
				String type = ph.optString("phone_type").toUpperCase();
				if ("CELL".equals(type)) type = PhoneVO.MOBILE_PHONE;
				else if ("WORK".equals(type)) type = PhoneVO.WORK_PHONE;
				else if ("HOME".equals(type)) type = PhoneVO.HOME_PHONE;
				PhoneVO phone = new PhoneVO(type, ph.optString("telephone"), user.getCountryCode());
				user.addPhone(phone);
				user.addAttribute(type.toLowerCase() + "PhoneId", ph.optString("phone_id"));
			}
			
			//parse alt emails
			JSONArray emails = JSONArray.fromObject(entity.get("emails"));
			for (x=0; x < emails.size(); x++) {
				JSONObject eml = emails.getJSONObject(x);
				if(!eml.optString("value").equals(user.getEmailAddress())){
					user.addAttribute("altEmail" + (x+1), eml.optString("value"));
					user.addAttribute("altEmail" + (x+1) + "Id", eml.optString("entity_attribute_value_id"));
				}
			}
		} catch (Exception e) {
			log.error("could not load entity", e);
			throw new InvalidDataException(e);
		}
		
		return user;
	}
	
	
	/**
	 * reusable method that turns the Keystone "franchise" JSON into a FranchiseVO 
	 * @param franObj
	 * @return
	 * @throws InvalidDataException
	 */
	public FranchiseVO loadFranchiseFromJSON(JSONObject franObj) throws InvalidDataException {
		
		FranchiseVO franVo = new FranchiseVO();
		try {
		franVo.setWebId(franObj.optString("web_number"));
		franVo.setFranchiseId(franObj.optString("franchise_id"));
		franVo.setLocationName(franObj.optString("franchise_name"));
		franVo.addAttribute("fedex_account",franObj.opt("fedex_account"));
		franVo.addAttribute("ups_account",franObj.opt("ups_account"));
		franVo.addAttribute("default_tax_service",franObj.opt("default_tax_service"));
		franVo.addAttribute("avalara_tax_id",franObj.opt("avalara_tax_id"));
		franVo.addAttribute("avalara_license_id",franObj.opt("avalara_license_id"));
		Object ecomTaxServ = franObj.opt("ecomm_tax_service");
		if(ecomTaxServ == null)
			throw new InvalidDataException("Ecommerce not active for franchise");
		franVo.addAttribute("ecomm_tax_service",ecomTaxServ);
		//set the FranchiseAddress
		JSONArray addrArr = JSONArray.fromObject(franObj.getJSONArray("addresses"));
		for (int y=0; y < addrArr.size(); y++) {
			JSONObject franAddr = addrArr.getJSONObject(y);
			if (franAddr.optInt("primary_address") != 1 && y < franAddr.size()-1) continue; //only save the primary...or the last
			
			franVo.setAddress(franAddr.optString("address_1"));
			franVo.setAddress2(franAddr.optString("address_2"));
			franVo.setCity(franAddr.optString("city"));
			franVo.setState(franAddr.optString("state_code"));
			franVo.setZipCode(franAddr.optString("zipcode"));
			franVo.setCountryCode(franAddr.optString("country_code"));
			break;
		}
		
		JSONArray phoneArr = JSONArray.fromObject(franObj.getJSONArray("phones"));
		if(phoneArr != null && phoneArr.size() > 0) {
			for (int y=0; y < addrArr.size(); y++) {
				JSONObject franPhone = phoneArr.getJSONObject(y);
				if (franPhone.optInt("primary_phone") != 1 && y < franPhone.size()-1) continue; //only save the primary...or the last
					franVo.setPhone(franPhone.optString("telephone"));
				break;
			}
		}
		} catch(Exception e) {
			log.error("Problem loading the Franchise Data.", e);
		}
		return franVo;
	}


	
	public KeystoneUserDataVO submitProfileToKeystone(KeystoneUserDataVO user, SMTServletRequest req) 
			throws InvalidDataException {
		Map<String, Object> attribs = user.getAttributes();
		
		//send the data to Keystone to update the master record
		KeystoneProxy proxy = new KeystoneProxy(attributes);
		proxy.setSessionCookie(req.getCookie(Constants.JSESSIONID));
		proxy.setModule("userContact");
		proxy.setAction("updateUser");
		proxy.addPostData("users_id", user.getUserId());
		if (user.getAuthenticationId() != null) proxy.addPostData("userLoginId", user.getAuthenticationId());
		if (attribs.get("franchise_id") != null) proxy.addPostData("franchise_id", (String)attribs.get("franchise_id"));
		proxy.addPostData("first_name", user.getFirstName());
		proxy.addPostData("last_name", user.getLastName());
		if (attribs.get("divisionName") != null) proxy.addPostData("division_name", (String) attribs.get("divisionName"));
		if (req.hasParameter("emailAddress") || StringUtil.checkVal(user.getEmailAddress()).length() > 0) proxy.addPostData("username", user.getEmailAddress());
		if (attribs.get("origUserName") != null) proxy.addPostData("orig_username", (String)attribs.get("origUserName")); 
		if (req.hasParameter("password") || StringUtil.checkVal(user.getPassword()).length() > 0) proxy.addPostData("password", user.getPassword());
		proxy.addPostData("phone", formatJSONPhones(user));
		proxy.addPostData("multi_email", formatJSONEmails(user));
		proxy.addPostData("company", user.getFullName());
		proxy.setParserType(KeystoneDataParser.DataParserType.DoNothing);
		
		//tell the proxy to submit our data and get a response back
		String msg = null;
		//log.debug(user);
		try {
			byte[] byteData = (byte[]) proxy.getData().getActionData();
			JSONObject jsonObject = JSONObject.fromObject(new String(byteData));
			
			if (jsonObject.optBoolean("success")) {
				//reload the user's session data from when they logged in, with what was just acknowledged & saved by Keystone
				JSONObject entity = jsonObject.getJSONObject("data");
				user = this.loadProfileFromEntityJSON(entity);
				
			} else {
				msg = jsonObject.getString("responseText");
				throw new InvalidDataException(msg);
			}
			
			if (req.hasParameter("password")) {
				proxy = new KeystoneProxy(attributes);
				proxy.setModule("userContact");
				proxy.setAction("eCommSetPassword");
				proxy.addPostData("username", user.getEmailAddress());
				proxy.addPostData("password", req.getParameter("password"));
				proxy.setParserType(KeystoneDataParser.DataParserType.DoNothing);
				
				byteData = (byte[]) proxy.getData().getActionData();
				jsonObject = JSONObject.fromObject(new String(byteData));
				
				if (!jsonObject.optBoolean("success")) {
					msg = "Password could not be saved.";
					throw new SecurityException("Password could not be saved.");
				}
			}
			
		} catch (Exception e) {
			if (msg == null) msg = "Error Saving Changes";
			throw new InvalidDataException(msg);
		}
		
		return user;
	}


	
	/**
	 * structures a JSON object containing phone #s to submit to Keystone
	 * @param req
	 * @return
	 */
	private String formatJSONPhones(UserDataVO user) {
		JSONObject data = new JSONObject();
		
		//loop the phone #s
		for (PhoneVO vo : user.getPhoneNumbers()) {
			if (vo.getPhoneNumber() == null || "null".equalsIgnoreCase(vo.getPhoneNumber()))
					continue;
			
			log.debug(vo + " id=" + vo.getPhoneNumberId());
			String typeNm = StringUtil.checkVal(vo.getPhoneType(), PhoneVO.HOME_PHONE);
			int phoneType = 1; //HOME by default
			if (PhoneVO.WORK_PHONE.equals(typeNm)) phoneType = 2;
			else if (PhoneVO.MOBILE_PHONE.equals(typeNm)) phoneType = 3;
			
			JSONObject tele = new JSONObject();
			tele.accumulate("telephone", vo.getPhoneNumber());
			tele.accumulate("phone_type_id", phoneType);
			data.accumulate(vo.getPhoneNumberId(), tele);
		}
		
		log.debug("phones JSON: " + data.toString());
		return data.toString();
	}
	
	/**
	 * structures a JSON object containing altEmails to submit to Keystone
	 * @param req
	 * @return
	 */
	private String formatJSONEmails(UserDataVO user) {
		JSONObject data = new JSONObject();
		Map<String, Object> attribs = user.getAttributes();
		
		JSONObject email = new JSONObject();
		if (attribs.containsKey("altEmail1")) {
			email = new JSONObject();
			email.accumulate("value", (String)attribs.get("altEmail1"));
			email.accumulate("attribute_slug", "Email");
			data.accumulate((String)attribs.get("altEmail1Id"), email);
		}
		
		if (attribs.containsKey("altEmail2")) {
			email = new JSONObject();
			email.accumulate("value", (String)attribs.get("altEmail2"));
			email.accumulate("attribute_slug", "Email");
			data.accumulate((String)attribs.get("altEmail2Id"), email);
		}
		
		log.debug("emails JSON: " + data.toString());
		return data.toString();
	}
}

package com.wsla.action;

import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.wsla.common.WSLALocales;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <p><b>Title:</b> CustomerListAction.java</p>
 * <p><b>Description:</b> Called from DynamicListLoader to populate a dropdown for Tasks tool.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Feb 21, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class CustomerListAction extends SimpleActionAdapter {

	public CustomerListAction() {
		super();
	}

	public CustomerListAction(ActionInitVO arg0) {
		super(arg0);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (req.hasParameter("add")) {
			//create a new customer
			addCustomer(req);
		} else {
			//return admin list to manage
			putModuleData(listCustomers());
		}
	}

	/**
	 * @param req
	 */
	private void addCustomer(ActionRequest req) {
		UserVO vo = new UserVO(req);
		vo.setActiveFlag(1);
		vo.setLocale(WSLALocales.en_US.toString());
		UserDataVO usr = vo.getProfile();

		ProfileManager pm = ProfileManagerFactory.getInstance(getAttributes());
		DBProcessor db = new DBProcessor(getDBConnection());
		try {
			pm.updateProfile(usr, dbConn);
			vo.setProfile(usr);
			vo.setProfileId(usr.getProfileId());
			db.save(vo);
		} catch (Exception e) {
			log.error("could not create wsla user", e);
		}
	}


	/**
	 * Reusable retrieve to get the list outside of a Request
	 * @return
	 */
	private List<GenericVO> listCustomers() {
		String sql = StringUtil.join("select profile_id as key, last_nm + ', ' + first_nm + ' (' + email_address_txt + ')' as value ",
				"from ", getCustomSchema(), "wsla_user where length(email_address_txt) > 0 and active_flg=1 order by 2");

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		return db.executeSelect(sql, null, new GenericVO());
	}
}

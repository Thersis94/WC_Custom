package com.wsla.util.migration.vo;

import com.siliconmtn.annotations.Importable;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <p><b>Title:</b> WSLAStaffFileVO.java</p>
 * <p><b>Description:</b> models SW User listing.xlsx provided by Steve</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jan 28, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class WSLAStaffFileVO extends UserVO {

	private static final long serialVersionUID = -4891471567476559393L;

	public WSLAStaffFileVO() {
		super();
		super.setLocale("es_MX");
		super.setActiveFlag(1);
	}

	@Importable(name="Oper ID")
	@Override
	public void setUserId(String id) {
		super.setUserId("WSLA_" + id);
	}

	@Importable(name="Operator Name")
	public void setName(String name) {
		if (name == null || name.isEmpty()) return;
		//data cleanup!
		if ("Santiago Castillo G.".equals(name)) name = "Santiago Castillo";

		String[] arr = name.split("\\s");
		String fn = arr[0];
		String ln = arr[arr.length-1];
		if (arr.length == 3) fn = fn + " " + arr[1];

		super.setFirstName(StringUtil.capitalizePhrase(fn));
		super.setLastName(StringUtil.capitalizePhrase(ln));
		super.setEmail((StringUtil.truncate(fn, 1) + getLastName() + "@wsla.mx").toLowerCase());
	}
}
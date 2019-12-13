package com.mts.action;

// JDK 1.8.x
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.user.UserLoginReport;

/****************************************************************************
 * <b>Title</b>: MTSReportAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the retrieval of the report data
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 24, 2019
 * @updates:
 ****************************************************************************/

public class MTSReportAction extends SimpleActionAdapter {

	/**
	 * Key to be passed to utilize this action
	 */
	public static final String SELECT_KEY = "type";

	/**
	 * Assigns the keys for the select type to method mapping.  In the generic vo
	 * the key is the method name.  The value is a boolean which indicates whether
	 * or not the request object is needed in that method 
	 */
	private static Map<String, GenericVO> keyMap = new HashMap<>(16);
	static {
		keyMap.put("loginReport", new GenericVO("getUserLogins", Boolean.TRUE));
	}

	/**
	 * 
	 */
	public MTSReportAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public MTSReportAction(ActionInitVO arg0) {
		super(arg0);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (! req.hasParameter("json")) return;

		String listType = req.getStringParameter(SELECT_KEY);
		GenericVO vo = keyMap.get(listType);

		try {
			if (vo == null) throw new InvalidDataException("List Type Not Found in KeyMap");
			else if (Convert.formatBoolean(vo.getValue())) {
				Method method = this.getClass().getMethod(vo.getKey().toString(), req.getClass());
				putModuleData(method.invoke(this, req));
			} else {
				Method method = this.getClass().getMethod(vo.getKey().toString());
				putModuleData(method.invoke(this));
			}

		} catch (Exception e) {
			log.error("Unable to retrieve list: " + listType, e);
			putModuleData(null, 0, false, e.getLocalizedMessage(), true);
		}
	}

	/**
	 * Retrieves the data for the login report
	 * @param req
	 * @return
	 */
	public List<Object> getUserLogins(ActionRequest req) {
		UserLoginReport rpt = new UserLoginReport(getDBConnection(), getAttributes());
		List<GenericVO> gData = rpt.detailReport(req);
		List<Object> rptData = new ArrayList<>(gData.size());
		for (GenericVO gvo : gData)
			rptData.add(gvo.getValue());

		return rptData;
	}
}

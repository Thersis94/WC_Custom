package com.depuysynthes.srt;

import java.util.ArrayList;
import java.util.List;

import com.depuysynthes.srt.util.SRTUtil;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.EnumUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.list.ListDataVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> SRTEmployeeListAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Retrieves Employee Lists for front end List
 * population.  Retrieved over AJAX and requires employeeType to be passed
 * on the request of the kind to be retrieved. 
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 25, 2018
 ****************************************************************************/
public class SRTEmployeeListAction extends SimpleActionAdapter {

	public enum EmployeeType {ENGINEER("2"), DESIGNER("3"), QA("9"), BUYER("5");
		private String typeId;
		EmployeeType(String typeId) {
			this.typeId = typeId;
		}
		public String getTypeId() {
			return typeId;
		}
	}

	public SRTEmployeeListAction() {
		super();
	}

	public SRTEmployeeListAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		EmployeeType type = EnumUtil.safeValueOf(EmployeeType.class, req.getParameter("employeeType"));
		String opCoId = SRTUtil.getOpCO(req);
		if(type != null) {
			List<ListDataVO> employees = loadEmployees(type, opCoId);
			putModuleData(employees, employees.size(), false);
		}
	}

	/**
	 * Retrieve List of Employees stores in GenericVO's of rosterId -> Name
	 * for the given EmplyoeeType and OpCoId.
	 * @param type
	 * @param opCoId
	 * @return
	 */
	private List<ListDataVO> loadEmployees(EmployeeType type, String opCoId) {
		List<Object> vals = new ArrayList<>();
		vals.add(type.getTypeId());
		vals.add(1);	//We Only want active users retrieved here.
		vals.add(opCoId);

		List<ListDataVO> employees = new DBProcessor(dbConn).executeSelect(loadEmployeesSql(), vals, new ListDataVO());

		try {
			StringEncrypter se = new StringEncrypter((String) attributes.get(Constants.ENCRYPT_KEY));
	
			for(ListDataVO e : employees) {
				e.setLabelTxt(SRTUtil.decryptName(e.getLabelTxt(), se));
			}
		} catch(EncryptionException e) {
			log.error("Unable to decrypt Employee Name: ", e);
		}

		return employees;
	}

	/**
	 * Build Employee Retrieval Query.
	 * @return
	 */
	private String loadEmployeesSql() {
		String custom = getCustomSchema();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select r.roster_id as LIST_DATA_ID, r.roster_id as VALUE_TXT, ");
		sql.append("concat(p.first_nm, ' ', p.last_nm) ");
		sql.append("as LABEL_TXT ").append(DBUtil.FROM_CLAUSE).append(custom);
		sql.append("dpy_syn_srt_roster r ").append(DBUtil.INNER_JOIN).append(" profile p ");
		sql.append("on r.profile_id = p.profile_id ").append(DBUtil.WHERE_CLAUSE);
		sql.append("r.workgroup_id = ? and r.is_active = ? and r.op_co_id = ?");
		return sql.toString();
	}
}
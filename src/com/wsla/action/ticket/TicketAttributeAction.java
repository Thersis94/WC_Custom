package com.wsla.action.ticket;

import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.ticket.TicketAttributeACLVO;
import com.wsla.data.ticket.TicketAttributeVO;

/****************************************************************************
 * <b>Title</b>: TicketAttributeAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Class for managing the ticket attributes related to service orders
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Sep 25, 2018
 * @updates:
 ****************************************************************************/
public class TicketAttributeAction  extends SBActionAdapter {
	public static final String TICKET_ATTRRIBUTE_TYPE = "ticketAttribute";

	/**
	 * 
	 */
	public TicketAttributeAction() {
		super();
	}
	

	/**
	 * @param actionInit
	 */
	public TicketAttributeAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("Ticket Attribute action Retrieve called.");
		
		//isolate and control flow for attribute ACL changes
		if (req.hasParameter("ACLTable") || req.hasParameter("ACLChange")) {
			
			
			setModuleData(processACL(req));
			return;
		}
		
		String attributeCode = req.getParameter("attributeCode");
		String attributeGroupCode = req.getParameter("attributeGroupCode");
		boolean hasActiveFlag = req.hasParameter("activeFlag");
		int activeFlag = Convert.formatInteger(req.getParameter("activeFlag"));
		setModuleData(getAttributes(attributeCode, attributeGroupCode, activeFlag, hasActiveFlag, new BSTableControlVO(req, TicketAttributeVO.class)));
		
	}

	
	/**
	 * this method acts and the main control for changes to the acl
	 * @param req
	 * @return
	 */
	private Object processACL(ActionRequest req) {
		
		StringBuilder sql = new StringBuilder(365);
		sql.append("select a.role_id, a.role_nm, b.ticket_attribute_acl_cd, b.attribute_cd, b.read_flg, b.write_flg, b.create_dt from core.role a ");
		sql.append("left outer join ").append(getCustomSchema()).append("wsla_ticket_attribute_acl b ");
		sql.append("on a.role_id = b.role_id and b.attribute_cd = ? where (organization_id is null or organization_id = 'WSLA' ) ");
		sql.append("and a.role_id not in ('0','WSLA_PROSPECT','10') order by a.role_nm asc ");

		List<Object> params = new ArrayList<>();
		params.add(StringUtil.checkVal(req.getParameter("attributeCode")));
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), params, new TicketAttributeACLVO(), "role_id");
	}


	/**
	 * @param hasActiveFlag 
	 * @param providerId
	 * @param providerTypeId
	 * @param bsTableControlVO
	 * @return
	 */
	private  GridDataVO<TicketAttributeVO>  getAttributes(String attributeCode, String attributeGroupCode, int activeFlag, boolean hasActiveFlag, BSTableControlVO bst) {
		StringBuilder sql = new StringBuilder(72);
		sql.append("select * from ").append(getCustomSchema()).append("wsla_ticket_attribute a ");
		sql.append("inner join ").append(getCustomSchema()).append("wsla_attribute_group g on a.attribute_group_cd = g.attribute_group_cd ");
		sql.append("where 1=1 ");
		List<Object> params = new ArrayList<>();
		
		// Filter by attribute code
		if (! StringUtil.checkVal(attributeCode).isEmpty()) {
			sql.append("and attribute_cd = ? ");
			params.add(attributeCode);
		}
		
		// Filter by search criteria
		if (bst.hasSearch()) {
			sql.append("and attribute_nm like ? ");
			params.add(bst.getLikeSearch());
		}
		
		// Filter by Group code
		if (! StringUtil.isEmpty(attributeGroupCode)) {
			sql.append("and g.attribute_group_cd = ? ");
			params.add(attributeGroupCode);
		}
		
		// Filter by active flag
		if (hasActiveFlag &&  activeFlag >= 0 && activeFlag < 2) {
			sql.append("and active_flg = ? ");
			params.add(activeFlag);
		}
		
		sql.append(bst.getSQLOrderBy("attribute_nm",  "asc"));
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSQLWithCount(sql.toString(), params, new TicketAttributeVO(), bst.getLimit(), bst.getOffset());
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("ticket attribute build called");
		TicketAttributeVO tvo = new TicketAttributeVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.save(tvo);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Unable to save ticket attribute", e);
		}
	}

}

package com.wsla.action.ticket;

import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.provider.ProviderVO;
import com.wsla.data.ticket.TicketAttributeVO;

/****************************************************************************
 * <b>Title</b>: TicketAttributeAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> TODO Put Something Here
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
		log.debug("###########################Ticket Attribute action Retrieve called.");
		String attributeCode = req.getParameter("attributeCode");
		String attributeGroupCode = req.getParameter("attributeGroupCode");
		setModuleData(getAttributes(attributeCode, attributeGroupCode, new BSTableControlVO(req, TicketAttributeVO.class)));
		
	}

	
	/**
	 * @param providerId
	 * @param providerTypeId
	 * @param bsTableControlVO
	 * @return
	 */
	private  GridDataVO<TicketAttributeVO>  getAttributes(String attributeCode, String attributeGroupCode, BSTableControlVO bst) {
		StringBuilder sql = new StringBuilder(72);
		sql.append("select * from ").append(getCustomSchema()).append("wsla_ticket_attribute where 1=1 ");
		List<Object> params = new ArrayList<>();
		
		// Filter by provider id
		if (! StringUtil.checkVal(attributeCode).isEmpty()) {
			sql.append("and attribute_cd = ? ");
			params.add(attributeCode);
		}
		
		// Filter by search criteria
		if (bst.hasSearch()) {
			sql.append("and attribute_nm like ? ");
			params.add(bst.getLikeSearch());
		}
		
		// Filter by provider type
		if (! StringUtil.isEmpty(attributeGroupCode)) {
			sql.append("and attributeGroupCode = ? ");
			params.add(attributeGroupCode);
		}
		
		sql.append(bst.getSQLOrderBy("attribute_nm",  "asc"));
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSQLWithCount(sql.toString(), params, new TicketAttributeVO(), bst.getLimit(), bst.getOffset());
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		log.debug("&&&&&&&&&&&&&&&&&&&&&Ticket Attribute action list called.");
	}

}

package com.wsla.data.ticket;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataMapper;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.wsla.data.product.ProductSerialNumberVO;

/****************************************************************************
 * <p><b>Title:</b> HarvestApprovalVO.java</p>
 * <p><b>Description:</b> Stub bean represending the data used for displaying/approving 
 * Harvest requests (done by an OEM).</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2018, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Oct 26, 2018
 * <b>Changes:</b>
 ****************************************************************************/
public class HarvestApprovalVO extends BeanDataVO {

	private static final long serialVersionUID = -5010853405721828175L;

	private TicketVO ticket;
	private ProductSerialNumberVO product;
	private String harvestTicketId;

	public HarvestApprovalVO() {
		super();
		ticket = new TicketVO();
		product = new ProductSerialNumberVO();
	}

	public HarvestApprovalVO(ActionRequest req) {
		this();
		populateData(req);
		BeanDataMapper.parseBean(ticket, req.getParameterMap());
		//could call parseBean for product here, if needed
	}

	public ProductSerialNumberVO getProduct() {
		return product;
	}

	@BeanSubElement
	public void setProduct(ProductSerialNumberVO product) {
		this.product = product;
	}

	public TicketVO getTicket() {
		return ticket;
	}

	@BeanSubElement
	public void setTicket(TicketVO ticket) {
		this.ticket = ticket;
	}

	/**
	 * added this getters and setters for compatibility with db processors selects
	 * @return the harvestTicketId
	 */
	@Column(name="ticket_id", isPrimaryKey=true)
	public String getHarvestTicketId() {
		return harvestTicketId;
	}

	/**
	 * @param harvestTicketId the harvestTicketId to set
	 */
	public void setHarvestTicketId(String harvestTicketId) {
		this.harvestTicketId = harvestTicketId;
	}
}
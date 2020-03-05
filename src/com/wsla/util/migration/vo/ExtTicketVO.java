package com.wsla.util.migration.vo;

import java.util.Date;

import com.siliconmtn.db.orm.Column;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.ticket.TicketVO;

/****************************************************************************
 * <p><b>Title:</b> ExtTicketVO.java</p>
 * <p><b>Description:</b> Extends the stock TicketVO with some data holders - stuff related to 
 * the ticket but imported separately. e.g. comments, ledger entries, parts, assignments...</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since May 30, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class ExtTicketVO extends TicketVO {

	private static final long serialVersionUID = -6655500247852438146L;

	private String productId; //the productId (parent record of serial# already in the DB)
	private String customerProductId; //the name correlating to the productId - typically ID is blank when we're looking at these, meaning we're inserting records
	private String serialNoText;
	private String uniqueUserId;
	private String casLocationId;
	private Date closedDate;
	private String operator;
	private String batchName;

	@Override
	@Column(name="originator_user_id")
	public String getOriginatorUserId() {
		String id = super.getOriginator() != null ? super.getOriginator().getUserId() : null;
		return  !StringUtil.isEmpty(id) ? id :  super.getOriginatorUserId();
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getCustProductId() {
		return customerProductId;
	}

	public void setCustProductId(String customerProductId) {
		this.customerProductId = customerProductId;
	}

	public String getSerialNoText() {
		return serialNoText;
	}

	public void setSerialNoText(String serialNoText) {
		this.serialNoText = serialNoText;
	}

	public String getUniqueUserId() {
		return uniqueUserId;
	}

	public void setUniqueUserId(String uniqueUserId) {
		this.uniqueUserId = uniqueUserId;
	}

	public String getCasLocationId() {
		return casLocationId;
	}

	public void setCasLocationId(String casLocationId) {
		this.casLocationId = casLocationId == null || casLocationId.matches("0+") ? null : casLocationId;
	}

	public Date getClosedDate() {
		return closedDate;
	}

	public void setClosedDate(Date closedDate) {
		this.closedDate = closedDate;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	/**
	 * used for 800# lookup - operator when available, OEM otherwise
	 * @return
	 */
	public String getPhoneLookup() {
		return StringUtil.checkVal(operator, getOemId());
	}

	@Column(name="batch_txt", isInsertOnly=true)
	public String getBatchName() {
		return batchName;
	}

	public void setBatchName(String batchName) {
		this.batchName = batchName;
	}
}

package com.fastsigns.action.franchise.vo;

import com.siliconmtn.data.GenericVO;

/****************************************************************************
 * <b>Title</b>: OptionAttributeVO.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Dec 20, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class OptionAttributeVO extends GenericVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	private String id = null;
	private int orderNo = 0;
	private String parentId = null;
	private Integer moduleOptionId = Integer.valueOf(0);

	/**
	 * 
	 */
	public OptionAttributeVO() {
		
	}
	
	/**
	 * 
	 * @param key
	 * @param value
	 */
	public OptionAttributeVO(String key, String value) {
		this.setKey(key);
		this.setValue(value);
	}
	
	/**
	 * 
	 * @param id
	 * @param key
	 * @param value
	 */
	public OptionAttributeVO(String id, String key, String value) {
		this.setKey(key);
		this.setValue(value);
		this.id = id;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * @return the orderNo
	 */
	public int getOrderNo(){
		return orderNo;
	}

	/**
	 * @param orderNo the orderNo to set
	 */
	public void setOrderNo(int orderNo){
		this.orderNo = orderNo;
	}
	
	/**
	 * @return the parentId
	 */
	public String getParentId(){
		return parentId;
	}
	
	/**
	 * @param parentId the parentId to set
	 */
	public void setParentId(String parentId){
		this.parentId = parentId;
	}

	public void setModuleOptionId(Integer moduleOptionId) {
		this.moduleOptionId = moduleOptionId;
	}

	public Integer getModuleOptionId() {
		return moduleOptionId;
	}
}

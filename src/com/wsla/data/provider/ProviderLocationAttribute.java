package com.wsla.data.provider;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: ProviderLocationAttribute.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object to hold extended data for the provider
 * location
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 15, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_provider_location_attribute")
public class ProviderLocationAttribute extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3630670500479205159L;
	
	// Member Variables
	private String locationAttributeId;
	private String locationId;
	private String attributeName;
	private String value;
	private Date createDate;
	private Date updateDate;

	/**
	 * 
	 */
	public ProviderLocationAttribute() {
		super();
	}

	/**
	 * @param req
	 */
	public ProviderLocationAttribute(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public ProviderLocationAttribute(ResultSet rs) {
		super(rs);
	}

}


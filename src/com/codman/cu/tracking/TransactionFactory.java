package com.codman.cu.tracking;

import com.codman.cu.tracking.vo.UnitVO.ProdType;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: TransactionFactory.java<p/>
 * <b>Description: Used to get the proper transaction work flow for a 
 * product type. </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Erik Wingo
 * @version 1.0
 * @since Oct 28, 2014
 ****************************************************************************/
public class TransactionFactory {

	private TransactionFactory() {}

	/**
	 * Get the action corresponding to a product type
	 * @param type Product type
	 * @param ai
	 * @return Action used for submitting that product type
	 * @throws InvalidDataException When product type is unknown
	 */
	public static AbstractTransAction getInstance( String type, ActionInitVO ai )
	throws InvalidDataException{
		
		ProdType t = ProdType.valueOf( StringUtil.checkVal(type).toUpperCase() );
		return getInstance(t, ai); 
	}
	
	/**
	 * Gets the action corresponding to a product type
	 * @param type Product type
	 * @param ai
	 * @return Action used for submitting this type of product
	 * @throws InvalidDataException When product type is unknown
	 */
	public static AbstractTransAction getInstance( ProdType type, ActionInitVO ai )
	throws InvalidDataException{
		AbstractTransAction ta = null;
		switch ( type ){
		
		case MEDSTREAM:
			//Previous implementation (for CU/MEDSTREAM unit types)
			ta = new TransAction(ai);
			break;
		case ICP:
			//ICP unit refurbishment action
			ta = new TransIcpAction(ai);
			break;
			
		default:
			throw new InvalidDataException("Invalid Transaction Type: "+type);
		}
		return ta;
	}
}

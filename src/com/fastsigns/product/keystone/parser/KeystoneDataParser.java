package com.fastsigns.product.keystone.parser;

import org.apache.log4j.Logger;

import com.siliconmtn.exception.InvalidDataException;
import com.smt.sitebuilder.common.ModuleVO;

/****************************************************************************
 * <b>Title</b>: KeystoneDataParser.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 26, 2014
 ****************************************************************************/
public abstract class KeystoneDataParser {

	protected static Logger log;
	
	public KeystoneDataParser() {
		log = Logger.getLogger(getClass());
	}
	
	public enum DataParserType {
		DoNothing, Catalog, MyAssets, MyOrders, FromScratch, Account, Invoices,
		DSOLMaterials, DSOLTemplates, ProductDetail, PaymentHistory
	}
	
	/**
	 * this method provides a generic face to all the Ecomm actions and how they uniquely parse 
	 * JSON responses from Keystone.  For performance reasons, WC must 
	 * cache the PARSED JSON response, instead of the raw byte[] returned.
	 * Inflating the byte[] into JSON objects on every pageview is quite taxing on the server.
	 * @param byteData
	 * @return
	 * @throws InvalidDataException
	 */
	public abstract ModuleVO formatData(byte[] byteData) throws InvalidDataException ;
	
	
	
	/**
	 * classloader method the proxies can call without needing to know classes or
	 * pass them around.  KeystoneProxy should be the only class calling this method.
	 * @param dpt
	 * @return
	 */
	public static KeystoneDataParser newInstance(DataParserType dpt) {
		if (dpt == null) dpt = DataParserType.DoNothing;
		
		switch(dpt) {
			case Catalog: return new CatalogParser();
			case MyOrders: return new MyOrdersParser();
			case FromScratch: return new FromScratchParser();
			case Account: return new AccountParser();
			case Invoices: return new InvoicesParser();
			case MyAssets: return new MyAssetsParser();
			case DSOLMaterials: return new DSOLMaterialsParser();
			case DSOLTemplates: return new DSOLTemplatesParser();
			case ProductDetail: return new ProductDetailParser();
			case PaymentHistory: return new PaymentHistoryParser();
			
			case DoNothing:
			default: return new DoNothingParser();
		}
	}

}

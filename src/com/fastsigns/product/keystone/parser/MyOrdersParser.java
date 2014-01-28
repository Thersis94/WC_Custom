package com.fastsigns.product.keystone.parser;

// JDK 1.7.
import java.lang.reflect.Type;
import java.util.List;

// WC Libs
import com.fastsigns.product.keystone.vo.OrderVO;
import com.smt.sitebuilder.common.ModuleVO;

// Gson 2.2.4
import com.google.gson.reflect.TypeToken;

// SMT Base libs
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.SMTSerializer;

/****************************************************************************
 * <b>Title</b>: MyOrdersParser.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 26, 2014
 ****************************************************************************/
public class MyOrdersParser extends KeystoneDataParser {
	
	
	/* (non-Javadoc)
	 * @see com.fastsigns.product.keystone.parser.KeystoneDataParser#parseData(byte[])
	 */
	@Override
	public ModuleVO formatData(byte[] byteData) throws InvalidDataException {
		ModuleVO mod = new ModuleVO();
		log.info("Data: " + new String(byteData) );
		try {
			Type type = new TypeToken<List<OrderVO>>(){}.getType();
			List<?> orders = (List<?>)SMTSerializer.fromJson(new String(byteData), type);
			mod.setActionData(orders);

		} catch (Exception e) {
			log.error("could not parse JSON", e);
			throw new InvalidDataException(e);
		}
		
		return mod;
	}

}

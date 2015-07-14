package com.depuysynthes.nexus;

import com.siliconmtn.barcode.BarcodeFormatterIntfc;

/****************************************************************************
 * <b>Title</b>: NexusBarcodeFormatter.java<p/>
 * <b>Description: Formats the barcode data to surround the identifier 
 * digits with parentheses</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 6, 2014
 ****************************************************************************/

public class NexusBarcodeFormatter implements BarcodeFormatterIntfc {
	private final int GTIN_LENGTH = 16;
	private final String GTIN_PREPEND = "01";
	private final String LOT_PREPEND = "10";

	@Override
	public String prepareCustomData(String data) {
		if (data == null) return "";
		if (data.indexOf(GTIN_PREPEND) == 0) data = data.replaceFirst(GTIN_PREPEND, "("+GTIN_PREPEND+")");
		if (data.indexOf(LOT_PREPEND) == 0) data = data.replaceFirst(LOT_PREPEND, "("+LOT_PREPEND+")");
		
		// Check if the data is long enough to be a gtin lot no combination
		// If so we need to check the latter part of the data for a lot prepend
		if (data.length() > 20) {
			String lotNo = data.substring(GTIN_LENGTH, data.length());
			data = data.replace(lotNo, lotNo.replaceFirst(LOT_PREPEND, "("+LOT_PREPEND+")"));
		}
		
		return data;
	}

}

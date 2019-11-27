package com.wsla.util.migration.archive;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.wsla.util.migration.AssetParser;
import com.wsla.util.migration.vo.AssetPathVO;

/****************************************************************************
 * <p><b>Title:</b> AssetParserArchive.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Oct 11, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class AssetParserArchive extends AssetParser {
	

	Map<String, String> tickets = new HashMap<>(100000);

	public AssetParserArchive(Map<String, String> tickets ) {
		super();
		this.tickets = tickets;
	}

	@Override
	protected void sortData() {
		//discard comments for tickets not being imported
		Iterator<AssetPathVO> iter = assets.listIterator();
		while (iter.hasNext()) {
			AssetPathVO vo = iter.next();
			if (!tickets.containsKey(vo.getTicketId()))
				iter.remove();
		}
		log.info("loaded " + assets.size() + " asset comments to save");
	}



	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbsImporter#save()
	 */
	@Override
	public void save() throws Exception {
		for (AssetPathVO vo : assets)
			vo.setFileName(fileName);

		writeToDB(assets);
	}
}

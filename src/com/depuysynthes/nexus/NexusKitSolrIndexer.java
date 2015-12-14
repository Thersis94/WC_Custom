package com.depuysynthes.nexus;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.impl.HttpSolrServer;

import com.siliconmtn.util.CommandLineUtil;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.solr.SolrActionUtil;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: NexusKitSolrIndexer.java<p/>
 * <b>Description: Load all sets in the database into solr</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since October 13, 2015
 * @updates 
 * 
 ****************************************************************************/

public class NexusKitSolrIndexer extends CommandLineUtil {
	    
    
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {       
		NexusKitSolrIndexer ksi = new NexusKitSolrIndexer(args);
		ksi.run();
	}
	
	
	public NexusKitSolrIndexer(String[] args) {
		super(args);
		loadProperties("scripts/Nexus.properties");
		loadDBConnection(props);
	}

	
	/**
	 * Load all kits and all users that have access to them
	 * @return
	 * @throws SQLException
	 */
	private List<SolrDocumentVO> loadKits() throws SQLException {
		StringBuilder sql = new StringBuilder(250);
		String customDb = (String) props.get("customDbSchema");
		sql.append("SELECT *, s.PROFILE_ID as SHARED_ID FROM ").append(customDb).append("DPY_SYN_NEXUS_SET_INFO i ");
		sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_NEXUS_SET_SHARE s ");
		sql.append("ON s.SET_INFO_ID = i.SET_INFO_ID ");
		sql.append("ORDER BY s.SET_INFO_ID");
		
		List<SolrDocumentVO> kits = new ArrayList<>();
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			
			NexusKitVO kit = null;
			String kitId = "";
			String solrIndex = props.getProperty("solrIndex");
			while (rs.next()) {
				if (!kitId.equals(rs.getString("SET_INFO_ID"))) {
					if (kit != null) kits.add(kit);
					kit = new NexusKitVO(rs, solrIndex);
				}
				kit.addPermision(rs.getString("SHARED_ID"), "");
			}
			if (kit != null) kits.add(kit);
		}
		
		return kits;
	}


	@Override
	public void run() {
		try {
			List<SolrDocumentVO> kits = loadKits();
			HttpSolrServer server = new HttpSolrServer(props.getProperty(Constants.SOLR_BASE_URL)+props.getProperty(Constants.SOLR_COLLECTION_NAME));
			SolrActionUtil solr = new SolrActionUtil(server);
			System.out.println("Adding " + kits.size() + " Documents");
			solr.addDocuments(kits);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}

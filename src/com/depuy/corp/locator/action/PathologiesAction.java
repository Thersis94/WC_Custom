package com.depuy.corp.locator.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

public class PathologiesAction extends SimpleActionAdapter {

	public PathologiesAction() {
	}

	public PathologiesAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	public void delete(SMTServletRequest req){
		String dlrLocnId = req.getParameter("dealerLocationId");
	
		StringBuilder sql = new StringBuilder();
		sql.append("delete from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_CORP_PATHOLOGY_XR where dealer_location_id=?");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, dlrLocnId);
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("could not delete pathologies", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}
	
	public void update(SMTServletRequest req){
		String dlrLocnId = req.getParameter("dealerLocationId");
		String [] pathologyIds = req.getParameter("pathologies").split(",");
		if (pathologyIds == null || pathologyIds.length == 0) return;
		
		StringBuilder sql = new StringBuilder();
		sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DEPUY_CORP_PATHOLOGY_XR (pathology_xr_id, dealer_location_id, ");
		sql.append("pathology_id, create_dt) values (?,?,?,?)");
	
		UUIDGenerator uuid = new UUIDGenerator();
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			for (String p : pathologyIds) {
				if (p == null || p.length() == 0) continue;
				ps.setString(1, uuid.getUUID());
				ps.setString(2, dlrLocnId);
				ps.setString(3, p);
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.debug(cnt.length + " pathologies saved for " + dlrLocnId);
			
		} catch (SQLException sqle) {
			log.error("could not save pathologies", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}
	public void retrieve(SMTServletRequest req) {
		String dlrLocnId = req.getParameter("dealerLocationId");
		List<Node> nodes = new ArrayList<Node>();
		
		StringBuilder sb = new StringBuilder();
		sb.append("select a.pathology_id, a.parent_id, a.pathology_nm, a.level_no, b.pathology_xr_id ");
		sb.append("from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("DEPUY_CORP_PATHOLOGY a ");
	    sb.append("left outer join ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("DEPUY_CORP_PATHOLOGY_xr b "); 
	    sb.append("on a.pathology_id=b.pathology_id and b.dealer_location_id=? order by level_no, parent_id");
	    
	    PreparedStatement ps = null;
	    try {
	    	ps = dbConn.prepareStatement(sb.toString());
	    	ps.setString(1, dlrLocnId);
	    	ResultSet rs = ps.executeQuery();
	    	while(rs.next()) {
	    		Node n = new Node(rs.getString("pathology_id"), rs.getString("parent_id"));
		    	n.setUserObject(new PathologiesVO(rs));
		    	nodes.add(n);
	    	}
	    	log.debug("Retrieved " + nodes.size() + " pathologies");
	    	
	    } catch(SQLException sqle) {
	    	log.debug(sqle);
	    } finally {
	    	try { ps.close(); } catch(Exception e) {}
	    }

	    req.setAttribute("treeList", new Tree(nodes));
	}
	
//	public boolean treeLookAhead(Node n, int count){
//		if(n.children().size() == 0){
//			return ((PathologiesVO) n.getUserObject()).getSelected();
//		}
//		if(n.getUserObject() != null && (n.getParentId() == null || n.getParentId().equals(""))){
//			if(((PathologiesVO) n.getUserObject()).getSelected()){
//				((PathologiesVO) n.getUserObject()).setShowChildren(true);
//				log.debug(((PathologiesVO) n.getUserObject()).getParentId()+"/"+((PathologiesVO) n.getUserObject()).getPathologyNm() + " " + count );
//			}
//		}
//		for(Node t : n.children()){
//			if(treeLookAhead(t, count++)){
//				if(n.getUserObject() != null){
//					log.debug(((PathologiesVO) t.getUserObject()).getParentId()+"/"+((PathologiesVO) t.getUserObject()).getPathologyNm() + " " + count );
//					((PathologiesVO) n.getUserObject()).setShowChildren(true);
//				}
//			}
//		}
//		if(n.getUserObject()!= null)
//			return ((PathologiesVO) n.getUserObject()).getSelected();
//		
//		return false;
//	}
	
}
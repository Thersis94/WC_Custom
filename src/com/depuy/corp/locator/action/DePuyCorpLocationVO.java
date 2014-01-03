/**
 * 
 */
package com.depuy.corp.locator.action;

import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.DBUtil;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;

/****************************************************************************
 * <b>Title</b>: DePuyCorpLocationVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 18, 2011
 ****************************************************************************/
public class DePuyCorpLocationVO {
	private String physicianName = null;
	private String caseDays = null;
	private String photoUrl = null;
	private String videoUrl = null;
	private String courseworkUrl = null;
	private String hospitalAffiliation = null;
	private String hotels = null;
	private String airports = null;
	private String alternateUrl = null;
	private String speciality = null;
	private Map<String, String> products = new HashMap<String, String>();
	private Map<String, Node> pathologies = new HashMap<String, Node>();
	private DealerLocationVO dealerLocation = new DealerLocationVO();
	
	public DePuyCorpLocationVO() {
	}
	
	public DePuyCorpLocationVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		physicianName = db.getStringVal("physician_nm", rs);
		caseDays = db.getStringVal("case_days_txt", rs);
		photoUrl = db.getStringVal("photo_url", rs);
		videoUrl = db.getStringVal("video_url", rs);
		courseworkUrl = db.getStringVal("coursework_url", rs);
		hospitalAffiliation = db.getStringVal("hosp_affil_txt", rs);
		hotels = db.getStringVal("hotels_txt", rs);
		airports = db.getStringVal("airports_txt", rs);
		alternateUrl = db.getStringVal("alt_website_url", rs);
		speciality = db.getStringVal("speciality_txt", rs);
		dealerLocation.setDealerLocationId(db.getStringVal("dealer_location_id", rs));
	}
	
	
	public String getPhysicianName() {
		return physicianName;
	}
	public void setPhysicianName(String physicianName) {
		this.physicianName = physicianName;
	}
	public String getCaseDays() {
		return caseDays;
	}
	public void setCaseDays(String caseDays) {
		this.caseDays = caseDays;
	}
	public String getPhotoUrl() {
		return photoUrl;
	}
	public void setPhotoUrl(String photoUrl) {
		this.photoUrl = photoUrl;
	}
	public String getVideoUrl() {
		return videoUrl;
	}
	public void setVideoUrl(String videoUrl) {
		this.videoUrl = videoUrl;
	}
	public String getCourseworkUrl() {
		return courseworkUrl;
	}
	public void setCourseworkUrl(String courseworkUrl) {
		this.courseworkUrl = courseworkUrl;
	}
	public String getHospitalAffiliation() {
		return hospitalAffiliation;
	}
	public void setHospitalAffiliation(String hospitalAffiliation) {
		this.hospitalAffiliation = hospitalAffiliation;
	}
	public String getHotels() {
		return hotels;
	}
	public void setHotels(String hotels) {
		this.hotels = hotels;
	}
	public String getAirports() {
		return airports;
	}
	public void setAirports(String airports) {
		this.airports = airports;
	}
	public DealerLocationVO getDealerLocation() {
		return dealerLocation;
	}
	public void setDealerLocation(DealerLocationVO dealerLocation) {
		this.dealerLocation = dealerLocation;
	}
	
	//four helper methods for dealing with products
	public Map<String, String> getProducts() {
		return products;
	}
	public Map<String,String> getSortedProducts() {
		TreeMap<String, String> result = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		for (String p : products.keySet()) result.put(products.get(p), p);
		return result;
	}
	public void setProducts(Map<String, String> products) {
		this.products = products;
	}
	public void addProduct(String prodId, String prodNm) {
		products.put(prodId, prodNm);
	}
	
	
	//four helper methods for dealing with Pathologies
	public Map<String, Node> getPathologies() {
		return pathologies;
	}
	public List<Node> getSortedPathologies() {
		Tree tree = new Tree(new ArrayList<Node>(pathologies.values()));
		List<Node> data = tree.preorderList();
		java.util.Collections.sort(data, new NodeComparator());
		return data;
	}
	public void setPathologies(Map<String, Node> pathologies) {
		this.pathologies = pathologies;
	}
	public void addPathology(String pathId, String parentId, String pathNm) {
		Node n = new Node(pathId, parentId);
		n.setNodeName(pathNm);
		this.pathologies.put(pathId, n);
	}
	public String getAlternateUrl() {
		return alternateUrl;
	}
	public void setAlternateUrl(String alternateUrl) {
		this.alternateUrl = alternateUrl;
	}
	public String getSpeciality() {
		return speciality;
	}
	public void setSpeciality(String speciality) {
		this.speciality = speciality;
	}


	/**
	 * **************************************************************************
	 * <b>Title</b>: DePuyCorpLocationVO.java<p/>
	 * <b>Description: Compares Node names for alphabetical listing</b> 
	 * <p/>
	 * <b>Copyright:</b> Copyright (c) 2012<p/>
	 * <b>Company:</b> Silicon Mountain Technologies<p/>
	 * @author James McKain
	 * @version 1.0
	 * @since Jan 2, 2012
	 ***************************************************************************
	 */
	class NodeComparator implements Comparator<Node> {
		public int compare(Node n1, Node n2) {
			return n1.getNodeName().compareToIgnoreCase(n2.getNodeName());
		}
	}
}

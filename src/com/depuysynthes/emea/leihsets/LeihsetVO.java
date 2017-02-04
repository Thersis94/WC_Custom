package com.depuysynthes.emea.leihsets;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.approval.Approvable;
import com.smt.sitebuilder.approval.ApprovalVO;

/****************************************************************************
 * <b>Title</b>: LeihsetVO.java<p/>
 * <b>Description: Vo for Leihsets & LeihsetAssets.  The structures are so similar and simple it justified
 * reusing one bean.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 1, 2015
 ****************************************************************************/
public class LeihsetVO implements Approvable, Serializable, Comparable<LeihsetVO> {

	private static final long serialVersionUID = 8572661193156013730L;
	
	private String organizationId;
	private String leihsetGroupId;
	private String leihsetId;
	private String leihsetAssetId;
	private String leihsetName;
	private String assetName;
	private Set<String> categories;
	private String assetNumber; //used loosly! 
	private String imageUrl;
	private String excelUrl;
	private String pdfUrl;
	private String dpySynMediaBinId;
	private String dpySynAssetName;
	private String dpySynTrackingNo;
	private int orderNo = 0;
	private int archiveFlg = 0;
	private Map<String, LeihsetVO> assets; //a PDF or Excel uploaded to this Liehset
	private Map<String, LeihsetVO> materials; //Mediabin Literature
	private ApprovalVO approval;
	
	private String categoryName;
	private String parentCategoryName;
	private Tree categoryTree;

	public LeihsetVO() {
		assets = new LinkedHashMap<>();
		materials = new LinkedHashMap<>();
		categories = new HashSet<>();
	}
	
	public LeihsetVO(ResultSet rs, boolean isSet) {
		this();
		DBUtil db = new DBUtil();
		
		if (isSet) {
			//this is a setList entry being added to an existing Leihset
			setLeihsetAssetId(db.getStringVal("leihset_asset_id", rs));
			setLeihsetId(db.getStringVal("leihset_id", rs));
			setAssetName(db.getStringVal("asset_nm", rs));
			setAssetNumber(db.getStringVal("asset_no", rs));
			setExcelUrl(db.getStringVal("excel_url", rs));
			setPdfUrl(db.getStringVal("pdf_url", rs));
			setDpySynMediaBinId(db.getStringVal("dpy_syn_mediabin_id", rs));
			setDpySynAssetName(db.getStringVal("TITLE_TXT", rs)); //from dpy_syn_mediabin
			setDpySynTrackingNo(db.getStringVal("TRACKING_NO_TXT", rs)); //from dpy_syn_mediabin
			setOrderNo(db.getIntVal("order_no", rs));
		} else {
			//this is a Leihset itself
			setLeihsetGroupId(db.getStringVal("leihset_group_id", rs));
			setLeihsetId(db.getStringVal("leihset_id", rs));
			setOrganizationId(db.getStringVal("organization_id", rs));
			setLeihsetName(db.getStringVal("leihset_nm", rs));
			setImageUrl(db.getStringVal("image_url", rs));
			setOrderNo(db.getIntVal("order_no", rs));
			setArchiveFlg(db.getIntVal("archive_flg", rs));
			setSyncData(new ApprovalVO(rs));
		}
		db = null;
	}
	
	public LeihsetVO(ActionRequest req, boolean isSet) {
		this();
		setLeihsetGroupId(StringUtil.checkVal(req.getParameter("leihsetGroupId"), null));
		setLeihsetId(req.getParameter("sbActionId"));
		setOrderNo(Convert.formatInteger(req.getParameter("orderNo"), 0).intValue());
		
		if (isSet) {
			//this is a setList entry being added to an existing Leihset
			setLeihsetAssetId(req.getParameter("leihsetAssetId"));
			setAssetName(req.getParameter("assetName"));
			setAssetNumber(StringUtil.checkVal(req.getParameter("assetNumber"), null));
			setDpySynMediaBinId(StringUtil.checkVal(req.getParameter("dpySynMediaBinId"), null));
			
			// Check if we're getting a new file that will replace the old one.
			if (req.getFile("excelFile") == null)
				this.setExcelUrl(StringUtil.checkVal(req.getParameter("excelFileOrig"), null));

			// Check if we're getting a new file that will replace the old one.
			if (req.getFile("pdfFile") == null)
				this.setPdfUrl(StringUtil.checkVal(req.getParameter("pdfFileOrig"), null));
			
		} else {
			//this is a Leihset itself
			setOrganizationId(req.getParameter("organizationId"));
			setLeihsetName(req.getParameter("actionName"));
			if (req.hasParameter("categories"))
					this.setCategories(Arrays.asList(req.getParameterValues("categories")));
			setArchiveFlg(Convert.formatInteger(req.getParameter("archiveFlg"), 0).intValue());
			
			// Check if we're getting a new file that will replace the old one.
			if (req.getFile("imageFile") == null)
				this.setImageUrl(req.getParameter("imageFileOrig"));
			
			setSyncData(new ApprovalVO(req));
			
		}
	}

	public String getLeihsetName() {
		return leihsetName;
	}

	public String getAssetName() {
		return assetName;
	}

	public String getAssetNumber() {
		return assetNumber;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public String getExcelUrl() {
		return excelUrl;
	}

	public String getPdfUrl() {
		return pdfUrl;
	}

	public int getOrderNo() {
		return orderNo;
	}

	public void setLeihsetName(String leihsetName) {
		this.leihsetName = leihsetName;
	}

	public void setAssetName(String assetName) {
		this.assetName = assetName;
	}

	public void setAssetNumber(String assetNumber) {
		this.assetNumber = assetNumber;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public void setExcelUrl(String excelUrl) {
		this.excelUrl = excelUrl;
	}

	public void setPdfUrl(String pdfUrl) {
		this.pdfUrl = pdfUrl;
	}

	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}

	public List<LeihsetVO> getAssets() {
		return new ArrayList<LeihsetVO>(assets.values());
	}

	public List<LeihsetVO> getMaterials() {
		return new ArrayList<LeihsetVO>(materials.values());
	}
	
	private void addAsset(LeihsetVO vo) {
		assets.put(vo.getLeihsetAssetId(), vo);
	}

	private void addMaterial(LeihsetVO vo) {
		materials.put(vo.getLeihsetAssetId(), vo);
	}
	
	public void addResource(LeihsetVO vo) {
		if (vo.getDpySynMediaBinId() != null) {
			this.addMaterial(vo);
		} else {
			this.addAsset(vo);
		}
	}

	public String getLeihsetAssetId() {
		return leihsetAssetId;
	}

	public void setLeihsetAssetId(String leihsetAssetId) {
		this.leihsetAssetId = leihsetAssetId;
	}

	public String getLeihsetId() {
		return leihsetId;
	}

	public void setLeihsetId(String leihsetId) {
		this.leihsetId = leihsetId;
	}

	public String getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

	public String getDpySynAssetName() {
		return dpySynAssetName;
	}

	public void setDpySynAssetName(String dpySynAssetName) {
		this.dpySynAssetName = dpySynAssetName;
	}

	public String getDpySynMediaBinId() {
		return dpySynMediaBinId;
	}

	public void setDpySynMediaBinId(String dpySynMediaBinId) {
		this.dpySynMediaBinId = dpySynMediaBinId;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.approval.Approvable#getSyncData()
	 */
	@Override
	public ApprovalVO getSyncData() {
		return approval;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.approval.Approvable#setSyncData(com.smt.sitebuilder.approval.ApprovalVO)
	 */
	@Override
	public void setSyncData(ApprovalVO vo) {
		this.approval = vo;
		
	}

	public String getLeihsetGroupId() {
		return leihsetGroupId;
	}

	public void setLeihsetGroupId(String leihsetGroupId) {
		this.leihsetGroupId = leihsetGroupId;
	}

	public int getArchiveFlg() {
		return archiveFlg;
	}

	public void setArchiveFlg(int archiveFlg) {
		this.archiveFlg = archiveFlg;
	}
	
	public String getLeihsetMasterId() {
		return (leihsetGroupId == null) ? leihsetId : leihsetGroupId;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(LeihsetVO vo) {
		if (vo == null || !(vo instanceof LeihsetVO)) return -1;
		
		//sort categories alphabetically
		String cat1 = StringUtil.checkVal(vo.getCategoryName());
		if (!cat1.equals(this.getCategoryName())) {
			return StringUtil.checkVal(getCategoryName()).compareTo(cat1);
		}
		
		//compare using Liehset rank for two VOs in the same category
		return Integer.compare(this.getOrderNo(), vo.getOrderNo());
	}

	public String getDpySynTrackingNo() {
		return dpySynTrackingNo;
	}

	public void setDpySynTrackingNo(String dpySynTrackingNo) {
		this.dpySynTrackingNo = dpySynTrackingNo;
	}

	public Set<String> getCategories() {
		return categories;
	}

	public void setCategories(Collection<String> categories) {
		this.categories.addAll(categories);
	}
	
	public void addCategory(String cat) {
		this.categories.add(cat);
	}

	public Tree getCategoryTree() {
		return categoryTree;
	}

	public void setCategoryTree(Tree categoryTree) {
		this.categoryTree = categoryTree;
	}

	public String getBusinessUnits() {
		Tree t = getCategoryTree();
		if (t == null || t.getRootNode() == null) return null;
		
		Set<String> bizUnits = new HashSet<>();
		StringBuilder sb = new StringBuilder(100);
		for (Node n : t.getRootNode().getChildren()) {
			//dig down and find out of this root node is tagged for this Leihset
			for (Node n2 : n.getChildren()) {
				for (Node n3 : n2.getChildren()) {
					if (!((Boolean)(n3.getUserObject()))) continue;
					bizUnits.add(n.getNodeName());
				}
			}
		}
		for (String s : bizUnits) {
			if (sb.length() > 0) sb.append(", ");
			sb.append(s);
		}
		return sb.toString();
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public String getParentCategoryName() {
		return parentCategoryName;
	}

	public void setParentCategoryName(String parentCategoryName) {
		this.parentCategoryName = parentCategoryName;
	}
}

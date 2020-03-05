package com.biomed.smarttrak.admin.vo;

//JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Log4j 1.2.17
import org.apache.log4j.Logger;

// Google GSON 2.3
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.Expose;
// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/********************************************************************
 * <b>Title: </b>GridVO.java<br/>
 * <b>Description: </b>Stores the base information for the grid<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author james
 * @version 3.x
 * @since Feb 24, 2017
 * Last Updated:
 * 	
 *******************************************************************/
@Table(name="biomedgps_grid")
public class GridVO extends BeanDataVO {
	/**
	 * 
	 */
	public enum RowStyle {
		DATA("bs-data"),
		UNCHARTED_DATA("bs-nochart-data"),
		HEADING("bs-heading"),
		SUB_TOTAL("bs-sub-total"),
		TOTAL("bs-total");
		
		private final String name;
		private RowStyle(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		/**
		 * Allows the enum to be returned by its value
		 * @param key
		 * @return
		 */
		public static String getEnumKey(String key) {
			if (StringUtil.isEmpty(key)) return null;
			
			for (RowStyle rs : RowStyle.values()) {
				if (key.equalsIgnoreCase(rs.getName())) return rs.name();
			}
			
			return null;
		}
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected static final Logger log = Logger.getLogger(GridVO.class);
	
	/**
	 * Label utilized to match the columns and row data
	 */
	public static final String FIELD_LABEL = "field_";
	
	/**
	 * Defines the key used to send the json data in the request
	 */
	public static final String JSON_DATA_KEY = "gridData";
	
	/**
	 * Key used in the JSON data to designate the columns in the BS Table
	 */
	public static final String JSON_COLUMN_KEY = "columns";
	
	/**
	 * Key used in the JSON data to designate the rows in the BS Table
	 */
	public static final String JSON_ROW_KEY = "rows";
	
	// Member Variables
	private String gridId;
	private String gridGroupId;
	private String gridType;
	private String title;
	private String shortTitle;
	private String subtitle;
	private String disclaimer;
	private String primaryYTitle;
	private String secondaryYTitle;
	private String primaryXTitle;
	private String slug;
	private String seriesLabel;
	private boolean approved;
	private boolean archived;
	private int decimalDisplay;
	private Date updateDate;
	private Date createDate;
	private int numberRows = 0;
	private int numberColumns = 0;
	private int abbreviateNumbers;
	private List<GridUsageVO> usage;
	private String legacyId;
	private String legacyName;
	private int yearNo;
	
	// Data containers
	@Expose(serialize = false, deserialize = false)
	private String[] series;
	

	@Expose(serialize = false, deserialize = false)
	private int[] seriesTxtFlg;
	
	@Expose(serialize = false, deserialize = false)
	private List<GridDetailVO> details;
	
	private List<Integer> deletedRows;
	
	/**
	 * 
	 */
	public GridVO() {
		super();
		series = new String[10];
		seriesTxtFlg = new int[10];
		details = new ArrayList<>(10);
		deletedRows = new ArrayList<>(10);
		usage = new ArrayList<>();
	}
	
	/**
	 * Populates the bean from the request object
	 * @param req
	 */
	public GridVO(ActionRequest req) {
		this();
		this.populateData(req);
		
		// Convert JSON data if it exists
		if(! StringUtil.isEmpty(req.getParameter(JSON_DATA_KEY))) {
			convertJson(req.getParameter(JSON_DATA_KEY));
		}
	}
	
	/**
	 * Populates the bean from the result object
	 * @param req
	 */
	public GridVO(ResultSet rs) {
		this();
		this.populateData(rs);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this, false, 1, ",");
	}
	
	/**
	 * @return the gridId
	 */
	@Column(name="grid_id", isPrimaryKey=true)
	public String getGridId() {
		return gridId;
	}

	/**
	 * @return the gridGroupId
	 */
	@Column(name="grid_group_id")
	public String getGridGroupId() {
		return gridGroupId;
	}

	/**
	 * @return the gridType
	 */
	@Column(name="grid_type_cd")
	public String getGridType() {
		return gridType;
	}

	/**
	 * @return the title
	 */
	@Column(name="title_nm")
	public String getTitle() {
		return title;
	}

	/**
	 * @return the shortTitle
	 */
	@Column(name="short_title_nm")
	public String getShortTitle() {
		return shortTitle;
	}

	/**
	 * @return the subtitle
	 */
	@Column(name="subtitle_nm")
	public String getSubtitle() {
		return subtitle;
	}

	/**
	 * @return the disclaimer
	 */
	@Column(name="disclaimer_txt")
	public String getDisclaimer() {
		return disclaimer;
	}

	/**
	 * @return the primaryYTitle
	 */
	@Column(name="y_title_pri_nm")
	public String getPrimaryYTitle() {
		return primaryYTitle;
	}

	/**
	 * @return the secondaryYTitle
	 */
	@Column(name="y_title_sec_nm")
	public String getSecondaryYTitle() {
		return secondaryYTitle;
	}

	/**
	 * @return the primaryXTitle
	 */
	@Column(name="X_TITLE_NM")
	public String getPrimaryXTitle() {
		return primaryXTitle;
	}

	/**
	 * @param xTitle the xTitle to set
	 */
	public void setPrimaryXTitle(String primaryXTitle) {
		this.primaryXTitle = primaryXTitle;
	}

	/**
	 * @return the slug
	 */
	@Column(name="slug_txt")
	public String getSlug() {
		return slug;
	}

	/**
	 * @return the approved
	 */
	@Column(name="approve_flg")
	public boolean isApproved() {
		return approved;
	}

	@Column(name="archive_flg")
	public boolean isArchived() {
		return archived;
	}

	/**
	 * @return the decimalDisplay
	 */
	@Column(name="decimal_display_no")
	public int getDecimalDisplay() {
		return decimalDisplay;
	}

	/**
	 * @return the series label
	 */
	@Column(name="series_label_txt")
	public String getSeriesLabel() {
		return seriesLabel;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}
	
	/**
	 * Returns the first value
	 * @return
	 */
	@Column(name="series_1_nm")
	public String getSeries1() {
		return series[0];
	}
	
	/**
	 * Returns the second value
	 * @return
	 */
	@Column(name="series_2_nm")
	public String getSeries2() {
		return series[1];
	}
	
	/**
	 * Returns the third value
	 * @return
	 */
	@Column(name="series_3_nm")
	public String getSeries3() {
		return series[2];
	}
	
	/**
	 * Returns the fourth value
	 * @return
	 */
	@Column(name="series_4_nm")
	public String getSeries4() {
		return series[3];
	}
	
	/**
	 * Returns the fifth value
	 * @return
	 */
	@Column(name="series_5_nm")
	public String getSeries5() {
		return series[4];
	}
	
	/**
	 * Returns the sixth value
	 * @return
	 */
	@Column(name="series_6_nm")
	public String getSeries6() {
		return series[5];
	}

	/**
	 * Returns the seventh value
	 * @return
	 */
	@Column(name="series_7_nm")
	public String getSeries7() {
		return series[6];
	}
	
	/**
	 * Returns the eigth value
	 * @return
	 */
	@Column(name="series_8_nm")
	public String getSeries8() {
		return series[7];
	}
	
	/**
	 * Returns the ninth value
	 * @return
	 */
	@Column(name="series_9_nm")
	public String getSeries9() {
		return series[8];
	}
	
	/**
	 * Returns the tenth value
	 * @return
	 */
	@Column(name="series_10_nm")
	public String getSeries10() {
		return series[9];
	}
	
	/**
	 * Returns the first value
	 * @return
	 */
	@Column(name="series_1_txt_flg")
	public int getSeries1TxtFlg() {
		return seriesTxtFlg[0];
	}
	
	/**
	 * Returns the second value
	 * @return
	 */
	@Column(name="series_2_txt_flg")
	public int getSeries2TxtFlg() {
		return seriesTxtFlg[1];
	}
	
	/**
	 * Returns the third value
	 * @return
	 */
	@Column(name="series_3_txt_flg")
	public int getSeries3TxtFlg() {
		return seriesTxtFlg[2];
	}
	
	/**
	 * Returns the fourth value
	 * @return
	 */
	@Column(name="series_4_txt_flg")
	public int getSeries4TxtFlg() {
		return seriesTxtFlg[3];
	}
	
	/**
	 * Returns the fifth value
	 * @return
	 */
	@Column(name="series_5_txt_flg")
	public int getSeries5TxtFlg() {
		return seriesTxtFlg[4];
	}
	
	/**
	 * Returns the sixth value
	 * @return
	 */
	@Column(name="series_6_txt_flg")
	public int getSeries6TxtFlg() {
		return seriesTxtFlg[5];
	}

	/**
	 * Returns the seventh value
	 * @return
	 */
	@Column(name="series_7_txt_flg")
	public int getSeries7TxtFlg() {
		return seriesTxtFlg[6];
	}
	
	/**
	 * Returns the eigth value
	 * @return
	 */
	@Column(name="series_8_txt_flg")
	public int getSeries8TxtFlg() {
		return seriesTxtFlg[7];
	}
	
	/**
	 * Returns the ninth value
	 * @return
	 */
	@Column(name="series_9_txt_flg")
	public int getSeries9TxtFlg() {
		return seriesTxtFlg[8];
	}
	
	/**
	 * Returns the tenth value
	 * @return
	 */
	@Column(name="series_10_txt_flg")
	public int getSeries10TxtFlg() {
		return seriesTxtFlg[9];
	}

	/**
	 * @return the details
	 */
	public List<GridDetailVO> getDetails() {
		return details;
	}

	/**
	 * @param gridId the gridId to set
	 */
	public void setGridId(String gridId) {
		this.gridId = gridId;
	}

	/**
	 * @param gridGroupId - the gridGroupId to set
	 */
	public void setGridGroupId(String gridGroupId) {
		this.gridGroupId = gridGroupId;
	}

	/**
	 * @param gridType the gridType to set
	 */
	public void setGridType(String gridType) {
		this.gridType = gridType;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @param shortTitle the shortTitle to set
	 */
	public void setShortTitle(String shortTitle) {
		this.shortTitle = shortTitle;
	}

	/**
	 * @param subtitle the subtitle to set
	 */
	public void setSubtitle(String subtitle) {
		this.subtitle = subtitle;
	}

	/**
	 * @param disclaimer the disclaimer to set
	 */
	public void setDisclaimer(String disclaimer) {
		this.disclaimer = disclaimer;
	}

	/**
	 * @param primaryYTitle the primaryYTitle to set
	 */
	public void setPrimaryYTitle(String primaryYTitle) {
		this.primaryYTitle = primaryYTitle;
	}

	/**
	 * @param secondaryYTitle the secondaryYTitle to set
	 */
	public void setSecondaryYTitle(String secondaryYTitle) {
		this.secondaryYTitle = secondaryYTitle;
	}

	/**
	 * @param slug the slug to set
	 */
	public void setSlug(String slug) {
		this.slug = slug;
	}

	/**
	 * @param approved the approved to set
	 */
	public void setApproved(boolean approved) {
		this.approved = approved;
	}

	/**
	 * @param archived the archived to set
	 */
	public void setArchived(boolean archived) {
		this.archived = archived;
	}

	/**
	 * @param decimalDisplay the decimalDisplay to set
	 */
	public void setDecimalDisplay(int decimalDisplay) {
		this.decimalDisplay = decimalDisplay;
	}

	/**
	 * @param seriesLabel the seriesLabel to set
	 */
	public void setSeriesLabel(String seriesLabel) {
		this.seriesLabel = seriesLabel;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param details the details to set
	 */
	public void setDetails(List<GridDetailVO> details) {
		this.details = details;
	}
	
	/**
	 * Adds a single detail to the collection
	 * @param detail
	 */
	@BeanSubElement
	public void addDetail(GridDetailVO detail) {
		details.add(detail);
	}
	
	/**
	 * @param value value to set
	 */
	public void setSeries1(String value) {
		this.series[0] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setSeries2(String value) {
		this.series[1] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setSeries3(String value) {
		this.series[2] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setSeries4(String value) {
		this.series[3] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setSeries5(String value) {
		this.series[4] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setSeries6(String value) {
		this.series[5] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setSeries7(String value) {
		this.series[6] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setSeries8(String value) {
		this.series[7] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setSeries9(String value) {
		this.series[8] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setSeries10(String value) {
		this.series[9] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setSeries1TxtFlg(int value) {
		this.seriesTxtFlg[0] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setSeries2TxtFlg(int value) {
		this.seriesTxtFlg[1] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setSeries3TxtFlg(int value) {
		this.seriesTxtFlg[2] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setSeries4TxtFlg(int value) {
		this.seriesTxtFlg[3] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setSeries5TxtFlg(int value) {
		this.seriesTxtFlg[4] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setSeries6TxtFlg(int value) {
		this.seriesTxtFlg[5] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setSeries7TxtFlg(int value) {
		this.seriesTxtFlg[6] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setSeries8TxtFlg(int value) {
		this.seriesTxtFlg[7] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setSeries9TxtFlg(int value) {
		this.seriesTxtFlg[8] = value;
	}
	
	/**
	 * @param value value to set
	 */
	public void setSeries10TxtFlg(int value) {
		this.seriesTxtFlg[9] = value;
	}

	/**
	 * @return the numberRows
	 */
	public int getNumberRows() {
		if (numberRows > 0) return numberRows; 
		else return details.size();
	}

	/**
	 * @param numberRows the numberRows to set
	 */
	public void setNumberRows(int numberRows) {
		this.numberRows = numberRows;
	}
	
	/**
	 * Returns the date of last update in String format
	 * @return
	 */
	public String getLastUpdated() {
		Date d = this.updateDate;
		if (d == null) d = createDate;
		
		return Convert.formatDate(d, Convert.DATE_DASH_PATTERN);
	}
	
	/**
	 * Takes the json data and builds the series and details
	 * @param json
	 */
	public void convertJson(String json) {
		
		try {
			JsonElement element = new JsonParser().parse(json);
			JsonObject obj = element.getAsJsonObject();
			processColumns(obj.get(JSON_COLUMN_KEY));
			processRows(obj.get("data"));
			
		} catch(Exception e) { log.error("issue", e); }
		
	}
	
	/**
	 * JSON parses the data and updates the details
	 * @param ele
	 */
	@SuppressWarnings("unchecked")
	protected void processColumns(JsonElement ele) {
		Gson g = new Gson();
		JsonArray jArray = ele.getAsJsonArray();
		
		try {
			for (Object object : jArray) {
				JsonArray jsonObject = (JsonArray) object;
				
				int column = 0;
				for(Object data : jsonObject) {
					updateColumn(g.fromJson(data.toString(), Map.class), column);
					column++;
				}
				
			}
		} catch(Exception e) {
			log.error("unable to parse JSON for grid data", e);
		}
	}
	
	/**
	 * Updates the appropriate series column information
	 * @param column
	 */
	protected void updateColumn(Map<String, ?> column, int columnNo) {
		// If this column is not visible it has been deleted and should not be added
		if (!Convert.formatBoolean(column.get("visible"))) {
			deletedRows.add(columnNo);
			return;
		}
		
		// Get the data elements, make sure they are populated
		String field = (String) column.get("field");
		String cTitle = (String) column.get("title");
		if (StringUtil.isEmpty(field)) return;

		// Parse out the index from the field and store the data in the series array
		String val = field.substring(field.lastIndexOf('_') + 1);
		int index = Convert.formatInteger(val) - 1;
		if(index - deletedRows.size() == -1) {
			seriesLabel = cTitle;
		} else {
			series[index - deletedRows.size()] = cTitle;
			seriesTxtFlg[index - deletedRows.size()] = (Convert.formatDouble(StringUtil.checkVal(column.get("txtFlg")), 0)).intValue();
		}
	}
	
	/**
	 * Parses the JSON element and loads the data into the gridDetailVO
	 * @param ele JSON Element to be parsed
	 */
	@SuppressWarnings("unchecked")
	protected void processRows(JsonElement ele) {
		Gson g = new Gson();
		JsonArray jArray = ele.getAsJsonArray();
		
		try {
			int i=0;
			for (Object object : jArray) {
				updateRow(g.fromJson(object.toString(), Map.class), i++);
			}
		} catch(Exception e) {
			log.error("unable to parse JSON for grid data", e);
		}
	}
	
	/**
	 * Parses the data into a detail object and adds to the collection
	 * @param row
	 */
	protected void updateRow(Map<String, Object> row, int ctr) {
		// Load up the details
		GridDetailVO detail = new GridDetailVO();
		detail.setOrder(ctr);
		detail.setGridId(gridId);
		detail.setGridDetailId(row.get("id") + "");
		
		detail.setDetailType(RowStyle.getEnumKey(row.get("class") + ""));
		detail.setCreateDate(new Date());
		detail.setUpdateDate(new Date());
		
		int currentLabel = 0;
		if (deletedRows.contains(0)) {
			currentLabel = getNewLabel();
		}

		detail.setLabel(row.get("field_" + currentLabel) + "");

		String[] values = detail.getValues();
		int offset = 0;
		for(int i=0; i < 10; i++) {
			// If this column has been deleted skip this data and
			// increment the offset to properly delete the data
			if (deletedRows.contains(i+1)) {
				offset++;
			} else if (i+1 != currentLabel) {
				// nulls are being inserted as a tring "null" value.  Make sure to remove
				String val = StringUtil.checkVal(row.get("field_" + (i + 1)));
				if (val.length() == 0 || "null".equalsIgnoreCase(val)) val = null;
				values[i- offset] = val;
			}
		}
		
		// Add to the local collection
		details.add(detail);
	}
	
	/**
	 * If the first row has been deleted find the 
	 * next, viable, row and set the label from that value.
	 * @param row
	 * @param detail
	 */
	private int getNewLabel() {
		for (int i = 0; i< 10; i++) {
			if (!deletedRows.contains(i)) {
				return i;
			}
		}
		
		// This will only occur when all the columns have been deleted
		// at which point it no longer matters
		return 0;
	}

	
	/**
	 * Converts the Grid into JSON tables formatted for Bootstrap
	 * @return
	 */
	public String getTableJson() {
		
		Map<String, List<Map<String, String>>> tableData = new HashMap<>();
		Gson g = new Gson();
		tableData.put(JSON_ROW_KEY, getRows());
		tableData.put(JSON_COLUMN_KEY, getColumns());

		return g.toJson(g.toJson(tableData));
	}
	
	/**
	 * Returns the Map Structure for bootstrap tables
	 * @return
	 */
	public Map<String, List<Map<String, String>>> getTableMap() {
		Map<String, List<Map<String, String>>> tableData = new HashMap<>();
		tableData.put(JSON_ROW_KEY, getRows());
		tableData.put(JSON_COLUMN_KEY, getColumns());
		return tableData;
	}
	
	/**
	 * Builds the row of data
	 * @return
	 */
	protected List<Map<String, String>> getRows() {
		// Stores each row of data
		List<Map<String, String>> rows = new ArrayList<>();
		
		// Loop all of the rows of data
		for(GridDetailVO detail : details) {
			Map<String, String> row = new LinkedHashMap<>();
			String[] values = detail.getValues();
			
			// Add the id and class as well as the label
			if (! StringUtil.isEmpty(detail.getDetailType()))
				row.put("class", RowStyle.valueOf(detail.getDetailType()).getName());
			else 
				row.put("class", "bs-data");
			
			row.put("id", detail.getGridDetailId());
			row.put(FIELD_LABEL + 0, detail.getLabel());
			
			// Loop the values and add to container assumes "" for empty cell and null for unused column
			for (int x = 1; x < 11; x++) {
				if (values[x-1] != null || x <= getNumberColumns()) {
					row.put(FIELD_LABEL + x,StringUtil.checkVal(values[x-1]));
				}
			}
			
			rows.add(row);
		}
		
		return rows;
	}
	
	/**
	 * Builds the column objects for the JSON Stream
	 * @return
	 */
	protected List<Map<String, String>> getColumns() {
		// Create the columns
		List<Map<String, String>> columns = new ArrayList<>();
		Map<String, String> column = new LinkedHashMap<>();
		
		// First col header is empty as it holds labels for each row
		column.put("class", "bs-header");
		column.put("field", FIELD_LABEL + "0");
		column.put("fieldIndex", "0");
		column.put("title", getSeriesLabel());
		column.put("txtFlg", "0");
		columns.add(column);
		for (int x = 1; x < getNumberColumns()+1; x++) {
			column = new LinkedHashMap<>();

			column.put("class", "bs-header");
			column.put("field", FIELD_LABEL + x);
			column.put("fieldIndex", Integer.toString(x));
			column.put("title", series[x-1]);
			column.put("txtFlg", StringUtil.checkVal(seriesTxtFlg[x-1], "0"));
			columns.add(column);
		}
		
		return columns;
		
	}

	/**
	 * @return the series
	 */
	public String[] getSeries() {
		return series;
	}
	

	public int[] getSeriesTxtFlg() {
		return seriesTxtFlg;
	}
	
	/**
	 * Returns the number of columns with populated data
	 * @return
	 */
	public int getNumberColumns() {
		if (numberColumns == 0) numberColumns = calculateNumberColumns();
		
		return numberColumns;
	}
	
	/**
	 * Calculates the number of columns with populated data
	 * @return
	 */
	private int calculateNumberColumns() {
		int numCols = 0;
		
		// Count the series
		for (int i = 0; i < series.length; i++) {
			if (! StringUtil.isEmpty(series[i]) && i > numCols) numCols = i;
		}
		
		// Count the details
		for (int x = 0; x < details.size(); x++) {
			GridDetailVO vo = details.get(x);

			for (int i = 0; i < series.length; i++) {
				if (!StringUtil.isEmpty(vo.getValues()[i]) && i >= numCols) numCols = i;
			}
		}
		
		return ++numCols;
	}

	@Column(name="abbreviate_numbers")
	public int getAbbreviateNumbers() {
		return abbreviateNumbers;
	}

	public void setAbbreviateNumbers(int abbreviateNumbers) {
		this.abbreviateNumbers = abbreviateNumbers;
	}

	/**
	 * @return the usage
	 */
	public List<GridUsageVO> getUsage() {
		return usage;
	}

	/**
	 * @param usage the usage to set
	 */
	public void setUsage(List<GridUsageVO> usage) {
		this.usage = usage;
	}
	
	public void addUsage(GridUsageVO use) {
		usage.add(use);
	}

	@Column(name="legacy_id", isReadOnly=true)
	public String getLegacyId() {
		return legacyId;
	}

	public void setLegacyId(String legacyId) {
		this.legacyId = legacyId;
	}

	@Column(name="legacy_nm", isReadOnly=true)
	public String getLegacyName() {
		return legacyName;
	}

	public void setLegacyName(String legacyName) {
		this.legacyName = legacyName;
	}

	/**
	 * @return the yearNo
	 */
	@Column(name="year_no")
	public int getYearNo() {
		return yearNo;
	}

	/**
	 * @param yearNo the yearNo to set
	 */
	public void setYearNo(int yearNo) {
		this.yearNo = yearNo;
	}
}
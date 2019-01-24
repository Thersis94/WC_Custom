package com.wsla.util.migration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.util.StringUtil;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketVO;
import com.wsla.util.migration.vo.ClosedSOFileVO;

/****************************************************************************
 * <p><b>Title:</b> Ticket.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jan 24, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class Ticket extends AbsImporter {

	private List<ClosedSOFileVO> data;

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbstractImporter#run()
	 */
	@Override
	void run() throws Exception {
		data = readFile(props.getProperty("closedSoFile"), ClosedSOFileVO.class, SHEET_1);

		String sql = StringUtil.join("delete from ", schema, "wsla_ticket");
		delete(sql);

		save();
	}


	/**
	 * Save the imported providers to the database.
	 * @param data
	 * @throws Exception 
	 */
	@Override
	protected void save() throws Exception {
		//turn the list of data into a unique list of providers we write to the database
		Map<String, TicketVO> tickets = new HashMap<>(data.size());
		for (ClosedSOFileVO dataVo : data) {
			if (tickets.containsKey(dataVo.getServiceOrderNo())) continue;

			TicketVO vo = transposeData(dataVo, new TicketVO());
			tickets.put(vo.getTicketIdText(), vo);
		}

		writeToDB(new ArrayList<>(tickets.values()));
	}


	/**
	 * Transpose and enhance the data we get from the import file into what the new schema needs
	 * @param dataVo
	 * @param vo
	 * @return
	 */
	private TicketVO transposeData(ClosedSOFileVO dataVo, TicketVO vo) {
		vo.setTicketId(dataVo.getServiceOrderNo());
		vo.setTicketIdText(dataVo.getServiceOrderNo());
		vo.setProductCategoryId("FTP");
		vo.setStatusCode(StatusCode.CLOSED);
		//TODO need creator/owner userId
		
		vo.setCreateDate(dataVo.getRegistrationDate());
		return vo;
	}
}
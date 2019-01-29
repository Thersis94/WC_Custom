<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html lang="en">
	<head>
		<title>Packing List PDF</title>
		<style type="text/css">
			.heading {
				height:25px;background-color:#ccc;font-size:16px;font-weight:bolder; text-align:center;
			}
			.subheading {
				border-bottom : solid 1px black;font-weight: bold; color: #166ABD;
			}
			
			.tableFull {
				border-collapse: collapse; width:100%; border: none;
			}
			
			.tableHeader {
				height:30px;background-color:#166ABD;color:white;font-size:20px;font-weight:bolder;
			}
			
			.rowHeader {
				height:25px;background-color:gray;font-size:14px;color:white;font-weight:bolder;
			}
			
			td, th {
				padding:0px 10px 0px 10px;
				border-bottom:solid 1px #ccc;
				height:25px;
			}
			
			.small td { font-size: 12px; }
			.nowrap { white-space: nowrap; }
			.bottom { border-bottom: solid 2px black; }
			.label {
				font-weight : bolder;
			}

			@page { size:landscape; }
		</style>
	</head>
	<body>
		<table summary="Page Header" class="tableFull tableHeader">
			<tr>
				<td style="padding-left:20px width:120px;">WSLA</td>
				<td style="text-align:left;">${rb['wsla.name']}</td>
			</tr>
		</table>
		
		<h3>${rb["wsla.debit.memo"]}&nbsp; - &nbsp; ${data.oem.providerName} &amp; ${data.retailer.providerName}</h3>
		
		<table summary="Memo Status Info" style="width:60%;">
			<tr><td class="label">${rb["common.date"]}:</td><td>${data.createDate?date}</td></tr>
			<tr><td class="label">${rb["debit.customer.memo.id"]}:</td><td>${data.customerMemoCode}</td></tr>
			<tr><td class="label">${rb["debit.transfer.amount"]}:</td><td>&#36;${data.totalCreditMemoAmount}</td></tr>
			
		</table>
		<h3>${rb["credit.memo.list"]}</h3>
		
		<table class="tableFull small" border="0" summary="list of credit memos">
			<tr class="rowHeader">
				<th nowrap>${rb["ticket.id"]}</th>
				<th>${rb["debit.memo.id"]}</th>
				<th>${rb["product.productName"]}</th>
				<th>${rb["wsla.approval.date"]}</th>
				<th>${rb["common.approvedBy"]}</th>
				<th>${rb["refund.amount"]}</th></tr>
			<#list data.creditMemos as memo>
				<tr><td>${memo.ticketIdText}</td>
					<td>${memo.customerMemoCode}</td>
					<td>${memo.productName}</td>
					<td>${memo.approvalDate?date}</td>
					<td>${memo.approvedBy}</td>
					<td>&#36;${memo.refundAmount}</td></tr>
			</#list>
			<tr><td colspan="6">&nbsp;</td></tr>
			
			<tr class="total"><th>${rb['common.total']}:</th>
				<th colspan="4">&nbsp;</th>
				<th>&#36;${data.totalCreditMemoAmount}</th></tr>
		</table>
		<p>&nbsp;</p>
		<br/>
		<br/>
		<table class="tableFull" summary="Signature Line">
			<tr><td width="60">${rb['common.recipient']}:</td>
				<td width="250">&nbsp;</td>
				<td width="80">${rb['wsla.ticket.schedule.signature']}:</td>
				<td>&nbsp;</td>
				<td width="50"> ${rb['common.date']}:</td>
				<td width="80">&nbsp;</td>
		</table>
	</body>
</html>
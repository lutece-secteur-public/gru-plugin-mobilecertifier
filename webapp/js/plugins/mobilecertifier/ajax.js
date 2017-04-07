function validateCode() {
	$('#modal-content').load("jsp/site/Portal.jsp?page=mobilecertifier&action=validateCode&view_mode=ajax&validation_code="+$("#validation_code").val());	
}
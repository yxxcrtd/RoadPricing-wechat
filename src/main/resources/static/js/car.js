
FastClick.attach(document.body);

(function(window, $) {
	// 验证车牌号
	// document.getElementById("car_no").addEventListener("input", car_no);

	// form提交
	$(".btnable a").bind("click", function() {
		$(this).addClass("btnDisdefalut");
		$(this).unbind("click");
		$(".bindCarNO").submit();
	});

	$("#car_no").blur(function(e) {
		var no = $(this).val().trim();
		if (no.match(/[a-zA-Z]+/)) {
			$(this).val($(this).val().trim().toLocaleUpperCase());
		}
	});

	// 选择并统计金额
	var total = parseFloat(0);
	$(":checkbox").click(function() {
		if ($(this).prop("checked")) {
			total += parseFloat($(this).val());
		} else {
			total -= parseFloat($(this).val());
		}
		$("#money").html("￥" + parseFloat(total).toFixed(0));
	});

	// 全选/全不选
    $("#total_select").click(function() {
		total = 0;
    	if ($(this).prop("checked")) {
    		$("input[name='arrearage']").each(function() {
    			$(this).prop("checked", true);
				total += parseFloat($(this).val());
    		});
    	} else {
    		$("input[name='arrearage']").each(function() {
    			$(this).prop("checked", false);
    		});
    	}
		$("#money").html("￥" + parseFloat(total).toFixed(0));
    });

	$("#total_pay a").bind("click", function() {
		var money = moneyContent.substring(1, moneyLength);
		if (0 == parseFloat(total).toFixed(0)) {
			alert(123456);
			$(this).unbind("click");
		}
	});

})(window, $);


function car_no() {
	var no = $(this).val().trim();
	// 将输入的字母变成大写
	// no = no.toLocaleUpperCase();
	console.info(no);
	if (no.match(/[a-zA-Z]+/)) {
		$(this).val($(this).val().trim().toLocaleUpperCase());
	}
	console.info($(this).val());

	// $(this).attr("value", no);

    // $(this).val($(this).val().trim().toLocaleUpperCase());

	// var express = /^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领A-Z]{1}[A-Z]{1}[警京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼]{0,1}[A-Z0-9]{4}[A-Z0-9挂学警港澳]{1}$/;
	//
	// if (!express.test(no)) { // "" == no ||
	// 	$("#tips").text("请输入正确的车牌号！");
	// }
	// return express.test(no);
}

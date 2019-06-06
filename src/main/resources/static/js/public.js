
var _hmt = _hmt || [];
var obj = {
	srollId: "",
	tabIndex: 0,
	scrollTop: 0
};
$(function() {
	if ($(".dialog_footer") && $(".dialog_footer").length > 0) {
		$(".dialog_footer a,.dialog").click(function() {
			$(".dialog").hide();
		});
		$(".dialog_cont").click(function(e) {
			e.stopPropagation();
		});
		$("#payBtn").click(function() {
			$(".dialog").show();
		});
	}

	//滚动加载
	function scroll() {
		if (document.body.scrollHeight - document.body.scrollTop <= document.body.clientHeight + 100) {
			$(".loading").show();
			typeof scrollLoad != "undefined" ? scrollLoad.apply($(".loading").eq(0)) : "";
		}
		obj.scrollTop = document.body.scrollTop;
	}
	if ($(".loading").length > 0 && window.location.hash.indexOf("back") == -1){
		$(document).scroll(scroll);
	}
	//点赞功能
	if ($(".defalut_good") && $(".defalut_good").length) {
		$("#dz a").click(function() {
			var _this = $(this).children("i");
			if ($(this).parent().attr("isBuy") == -1) {
				return;
			}
			var className = $(_this).attr("targetid"),
				type = 0;
			var spanObj = $("#" + className);
			var sibClassName = className == "good" ? "nogood" : "good";
			if ($(_this).hasClass(className)) {
				$(_this).removeClass(className);
				//spanObj.html(parseInt(spanObj.html())-1);
				type = 0;
			} else {
				var sibobj = $(this).siblings("a").children("i");
				if (sibobj.hasClass(sibClassName)) {
					sibobj.removeClass(sibClassName);
					var spanObj1 = $("#" + sibobj.attr("targetid"));
					//spanObj1.html(parseInt(spanObj1.html())-1);
				}
				$(_this).addClass(className);
				//spanObj.html(parseInt(spanObj.html())+1);
				type = className == "good" ? 1 : 2;
			}
			execGood ? execGood.call(_this, type) : "";
		});
	}
	//切换，如关注页面
	if ($("#switch") && $("#switch").length) {
		obj.srollId = $("#switch").children(".on").attr("target-id");
		$("#switch").children("span").click(switchFun);
	}
	//添加点击效果
	if ($(".clickHove") && $(".clickHove").length)
		$(".clickHove").click(function() {
			var _this = $(this);
			_this.addClass("active");
			_this.siblings().removeClass("active");
		});
	if($("[skipAddr]") && $("[skipAddr]").length)
		$("[skipAddr]").click(function(e) {
			if($(e.target).hasClass("atten_btn") || window.location.href.indexOf($(this).attr("skipAddr"))!=-1)
				return;
			$(this).css("background-color","#e6e6e6");
			window.location.href=$(this).attr("skipAddr");
		});
	if ($(".needBack") && $(".needBack").length) {
		if (sessionStorage.length > 0 && window.location.hash.indexOf("back") != -1) {
			for (var needBack = $(".needBack"), i = needBack.length - 1; i >= 0; i--) {
				needBack.eq(i).replaceWith(sessionStorage.getItem(needBack.eq(i).attr("id")));
			}
			if ($("#switch") && $("#switch").length) {
				var base = JSON.parse(sessionStorage.getItem("base"));
				$("#switch").children("span").removeClass("on");
				switchFun.apply($("#switch").children("span").eq(base.switch));
				document.body.scrollTop = base.srollTop;
			}
		}
		setTimeout(function(){
			$(document).scroll(scroll);
		},50);
		$(".needBack a").click(function(e){
			if(e.target.nodeName!="A" && $(e.target).parents("a").length<=0){
				return;
			}
		    var hash = window.location.hash;
			if (history.state!="attenList") {
				window.history.replaceState("attenList", "", window.location.href + (hash.indexOf("#") == -1 ? "#back=1" : "&back=1"));
			}
			for (var needBack = $(".needBack"), i = needBack.length - 1; i >= 0; i--) {
				sessionStorage.setItem(needBack.eq(i).attr("id"), needBack[i].outerHTML);
			}
			if ($("#switch") && $("#switch").length)
				sessionStorage.setItem("base", JSON.stringify({
					switch: obj.tabIndex,
					srollTop: obj.scrollTop
				}));
		});
	}
});
//切换
function switchFun() {
	if (!$(this).hasClass("on")) {
		var targetId = $(this).attr("target-id");
		obj.scrollTop = document.body.scrollTop;
		$(this).siblings("span").removeClass("on");
		$(this).addClass("on");
		$("#" + targetId).show();
		obj.srollId = targetId;
		obj.tabIndex = $(this).index();
		$("#" + $(this).attr("hide-id")).hide();
	}
}
//弹出框
function dialog(content, time, fun) {
	if ($(".dialog_tip_cont") && $(".dialog_tip_cont").length > 0)
		$(".dialog_tip_cont").html(content).show();
	else {
		$(document.body).append('<div class="dialog_tip_cont">' + content + '</div>');
		$(".dialog_tip_cont").show();
	}
	setTimeout(function() {
		$(".dialog_tip_cont").hide();
		fun ? fun() : "";
	}, time ? time : 3000);
}

//date添加格式化方法
Date.prototype.format = function(formatDate, formatString1) {
		//		var dateArr=formatString1.split("-");
		//		var formatString="yyyy-mm-dd hh:mm:ss";
		var template = "ymdhms";
		var date = new Date(formatDate);
		var dateArrInfo = [];
		dateArrInfo.push(date.getFullYear() + "-");
		var month = date.getMonth() + 1;
		dateArrInfo.push(month > 9 ? month : "0" + month + "-");
		var dateDay = date.getDate();
		dateArrInfo.push((dateDay > 9 ? dateDay : "0" + dateDay) + " ");
		var hour = date.getHours();
		dateArrInfo.push((hour > 9 ? hour : "0" + hour) + ":");
		var min = date.getMinutes();
		dateArrInfo.push((min > 9 ? min : "0" + min) + ":");
		var sec = date.getSeconds();
		dateArrInfo.push(sec > 9 ? sec : "0" + sec);
		if (typeof formatString1 == "undefined") {
			return dateArrInfo.join("");
		} else {
			formatString1 = formatString1.toLowerCase();
			if (formatString1 == "yyyy-mm-dd") {
				return dateArrInfo[0] + dateArrInfo[1] + dateArrInfo[2];
			} else if (formatString1 == "mm-dd") {
				return dateArrInfo[1] + dateArrInfo[2];
			} else if (formatString1 == "hh:mm:ss") {
				return dateArrInfo[3] + dateArrInfo[4] + dateArrInfo[5];
			} else if (formatString1 == "mm:ss") {
				return dateArrInfo[4] + dateArrInfo[5];
			} else if (formatString1 == "hh:mm") {
				return dateArrInfo[3] + dateArrInfo[4];
			}
		}

	}
	//底部弹出按钮
function tip_dialog(fun, arrBtn, title) {
	var html = ['<div class="sel_mask">'];
	html.push('<ul class="selBtn">');
	if (typeof arrBtn == "string")
		html.push('<li class="sel_title"><a href="javascript:void(0);">' + arrBtn + '</a></li>');
	else
		html.push('<li  class="sel_title"><a href="javascript:void(0);">' + title + '</a></li>');
	if (!arrBtn || typeof arrBtn == "number" || typeof arrBtn == "string") {
		html.push('<li class="mb"><a href="javascript:void(0);">确定</a></li>');
		if (typeof arrBtn == "undefined" || arrBtn != 1) {
			html.push('<li><a href="javascript:void(0);">取消</a></li>');
		}
	} else {
		for (var i = arrBtn.length; i > 0; i--) {
			html.push('<li><a href="' + arrBtn[i][0] ? arrBtn[i][0] : "javascript:void(0);" + '">' + arrBtn[i] + '</a></li>');
		}
	}
	html.push('</ul></div>');
	$(document.body).append(html.join(""));
	setTimeout(function() {
		$(".selBtn").addClass("selBtnActive");
	}, 50);
	$(".selBtn li").click(function(e) {
		if ($(this).hasClass("sel_title")) {
			e.stopPropagation();
			return;
		}
		$(this).children("a").addClass("colorf15438");
		var index = $(this).index();
		if ((!arrBtn || typeof arrBtn == "number" || typeof arrBtn == "string") && index == 1) {
			typeof fun == "function" ? fun.call($(this), index) : "";
		} else if (typeof arrBtn == "object" && arrBtn.length) {
			typeof fun == "function" ? fun.call($(this), index) : "";
		}
	});
	$(".sel_mask").click(function() {
		$(".selBtn").addClass("selBtnActive1");
		setTimeout(function() {
			$(".sel_mask").remove();
		}, 500);
	});
}

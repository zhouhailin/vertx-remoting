$(document).ready(function(){
	
	var opts = {
        addr: 'localhost',
        post: 9888
    }

	vrc.init(opts);
	
	// 清楚日志输出文本域
	$(".vrc-btn-send").click(function(){
		vrc.invokeOneWay();
	});

});

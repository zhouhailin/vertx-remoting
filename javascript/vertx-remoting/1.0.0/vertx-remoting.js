/*global window, require, define */
(function(_window) {
	
	// ws://localhost:9900/v1/ws
	var ws = null;
	
	var options = {};
	
	const VERSION = 3;
	var requestID = 1;
	const OP_CODE_SUCCESS = 0;
    const OP_CODE_SYSTEM_ERROR = 1;
    const OP_CODE_SYSTEM_BUSY = 2;
    const OP_CODE_REQUEST_CODE_NOT_SUPPORTED = 3;
	
	const TRAFFIC_TYPE_REQUEST_SYNC = 'REQUEST_SYNC';
	const TRAFFIC_TYPE_REQUEST_ASYNC = 'REQUEST_ASYNC';
	const TRAFFIC_TYPE_REQUEST_ONEWAY = 'REQUEST_ONEWAY';
	const TRAFFIC_TYPE_RESPONSE = 'RESPONSE';
	
	var messageHandler = {
		onLogoutFailedEvt					: function(code, message){
			console.log("demo onLogoutFailedEvt code : " + code + ", message : " + message);
		}
	};
	
	var Log = {
		trace : function(msg){
			writeLine(new Date() + " [TRACE] " + msg);
		},
		debug : function(msg){
			writeLine(new Date() + " [DEBUG] " + msg);
		},
		info  : function(msg){
			writeLine(new Date() + " [ INFO] " + msg);
		},
		warn  : function(msg){
			writeLine(new Date() + " [ WARN] " + msg);
		},
		error : function(msg){
			writeLine(new Date() + " [ERROR] " + msg);
		}
	};
	
	var ObjectUtils = {
		isEmpty								: function(obj){
			return (obj === null || obj === undefined)
		},
		isNotEmpty							: function(obj){
			return !this.isEmpty(obj);
		}
	};

	function write(line){
		console.log(line);
	}

	function writeLine(line){
		write(line + "\n");
	}

	var JSONUtils = {
			decode							: function(text){
				return JSON.parse(text);
			},
			encode							: function(object){
				return JSON.stringify(object);
			}
		};
	
	var StrUtils = {
		isEmpty								: function(str){
			return (str === null || str === undefined || str.length === 0);
		},
		isNotEmpty							: function(str){
			return !this.isEmpty(str);
		},
		equals								: function(str1, str2){
			return (str1 === str2);
		},
		notEquals							: function(str1, str2){
			return !this.equals(str1, str2);
		}			
	};

	// ws 
	function onmessage(data){
		console.log("ws onmessage" + data);
		var res = JSONUtils.decode(data);
		console.log(res);
	}

	function invokeOneWay(){
		if(ws == null) {
			Log.error("ws is not connected.");
			return;
		}
		// ws.send(JSONUtils.encode(createRequest(13, TRAFFIC_TYPE_REQUEST_ONEWAY, 'test', {}, null)));
		ws.send(JSONUtils.encode(createRequest(13, TRAFFIC_TYPE_REQUEST_ASYNC, 'test', {}, null)));
	}
	
	function createRequest(cmdCode, trafficType, remark, properties, payload){
		const cmd = {};
		cmd.cmdCode = cmdCode;
		cmd.cmdVersion = VERSION;
		cmd.requestId = requestID++;
		cmd.trafficType = trafficType;
		cmd.opCode = OP_CODE_SUCCESS;
		cmd.remark = remark;
		cmd.properties = properties;
		cmd.payload = payload;
		return cmd;
	}
	
	function onopen(){
		console.log("ws onopen ...");
	}
	
	function onclose(code, reason){
		console.log("ws onclose code : " + code + ", reason : " + reason);
		ws = null;
		setTimeout(function(){ connServer(); }, 1000);
	}

	function connServer(){
		if(ObjectUtils.isNotEmpty(ws)){
			Log.warn("connected server already.");
			return;
		}
		ws = new WebSocket(options.wsurl);
		ws.onopen = function(evt){
			onopen();
		}
		ws.onclose = function(evt){
			ws = null;
			onclose(evt.code, evt.reason);
		}
		ws.onmessage = function(evt){
			onmessage(evt.data);
		}
	}
	
	function init(opts, handler0){
		opts = opts || {};
		if (opts.debug){
			options.debug = opts.debug;
		}else{
			options.debug = false;
		}
		if(opts.addr){
			options.addr = opts.addr;
		}else{
			options.addr = "127.0.0.1";
		}
		if(opts.port){
			options.port = opts.port;
		}else{
			options.port = 9888;
		}
		if(opts.wsurl){
			options.wsurl = opts.wsurl;
		}else{
			options.wsurl = "ws://"+options.addr+":"+options.port+"/vertx/remoting";
		}
		if(handler0){
			handler = handler0;
		}
		console.log("opts : " + JSONUtils.encode(opts));
		console.log("options : " + JSONUtils.encode(options));
		
		connServer();
	}
	
	
	
	// Export public API
    var vrc = {};
    vrc.init = init;
    vrc.invokeOneWay = invokeOneWay;
    
    if (('undefined' !== typeof module) && module.exports) {
      // Publish as node.js module
      module.exports = vrc;
    } else if (typeof define === 'function' && define.amd) {
      // Publish as AMD module
      define(function() {return vrc;});
    } else {
      // Publish as global (in browsers)
      _previousRoot = _window.vrc;
    
      // **`noConflict()` - (browser only) to reset global 'uuid' var**
      vrc.noConflict = function() {
        _window.vrc = _previousRoot;
        return vrc;
      };
      _window.vrc = vrc;
    }

})('undefined' !== typeof window ? window : null);
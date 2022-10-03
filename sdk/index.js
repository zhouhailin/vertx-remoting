const vrc = {
    options: {
        url: '',
        debug: false,
        version: 1,
        requestId: 1,
        socket: null,
        state: WebSocket.CLOSED,
        auth: {
            code: 1,
            // 0 - none, 1 - need, 2 - success, 3 - failed
            state: 0,
            resp: null,
            handle: null,
            payload: {
                nonce: '',
                token: ''
            }
        },
        close: {
            handle: null,
            timeout: 1000
        },
        callback: {
            onOpened: null,
            onClosed: null,
            request: new Map(),
            response: new Map()
        },
    },
    createRequest: function (cmdCode, trafficType, remark, properties, payload) {
        const cmd = {};
        cmd.cmdCode = cmdCode;
        cmd.cmdVersion = this.options.version;
        cmd.requestId = this.options.requestId++;
        // 	const TRAFFIC_TYPE_REQUEST_ASYNC = 'REQUEST_ASYNC';
        cmd.trafficType = trafficType;
        // OP_CODE_SUCCESS
        cmd.opCode = 0;
        cmd.remark = remark;
        cmd.properties = properties;
        cmd.payload = payload;
        return cmd;
    },
    createResponse: function (request) {
        const cmd = {};
        cmd.cmdCode = request.cmdCode;
        cmd.cmdVersion = request.version;
        cmd.requestId = request.requestId;
        cmd.trafficType = 'RESPONSE';
        // OP_CODE_SUCCESS
        cmd.opCode = 0;
        return cmd;
    },
    open(succeed, error) {
        if (this.options.state === WebSocket.OPEN || this.options.state === WebSocket.CONNECTING) {
            if (error) {
                error('socket is opened.');
            }
            return;
        }
        this.options.state = WebSocket.CONNECTING;
        const that = this;
        let socket = new WebSocket(this.options.url);
        socket.onopen = function (event) {
            that.options.state = WebSocket.OPEN;
            if (that.options.debug) {
                console.log("WebSocket is open now.", event);
            }
            that.onOpened();
        };
        socket.onclose = function (event) {
            that.options.state = WebSocket.CLOSED;
            if (that.options.debug) {
                console.log("WebSocket is closed now.", event);
            }
            let res = {};
            if (that.options.auth.state === 3) {
                res.code = 401;
                res.reason = that.options.auth.resp.message;
            } else {
                res.code = event.code;
                res.reason = event.reason;
            }
            that.onClosed(res);
        };
        socket.onerror = function (event) {
            that.options.state = WebSocket.CLOSED;
            if (that.options.debug) {
                console.log('WebSocket error: ', event);
            }
            that.onClosed();
        };
        socket.onmessage = function (event) {
            let cmd = JSON.parse(event.data);
            if (that.options.debug) {
                console.log('Message from server ', cmd);
                console.log('callback ', that.options.callback);
            }
            if (cmd.trafficType === 'RESPONSE') {
                let callback = that.options.callback.response.get(cmd.requestId);
                if (callback !== undefined) {
                    that.options.callback.response.delete(cmd.requestId);
                    callback(cmd);
                }
            } else if (cmd.trafficType === 'REQUEST_ASYNC') {
                let callback = that.options.callback.request.get(cmd.cmdCode);
                if (callback !== undefined) {
                    let response = callback(cmd);
                    if (response !== null) {
                        that.send(response);
                    } else {
                        that.send(that.createResponse(cmd));
                    }
                }
            } else if (cmd.trafficType === 'REQUEST_ONEWAY') {
                let callback = that.options.callback.request.get(cmd.cmdCode);
                if (callback !== undefined) {
                    callback(cmd);
                }
            }
        };
        this.options.socket = socket;
        if (succeed) {
            succeed();
        }
    },
    close(succeed, error) {
        if (this.options.state === WebSocket.OPEN) {
            this.options.state = WebSocket.CLOSING;
            this.options.socket.close();
            if (succeed) {
                succeed();
            }
        } else if (error) {
            error('socket is closed.');
        }
    },
    onOpened() {
        if (this.options.auth.state === 1) {
            const that = this;
            this.invokeAsync(this.options.auth.code, this.options.auth.payload, function (cmd) {
                let payload = cmd.payload;
                if (payload.code === 200) {
                    that.options.auth.state = 2;
                } else {
                    that.options.auth.state = 3;
                    that.options.auth.resp = payload;
                    that.close();
                }
                if (that.options.auth.handle) {
                    that.options.auth.handle(payload);
                }
            }, function (error) {
                console.log("auth failed ", error)
            });
        }
        if (this.options.callback.onOpened) {
            this.options.callback.onOpened();
        }
    },
    onClosed() {
        if (this.options.auth.state > 0) {
            this.options.auth.state = 1;
        }
        if (this.options.callback.onClosed) {
            this.options.callback.onClosed();
            if (this.options.close.handle) {
                const that = this;
                setTimeout(function () {
                    that.options.close.handle();
                }, this.options.close.timeout);
            }
        }
    },
    send(msg, interceptor, succeed, error) {
        if (this.options.state === WebSocket.OPEN) {
            if (interceptor) {
                interceptor(msg);
            }
            this.options.socket.send(JSON.stringify(msg));
            if (succeed) {
                succeed(msg);
            }
        } else if (error) {
            error('socket is closed.');
        }
    },
    addEventListener(code, listener) {
        if (listener) {
            this.options.callback.request.set(code, listener);
        }
    },
    invokeOneway(code, payload, error) {
        this.invokeOneway0(code, null, null, payload, null, null, error);
    },
    invokeOneway0(code, remark, properties, payload, interceptor, succeed, error) {
        this.send(this.createRequest(code, 'REQUEST_ONEWAY', remark, properties, payload), interceptor, succeed, error);
    },
    invokeAsync(code, payload, callback, error) {
        this.invokeAsync0(code, null, null, payload, callback, null, null, error);
    },
    invokeAsync0(code, remark, properties, payload, callback, interceptor, succeed, error) {
        const that = this;
        this.send(this.createRequest(code, 'REQUEST_ASYNC', remark, properties, payload), function (cmd) {
            that.options.callback.response.set(cmd.requestId, callback)
            if (interceptor) {
                interceptor(cmd);
            }
        }, succeed, error);
    },
}

exports.init = function (url, debug) {
    vrc.options.url = url;
    vrc.options.debug = debug;
    return this;
}

exports.auth = function (nonce, token, handle) {
    vrc.options.auth.state = 1;
    vrc.options.auth.payload.nonce = nonce;
    vrc.options.auth.payload.token = token;
    if (handle) {
        vrc.options.auth.handle = handle;
    }
    return this;
}

exports.open = function (succeed, error) {
    vrc.open(succeed, error);
    return this;
}

exports.close = function (succeed, error) {
    vrc.close(succeed, error);
    return this;
}

exports.onOpened = function (callback) {
    vrc.options.callback.onOpened = callback;
    return this;
}

exports.onClosed = function (callback, handle, timeout) {
    vrc.options.callback.onClosed = callback;
    if (handle) {
        vrc.options.close.handle = handle;
        if (timeout > 0) {
            vrc.options.close.timeout = timeout;
        }
    }
    return this;
}

exports.createResponse = function (request) {
    return vrc.createResponse(request);
}

exports.invokeOneway = function (code, payload, error) {
    vrc.invokeOneway(code, payload, error);
    return this;
}

exports.invokeOneway0 = function (code, remark, properties, payload, interceptor, succeed, error) {
    vrc.invokeOneway0(code, remark, properties, payload, interceptor, succeed, error);
    return this;
}

exports.invokeAsync = function (code, payload, callback, error) {
    vrc.invokeAsync(code, payload, callback, error);
    return this;
}

exports.invokeAsync0 = function (code, remark, properties, payload, callback, interceptor, succeed, error) {
    vrc.invokeAsync0(code, remark, properties, payload, callback, interceptor, succeed, error);
    return this;
}

exports.addEventListener = function (code, listener) {
    vrc.addEventListener(code, listener);
    return this;
}


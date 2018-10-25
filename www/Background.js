 	var exec = require("cordova/exec");

    	/**
		* Constructor.
		*
		* @returns {Background}
		*/
    	function Background() {

    	};

    	/**
		* Register for system events.
		*
		* @param {Function} successCallback
		* @param {Function} errorCallback
		*/
    	Background.prototype.register = function (successCallback, errorCallback) {
    		if (errorCallback == null) {
    			errorCallback = function () {
    			};
    		}

    		if (typeof errorCallback != "function") {
    			console.log("Background.register failure: failure parameter not a function");
    			return;
    		}

    		if (typeof successCallback != "function") {
    			console.log("Background.register failure: success callback parameter must be a function");
    			return;
    		}

    		exec(successCallback, errorCallback, 'Background', 'registerDevicePowerChanges', []);
    	};

    	Background.prototype.unregister = function (successCallback, errorCallback) {
    		if (errorCallback == null) {
    			errorCallback = function () {
    			};
    		}

    		if (typeof errorCallback != "function") {
    			console.log("Background.unregister failure: failure parameter not a function");
    			return;
    		}

    		if (typeof successCallback != "function") {
    			console.log("Background.unregister failure: success callback parameter must be a function");
    			return;
    		}

    		exec(successCallback, errorCallback, 'Background', 'unregisterDevicePowerChanges', []);
    	};

    	Background.prototype.setAlarm = function (successCallback, errorCallback, parameters) {
    		if (errorCallback == null) {
    			errorCallback = function () {
    			};
    		}

    		if (typeof errorCallback != "function") {
    			console.log("Background.setAlarm failure: failure parameter not a function");
    			return;
    		}

    		if (typeof successCallback != "function") {
    			console.log("Background.setAlarm failure: success callback parameter must be a function");
    			return;
    		}

    		if (!parameters || parameters.length < 1) {
            	console.log("Background.setAlarm failure: no parameters were passed");
            	return;
            }

    		exec(successCallback, errorCallback, 'Background', 'setAlarm', parameters);
    	};

    	Background.prototype.cancelAlarm = function (successCallback, errorCallback) {
    		if (errorCallback == null) {
    			errorCallback = function () {
    			};
    		}

    		if (typeof errorCallback != "function") {
    			console.log("Background.cancelAlarm failure: failure parameter not a function");
    			return;
    		}

    		if (typeof successCallback != "function") {
    			console.log("Background.cancelAlarm failure: success callback parameter must be a function");
    			return;
    		}

    		exec(successCallback, errorCallback, 'Background', 'cancelAlarm', []);
    	};

    	Background.prototype.getStartupTimestamp = function (successCallback, errorCallback) {
    		if (errorCallback == null) {
    			errorCallback = function () {
    			};
    		}
    		if (typeof errorCallback != "function") {
    			console.log("Background.getStartupTimestamp failure: failure parameter not a function");
    			return;
    		}
    		if (typeof successCallback != "function") {
    			console.log("Background.getStartupTimestamp failure: success callback parameter must be a function");
    			return;
    		}
    		exec(successCallback, errorCallback, 'Background', 'getStartupTimestamp', []);
    	}
    	var background = new Background();
    	module.exports = background;
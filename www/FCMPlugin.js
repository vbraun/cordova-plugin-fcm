
var exec = require('cordova/exec');

exports.subscribeToTopic = function(success, error) {
    exec(success, error, "FCMPlugin", "subscribeToTopic", []);
};

exports.getToken = function(success, error) {
    exec(success, error, "FCMPlugin", "getToken", []);
};

exports.unsubscribeFromTopic = function( topic, success, error ){
    exec(success, error, "FCMPlugin", 'unsubscribeFromTopic', [topic]);
}


// NOTIFICATION CALLBACK //
exports.onNotification = function( callback, success, error ){
    FCMPlugin.onNotificationReceived = callback;
    exec(success, error, "FCMPlugin", 'onNotification',[]);
}

// DEFAULT NOTIFICATION CALLBACK //
exports.onNotificationReceived = function(payload){
    console.log("Received push notification")
    console.log(payload)
}


/**
 * FIRE READY
 *
 * Notifications are delivered after the plugin receives the ready command
 */
exec(
    function(result) { console.log("FCMPlugin Ready OK") }, 
    function(result) { console.log("FCMPlugin Ready ERROR") }, 
    "FCMPlugin", 'ready', []);





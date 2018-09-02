var exec = require('cordova/exec');

exports.startScreen = function(arg0, success, error) {
    exec(success, error, "screen-plugin", "startScreen", [arg0]);
};

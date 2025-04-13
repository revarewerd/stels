var alertFallback = false;
if (typeof console === "undefined" || typeof console.log === "undefined") {
    console = {};
    if (alertFallback) {
        console.log = function (msg) {
            alert(msg);
        };
    } else {
        console.log = function () {
        };
    }
}

window.onerror = function (message, file, line, error) {

    var toSend = {
        message: message.substring(0, 1000),
        file: file,
        line: line,
        error: error
    };

    Ext.Ajax.request({
        url: 'EDS/senderror',
        method: 'post',
        timeout: '3000',
        params: {
            message: JSON.stringify(toSend)
        },
        defaultHeaders: {
            'Content-Type': 'application/json; charset=utf-8'
        },
        success: function (response, opts) {
            console.log("response=", response);
        },
        failure: function (response, opts) {
            console.log("respfailture=", response);
        }
    });
};

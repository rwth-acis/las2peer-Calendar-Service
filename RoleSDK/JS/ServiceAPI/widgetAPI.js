
/*
 * requires moduleHelper.js
 * requires JQuery 2.1.1
 * requires http://open-app.googlecode.com/files/openapp.js
 * requires http://dbis.rwth-aachen.de/gadgets/iwc/lib/iwc.js
 */

(function() {
  this.module("i5", function() {
    return this.module("las2peer", function() {
      return this.module("jsAPI", function() {

        /*
          Simple manager for inter widget communication
          The callback given in the constructor is executed, whenever an intent is received
         */
        return this.IWCManager = (function() {
          function IWCManager(callback) {
            this.callback = callback;
            this.iwcClient = new iwc.Client();
            this.iwcClient.connect(this.callback);
          }

          IWCManager.prototype.sendIntent = function(action, data, global) {
            var intent;
            if (global == null) {
              global = true;
            }
            intent = {
              "component": "",
              "data": data,
              "dataType": "text/xml",
              "action": action,
              "categories": ["", ""],
              "flags": [global ? "PUBLISH_GLOBAL" : void 0],
              "extras": {}
            };
            this.publish(intent);
          };

          IWCManager.prototype.publish = function(intent) {
            if (iwc.util.validateIntent(intent)) {
              this.iwcClient.publish(intent);
            }
          };

          return IWCManager;

        })();
      });
    });
  });

}).call(this);

//# sourceMappingURL=widgetAPI.js.map
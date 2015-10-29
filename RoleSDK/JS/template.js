var api = i5.las2peer.jsAPI;

/*
  Check explicitly if gadgets is known, i.e. script is executed in widget environment.
  Allows compatibility as a Web page.
 */

if (typeof gadgets !== "undefined" && gadgets !== null) {
  iwcCallback = function (intent) { };
  iwcManager = new api.IWCManager(iwcCallback);
}


var login = new api.Login(api.LoginTypes.HTTP_BASIC);
login.setUserAndPassword("alice", "pwalice");






/*
  Init RequestSender object with uri and login data.
 */

var requestSender = new api.RequestSender("http://127.0.0.1:8080/example", login);



$(document).ready(function () {
  init();
});

var init = function () {
  alert("Testing!");
  var request = new api.Request("get", "getNumber/", "", function(data) {
  alert("success");
  });
  requestSender.sendRequestObj(request);
}
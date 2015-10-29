/**
 * Simple example showing usage of serviceAPI and widgetAPI.
 * Add multpile widgets to your space.
 * Each one will have a random color.
 * By clicking on a widget it will send an intent to all other widgets.
 * Some widgets will then change their color.
 */
var api = i5.las2peer.jsAPI;

/*
  Check explicitly if gadgets is known, i.e. script is executed in widget environment.
  Allows compatibility as a Web page.
 */

if (typeof gadgets !== "undefined" && gadgets !== null) {
  iwcCallback = function (intent) {
    //listen to intent, wait for action
    if (intent.action === "WIDGET_CLICKED") {
      //react on intent data
      if (intent.data == myId%2)
        setRandomColor();
    }

    };
  iwcManager = new api.IWCManager(iwcCallback);
}

//we don't need to login
var login = new api.Login(api.LoginTypes.NONE);

//get a simple id [1,2]
var myId = Math.floor((Math.random() * 2) + 1);



/*
  Init RequestSender object with uri and login data.
 */

var requestSender = new api.RequestSender("http://www.random.org", login);



$(document).ready(function () {
  init();
});

var init = function () {
  setRandomColor();
  //create click event
  if (typeof gadgets !== "undefined" && gadgets !== null) {    
    $("html").click(function (e) {
      //send intent to other widgets with own id modulo 2
      iwcManager.sendIntent("WIDGET_CLICKED", (myId % 2) + "");
    });
  }
};

var setRandomColor = function () {

  var min = 0;
  var max = 255;

  //create a simple request to fetch 3 random numbers and set the background to a random color
  var request = new api.Request("get", "integers/?num=3&min=" + min + "&max=" + max + "&col=1&base=10&format=plain&rnd=new", "", function (data) {

    //data contains the server response
    var parts = data.trim().split("\n");
    var r, g, b;

    r = parts[0];
    g = parts[1];
    b = parts[2];

    //set background to a random color
    $("html").css("background-color", "rgb(" + r + "," + g + "," + b + ")");
  });
  //send request
  requestSender.sendRequestObj(request);

};
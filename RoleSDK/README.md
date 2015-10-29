Web-Client-Template-Project
=======================

A simple template to create web based clients (ROLE widget support) for REST services (Las2peer and others).

This project contains a simple template and libraries for easy client development.
Because REST is a quite broad and simple API standard, this template is not restricted to Las2peer services (in fact the example uses another external service).
The included libraries provide a simple way to manage authentication (currently only BASIC auth is supported) and send requests (synchronously and asynchronously).

Among a template for a web based client, there is also a template for a ROLE widget 
(Project:  https://github.com/rwth-acis/ROLE-SDK Wiki: https://github.com/ROLE/ROLE/wiki/About-the-project).
A library provides very simple usage of inter widget communication.

This project includes a small example both as a web page and as a widget, where a request is used to obtain random numbers to change the background color to a random color.
It also demonstrates the usage of inter widget communication.

For a more elaborate example you can look at https://github.com/rwth-acis/LAS2peer-Microblog-Service

Getting Started
=======================

First of all you need a local file server or upload the provided files to a server.
For widgets to work the files must be accessible externally.

If you use windows a very simple way to accomplish this is using Mongoose https://code.google.com/p/mongoose/
Create a mongoose.conf in the same directory as the mongoose.exe and point in it to the directory with the template files:

document_root C:\Visual Studio 2013\WebSites\Web-Client-Template-Project
listening_ports  8080

Opening your browser and navigating to http://localhost:8080/example.html will show you the example web page.


The hard part is to get the widgets running.
Go to http://role-sandbox.eu/ sign in and create a space with a nice name.

Now you will have to edit example-widget.xml:
All your scripts and other files must be accessible from the ROLE servers (use file server like Mongoose or upload the files somewhere).
So replace all $yourServerURL$ with a valid absolute URL (if you use a local fileserver don't use localhost, but your IP).

Now you can add your widget to your role space. Back on http://role-sandbox.eu/spaces/mynicename add a new widget by clicking on the + button on the left.
Enter the absolute address to your example-widget.xml in the prompt. And accept.
Now hopefully the example widget should appear in your space showing a colored square.
You can add the widget multiple times and each widget should have a different color.
By clicking on a widget some widgets will change its color.

Sending Requests
=======================

To send a request you need a Login object.

	var login = new i5.las2peer.jsAPI.Login(i5.las2peer.jsAPI.LoginTypes.HTTP_BASIC);
	login.setUserAndPassword("myname", "mypass");

Currently only HTTP BASIC auth is supported.
So you have to provide user name and password to the object.
If the service does not require authentication simply use

	var login = new i5.las2peer.jsAPI.Login(i5.las2peer.jsAPI.LoginTypes.NONE);

Having the Login object you can create a RequestSender object, which will handle all request.

	var requestSender = new i5.las2peer.jsAPI.RequestSender("http://myaddress", login);

The first parameter is the URL pointing to your service. All request URIs later will be appended to this URL,
so you won't have to write "http://myaddress/dostuff1", "http://myaddress/dostuff2" ... every time, but only "dostuff1", "dostuff2"...
If you have special requirements, like a specific MIME type for the content or want to add an accept header, you can pass a 
jQuery ajax object as a 3rd parameter, which is then used as a basis (only the URI, method, data and callbacks are overwritten).
To send a simple request just use

	requestSender.sendRequest(method,URI,content,callback,errorCallback);

Method is the HTTP-Method, like "get" or "post".
URI is the URI of your request, which is appended to the service address you provided in the constructor.
callback is called with the response data, when the request was successful.
errorCallback is optional and called with an error message, when there was an error.

You can also create a Request object

	var request = new i5.las2peer.jsAPI.Request(method, URI, content, callback, errorCallback);

and pass it to 

	requestSender.sendRequestObj(request);

It is also possible to send multiple requests at once.
To do so create an array of Request objects.

To send and process the requests synchronously use sendRequestsSync: 

	requestSender.sendRequestsSync(requestArray, callback);

sendRequestsAsync does the same asynchronously. 


Using Inter Widget Communication
=======================

First of all, you need to include 
http://open-app.googlecode.com/files/openapp.js
http://dbis.rwth-aachen.de/gadgets/iwc/lib/iwc.js

and be in a widget environment (e.g. ROLE Sandbox).
Using widgetAPI.js you first define a callback like

	var iwcCallback = function (intent) {
		if (intent.action === "WIDGET_CLICKED") 
			alert(intent.data);
		
	};

Which is called whenever a widget receives a message (intent) from another widget.
You should check for the action of the intent before using the data (i.e. only react to intents that are meant for this widget).
Then you create a IWCManager object and pass this callback to the constructor.

	var iwcManager = new i5.las2peer.jsAPI.IWCManager(iwcCallback);

Now your widget can receive messages from other widgets.
To send (broadcast) an own message (intent) simply use:

	iwcManager.sendIntent("MY_ACTION", "MY_DATA_STRING);

If you need more control over the intent, use

	iwcManager.publish(intent);

and pass the intent object as a parameter.
You can find more information on inter widget communication on https://github.com/ROLE/ROLE/wiki




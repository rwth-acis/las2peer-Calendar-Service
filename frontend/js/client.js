/*
Copyright (c) 2014 Advanced Community Information Systems (ACIS) Group,
Chair of Computer Science 5 (Databases & Information Systems), RWTH Aachen University, Germany
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of the ACIS Group nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

/**
* Instantiates a new TemplateServiceClient, given its endpoint URL.
*/

function TemplateServiceClient(endpointUrl) {
	// care for trailing slash in endpoint URL
	if(endpointUrl.endsWith("/")) {
		this._serviceEndpoint = endpointUrl.substr(0,endpointUrl.length-1);
	} else {
		this._serviceEndpoint = endpointUrl;
	}
};

function decorateMonth(data) {
	data = JSON.parse(data);
	var html='<div class="dailyEntries">';
	for(var i = 1; i<32; i++){	
		$('#button'+i).css('background-color', 'white');
		$('#button'+i).css('color', 'black');
	}

	for(var i = 0; i<data.length; i++){
		$('#button'+data[i].eday).css('background-color', '#CC071E');
		$('#button'+data[i].eday).css('color', '#FFF');
	}
	
	
	html+= "</div>";

	document.getElementById('daily').innerHTML = html;
	
	
}

function renderDay(data) {
	data = JSON.parse(data);
	var html='<div class="dailyEntries">'
	$('#comment-list').html("");
	
	for(var i = 0; i<data.length; i++){
		var b = i+1;
		html+='<div id="entry-'+data[i].entry_id+'" class="entry" style="margin-top:10px">';
		html+= "<b>" + b + ". Entry: " + "</b>" + data[i].title + " - " + data[i].description + ". Starts at  " + data[i].shour +
			  ":" + data[i].sminute + " and ends at " + data[i].ehour + ":" + data[i].eminute;
		html+= ". <button class=\"btn btn-default\" style=\"float:right;\" onClick=update('" + data[i].comments + "') data-target=\"#comments\" data-toggle=\"modal\"><span class=\"glyphicon glyphicon-th-list\"></span> Comments</button>";
		
		html+= "<br><i><div id="+data[i].creator+ data[i].entry_id+ " >" + client.getName(data[i].creator, data[i].entry_id) + "</div></i>"; 
		
		
		html+='</div><br>';
		
		//add comments to window
		
	}
	
	
	html+= "</div>";

	document.getElementById('daily').innerHTML = html;
	
	
}

function showComment(id){
	$('#comment-list')[0].innerHTML = ""
	$("<comment-thread-widget id='comments' login-oidc-provider='https://api.learning-layers.eu/o/oauth2' login-oidc-token='"+window.localStorage["access_token"]+"' thread='"+id+"'></comment-thread-widget>").appendTo('#comment-list');
}

function showUserInformation(){
	if(!client.isAnonymous()){
		$('#user-information')[0].innerHTML = ""
		$("<las2peer-user-widget login-oidc-provider='https://api.learning-layers.eu/o/oauth2' login-oidc-token='"+window.localStorage["access_token"]+"'></las2peer-user-widget>").appendTo('#user-information');
	}
}


/**
 * A function to retrieve the calendar entries on that given day 
 */
TemplateServiceClient.prototype.getDay = function(year, month, day, successCallback, errorCallback) {
	this.sendRequest("GET", 
			"calendar/getDay/" + year + "/" + month + "/" + day,
			"",
			"application/json",
			{},
			function(data){
				renderDay(data);
			},
			errorCallback
	);
}

/**
 * A function to retrieve the calendar entries on that given day 
 */
TemplateServiceClient.prototype.getMonth = function(year, month, successCallback, errorCallback) {
	this.sendRequest("GET", 
			"calendar/getMonth/" + year + "/" + month,
			"",
			"application/json",
			{},
			function(data){
				decorateMonth(data);
			},
			errorCallback
	);
}

/**
 * A function to create an entry on a certain date
 */
TemplateServiceClient.prototype.create = function(title, description, groupid, successCallback, errorCallback) {
	var content = {};
	content["title"] = title;
	content["description"] = description;
	content['groupID'] = groupid;
	console.log(content);
	this.sendRequest("POST", 
			"calendar/create/",
			JSON.stringify(content),
			"application/json",
			{},
			successCallback,
			errorCallback
	);
}

TemplateServiceClient.prototype.setStart = function(id, year, month, day, shour, sminute, successCallback, errorCallback) {
	var content = {};
	content["year"] = year;
	content["month"] = month;
	content["day"] = day;
	content["hour"] = shour;
	content["minute"] = sminute;
	this.sendRequest("put", 
			"calendar/setStart/" + id ,
			JSON.stringify(content),
			"application/json",
			{},
			successCallback,
			errorCallback
	);
}

TemplateServiceClient.prototype.setEnd = function(id, year, month, day, ehour, eminute, successCallback, errorCallback) {
	var content = {};
	content["year"] = year;
	content["month"] = month;
	content["day"] = day;
	content["hour"] = ehour;
	content["minute"] = eminute;
	this.sendRequest("put", 
			"calendar/setEnd/" + id,
			JSON.stringify(content),
			"application/json",
			{},
			successCallback,
			errorCallback
	);
}

TemplateServiceClient.prototype.createRegular = function(title, description, comments , group, sYear, sMonth, sDay, sHour, sMinute, eYear, eMonth, eDay, eHour, eMinute, interval, number, successCallback, errorCallback) {
	var content = {};
	content["title"] = title;
	content["description"] = description;
	content["comments"] = comments;

	content["syear"] = sYear;
	content["smonth"] = sMonth;
	content["sday"] = sDay;
	content["shour"] = sHour;
	content["sminute"] = sMinute;
	content["eyear"] = eYear;
	content["emonth"] = eMonth;
	content["eday"] = eDay;
	content["ehour"] = eHour;
	content["eminute"] = eMinute;

	content["interval"] = interval;
	content["number"] = number;
	this.sendRequest("post", 
			"calendar/createRegular",
			JSON.stringify(content),
			"application/json",
			{},
			function(data){
				alert(data);
			},
			errorCallback
	);
}

TemplateServiceClient.prototype.createComment = function(id, comment, errorCallback){
	this.sendRequest("post", 
			"calendar/createComment/" + id,
			JSON.stringify(comment),
			"application/json",
			{},
			function(data){
			 //alert(data);
			},
			errorCallback
	);	
}

TemplateServiceClient.prototype.deleteComment = function(id, errorCallback){
	this.sendRequest("delete", 
			"calendar/deleteComment/" + id,
			"",
			"application/json",
			{},
			function(data){
			//alert(data);
			},
			errorCallback
	);	
}

TemplateServiceClient.prototype.getName = function(id){
	this.sendRequest("get", 
			"calendar/name/" + id,
			"",
			"application/json",
			{},
			function(data){
			document.getElementById(id).innerHTML = "&emsp;Created by: "+data;
			},
			function(){
				
			}
	);	
}

TemplateServiceClient.prototype.getName = function(id, id2){
	this.sendRequest("get", 
			"calendar/name/" + id,
			"",
			"application/json",
			{},
			function(data){
			document.getElementById(""+id+id2).innerHTML = "&emsp;Created by: "+data;
			},
			function(){
				
			}
	);	
}

TemplateServiceClient.prototype.getGroups = function(){
	this.sendRequest("get", 
			"contactservice/groups",
			"",
			"application/json",
			{},
			function(data){
				var select = document.getElementById('groupID');
				for (var key in data) {
				    var opt = document.createElement('option');
					console.log(data[key]);
				    opt.value = key;
				    opt.innerHTML = data[key];
				    select.appendChild(opt);
				}
				if (data.length>0){
					$('#group').val(data[key]);
				}
			},
			function(){
				console.log("Error");
			}
	);	
}




/**
* sends an AJAX request to a resource.
* Parameters:
*   - method: the HTTP method used
*	- relativePath: the path relative to the client's endpoint URL
*   - content: the content to be sent in the HTTP request's body
*   - mime: the MIME-type of the content
*   - customHeaders: a JSON string with additional header parameters to be sent
*   - successCallback: a callback function invoked in case the request succeeded. Expects two parameters "data" and "type", where "data" represents the content of the response and "type" describes the MIME-type of the response
*   - errorCallback: a callback function invoked in case the request failed. Expects one parameter "error" representing the error occurred.
*   
*/
TemplateServiceClient.prototype.sendRequest = function(method, relativePath, content, mime, customHeaders, successCallback, errorCallback) {
	var mtype = "text/plain; charset=UTF-8"
	if(mime !== 'undefined') {
		mtype = mime;
	}
	
	var rurl = this._serviceEndpoint + "/" + relativePath;
	
	if(!this.isAnonymous()){
		console.log("Authenticated request");
		if(rurl.indexOf("\?") > 0){	
			rurl += "&access_token=" + window.localStorage["access_token"];
		} else {
			rurl += "?access_token=" + window.localStorage["access_token"];
		}
	} else {
		console.log("Anonymous request... ");
	}
	
	var ajaxObj = {
		url: rurl,
		type: method.toUpperCase(),
		data: content,
		contentType: mtype,
		crossDomain: true,
		headers: {},
		
		error: function (xhr, errorType, error) {
			console.log(error);
			var errorText = error;
			if (xhr.responseText != null && xhr.responseText.trim().length > 0) {
				errorText = xhr.responseText;
			}
			errorCallback(errorText);
		},
		success: function (data, status, xhr) {
			var type = xhr.getResponseHeader("Content-Type");
			successCallback(data, type);
		},
	};
	
	if (customHeaders !== undefined && customHeaders !== null) {
		$.extend(ajaxObj.headers, customHeaders);
	}
	
	$.ajax(ajaxObj);
};

/**
* determines if user is authenticated via OpenID Connect or not.
*/
TemplateServiceClient.prototype.isAnonymous = function(){
	if (oidc_userinfo !== undefined){
		return false;
	} else {
		return true;
	}
};

/**
* Convenience function to check if a String ends with a given suffix.
*/
String.prototype.endsWith = function(suffix) {
	return this.indexOf(suffix, this.length - suffix.length) !== -1;
};

// function to get month as a number
function getMonth(month) {

	var i = month.options.selectedIndex;
	i++;
	
	return i;
}


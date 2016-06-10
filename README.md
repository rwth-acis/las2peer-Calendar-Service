![las2peer](https://github.com/rwth-acis/las2peer/blob/master/img/logo/bitmap/las2peer-logo-128x128.png)

las2peer-Calendar-Service  [![Build Status](https://travis-ci.org/rwth-acis/las2peer-Calendar-Service.svg?branch=master)](https://travis-ci.org/rwth-acis/las2peer-Calendar-Service)
=======================
This is a calendar service for the las2peer platform. Users can create entries in a common calender and also comment on them. Entries are stored long-term inside the Node storage. A frontend can be used to look at entries and create them. Furthermore, Users can import their .ics files to store them in the calendar via the frontend.

Instructions
--------

To use the service, run ''ant build'' and fetch the dependencies. If all dependencies were succesfully downloaded, run ''ant build'' with the command ''all''. Then start an instance of the service using ''start_service.bat''. Then you can use the command line or the frontend to use the service.

Features
--------
* add or delete entries 
* add or delete comments on entries
* create entries on regular intervals (monthly, yearly, etc. )
* import .ics files in the front end

In-Service Usage
--------

You can use the ''create(String title, String description)'' to create entries. A method that makes it possible to create entries regulary in certain intervals (months, quarterly, etc. ) is also provided.

![LAS2peer](https://github.com/rwth-acis/LAS2peer/blob/master/img/logo/bitmap/las2peer-logo-128x128.png)

LAS2peer-Calendar-Service
=======================
This is a calendar service for the las2peer platform. Users can create entries in a common calender and also comment on them. Entries are stored long-term inside the Node storage. A frontend can be used to look at entries and create them.

Instructions
--------

To use the service, run ''ant build'' and fetch the dependencies. If all dependencies were succesfully downloaded, run ''ant build'' with the command ''all''. Then start an instance of the service using ''start_service.bat''. Then you can use the command line or the frontend to use the service.

In-Service Usage
--------

You can use the ''create(String title, String description)'' to create entries. Methods to create weekly or monthly entries are also provided.
package org.lerchenflo.schneaggchatv3mp.network


// Account & Authentication (0-99)

val CREATEACCOUNTMESSAGE: String = "0"

val LOGINMESSAGE: String = "10"

val DELETEACCOUNTMESSAGE: String = "20"

// User Management (100-199)

val CHANGEUSERNAMEMESSAGE: String = "100"

val CHANGEPASSWORDMESSAGE: String = "110"

val SETPROFILEPICTURE: String = "120"

val SETUSERDESCRIPTION: String = "130"

val SETUSERSTATUS: String = "140"

val USERIDSYNC: String = "150"

val GETUSERBYID: String = "160"

// Friends (200-299)

val FRIENDREQUEST: String = "200"

val FRIENDACCEPT: String = "210"

val FRIENDREMOVE: String = "220"

val GETCOMMONGROUPS: String = "230"

// Messaging (300-399)

val TEXTMESSAGE: String = "300"

val PICTUREMESSAGE: String = "310"

val VOICEMESSAGE: String = "320"

val POLLMESSAGE: String = "330"

val DELETECHATMESSAGE: String = "340"

val SETMESSAGEGELESEN: String = "350"

val CHANGEMESSAGE: String = "360"

val SETALLMESSAGESINCHATGELESEN: String = "370"

// Sync & Utilities (400-499)

val MSGIDSYNC: String = "400"

val GETUSERLISTE: String = "410"

val GETALLCHANGEDMESSAGES: String = "420"

val GETMESSAGEBYID: String = "430"

val GETMESSAGESWITHOUTPICTURES: String = "440"

// Group Management (500-599)

val CREATEGROUPMESSAGE: String = "500"

val GETGROUPLISTE: String = "510"

val GETGROUPMEMBERS: String = "520"

val ADDGROUPMEMBER: String = "530"

val REMOVEGROUPMEMBER: String = "540"

val MAKEGROUPADMIN: String = "550"

val REMOVEGROUPADMIN: String = "560"

val SETGROUPPROFILEPICTURE: String = "570"

val SETGROUPNAME: String = "580"

val SETGROUPDESCRIPTION: String = "590"

val DELETEGROUP: String = "600"

val GROUPIDSYNC: String = "610"

val GETGROUPBYID: String = "620"

// Location (700-799)

val GETUSERLOCATIONS: String = "700"

val IMPORTANTLOCATIONSYNC: String = "710"

val ADDIMPORTANTLOCATION: String = "720"

val REMOVEIMPORTANTLOCATION: String = "730"

val CHANGEIMPORTANDLOCATIONDESCRIPTION: String = "740"

val GETIMPORTANTLOCATIONBYID: String = "750"

val UPDATELOCATIONRATING: String = "760"

val REMOVELOCATIONRATING: String = "770"

val SHARELOCATIONWITHUSER: String = "780"

val SETWAKEUPENABLEDWITHUSER: String = "790"

// System & Public (800-899)

val PING: String = "800"

val WAKEUPMESSAGE: String = "810"

val DDOS: String = "820"

val SETFIREBASETOKEN: String = "830"

// Games (2000-2099)

val GAME_CREATE: String = "2000"

val GAME_JOIN: String = "2010"

val GAME_GETPLAYERS: String = "2020"

val GAME_GETCONTENT: String = "2030"

// GeoGuesser (2100-2199)

val GEOGUESSER_GETHIGHSCORE: String = "2100"

val GEOGUESSER_SETHIGHSCORE: String = "2110"

// Leaderboard (2200-2299)

val CREATELEADERBOARD: String = "2200"

val GETHIGHSCORE: String = "2210"

val SETHIGHSCORE: String = "2220"

val DELETEHIGHSCORE: String = "2230"

// Bug Reports (2300-2399)

val BUGFEATURESYNC: String = "2300"

val ADDBUGFEATURE: String = "2310"

val GETBUGFEATUREBYID: String = "2320"

val DELETEBUGFEATUREBYID: String = "2330"
package org.lerchenflo.schneaggchatv3mp.network

import schneaggchatv3mp.composeapp.generated.resources.Res.string


//A paar messagetypes

const val CREATEACCOUNTMESSAGE = "0"

const val CREATEGROUPMESSAGE = "1"

const val LOGINMESSAGE = "7"

const val DELETEACCOUNTMESSAGE = "8"

//Für settings und so

const val CHANGEPASSWORDMESSAGE = "11"

const val CHANGEUSERNAMEMESSAGE = "12"


const val SETPROFILEPICTURE = "13"

const val FRIENDREQUEST = "14"

const val FRIENDACCEPT = "15"

const val FRIENDREMOVE = "16"


//Textnachrichten
const val TEXTMESSAGE = "21"

//Bilder
const val PICTUREMESSAGE = "23"

const val VOICEMESSAGE = "25"

const val POLLMESSAGE = "27"


const val MSGIDSYNC = "30"

const val GETUSERLISTE = "31"

const val GETALLCHANGEDMESSAGES = "32"

const val GETMESSAGEBYID = "33"

const val GETGROUPMEMBERS = "34"

const val REMOVEGROUPMEMBER = "35"

const val ADDGROUPMEMBER = "36"

const val MAKEGROUPADMIN = "37"

const val GETGROUPLISTE = "38"

const val SETGROUPPROFILEPICTURE = "39"

const val REMOVEGROUPADMIN = "1111" //Muss ma no ändra also alle messagetypes verschiaba

const val DELETEGROUP = "1112"

const val GROUPIDSYNC = "1113"

const val GETGROUPBYID = "1114"



const val SETGROUPDESCRIPTION = "1115"

const val SETGROUPNAME = "1116"


//Einzelne user

const val DELETECHATMESSAGE = "40"

const val GETCOMMONGROUPS = "41"

const val SETUSERDESCRIPTION = "42"

const val SETUSERSTATUS = "43"

const val USERIDSYNC = "44"

const val GETUSERBYID = "45"

//Locationzüg

const val GETUSERLOCATIONS = "50"

const val IMPORTANTLOCATIONSYNC = "51"

const val ADDIMPORTANTLOCATION = "52"

const val REMOVEIMPORTANTLOCATION = "53"

const val CHANGEIMPORTANDLOCATIONDESCRIPTION = "54"

const val GETIMPORTANTLOCATIONBYID = "56"

const val UPDATELOCATIONRATING = "57"

const val REMOVELOCATIONRATING = "58"



const val SHARELOCATIONWITHUSER = "55"



const val GETMESSAGESWITHOUTPICTURES = "99"


const val PING = "100"

const val WAKEUPMESSAGE = "101"

const val DDOS = "102"


//Spiele

const val GAME_CREATE = "4000"

const val GAME_JOIN = "4001"

const val GAME_GETPLAYERS = "4002"

const val GAME_GETCONTENT = "4003"


const val GEOGUESSER_GETHIGHSCORE = "5000"

const val GEOGUESSER_SETHIGHSCORE = "5001"


const val SETMESSAGEGELESEN = "6000"

const val CHANGEMESSAGE = "6001"



const val BUGFEATURESYNC = "7000"

const val ADDBUGFEATURE = "7001"

const val GETBUGFEATUREBYID = "7002"

const val DELETEBUGFEATUREBYID = "7003"

const val SETFIREBASETOKEN = "10000"

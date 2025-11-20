package org.lerchenflo.schneaggchatv3mp.chat.domain

import org.lerchenflo.schneaggchatv3mp.chat.data.dtos.MessageWithReadersDto

interface _SelectedChatBase {
    val id: String
    val isGroup: Boolean
    val name: String
    val profilePicture: String //TODO: Change profilepicture (what is it needed for)
    val status : String?
    val description : String?


    //Default values die ned vo da klasse üverschrieba werrand, bruchts eh nur fürd gegnerauswahl (Chatselector)
    val unreadMessageCount: Int
    val unsentMessageCount: Int
    val lastmessage: MessageWithReadersDto?

}

//A abstract class die die die werte overridet damit ma se o setza kann und ned in da andra klassen überschrieba
abstract class SelectedChat : _SelectedChatBase {
    override var unreadMessageCount: Int = 0
    override var unsentMessageCount: Int = 0
    override var lastmessage: MessageWithReadersDto? = null
}




/*
Des isch a interface selectedchat. also a base klasse, die im global view model denn gsetzt werra kann,.
es git 3 typen: Groupwithmember, User und Notselected, wenn ma kuan user usgwählt hot. So hot ma theoretisch nie null verweise,
und weniger code horror. Ma kann direkt uf id und isgroup und alles zugriefa obwohl ma ned mol woas was as genau isch,
und denn im switch case da rest macha abhängig vom typ.


 */

data class NotSelected(
    override val id: String = "",
    override val isGroup: Boolean = false,
    override val name: String = "",
    override val profilePicture: String = "",
    override val status: String = "",
    override val description: String = ""
) : SelectedChat() {

}
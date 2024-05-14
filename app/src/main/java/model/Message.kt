package model

class Message(
    var message: String,
    var sentBy: String
) {
    companion object {
        const val SENT_BY_ME = "0"
        const val SENT_BY_BOT = "1"
    }

    constructor() : this("", "")
}
/**
 *  Medicine Reminder
 *
 *  Author: Andrei Zharov
 */

definition(
        name: "Dishwasher Reminder",
        namespace: "belmass@gmail.com",
        author: "Andrei Zharov",
        description: "Set up a reminder to load a dishwasher",
        category: "Convenience",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/text_contact.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/text_contact@2x.png"
)

preferences {
    section("Choose your acceleration sensor..."){
        input "cabinet1", "capability.accelerationSensor", title: "Where?"
    }
    section("I usually load detergent at..."){
        input "time", "time", title: "Time"
    }
    section("I forget send me a notification and/or text message..."){
        input("recipients", "contact", title: "Send notifications to") {
            input "sendPush", "enum", title: "Push Notification", required: false, options: ["Yes", "No"]
            input "phone1", "phone", title: "Phone Number", required: false
        }
    }
    section("Time window (optional, defaults to plus or minus 15 minutes") {
        input "timeWindow", "decimal", title: "Minutes", required: false
    }
}

def installed() {
    initialize()
}

def updated() {
    unschedule()
    initialize()
}

def initialize() {
    def endTime = new Date(timeToday(time).time + timeWindowMsec)
    log.debug "Scheduling check at $endTime"
    schedule(endTime, scheduleCheck)
}

def scheduleCheck()  {
    log.debug "scheduleCheck"
    def t0 = new Date(now() - (2 * timeWindowMsec))
    def t1 = new Date()
    def cabinetOpened = cabinet1.eventsBetween(t0, t1).find{it.name = "acceleration" && it.value == "active"}
    log.trace "Looking for events between $t0 and $t1: $cabinetOpened"

    if (cabinetOpened) {
        log.trace "Cabinet was opened since $midnight, no notification required"
    } else {
        log.trace "Cabinet was not opened since $midnight, sending notification"
        sendMessage()
    }
}

private sendMessage() {
    def msg = "Please remember to schedule dishwasher!"
    log.info msg
    if (location.contactBookEnabled) {
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (phone1) {
            sendSms(phone1, msg)
        }
        if (sendPush == "Yes") {
            sendPush(msg)
        }
    }
}

def getTimeWindowMsec() {
    (timeWindow ?: 15) * 60000 as Long
}

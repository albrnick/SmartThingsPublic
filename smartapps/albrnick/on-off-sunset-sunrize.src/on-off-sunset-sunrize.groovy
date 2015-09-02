/**
 *  Outside Lights Nighttime
 *
 *  Copyright 2015 Nick Albright
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "On/Off sunset/sunrize",
    namespace: "albrnick",
    author: "Nick Albright",
    description: "Turn on and off the lights around night time",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section() {
        input(name: "switches", title: "Lights/Switches to turn on & off", type: "capability.switch", required: "false", multiple: "true")
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// log.debug("init. Timezone: ${location.timeZone}")

	// Schedule every day
    subscribe(location, "sunsetTime", sunsetTimeHandler)
    subscribe(location, "sunriseTime", sunriseTimeHandler)

	// Do today's stuff
    scheduleTurnOn(location.currentValue("sunsetTime"))
    scheduleTurnOff(location.currentValue("sunriseTime"))
}

def turnOn() {
	log.debug("Turning on")
	settings.switches?.on()
}

def turnOff() {
	log.debug("Turning off")
	settings.switches?.off()
}

def sunsetTimeHandler(evt) {
    //when I find out the sunset time, schedule the lights to turn on with an offset
    log.debug("Sunset Time Handler: ${evt.value}")
    scheduleTurnOn(evt.value)
}

def sunriseTimeHandler(evt) {
    //when I find out the sunset time, schedule the lights to turn on with an offset
    log.debug("Sunrise Time Handler: ${evt.value}")
    scheduleTurnOff(evt.value)
}

// Display the date/time in the current timezone
def formatTime( dateObj ) {
	return( dateObj.format("MMM d h:mm aa z", location.timeZone))
	//return( dateObj.format("yyyy/M/d h:mm aa", location.timeZone))
}

def scheduleTurnOff( sunriseString ) {
    //get the Date value for the string
    def sunriseTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunriseString)

    //calculate the offset
    def timeBeforeSunrise = new Date(sunriseTime.time - (0 * 60 * 1000))

    log.debug "Turning Off at: " + formatTime(timeBeforeSunrise) + " (sunrise is " + formatTime(sunriseTime) + ")"

    //schedule this to run one time
    runOnce(timeBeforeSunrise, turnOff)
}

def scheduleTurnOn( sunsetString ) {
    //get the Date value for the string
    def sunsetTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunsetString)

    //calculate the offset
    def timeBeforeSunset = new Date(sunsetTime.time + (0 * 60 * 1000))

    log.debug "Turning On at: " + formatTime(timeBeforeSunset) + " (sunset is " + formatTime(sunsetTime) + ")"

    //schedule this to run one time
    runOnce(timeBeforeSunset, turnOn)
}

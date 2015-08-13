/**
 *  Smart Room
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
    name: "Smart Room",
    namespace: "albrnick",
    author: "Nick Albright",
    description: "Room that has lights, presense motion sensors and entrance/exit motion sensor",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Sensors") {
        input(name: "switches", title: "Lights/Switches to turn on & off", type: "capability.switch", required: "false", multiple: "true")
        input(name: "motions_occupied", title: "Occupied Motion Sensors", type: "capability.motionSensor", required: "false", multiple: "true")
        input(name: "motions_entrance_exit", title: "Entrance/Exit Motion Sensors", type: "capability.motionSensor", required: "false", multiple: "true")
	}
    section("Preferences") {
        input(name: "no_trigger_lights", title: "Disable lights/switches being triggered?", type: "bool" )
    }
    section(title: "Advanced", hideable: true, hidden: true) {
    	input(name: "occupy_min_seconds", title: "How many seconds to wait for an occupy sensor to trigger before turning off lights? (Default 30)", type: "number",
        	required: "false")
    	input(name: "unoccupy_min_seconds", title: "How many seconds to wait for an occupy sensor to trigger before turning off lights? (Default 30)", type: "number",
        	required: "false")
    	input(name: "can_not_be_occupied", title: "Can't be occupied?  (on for hallways, stairs, etc..)", type: "bool" )      
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	state.occupied = False

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	log.debug('OMS ${settings.occupy_min_seconds}')
	log.debug('OMS ' + settings.occupy_min_seconds)

	state.occupy_min_seconds = settings.occupy_min_seconds ? settings.occupy_min_seconds : 30
	state.unoccupy_min_seconds = settings.unoccupy_min_seconds ? settings.unoccupy_min_seconds : 30

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    log.debug( 'initialize' )
    if ( settings.motions_entrance_exit ) {
        log.debug( 'Subscribing to ${settings.motions_entrance_exit}');
    	subscribe( settings.motions_entrance_exit, 'motion.active', entrance_exit_triggered_handler )
    }
    if ( settings.motions_occupied ) {
    	subscribe( settings.motions_occupied, 'motion.active', occupied_triggered_handler )
    }

}

def occupied_triggered_handler( evt ) {
	log.debug('occupied_triggerd_handler')
	if ( settings.can_be_occupied ) {  // Normal Room
    	if ( !state.occupied ) {
        	turn_on_lights()
        }
        state.occupied = True
        unschedule("exit_room")
    }
    else {	// Non occupiable room!
    	turn_on_lights()
        room.occupied = True
        runIn( state.unoccupy_min_seconds, exit_room )
    }
}


def entrance_exit_triggered_handler( evt ) {
	if (state.occupied) {	// People may be leaving!
    	log.debug( "Room ${settings.displayName} is occupied, but someone could have exited")
    	runIn( state.unoccupy_min_seconds, exit_room )
    }
    else {  // Room is unoccupied, people entering!
    	log.debug( "Room ${settings.displayName} ${settings} is unoccupied!  But someome is coming in!")
        turn_on_lights()
        log.debug( 'Running in ${settings.occupy_min_seconds}')
        runIn( state.occupy_min_seconds, exit_room )
        log.debug( 'Ran')
    }
}

def exit_room() {
	log.debug('exit_room')
	state.occupied = False
    turn_off_lights()
}

def turn_off_lights() {
	log.debug('turn_off_lights')
    if (settings.no_trigger_lights || !settings.switches) {
    	return
    }
    
	settings.switches.off()
}

def turn_on_lights() {
	log.debug('turn_on_lights')
    if (settings.no_trigger_lights || !settings.switches) {
    	return
    }

	settings.switches.on()
}

// TODO: implement event handlers
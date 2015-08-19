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
metadata {
	definition (name: "Smart Room", namespace: "albrnick", author: "Nick Albright") {
		capability "Presence Sensor"	// For Occupied
		capability "Switch"   			// For Lights
        capability "Valve"				// For acting on the lights
        capability "Thermostat Mode"	// Hack for displaying the tile
	}

	simulator {
		// TODO: define status and reply messages here
	}

	// hack
	// heat -> Lights Changable & Occupied
    // cool -> Lights Changable & UnOccupied
    // off -> Lights Not Changable & UnOccupied
    // auto -> Lights Not Changable & Occupied

	tiles {
		standardTile("thermostatMode", "device.thermostatMode", width: 2, height: 2, canChangeIcon: true ) {
			state("heat", label: "a", action: "auto", icon: "st.doors.garage.garage-closed", backgroundColor: "#ffffff", nextState: "auto")
			state("cool", label: "${device.label}", action: "off", icon: "st.doors.garage.garage-open", backgroundColor: "#ffffff", nextState: "off", defaultState: true)
			state("off", label: "c", action: "cool", icon: "st.doors.garage.garage-open", backgroundColor: "#bbbbbb", nextState: "cool")
			state("auto", label: "d", action: "heat", icon: "st.doors.garage.garage-closed", backgroundColor: "#bbbbbb", nextState: "heat")
		}
		standardTile("state", "device.state", width: 2, height: 2, canChangeIcon: true ) {
			state("on", label: 'On', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821", nextState: "off")
			state("off", label: 'Off', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "on")
        }
        
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state("default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh")
		}
		main "thermostatMode"
		details(["thermostatMode", "state", "refresh"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "xParsing '${description}'"
	// TODO: handle 'presence' attribute
	// TODO: handle 'switch' attribute

}

def poll() {
	log.debug('polled!')
}

def set_state( occupied, no_trigger_lights ) {
	log.debug("${device.label} set_state")
    
    def s = null
	if (no_trigger_lights) {
    	if ( occupied ) {
        	s = 'auto'
        } else {
        	s = 'off'
        }        	
    } else {	// Triggerable lights!
    	if ( occupied ) {
        	s = 'heat'
        } else {
        	s = 'cool'
        }        	
    }
    if (s) {
      	sendEvent( name: 'thermostatMode', value: s, isStateChange: true  )
    }
    log.debug("${device.label} set state to ${s}")
}

def tell_parent_auto_lights() {
	log.debug('allowing lights 2')
    // Seems to fix a bug where the parent doesn't get like the first event 
    log.debug("${parent.state.occupied}")
    sendEvent(name: 'state', value: 'on')
}

def tell_parent_no_auto_lights() {
	log.debug('disallowing lights 2')
    // Seems to fix a bug where the parent doesn't get like the first event 
    log.debug("${parent.state.occupied}")
	sendEvent(name: 'state', value: 'off')
}



// handle commands
def on() {
	log.debug("on")

    log.debug("state ${state}")
    tell_parent_auto_lights()
    
}
def off() { 
	log.debug("off..2")


    tell_parent_no_auto_lights()

	sendEvent(name: 'thermostatMode', value: 'off', isStateChange: true)

}

def auto() { 
	log.debug('auto..2')
    tell_parent_no_auto_lights()

	sendEvent(name: 'thermostatMode', value: 'auto', isStateChange: true)

}

def heat() { 
	log.debug('heat..2')
    tell_parent_auto_lights()

	sendEvent(name: 'thermostatMode', value: 'heat', isStateChange: true)
    

}
def cool() { 
	log.debug('cool..2')
    tell_parent_auto_lights()
    
	sendEvent(name: 'thermostatMode', value: 'cool', isStateChange: true)
    
}




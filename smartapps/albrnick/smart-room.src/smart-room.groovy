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
    description: "A Room that automatically turns on lights/switches based on when people enter/leave.  It uses a combination of occupied motion sensors and edge motion sensors to determine when people are there and when they enter/exit",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Sensors") {
        input(name: "switches", title: "Lights/Switches to turn on & off", type: "capability.switch", required: "false", multiple: "true")
        input(name: "motions_occupied", title: "Occupied Motion Sensors", type: "capability.motionSensor", required: "false", multiple: "true")
        input(name: "motions_edge", title: "Edge Motion Sensors", type: "capability.motionSensor", required: "false", multiple: "true")
	}
    section("Preferences") {
        input(name: "no_trigger_lights", title: "Disable lights/switches being triggered?", type: "bool" )
    }
    section(title: "Advanced", hideable: true, hidden: true) {
       	input(name: "create_device", title: "Create a device to see room status and easily Disable lights/switches being triggered.", type: "bool" )      
    	input(name: "occupy_min_seconds", title: "How many seconds to wait for an occupy sensor to trigger before turning off lights? (Default 30)", type: "number",
        	required: "false")
    	input(name: "unoccupy_min_seconds", title: "How many seconds to wait for an occupy sensor to trigger before turning off lights? (Default 30)", type: "number",
        	required: "false")
    	input(name: "can_not_be_occupied", title: "Can't be occupied?  (on for hallways, stairs, etc..)", type: "bool" )      
	}        
    section(title: 'Super Advanced', hideable: true, hidden: true) {
	   	input(name: "do_motion_motion_trigger", title: "Create hook for Smart Room - Motion/Motion/Trigger App", type: "bool" )      
	   	input(name: "do_occupied_motion_trigger", title: "Create hook for Smart Room - Occupied/Motion/Trigger App", type: "bool" )              
    }

}

def installed() {
	Log("Installed with settings: ${settings}")

	state.occupied = false
    // state.app_device = null

	initialize()
}

def updated() {
	Log("Updated with settings: ${settings}")
    //! Make sure child device is updated with new state!

	state.occupy_min_seconds = settings.occupy_min_seconds ? settings.occupy_min_seconds : 30
	state.unoccupy_min_seconds = settings.unoccupy_min_seconds ? settings.unoccupy_min_seconds : 30
    state.no_trigger_lights = settings.no_trigger_lights

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    Log( 'initialize' )
    if ( settings.motions_edge ) {
        Log( "Subscribing to ${settings.motions_edge}");
    	subscribe( settings.motions_edge, 'motion.active', edge_triggered_handler )
    }
    if ( settings.motions_occupied ) {
    	subscribe( settings.motions_occupied, 'motion.active', occupied_triggered_handler )
    }
	// Doing in a device now - 
    subscribe( app, appTouch )
    
    
	state.app_device_id = getDeviceID('Smart Room', 'virtual device')
	def app_device = getChildDevice( state.app_device_id )

    if (settings.create_device && !app_device) {	// Create if needed
    	Log( "Adding Device" )
    	app_device = addChildDevice('albrnick', 'Smart Room', state.app_device_id, null, ['name': app.label])
    }

	if (!settings.create_device && app_device) {   	// Delete if needed
    	Log( "Deleting Device" )
   		deleteChildDevice( app_device.deviceNetworkId ) 
        app_device = null
    }
    if ( app_device ) {
    	Log("subscribing ${app_device}")
    	subscribe( app_device, 'state.on', app_device_on )
    	subscribe( app_device, 'state.off', app_device_off )
        app_device.poll()
        app_device.set_state( state.occupied, state.no_trigger_lights )
    	Log("set state for ${app_device}")

	}    
}

def set_app_device_state() {
	Log("set_app_device_state")
	if (settings.create_device) {
    	Log("trying to get child device ${state.app_device_id}")
    	def app_device = getChildDevice( state.app_device_id )
        if (app_device) {
        	Log("Setting App Device State: ${state.occupied} ${state.no_trigger_lights}")
        	app_device.set_state( state.occupied, state.no_trigger_lights )
        } else {
        	log.debug("ERROR!  Should have an app_device but don't!")
        }
    }	
}

def app_device_on( evt ) {
    log.debug("pre 2 ${state.no_trigger_lights}")
    state.no_trigger_lights = false
    log.debug("post 2 ${state.no_trigger_lights}")
}

def app_device_off( evt ) {
    log.debug("pre 2 ${state.no_trigger_lights}")
    state.no_trigger_lights = true
    log.debug("post 2 ${state.no_trigger_lights}")
}


def app_device_handler( evt ){
	Log("Got all device evt: ${evt}")
}

def uninstalled() {
	unsubscribe()
	unschedule()
	def app_device = getChildDevice( settings.app_device_id )
    if ( app_device ) {
    	Log( "Deleting Device due to uninstall" )
   		deleteChildDevice( app_device.deviceNetworkId )     
    }

}	

def getDeviceID( type, device_id ) {
	return([ app.id, type, device_id ].join('|'))
}

def appTouch( evt ) {
	Log( "appTouch: ${evt}" )
    Log( "app: ${app}")
    Log( "App state: ${state}")
    
    
}

//! Make this work!!
def set_occupied( value ) {
	state.occupied = value
    set_app_device_state()
}

def occupied_triggered_handler( evt ) {
	Log("occupied_triggerd_handler. state: ${state}")
	if ( !settings.can_not_be_occupied ) {  // Normal Room - Can be occupied
    	if ( !state.occupied ) {
        	Log('Turning on lights due to not previous occupied')
        	turn_on_lights()

			Log('Setting to occupied!')
    	    state.occupied = true
            set_app_device_state()
		}
        unschedule("exit_room")
    }
    else {	// Non occupiable room!
    	turn_on_lights()
        state.occupied = true
        set_app_device_state()
        runIn( state.unoccupy_min_seconds, exit_room )
    }
}


def edge_triggered_handler( evt ) {
	if (state.occupied) {	// People may be leaving!
    	Log("Is occupied, but someone could have exited")
    	runIn( state.unoccupy_min_seconds, exit_room )
    }
    else {  // Room is unoccupied, people entering!
    	Log("Is unoccupied!  But someome is coming in!")
        turn_on_lights()
        Log("Running in ${settings.occupy_min_seconds}")
        runIn( state.occupy_min_seconds, exit_room )
        Log('Ran')
    }
}

def exit_room() {
	Log('exit_room. setting occupied to false')    
	state.occupied = false
    set_app_device_state()
    
    turn_off_lights()
}

def turn_off_lights() {
	Log('turn_off_lights')
    if (state.no_trigger_lights) {
    	Log('skipping due to no trigger lights')
    	return
    }
    
	settings.switches?.off()
}

def turn_on_lights() {
	Log('turn_on_lights')
    if (state.no_trigger_lights) {
    	Log('skipping due to no trigger lights')
    	return
    }
    if (state.occupied) {
    	Log('skipping due to occupied!')
    }

	settings.switches?.on()
}

def Log( message ) {
	log.debug("${app.label}: ${message}")
}

// TODO: implement event handlers

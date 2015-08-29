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
    	input(name: "occupy_min_seconds", title: "How many seconds to wait for an occupy sensor to trigger before turning off lights? (Default 30)", type: "number",
        	required: "false")
    	input(name: "unoccupy_min_seconds", title: "How many seconds to wait for an occupy sensor to trigger before turning off lights? (Default 30)", type: "number",
        	required: "false")
    	input(name: "can_not_be_occupied", title: "Can't be occupied?  (on for hallways, stairs, etc..)", type: "bool" )      
       	// input(name: "create_device", title: "Create a device to see room status and easily Disable lights/switches being triggered.", type: "bool" )      
	}        

}

def installed() {
	Log("Installed with settings: ${settings}")

	state.occupied = false
    state.allow_exit = true		// Allow exit to happen.  Fixes bug where unschedule doesn't seem to work
    // state.app_device = null

	initialize()
}

def updated() {
	Log("Updated with settings: ${settings}")
    //! Make sure child device is updated with new state!

	state.occupy_min_seconds = settings.occupy_min_seconds ? settings.occupy_min_seconds : 30
	state.unoccupy_min_seconds = settings.unoccupy_min_seconds ? settings.unoccupy_min_seconds : 30
    state.no_trigger_lights = settings.no_trigger_lights
    // state.create_device = settings.create_device
    state.create_device = true

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
        subscribe( settings.motions_occupied, 'motion.inactive', occupied_inactive_handler )
    }
    subscribe( app, appTouch )
    
    state.waiting_for_motions = []
    
	state.app_device_id = getDeviceID('Smart Room', 'virtual device')
	def app_device = getChildDevice( state.app_device_id )

    if (state.create_device && !app_device) {	// Create if needed
    	Log( "Adding Device" )
    	app_device = addChildDevice('albrnick', 'Smart Room', state.app_device_id, null, ['name': app.label])
    }

	if (!state.create_device && app_device) {   	// Delete if needed
    	Log( "Deleting Device" )
   		deleteChildDevice( app_device.deviceNetworkId ) 
        app_device = null
    }
    if ( app_device ) {
    	Log("subscribing ${app_device}")
        // The app_device turning on/off will enable/disable lights going on/off automatically.
    	subscribe( app_device, 'state.on', app_device_on )
    	subscribe( app_device, 'state.off', app_device_off )
        // The app_device activating will simulate an edge_trigger/lights on
        subscribe( app_device, 'motion.active', edge_triggered_handler )	// 
        
        // Custom event to allow the device to turn on/off the room lights
        subscribe( app_device, 'lightsOn', turn_lights_on_handler )
        subscribe( app_device, 'lightsOff', turn_lights_off_handler )
        
        app_device.set_state( state.occupied, state.no_trigger_lights )
    	Log("set state for ${app_device}")

	}    
}

def set_app_device_state() {
	Log("set_app_device_state")
    
	if (state.create_device) {
    	Log("trying to get child device ${state.app_device_id}")
    	def app_device = getChildDevice( state.app_device_id )
        
        if (app_device) {
        	Log("Setting App Device State. Occupied: ${state.occupied} No_trigger_lights: ${state.no_trigger_lights}")
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
	def app_device = getChildDevice( state.app_device_id )
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


def occupied_inactive_handler( evt ) {
	Log("occupied_inactive_handler: ${evt.displayName}")
    Log("occupied_inactive_handler: PRE current list: ${state.waiting_for_motions}")

	state.waiting_for_motions.remove( evt.displayName )
    Log("occupied_inactive_handler: post current list: ${state.waiting_for_motions}")

}


def occupied_triggered_handler( evt ) {
	Log("occupied_triggerd_handler: ${evt.displayName} can_no_be_occupied: ${settings.can_not_be_occupied}")
   	Log("occupied_triggered_handler: PRE current list: ${state.waiting_for_motions}")


	if ( !settings.can_not_be_occupied ) {  // Normal Room - Can be occupied
    	Log('normal room')
    	if ( !state.occupied ) {
        	Log('Turning on lights due to not previous occupied')
        	turn_on_lights()

			Log('Setting to occupied!')
    	    state.occupied = true
            set_app_device_state()
		}
        Log('Unscheduling "exit_room"')
        unschedule('exit_room')
        disallow_exit()
    }
    else {	// Non occupiable room!
    	Log('unoccupiable normal room')
    	turn_on_lights()
        state.occupied = true
        set_app_device_state()
        Log("Scheduling 'exit_room' in ${state.unoccupy_min_seconds}")
        runIn( state.unoccupy_min_seconds, exit_room )
        allow_exit()
    }
    
    addToWaitingForMotions( evt.displayName )
   	Log("occupied_triggered_handler: post current list: ${state.waiting_for_motions}")

}

def addToWaitingForMotions( name ) {
	if ( !state.waiting_for_motions.contains( name )) {
		state.waiting_for_motions.add( name )    
    }
}


def edge_triggered_handler( evt ) {
	Log( "Edge Trigger ${evt.device}")
	if (state.occupied) {	// People may be leaving!
    	Log("Is occupied, but someone could have exited")
        Log("Trying 'exit_room' in ${state.unoccupy_min_seconds}")

    	runIn( state.unoccupy_min_seconds, exit_room )
        allow_exit()
    }
    else {  // Room is unoccupied, people entering!
    	Log("Is unoccupied!  But someome is coming in!")
        turn_on_lights()
        Log("Running 'exit_room' in ${state.occupy_min_seconds}")
        runIn( state.occupy_min_seconds, exit_room )
        allow_exit()
        Log('Ran')
    }
}

def turn_lights_on_handler( evt ) {
	turn_lights_on()
}

def turn_lights_off_handler( evt ) {
	turn_lights_off()
}


def exit_room() {
	Log('exit_room')    
   	Log("exit_room. allow_exit: ${state.allow_exit}  waiting_for_motions: ${state.waiting_for_motions}")

	if (!state.allow_exit) {
    	Log('exit_room. aborting due to not allowed exit!')
    }

	if (state.waiting_for_motions) {
    	Log("exit_room.  Cant exit room due to waiting for motions: ${state.waiting_for_motions}")
        return
    }

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

def allow_exit() {
	state.allow_exit = true
}

def disallow_exit() {
	state.allow_exit = false
}

def Log( message ) {
	log.debug("${app.label}: ${message}")
}

// TODO: implement event handlers

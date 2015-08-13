/**
 *  Brain
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

preferences {
    input("confIpAddr", "string", title:"Brain IP Address", required:true, displayDuringSetup: true)
    input("confTcpPort", "number", title:"Brain TCP Port", defaultValue:"80", required:true, displayDuringSetup:true)
}

metadata {


	definition (name: "Brain", namespace: "albrnick", author: "Nick Albright") {
		capability "Notification"
		capability "Polling"
		capability "Refresh"
        capability "Switch"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
		// TODO: define your main and details tiles here
	}
}

def updated() {
	log.debug("$device.displayName updated with settings: ${settings.inspect()}")

    setNetworkId(settings.confIpAddr, settings.confTcpPort)
    state.hostAddress = "${settings.confIpAddr}:${settings.confTcpPort}"
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"

}

// handle commands
def deviceNotification() {
	log.debug "Executing 'deviceNotification'"
	// TODO: handle 'deviceNotification' command
}

def poll() {
	log.debug "Executing 'poll'"
	// TODO: handle 'poll' command
}

def on() {
	log.debug "Executing 'on'"
	// TODO: handle 'poll' command
    return( refresh() )
}

def off() {
	log.debug "Executing 'off'"
	// TODO: handle 'poll' command
}


def refresh() {
	log.debug "Executing 'refresh'"
    // updated()
    
	// TODO: handle 'refresh' command
    def ret = test()
    log.debug('Refresh returning: ' + ret )
    return( ret )
}

def test() {
	log.debug( "Test" )
  	log.debug('executing action on: ' + getHostAddress() + ' / ' + device.deviceNetworkId)

    def hubAction = new physicalgraph.device.HubAction(
   	 	method: "GET",
    	path: '/room/',
    	headers: [HOST:getHostAddress()]
  	)
    hubAction
}

// Sets device Network ID in 'AAAAAAAA:PPPP' format
private String setNetworkId(ipaddr, port) { 
    log.debug("setNetworkId(${ipaddr}, ${port})")

    def hexIp = ipaddr.tokenize('.').collect {
        String.format('%02X', it.toInteger())
    }.join()

    def hexPort = String.format('%04X', port.toInteger())
    // hexPort = '5A00'
    device.deviceNetworkId = "${hexIp}:${hexPort}"
    log.debug "device.deviceNetworkId = ${device.deviceNetworkId}"
}


private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}
private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}
private getHostAddress() {
	return('192.168.5.222:9000')
	def ip = convertHexToIP('192.168.5.222')
	def port = convertHexToInt('9000')
	return ip + ":" + port
}

private def LOG(message) {
    //log.trace message
}
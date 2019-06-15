/**
 *  Tesla Powerwall Gateway II Local API Device Handler
 *
 *  Copyright 2019 Christian Ashby
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
}

metadata {
    definition (name: "Tesla Powerwall Gateway", namespace: "cscashby", author: "Christian Ashby") {
        capability "Polling"
        capability "Refresh"
        capability "Battery"
        capability "Energy Meter"
        capability "Power Meter"
    }

    simulator {
        // TODO: define status and reply messages here
    }

    tiles { //st.Home.home2
        valueTile("load", "device.load", width: 1, height: 1) {
            state "load", label:'Load: ${currentValue} kW', unit: "kW", icon: "st.Home.home5"
        }
		valueTile("grid", "device.grid", width: 1, height: 1) {
            state "grid", label:'Grid: ${currentValue} kW', unit: "kW", icon: "st.Seasonal Winter.seasonal-winter-011"
        }
        valueTile("solar", "device.solar", width: 1, height: 1) {
            state "solar", label:'Solar: ${currentValue} kW', unit: "kW", icon: "st.Weather.weather14"
        }
        valueTile("battery", "device.battery", width: 1, height: 1) {
            state "battery", label:'PW: ${currentValue} kW', unit: "kW", icon: "st.Home.home15"
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat") {
            state "default", action:"refresh.refresh", icon: "st.secondary.refresh"
        }
        main "solar"
        details(["load", "grid", "solar", "battery", "refresh"])
    }
}

// handle commands
def poll() {
    log.debug "Executing 'poll'"
    getReading()
}

def refresh() {
    log.debug "Executing 'refresh'"
    getReading()
}

// Get the sensor reading
private getReading() {
  def uri = "/api/meters/aggregates"
  def headers = [:]
  headers.put("HOST", getHostAddress())
  log.debug "Headers are ${headers}"

  def hubAction = new physicalgraph.device.HubAction([
    method: "GET",
    path: uri,
    headers: headers],
    null,
    [callback: calledBackHandler]
  )
  log.debug hubAction
  return sendHubCommand(hubAction) 
}

void calledBackHandler(physicalgraph.device.HubResponse hubResponse) {
    log.debug "Entered calledBackHandler()..."
    def result = hubResponse.json
    log.debug "body in calledBackHandler() is: ${result}"
	if( result ) {
	    sendEvent(name: "load", value: Math.round((result.load.instant_power/1000)*100) / 100)
		sendEvent(name: "grid", value: Math.round((result.site.instant_power/1000)*100) / 100)
	    sendEvent(name: "solar", value: Math.round((result.solar.instant_power/1000)*100) / 100)
	    sendEvent(name: "battery", value: Math.round((result.battery.instant_power/1000)*100) / 100)
	}
}

// gets the address of the device
private getHostAddress() {
    def ip = getDataValue("ip")
    def port = getDataValue("port")

    if (!ip || !port) {
        def parts = device.deviceNetworkId.split(":")
        log.debug("network ID ${device.deviceNetworkId}")
        if (parts.length == 2) {
            ip = parts[0]
            port = parts[1]
        } else {
            log.warn "Can't figure out ip and port for device: ${device.id}"
        }
    }

    log.debug "Using IP: $ip and port: $port for device: ${device.id}"
    return ip + ":" + port
}

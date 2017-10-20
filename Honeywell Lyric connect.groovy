/**
 *      Honeywell Lyric thermostat Service Manager
 *      API documentation: http://live-connectedhome.devportal.apigee.com/
 */
 include 'localization'
//Section1 definition, preference, mapping settings ---------------------------------------------------------
definition(
        name: "Honeywell Lyric (Connect)",
        namespace: "smartthings",
        author: "SmartThings",
        description: "Connect your Honeywell Lyric thermostat to SmartThings.",
        category: "SmartThings Labs",
        iconUrl: "https://smartapp-icons.s3.amazonaws.com/Partner/support/honeywell-lyric.png",
        iconX2Url: "https://smartapp-icons.s3.amazonaws.com/Partner/support/honeywell-lyric.png",
        iconX3Url: "https://smartapp-icons.s3.amazonaws.com/Partner/support/honeywell-lyric.png",
        singleInstance: true
) {
    appSetting "clientId"
    appSetting "secretKey"
}

preferences {
    page(name: "auth", title: "Honeywell Lyric", content:"authPage", uninstall: true)
    page(name: "deviceList", title: "Honeywell", content:"honeywellDeviceList", install:true)
}

mappings {
    path("/oauth/initialize") {action: [GET: "init"]}
    path("/oauth/callback")   {action: [GET: "callback"]}
}

//Section2: page-related methods ----------------------------------------------------------------------------
def authPage() {
    log.debug "authPage()"

    if(!atomicState.accessToken) {
        atomicState.accessToken = createAccessToken()  //this is used by ST dynamic callback uri
        log.debug "created access token: ${atomicState.accessToken}"
        log.debug "app.id: $app.id"
    }

    def description = null
    def uninstallAllowed = false
    def oauthTokenProvided = false

    if(atomicState.authToken) {
        description = "You are connected."
        uninstallAllowed = true
        oauthTokenProvided = true
    } else {
        description = "Click to enter Honeywell Credentials"
    }

    def redirectUrl = "${serverUrl}/oauth/initialize?appId=${app.id}&access_token=${atomicState.accessToken}&apiServerUrl=${apiServerUrl}"

    if (!oauthTokenProvided) {

        return dynamicPage(name: "auth", title: "Login", nextPage:null, uninstall:uninstallAllowed) {
            section(){
                paragraph "Tap below to log in to the Honeywell service and authorize SmartThings access. Be sure to scroll down on page 2 and press the 'Allow' button."
                href url:redirectUrl, style:"embedded", required:true, title:"Honeywell", description:description
            }
        }

    } else {
        return dynamicPage(name: "auth", title: "Log In", nextPage:"deviceList") {
            section(){
                paragraph "Tap Next to continue to setup your thermostats."
                href url:redirectUrl, style:"embedded", state:"complete", title:"Honeywell", description:description
            }
        }
    }

}

//1. redirect SmartApp to prompt user to input his/her credentials on 3rd party cloud service
def init() {
    log.debug "init()"
    def stcid = clientId
    def oauthParams = [
            response_type: "code",
            client_id: stcid,
            redirect_uri: callbackUrl,
    ]
    log.debug "${apiEndpoint}/oauth2/authorize?${toQueryString(oauthParams)}"
    redirect(location: "${apiEndpoint}/oauth2/authorize?${toQueryString(oauthParams)}")
}

/*2. Obtain authorization_code, access_token, refresh_token to be used with API calls
    2.1 get authorization_code from 3rd party cloud service
    2.2 use authorization_code to get access_token, refresh_token, and expire from 3rd party cloud service
*/
def callback() {
    log.debug "callback()>> params: $params, params.code ${params.code}"

    def appKey = "${appSettings.clientId}:${appSettings.secretKey}".encodeAsBase64()
    atomicState.appKey = appKey

    def tokenParams = [
            headers: ["Authorization": "Basic $appKey", "Content-Type": "application/x-www-form-urlencoded"],
            uri : "${apiEndpoint}/oauth2/token",
            body: [grant_type: 'authorization_code', code: params.code, redirect_uri: callbackUrl],
    ]

    try {
        httpPost(tokenParams) { resp ->
            atomicState.authToken = resp.data.access_token.toString()
            atomicState.refreshToken = resp.data.refresh_token.toString()
            atomicState.authTokenExpireIn = resp.data.expires_in.toString()
            log.debug "Response: ${resp.data}"
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        log.error "Error: ${e.statusCode}"
    }

    if (atomicState.authToken) {
        success()
    } else {
        fail()
    }
}

def success() {
    def message = """
    <p>Your Honeywell Account is now connected to SmartThings!</p>
    <p>Click 'Done' to finish setup.</p>
    """
    connectionStatus(message)
}

def fail() {
    def message = """
        <p>The connection could not be established!</p>
        <p>Click 'Done' to return to the menu.</p>
    """
    connectionStatus(message)
}

def connectionStatus(message, redirectUrl = null) {
    def redirectHtml = ""
    if (redirectUrl) {
        redirectHtml = """
            <meta http-equiv="refresh" content="3; url=${redirectUrl}" />
        """
    }

    def html = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=640">
<title>Honeywell Lyric & ST connection</title>
<style type="text/css">
        @font-face {
                font-family: 'Swiss 721 W01 Thin';
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot');
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot?#iefix') format('embedded-opentype'),
                         url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.woff') format('woff'),
                         url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.ttf') format('truetype'),
                         url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.svg#swis721_th_btthin') format('svg');
                font-weight: normal;
                font-style: normal;
        }
        @font-face {
                font-family: 'Swiss 721 W01 Light';
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot');
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot?#iefix') format('embedded-opentype'),
                         url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.woff') format('woff'),
                         url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.ttf') format('truetype'),
                         url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.svg#swis721_lt_btlight') format('svg');
                font-weight: normal;
                font-style: normal;
        }
        .container {
                width: 90%;
                padding: 4%;
                /*background: #eee;*/
                text-align: center;
        }
        img {
                vertical-align: middle;
        }
        p {
                font-size: 2.2em;
                font-family: 'Swiss 721 W01 Thin';
                text-align: center;
                color: #666666;
                padding: 0 40px;
                margin-bottom: 0;
        }
        span {
                font-family: 'Swiss 721 W01 Light';
        }
</style>
</head>
<body>
        <div class="container">
                <img src="https://smartapp-icons.s3.amazonaws.com/Partner/support/honeywell.png" alt="honeywell icon" />
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/connected-device-icn%402x.png" alt="connected device icon" />
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png" alt="SmartThings logo" />
                ${message}
        </div>
</body>
</html>
"""

    render contentType: 'text/html', data: html
}

/*
This method is called after "auth" page is done with Oauth2 authorization, then page "deviceList" with content of
honeywellDeviceList() will prompt the user to select which thermostat in his/her account to be used with ST.
Return -> (Map) thermostats object from page input (by user)
 */
def honeywellDeviceList() {
    //step1: get (list) of available devices associated with the login account.
    def devices = getHoneywellThermostats()
    log.debug "Page honeywellDeviceList() device list: $devices"

    //step2: render the page for user to select which device, if no devices was obtained, could be due to authentication issues,
    //       refresh page promptly, otherwise wait longer
    def p = dynamicPage(name: "deviceList", title: "Select Your Thermostats", refreshInterval: (devices ? 120 : 2), uninstall: true) {
        section(""){
            paragraph "Tap below to see the list of Honeywell thermostats available in your Honeywell account and select the ones you want to connect to SmartThings."
            //input to store all selected devices to be shown as (thermostat) things
            input(name: "thermostats", title:"", type: "enum", required:true, multiple:true, description: "Tap to choose", options:devices)
        }
    }
}

/*this method make HTTP GET to get locations, then use those locations to make another HTTP GET to obtain devices
  associated with a particular locations. Return-> Map([dni(mac addr):deviceDisplayName, ...])
*/
def getHoneywellThermostats() {
    /*
    required: 1. GET locations (headers: Content-Type, Authorization, params: apikey)
              2. GET devices (headers: Content-Type, Authorization, params: apikey, locationId)
     */
    log.debug "Page : getHoneywellThermostats()"

    //Step1: GET locations - returns list of maps containing data for each location (id, name, zipcode)
    def deviceLocationsParams = [
            uri: "${apiEndpoint}/v2/locations?apikey=${appSettings.clientId}",
            headers: ["Authorization": "Bearer ${atomicState.authToken}", "Content-Type":"application/json"]
    ]
    def thermostatLocations = []
    try {
        httpGet(deviceLocationsParams) { resp ->
            if(resp.status == 200){
                resp.data.each { respObject ->
                    thermostatLocations.add(['locationId': respObject.locationID, 'locationName': respObject.name, 'locationZipcode': respObject.zipcode])
                    log.info "Thermostats found at this location ${respObject.locationID}, name: ${respObject.name}, " +
                            "zipcode: ${respObject.zipcode}"
                }
            } else {
                log.debug "get locations failed with http status: ${resp.status}"
            }
            l
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        log.trace "Exception getting locations: " + e.response.data.status
        if (e.getStatusCode() == 401) {
            atomicState.action = "getHoneywellThermostats"
            log.debug "Refreshing your auth_token!"
            refreshAuthToken()
            return null
        } else {
            log.error "Authentication error, invalid authentication method, lack of credentials, etc."
        }
    }

    //Step2: GET devices (headers: Content-Type, Authorization, params: apikey, locationId) - need locationId from previous call
    def devices = [:]  //Map object to store number of found devices [dni:deviceDisplayName]
    def deviceList = [:]
    thermostatLocations.each {thermostatLocation ->
        def userParams = [
                uri: "${apiEndpoint}/v1/devices?apikey=${appSettings.clientId}&locationId=${thermostatLocation.get('locationId')}",
                headers: ["Authorization": "Bearer ${atomicState.authToken}", "Content-Type":"application/json"]
        ]
        try {
            httpGet(userParams) { resp ->

                if(resp.status == 200) {
                    resp.data.each { returnObject ->
                        def dni = returnObject.macID //used to identify device
                        def name = getThermostatDisplayName(returnObject)
                        devices[dni] = name
                        def deviceDetails = parseDeviceFromResponse(returnObject) << thermostatLocation
                        deviceList[dni] = [data:deviceDetails]
                        log.info "Found thermostat with dni(MAC): $dni and displayname: ${devices[dni]}"
                    }
                } else {
                    log.debug "get devices failed with http status: ${resp.status}"
                }
            }
        } catch (groovyx.net.http.HttpResponseException e) {
            log.trace "Exception getting devices: " + e.response.data.status
            if (e.getStatusCode() == 401) {
                atomicState.action = "getHoneywellThermostats"
                log.debug "Refreshing your auth_token!"
                refreshAuthToken()
                return null
            } else {
                log.error "Authentication error, invalid authentication method, lack of credentials, etc."
            }
        }
    }

    atomicState.devices = devices
    atomicState.thermostats = deviceList
    atomicState.locations = thermostatLocations
    return devices
}

//return String displayName of a thermostat
def getThermostatDisplayName(returnObject) {
    if(returnObject?.userDefinedDeviceName?.length() > 0) {
        return "${returnObject.userDefinedDeviceName}"
    } else {
        return "Honeywell Lyric"
    }
}

//Section3: installed, updated, initialize methods ----------------------------------------------------------

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def purgeDevice(dni) {
    // Method called by child when it's uninstalled so state.devices is pruned
    if (dni) {
        def thermostats = atomicState.thermostats
        thermostats.remove(dni)
        atomicState.thermostats = thermostats
        thermostats = atomicState.devices
        thermostats.remove(dni)
        atomicState.devices = thermostats
    }
}

def initialize() {
    log.debug "initialize"
    def devices = thermostats.collect{ dni ->
        //Check if the discovered thermostats are already initiated with corresponding device types.
        def d = getChildDevice(dni)  //this method inherits from SmartApp (data-management)
        if (!d){
            //addChildDevice() looks for corresponding device type specified by "childName" in "App Settings"
            d = addChildDevice(app.namespace, getChildName(), dni, null, ["label":atomicState.devices[dni]]) //addChildDevice() inherits from SmartApp (data-management)
            log.debug "created ${d.displayName} with id $dni"
        }else{
            log.debug "found ${d.displayName} with id $dni already exists"
        }
        return d
    }

    log.debug "created/found ${devices.size()} thermostats"

    def delete  // Delete children that are no longer selected
    if(!thermostats) {
        log.debug "delete thermostats"
        delete = getAllChildDevices() //inherits from SmartApp (data-management)
    } else {
        delete = getChildDevices().findAll { !thermostats.contains(it.deviceNetworkId) } //inherits from SmartApp (data-management)
    }
    log.debug "deleting ${delete.size()} thermostats"
    delete.each { deleteChildDevice(it.deviceNetworkId) } //inherits from SmartApp (data-management)

    // purge state from thermostats that are not selected
    def deviceList = [:]
    def locations = []
    devices = [:]
    thermostats.each { dni ->
        if (atomicState.thermostats[dni]) {
            def value = atomicState.thermostats.get(dni)
            deviceList.put(dni, value)
            if (!locations.contains(value.data.locationName)) {
                locations << value.data.locationName
            }
            if (atomicState.devices[dni]) {
                devices.put(dni, atomicState.devices.get(dni))
            }
        }
    }
    atomicState.devices = devices
    atomicState.thermostats = deviceList
    // Remove locations that doesn't have any thermostats
    def thermostatLocations = []
    locations.each { name ->
        thermostatLocations << atomicState.locations.find { it -> it.locationName == name }
    }
    atomicState.locations = thermostatLocations

    //send activity feeds to tell that device is connected
    def notificationMessage = "is connected to SmartThings"
    sendActivityFeeds(notificationMessage)
    atomicState.timeSendPush = null

    atomicState.pollRetries = 0
    poll()
    // unschedule and reschedule device update every 5 mins
    unschedule()
    runEvery5Minutes("poll")
}

//Section4: polling device info methods --------------------------------------------------------------------------------
/*
    This method is called to get obtain data from child devices, it updates the child devices with
    data:[temperatureUnit:Fahrenheit, temperature:77.0000, heatingSetpoint:72.0000, coolingSetpoint:78.0000,
    thermostatMode:heat, thermostatFanMode:auto, thermostatSetpoint:72.0000]
 */
def poll() {
    log.info "poll(), pollRetries:${atomicState.pollRetries}"
    atomicState.locations.each {thermostatLocation ->
        def pollParams = [
                uri: "${apiEndpoint}/v1/devices?apikey=${appSettings.clientId}&locationId=${thermostatLocation.locationId}",
                headers: ["Authorization": "Bearer ${atomicState.authToken}", "Content-Type":"application/json"]
        ]
        try {
            httpGet(pollParams) { resp ->
                atomicState.pollRetries = 0
                if(resp.status == 200) {
                    resp.data.each() {stat ->
                        def dni = stat.macID
                        def childDevice = getChildDevice(dni)
                        if (childDevice) {
                            def thermostats = atomicState.thermostats
                            def newData = parseDeviceFromResponse(stat) << thermostatLocation
                            log.debug "poll: updating dni:$dni with $newData"
                            thermostats[dni] = [data: newData]
                            atomicState.thermostats = thermostats
                            childDevice.sendEvent(name: "DeviceWatch-DeviceStatus", value: newData.deviceAlive? "online":"offline", displayed: false, isStateChange: true)
                            childDevice.generateEvent(newData)
                        }
                    }
                } else {
                    log.error "polling children & got http status ${resp.status}"
                }
            }
        } catch (groovyx.net.http.HttpResponseException e) {
            log.trace "Exception polling children: " + e.response.data.status
            if ((e.getStatusCode() == 401) && (atomicState.pollRetries < 5)) {
                atomicState.pollRetries = atomicState.pollRetries + 1
                atomicState.action = "poll"
                log.debug "Refreshing your auth_token! due to ${e.getStatusCode()}"
                refreshAuthToken()
            } else {
                if (atomicState.pollRetries > 4) {
                    atomicState.pollRetries = 0
                    log.error "Authentication retry error, failed 5 retries"
                } else {
                    log.error "Authentication error, invalid authentication method, lack of credentials, etc."
                }
            }
        }
    }
}

//Section5: device control methods -------------------------------------------------------------------------------------
//this method is called by (child) off(), heat(), and cool()
def setMode(child, jsonRequestBody, deviceId) {

    log.debug "setMode() >> requested mode ${mode} from deviceId: ${deviceId} and locationId: ${mapDeviceIdToLocationId(deviceId)}"
    def uri = "${apiEndpoint}/v2/devices/thermostats/${deviceId}?apikey=${appSettings.clientId}&locationId=${mapDeviceIdToLocationId(deviceId)}"
    def result = sendJson(child, uri, jsonRequestBody)
    return result

}

//this method is called by (child) raiseSetpoint() and lowerSetpoint()
def setHold(child, jsonRequestBody, deviceId) {

    log.debug "setHold() >> heatingSetpoints: $heating , coolingSetpoints: $cooling at deviceId: $deviceId"
    def uri = "${apiEndpoint}/v2/devices/thermostats/${deviceId}?apikey=${appSettings.clientId}&locationId=${mapDeviceIdToLocationId(deviceId)}"
    def result = sendJson(child, uri, jsonRequestBody)
    return result

}

def setFanMode(child, fanMode, deviceId) {

    log.debug "requested fan mode from (child) device type = ${fanMode} at deviceId: $deviceId"
    def uri = "${apiEndpoint}/v2/devices/thermostats/${deviceId}/fan?apikey=${appSettings.clientId}&locationId=${mapDeviceIdToLocationId(deviceId)}"
    def jsonRequestBody = '{"mode": "'+fanMode.capitalize()+'"}'
    def result = sendJson(child, uri, jsonRequestBody)
    return result

}

//provided "jsonBody", this method make httpPost call to update device settings
def sendJson(child, uri, jsonBody) {

    def postParams = [
            uri: uri,
            headers: ["Authorization": "Bearer ${atomicState.authToken}", "Content-Type":"application/json"],
            body: jsonBody
    ]

    def statusText
    def keepTrying = true
    atomicState.cmdAttempt = 1

    while (keepTrying) {
    try{
        httpPost(postParams) { resp ->
            if(resp.status == 200) {
                    atomicState.cmdAttempt = 0
                    keepTrying = false
            } else {
                log.debug "sendJson failed with http status: ${resp.status}"
                    keepTrying = false
            }
        }
    } catch(groovyx.net.http.HttpResponseException e) {
        if (e.getStatusCode() == 400) {
            statusText = "OFFLINE!"
            child.sendEvent("name": "thermostatStatus", "value": statusText, "description": statusText, displayed: true)
                keepTrying = false
        } else if (e.getStatusCode() == 401) {
                log.debug "Refreshing your auth_token! attempt#${atomicState.cmdAttempt}"
                if (atomicState.cmdAttempt < 6) {
                    atomicState.cmdAttempt = atomicState.cmdAttempt + 1
                    refreshAuthToken()
                    postParams.headers.Authorization = "Bearer ${atomicState.authToken}"
                } else {
                    log.info "Mode change failed: attempt#${atomicState.cmdAttempt}"
                    statusText = "Mode change failed due to expired token.  Login again."
                    child.sendEvent("name": "thermostatStatus", "value": statusText, "description": statusText, displayed: true)
                    keepTrying = false
                }
        } else {
            log.error "Authentication error, invalid authentication method, lack of credentials, etc."
                keepTrying = false
            }
        }
    }
    if (atomicState.cmdAttempt == 0) {
        return true
    }
    return keepTrying
}

private mapTemperatureSettingsToPreference(data) {
    data["deviceTemperatureUnit"] = data["deviceTemperatureUnit"] == "Fahrenheit"? "F" : data["deviceTemperatureUnit"] == "Celsius"? "C" : location.temperatureScale

    if (data["deviceTemperatureUnit"] == location.temperatureScale && data["deviceTemperatureUnit"] == "F") { // + - 1 F
        data["temperature"] = data["temperature"] ? data["temperature"].toInteger() : null
        data["outdoorTemperature"] = data["outdoorTemperature"] ? data["outdoorTemperature"].toInteger() : null
        data["heatingSetpoint"] = data["heatingSetpoint"] ? data["heatingSetpoint"].toInteger() : null
        data["coolingSetpoint"] = data["coolingSetpoint"] ? data["coolingSetpoint"].toInteger() : null
        data["minHeatingSetpoint"] = data["minHeatingSetpoint"] ? data["minHeatingSetpoint"].toInteger() : null
        data["maxHeatingSetpoint"] = data["maxHeatingSetpoint"] ? data["maxHeatingSetpoint"].toInteger() : null
        data["minCoolingSetpoint"] = data["minCoolingSetpoint"] ? data["minCoolingSetpoint"].toInteger() : null
        data["maxCoolingSetpoint"] = data["maxCoolingSetpoint"] ? data["maxCoolingSetpoint"].toInteger() : null
    } else if (data["deviceTemperatureUnit"] == location.temperatureScale && data["deviceTemperatureUnit"] == "C") { //+ - 0.5 C
        data["temperature"] = data["temperature"] ? roundC(data["temperature"]) : null
        data["outdoorTemperature"] = data["outdoorTemperature"] ? roundC(data["outdoorTemperature"]) : null
        data["heatingSetpoint"] = data["heatingSetpoint"] ? roundC(data["heatingSetpoint"]) : null
        data["coolingSetpoint"] = data["coolingSetpoint"] ? roundC(data["coolingSetpoint"]) : null
        data["minHeatingSetpoint"] = data["minHeatingSetpoint"] ? roundC(data["minHeatingSetpoint"]) : null
        data["maxHeatingSetpoint"] = data["maxHeatingSetpoint"] ? roundC(data["maxHeatingSetpoint"]) : null
        data["minCoolingSetpoint"] = data["minCoolingSetpoint"] ? roundC(data["minCoolingSetpoint"]) : null
        data["maxCoolingSetpoint"] = data["maxCoolingSetpoint"] ? roundC(data["maxCoolingSetpoint"]) : null
    } else if (data["deviceTemperatureUnit"] == "F") { //location.temperatureScale = C, convert temp F -> C
        data["temperature"] = data["temperature"] ? convertFtoC(data["temperature"]) : null
        data["outdoorTemperature"] = data["outdoorTemperature"] ? convertFtoC(data["outdoorTemperature"]) : null
        data["heatingSetpoint"] = data["heatingSetpoint"] ? convertFtoC(data["heatingSetpoint"]) : null
        data["coolingSetpoint"] = data["coolingSetpoint"] ? convertFtoC(data["coolingSetpoint"]) : null
        data["minHeatingSetpoint"] = data["minHeatingSetpoint"] ? convertFtoC(data["minHeatingSetpoint"]) : null
        data["maxHeatingSetpoint"] = data["maxHeatingSetpoint"] ? convertFtoC(data["maxHeatingSetpoint"]) : null
        data["minCoolingSetpoint"] = data["minCoolingSetpoint"] ? convertFtoC(data["minCoolingSetpoint"]) : null
        data["maxCoolingSetpoint"] = data["maxCoolingSetpoint"] ? convertFtoC(data["maxCoolingSetpoint"]) : null
    } else if (data["deviceTemperatureUnit"] == "C") { //location.temperatureScale = F, convert temp C -> F
        data["temperature"] = data["temperature"] ? convertCtoF(data["temperature"]) : null
        data["outdoorTemperature"] = data["outdoorTemperature"] ? convertCtoF(data["outdoorTemperature"]) : null
        data["heatingSetpoint"] = data["heatingSetpoint"] ? convertCtoF(data["heatingSetpoint"]) : null
        data["coolingSetpoint"] = data["coolingSetpoint"] ? convertCtoF(data["coolingSetpoint"]) : null
        data["minHeatingSetpoint"] = data["minHeatingSetpoint"] ? convertCtoF(data["minHeatingSetpoint"]) : null
        data["maxHeatingSetpoint"] = data["maxHeatingSetpoint"] ? convertCtoF(data["maxHeatingSetpoint"]) : null
        data["minCoolingSetpoint"] = data["minCoolingSetpoint"] ? convertCtoF(data["minCoolingSetpoint"]) : null
        data["maxCoolingSetpoint"] = data["maxCoolingSetpoint"] ? convertCtoF(data["maxCoolingSetpoint"]) : null
    }
}

private mapDeviceIdToLocationId(deviceId) {
    def thermostatItem
    def output
    atomicState.thermostats.keySet().each { key ->
        thermostatItem = atomicState.thermostats.get(key).get('data')
        if (thermostatItem.get('deviceId').equalsIgnoreCase(deviceId)) {
            output = thermostatItem.get('locationId')
        }
    }
    return output
}

//This method is used to refresh token from 3rd party cloud
private refreshAuthToken() {

    log.debug "refreshing auth token"
    def appKey = "${appSettings.clientId}:${appSettings.secretKey}".encodeAsBase64()

    def refreshTokenParams = [
            headers: ["Authorization": "Basic $appKey", "Content-Type": "application/x-www-form-urlencoded"],
            uri: "${apiEndpoint}/oauth2/token",
            body: [grant_type:'refresh_token', refresh_token:"${atomicState.refreshToken}"],
    ]

    def notificationMessage = "is disconnected from SmartThings, because the access credential changed or was lost. Please go to the Honeywell Lyric SmartApp and re-enter your account login credentials."

    try {
        httpPost(refreshTokenParams) { resp ->

            log.debug "Response: ${resp.data}"
            if(resp.status == 200) {
                if (resp.data) {
                    atomicState.refreshToken = resp.data.refresh_token.toString()
                    atomicState.authToken = resp.data.access_token.toString()
                    if (atomicState.action) {
                        def action = atomicState.action
                        atomicState.action = ""
                        log.debug "refreshAuthToken OK, calling ${action}"
                        runIn(2, "$action")
                    }
                }
            } else {
                sendPushAndFeeds(notificationMessage)
                log.error "refreshAuthToken()>> error msg: ${resp.data}"
            }

        }
    } catch (groovyx.net.http.HttpResponseException e) {
        log.error "refreshAuthToken() >> Error: e.statusCode ${e.statusCode}"
        log.error "refreshAuthToken() >> Error: e.response ${e.response.data}"
        if (e.getStatusCode() == 404 || e.getStatusCode() == 400 || e.getStatusCode() == 401) {
            sendPushAndFeeds(notificationMessage)
        }
    }
}

//Section6: helper methods ---------------------------------------------------------------------------------------------
def toJson(Map m) {
    return new org.codehaus.groovy.grails.web.json.JSONObject(m).toString()
}

/*
The collect() method in Groovy can be used to iterate over collections and transform each element of
the collection. The transformation is defined in as a closure and is passed to the collect() method.
 */
def toQueryString(Map m) {
    return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

def getChildName()               { return "Honeywell Lyric Thermostat" }
def getServerUrl()               { return "https://graph.api.smartthings.com" }
def getCallbackUrl()             { return "https://graph.api.smartthings.com/oauth/callback"}
def getApiEndpoint()             { return "https://api.honeywell.com"}
def getSecretKey()               { return appSettings.secretKey }
def getClientId()                { return appSettings.clientId }

def debugEvent(message, displayEvent = false) {

    def results = [
            name: "appdebug",
            descriptionText: message,
            displayed: displayEvent
    ]
    log.debug "Generating AppDebug Event: ${results}"
    sendEvent (results)
}

def debugEventFromParent(child, message) {

    child.sendEvent("name":"debugEventFromParent", "value":message, "description":message, displayed: true, isStateChange: true)
}

def availableModes(child) {

    def tData = atomicState.thermostats[child.device.deviceNetworkId]
    if(!tData) {
        log.error "ERROR: Device connection removed? no data for ${child.device.deviceNetworkId} after polling"
        return null
    } else {
        def modes = ["off"] //default "off"
        if (tData.data.availableModes.contains("heat")) modes.add("heat")
        if (tData.data.availableModes.contains("cool")) modes.add("cool")
        return modes
    }

}

def availableFanModes(child) {

    def tData = atomicState.thermostats[child.device.deviceNetworkId]
    if(!tData) {
        log.error "ERROR: Device connection removed? no data for ${child.device.deviceNetworkId} after polling"
        return null
    }else{
        def fanModes = []
        if (tData.data.availableFanModes.contains("auto")) fanModes.add("auto")
        if (tData.data.availableFanModes.contains("on")) fanModes.add("on")
        if (tData.data.availableFanModes.contains("circulate")) fanModes.add("circulate")
        return fanModes
    }

}

//send both push notification and mobile activity feeds
def sendPushAndFeeds(notificationMessage){
    log.warn "sendPushAndFeeds >> notificationMessage: ${notificationMessage}"
    log.warn "sendPushAndFeeds >> atomicState.timeSendPush: ${atomicState.timeSendPush}"
    if (atomicState.timeSendPush){
        if (now() - atomicState.timeSendPush > 86400000){ // notification is sent to remind user once a day
            sendPush("Honeywell Lyric " + notificationMessage)
            sendActivityFeeds(notificationMessage)
            atomicState.timeSendPush = now()
        }
    } else {
        sendPush("Honeywell Lyric " + notificationMessage)
        sendActivityFeeds(notificationMessage)
        atomicState.timeSendPush = now()
    }
    atomicState.authToken = null
}

def sendActivityFeeds(notificationMessage) {
    def devices = getChildDevices()
    devices.each { child ->
        child.generateActivityFeedsEvent(notificationMessage) //parse received message from parent
    }
}

/*
 * Utility method to create a device entry from an object in the API response
 * to the call to /v1/devices
 * API gets called in multiple places, this consolidates parsing
 */
private def parseDeviceFromResponse(resp) {
    def data = [
        deviceTemperatureUnit: resp.thermostat.units, // (String) Fahrenheit/Celsius
        temperature: resp.thermostat.indoorTemperature,
        outdoorTemperature: resp.thermostat.outdoorTemperature,
        deadband: resp.thermostat.deadband,
        heatingSetpoint: resp.thermostat?.changeableValues?.heatSetpoint,
        coolingSetpoint: resp.thermostat?.changeableValues?.coolSetpoint,
        thermostatMode: resp.thermostat?.changeableValues?.mode?.toLowerCase(),
        humidity: resp.thermostat.indoorHumidity?.toInteger(),
        minHeatingSetpoint: resp.thermostat.minHeatSetpoint,
        maxHeatingSetpoint: resp.thermostat.maxHeatSetpoint,
        minCoolingSetpoint: resp.thermostat.minCoolSetpoint,
        maxCoolingSetpoint: resp.thermostat.maxCoolSetpoint,
        deviceAlive: resp.isAlive,
        deviceId: resp.deviceID,
        deviceName: getThermostatDisplayName(resp),
        thermostatFanMode: resp.settings?.fan?.changeableValues?.mode?.toLowerCase()
    ]

    if (resp.thermostat?.changeableValues?.containsKey("emergencyHeatActive")) {
        data["emergencyHeatActive"] = resp.thermostat.changeableValues.emergencyHeatActive
    } else {
        data["emergencyHeatActive"] = "notConfigured"
    }

    mapTemperatureSettingsToPreference(data)

    def availableModes_temp = resp.thermostat.allowedModes
    data["availableModes"] = []
    availableModes_temp.each { data["availableModes"].add(it.toLowerCase()) }

    if (resp.settings.fan) {
        def availableFanModes_temp = resp.settings.fan.allowedModes
        data["availableFanModes"] = []
        availableFanModes_temp.each { data["availableFanModes"].add(it.toLowerCase()) }
    }
    return data
}


def roundC (tempC) {
    return String.format("%.1f", (Math.round(tempC * 2))/2)
}

def convertFtoC (tempF) {
    return String.format("%.1f", (Math.round(((tempF - 32)*(5/9)) * 2))/2)
}

def convertCtoF (tempC) {
    return (Math.round(tempC * (9/5)) + 32).toInteger()
}

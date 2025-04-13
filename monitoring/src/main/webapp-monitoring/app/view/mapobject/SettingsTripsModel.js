/**
 * Created by ivan on 27.07.17.
 */

Ext.define('Seniel.view.mapobject.SettingsTripsModel', {
    extend: 'Ext.data.Model',
    /*
     lazy val minMovementSpeed = 1
     lazy val minParkingTime = 300
     lazy val maxDistanceBetweenMessages = 10000
     lazy val minTripTime = minTripTime
     lazy val minTripDistance = 400
     lazy val useIgnitionToDetectMovement = false
     lazy val minSatelliteNum = 3
     */
    fields: [
        {name: 'repTripsMinMovementSpeed', type: 'int', defaultValue: 1},
        {name: 'repTripsMinParkingTime', type: 'int', defaultValue: 300},
        {name: 'repTripsMaxDistanceBetweenMessages', type: 'int', defaultValue: 10000},
        {name: 'repTripsMinTripTime', type: 'int', defaultValue: 180},
        {name: 'repTripsMinTripDistance', type: 'int', defaultValue: 400},
        {name: 'repTripsUseIgnitionToDetectMovement', type: 'boolean', defaultValue: false},
        {name: 'repTripsMinSatelliteNum', type: 'int', defaultValue: 3}
    ]
});
/**
 * Created by IVAN on 12.05.2014.
 */

Ext.define('WRExtUtils.AtmosphereExtJSBroadcaster', {
    mixins: {
        observable: 'Ext.util.Observable'
    },
    singleton: true,
    constructor: function (config) {
        // The Observable constructor copies all of the properties of `config` on
        // to `this` using Ext.apply. Further, the `listeners` property is
        // processed to add listeners.
        //
        this.mixins.observable.constructor.call(this, config);
    }
});
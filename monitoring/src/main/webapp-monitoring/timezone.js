/**
 * Created by IVAN on 06.04.2015.
 */
Ext.Date.formatFunctions['d.m.Y withTZ'] = function() {
    return moment(this).format("DD.MM.YYYY")
}
Ext.Date.formatFunctions['d.m.Y H:i:s withTZ'] = function() {
    return moment(this).format("DD.MM.YYYY HH:mm:ss")
};
Ext.Date.formatFunctions['Y-m-d withTz'] = function() {
    return moment(this).format("YYYY-MM-DD")
};
Ext.Date.formatFunctions['Y-m-d H:i:s withTz'] = function() {
    return moment(this).format("YYYY-MM-DD HH:mm:ss")
};
Ext.Date.formatFunctions['d/m/Y withTz'] = function() {
    return moment(this).format("DD/MM/YYYY")
};
Ext.Date.formatFunctions['d/m/Y H:i:s withTz'] = function() {
    return moment(this).format("DD/MM/YYYY HH:mm:ss")
};

//console.log("Ext.Date.formatFunctions",Ext.Date.formatFunctions);
//console.log("Ext.Date.parseFunctions",Ext.Date.parseFunctions);
Timezone = {
    setDefaultTimezone:function(tzId){
        moment.tz.setDefault(tzId);
    },
    getDefaultTimezone:function(){
        return moment.defaultZone;
    },
    setUserDefaultTimezone:function(){
            var self=this;
            timeZonesStore.getUserTimezone(
                function(tzId) {
                    if (!tzId) {
                        console.log("default zone not set");
                    }
                    else {
                        console.log("zoneName",tzId);
                        self.setDefaultTimezone(tzId);
                        console.log("moment.defaultZone",moment.defaultZone);
                    }
                }
            )
    }
};
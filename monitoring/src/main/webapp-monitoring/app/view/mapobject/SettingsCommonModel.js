Ext.define('Seniel.view.mapobject.SettingsCommonModel', {
    extend: 'Ext.data.Model',
    fields: [
        {name: 'repFuelFullVolume',  type: 'int', defaultValue: ''},
        {name: 'repFuelMinFueling', type: 'int', defaultValue: 10},
        {name: 'repFuelMinDraining', type: 'int', defaultValue: 10},
        {name: 'repFuelMinTime', type: 'int', defaultValue: 60},
        {name: 'repFuelFiltering', type: 'int', defaultValue: 0},
        {name: 'repFuelIsNoFiltering', type: 'boolean', defaultValue: false},
        //{name: 'repFuelRefuelingTimeout', type: 'int', defaultValue:0},
        {name: 'repFuelIgnoreMessagesAfterMoving', type: 'int', defaultValue:0},
        {name: 'repFuelNormIdling', type: 'float', defaultValue: ''},
        {name: 'repFuelNormUrban', type: 'float', defaultValue: ''},
        {name: 'repFuelNormXUrban', type: 'float', defaultValue: ''},
        {name: 'repFuelUseCalcStandards', type: 'boolean', defaultValue: false},
        {name: 'repFuelUseBasicStandards', type: 'boolean', defaultValue: false},
        {name: 'repFuelNormKLoad', type: 'float', defaultValue: ''},
        {name: 'repFuelNormWinter', type: 'float', defaultValue: ''},
        {name: 'repFuelNormSummer', type: 'float', defaultValue: ''}

    ]
});
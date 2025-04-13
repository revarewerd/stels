/**
 * Created by IVAN on 10.10.2014.
 */
Ext.define('Billing.view.object.sensors.ObjectSensorsWindow', {
    extend: 'Ext.window.Window',
    requires: [
        'Billing.view.object.sensors.SettingsSensorsGrid'
    ],
    alias: 'widget.objsensorswnd',
    stateId: 'objSensorsWnd',
    stateful: true,
    icon: 'images/ico16_options.png',
    title: "Настройки объекта",//tr('settingswidow.title'),
    width: 720,
    height: 460,
    minWidth: 480,
    minHeight: 400,
    layout: 'fit',
    initComponent: function () {

        var self = this;
        Ext.apply(this,{
            items: [
        {
            itemId: 'objSetingsTab',
            xtype: 'tabpanel',
            padding: 0,
            defaults: {
                bodyPadding: 0,
                padding: 0
            },
            items: [
                {
                    title: "Датчики",//tr('main.reports.sensors'),
                    itemId: 'objSensors',
                    xtype: 'panel',
                    closable: false,
                    layout: 'fit',
                    items: [
                        {
                            xtype: 'settingssensorsgrid',
                            objRecord:this.objRecord
                        }
                    ]
                },
                {
                    title: "Топливо",//tr('settingswidow.fuel'),
                    itemId: 'objProperties',
                    xtype: 'panel',
                    closable: false,
                    layout: {
                        type: 'vbox',
                        align: 'stretch'
                    },
                    items: [
                        {
                            xtype: 'form',
//                            title: 'Параметры отчётов',
                            defaults: {
                                xtype: 'textfield',
                                anchor: '100%',
                                labelWidth: 220,
                                labelPad: 10,
                                padding: '4 8 4 8',
                                value: '',
                                listeners: {
                                    change: function(field, newVal, oldVal) {
                                        var form = field.up('form'),
                                            rec = form.getRecord();
                                        rec.set(field.getName(), newVal);
                                        form.updateRecord(rec);

                                        if (oldVal) {
                                            field.up('objsensorswnd').down('#btnApplySettings').enable();
                                        }
                                    }
                                }
                            },
                            layout: 'anchor',
                            border: false,
                            items: [
                                {
                                    name: 'repFuelFullVolume',
                                    fieldLabel: "Объем топливного бака, л",//tr('settingswidow.fuelvolume'),
                                    padding: '8 8 4 8'
                                },
                                {
                                    name: 'repFuelMinFueling',
                                    fieldLabel: "Минимальный объем заправки, л"//tr('settingswidow.minfuelvolume')
                                },
                                {
                                    name: 'repFuelMinDraining',
                                    fieldLabel: "Минимальный объем слива, л"//tr('settingswidow.minfueldrain')
                                },
                                {
                                    name: 'repFuelMinTime',
                                    fieldLabel:"Минимальное время слива, л"//tr('settingswidow.minfueldraintime')
                                },
                                {
                                    name: 'repFuelFiltering',
                                    fieldLabel: "Усреднение данных датчиков"//tr('settingswidow.averagesensordata')
                                }
                            ]
                        }
                    ]
                }
            ]
        }
    ],
    dockedItems: [
        {
            xtype: 'toolbar',
            dock: 'bottom',
            items: [
                '->',
                {
                    xtype: 'button',
                    text: "Применить",//tr('main.apply'),
                    itemId: 'btnApplySettings',
                    disabled: true,
                    handler: function(btn) {
                        self.saveSettings();
                        this.disable();
                    }
                },
                {
                    xtype: 'button',
                    text: "ОК",//tr('main.ok'),
                    itemId: 'btnOk',
                    icon: 'images/ico16_okcrc.png',
                    handler: function(btn) {
                        self.saveSettings();
                        self.close();
                    }
                },
                {
                    xtype: 'button',
                    text: "Отмена",//tr('main.cancel'),
                    itemId: 'btnCancel',
                    icon: 'images/ico16_cancel.png',
                    handler: function(btn) {
                        self.close();
                    }
                }
            ]
        }
    ]
    });
        this.callParent(arguments);

        this.on('boxready', function() {
            this.loadSettings();
        });
    },

    //---------
    // Функциии
    //---------
    loadSettings: function() {
        var self=this
        var rec=self.objRecord;
        sensorsSettings.loadObjectSettings(rec.get('uid'), function(data, response) {
            var os = data;
            var cmnRec = Ext.create('Billing.view.object.sensors.FuelModel', os),
                frmCommon = self.down('#objProperties form');
            frmCommon.loadRecord(cmnRec);
            frmCommon.initRecord = cmnRec;

            var uid = rec.get('uid');
            var sensorsGrid = self.down('settingssensorsgrid');
            sensorsSettings.loadObjectSensors(uid, function (resp, request) {
                if (resp && resp.length > 0 && sensorsGrid) {
                    sensorsGrid.getStore().add(resp);
                }
            });
        })
    },
    saveSettings: function() {
        var self = this;
        var frmCommon = self.down('#objProperties').down('form');
        var params = {};
        var settings = frmCommon.getRecord().data;
        var sensorsGrid = self.down('settingssensorsgrid');
        console.log('Object settings data = ', settings);
        if (sensorsGrid) {
            params.sensors = [];
            sensorsGrid.getStore().each(function(rec) {
                params.sensors.push(rec.getData());
            });
        }

        if (settings && self.objRecord) {
         sensorsSettings.saveObjectSettings(self.objRecord.get('uid'), settings, params);
        }
        console.log('Object params = ', params);
    }
});
Ext.define('Billing.view.object.sensors.FuelModel', {
    extend: 'Ext.data.Model',
    fields: [
        {name: 'repFuelFullVolume',  type: 'int', defaultValue: ''},
        {name: 'repFuelMinFueling', type: 'int', defaultValue: 10},
        {name: 'repFuelMinDraining', type: 'int', defaultValue: 10},
        {name: 'repFuelMinTime', type: 'int', defaultValue: 60},
        {name: 'repFuelFiltering', type: 'int', defaultValue: 10}
    ]
});
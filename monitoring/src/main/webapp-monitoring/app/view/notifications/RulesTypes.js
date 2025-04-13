Ext.define('Seniel.view.notifications.RulesTypes', {
    singleton: true,
    getTypesList: function() {
        var list = [];
        for (var i in this.items) {
            list.push({type: i, text: this.getRuleName(i)});
        }
        return list;
    },
    getMsgMasksList: function() {
        var items = this.items;
        var map = {};
        for (var i in items) {
            map[i] = items[i].msgMask;
        }
        return map;
    },
    getFormPanels: function() {
        var items = this.items;
        var panels = [];
        var isHidden = true;
        for (var i in items) {
            isHidden = (i === 'ntfVelo')?(false):(true);
            panels.push({
                xtype: 'form',
                itemId: (i + 'Params'),
                border: false,
                defaults: {
                    anchor: '100%'
                },
                hidden: isHidden,
                items: items[i].formContent
            });
        }
        return panels;
    },
    
    getRuleName: function(type) {
        return (this.isRuleExists(type))?(this.items[type].name):(tr('rules.grid.type.newrule'));
    },
    getRuleParams: function(type, wnd) {
        return (this.isRuleExists(type))?(this.items[type].getParams(wnd)):(null);
    },
    setRuleParams: function(type, params, wnd) {
        return (this.isRuleExists(type))?(this.items[type].setParams(params, wnd)):(false);
    },
    validateRuleParams: function(type, wnd) {
        return (this.isRuleExists(type))?(this.items[type].validate(wnd)):({errorTitle: tr('rules.nooparam'), errorMsg: ''});
    },
    formatRuleParams: function(type, value, grid) {
        return (this.isRuleExists(type))?(this.items[type].format(value, grid)):(value);
    },
    isRuleExists: function(type) {
        return (this.items[type] !== null && typeof this.items[type] === 'object');
    },
    
    items: {
        ntfVelo: {
            id: 'ntfVelo',
            name: tr('rules.grid.type.velo'),
            msgMask: tr('rules.ntfmasks.velo'),
            formContent: [
                {
                    xtype: 'numberfield',
                    itemId: 'ntfVeloMax',
                    fieldLabel: tr('rules.maxspeed'),
                    allowBlank: false,
                    hideTrigger: true,
                    labelWidth: 180,
                    minValue: 10,
                    value: 80,
                    padding: '8 8 4 8'
                }
            ],
            getParams: function(wnd) {
                var veloParam = wnd.down('#ntfVeloMax');
                return {
                    ntfVeloMax: veloParam.getValue()
                };
            },
            setParams: function(params, wnd) {
                wnd.down('#ntfVeloMax').setValue(params['ntfVeloMax']);
            },
            validate: function(wnd) {
                var veloParam = wnd.down('#ntfVeloMax');
                if (veloParam.validate()) {
                    return true;
                }
                return {
                    errorTitle: tr('rules.nooparam'),
                    errorMsg: tr('rules.nooparam.msg')
                };
            },
            format: function(value, grid) {
                return (tr('rules.overspeeding')+' ' + value['ntfVeloMax'] + ' '+tr('mapobject.laststate.kmsperhour'));
            }
        },
        ntfGeoZ: {
            id: 'ntfGeoZ',
            name: tr('rules.grid.type.geozone'),
            msgMask: tr('rules.ntfmasks.geo'),
            formContent: [
                {
                    xtype: 'combo',
                    itemId: 'ntfGeoZCh',
                    fieldLabel: tr('rules.selectgeo'),
                    store: {
                        fields: ['id', 'name'],
                        data: {
                                'items': []
                        },
                        proxy: {
                                type: 'memory',
                                reader: {
                                        type: 'json',
                                        root: 'items'
                                }
                        }
                    },
                    labelWidth: 180,
                    padding: '8 8 0 8',
                    allowBlank: false,
                    queryMode: 'local',
                    valueField: 'id',
                    displayField: 'name',
                    forceSelection: true,
                    anyMatch: true,
                    caseSensitive: false
                },
                {
                    xtype: 'wrradiogroup',
                    itemId: 'ntfGeoZRG',
                    columns: 1,
                    defaults: {
                        labelWidth: 320,
                        labelAlign: 'right',
                        labelPad: 10
                    },
                    initItemsConfig: [
                        {boxLabel: tr('rules.geo.controlleave'), inputValue: true, checked: true},
                        {boxLabel: tr('rules.geo.controlentrance'), inputValue: false}
                    ],
                    padding: '4 8 0 4',
                    listeners: {
                        change: function(rg, newVal, oldVal) {
                            var wnd = rg.up('window'),
                                mask = wnd.down('#ntfMessageMask'),
                                text = mask.getValue();
                            if (!newVal[rg.rgName]) {
                                text = text.replace(tr('rules.ntfmasks.geo.left'), tr('rules.ntfmasks.geo.entered'));
                            } else {
                                text = text.replace(tr('rules.ntfmasks.geo.entered'), tr('rules.ntfmasks.geo.left'));
                            }
                            mask.setValue(text);
                        }
                    }
                }
            ],
            getParams: function(wnd) {
                var geozParam = wnd.down('#ntfGeoZCh');
                return {
                    ntfGeoZCh: geozParam.getValue(),
                    ntfGeoZRG: geozParam.next().getRealValue()
                };
            },
            setParams: function(params, wnd) {
                var geozParam = wnd.down('#ntfGeoZCh');
                wnd.down('#ntfGeoZRG').setRealValue(params['ntfGeoZRG']);
                if (params['ntfGeoZCh']) {
                    if (geozParam.store) {
                        geozParam.getStore().on('load', function(store, records) {
                            geozParam.setValue(params['ntfGeoZCh']);
                        });
                        geozParam.setValue(params['ntfGeoZCh']);
                    }
                }
            },
            validate: function(wnd) {
                var geozParam = wnd.down('#ntfGeoZCh');
                if (geozParam.validate()) {
                    return true;
                }
                return {
                    errorTitle: tr('rules.nooparam'),
                    errorMsg: tr('rules.selectgeo.msg')
                };
            },
            format: function(value, grid) {
                var viewport = grid.up('viewport'),
                    geozArr = viewport.geozArray,
                    geozId = parseInt(value['ntfGeoZCh']),
                    str = (value['ntfGeoZRG'])?(tr('rules.leavegeozone')):(tr('rules.entergeozone'));

                for (var i = 0; i < geozArr.length; i++) {
                    if (geozId === geozArr[i].id) {
                        str += ' &laquo;'+geozArr[i].name+'&raquo;';
                        break;
                    }
                }
                return str;
            }
        },
        ntfData: {
            id: 'ntfData',
            name: tr('rules.grid.type.sensor'),
            msgMask: tr('rules.ntfmasks.sensor'),
            formContent: [
                {
                    xtype: 'combo',
                    itemId: 'ntfDataSensor',
                    fieldLabel: tr('rules.sensorselection'),
                    allowBlank: false,
                    queryMode: 'local',
                    valueField: 'code',
                    displayField: 'name',
                    labelWidth: 180,
                    padding: '8 8 4 8',
                    listeners: {
                        boxready: function() {
                            var self = this;

                            Ext.apply(self, {
                                store: Ext.create('EDS.store.SensorsList', {
                                    autoLoad: true,
                                    isDataLoaded: true,
                                    listeners: {
                                        beforeload: function(store, operation) {
                                            if (self.selectedUIDs) {
                                                Ext.apply(store.getProxy().extraParams, {
                                                    uids: self.selectedUIDs
                                                });
                                            }
                                            console.log('Sensor picker\'s store', store);
                                        }
                                    }
                                })
                            });
                        }
                    }
                },
                {
                    xtype: 'numberfield',
                    itemId: 'ntfDataMin',
                    fieldLabel: tr('rules.lowerintervalbound'),
                    allowBlank: false,
                    hideTrigger: true,
                    labelWidth: 180,
                    //minValue: 0,
                    padding: '0 8 4 8'
                },
                {
                    xtype: 'numberfield',
                    itemId: 'ntfDataMax',
                    fieldLabel: tr('rules.upperintervalbound'),
                    allowBlank: false,
                    hideTrigger: true,
                    labelWidth: 180,
                    //minValue: 0,
                    padding: '0 8 4 8'
                },
                {
                    xtype: 'wrradiogroup',
                    itemId: 'ntfDataOperate',
                    columns: 1,
                    defaults: {
                        labelWidth: 320,
                        labelAlign: 'right',
                        labelPad: 10
                    },
                    initItemsConfig: [
                        {boxLabel: tr('rules.workswithin'), inputValue: false, checked: true},
                        {boxLabel: tr('rules.worksoutside'), inputValue: true}
                    ],
                    padding: '0 8 0 4'
                }
            ],
            getParams: function(wnd) {
                var dataParam = wnd.down('#ntfDataSensor');
                return {
                    ntfDataSensor: dataParam.getValue(),
                    ntfDataMin: dataParam.next().getValue(),
                    ntfDataMax: dataParam.next().next().getValue(),
                    ntfDataOperate: dataParam.next().next().next().getRealValue()
                };
            },
            setParams: function(params, wnd) {
                var dataParam = wnd.down('#ntfDataSensor');
                wnd.down('#ntfDataOperate').setRealValue(params['ntfDataOperate']);
                if (params['ntfDataSensor']) {
                    if (dataParam.store && dataParam.store.isDataLoaded) {
                        dataParam.getStore().on('load', function(store, records) {
                            dataParam.setValue(params['ntfDataSensor']);
                        });
                        dataParam.setValue(params['ntfDataSensor']);
                    } else {
                        dataParam.on('boxready', function() {
                            dataParam.setValue(params['ntfDataSensor']);
                        });
                    }
                }
                wnd.down('#ntfDataMin').setValue(params['ntfDataMin']);
                wnd.down('#ntfDataMax').setValue(params['ntfDataMax']);
            },
            validate: function(wnd) {
                var dataParam = wnd.down('#ntfDataSensor');
                if (dataParam.validate() && dataParam.next().validate() && dataParam.next().next().validate()) {
                    return true;
                }
                return {
                    errorTitle: tr('rules.nooparam'),
                    errorMsg: tr('rules.selectsensor.msg')
                };
            },
            format: function(value, grid) {
                var str = value['ntfDataSensor'];
                str += (value['ntfDataOperate'])?(' ' + tr('rules.valuesoutside')):(' ' + tr('rules.valueswithin'));
                str += ' [' + value['ntfDataMin'] + ' ... ' + value['ntfDataMax'] + ']';
                return str;
            }
        },
        ntfSlpr: {
            id: 'ntfSlpr',
            name: tr('rules.grid.type.sleeper'),
            msgMask: tr('rules.ntfmasks.power'),
            formContent: [
                {
                    xtype: 'combo',
                    itemId: 'ntfSlprAlarmOpts',
                    name: 'ntfSlprAlarmOpts',
                    fieldLabel: tr('rules.controlevent'),
                    store: Ext.create('Ext.data.ArrayStore', {
                        fields: ['type', 'text'],
                        data: {
                            'items': [
                                {type: 'ExtPower', text: tr('rules.poweroff')},
                                {type: 'Movement', text: tr('rules.movingstart')},
                                {type: 'LightInput', text: tr('rules.photooff')}
                            ]
                        },
                        proxy: {
                            type: 'memory',
                            reader: {
                                type: 'json',
                                root: 'items'
                            }
                        }
                    }),
                    allowBlank: false,
                    editable: false,
                    queryMode: 'local',
                    valueField: 'type',
                    displayField: 'text',
                    value: 'ExtPower',
                    labelWidth: 180,
                    padding: '8 8 4 8',
                    maskArray: {
                        'ExtPower': tr('rules.maskarray2.poweroff'),
                        'Movement': tr('rules.maskarray2.movementdetected'),
                        'LightInput': tr('rules.maskarray2.photo')
                    },
                    listeners: {
                        change: function(cb, newVal, oldVal) {
                            var wnd = cb.up('window'),
                                mask = wnd.down('#ntfMessageMask');
                            if (mask.getValue() !== cb.maskArray[oldVal]) {
                                cb.maskArray[oldVal] = mask.getValue();
                            }
                            mask.setValue(cb.maskArray[newVal]);
                        }
                    }
                }
            ],
            getParams: function(wnd) {
                var slprParam = wnd.down('#ntfSlprAlarmOpts');
                var params = {};
                params['ntfSlpr' + slprParam.getValue()] = true;
                return params;
            },
            setParams: function(params, wnd) {
                var slprParam = wnd.down('#ntfSlprAlarmOpts');
                if (params['ntfSlprLightInput']) {
                    slprParam.setValue('LightInput');
                } else if (params['ntfSlprMovement']) {
                    slprParam.setValue('Movement');
                } else {
                    slprParam.setValue('ExtPower');
                }
            },
            validate: function(wnd) {
                var slprParam = wnd.down('#ntfSlprAlarmOpts');
                if (slprParam.validate()) {
                    return true;
                }
                return {
                    errorTitle: tr('rules.nooparam'),
                    errorMsg: tr('rules.selectparam.msg')
                };
            },
            format: function(value, grid) {
                return  (value['ntfSlprExtPower'])?(tr('rules.poweroff')):(
                        (value['ntfSlprMovement'])?(tr('rules.movingstart')):(
                        (value['ntfSlprLightInput'])?(tr('rules.photooff')):('')));
            }
        },
        ntfDist: {
            id: 'ntfDist',
            name: tr('rules.grid.type.distance'),
            msgMask: tr('rules.ntfmasks.distance'),
            formContent: [
                {
                    xtype: 'numberfield',
                    itemId: 'ntfDistMax',
                    fieldLabel: tr('rules.maxdaydistance'),
                    allowBlank: false,
                    hideTrigger: true,
                    labelWidth: 180,
                    minValue: 10,
                    value: 120,
                    padding: '8 8 4 8'
                }
            ],
            getParams: function(wnd) {
                var distParam = wnd.down('#ntfDistMax');
                return {
                    ntfDistMax: distParam.getValue()
                };
            },
            setParams: function(params, wnd) {
                wnd.down('#ntfDistMax').setValue(params['ntfDistMax']);
            },
            validate: function(wnd) {
                var distParam = wnd.down('#ntfDistMax');
                if (distParam.validate()) {
                    return true;
                }
                return {
                    errorTitle: tr('rules.nooparam'),
                    errorMsg: tr('rules.selectdistlimit.msg')
                };
            },
            format: function(value, grid) {
                return tr('rules.distanceexcess') + ' ' + value['ntfDistMax'] + ' ' + tr('movinggroupgrid.km');
            }
        },
        ntfMntnc: (function () {

            function MntAttribute(prefix, prefixShort) {

                if (!prefixShort)
                    prefixShort = prefix;

                this.field = function () {
                    return {
                        xtype: 'fieldcontainer',
                        layout: 'hbox',
                        fieldLabel: (tr('rules.max' + prefix.toLowerCase())),
                        labelWidth: 280,
                        padding: '8 8 4 8',
                        items: [
                            {
                                name: (prefix + "Enabled"),
                                itemId: ('ntfMntnc' + prefix + 'Enabled'),
                                xtype: 'checkbox',
                                value: 'true',
                                boxLabel: (tr('enabled')),
                                checked: 'true',
                                padding: '0 8 0 0',
                                listeners: {
                                    'change': function (field, newVal) {
                                        field.up('fieldcontainer').query('field').forEach(function (e) {
                                            if (e != field)
                                                e.setDisabled(!newVal);
                                        });
                                    }
                                }
                            },
                            {
                                xtype: 'numberfield',
                                itemId: ('ntfMntnc' + prefix + 'Max'),
                                allowBlank: true,
                                hideTrigger: true,
                                minValue: 10,
                                value: 10000

                            }
                        ]
                    }
                };


                this.params = function (wnd) {
                    var params = {};
                    params['ntf' + prefixShort + 'Max'] = wnd.down('#ntfMntnc' + prefix + 'Max').getValue();
                    params['ntf' + prefixShort + 'Enabled'] = wnd.down('#ntfMntnc' + prefix + 'Enabled').getValue();
                    return params;
                };

                this.setParams = function (params, wnd) {
                    wnd.down('#ntfMntnc' + prefix + 'Max').setValue(params['ntf' + prefixShort + 'Max']);
                    wnd.down('#ntfMntnc' + prefix + 'Enabled').setValue(params['ntf' + prefixShort + 'Enabled']);
                }

            }

            var attributes = [
                new MntAttribute("Distance", "Dist"),
                new MntAttribute("Moto"),
                new MntAttribute("Hours")
            ];


            return {
                id: 'ntfMntnc',
                name: tr('rules.grid.type.maintenance'),
                msgMask: tr('rules.ntfmasks.maintenance'),
                formContent: (Ext.Array.map(attributes, function (a) {
                    return a.field();
                })),
                getParams: function (wnd) {
                    return Ext.Object.merge.apply(this, Ext.Array.map(attributes, function (a) {
                        return a.params(wnd);
                    }));
                },
                setParams: function (params, wnd) {
                    Ext.Array.forEach(attributes, function (a) {
                        a.setParams(params, wnd);
                    })
                },
                validate: function (wnd) {
                    //var veloParam = wnd.down('#ntfVeloMax');
                    //if (veloParam.validate()) {
                    return true;
                    //}
                    //return {
                    //    errorTitle: tr('rules.nooparam'),
                    //    errorMsg: tr('rules.nooparam.msg')
                    //};
                },
                format: function (value, grid) {
                    return (tr('rules.maintenance') + ' ' + value['ntfDistMax'] + ' ' + tr('mapobject.laststate.kmsperhour') + ' ' + value['ntfMotoMax'] + tr('units.hour'));
                }
            }
        })(),
        ntfNoMsg: {
            id: 'ntfNoMsg',
            name: tr('rules.grid.type.nomessages'),
            msgMask: tr('rules.ntfmasks.nomessages'),
            formContent: [
                {
                    xtype: 'numberfield',
                    itemId: 'ntfNoMsgHours',
                    fieldLabel: tr('rules.nomessagehours'),
                    allowBlank: false,
                    hideTrigger: true,
                    labelWidth: 180,
                    minValue: 1,
                    value: 8,
                    padding: '8 8 4 8'
                }
            ],
            getParams: function(wnd) {
                var ntfNoMsgParam = wnd.down('#ntfNoMsgHours');
                return {
                    interval: (ntfNoMsgParam.getValue() * 1000 * 60 * 60)
                };
            },
            setParams: function(params, wnd) {
                wnd.down('#ntfNoMsgHours').setValue(params['interval'] / (1000*60*60));
            },
            validate: function(wnd) {
                var ntfNoMsgParam = wnd.down('#ntfNoMsgHours');
                if (ntfNoMsgParam.validate()) {
                    return true;
                }
                return {
                    errorTitle: tr('rules.nooparam'),
                    errorMsg: tr('rules.selecttimelimit.msg')
                };
            },
            format: function(value, grid) {
                return tr('rules.grid.type.nomessages') + ' ' + value['interval'] / (1000 * 60 * 60) + ' ' + tr('units.hour');
            }
        },
        ntfLongParking: {
            id: 'ntfLongParking',
            name: tr('rules.grid.type.longparking'),
            msgMask: tr('rules.ntfmasks.longparking'),
            formContent: [
                {
                    xtype: 'numberfield',
                    itemId: 'ntfLongParkingHours',
                    fieldLabel: tr('rules.longparkinghours'),
                    allowBlank: false,
                    hideTrigger: true,
                    labelWidth: 180,
                    minValue: 1,
                    value: 8,
                    padding: '8 8 4 8'
                }
            ],
            getParams: function(wnd) {
                var ntfLongParkingParam = wnd.down('#ntfLongParkingHours');
                return {
                    interval: (ntfLongParkingParam.getValue() * 1000 * 60 * 60)
                };
            },
            setParams: function(params, wnd) {
                wnd.down('#ntfLongParkingHours').setValue(params['interval'] / (1000*60*60));
            },
            validate: function(wnd) {
                var ntfLongParkingParam = wnd.down('#ntfLongParkingHours');
                if (ntfLongParkingParam.validate()) {
                    return true;
                }
                return {
                    errorTitle: tr('rules.nooparam'),
                    errorMsg: tr('rules.selecttimelimit.msg')
                };
            },
            format: function(value, grid) {
                return tr('rules.grid.type.longparking') + ' ' + value['interval'] / (1000 * 60 * 60) + ' ' + tr('units.hour');
            }
        },
        ntfBadFuel: {
            id: 'ntfBadFuel',
            name: tr('rules.grid.type.badfuel'),
            msgMask: tr('rules.ntfmasks.badfuel'),
            formContent: [
                {
                    xtype: 'numberfield',
                    itemId: 'ntfBadFuelCount',
                    fieldLabel: tr('rules.ntfbadfuelcount'),
                    allowBlank: false,
                    hideTrigger: true,
                    labelWidth: 180,
                    minValue: 1,
                    value: 5,
                    padding: '8 8 4 8'
                }
            ],
            getParams: function(wnd) {
                var veloParam = wnd.down('#ntfBadFuelCount');
                return {
                    ntfBadFuelCount: veloParam.getValue()
                };
            },
            setParams: function(params, wnd) {
                wnd.down('#ntfBadFuelCount').setValue(params['ntfBadFuelCount']);
            },
            validate: function(wnd) {
                var veloParam = wnd.down('#ntfBadFuelCount');
                if (veloParam.validate()) {
                    return true;
                }
                return {
                    errorTitle: tr('rules.nooparam'),
                    errorMsg: tr('rules.nooparam.msg')
                };
            },
            format: function(value, grid) {
                return (tr('rules.grid.type.badfuel'));
            }
        }
    }
});

//    Notification rule type item must be like following:
//    ###: {
//        id: '###',
//        name: tr('rules.grid.type.###'),
//        msgMask: tr('rules.ntfmasks.###'),
//        formContent: [
//            {
//                ...
//            }
//        ],
//        getParams: function(wnd) {
//            ...
//        },
//        setParams: function(params, wnd) {
//            ...
//        },
//        validate: function(wnd) {
//            ...
//        },
//        format: function(value, grid) {
//            ...
//        }
//    }
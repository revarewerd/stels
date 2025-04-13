Ext.define('Seniel.view.mapobject.SettingsWindow', {
    extend: 'Seniel.view.WRWindow',
    requires: [
        'Seniel.view.mapobject.ImagesView',
        'Seniel.view.mapobject.SettingsSensorsGrid',
        'Seniel.view.WRRadioGroup',
        'Seniel.view.WRWindow'
    ],
    alias: 'widget.objsetwnd',
    stateId: 'objSetWnd',
    stateful: true,
    icon: 'images/ico16_options.png',
    title: tr('settingswidow.title'),
    width: 720,
    height: 460,
    minWidth: 480,
    minHeight: 400,
    layout: 'fit',
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
                    title: tr('settingswidow.objviewing'),
                    itemId: 'objMapImage',
                    xtype: 'panel',
                    closable: false,
                    layout: {
                        type: 'vbox',
                        align: 'stretch'
                    },
                    items: [
                        {
                            xtype: 'textfield',
                            name: 'customName',
                            itemId:'customName',
                            fieldLabel: tr('settingswidow.objviewing.grid,customName'),
                            labelWidth: 220,
                            labelPad: 10,
                            padding: '8 8 -4 8',
                            regex:/^[\S]+.*[\S]+$/i,
                            listeners: {
                                change: function(field, newVal, oldVal) {
                                    if (oldVal) {
                                        field.up('objsetwnd').down('toolbar[dock="bottom"] button[itemId="btnApplySettings"]').enable();
                                    }
                                }
                            }
                        },
                        {
                            xtype: 'form',
                            defaults: {
                                labelWidth: 220,
                                labelPad: 10
                            },
                            minHeight: 204,
                            border: false,
                            padding: 8,
                            items: [
                                {
                                    xtype: 'displayfield',
                                    name: 'imgSource',
                                    fieldLabel: tr('settingswidow.objviewing.grid,currentimg'),
                                    value: 'images/cars/car_rot_swc_32.png',
                                    renderer: function(val, field) {
                                        return '<div class="object-images"><div class="object-images-current"><img src="' + val + '" alt="'+tr('settingswidow.objviewing.grid,currentimg.alt')+'" /></div></div>';
                                    }
                                },
                                {
                                    xtype: 'wrradiogroup',
                                    itemId: 'imgSize',
                                    fieldLabel: tr('settingswidow.objviewing.grid,imgsize'),
                                    labelPad: 6,
                                    initItemsConfig: [
                                        {boxLabel: '24x24', inputValue: 24, padding: '0 12 0 0'},
                                        {boxLabel: '32x32', inputValue: 32, padding: '0 12 0 0'},
                                        {boxLabel: '40x40', inputValue: 40, padding: '0 12 0 0'}
                                    ],
                                    listeners: {
                                        change: function(rg, newVal) {
                                            var panel = rg.up('panel[itemId="objMapImage"]'),
                                                imgView = panel.down('objimgview'),
                                                isMarker = panel.down('checkbox[name="imgMarker"]').getValue(),
                                                isRotate = panel.down('checkbox[name="imgRotate"]').getValue();
                                            
                                            if (imgView.getStore().getTotalCount()) {
                                                imgView.filterView(newVal[rg.rgName], isMarker, isRotate);
                                            }
                                        }
                                    }
                                },
                                {
                                    xtype: 'checkbox',
                                    name: 'imgMarker',
                                    boxLabel: tr('settingswidow.objviewing.grid,usemarker'),
                                    checked: false,
                                    listeners: {
                                        change: function(cb, newVal) {
                                            var panel = cb.up('panel[itemId="objMapImage"]'),
                                                imgView = panel.down('objimgview'),
                                                rgSize = panel.down('radiogroup'),
                                                isRotate = cb.next().getValue();
                                            
                                            if (newVal) {
                                                cb.next().disable();
                                            } else {
                                                cb.next().enable();
                                            }
                                            
                                            if (imgView.getStore().getTotalCount()) {
                                                imgView.filterView(rgSize.getRealValue(), newVal, isRotate);
                                            }
                                        }
                                    }
                                },
                                {
                                    xtype: 'checkbox',
                                    name: 'imgRotate',
                                    boxLabel: tr('settingswidow.objviewing.grid,imgrotate'),
                                    checked: true,
                                    listeners: {
                                        change: function(cb, newVal) {
                                            var panel = cb.up('panel[itemId="objMapImage"]'),
                                                imgView = panel.down('objimgview'),
                                                rgSize = panel.down('radiogroup'),
                                                isMarker = panel.down('checkbox[name="imgMarker"]').getValue();
                                            
                                            if (imgView.getStore().getTotalCount()) {
                                                imgView.filterView(rgSize.getRealValue(), isMarker, newVal);
                                            }
                                        }
                                    }
                                },
                                {
                                    xtype: 'checkbox',
                                    name: 'imgArrow',
                                    boxLabel: tr('settingswidow.objviewing.grid,showdirection'),
                                    listeners: {
                                        change: function(cb, newVal, oldVal) {
                                            var form = cb.up('form'),
                                                rec = form.getRecord();

                                            if (newVal !== rec.get('imgArrow')) {
                                                rec.set('imgArrow', newVal);
                                                form.updateRecord(rec);
                                                cb.up('objsetwnd').down('toolbar[dock="bottom"] button[itemId="btnApplySettings"]').enable();
                                            }
                                                                                        
                                            if (!newVal) {
                                                cb.next().hide();
                                            } else {
                                                cb.next().show();
                                            }
                                        }
                                    }
                                },
                                {
                                    xtype: 'combobox',
                                    name: 'imgArrowSrc',
                                    fieldLabel: tr('settingswidow.objviewing.grid,directionpointer'),
                                    listConfig: {
                                        cls: 'object-images-arrows-list',
                                        width: 170
                                    },
//                                    editable: false,
                                    queryMode: 'local',
                                    width: 280,
                                    triggerCls: 'x-form-arrow-trigger object-images-arrows-trigger',
                                    hideMode: 'display',
                                    hidden: true,
                                    valueField: 'value',
                                    displayField: 'value',
                                    matchFieldWidth: false,
                                    tpl: '<tpl for="."><div class="x-boundlist-item" ><img src="images/{value}" alt="" /></div></tpl>',
                                    fieldSubTpl: ['<div class="{hiddenDataCls}" role="presentation"></div>', '<div class="object-images-arrows-list-output">', '<tpl if="value"><img src="{[Ext.util.Format.htmlEncode(values.value)]}" alt="" /></tpl>', '</div><input id="{id}" type="{type}" {inputAttrTpl} class="{fieldCls} {typeCls} {editableCls}" autocomplete="off" style="display: none;"', '<tpl if="value"> value="{[Ext.util.Format.htmlEncode(values.value)]}"</tpl>', '<tpl if="name"> name="{name}"</tpl>', '<tpl if="placeholder"> placeholder="{placeholder}"</tpl>', '<tpl if="size"> size="{size}"</tpl>', '<tpl if="maxLength !== undefined"> maxlength="{maxLength}"</tpl>', '<tpl if="readOnly"> readonly="readonly"</tpl>', '<tpl if="disabled"> disabled="disabled"</tpl>', '<tpl if="tabIdx"> tabIndex="{tabIdx}"</tpl>', '<tpl if="fieldStyle"> style="{fieldStyle}"</tpl>', '/>', {compiled: true, disableFormats: true}],
                                    listeners: {
                                        beforerender: function(cb) {
                                            Ext.apply(cb, {
                                                store: new Ext.data.ArrayStore({
                                                    autoLoad: true,
                                                    fields: ['value'],
                                                    data: {
                                                        'items': [
                                                            {value: 'arrow_sgr_01.png'},
                                                            {value: 'arrow_sgr_02.png'},
                                                            {value: 'arrow_sgr_03.png'},
                                                            {value: 'arrow_sgr_04.png'},
                                                            {value: 'arrow_sgr_05.png'},
                                                            {value: 'arrow_sgr_06.png'},
                                                            {value: 'arrow_sor_01.png'},
                                                            {value: 'arrow_sor_02.png'},
                                                            {value: 'arrow_sor_03.png'},
                                                            {value: 'arrow_sor_04.png'},
                                                            {value: 'arrow_sor_05.png'},
                                                            {value: 'arrow_sor_06.png'}
                                                        ]
                                                    },
                                                    proxy: {
                                                        type: 'memory',
                                                        reader: {
                                                            type: 'json',
                                                            root: 'items'
                                                        }
                                                    }
                                                })
                                            });
                                        },
                                        boxready: function() {
                                            if (this.inputCell) {
                                                this.inputCell.dom.childNodes[1].innerHTML = '<div><img src="images/' + this.value + '" alt="" /></div>';
                                            }
                                        },
                                        change: function(cb, newVal, oldVal) {
//                                            console.log('Change event fired!');
//                                            var form = cb.up('form'),
//                                                rec = form.getRecord();
//                                            rec.set('imgArrowSrc', newVal);
//                                            form.updateRecord(rec);
                                            if (oldVal) {
                                                var form = cb.up('form'),
                                                    rec = form.getRecord();
                                                rec.set('imgArrowSrc', newVal);
                                                form.updateRecord(rec);
                                                
                                                cb.up('objsetwnd').down('toolbar[dock="bottom"] button[itemId="btnApplySettings"]').enable();
                                            }
                                            
                                            if (cb.inputCell) {
                                                cb.inputCell.dom.childNodes[1].innerHTML = '<div><img src="images/' + newVal + '" alt="" /></div>';
                                            }
                                        }
                                    }
                                }
                            ]
                        },
                        {
                            xtype: 'panel',
                            flex: 1,
                            autoScroll: true,
                            cls: 'object-images',
                            title: tr('settingswidow.imglib'),
                            collapsible: false,
                            bodyPadding: '4 0 4 4',
                            border: 0,
                            items: [
                                {
                                    xtype: 'objimgview'
                                }
                            ]
                        }
                    ]
                },
                {
                    title: tr('settingswindow.trips'),
                    itemId: 'tripsProperties',
                    xtype: 'panel',
                    closable: false,
                    layout: 'fit',
                    items: [
                        {
                            xtype: 'form',
                            defaults: {
                                xtype: 'numberfield',
                                anchor: '100%',
                                hideTrigger: true,
                                labelWidth: 280,
                                labelPad: 10,
                                padding: '4 8 4 8',
                                allowBlank: false,
                                minValue: 0,
                                listeners: {
                                    change: function(field, newVal, oldVal) {
                                        var form = field.up('form'),
                                            rec = form.getRecord();
                                        rec.set(field.getName(), newVal);
                                        form.updateRecord(rec);

                                        if (form.initRecord) {
                                            field.up('objsetwnd').down('#btnApplySettings').enable();
                                        }
                                    }
                                }
                            },
                            layout: 'anchor',
                            autoScroll: true,
                            border: false,
                            //     lazy val minMovementSpeed = 1
                            // lazy val minParkingTime = 300
                            // lazy val maxDistanceBetweenMessages = 10000
                            // lazy val minTripTime = 360
                            // lazy val minTripDistance = 500
                            // lazy val useIgnitionToDetectMovement = false
                            // lazy val minSatelliteNum = 3
                            items: [
                                {
                                    name: 'repTripsMinMovementSpeed',
                                    minValue: 1,
                                    fieldLabel: tr('settingswidow.minMovementSpeed'),
                                    padding: '8 8 4 8'
                                },
                                {
                                    name: 'repTripsMinParkingTime',
                                    minValue: 1,
                                    fieldLabel: tr('settingswidow.minParkingTime')
                                },
                                {
                                    name: 'repTripsMaxDistanceBetweenMessages',
                                    minValue: 50,
                                    fieldLabel: tr('settingswidow.maxDistanceBetweenMessages')
                                },
                                {
                                    name: 'repTripsMinTripTime',
                                    fieldLabel: tr('settingswidow.minTripTime')
                                },
                                {
                                    name: 'repTripsMinTripDistance',
                                    fieldLabel: tr('settingswidow.minTripDistance')
                                },
                                {
                                    name: 'repTripsMinSatelliteNum',
                                    fieldLabel: tr('settingswidow.minSatelliteNum')
                                },
                                {
                                    xtype: 'checkbox',
                                    name: 'repTripsUseIgnitionToDetectMovement',
                                    fieldLabel: tr('settingswindow.useIgnitionToDetectMovement')
                                }
                            ]

                        }




                    ]
                },
                {
                    title: tr('settingswidow.fuel'),
                    itemId: 'objProperties',
                    xtype: 'panel',
                    closable: false,
                    layout: 'fit',
                    items: [
                        {
                            xtype: 'form',
                            defaults: {
                                xtype: 'numberfield',
                                anchor: '100%',
                                hideTrigger: true,
                                labelWidth: 280,
                                labelPad: 10,
                                padding: '4 8 4 8',
                                minValue: 0,
                                listeners: {
                                    change: function(field, newVal, oldVal) {
                                        var form = field.up('form'),
                                            rec = form.getRecord();
                                        rec.set(field.getName(), newVal);
                                        form.updateRecord(rec);
                                        
                                        if (form.initRecord) {
                                            field.up('objsetwnd').down('#btnApplySettings').enable();
                                        }
                                    }
                                }
                            },
                            layout: 'anchor',
                            autoScroll: true,
                            border: false,
                            items: [
                                {
                                    name: 'repFuelFullVolume',
                                    fieldLabel: tr('settingswidow.fuelvolume'),
                                    padding: '8 8 4 8'
                                },
                                {
                                    name: 'repFuelMinFueling',
                                    fieldLabel: tr('settingswidow.minfuelvolume')
                                },
                                // {
                                //     name: 'repFuelRefuelingTimeout',
                                //     fieldLabel: tr('settingswindow.refuelingTimeout')
                                // },
                                {
                                    name: 'repFuelIgnoreMessagesAfterMoving',
                                    fieldLabel: tr('settingswindow.ignoreMessagesAfterMoving')
                                },

                                // {
                                //     name: 'repFuelMinDraining',
                                //     fieldLabel: tr('settingswidow.minfueldrain')
                                // },
                                // {
                                //     name: 'repFuelMinTime',
                                //     fieldLabel: tr('settingswidow.minfueldraintime')
                                // },
                                {
                                    name: 'repFuelFiltering',
                                    fieldLabel: tr('settingswidow.averagesensordata')
                                },
                                {
                                    xtype: 'checkbox',
                                    name: 'repFuelIsNoFiltering',
                                    fieldLabel: tr('settingswindow.isnofiltering')
                                },
                                {
                                    xtype: 'checkbox',
                                    name: 'repFuelUseCalcStandards',
                                    fieldLabel: tr('settingswindow.fuelaveragenorms'),
                                    labelStyle: 'font-weight: bold;'
                                },
                                {
                                    name: 'repFuelNormIdling',
                                    fieldLabel: tr('settingswindow.fuelaveragenorms.idling')
                                },
                                {
                                    name: 'repFuelNormUrban',
                                    fieldLabel: tr('settingswindow.fuelaveragenorms.urban')
                                },
                                {
                                    name: 'repFuelNormXUrban',
                                    fieldLabel: tr('settingswindow.fuelaveragenorms.extraurban')
                                },
                                {
                                    name: 'repFuelNormKLoad',
                                    fieldLabel: tr('settingswindow.fuelaveragenorms.loadfactor')
                                },
                                {
                                    xtype: 'checkbox',
                                    name: 'repFuelUseBasicStandards',
                                    fieldLabel: tr('settingswindow.fuelcalendarnorms'),
                                    labelStyle: 'font-weight: bold;'
                                },
                                {
                                    name: 'repFuelNormWinter',
                                    fieldLabel: tr('settingswindow.fuelcalendarnorms.winter')
                                },
                                {
                                    name: 'repFuelNormSummer',
                                    fieldLabel: tr('settingswindow.fuelcalendarnorms.summer')
                                }
                            ]
                        }
                    ]
                },
                {
                    title: tr('settingswidow.maintenance'),
                    itemId: 'objMaintenance',
                    xtype: 'panel',
                    closable: false,
                    layout: 'fit',
                    items: [
                        {
                            xtype: 'form',
                            defaults: {
                                xtype: 'numberfield',
                                anchor: '100%',
                                hideTrigger: true,
                                labelWidth: 100,
                                labelPad: 10,
                                padding: '4 8 4 8',
                                minValue: 0,
                                listeners: {
                                    change: function(field, newVal, oldVal) {
                                        var form = field.up('form'),
                                            rec = form.getRecord();
                                        if(rec) {
                                            rec.set(field.getName(), newVal);
                                            form.updateRecord(rec);
                                        }

                                        if (form.initRecord) {
                                            field.up('objsetwnd').down('#btnApplySettings').enable();
                                        }
                                    }
                                }
                            },

                            loadRecord: function (rec) {
                                console.log("loadRecord", rec, arguments);
                                var o = Ext.form.Panel.prototype.loadRecord.call(this, rec);
                                var query = this.query("combobox");
                                console.log("query:", query);
                                query.forEach(function (cmb) {
                                   var prefix = cmb.up("fieldcontainer").itemId;
                                   console.log("it:", prefix);
                                   if(!rec.get(prefix + "Interval")){
                                       console.log("settingValue", prefix, rec);
                                       var record = cmb.getStore().findRecord('field1', 'default');
                                       record.set('field2', rec.get(prefix + "IntervalDefault") + ' (' + record.get('field2') + ')');
                                       cmb.setValue("default");

                                   }
                                });
                                return o;
                            },
                            layout: 'anchor',
                            autoScroll: true,
                            border: false,
                            items: function () {

                                function genMaintenance(prefix) {
                                    return {
                                        xtype: 'fieldcontainer',
                                        itemId: prefix,
                                        fieldLabel: tr("settingswidow." + prefix),
                                        defaults: {
                                            xtype: 'numberfield',
                                            anchor: '100%',
                                            hideTrigger: false,
                                            labelWidth: 280,
                                            labelPad: 10,
                                            padding: '4 8 4 8',
                                            minValue: 0,
                                            listeners: {
                                                change: function (field, newVal, oldVal) {
                                                    var form = field.up('form'),
                                                        rec = form.getRecord();
                                                    if (rec) {
                                                        rec.set(field.getName(), newVal);
                                                        form.updateRecord(rec);
                                                    }

                                                    if (form.initRecord) {
                                                        field.up('objsetwnd').down('#btnApplySettings').enable();
                                                    }
                                                }
                                            }
                                        },
                                        layout: 'vbox',
                                        items: [
                                            {
                                                name: (prefix + "Enabled"),
                                                fieldLabel: tr('Enabled'),
                                                xtype: 'checkbox',
                                                value: 'true',
                                                checked: 'true',
                                                listeners: {
                                                    'change': function (field, newVal) {
                                                        var form = field.up('form');
                                                        var rec = form.getRecord();

                                                        if(rec && !rec.get(prefix + 'RuleEnabled')){
                                                            newVal = false;
                                                            field.disable();
                                                        }


                                                        field.up('fieldcontainer').query('field').forEach(function (e) {
                                                            console.log("e:", e, field);
                                                            if (e != field)
                                                                e.setDisabled(!newVal);
                                                        });


                                                        if (rec) {
                                                            rec.set(field.getName(), newVal);
                                                            form.updateRecord(rec);
                                                        }

                                                        if (form.initRecord) {
                                                            field.up('objsetwnd').down('#btnApplySettings').enable();
                                                        }

                                                    }
                                                }
                                            },
                                            {
                                                name: (prefix + 'Until'),
                                                fieldLabel: tr('settingswidow.' + prefix + 'Until'),
                                                padding: '8 8 4 8',
                                                resetMnt: function (field) {
                                                    var form = field.up('form');
                                                    var rec = form.getRecord();
                                                    if (rec) {
                                                        var interval = rec.get(prefix + 'Interval') || rec.get(prefix + 'IntervalDefault');
                                                        field.setValue(interval)
                                                    }

                                                }
                                            },

                                            {
                                                name: (prefix + 'Interval'),
                                                fieldLabel: tr('settingswidow.' + prefix + 'Interval'),
                                                xtype: 'combobox',
                                                forceSelection: false,
                                                store: [
                                                    ['default', 'По умолчанию ']
                                                ]
                                            }
                                        ]
                                    }
                                }

                                return [
                                    {
                                        fieldLabel: tr('maintenance.reset'),
                                        xtype: 'button',
                                        text: tr('maintenance.reset'),
                                        handler: function(btn) {
                                            btn.up('form').query('field').forEach(function (f) {
                                                if(f.resetMnt)
                                                    f.resetMnt(f);
                                            })

                                        }
                                    },
                                    genMaintenance('distance'),
                                    genMaintenance('motohours'),
                                    genMaintenance('hours')
                                ]
                            }()
                        }
                    ]
                },
                {
                    title: tr('main.reports.sensors'),
                    itemId: 'objSensors',
                    xtype: 'panel',
                    closable: false,
                    layout: 'fit',
                    items: [
                        {
                            xtype: 'settingssensorsgrid'
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
                    text: tr('main.apply'),
                    itemId: 'btnApplySettings',
                    disabled: true,
                    handler: function(btn) {
                        var wnd = btn.up('objsetwnd');
                        if (wnd.saveSettings()) {
                            this.disable();
                        }
                    }
                },
                {
                    xtype: 'button',
                    text: tr('main.ok'),
                    itemId: 'btnOk',
                    icon: 'images/ico16_okcrc.png',
                    handler: function(btn) {
                        var wnd = btn.up('objsetwnd');
                        if (wnd.saveSettings()) {
                            wnd.close();
                        }
                    }
                },
                {
                    xtype: 'button',
                    text: tr('main.cancel'),
                    itemId: 'btnCancel',
                    icon: 'images/ico16_cancel.png',
                    handler: function(btn) {
                        var wnd = btn.up('objsetwnd');
                        wnd.close();
                    }
                }
            ]
        }
    ],
    initComponent: function() {
        this.callParent(arguments);
        
        this.on('boxready', function() {
            this.loadSettings();
            
            if (this.objSettings && this.objSettings.hideSaveButtons) {
                this.down('#btnApplySettings').hide();
                this.down('#btnOk').hide();
            }
            if (this.objSettings && this.objSettings.hideFuelSettings) {
                this.down('tabpanel').child('#objProperties').tab.hide();
            }
            if (this.objSettings && this.objSettings.hideSensorSettings) {
                this.down('tabpanel').child('#objSensors').tab.hide();
            }
            console.log('Settings window items = ', this.down('tabpanel'));
        });
    },

    //---------
    // Функциии
    //---------
    loadSettings: function() {
        var rgImgSize = this.down('radiogroup[itemId="imgSize"]');
        var os = this.objSettings;
        var or = this.objRecords[0];

        if (!os.imgRotate && os.imgSource) {
            os.imgRotate = (os.imgSource.search(/rot/) > -1)?(true):(false);
        }

        if (!os.imgSize && os.imgSource) {
            var start = os.imgSource.search(/_\d\d\.png/);
            os.imgSize = os.imgSource.substring(start + 1).substring(0, 2);
        }
        os.imgSize = (os.imgSize)?(Number(os.imgSize)):(os.imgSize);

        if (!os.imgArrowSrc) {
            os.imgArrowSrc = undefined;
        }

        var imgRec = Ext.create('Seniel.view.mapobject.SettingsViewModel', os),
            frmImage = this.down('#objMapImage form');
        frmImage.loadRecord(imgRec);
        frmImage.initRecord = imgRec;

        var tripRec = Ext.create('Seniel.view.mapobject.SettingsTripsModel', os),
            frmTrips = this.down('#tripsProperties form');

        frmTrips.loadRecord(tripRec);
        frmTrips.initRecord = imgRec;

        var cmnRec = Ext.create('Seniel.view.mapobject.SettingsCommonModel', os),
            frmCommon = this.down('#objProperties form');

        frmCommon.loadRecord(cmnRec);
        frmCommon.initRecord = cmnRec;

        var uid = or.get('uid');
        var frmMnt = this.down('#objMaintenance form');
        maintenanceService.getMaintenanceState(uid, function(res){
            if(res != null) {
                console.log("retrived maintenance state:", res);
                var mntRec = Ext.create('Seniel.view.mapobject.SettingsMaintenanceModel', res);
                frmMnt.loadRecord(mntRec);
                frmMnt.initRecord = mntRec;
            } else {
                var container = frmMnt.up('#objMaintenance');
                container.up('tabpanel').remove(container);
            }
        });

        rgImgSize.setRealValue(imgRec.get('imgSize'));

        var imgView = this.down('objimgview');
        if (imgView) {
            imgView.initItem = {
                size: imgRec.get('imgSize'),
                src: imgRec.get('imgSource'),
                isMarker: imgRec.get('imgMarker'),
                isRotate: imgRec.get('imgRotate')
            };
        }
        

        var sensorsGrid = this.down('settingssensorsgrid');
        objectSettings.loadObjectSensors(uid, function(resp, request) {
            if (resp && resp.length > 0 && sensorsGrid) {
                sensorsGrid.getStore().add(resp);
            }
        });

        var customNameField  = this.down('#customName');
        if (customNameField) {
            customNameField.setValue(Ext.String.htmlDecode(or.get('name')));
        }
    },
    saveSettings: function() {
        var wnd = this;
        var frmImage = wnd.down('#objMapImage').down('form');
        var frmCommon = wnd.down('#objProperties').down('form');
        var frmTrips = wnd.down('#tripsProperties form')
        var frmMaintenance = wnd.down('#objMaintenance') != null ? wnd.down('#objMaintenance').down('form') : null;
        var customNameField = wnd.down('#customName');

        if(!customNameField.isValid()){
            Ext.MessageBox.show({
                title: tr('settingswindow.invalidform'),
                msg: tr('settingswindow.invalidCustomName'),
                icon: Ext.MessageBox.WARNING,
                buttons: Ext.Msg.OK
            });
            return false;
        }
        if (!frmCommon.isValid()) {
            Ext.MessageBox.show({
                title: tr('settingswindow.invalidform'),
                msg: tr('settingswindow.invalidform.msg'),
                icon: Ext.MessageBox.WARNING,
                buttons: Ext.Msg.OK
            });
            return false;
        }
        
        var settings = frmImage.getRecord().data,
            params = {};
        var settingsAdd = frmCommon.getRecord().data;
        var settingsTrips = frmTrips.getRecord().data;
        var settingsMaintenance = frmMaintenance != null ? frmMaintenance.getRecord().data : null;
        console.log("settingsMaintenance:", settingsMaintenance);
        var sensorsGrid = wnd.down('settingssensorsgrid');
        
        for (var i in settingsAdd) {
            settings[i] = settingsAdd[i];
        }

        for (var i in settingsTrips) {
            settings[i] = settingsTrips[i];
        }

        console.log('Object settings data = ', settings);
        if (sensorsGrid) {
            params.sensors = [];
            sensorsGrid.getStore().each(function(rec) {
                params.sensors.push(rec.getData());
            });
        }
        
        if (/*settings && */wnd.objRecords[0]) {
            for (var i = 0; i < wnd.objRecords.length; i++) {
                if (!customNameField.isDisabled() && i === 0) {
                    params.customName = customNameField.getValue();
                    wnd.updateObjName(customNameField.getValue(), wnd.objRecords[i]);
                }
                objectSettings.saveObjectSettings(wnd.objRecords[i].get('uid'), settings, params);
                if (settingsMaintenance != null)
                    maintenanceService.saveSettings(wnd.objRecords[i].get('uid'), settingsMaintenance);
                wnd.updateObjImage(settings, wnd.objRecords[i]);
            }
        }

        console.log('Object params = ', params);
        
        return true;
    },
    updateObjImage: function(settings, rec) {
        rec.set('imgSource', settings.imgSource);
        rec.set('imgRotate', settings.imgRotate);
        rec.set('imgArrow', settings.imgArrow);
        rec.set('imgArrowSrc', settings.imgArrowSrc);
        if (rec.get('checked')) {
            var map = this.up('viewport').down('mainmap');
            map.moveMarker(rec, rec.get('lon'), rec.get('lat'));
        }
    },
    updateObjName: function(name, rec) {
        rec.set('name', name);
        var roData = this.up('viewport').down('mapobjectlist').roData;
        var mainMap = this.up('viewport').down('mainmap');
        if (roData && roData.length) {
            for (var i = 0; i < roData.length; i++) {
                if (roData[i].uid === rec.get('uid')) {
                    roData[i].name = name;
                    break;
                }
            }
        }
        if (rec.get('checked')) {
            var marker = mainMap.mapObjects[rec.get('uid')];
            marker.style.label = name;
        }
    }

});
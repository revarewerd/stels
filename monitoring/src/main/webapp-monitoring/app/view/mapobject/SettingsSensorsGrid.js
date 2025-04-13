Ext.define('Seniel.view.mapobject.SettingsSensorsGrid', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.settingssensorsgrid',
    stateId: 'sensorsGrid',
    stateful: true,
    tbar: [
        {
            icon: 'images/ico16_plus_def.png',
            itemId: 'btnAddRecord',
            text: tr('settingssensors.addsensor.tooltip'),
            tooltip: tr('settingssensors.addsensor'),
            tooltipType: 'title',
            handler: function(btn) {
                var wnd = btn.up('window');
                var viewport = wnd.up('viewport');
                var uid = wnd.objRecords[0].get('uid');
                var grid = wnd.down('settingssensorsgrid');
                
                if (!btn.openedWnd) {
                    btn.openedWnd = Ext.create('Seniel.view.mapobject.SettingsSensorsWindow', {
                        objUid: uid,
                        linkedGrid: grid
                    });
                    
                    btn.openedWnd.on('close', function(){
                        delete btn.openedWnd;
                    });
                    
                    viewport.showNewWindow(btn.openedWnd);
                } else {
                    btn.openedWnd.setActive(true);
                }
            }
        },
        {
            icon: 'images/ico16_edit_def.png',
            itemId: 'btnEditRecord',
            text: tr('settingssensors.editsensor'),
            tooltip: tr('settingssensors.editsensor.tooltip'),
            tooltipType: 'title',
            disabled: true,
            handler: function(btn) {
                var wnd = btn.up('window');
                var viewport = wnd.up('viewport');
                var uid = wnd.objRecords[0].get('uid');
                var grid = wnd.down('settingssensorsgrid');
                var rec = grid.getSelectionModel().getSelection()[0];
                
                if (rec && !rec.get('openedWnd')) {
                    var wnd = Ext.create('Seniel.view.mapobject.SettingsSensorsWindow', {
                        objUid: uid,
                        linkedGrid: grid,
                        editRecord: rec,
                        title: tr('settingssensors.editing')+' &laquo;'+rec.get('name')+'&raquo;',
                        btnConfig: {
                            icon: 'images/ico24_bell.png',
                            text: tr('settingssensors.editing')+' &laquo;'+rec.get('name')+'&raquo;'
                        }
                    });
                    
                    rec.set('openedWnd', wnd);
                    wnd.on('close', function(){
                        rec.set('openedWnd', null);
                    });
                    
                    viewport.showNewWindow(wnd);
                } else {
                    rec.get('openedWnd').setActive(true);
                }
            }
        },
        {
            icon: 'images/ico16_signno.png',
            itemId: 'btnDeleteRecord',
            text: tr('settingssensors.removesensor'),
            tooltip: tr('settingssensors.removesensor.tooltip'),
            tooltipType: 'title',
            disabled: true,
            handler: function(btn) {
                var grid = btn.up('window').down('grid');
                var rec = grid.getSelectionModel().getSelection()[0];
                
                Ext.MessageBox.show({
                    title: tr('settingssensors.sensorremoving'),
                    msg: tr('settingssensors.sensorremoving.sure')+' &laquo;'+rec.get('name')+'&raquo;?',
                    icon: Ext.MessageBox.QUESTION,
                    buttons: Ext.Msg.YESNO,
                    fn: function(a) {
                        if (a === 'yes') {
                            grid.getSelectionModel().deselect(rec);
                            grid.getStore().remove(rec);
                            var btn = grid.up('window').down('toolbar #btnApplySettings');
                            if (btn && btn.isDisabled) {
                                btn.enable();
                            }
                        }
                    }
                });
            }
        }
    ],
    viewConfig: {
        markDirty: false
    },
    store: {
        fields: ['name', 'type', 'paramName', 'unit', 'comment', 'ratio', 'dataTable', 'minValue', 'maxValue', {name: 'showInInfo', defaultValue: true}],
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
    rowLines: true,
    border: false,
    padding: false,
    layout: 'fit',
    columns: [
        {
            stateId: 'name',
            text: tr('settingssensors.grid.name'),
            dataIndex: 'name',
            flex: 1
        },
        {
            stateId: 'type',
            text: tr('settingssensors.grid.type'),
            dataIndex: 'type',
            flex: 1,
            renderer: function(val, metaData, rec) {
                switch (val) {
                    case 'sFuelL': val = tr('settingssensors.grid.type.fuellevel'); break;
                    case 'sTmp': val = tr('settingssensors.grid.type.temperature'); break;
                    case 'sEngS': val = tr('settingssensors.grid.type.enginespeed'); break;
                    case 'sIgn': val = tr('settingssensors.grid.type.ignition'); break;
                    case 'sPwr': val = tr('settingssensors.grid.type.powervoltage'); break;
                    default: val = tr('settingssensors.grid.type.unknown');
                }
                return val;
            }
        },
        {
            stateId: 'unit',
            text: tr('settingssensors.grid.unit.dim'),
            dataIndex: 'unit',
            flex: 1
        },
        {
            stateId: 'param',
            text: tr('settingssensors.grid.paramName'),
            dataIndex: 'paramName',
            flex: 1
        },
        {
            stateId: 'edit',
            menuText: tr('settingssensors.grid.sensorediting'),
            xtype: 'actioncolumn',
            width: 20,
            menuDisabled: true,
            sealed: true,
            sortable: false,
            resizable: false,
            items: [
                {
                    icon: 'images/ico16_edit_def.png',
                    handler: function(grid, rowIndex, colIndex, item, e, rec) {
                        var wnd = grid.up('window');
                        var viewport = wnd.up('viewport');
                        var uid = wnd.objRecords[0].get('uid');

                        if (!rec.get('openedWnd')) {
                            var wnd = Ext.create('Seniel.view.mapobject.SettingsSensorsWindow', {
                                objUid: uid,
                                linkedGrid: grid,
                                editRecord: rec,
                                title: tr('settingssensors.editing') + ' &laquo;'+rec.get('name')+'&raquo;',
                                btnConfig: {
                                    icon: 'images/ico24_bell.png',
                                    text: tr('settingssensors.editing') +' &laquo;'+rec.get('name')+'&raquo;'
                                }
                            });
                            
                            rec.set('openedWnd', wnd);
                            wnd.on('close', function() {
                                rec.set('openedWnd', null);
                            });
                            
                            viewport.showNewWindow(wnd);
                        } else {
                            rec.get('openedWnd').setActive(true);
                        }
                    },
                    getClass: function(val, metaData) {
                        metaData.tdAttr = 'title="'+tr('settingssensors.editsensor')+'"';
                        return 'object-list-button';
                    }
                }
            ]
        },
        {
            stateId: 'delete',
            menuText: tr('settingssensors.sensorsremoving'),
            xtype: 'actioncolumn',
            width: 20,
            menuDisabled: true,
            sealed: true,
            sortable: false,
            resizable: false,
            items: [
                {
                    icon: 'images/ico16_signno.png',
                    handler: function(grid, rowIndex, colIndex, item, e, rec) {
                        Ext.MessageBox.show({
                            title: tr('settingssensors.sensorremoving'),
                            msg: tr('settingssensors.sensorremoving.sure') + ' &laquo;'+rec.get('name')+'&raquo;?',
                            icon: Ext.MessageBox.QUESTION,
                            buttons: Ext.Msg.YESNO,
                            fn: function(a) {
                                if (a === 'yes') {
                                    grid.getSelectionModel().deselect(rec);
                                    grid.getStore().remove(rec);
                                    var btn = grid.up('window').down('toolbar #btnApplySettings');
                                    if (btn && btn.isDisabled) {
                                        btn.enable();
                                    }
                                }
                            }
                        });
                    },
                    getClass: function(val, metaData) {
                        metaData.tdAttr = 'title="'+tr('settingssensors.removesensor')+'"';
                        return 'object-list-button';
                    }
                }
            ]
        }
    ],
    listeners: {
        select: function(selection, rec, index) {
            var tbar = selection.view.up('grid').down('toolbar');
            tbar.down('#btnEditRecord').enable();
            tbar.down('#btnDeleteRecord').enable();
        },
        deselect: function(selection, rec, index) {
            var tbar = selection.view.up('grid').down('toolbar');
            tbar.down('#btnEditRecord').disable();
            tbar.down('#btnDeleteRecord').disable();
        }
    }
});
Ext.define('EditRulesGrid', {
    extend: 'Ext.grid.Panel',
    requires: [
        'Seniel.view.notifications.RulesTypes'
    ],
    alias: 'widget.editnotgrid',
    stateId: 'ntfRuleGrid',
    stateful: true,
    initComponent: function() {
        var self = this;
        Ext.apply(self, {
            store: Ext.create('EDS.store.NotificationRules', {
                autoLoad: true,
                grid: self
            })
        });
        this.callParent();
    },
    tbar: [
        {
            icon: 'images/ico16_plus_def.png',
            itemId: 'btnAddRecord',
            text: tr('rules.addrule'),
            tooltip: tr('rules.addrule.tip'),
            tooltipType: 'title',
            handler: function(btn) {
                var viewport = btn.up('viewport');
                var wnd = Ext.create('Seniel.view.notifications.RuleWindow', {});
                viewport.showNewWindow(wnd);
            }
        },
        {
            icon: 'images/ico16_edit_def.png',
            itemId: 'btnEditRecord',
            text: tr('rules.editrule'),
            tooltip: tr('rules.editrule.tip'),
            tooltipType: 'title',
            disabled: true,
            handler: function(btn) {
                var viewport = btn.up('viewport');
                var grid = btn.up('window').down('grid');
                var rec = grid.getSelectionModel().getSelection()[0];
                
                if (!rec.get('openedWnd')) {
                    console.log('Record data = ', rec.data);
                    var wnd = Ext.create('Seniel.view.notifications.RuleWindow', {
                        title: tr('rules.ruleediting') + ' &laquo;'+rec.get('name')+'&raquo;',
                        editRecord: rec,
                        btnConfig: {
                            icon: 'images/ico24_bell.png',
                            text: tr('settingssensors.editing') + ' &laquo;'+rec.get('name')+'&raquo;'
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
            }
        },
        {
            icon: 'images/ico16_signno.png',
            itemId: 'btnDeleteRecord',
            text: tr('rules.removerule'),
            tooltip: tr('rules.removerule.tip'),
            tooltipType: 'title',
            disabled: true,
            handler: function(btn) {
                var grid = btn.up('window').down('grid');
                var rec = grid.getSelectionModel().getSelection()[0];
                console.log('Selected record = ', rec);
                Ext.MessageBox.show({
                    title: tr('rules.ruleremoving'),
                    msg: tr('rules.ruleremoving.sure'),
                    icon: Ext.MessageBox.QUESTION,
                    buttons: Ext.Msg.YESNO,
                    fn: function(a) {
                        if (a === 'yes') {
                            notificationRules.delNotificationRule(rec.get('name'));
                            grid.getSelectionModel().deselect(rec);
                            grid.getStore().remove(rec);
                        }
                    }
                });
            }
        }
    ],
    viewConfig: {
        markDirty:false
    },
    rowLines: true,
    border: false,
    padding: false,
    layout: 'fit',
    columns: [
        {
            stateId: 'name',
            text: tr('rules.grid.name'),
            dataIndex: 'name',
            width: 120
        },
        {
            stateId: 'type',
            text: tr('rules.grid.type'),
            dataIndex: 'type',
            width: 160,
            renderer: function(val, metaData, rec) {
                var str = Seniel.view.notifications.RulesTypes.getRuleName(val);
                metaData.tdAttr = 'title="' + str + '"';
                return str;
            }
        },
        {
            stateId: 'params',
            text: tr('rules.params'),
            dataIndex: 'params',
            menuDisabled: true,
            sortable: false,
            resizable: false,
            width: 200,
            renderer: function(val, metaData, rec) {
                var grid = rec.store.grid;
                var str = Seniel.view.notifications.RulesTypes.formatRuleParams(rec.get('type'), val, grid);
                metaData.tdAttr = 'title="' + str + '"';
                return str;
            }
        },
        {
            stateId: 'objects',
            text: tr('rules.objects'),
            dataIndex: 'objects',
            menuDisabled: true,
            sortable: false,
            resizable: true,
            minWidth: 240,
            flex: 1,
            renderer: function(val, metaData, rec) {
                if (rec.get('allobjects')) {
                    val = tr('rules.allobjects');
                } else {
                    var arr = val,
                        viewport = rec.store.grid.up('viewport'),
                        rda = viewport.down('mapobjectlist').roData;
                    val = '';
                    for (var i = 0; i < arr.length; i++) {
                        for (var j = 0; j < rda.length; j++) {
                            if (rda[j].uid === arr[i]) {
                                if (val) val += ', ';
                                val += '&laquo;'+rda[j].name+'&raquo;';
                                break;
                            }
                        }
                    }
                }
                metaData.tdAttr = 'title="'+val+'"';
                return val;
            }
        },
        {
            stateId: 'edit',
            menuText: tr('rules.rulesediting'),
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
                        var viewport = grid.up('viewport');
                        
                        if (!rec.get('openedWnd')) {
                            console.log('Record data = ', rec.data);
                            var wnd = Ext.create('Seniel.view.notifications.RuleWindow', {
                                title: tr('rules.ruleediting') + ' &laquo;'+rec.get('name')+'&raquo;',
                                editRecord: rec,
                                btnConfig: {
                                    icon: 'images/ico24_bell.png',
                                    text: tr('rules.editing') + ' &laquo;'+rec.get('name')+'&raquo;'
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
                        metaData.tdAttr = 'title="'+tr('rules.editrule')+'"';
                        return 'object-list-button';
                    }
                }
            ]
        },
        {
            stateId: 'delete',
            menuText: tr('rules.rulesremoving'),
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
                            title: tr('rules.ruleremoving'),
                            msg: tr('rules.ruleremoving.sure'),
                            icon: Ext.MessageBox.QUESTION,
                            buttons: Ext.Msg.YESNO,
                            fn: function(a) {
                                if (a === 'yes') {
                                    notificationRules.delNotificationRule(rec.get('name'));
                                    grid.getSelectionModel().deselect(rec);
                                    grid.getStore().remove(rec);
                                }
                            }
                        });
                    },
                    getClass: function(val, metaData) {
                        metaData.tdAttr = 'title="'+tr('rules.removerule')+'"';
                        return 'object-list-button';
                    }
                }
            ]
        }
    ],
    listeners: {
        select: function(selection, rec, index) {
            var tbar = selection.view.up('window').down('toolbar');
            tbar.down('#btnEditRecord').enable();
            tbar.down('#btnDeleteRecord').enable();
        },
        deselect: function(selection, rec, index) {
            var tbar = selection.view.up('window').down('toolbar');
            tbar.down('#btnEditRecord').disable();
            tbar.down('#btnDeleteRecord').disable();
        }
    }
});


Ext.define('Seniel.view.notifications.RulesListWindow', {
    extend: 'Seniel.view.WRWindow',
    alias: 'widget.editnotwnd',
    stateId: 'ntfListWnd',
    stateful: true,
    icon: 'images/ico16_bell.png',
    title: tr('main.notificationrules.tooltip'),
    btnConfig: {
        icon: 'images/ico24_bell.png',
        text: tr('rules.rulesediting')
    },
    minHeight: 400,
    minWidth: 640,
    layout: 'fit',
    items: [
        {
            itemId: 'editNotifications',
            xtype: 'editnotgrid'
        }
    ]
});
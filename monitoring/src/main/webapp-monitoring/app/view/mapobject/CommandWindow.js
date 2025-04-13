/**
 * Created by IVAN on 13.04.2015.
 */
Ext.define('Seniel.view.mapobject.CommandWindow', {
    extend: 'Seniel.view.WRWindow',
    requires: ['Seniel.view.WRWindow'],
    title: tr('mapobject.objectactions.commands'),
    height: 125,
    width: 300,
    layout: 'fit',
    resizable: false,
    maximizable: false,
    commandPasswordNeeded: false,
    icon: 'images/ico24_messages.png',
    btnConfig: {
        icon: 'images/ico24_messages.png',
        text: tr('mapobject.objectactions.commands')
    },
    initComponent: function () {
        var self = this;
        var itemsElements = [
            {
                text: tr("main.notification"),
                icon: 'images/ico16_bell.png',
                handler: function (btn, e) {
                    var viewport = btn.up('viewport');
                    var rec = btn.up('window').record;
                    var wnd = viewport.createOnlyOneWnd('notificationWnd', 'Seniel.view.notifications.HistoryWindow', {
                        objects: [rec.get("uid")]
                    });
                    viewport.showNewWindow(wnd)
                }
            },
            {
                text: tr("mapobject.objectactions.blocking"),
                icon: 'images/ico16_lock_def.png',
                disabled: !self.record.get("blockingEnabled") || !self.record.get("canBlock"),
                handler: function (btn, e) {
                    var viewport = btn.up('viewport');
                    var rec = btn.up('window').record;
                    console.log("record", rec);
                    var wnd = viewport.createOnlyOneWnd('blockingWnd', 'Seniel.view.mapobject.BlockingWindow', {
                            title: tr("mapobject.objectactions.blocking") + ' "' + rec.get("name") + '"',
                            commandPasswordNeeded: self.commandPasswordNeeded,
                            btnConfig: {
                                icon: 'images/ico16_lock.png',
                                text: tr("mapobject.objectactions.blocking") + ' "' + rec.get("name") + '"'
                            },
                            record: rec
                        }
                    );
                    viewport.showNewWindow(wnd)
                }
            },
            {
                text: tr("mapobject.objectactions.commands.getcoords"),
                icon: 'images/ico16_target.png',
                disabled: !self.record.get("canGetCoords"),
                handler: function (btn, e) {
                    var rec = btn.up('window').record;
                    var viewport = btn.up('viewport');
                    var wnd = viewport.createOnlyOneWnd('commandWnd', 'Seniel.view.mapobject.SendCommandWindow', {
                        title: tr('mapobject.objectactions.commands.getcoords') + ' "' + rec.get("name") + '"',
                        commandPasswordNeeded: self.commandPasswordNeeded,
                        command: 'getcoords',
                        record: rec,
                        icon: 'images/ico16_target.png',
                        btnConfig: {
                            icon: 'images/ico16_target.png',
                            text: tr('mapobject.objectactions.commands.getcoords') + ' "' + rec.get("name") + '"'
                        }
                    });
                    viewport.showNewWindow(wnd);
                    //objectsCommander.sendGetCoordinatesCommand(uid, password)
                }
            },
            {
                text: tr("mapobject.objectactions.commands.restartingterminal"),
                icon: 'images/ico16_loading_def.png',
                disabled: !self.record.get("canRestartTerminal"),
                handler: function (btn, e) {
                    var rec = btn.up('window').record;
                    var viewport = btn.up('viewport');
                    var wnd = viewport.createOnlyOneWnd('commandWnd', 'Seniel.view.mapobject.SendCommandWindow', {
                        title: tr('mapobject.objectactions.commands.restartingterminal') + ' "' + rec.get("name") + '"',
                        commandPasswordNeeded: self.commandPasswordNeeded,
                        command: 'restartterminal',
                        icon: 'images/ico16_loading_def.png',
                        record: rec,
                        btnConfig: {
                            icon: 'images/ico16_loading_def.png',
                            text: tr('mapobject.objectactions.commands.restartingterminal') + ' "' + rec.get("name") + '"'
                        }
                    });
                    viewport.showNewWindow(wnd);
                    //objectsCommander.sendRestartingTerminalCommand(uid, password)
                }
            }
        ];

        if (self.record.get('requireMaintenance')) {
            itemsElements.push({
                text: tr("maintenance.reset"),
                icon: 'images/Wrench16.png',
                handler: function (btn, e) {
                    var rec = btn.up('window').record;
                    Ext.MessageBox.confirm(tr("maintenance.reset"),
                        tr("maintenance.reset.confirmation").replace("{name}", rec.get('name')), function (button) {
                            if (button === 'yes') {
                                maintenanceService.resetMaintenance(rec.get('uid'), function (result) {
                                    console.log("maintenance reset result = ", result);
                                    rec.set('requireMaintenance', result.requireMaintenance);
                                    if (!rec.get('requireMaintenance')) {
                                        var map = self.up('viewport').down('mainmap');
                                        map.removeMaintenanceMarker(map.mapObjects[rec.get('uid')]);
                                    }
                                });

                            }
                        });
                }
            });
            self.height += 20;
        }

        Ext.apply(this, {
            items: [
                {
                    xtype: 'buttongroup',
                    columns: 1,
                    layout: {
                        type: "vbox",
                        align: "stretch"
                    },
                    defaults: {
                        textAlign: 'left'
                    },
                    items: itemsElements
                }
            ]
        });
        this.callParent();
    }
});
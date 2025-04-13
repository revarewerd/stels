Ext.define('Seniel.view.mobile.Objects', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.gridobjectslist',
    layout: 'fit',
    viewConfig: {
        stripeRows: false,
        markDirty: false,
        getRowClass: function(record) {
            return record.get('hidden') === true ? 'mobile-object-list-row mobile-object-list-row-hidden' : 'mobile-object-list-row';
        }
    },
//    plugins: 'bufferedrenderer',
    commandPasswordNeeded: true,
    cls: 'mobile-grid-panel',
    tbar: [
        {
            xtype: 'textfield',
            name: 'filterobjs',
            cls: 'mobile-panel-toolbar',
            allowBlank: true,
            fieldLabel: tr('mobile.search'),
            flex: 1,
            listeners: {
                change: function(field, val, oldval) {
                    var str = val.replace(/\\/,'\\\\').replace(/\[/, '\\[').replace(/\]/, '\\]').replace(/\+/, '\\+').replace(/\|/, '\\|').replace(/\./, '\\.').replace(/\(/, '\\(').replace(/\)/, '\\)').replace(/\?/, '\\?').replace(/\*/, '\\*').replace(/\^/, '\\^').replace(/\$/, '\\$');
                    console.log('Filter value = ' + str);
                    var re = new RegExp(str, 'i'),
                        fname = {id: 'name', property: 'name', value: re};
                    if ((oldval && val.length < oldval.length) || (oldval && val.length === oldval.length && val !== oldval)){
                        field.up('gridobjectslist').getStore().clearFilter(true);
                    }
                    field.up('gridobjectslist').getStore().filter(fname);
                }
            }
        }
    ],
    rowLines: true,
    border: false,
    padding: false,
    hideCollapseTool: true,
    lastObjectsUpdate: new Date().getTime(),
    lastMessagesUpdate: new Date().getTime(),
    enableColumnHide: false,
    enableColumnMove: false,
    enableColumnResize: false,
    columns: [
        {
            menuDisabled: true,
            xtype: 'actioncolumn',
            dataIndex: 'checked',
            width: 48,
            items: [
                {
                    icon: 'images/ico32_globe.png',
                    isDisabled: function(view, rowIndex, colIndex, item, rec) {
                        return !rec.get('latestmsg');
                    },
                    handler: function(view, rowIndex, colIndex, item, e, rec) {
                        var grid = this.up('gridobjectslist');
                        rec.set('checked', true);
                        grid.checkedRecords.push(rec.get('uid'));
                        mapObjects.updateCheckedUids(grid.checkedRecords);
                        grid.showObjectOnMap(rec);
                    },
                    getClass: function(val, metaData, rec){
                        if (rec.get('checked')) {
                            metaData.tdAttr = 'title="' + tr('mapobject.showingonmap.stop') + '"';
                            return 'x-hide-display';
                        } else if (rec.get('latestmsg')) {
                            metaData.tdAttr = 'title="' + tr('mapobject.showingonmap.show') + '"';
                            return 'object-list-button';
                        } else {
                            return 'object-list-button-disabled';
                        }
                    }
                },
                {
                    icon: 'images/ico32_globe_act.png',
                    handler: function(view, rowIndex, colIndex, item, e, rec) {
                        var grid = this.up('gridobjectslist');
                        rec.set('checked', false);
                        for (var i = 0; i < grid.checkedRecords.length; i++) {
                            if (grid.checkedRecords[i] === rec.get('uid')) {
                                grid.checkedRecords.splice(i, 1);
                                break;
                            }
                        }
                        mapObjects.updateCheckedUids(grid.checkedRecords);
                        grid.hideObjectOnMap(rec);
                    },
                    getClass: function(val, metaData, rec) {
                        if (rec.get('checked')) {
                            return 'object-list-button';
                        } else {
                            return 'x-hide-display';
                        }
                    }
                }
            ]
        },
        {
            text: tr('mapobject.objectname'),
            menuDisabled: true,
            dataIndex: 'name',
            flex: 1,
            renderer: function(val) {
                return '<div style="white-space:normal !important;">' + val + '</div>';
            }
        },
        {
            menuDisabled: true,
            xtype: 'actioncolumn',
            dataIndex: 'blocked',
            width: 48,
            items: [
                {
                    iconCls: '',
                    isDisabled: function(view, rowIndex, colIndex, item, rec) {
                        return !(rec.get('canBlock') && rec.get('blockingEnabled') && rec.get('latestmsg'));
                    },
                    handler: function(view, rowIndex, colIndex, item, e, rec) {
                        if (rec.get('blocked') === 'wait') {
                            view.up('viewport').messageBox.showMessage(tr('mapobject.objectactions.blocking.sended'));
                            return null;
                        }
                        var block = (rec.get('blocked'))?(false):(true);
                        view.up('grid').objectBlockConfirmation(block, rec);
                    },
                    getClass: function(val, metaData, rec) {
                        if (rec.get('blocked') === 'wait') {
                            return 'object-icon-locking-progress object-list-button-disabled';
                        }
                        if (rec.get('blocked')) {
                            return 'object-icon-locked';
                        } else {
                            return 'object-icon-unlocked';
                        }
                    }
                }
            ]
        },
        {
            menuDisabled: true,
            xtype: 'actioncolumn',
            dataIndex: 'ignition',
            width: 48,
            items: [
                {
                    icon: 'images/ico32_ignition.png',
                    handler: showObjectInformation,
                    getClass: function(val, metaData, rec) {
                        if (val !== 'unknown' && val > 0) {
                            return 'object-list-button';
                        } else {
                            return 'x-hide-display';
                        }
                    }
                }
            ]
        },
        {
            menuDisabled: true,
            xtype: 'actioncolumn',
            dataIndex: 'speed',
            width: 48,
            items: [
                {
                    iconCls: '',
                    handler: showObjectInformation,
                    getClass: function(val, metaData, rec) {
                        var lastmsg = new Date(rec.get('latestmsg')),
                            current = new Date(),
                            yesterday = Ext.Date.subtract(current, Ext.Date.DAY, 20),
                            isOutdated = !Ext.Date.between(lastmsg, yesterday, current);
                            
                        if (val > 0) {
                            if (isOutdated) {
                                return 'object-icon-move-outdated';
                            } else {
                                return 'object-icon-move';
                            }
                        } else {
                            if (isOutdated) {
                                return 'object-icon-stop-outdated';
                            } else {
                                return 'object-icon-stop';
                            }
                        }
                    }
                }
            ]
        },
        {
            menuDisabled: true,
            xtype: 'actioncolumn',
            dataIndex: 'latestmsg',
            width: 48,
            items: [
                {
                    iconCls: '',
                    handler: showObjectInformation,
                    getClass: function(val, metaData, rec) {
                        var color = 'red',
                            strSat = (rec.get('satelliteNum') > 5)?('h'):('l');
                        
                        if (val) {
                            color = Seniel.view.WRUtils.getLastMsgColor(new Date(val)).code;
                        }
                        
                        return 'object-icon-satellite-' + strSat + color;
                    }
                }
            ]
        }
    ],
    checkedRecords: [],
    mapBounds: {
        lonmin: 1000,
        lonmax: 0,
        latmin: 1000,
        latmax: 0,
        set: function(lon, lat, self) {
            if (this.lonmin > lon) this.lonmin = lon;
            if (this.lonmax < lon) this.lonmax = lon;
            if (this.latmin > lat) this.latmin = lat;
            if (this.latmax < lat) this.latmax = lat;
            if (this.loadSelected < self.checkedRecords.length) {
                this.loadSelected++;
            }
            if (this.loadSelected === self.checkedRecords.length) {
                self.mapZoomExtent();
                this.loadSelected++;
            }
        },
        loadSelected: null
    },
    initComponent: function() {
        var self = this;
        objectsCommander.commandPasswordNeeded(function(resp) {
            self.commandPasswordNeeded = resp;
        });
        self.serverRequest();

        var thestore = Ext.create('EDS.store.MapObjects', {
            autoLoad: true,
            listeners: {
                load: function(store, records, successful, eOpts) {
                    self.objectsArray = [];
                    Ext.Array.each(records, function(r) {
                        self.objectsArray.push({uid: r.get('uid'), name: r.get('name')});
                        if (r.get('checked')) {
                            self.showObjectOnMap(r);
                            self.checkedRecords.push(r.get('uid'));
                        }
                    });
//                },
//                update: function(store, record, operation, modifiedFieldNames, eOpts ) {
//                    if (modifiedFieldNames.indexOf('blocked', 0) !== -1) {
//                        if (record.get('blocked') === true) {
//                            self.fireEvent('mapobjlock', record);
//                        } else if (record.get('blocked') === false) {
//                            self.fireEvent('mapobjunlock', record);
//                        }
//                    }
                }
            },
            sort: function(params) {
                var prop = params ? params.property : 'name';

                params = params || { property: "name", direction: "ASC" };
                var mod = params.direction.toUpperCase() === "DESC" ? -1 : 1;

                thestore.superclass.sort.call(this,params);

                if (prop === 'sleeper') {
                    this.doSort(function (o1, o2) {
                        var sleeper1 = o1.get('sleeper');
                        var sleeper2 = o2.get('sleeper');

                        if (sleeper1 === sleeper2)
                            return 0;
                        if (!sleeper1)
                            return -1 * mod;
                        if (!sleeper2)
                            return 1 * mod;
                        return mod * (sleeper1.time > sleeper2.time ? 1 : sleeper1.time === sleeper2.time ? 0 : -1);
                    });
                }

                if (prop === 'radioUnit') {
                    this.doSort(function (o1, o2) {
                        var radio1 = o1.get('radioUnit');
                        var radio2 = o2.get('radioUnit');

                        if (!radio1 || !radio1.installed)
                            return -1 * mod;
                        if (!radio2 || !radio2.installed)
                            return 1 * mod;
                        return mod * (radio1.workDate > radio2.workDate ? 1 : radio1.workDate === radio2.workDate ? 0 : -1);
                    });
                }
            }
        });

        Ext.apply(this, {
            store: thestore
        });
        this.callParent();
    },
    
    //---------
    // Функциии
    //---------
    objectBlockConfirmation: function(block, rec) {
        var self = this;
        var callback = function(btn, text) {
            if (btn === 'Ok') {
                objectsCommander.sendBlockCommand(rec.get('uid'), block, text, function(resp, e) {
                    if (!e.status) {
                        self.up('viewport').messageBox.showMessage(tr('mapobject.objectactions.error') + '. ' + e.message);
                    } else {
                        rec.set('blocked', 'wait');
                    }
                });
            }
        };
        
        if (self.commandPasswordNeeded) {
            self.up('viewport').messageBox.showPasswordPromt(tr('mapobject.objectactions.blocking.password.mobile') + ': ', null, callback);
        } else {
            self.up('viewport').messageBox.showConfirm(tr('mapobject.objectactions.blocking.sure') + ' ' + (block ? tr('mapobject.objectactions.blocking.block') : tr('mapobject.objectactions.blocking.unblock')) + ' ' + tr('mapobject.objectactions.blocking.object') + ' «' + rec.get('name') + '» ?', null, callback);
        }
    },
    serverRequest: function() {
        var self = this;
        
        mapObjects.getUpdatedAfter(self.lastObjectsUpdate, function(result, request) {
            self.updateFromServer(result, request);
        });
        
        eventsMessages.getUpdatedAfter(self.lastMessagesUpdate, function(resp) {
            if (!resp || resp.data === []) {
                return;
            }
            self.lastMessagesUpdate = resp.newTime;
            
            if (resp.data.length) {
                var map = self.up('viewport').down('mapcontainer');
                self.up('viewport').down('gridmessageslist').getStore().add(resp.data);
                self.up('viewport').down('#btnMessagesPanel').setIcon('images/ico32_messages_act.png');
                console.log('Updated messages = ', resp.data);
                for (var i = 0; i < resp.data.length; i++) {
                    var rec = self.getStore().getById(resp.data[i].uid);
                    if(resp.data[i].blocked!=undefined){
                        if (resp.data[i].blocked) {
                            rec.set('blocked', true);
                            map.addLockMarker(rec, map.mapObjects[resp.data[i].uid]);
                        } else {
                            rec.set('blocked', false);
                            map.removeLockMarker(map.mapObjects[resp.data[i].uid]);
                        }
                    }
                    else if (resp.data[i].text) {
                        resp.data[i].text= tr(resp.data[i].text);
                        map.addEventMarker(rec, map.mapObjects[resp.data[i].uid]);
                    }
                }
            }
            
            if (resp.reload) {
                self.getStore().reload();
            }
            
            setTimeout(function() {
                self.serverRequest();
            }, 2000);
        });
    },
    updateFromServer: function(result, request) {
        if (request.xhr && request.xhr.status === 403) {
            this.up('viewport').messageBox.showMessage((tr('mapobjects.authtorizationerror') + '. ' + tr('mapobjects.authtorizationerror.msg')), null, function() {
                window.location = '/login.html';
            });
            return;
        }

        var self = this;

        Ext.each(result, function(servUpdate, index) {
            if (!self.lastObjectsUpdate || self.lastObjectsUpdate < servUpdate.newTime)
                self.lastObjectsUpdate = servUpdate.newTime;
            var node = self.getStore().getById(servUpdate.uid);

            if (node) {
                node.beginEdit();
                node.set('lon', servUpdate.lon);
                node.set('lat', servUpdate.lat);
                node.set('time', servUpdate.time);
                node.set('latestmsg', servUpdate.time);
                node.set('satelliteNum', servUpdate.satelliteNum);
                if (servUpdate.speed > 1) {
                    node.set('course', servUpdate.course);
                }
                node.set('speed', servUpdate.speed);
                if (servUpdate.sleeper) {
                    node.set('sleeper', servUpdate.sleeper);
                }
                node.endEdit(false);
                var marker = self.up('viewport').down('mapcontainer').mapObjects[node.get('uid')];
                if (marker) {
                    self.up('viewport').down('mapcontainer').moveMarker(node, servUpdate.lon, servUpdate.lat);
                    if (node.get('targeted') && node.get('speed') > 0) {
                        self.up('viewport').down('mapcontainer').setMapCenter(servUpdate.lon, servUpdate.lat);
                    }
                }
            }
        });
    },
    mapZoomExtent: function() {
        console.log('Map zoom to bounds. Bounds = ', this.mapBounds);
        var map = this.up('viewport').down('mapcontainer'),
            bounds = new OpenLayers.Bounds(),
            min = map.transLonLat(this.mapBounds.lonmin, this.mapBounds.latmin),
            max = map.transLonLat(this.mapBounds.lonmax, this.mapBounds.latmax);
        bounds.extend(new OpenLayers.LonLat(min.lon, min.lat));
        bounds.extend(new OpenLayers.LonLat(max.lon, max.lat));
        map.map.zoomToExtent(bounds);
    },
    showObjectOnMap: function(rec) {
        var self = this;
        var map = self.up('viewport').down('mapcontainer');
        
        console.log("adding marker " + rec.get('name'));

        if (rec.get('lon')) {
            map.addMarker(rec);
            map.setMapCenter(rec.get('lon'), rec.get('lat'));
            self.mapBounds.set(rec.get('lon'), rec.get('lat'), self);
        } else {
            mapObjects.getLonLat([rec.get('uid')], function(result, request) {
                self.updateFromServer(result, request);
                map.addMarker(rec);
                map.setMapCenter(rec.get('lon'), rec.get('lat'));
                self.mapBounds.set(rec.get('lon'), rec.get('lat'), self);
            });
        }
        if (map.isHidden()) {
            this.up('viewport').down('#btnMapPanel').toggle();
        }
    },
    hideObjectOnMap: function(rec) {
        this.up('viewport').down('mapcontainer').removeMarker(rec);
    }
});

function showObjectInformation(view, rowIndex, colIndex, item, e, rec) {
    var ignition = (rec.get('ignition') !== 'unknown')?(tr('mainmap.ignition') + ' '):('');
    ignition += (ignition && rec.get('ignition') > 0)?(tr('mainmap.ignition.on') + '<br>'):((ignition)?(tr('mainmap.ignition.off') + '<br>'):(''));
    var lastmsg = tr('mapobject.lastmessage.empty');
    if (rec.get('latestmsg')) {
        lastmsg = Seniel.view.WRUtils.getLastMsgText(new Date(rec.get('latestmsg')));
    }
    if (rec.get('lon') && rec.get('lat')) {
        mapObjects.regeocode(rec.get('lon'), rec.get('lat'), function(place) {
            view.up('viewport').messageBox.showMessage('<b>' + tr('mobile.objectinfo') + ' «' + rec.get('name') + '»:</b><br>' + tr('mainmap.position') + ': ' + place + '<br>' + ignition + tr('mainmap.speed') + ': ' + rec.get('speed') + ' ' + tr('units.speed') + '<br>' + tr('mainmap.lastmessage') + ': ' + lastmsg + ' <br>' + tr('mainmap.satcount') + ': ' + rec.get('satelliteNum'));
        });
    } else {
        view.up('viewport').messageBox.showMessage('<b>' + tr('mobile.objectinfo') + ' «' + rec.get('name') + '»:</b><br>' + ignition + tr('mainmap.speed') + ': ' + rec.get('speed') + ' ' + tr('units.speed') + '<br>' + tr('mainmap.lastmessage') + ': ' + lastmsg + ' <br>' + tr('mainmap.satcount') + ': ' + rec.get('satelliteNum'));
    }
}
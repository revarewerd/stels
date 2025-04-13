Ext.define('Seniel.view.notifications.PopupWindow', {
    extend: 'Ext.window.Window',
    renderTo: Ext.getBody(),
    id: 'notificationsPopupWindow',
    style: {
        opacity: '0.8'
    },
    header: {
        title: tr('notifications.popup.title')
    },
    width: 360,
    minHeight: 120,
    maxWidth: 640,
    maxHeight: 480,
    layout: 'fit',
    items: [
        {
            xtype: 'grid',
            hideHeaders: false,
            disableSelection: true,
            enableColumnHide: false,
            hideCollapseTool: true,
            rowLines: true,
            border: 0,
            padding: 0,
            columns: [
                {
                    tooltip: tr('notifications.history.grid.showonmap'),
                    tooltipType: 'title',
                    xtype: 'actioncolumn',
                    dataIndex: 'lon',
                    width: 20,
                    menuDisabled: true,
                    sealed: true,
                    resizable: false,
                    items: [
                        {
                            icon: 'images/ico16_globe.png',
                            isDisabled: function(view, rowIndex, colIndex, item, rec) {
                                var coords=view.up("window").getObjectLonLat(rec);
                                return !((rec.get('lon') && rec.get('lat')) || coords );
                            },
                            handler: function(view, rowIndex) {
                                var rec = view.getStore().getAt(rowIndex);
                                var map = Ext.getCmp('notificationsPopupWindow').map;
                                var showPopup = function() {
                                    var time = Ext.Date.format(new Date(rec.get('time')), tr('format.extjs.datetime'));
                                    var text = '<div class="ol-popup-content"><b>' + tr('notifications.history.grid.object') + ': </b>' + rec.get('name') + '<br/><b>' + tr('notifications.history.grid.time') + ': </b>' + time + '<br/><b>' + tr('notifications.history.grid.message') + ': </b>' + tr(rec.get('text')) + '</div>';


                                    if(!rec.get('lon') && !rec.get('lat')){
                                        var coords=view.up("window").getObjectLonLat(rec);
                                        if(coords!=null){
                                            rec.set('lon',coords.lon);
                                            rec.set('lat',coords.lat);
                                        }
                                    }

                                    rec.set('onMap', true);
                                    if (history) {
                                        var hRec = history.down('grid').getStore().findRecord('eid', rec.get('eid'));
                                        if (hRec) hRec.set('onMap', true);
                                    }

                                    var history = map.up('viewport').down('eventswnd');
                                    map.addPopup(text, rec.getData());
                                };
                                
                                if (!rec.get('eid')) {
                                    eventsMessages.getMessageHash(rec.get('text'), rec.get('type'), rec.get('time'), function(resp) {
                                        if (resp && map) {
                                            rec.set('eid', resp);
                                            showPopup();
                                        }
                                    });
                                } else if (map) {
                                    showPopup();
                                }
                            },
                            getClass: function(val, metaData, rec){
                                if (rec.get('lon') && rec.get('lat') && rec.get('onMap')) {
                                    metaData.tdAttr = 'title="' + tr('notifications.history.grid.showonmap.stop') + '"';
                                    return 'x-hide-display';
                                } else if (rec.get('lon') && rec.get('lat')) {
                                    metaData.tdAttr = 'title="' + tr('notifications.history.grid.showonmap.start') + '"';
                                    return 'object-list-button';
                                } else {
                                    return 'object-list-button-disabled';
                                }
                            }
                        },
                        {
                            icon: 'images/ico16_globeact.png',
                            handler: function(view, rowIndex) {
                                var rec = view.getStore().getAt(rowIndex);
                                var map = Ext.getCmp('notificationsPopupWindow').map;
                                
                                if (map && rec.get('eid')) {
                                    rec.set('onMap', false);
                                    var history = map.up('viewport').down('eventswnd');
                                    if (history) {
                                        var hRec = history.down('grid').getStore().findRecord('eid', rec.get('eid'));
                                        if (hRec) hRec.set('onMap', false);
                                    }

                                    map.removePopup(rec.get('eid'), false);
                                }
                            },
                            getClass: function(val, metaData, rec) {
                                if (val && rec.get('onMap')) {
                                    return 'object-list-button';
                                } else {
                                    return 'x-hide-display';
                                }
                            }
                        }
                    ]
                },
                {
                    text: tr('notifications.history.grid.time'),
                    dataIndex: 'time', 
                    width: 120,
                    renderer: function(val){
                        var d = new Date(val);
                        return Ext.Date.format(d, tr('format.extjs.datetime'));
                    }
                },
                {
                    text: tr('notifications.popup.messagetext'),
                    dataIndex: 'text',
                    flex: 1,
                    renderer: function(val, metaData, rec) {
                        if(rec.get("name")) val = rec.get('name') + ': ' + val;
                        metaData.tdAttr = 'title="' + val + '"';
                        return val;
                    }
                }
            ],
            store: {
                fields: ['eid', 'uid', 'time', 'type', 'name', 'text', 'lon', 'lat'],
                data: {
                    'items': [
                        
                    ]
                },
                proxy: {
                    type: 'memory',
                    reader: {
                        type: 'json',
                        root: 'items'
                    }
                }
            },
            listeners: {
                itemdblclick: function(view, rec, item, i) {
                    if (rec.get('uid')) {
                        var map = Ext.getCmp('notificationsPopupWindow').map;
                        var molStore = map.up('viewport').down('mapobjectlist').getStore();

                        var objRec = (molStore)?(molStore.findRecord('uid', rec.get('uid'))):(null);
                        if (objRec && objRec.get('lon') && objRec.get('lat')) {
                            map.setMapCenter(objRec.get('lon'), objRec.get('lat'));
                            map.map.zoomTo(map.map.numZoomLevels - 3);
                        }
                    }
                }
            }
        }
    ],
    listeners: {
        hide: function (comp) {
            comp.down('grid').getStore().removeAll();
            comp.destroy();
        }
    },
    getObjectLonLat:function(rec) {
        var coords = {};
        if (rec.get('uid')) {
            var molStore = Ext.ComponentQuery.query('mapobjectlist')[0].getStore();

            var objRec = (molStore) ? (molStore.findRecord('uid', rec.get('uid'))) : (null);
            if (objRec) {
                var objLon = objRec.get('lon');
                var objLat = objRec.get('lat');
                if (objLon != "" && !!objLon && objLat != "" && !!objLat) {
                    coords.lon = objLon;
                    coords.lat = objLat;
                }
            }
        }
        if(Object.keys(coords).length==0) return null;
        return coords;
    }
});
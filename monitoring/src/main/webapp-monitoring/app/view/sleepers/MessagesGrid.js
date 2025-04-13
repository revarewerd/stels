Ext.define('Seniel.view.sleepers.MessagesGrid', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.sleepergrid',
    stateId: 'sleeperMsgGrid',
    stateful: true,
    autoScroll: true,
    border: false,
    columns:[
        {
            stateId: 'phone',
            header: tr('datapanel.sleeper'),
            flex: 7,
            sortable: true,
            dataIndex: 'senderPhone',
            renderer: function(val, metaData, rec) {
                var grid = rec.store.grid,
                    wnd = grid.up('window');
                for (var i = 0; i < wnd.slprData.length; i++) {
                    if (wnd.slprData[i].simNumber.match(val)) {
                        rec.set('model', wnd.slprData[i].model);
                        metaData.tdAttr = 'title="' + wnd.slprData[i].model + '"';
                        return wnd.slprData[i].model;
                    }
                }
            }
        },
        {
            stateId: 'date',
            header: tr('datapanel.receivedate'),
            flex: 6,
            sortable: true,
            dataIndex: 'sendDate',
            renderer: function (val, metaData, rec) {
                if (val) {
                    var lmd = new Date(val); 
                    return Ext.Date.format(lmd, tr('format.extjs.datetime'));
                } else return '';
            }
        },
        {
            stateId: 'batt',
            header: tr('datapanel.battery'),
            flex: 10,
            sortable: true,
            dataIndex: 'battery'
        },
        {
            stateId: 'lonlat',
            header: tr('datapanel.showonmap'),
            flex: 6,
            dataIndex: 'posData',
            renderer: function(val, metaData, rec) {
                var wnd = rec.store.grid.up('window')
                if (val) {
                    if (val.match(/Some\(\((\d*.\d*),(\d*.\d*)\)\)/g)) {
                        var lonlat = val.substr(6, (val.length - 8)).split(',');
                        var recId=rec.get("smsId");
                        var time = Ext.Date.format(new Date(rec.get('sendDate')), "d.m.Y H:i:s");
                        var text = '<div class=ol-popup-content><b>' + tr('datapanel.sleeper') + ': </b>' + rec.get('model') + '<br/><b>' + tr('datapanel.receivedate') + ': </b>' + time + '<br/><b>' + tr('datapanel.battery') + ': </b>' + rec.get('battery') + '<br/><b>' + tr('datapanel.object') + ': </b>' + Ext.String.htmlEncode(wnd.title.substr(20, (wnd.title.length - 27))) + '</div>';
                        var mapId = this.up('viewport').down('mainmap').id;
                        
                        return '<a href="#" onclick="Ext.getCmp(\'' + mapId + '\').addPopup(\'' + text + '\', ' + '{lon:'+lonlat[0]+',lat:'+lonlat[1]+',eid:'+recId+'}, ' + rec.get('smsId') + '); return false;">' + tr('sleeper.position.onmap') + '</a>';
                    } else if (val.match('http')) {
                        return '<a href="' + val.substr(5, val.length - 6) + '" target="_blank">' + tr('sleeper.position.onynd') + '</a>';
                    } else {
                        return '<a href="EDS/showbylbs?id=' + encodeURIComponent(rec.get('smsId')) + '" target="_blank">' + tr('sleeper.position.onynd') + '</a>';
                    }
                } else {
                    return '';
                }
            }
        }
    ],
    initComponent: function() {
        var store = Ext.create('EDS.store.SleeperMessages', {
            autoLoad: false,
            listeners: {
                load: function(store, records, successful) {
                    var slprData = store.grid.up('window').slprData;
                    store.simNumbers = [];
                    for (var i = 0; i < slprData.length; i++) {
                        store.simNumbers[i] = slprData[i].simNumber;
                    }
                    store.filter([{
                        filterFn: function(rec) {
                            return (store.simNumbers[0].match(rec.get('senderPhone')))?(true):(false);
                        }
                    }]);
                }
            }
        });
        Ext.apply(this, {
            store: store
        });
        
        this.callParent(arguments);
    },
    listeners: {
        boxready: function(grid) {
            var wnd = grid.up('window');
            grid.getStore().grid = grid;
            grid.getStore().getProxy().setExtraParam('uid', wnd.uid);
            grid.getStore().load();
        }
    }
});
Ext.define('EventsGrid', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.eventsgrid',
    stateId: 'eventsGrid',
    stateful: true,
    selType: 'checkboxmodel',
    selModel: {
        showHeaderCheckbox: true,
        mode: 'SIMPLE',
        checkOnly: true
    },
    initComponent: function () {
        var self=this
        Ext.apply(this, {
            store: Ext.create('EDS.store.EventsMessages', {
                autoLoad: true
            })
        });
//        this.getStore().grid = this
//        this.getStore().sort('time','DESC');
        this.callParent();
        this.getSelectionModel().on('selectionchange', this.onSelectChange, this);
    },
    rowLines: true,
    border: false,
    padding: false,
    columns: [
        { 
            stateId: 'onMap',
            text: '<img src="images/ico16_globe.png"/>',
            menuText: tr('notifications.history.grid.showonmap'),
            tooltip: tr('notifications.history.grid.showonmap'),
            tooltipType: 'title',
            xtype: 'actioncolumn',
            dataIndex: 'lon',
            width: 38,
            align:"center",
            menuDisabled: true,
            sealed: true,
            resizable: false,
            items: [
                {
                    icon: 'images/ico16_globe.png',
                    isDisabled: function(view, rowIndex, colIndex, item, rec) {
                        var coords=view.up("eventsgrid").getObjectLonLat(rec);
                        return !((rec.get('lon') && rec.get('lat'))|| coords);
                    },
                    handler: function(view, rowIndex) {
                        var rec = view.getStore().getAt(rowIndex);
                        rec.set('onMap', true);
                        var popup = Ext.getCmp('notificationsPopupWindow');
                        if (popup) {
                            var pRec = popup.down('grid').getStore().findRecord('eid', rec.get('eid'));
                            if (pRec) pRec.set('onMap', true);
                        }
                        
                        var time = Ext.Date.format(new Date(rec.get('time')), "d.m.Y H:i:s");
                        var text = '<div class="ol-popup-content"><b>' + tr('notifications.history.grid.object') + ': </b>' + rec.get('name') + '<br/><b>' + tr('notifications.history.grid.time') + ': </b>' + time + '<br/><b>' + tr('notifications.history.grid.message') + ': </b>' + tr(rec.get('text')) + '</div>';

                        if(!rec.get('lon') && !rec.get('lat')){
                            var coords=view.up("eventsgrid").getObjectLonLat(rec);
                            if(coords!=null){
                                rec.set('lon',coords.lon);
                                rec.set('lat',coords.lat);
                            }
                        }
                        view.up('viewport').down('mainmap').addPopup(text, rec.getData());
                        rec.commit(true);
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
                        rec.set('onMap', false);
                        var popup = Ext.getCmp('notificationsPopupWindow');
                        if (popup) {
                            var pRec = popup.down('grid').getStore().findRecord('eid', rec.get('eid'));
                            if (pRec) pRec.set('onMap', false);
                        }
                        
                        view.up('viewport').down('mainmap').removePopup(rec.get('eid'), false);
                        rec.commit(true);
                    },
                    getClass: function(val, metaData, rec) {
                        if (rec.get('lon') && rec.get('lat') && rec.get('onMap')) {
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
            stateId: 'time',
            dataIndex: 'time',
            width: 120,
            renderer: function(val){
                var d = new Date(val);
                return Ext.Date.format(d, tr('format.extjs.datetime'));
            }
        },
        {text: tr('notifications.history.grid.type'), stateId: 'type', dataIndex: 'type', width: 120, renderer: function(val){return tr(val);}},
        {text: tr('notifications.history.grid.object'), stateId: 'name', dataIndex: 'name', width: 120},
        {text: tr('notifications.history.grid.message'), stateId: 'text', dataIndex: 'text', minWidth: 240, flex: 3, renderer: function(val){return tr(val);}},
        {text: tr('notifications.history.grid.user'), stateId: 'user', dataIndex: 'user', minWidth: 120, flex: 1},
        {
            text: '<img src="images/ico16_msghist.png"/>',
            menuText:tr("notifications.history.readStatus"),
            tooltip: tr("notifications.history.readStatus"),
            tooltipType: 'title',
            sortable: true,
            resizable: false,
            dataIndex: 'readStatus',
            width: 40,
            align:"center",
            renderer: function (val, metaData, rec) {
                if (val === true || val==="")
                {
                    metaData.tdAttr = 'style="cursor: pointer !important;" ' + 'title="' + tr("notifications.history.readStatus.read") + '"'
                    return '<img src="images/ico16_msghist.png"  alt=""/>'
                }
                else
                {
                    metaData.tdAttr = 'style="cursor: pointer !important;" '+'title="'+tr("notifications.history.readStatus.unread")+'"'
                    return '<img src="images/ico16_msghist_yel.png" alt=""/>'
                }
            }
        },
        {hideable: false,menuDisabled: true,sortable: false,width: 0},
    ],
    bbar: [
        '->',
        {
            xtype: 'button',
            text: tr('notifications.history.readStatus.markAs'),
            itemId:'markAs',
            disabled:true,
            width: 120,
            menu: {
                items: [
                    {
                        text: tr("notifications.history.readStatus.markAs.read"), icon: 'images/ico16_msghist.png"', handler: function (btn) {
                        this.up('grid').markSelectedAs(true)
                    }
                    },
                    {
                        text: tr("notifications.history.readStatus.markAs.unread"),
                        icon: 'images/ico16_msghist_yel.png"',
                        handler: function (btn) {
                            this.up('grid').markSelectedAs(false)
                        }
                    }
                ]
            }
        }],
    listeners: {
        cellclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex,
        /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
            var grid=self0.up('grid')
            var dataIndex =  grid.columnManager.columns[cellIndex].dataIndex
            switch (dataIndex) {
                case  "readStatus":
                {
                    var readStatus = record.get('readStatus');
                    if(readStatus==="") readStatus=true;
                    var ticketId = record.get("_id");
                    //var text = "Пометить прочитанным";
                    //if (readStatus) text = "Пометить непрочитанным";
                    var data=record.getData();
                    console.log("record data=",data)
                    //Ext.MessageBox.confirm('Изменить статус прочтения', text, function (button) {
                    //    if (button === 'yes') {
                            data.readStatus=!readStatus
                            eventsMessages.updateEventReadStatus(data, function (res, e) {
                                if (!e.status) {
                                    Ext.MessageBox.show({
                                        title: 'Произошла ошибка',
                                        msg: e.message,
                                        icon: Ext.MessageBox.ERROR,
                                        buttons: Ext.Msg.OK
                                    });
                                }
                                else grid.getStore().load()
                            })
                    //    }
                    //});
                    break;
                }
            }
        },
        itemdblclick: function(view, rec, item, i) {
            var map = view.up('viewport').down('mainmap');
            var coords=view.up("eventsgrid").getObjectLonLat(rec);
            if(coords!=null) {
                map.setMapCenter(coords.lon, coords.lat);
                map.map.zoomTo(map.map.numZoomLevels - 3);
            }
        }
    },
    markSelectedAs:function(readStatus){
        var grid=this;
        var records=grid.getSelectionModel().getSelection();
        if (records.length>0) {
            var data = [];
            for(var i in records){
                data[i]=records[i].getData()
            }
            eventsMessages.updateEventsReadStatus(data,readStatus,function(res){
                grid.getStore().reload()
            })
        }
    },
    onSelectChange: function (selModel, selections) {
        this.down('#markAs').setDisabled(selections.length === 0);
    },
    getObjectLonLat:function(rec) {
        var coords = {};
        if (rec.get('uid')) {
            var molStore = this.up('viewport').down('mapobjectlist').getStore();
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


Ext.define('Seniel.view.notifications.HistoryWindow', {
    extend: 'Seniel.view.WRWindow',
    alias: 'widget.eventswnd',
    stateId: 'eventsWnd',
    stateful: true,
    icon: 'images/ico16_eventsmsgs.png',
    title: tr('notifications.history.title'),
    btnConfig: {
        icon: 'images/ico24_eventsmsgs.png',
        text: tr('notifications.history.ntfhistory')
    },
    minWidth: 640,
    minHeight: 240,
    layout: 'border',
    items: [
        {
            region: 'north',
            itemId: 'filterPanel',
            title: tr('notifications.history.filter'),
            xtype: 'panel',
            height: 140,
            collapsible: true,
            collapseMode:'header',
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            bbar: [
                '->',
                {
                    xtype: 'button',
                    margin: '0 0 0 20',
                    text: tr('notifications.history.filter.apply'),
                    width: 120,
                    handler: function () {
                        var wnd = this.up('eventswnd');
                        wnd.loadNotifications();
                    }
                }],
            items: [
                {
                    xtype: 'fieldcontainer',

                    layout: 'hbox',


                    items: [
                        {
                            xtype: 'radiogroup',
                            itemId:'filterType',
                            margin: '4 12 0 8',
                            fieldLabel: '<b>' + tr('notifications.history.filter.type') + '<b>',
                            vertical: true,
                            columns: 1,
                            items: [
                                {
                                    boxLabel: tr("notifications.history.filter.type.unread"),
                                    inputValue: "unread",
                                    name: "filterType"//,
                                    //checked: true
                                },
                                {
                                    boxLabel:tr("notifications.history.filter.type.period"),
                                    inputValue: "period",
                                    name: "filterType",
                                    checked: true
                                }
                            ],
                            listeners: {
                                change: function (rg, newVal, oldVal) {
                                    var wnd = rg.up('window')

                                    switch (newVal.filterType) {
                                        case "unread":
                                        {
                                            wnd.down('#periodContainer').disable();
                                            break;
                                        }
                                        case "period":
                                        {
                                            console.log(wnd.down('#periodContainer'))
                                            wnd.down('#periodContainer').enable();
                                            break;
                                        }

                                    }


                                }
                            }
                        },
                        {
                            xtype: 'splitter',
                            width: 150
                        }, {
                            margin: '14 12 0 8',
                            xtype: 'combo',
                            itemId: 'selectedObjects',
                            labelWidth: 60,
                            fieldLabel: '<b>' + tr('basereport.objectfield') + '</b>',
                            emptyText: tr('basereport.objectfield.emptytext'),
                            minChars: 0,
                            queryMode: 'local',
                            valueField: 'uid',
                            displayField: 'name',
                            forceSelection: true,
                            multiSelect: true,
                            caseSensitive: false,
                            store: {
                                fields: ['uid', 'name'],
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
                            listeners: {
                                boxready: function () {
                                    var viewport = this.up('window').up('viewport');
                                    var wnd = this.up('window');
                                    this.getStore().add(viewport.down('mapobjectlist').roData);
                                    if (wnd.objects != null) {
                                        this.select(wnd.objects[0]);
                                        wnd.loadNotifications();
                                        var mapobjectlist = Ext.ComponentQuery.query('mapobjectlist');
                                        var rec = this.getStore().findRecord("uid", wnd.objects[0])
                                        mapobjectlist[0].fireEvent('mapobjremoveevent', rec)
                                    }
                                }
                            }
                        }
                    ]
                },
                {
                    xtype: 'fieldcontainer',
                    margin: '4 12 4 8',
                    layout: 'hbox',
                    fieldLabel: '<b>' + tr('notifications.history.period') + '<b>',
                    itemId: 'periodContainer',
                    //disabled: true,
                    labelWidth: 100,
                    items: [
                        {
                            margin: '0 4 4 0',
                            xtype: 'datefield',
                            itemId: 'fromDate',
                            labelWidth: 35,
                            labelPad: 2,
                            width: 140,
                            fieldLabel: tr('basereport.fromdate').toLowerCase(),
                            name: 'from_date',
                            format: tr('format.extjs.date'),
                            value: new Date()
                        },
                        {
                            margin: '0 12 4 0',
                            xtype: 'timefield',
                            itemId: 'fromTime',
                            width: 80,
                            name: 'from_time',
                            format: tr('format.extjs.time'),
                            value: '00:00'
                        },
                        {
                            margin: '0 4 4 20',
                            itemId: 'toDate',
                            xtype: 'datefield',
                            labelWidth: 35,
                            labelPad: 2,
                            width: 140,
                            fieldLabel: tr('basereport.toDate').toLowerCase(),
                            name: 'to_date',
                            format: tr('format.extjs.date'),
                            value: new Date()
                        },
                        {
                            margin: '0 12 4 0',
                            xtype: 'timefield',
                            itemId: 'toTime',
                            width: 80,
                            name: 'to_time',
                            format: tr('format.extjs.time'),
                            value: '23:59'
                        },
                    ]
                },
            ]
        },
        {
            region:"center",
            itemId: 'events',
            xtype: 'eventsgrid'
        }
    ],
    loadNotifications:function(){
        var wnd = this;
        var objects = wnd.down('combo#selectedObjects').getValue();
        var filterType= wnd.down('#filterType').getValue().filterType;
        var params = null;
        if(filterType=="period") {
            var fromdate = wnd.down('datefield#fromDate').getValue();
            var fromtime = wnd.down('timefield#fromTime').getValue();
            var todate = wnd.down('datefield#toDate').getValue();
            var totime = wnd.down('timefield#toTime').getValue();
            var sumDateAndTime = function (date, time) {
                date.setHours(time.getHours(), time.getMinutes(), time.getSeconds(), time.getMilliseconds())
                var fdate = Ext.Date.format(date, "d.m.Y H:i:s:u")
                var defaultTimezone = Timezone.getDefaultTimezone()
                if (defaultTimezone) {
                    return moment.tz(fdate, "DD.MM.YYYY HH:mm:ss:SSS", defaultTimezone.name).toDate()
                }
                return date;
            };

            var currnt = new Date(),
                compto = sumDateAndTime(todate, totime);

            if ((compto.toLocaleDateString() === currnt.toLocaleDateString()) && (compto.getTime() > currnt.getTime())) {
                totime = currnt;
            }

            if (fromdate <= todate) {
                params = {
                    filterType: filterType,
                    from: sumDateAndTime(fromdate, fromtime),
                    to: sumDateAndTime(todate, totime),
                    uids: objects
                };
            }
        }
        else {
            params = {
                filterType: filterType,
                uids: objects
            };
        }

        console.log('Store load parameters = ', params);

        if (params) {
            var grid = wnd.down('grid');
            Ext.apply(grid.getStore().getProxy().extraParams, params);
            var store = grid.getStore()
            store.load(function(records, operation, success){
                //if(records.length>0) wnd.down("#filterPanel").collapse();
            });
        }
    }
});
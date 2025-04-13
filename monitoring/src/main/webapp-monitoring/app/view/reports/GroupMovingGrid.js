/**
 * Created by IVAN on 16.09.2015.
 */
Ext.define('Seniel.view.reports.GroupMovingGrid', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.groupmovinggrid',
    //stateId: 'repGrPGrid',
    //stateful: true,
    title: tr('main.reports.intervals'),
    loadMask: true,
    loadBeforeActivated: false,
    manualLoad: true,
    dockedToolbar: [/*'refresh', 'search'*/],
    lbar: [
        {
            icon: 'images/ico16_zoomout.png',
            iconAlign: 'top',
            text: ' ',
            height: 22,
            width: 32,
            tooltip: tr('main.groupreport.collapseall'),
            tooltipType: 'title',
            scale: 'small',
            handler: function (btn) {
                var grid = btn.up('groupmovinggrid');
                grid.collapseAllRows();
            }
        },
        {
            icon: 'images/ico16_printer.png',
            iconAlign: 'top',
            text: ' ',
            height: 22,
            width: 32,
            tooltip: tr('movinggrid.printtable'),
            tooltipType: 'title',
            scale: 'small',
            handler: function(btn) {
                var grid = btn.up('groupmovinggrid');
                Ext.ux.grid.Printer.print(grid);
            }
        }
    ],
    storeName: 'EDS.store.GroupMovingReport',
    plugins: [
        {
            ptype: 'rowexpander',
            pluginId: 'rowExpander',
            rowBodyTpl : new Ext.XTemplate('<div class="nested-grid"></div>')
        }
    ],
    viewConfig: {
        listeners: {
            expandbody: function(rowNode, rec, row) {
                var groupreportwnd = this.up('groupreportwnd');
                var dates = groupreportwnd.getWorkingDates();
                var mapWidget = groupreportwnd.down('seniel-mapwidget'),
                    self = this;
                if (!rec.nestedGrid) {
                    groupMovingReport.getReportPerObject(rec.get('uid'), dates.from, dates.to, function(resp) {
                        rec.nestedGrid = Ext.create('GroupMovingNestedGrid', {
                            parentGridView: self
                        });
                        rec.nestedGrid.getStore().add(resp);
                        rec.nestedGrid.render(row.childNodes[0].childNodes[0].childNodes[0]);
                        rec.nestedGrid.getEl().swallowEvent(['mouseover', 'mouseout', 'mousedown', 'mouseup', 'click', 'dblclick', 'mousemove']);
                    });
                }
            }
        }
    },
    columns: [
        {
            stateId: 'name',
            header: tr("basereport.objectfield"),
            dataIndex: 'name',
            width: 120
        },
        {
            stateId: 'intervalstart',
            header: tr("statistics.intervalstart"),
            dataIndex: 'sDate',
            width: 120
        },
        {
            stateId: 'intervalfinish',
            header: tr("statistics.intervalfinish"),
            dataIndex: 'fDate',
            width: 120
        },
        {
            stateId: 'points',
            header: tr("statistics.messagescount"),
            sortable: true,
            dataIndex: 'pointsCount',
            width: 80
        },
        {
            stateId: 'dist',
            header: tr("statistics.distance"),
            sortable: true,
            dataIndex: 'distance',
            width: 80,
            renderer: function(val, metaData, rec) {
                return val.toFixed(2) + ' '+tr('movinggroupgrid.km');
            }
        },
        {
            stateId: 'parkInt',
            header: tr('movinggroupgrid.columns.parkingInterval'),
            sortable: true,
            dataIndex: 'parkingInterval',
            width: 140
        },
        {
            stateId: 'parkCount',
            header: tr('movinggroupgrid.columns.parkingCount'),
            sortable: true,
            dataIndex: 'parkingCount',
            width: 140
        },
        {
            stateId: 'moveInt',
            header: tr('movinggroupgrid.columns.movingInterval'),
            sortable: true,
            dataIndex: 'movingInterval',
            width: 140
        },
        {
            stateId: 'moveCount',
            header: tr('movinggroupgrid.columns.movingCount'),
            sortable: true,
            dataIndex: 'movingCount',
            width: 140
        },
        {
            stateId: 'maxSpeed',
            header: tr('movinggroupgrid.columns.maxSpeed'),
            sortable: true,
            dataIndex: 'maxSpeed',
            width: 100,
            renderer: function(val, metaData, rec) {
                return val + ' '+tr('mapobject.laststate.kmsperhour');
            }
        },
        {
            stateId: 'sPlace',
            header: tr('movinggrid.columns.startposition'),
            dataIndex: 'sRegeo',
            width: 250,
            renderer: function(val, metaData, rec) {
                metaData.tdAttr = 'title="' + val + '"';
                return val;
            }
        },
        {
            stateId: 'fPlace',
            header: tr('movinggrid.columns.finishposition'),
            dataIndex: 'fRegeo',
            //width: 250,
            flex:1,
            renderer: function(val, metaData, rec) {
                metaData.tdAttr = 'title="' + val + '"';
                return val;
            }
        }
    ],
    refreshReport: function(settings) {
        var store = this.getStore();

        this.collapseAllRows();
        this.removeAllNestedStories();
        store.each(function(rec) {
            if (rec.nestedGrid) {
                delete rec.nestedGrid;
            }
        });
        this.getSelectionModel().deselectAll();

        var defaultTimezone=Timezone.getDefaultTimezone();
        var tzone=timeZone
        if(defaultTimezone)
            tzone = -moment.tz.zone(defaultTimezone.name).offset(settings.dates.to)
        Ext.apply(store.getProxy().extraParams, {
            from: settings.dates.from,
            to: settings.dates.to,
            selected: settings.selected,
            timezone:tzone

        });
        store.load();
    },
    collapseAllRows: function() {
        var self = this;
        var expander = self.getPlugin('rowExpander');
        var store=self.getStore();
        for(var i = 0; i < store.getCount(); i++) {
            var record = store.getAt(i);
            if(expander.recordsExpanded[record.internalId]){
                expander.toggleRow(i,record);
            }
        }
    },
    removeAllNestedStories: function() {
        var self = this;
        var store=self.getStore();
        for(var i = 0; i < store.getCount(); i++) {
            var record = store.getAt(i);
            if (record && record.nestedGrid) {
                delete record.nestedGrid;
            }
        }
    },
    removeSelection:function(){
        this.getSelectionModel().deselectAll()
    },
    listeners: {
        select: function(view, rec) {
            var mapWidget = this.up('groupreportwnd').down('seniel-mapwidget');
            var groupreportwnd = this.up('groupreportwnd');
            var dates = groupreportwnd.getWorkingDates();
            mapWidget.drawMovingPath(rec.get("uid"), dates.from, dates.to);
        },
        sortchange: function(ct, column, direction) {
            var grid = ct.up('grid');
            grid.collapseAllRows();
            grid.removeAllNestedStories();
        }
    }
});
Ext.define('GroupMovingNestedGrid', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.gmnestedgrid',
    //stateId: 'mgNestedGrid',
    //stateful: true,
    loadMask: true,
    store: {
        fields: ['uid','sDate', 'fDate', 'interval', 'intervalRaw', 'distance', 'pointsCount', 'maxSpeed', 'sLon', 'sLat', 'fLon', 'fLat', 'sRegeo', 'fRegeo', 'isParking'],
        //fields: ["num","name","uid" ,"lat" ,"lon","satelliteNum","speed",
        //    "course","regeo","time","insertTime","devdata"],
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
    border: 1,
    padding: 0,
    columns: [
        {
            stateId: 'sDate',
            header: tr('movinggroupgrid.columns.sDate'),
            sortable: true,
            dataIndex: 'sDate',
            xtype: 'datecolumn',
            format: tr('format.extjs.datetime'),
            width: 153
        },
        {
            stateId: 'type',
            header: tr('movinggroupgrid.columns.intervaltype'),
            sortable: true,
            dataIndex: 'isParking',
            width: 140,
            renderer: function(val, metaData, rec) {
                return (val)?((rec.get('intervalRaw') > 5 * 60 * 1000)?
                    (tr('movinggroupgrid.parking')):(tr('movinggroupgrid.smallparking'))):(tr('movinggroupgrid.moving'));
            }
        },
        {
            stateId: 'interval',
            header: tr('movinggroupgrid.columns.interval'),
            sortable: true,
            dataIndex: 'interval',
            width: 140
        },
        {
            stateId: 'dist',
            header: tr('movinggrid.columns.distance'),
            sortable: true,
            dataIndex: 'distance',
            width: 80,
            renderer: function(val, metaData, rec) {
                return val.toFixed(2) + ' '+tr('movinggroupgrid.km');
            }
        },
        {
            stateId: 'points',
            header: tr('movinggrid.columns.pointscount'),
            sortable: true,
            dataIndex: 'pointsCount',
            width: 80
        },
        {
            stateId: 'maxSpeed',
            header: tr('movinggroupgrid.columns.maxSpeed'),
            sortable: true,
            dataIndex: 'maxSpeed',
            width: 100,
            renderer: function(val, metaData, rec) {
                return val + ' ' + tr('mapobject.laststate.kmsperhour');
            }
        },
        {
            stateId: 'sPlace',
            header: tr('movinggrid.columns.startposition'),
            dataIndex: 'sRegeo',
            width: 250,
            renderer: function(val, metaData, rec) {
                metaData.tdAttr = 'title="' + val + '"';
                return val;
            }
        },
        {
            stateId: 'fPlace',
            header: tr('movinggrid.columns.finishposition'),
            dataIndex: 'fRegeo',
            width: 250,
            renderer: function(val, metaData, rec) {
                metaData.tdAttr = 'title="' + val + '"';
                return val;
            }
        }
    ],
    listeners: {
        select: function(view, rec) {
            var mapWidget = this.parentGridView.up('groupreportwnd').down('seniel-mapwidget')//.addPositionMarker(rec.get('lon'), rec.get('lat'));
            this.parentGridView.getSelectionModel().deselectAll();
            if (!rec.get('isParking')) {
                mapWidget.drawMovingPath(rec.get("uid"), rec.get('sDate'), rec.get('fDate'));
            } else {
                var isParking = (rec.get('intervalRaw') > 5 * 60 * 1000)?(true):(false);
                mapWidget.addParkingMarker(rec.get('sLon'), rec.get('sLat'), isParking);
            }
        }
    }
});
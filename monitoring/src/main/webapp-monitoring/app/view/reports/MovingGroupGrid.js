Ext.define('Seniel.view.reports.MovingGroupGrid', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.movinggroupgrid',
    stateId: 'repMvGrGrid',
    stateful: true,
    title: tr('main.reports.intervals'),
    loadMask: true,
    loadBeforeActivated: false,
    manualLoad: true,
    dockedToolbar: [/*'refresh', 'search'*/],
    storeName: 'EDS.store.MovingGroupReport',
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
                var mapWidget = this.up('reportwnd').down('seniel-mapwidget'),
                    self = this;
                if (!rec.nestedGrid) {
                    movingGroupReport.getReportPerDay(mapWidget.repObjectId, rec.get('sDate'), rec.get('fDate'), function(resp) {
                        rec.nestedGrid = Ext.create('MovingGroupNestedGrid', {
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
            stateId: 'sDate',
            header: tr('movinggroupgrid.columns.sDate'),
            sortable: true,
            dataIndex: 'sDate',
            xtype: 'datecolumn',
            format: tr('format.extjs.date.withtimezone'),
            width: 160
        },
        {
            stateId: 'parkInt',
            header: tr('movinggroupgrid.columns.parkingInterval'),
            sortable: true,
            dataIndex: 'parkingInterval',
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
            width: 250,
            renderer: function(val, metaData, rec) {
                metaData.tdAttr = 'title="' + val + '"';
                return val;
            }
        }
    ],
    refreshReport: function(settings) {
        var store = this.getStore();
        
        this.collapseAllRows();
        store.each(function(rec) {
            if (rec.nestedGrid) {
                delete rec.nestedGrid;
            }
        });
        this.getSelectionModel().deselectAll();
        
        Ext.apply(store.getProxy().extraParams, {
            from: settings.dates.from,
            to: settings.dates.to,
            selected: settings.selected
        });
        store.load();
    },
    collapseAllRows: function() {
        var self = this,
            expander = self.getPlugin('rowExpander'),
            rec;
        
        for (var i in expander.recordsExpanded) {
            rec = self.getStore().findRecord('sDate', i);
            if (rec && expander.recordsExpanded[i]) {
                expander.toggleRow(rec.index, rec);
            }
            if (rec && rec.nestedGrid) {
                delete rec.nestedGrid;
            }
        }
    },
    listeners: {
        select: function(view, rec) {
            var mapWidget = this.up('reportwnd').down('seniel-mapwidget');
            mapWidget.drawMovingPath(mapWidget.repObjectId, rec.get('sDate'), rec.get('fDate'));
        },
        sortchange: function(ct, column, direction) {
            var grid = ct.up('grid');
            grid.collapseAllRows();
        }
    }
});

Ext.define('MovingGroupNestedGrid', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.mgnestedgrid',
    stateId: 'mgNestedGrid',
    stateful: true,
    loadMask: true,
    store: {
        fields: ['sDate', 'fDate', 'interval', 'intervalRaw', 'distance', 'pointsCount', 'maxSpeed', 'sLon', 'sLat', 'fLon', 'fLat', 'sRegeo', 'fRegeo', 'isParking'],
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
            var mapWidget = this.parentGridView.up('reportwnd').down('seniel-mapwidget');
            this.parentGridView.getSelectionModel().deselectAll();
            if (!rec.get('isParking')) {
                mapWidget.drawMovingPath(mapWidget.repObjectId, rec.get('sDate'), rec.get('fDate'));
            } else {
                var isParking = (rec.get('intervalRaw') > 5 * 60 * 1000)?(true):(false);
                mapWidget.addParkingMarker(rec.get('sLon'), rec.get('sLat'), isParking);
            }
        }
    }
});
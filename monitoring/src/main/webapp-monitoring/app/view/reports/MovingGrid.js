Ext.define('Seniel.view.reports.MovingGrid', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.movinggrid',
    stateId: 'repMoveGrid',
    stateful: true,
    title: tr('movinggrid.title'),
    loadMask: true,
    loadBeforeActivated: false,
    manualLoad: true,
    dockedToolbar: [],
    dockedItems: [],
    lbar: [
        {
            icon: 'images/ico16_zoomout.png',
            iconAlign: 'top',
            text: ' ',
            height: 22,
            width: 32,
            tooltip: tr('movinggrid.returnprevroute'),
            tooltipType: 'title',
            scale: 'small',
            handler: function(btn) {
                var mapWidget = btn.up('reportwnd').down('seniel-mapwidget');
                mapWidget.drawMovingPath(mapWidget.repObjectId, mapWidget.dateFrom, mapWidget.dateTo);

                var dataPanels = btn.up('reportwnd').down('panel[itemId="repdataPanel"]').items.items;
                for (var i = 0; i < dataPanels.length; i++) {
                    if (dataPanels[i].hasView) {
                        dataPanels[i].getView().getSelectionModel().deselectAll();
                    }
                }
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
                var grid = btn.up('movinggrid');
                Ext.ux.grid.Printer.print(grid);
            }
        }
    ],
    storeName: 'EDS.store.MovingReport',
    columns: [
        {
            stateId: 'num',
            header: '№',
            dataIndex: 'num',
            width: 30
        },
        {
            stateId: 'sDate',
            header: tr('movinggrid.columns.startmoving'),
            sortable: true,
            dataIndex: 'sDatetime',
            xtype: 'datecolumn',
            format: tr('format.extjs.datetime'),
            width: 160
        },
        {
            stateId: 'interval',
            header: tr('movinggrid.columns.duration'),
            sortable: true,
            dataIndex: 'interval',
            width: 100
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
            header: tr('movinggrid.columns.speed'),
            sortable: true,
            dataIndex: 'maxSpeed',
            width: 80,
            renderer: function(val, metaData, rec) {
                return val + ' '+tr('mapobject.laststate.kmsperhour');
            }
        },
        {
            stateId: 'sPos',
            header: tr('movinggrid.columns.startposition'),
            dataIndex: 'sRegeo',
            width: 350,
            renderer: function(val, metaData, rec) {
                metaData.tdAttr = 'title="' + val + '"';
                return val;
            }
        },
        {
            stateId: 'fPos',
            header: tr('movinggrid.columns.finishposition'),
            dataIndex: 'fRegeo',
            width: 350,
            renderer: function(val, metaData, rec) {
                metaData.tdAttr = 'title="' + val + '"';
                return val;
            }
        }
    ],
    refreshReport: function(settings) {
        var store = this.getStore();
        
        Ext.apply(store.getProxy().extraParams, {
            from: settings.dates.from,
            to: settings.dates.to,
            selected: settings.selected
        });
        store.load();
    },
    listeners: {
        select: function(view, rec) {
            var mapWidget = this.up('reportwnd').down('seniel-mapwidget');
            mapWidget.drawMovingPath(mapWidget.repObjectId, rec.get('sDatetime'), rec.get('fDatetime'));
        },
        activate: function (grid, eOpts) {
            grid.getSelectionModel().deselectAll();
        }
    }
});
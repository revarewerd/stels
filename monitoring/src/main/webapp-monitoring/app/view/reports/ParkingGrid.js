Ext.define('Seniel.view.reports.ParkingGrid', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.parkinggrid',
    stateId: 'repParkGrid',
    stateful: true,
//    selType: 'checkboxmodel',
//    selModel: {
//        showHeaderCheckbox: true,
//        ignoreRightMouseSelection: true,
//        checkOnly: true,
//        mode: 'MULTI'
//    },
    title: tr('parkinggrid.title'),
    loadMask: true,
    loadBeforeActivated: false,
    manualLoad: true,
    dockedToolbar: [],
    dockedItems: [],
    lbar: [
        {
            icon: 'images/ico16_show.png',
            iconAlign: 'top',
            text: ' ',
            height: 22,
            width: 32,
            tooltip: tr('parkinggrid.showparkingsonmap'),
            tooltipType: 'title',
            scale: 'small',
            enableToggle: true,
            toggleHandler: function(btn, pressed) {
                var mapWidget = btn.up('reportwnd').down('reportmap');
                if (pressed) {
                    mapWidget.parkingLayer.setVisibility(true);
                    btn.setTooltip(tr('parkinggrid.stopshowparkingsonmap'));
                } else {
                    mapWidget.parkingLayer.setVisibility(false);
                    btn.setTooltip(tr('parkinggrid.showparkingsonmap'));
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
                var grid = btn.up('parkinggrid');
                Ext.ux.grid.Printer.print(grid);
            }
        }
    ],
    storeName: 'EDS.store.ParkingReport',
    
    columns: [
        {
            stateId: 'num',
            header: '№',
            dataIndex: 'num',
            width: 30
        },
        {
            stateId: 'type',
            header: tr('parkinggrid.columns.intervalType'),
            sortable: true,
            dataIndex: 'isSmall',
            width: 80,
            renderer: function(isStop, metaData, rec) {
                if (!isStop) {
                    return tr('movinggroupgrid.parking');
                } else {
                    return tr('movinggroupgrid.smallparking');
                }
            }
        },
        {
            stateId: 'sDate',
            header: tr('movinggroupgrid.columns.sDate'),
            sortable: true,
            dataIndex: 'datetime',
            xtype: 'datecolumn',
            format: tr('format.extjs.datetime'),
            width: 140
        },
        {
            stateId: 'interval',
            header: tr('movinggrid.columns.duration'),
            sortable: true,
            dataIndex: 'interval',
            width: 140
        },
        {
            stateId: 'points',
            header: tr('movinggrid.columns.pointscount'),
            dataIndex: 'pointsCount',
            width: 80
        },
        {
            stateId: 'place',
            header: tr('pathgrid.columns.place'),
            dataIndex: 'regeo',
            flex: 1,
            renderer: function(val, metaData, rec) {
                metaData.tdAttr = 'title="' + val + '"';
                return val;
            }
        }
    ],
    refreshReport: function(settings) {
        console.log("parking refreshReport");
        var reportWnd = this.up('reportwnd');
        var store = this.getStore();
        var self = this;
        
        Ext.apply(store.getProxy().extraParams, {
            from: settings.dates.from,
            to: settings.dates.to,
            selected: settings.selected
        });
        store.load({
            scope: this,
            callback: function(records, operation, success) {
                reportWnd.down('seniel-mapwidget').drawParkingPoints(records);
            }
        });
    },
    selectRowInParkingGrid: function(rowNum) {
        var grid = this,
            wnd = grid.up('reportwnd');
        grid.getSelectionModel().deselectAll();
        wnd.down('tabpanel').setActiveTab('parking');
        grid.getSelectionModel().select(rowNum);
    },
    listeners: {
        select: function(view, rec) {
            var isParking = (rec.get('intervalRaw') >= 5 * 60 * 1000)?(true):(false);
            this.up('reportwnd').down('seniel-mapwidget').addParkingMarker(rec.get('lon'), rec.get('lat'), isParking);
        },
        activate: function (grid, eOpts) {
            grid.getSelectionModel().deselectAll();
        }
    }
});
Ext.define('Seniel.view.reports.FuelingGrid', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.fuelinggrid',
    stateId: 'repFuelGrid',
    stateful: true,
    title: tr('main.reports.fuelings'),
    loadMask: true,
    loadBeforeActivated: false,
    manualLoad: true,
    dockedToolbar: [],
    dockedItems: [],
    lbar: [
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
                var grid = btn.up('fuelinggrid');
                Ext.ux.grid.Printer.print(grid);
            }
        }
    ],
    storeName: 'EDS.store.FuelingReport',
    columns: [
        {
            stateId: 'num',
            header: '№',
            dataIndex: 'num',
            width: 30
        },
        {
            stateId: 'time',
            header: tr('fuelinggrid.columns.datetime'),
            sortable: true,
            dataIndex: 'datetime',
            xtype: 'datecolumn',
            format: tr('format.extjs.datetime'),
            width: 160
        },
        {
            stateId: 'type',
            header: tr('fuelinggrid.columns.type'),
            sortable: true,
            dataIndex: 'isFueling',
            width: 80,
            renderer: function(val, metaData, rec) {
                return (val)?(tr('fuelinggrid.fueling')):(tr('fuelinggrid.draining'));
            }
        },
        {
            stateId: 'volume',
            header: tr('fuelinggrid.columns.volume'),
            sortable: true,
            dataIndex: 'volumeCheck',
            width: 80,
            renderer: function(val, metaData, rec) {
                val = (val > 0)?(val):(rec.get('volume'));
                return val + ' ' + tr('units.fuellevel');
            }
        },
//        {
//            header: 'Сообщения',
//            sortable: true,
//            dataIndex: 'pointsCount',
//            width: 80
//        },
//        {
//            header: 'Интервал',
//            sortable: true,
//            dataIndex: 'interval',
//            width: 80
//        },
        {
            stateId: 'startVal',
            header: tr('fuelinggrid.columns.startval'),
            sortable: true,
            dataIndex: 'startVal',
            width: 120,
            renderer: function(val, metaData, rec) {
                return val + ' ' + tr('units.fuellevel');
            }
        },
        {
            stateId: 'endVal',
            header: tr('fuelinggrid.columns.endval'),
            sortable: true,
            dataIndex: 'endVal',
            width: 120,
            renderer: function(val, metaData, rec){
                return val + ' ' + tr('units.fuellevel');
            }
        },
//        {
//            header: 'Объём',
//            sortable: true,
//            dataIndex: 'volume',
//            width: 80,
//            renderer: function(val, metaData, rec) {
//                var unit = (rec.get('volume') === rec.get('volumeRaw'))?('ед.'):('л');
//                return val + ' ' + unit;
//            }
//        },
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
            this.up('reportwnd').down('seniel-mapwidget').addPositionMarker(rec.get('lon'), rec.get('lat'));
        }
    }
});
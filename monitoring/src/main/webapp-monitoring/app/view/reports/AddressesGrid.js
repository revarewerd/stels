Ext.define('Seniel.view.reports.AddressesGrid', {
    extend:'Ext.grid.Panel',
    alias: 'widget.addressesgrid',
    stateId: 'repAdrGrid',
    stateful: true,
    title: tr('main.reports.visitedaddrs'),
    lbar: [
        {
            icon: 'images/ico16_minus_def.png',
            iconAlign: 'top',
            text: ' ',
            height: 22,
            width: 32,
            tooltip: tr('addressesgrid.expandreport'),
            tooltipType: 'title',
            scale: 'small',
            enableToggle: true,
            toggleHandler: function(btn, pressed) {
                var grid = btn.up('addressesgrid');
                if (pressed) {
                    btn.setIcon('images/ico16_plus_def.png');
                    btn.setTooltip(tr('addressesgrid.collapsereport'));
                    grid.getView().getFeature('addressesGrouping').expandAll();
                } else {
                    btn.setIcon('images/ico16_minus_def.png');
                    btn.setTooltip(tr('addressesgrid.expandreport'));
                    grid.getView().getFeature('addressesGrouping').collapseAll();
                }
            }
        },
        "-",
        {
            icon: 'images/ico16_printer.png',
            iconAlign: 'top',
            text: ' ',
            height: 22,
            width: 32,
            tooltip: tr('addressesgrid.printreport'),
            tooltipType: 'title',
            scale: 'small',
            handler: function(btn) {
                var grid = btn.up('addressesgrid');
                Ext.ux.grid.Printer.print(grid);
            }
        },
        {
            icon: 'images/ico16_msghist.png',
            iconAlign: 'top',
            text: ' ',
            height: 22,
            width: 32,
            tooltip: tr('addressesgrid.emailreport'),
            tooltipType: 'title',
            scale: 'small',
            handler: function(btn) {
                var grid = btn.up('addressesgrid');
                var url = 'uid=' + grid.settings.selected +
                    '&from=' + encodeURIComponent(grid.settings.dates.from) +
                    '&to=' + encodeURIComponent(grid.settings.dates.to);
                var wnd = Ext.create('Seniel.view.reports.SendMailWindow', {
                    urlPrefix: url
                });
                wnd.show();
            }
        }
    ],
    features: [{
        id: 'addressesGrouping',
        ftype: 'grouping',
        groupHeaderTpl: tr('addressesgrid.columns.date')+': {name}',
        hideGroupedHeader: true,
        enableGroupingMenu: false,
        startCollapsed: true
    }],
    columns: [
        {
            stateId: 'num',
            header: '№',
            dataIndex: 'num',
            width: 40
        },
        {
            stateId: 'date',
            header: tr('addressesgrid.columns.date'),
            dataIndex: 'date',
            width: 160
        },
        {
            stateId: 'time',
            header: tr('addressesgrid.columns.time'),
            dataIndex: 'time',
            width: 160
        },
        {
            stateId: 'addr',
            header: tr('addressesgrid.columns.visitedaddrs'),
            dataIndex: 'address',
            flex: 1
        }
    ],
    refreshReport: function(settings) {
        var store = this.getStore();
        store.proxy.extraParams.from = settings.dates.from;
        store.proxy.extraParams.to = settings.dates.to;
        store.proxy.extraParams.selected = settings.selected;
        store.removeAll(true);
        store.reload();
        this.settings = settings;
    },
    listeners: {
        select: function(model, rec) {
            this.up('reportwnd').down('seniel-mapwidget').addPositionMarker(rec.get('lon'), rec.get('lat'));
        }
    },

    initComponent: function() {
        Ext.apply(this, {
            store: Ext.create('EDS.store.AddressesReport', {
                autoSync: false,
                groupField: 'date'
            })
        });
        this.callParent(arguments);
    }
});
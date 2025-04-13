/**
 * Created by ivan on 27.07.17.
 */

Ext.define('Seniel.view.reports.TerminalGapsGrid', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.gapsgrid',
    loadBeforeActivated: false,
    loadMask: true,
    title: tr('main.reports.gaps'),
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
                var grid = btn.up('movinggrid');
                Ext.ux.grid.Printer.print(grid);
            }
        }
    ],
    storeName: 'EDS.store.TerminalMessagesGaps',
    columns: [
        {
            header: '№',
            xtype: 'rownumberer',
            width: 30,
            resizable: true
        },
        {
            header: tr('gaps.startTime'),
            dataIndex: 'firstTime',
            xtype: 'datecolumn',
            format: tr('format.extjs.datetime'),
            width: 150
        },
        {
            header: tr('gaps.endTime'),
            dataIndex: 'secondTime',
            xtype: 'datecolumn',
            format: tr('format.extjs.datetime'),
            width: 150
        },
        {
            header: tr('gaps.interval'),
            dataIndex: 'period',
            width: 100
        },
        {
            header: tr('gaps.distance'),
            dataIndex: 'distance',
            width: 100,
            renderer: function (val) { return val.toFixed(2) + ' '+tr('movinggroupgrid.km')}
        },
        {
            header: tr('gaps.startCoords'),
            dataIndex: 'firstCoordinates',
            width: 200
        },
        {
            header: tr('gaps.endCoords'),
            dataIndex: 'secondCoordinates',
            width: 200
        }
    ],
    refreshReport: function(settings) {
        var store = this.getStore();

        Ext.apply(store.getProxy().extraParams, {
            from: settings.dates.from,
            to: settings.dates.to,
            uid: settings.selected,
            report: true
        });
        store.load();
    },
    listeners: {
        select: function(view, rec) {
            // if (!this.up('reportwnd').down('pathgrid').gridViewed) {
            //     this.up('reportwnd').down('tabpanel').setActiveTab('movement');
            //     this.up('reportwnd').down('tabpanel').setActiveTab('gaps'); }
            // this.up('reportwnd').down('pathgrid').selectRowInGrid(rec.get('_firstRow'));
            var mapWidget = this.up('reportwnd').down('seniel-mapwidget');
            mapWidget.drawMovingPath(mapWidget.repObjectId, rec.get('firstTime'), rec.get('secondTime'));
            //var mapWidget = this.up('reportwnd').down('seniel-mapwidget');
        },
        celldblclick: function(/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record) {
            this.up('reportwnd').down('tabpanel').setActiveTab('movement');
            this.up('reportwnd').down('pathgrid').selectRowInGrid(record.get('_firstRow'));
        }
        // activate: function (grid, eOpts) {
        //     grid.getSelectionModel().deselectAll();
        // }
    }
});
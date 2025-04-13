/**
 * Created by ivan on 01.02.17.
 */

Ext.define('Seniel.view.reports.ObjectEventsGrid', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.objecteventsgrid',
    stateId: 'repEventsGrid',
    stateful: true,
    title: tr('main.reports.objectevents'),
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
                mapWidget.removeGeozones();

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
                var grid = btn.up('objecteventsgrid');
                Ext.ux.grid.Printer.print(grid);
            }
        }
    ],
    storeName: 'EDS.store.EventsReport',
    // "num", "time", "uuid", "additionalData"
    columns: [
        {
            stateId: 'num',
            header: '№',
            dataIndex: 'num',
            width: 30
        },
        {
            stateId: 'time',
            header: tr('eventsreportgrid.time'),
            sortable: true,
            dataIndex: 'time',
            width: 140,
            renderer: function (v) {
                return Ext.Date.format(new Date(v),tr('format.extjs.datetime')) ;
            }
        },
        {
            stateId: 'eventType',
            header: tr('eventsreportgrid.type'),
            sortable: true,
            dataIndex: 'type',
            renderer: function(val){
                return tr(val);
            }
        },
        {
            stateId: 'message',
            header: tr('eventsreportgrid.message'),
            flex: 1,
            dataIndex: 'message',
            renderer: function(val){
                return tr(val);
            }
        }

    ],
    refreshReport: function(settings) {
        console.log("parking refreshReport");
        var store = this.getStore();

        Ext.apply(store.getProxy().extraParams, {
            from: settings.dates.from,
            to: settings.dates.to,
            selected: settings.selected
        });
        store.load({
            scope: this,
            callback: function(records, operation, success) {
                console.log("Events grid records are ")
                Ext.Array.each(records, function(v) {
                   console.log(v.data);
                });
            }
        });
        this.getSelectionModel().deselectAll();
    },

    listeners: {
        select: function(view, rec) {
            var map = this.up('reportwnd').down('seniel-mapwidget');
            map.removeGeozones();
            var self = this;
            console.log("select ");
            //console.log(rec.get('additionalData'));
            var additionalData = rec.get('additionalData');
            if(additionalData.lon && additionalData.lat)
                map.addPositionMarker(additionalData.lon, additionalData.lat);
            if(additionalData.geozoneId)
            {
                geozonesData.loadById(additionalData.geozoneId, function(rec,resp) {
                    if(rec) {
                        map.drawGeozone(rec);
                    }
                    else {
                        Ext.MessageBox.show({
                            title: tr('eventsreportgrid.loadgzfailure'),
                            msg: tr('eventsreportgrid.loadgzfailuretext'),
                            buttons: Ext.MessageBox.OK,
                            icon: Ext.Msg.WARNING
                        });
                    }
                });
            }
        },
        activate: function (grid, eOpts) {
            grid.getSelectionModel().deselectAll();
        }
    }
});
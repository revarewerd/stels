/**
 * Created by IVAN on 11.08.2015.
 */
Ext.define('Seniel.view.reports.GroupMovementStats', {
    extend: 'Ext.grid.Panel',
    title: tr('main.reports.statistics'),
    initComponent: function () {
        Ext.apply(this, {
            store:
                Ext.create('EDS.store.GroupMovementStats', {
                autoSync: false
            })
        });
        this.callParent(arguments);
    },
    lbar: [
        //{
        //    icon: 'images/ico16_target.png',
        //    iconAlign: 'top',
        //    text: ' ',
        //    height: 22,
        //    width: 32,
        //    tooltip: tr('statistics.showcurrentposition'),
        //    tooltipType: 'title',
        //    scale: 'small',
        //    enableToggle: true,
        //    toggleHandler: function(btn, pressed) {
        //        var mapWidget = btn.up('reportwnd').down('reportmap'),
        //            self = this;
        //        if (pressed && !mapWidget.curposMarker) {
        //            mapObjects.getLonLat([mapWidget.repObjectId], function(result) {
        //                if (result[0]) {
        //                    mapWidget.addCurrentPosMarker(result[0].lon, result[0].lat, result[0].course, result[0].uid);
        //                    self.setTooltip(tr('statistics.hidecurrentposition'));
        //                } else {
        //                    Ext.MessageBox.show({
        //                        title: tr('statistics.currentposition.nodata'),
        //                        msg: tr('statistics.currentposition.nodata.msg'),
        //                        buttons: Ext.MessageBox.OK,
        //                        icon: Ext.Msg.INFO
        //                    });
        //                    btn.toggle();
        //                }
        //            });
        //        } else {
        //            if (mapWidget.curposMarker) {
        //                mapWidget.markersLayer.removeMarker(mapWidget.curposMarker);
        //                mapWidget.curposMarker = null;
        //            } else {
        //                return false;
        //            }
        //            this.setTooltip(tr('statistics.showcurrentposition'));
        //        }
        //    }
        //},
//        {
//            icon: 'images/ico16_loading_def.png',
//            iconAlign: 'top',
//            text: ' ',
//            height: 22,
//            width: 32,
//            tooltip: 'Обновить данные отчёта',
//            tooltipType: 'title',
//            scale: 'small',
//            handler: function(btn) {
//                var mapWidget = btn.up('reportwnd').down('seniel-mapwidget');
//                mapWidget.drawMovingPath(mapWidget.repObjectId, mapWidget.dateFrom, mapWidget.dateTo);
//
//                var dataPanels = btn.up('reportwnd').down('panel[itemId="repdataPanel"]').items.items;
//                for (var i = 0; i < dataPanels.length; i++) {
//                    if (dataPanels[i].hasView) {
//                        dataPanels[i].getView().getSelectionModel().deselectAll();
//                    }
//                }
//
//                var speedGraph = btn.up('reportwnd').down('speedreport');
//                if (speedGraph && speedGraph.dygraph) {
//                    speedGraph.dygraph.updateOptions({
//                        dateWindow: null,
//                        valueRange: null
//                    });
//                }
//                var sensorsGraph = btn.up('reportwnd').down('sensorsreport').down('dygraphpanel');
//                if (sensorsGraph && sensorsGraph.dygraph) {
//                    sensorsGraph.dygraph.updateOptions({
//                        dateWindow: null,
//                        valueRange: null
//                    });
//                }
//            }
//        },
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
                var grid = btn.up('grid');
                Ext.ux.grid.Printer.print(grid);
            }
        },
        //{
        //    icon: 'images/ico16_download.png',
        //    iconAlign: 'top',
        //    text: ' ',
        //    height: 22,
        //    width: 32,
        //    tooltip: tr('basereport.export.title'),
        //    tooltipType: 'title',
        //    scale: 'small',
        //    handler: function(btn) {
        //        var grid = btn.up('grid');
        //        var tabpanel = grid.up('tabpanel');
        //        var params = grid.getStore().getProxy().extraParams;
        //        var reportsList = [];
        //        if (tabpanel && tabpanel.items) {
        //            var tbitems = tabpanel.items.items;
        //            for (var i = 0; i < tbitems.length; i++) {
        //                if (tbitems[i].repCodeName) {
        //                    reportsList.push({label: tbitems[i].title, input: tbitems[i].repCodeName});
        //                    if (tbitems[i].repCodeName === 'sensor') {
        //                        params.sensor = (tbitems[i].curSensor)?(tbitems[i].curSensor):('');
        //                    }
        //                }
        //            }
        //        } else {
        //            reportsList.push({label: tr('main.reports.statistics'), input: 'stat'});
        //        }
        //
        //        var wnd = Ext.create('Seniel.view.reports.ExportWindow', {
        //            reportsList: reportsList,
        //            reportsParams: {
        //                uid: params.selected,
        //                from: params.from,
        //                to: params.to,
        //                sensor: params.sensor
        //            }
        //        });
        //        wnd.show();
        //    }
        //}
    ],
    hideHeaders : true,
    disableSelection: true,
    columns: [
        {
            header: tr('statistics.columns.name'),
            dataIndex: 'name',
            flex: 1
        },
        {
            header: tr('statistics.columns.value'),
            dataIndex: 'value',
            flex: 2,
            renderer: function (val, metaData, rec) {
                metaData.tdAttr = 'style="cursor: pointer !important;" title="' + val + '"';
                return val
            }
        }
    ],
    alias: 'widget.groupmovstatsgrid',
    refreshReport: function (settings) {
        if (this.isHidden())
            this.show();
        var store = this.getStore(),
            self = this;
        store.proxy.extraParams.from = settings.dates.from;
        store.proxy.extraParams.to = settings.dates.to;
        store.proxy.extraParams.selected = settings.selected;
        var defaultTimezone=Timezone.getDefaultTimezone();
        if(defaultTimezone)
            store.proxy.extraParams.timezone = -moment.tz.zone(defaultTimezone.name).offset(settings.dates.to)
        else
            store.proxy.extraParams.timezone = timeZone;
        store.removeAll(true);
        store.reload();
        store.on('load', function(store) {
            var tabs = self.up('groupreportwnd').down('tabpanel').getTabBar().items;
            var isEmpty = (store.getAt(4).get('value') > 0)?false:true;
            for (var i = 1; i < tabs.length; i++) {
                if (isEmpty) {
                    tabs.get(i).setDisabled(true);
                } else {
                    tabs.get(i).setDisabled(false);
                }
            }
        });
    }
});

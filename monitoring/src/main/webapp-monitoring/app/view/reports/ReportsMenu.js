/**
 * Created by ivan on 01.06.16.
 */

Ext.define('Seniel.view.reports.ReportsMenu', {
    alias: 'widget.reportsmenu',
    extend: 'Ext.menu.Menu',
    getViewport: function() {
        return this.parentViewport || this.up('viewport');
    },
    reportsByType: {
        general: {
            text: tr('main.generalreport'),
            icon: 'images/ico16_coldiag.png',
            tooltip: tr('main.generalreport.tooltip'),
            tooltipType: 'title',
            handler: function () {
                var menu = this.up('reportsmenu');
                var viewport = menu.getViewport(),
                    wndid = ++viewport.wndcount;

                var wnd = Ext.create('Seniel.view.reports.BaseWindow', {
                    icon: 'images/ico16_coldiag.png',
                    title: tr('main.report') +' ' + wndid,
                    btnConfig: {
                        icon: 'images/ico24_coldiag.png',
                        text: tr('main.report') + ' ' + wndid
                    },
                    repConfig: {
                        dataPanels: [
                            {
                                type: 'Seniel.view.reports.MovementStats',
                                title: tr('main.reports.statistics') ,
                                itemId: 'movstats',
                                repCodeName: 'stat'
                            },
                            {
                                type: 'Seniel.view.reports.SpeedReport',
                                title: tr('main.reports.speed'),
                                itemId: 'speed',
                                repCodeName: 'speed'
                            },
                            {
                                type: 'Seniel.view.reports.PathGrid',
                                title: tr('main.reports.mesfromobj'),
                                itemId: 'movement'
                            },
                            {
                                type: 'Seniel.view.reports.ParkingGrid',
                                title: tr('main.reports.parkings'),
                                itemId: 'parking',
                                repCodeName: 'parking'
                            },
                            {
                                type: 'Seniel.view.reports.MovingGrid',
                                title: tr('main.reports.movings'),
                                itemId: 'moving',
                                repCodeName: 'moving'
                            },
                            // {
                            //    type: 'Seniel.view.reports.TripGrid',
                            //     title: 'поездки',
                            //     itemId: 'trips',
                            //     repCodeName: 'trips'
                            // },
                            {
                                type: 'Seniel.view.reports.ObjectEventsGrid',
                                title: tr('main.reports.objectevents'),
                                itemId: 'objecteventsgrid',
                                repCodeName: 'objectEvents'
                            },
                            {
                                type: 'Seniel.view.reports.SensorsReport',
                                title: tr('main.reports.sensors'),
                                itemId: 'sensors',
                                repCodeName: 'sensor'
                            },
                            {
                                type: 'Seniel.view.reports.TerminalGapsGrid',
                                title: tr('main.reports.gaps'),
                                itemId: 'gaps',
                                repCodeName: 'gaps'
                            }
                        ],
                        hideToolbar: false
                    }
                });

                viewport.showNewWindow(wnd);
                if(menu.selectedUid)
                    wnd.setSelected(menu.selectedUid);
            }
        },
        fuel: {
            text: tr('main.fuelreport'),
            icon: 'images/ico16_coldiag.png',
            tooltip: tr('main.fuelreport.tip'),
            tooltipType: 'title',
            handler: function() {
                var menu = this.up('reportsmenu');
                var viewport = menu.getViewport(),
                    wndid = ++viewport.wndcount;

                var wnd = Ext.create('Seniel.view.reports.BaseWindow', {
                    icon: 'images/ico16_coldiag.png',
                    title: tr('main.report') + ' ' + wndid,
                    btnConfig: {
                        icon: 'images/ico24_coldiag.png',
                        text: tr('main.report') + ' ' + wndid
                    },
                    repConfig: {
                        dataPanels: [
                            {
                                type: 'Seniel.view.reports.MovementStats',
                                title: tr('basereport.statistics'),
                                itemId: 'fuelmovstats',
                                repCodeName: 'stat'
                            },
                            {
                                type: 'Seniel.view.reports.FuelGraphReport',
                                title: tr('main.reports.fuelgraph'),
                                itemId: 'fuelgraph',
                                repCodeName: 'fuelgraph'
                            },
                            {
                                type: 'Seniel.view.reports.PathGrid',
                                title: tr('main.reports.mesfromobj'),
                                itemId: 'movement'
                            },
                            {
                                type: 'Seniel.view.reports.FuelingGrid',
                                title: tr('main.reports.fuelings'),
                                itemId: 'fueling',
                                repCodeName: 'fueling'
                            }
                        ],
                        hideToolbar: false
                    }
                });

                viewport.showNewWindow(wnd);
                if(menu.selectedUid)
                    wnd.setSelected(menu.selectedUid);
            }
        },
        groupReport: {
            text: tr('main.groupreport'),
            icon: 'images/ico16_coldiag.png',
            tooltip: tr('main.groupreport.tooltip'),
            tooltipType: 'title',
            handler: function() {
                var menu = this.up('reportsmenu');
                var viewport = menu.getViewport(),
                    wndid = ++viewport.wndcount;

                var wnd = Ext.create('Seniel.view.reports.BaseWindow', {
                    icon: 'images/ico16_coldiag.png',
                    title: tr('main.report') + ' ' + wndid,
                    btnConfig: {
                        icon: 'images/ico24_coldiag.png',
                        text: tr('main.report') + ' ' + wndid
                    },
                    repConfig: {
                        dataPanels: [
                            {
                                type: 'Seniel.view.reports.MovingGroupGrid',
                                title: tr('main.reports.intervals'),
                                itemId: 'movgroup'
                            }
                        ],
                        hideToolbar: false
                    }
                });

                viewport.showNewWindow(wnd);
                if(menu.selectedUid)
                    wnd.setSelected(menu.selectedUid);
            }
        },
        addresses: {
            text: tr('main.guisreport'),
            icon: 'images/ico16_coldiag.png',
            tooltip: tr('main.guisreport.tooltip'),
            tooltipType: 'title',
            handler: function() {
                var menu = this.up('reportsmenu');
                var viewport = menu.getViewport(),
                    wndid = ++viewport.wndcount;

                var wnd = Ext.create('Seniel.view.reports.BaseWindow', {
                    icon: 'images/ico16_coldiag.png',
                    title: tr('main.report') + ' ' + wndid,
                    btnConfig: {
                        icon: 'images/ico24_coldiag.png',
                        text: tr('main.report') + ' ' + wndid
                    },
                    repConfig: {
                        dataPanels: [
                            {
                                type: 'Seniel.view.reports.AddressesGrid',
                                title: tr('main.reports.visitedaddrs'),
                                itemId: 'addressesgrid'
                            }
                        ],
                        hideToolbar: false
                    }
                });

                viewport.showNewWindow(wnd);
                if(menu.selectedUid)
                    wnd.setSelected(menu.selectedUid);
            }
        },
        groupOfObjects: {
            text: tr('main.groupsofobjects.report'),
            icon: 'images/ico16_coldiag.png',
            tooltip: tr('main.groupsofobjects.report'),
            tooltipType: 'title',
            handler: function() {
                var viewport = this.up('reportsmenu').getViewport(),
                    wndid = ++viewport.wndcount;

                var wnd = Ext.create('Seniel.view.reports.GroupReportWindow', {
                    icon: 'images/ico16_coldiag.png',
                    title: tr('main.report') + ' ' + wndid,
                    btnConfig: {
                        icon: 'images/ico24_coldiag.png',
                        text: tr('main.report') + ' ' + wndid
                    },
                    repConfig: {
                        dataPanels: [
                            {
                                type: 'Seniel.view.reports.GroupMovementStats',
                                title: tr('basereport.statistics'),
                                itemId: 'groupmovstats',
                                repCodeName: 'stat'
                            },
                            {
                                type: 'Seniel.view.reports.GroupPathGrid',
                                title:  tr('main.reports.mesfromobj'),
                                itemId: 'grouppathgrid',
                                repCodeName: 'path'
                            },
                            {
                                type: 'Seniel.view.reports.GroupMovingGrid',
                                title:  tr('main.reports.intervals'),
                                itemId: 'groupmovinggrid',
                                repCodeName: 'moving'
                            }
                        ],
                        hideToolbar: false
                    }
                });

                viewport.showNewWindow(wnd);
            }
        }
    },
    initComponent: function() {
        var items = [];
        var self = this;
        var reports = this.reports || ['general', 'fuel', 'groupReport', 'addresses', 'groupOfObjects'];
        Ext.Array.each(reports, function(reportType) {
           items.push(self.reportsByType[reportType]) ;
        });
        Ext.apply(this, {
           items: items
        });
        this.callParent();
    }
});
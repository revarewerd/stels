Ext.require([
    'Ext.grid.plugin.BufferedRenderer'
]);
Ext.define('Seniel.view.reports.PathGrid.store', {
    extend: 'Ext.data.Store',
    autoLoad: false,
    fields: ['num', 'name', 'lon', 'lat', 'satelliteNum', 'regeo', 'speed', 'course', {
        name: 'time',
        type: 'date'
    }, {name: 'insertTime', type: 'date'}, "devdata"],
    buffered: true,
    pageSize: 100,
    constructor: function (config) {
        config = Ext.apply({
            proxy: {
                type: 'ajax',
                url: 'pathdata',
                reader: {
                    type: 'json',
                    root: 'items',
                    successProperty: 'success',
                    totalProperty: 'totalCount'
                },
                extraParams: {
                    workingDates: '',
                    selected: '',
                    data: 'grid'
                },
                listeners: {
                    exception: function (proxy, response, operation) {
                        console.log("PathGrid proxy error=", operation.getError());
                        Ext.MessageBox.show({
                            title: 'REMOTE EXCEPTION',
                            msg: operation.getError().statusText,
                            icon: Ext.MessageBox.ERROR,
                            buttons: Ext.Msg.OK
                        });
                    }
                }
            }
        }, config);
        this.superclass.constructor.call(this, config);
    }
});


Ext.define('Seniel.view.reports.PathGrid', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.pathgrid',
    stateId: 'repPathGrid',
    stateful: true,
    title: 'Точки объекта',
    dockedItems: [
        {
            xtype: 'toolbar',
            dock: 'left',
            defaults: {
                xtype: 'button',
                height: 22,
                width: 32,
                scale: 'small',
                tooltipType: 'title'
            },
            items: [
                {
                    icon: 'images/ico16_play.png',
                    iconAlign: 'top',
                    text: ' ',
                    tooltip: tr('pathgrid.play'),
                    play: null,
                    enableToggle: true,
                    toggleHandler: function (btn, pressed) {
                        if (pressed) {
//                            btn.up('reportwnd').down('tabpanel').setActiveTab('movement');
                            var pg = btn.up('window').down('pathgrid'),
                                bt = btn.next();
                            if (!pg.getSelectionModel().getCount()) {
                                pg.getSelectionModel().select(0);
                            }
                            btn.play = setInterval(function () {
                                var si = pg.getSelectionModel().getSelection();
                                if (si[0]) {
                                    if (si[0].get('num') < pg.getStore().getTotalCount()) {
                                        try {
                                            pg.selectRowInGrid(si[0].get('num'));
                                        } catch (exception) {
                                            console.log('Path Play Exception = ', exception);
                                            btn.toggle();
                                        }
                                    } else {
                                        pg.selectRowInGrid(0);
                                    }
                                } else {
                                    btn.toggle();
                                }
                            }, Math.floor(540 / bt));
                            btn.setTooltip(tr('pathgrid.stop'));
                            btn.setIcon('images/ico16_pause.png');
                        } else {
                            if (btn.play) {
                                clearInterval(btn.play);
                                btn.play = null;
                            }
                            btn.setTooltip(tr('pathgrid.play'));
                            btn.setIcon('images/ico16_play.png');
                        }
                    }
                },
                {
                    text: '1x',
                    tooltip: tr('pathgrid.playspeed'),
                    menu: {
                        width: 88,
                        minWidth: 80,
                        selected: null,
                        items: [
                            {text: '1x'},
                            {text: '2x'},
                            {text: '3x'},
                            {text: '4x'},
                            {text: '5x'}
                        ],
                        listeners: {
                            click: function (menu, item) {
                                if (item !== undefined) {
                                    menu.up('button').setText(item.text);
                                    if (menu.selected === null) {
                                        menu.selected = item;
                                    } else {
                                        menu.selected.removeCls('report-toolbar-menu-item-selected');
                                        menu.selected = item;
                                    }
                                    item.addCls('report-toolbar-menu-item-selected');
                                }
                            }
                        }
                    }
                }
            ]
        }
    ],
    initComponent: function () {
        Ext.apply(this, {
            store: Ext.create('Seniel.view.reports.PathGrid.store')
        });

        this.getStore().grid = this;
        this.callParent(arguments);
    },
    sortableColumns: false,
    columns: [
        {
            stateId: 'num',
            header: '№',
            dataIndex: 'num',
            width: 60
        },
        {
            stateId: 'time',
            header: tr('pathgrid.columns.time'),
            dataIndex: 'time',
            xtype: 'datecolumn',
            format: tr('format.extjs.datetime'),
            width: 160
        },
        {
            stateId: 'iTime',
            header: tr('pathgrid.columns.insertTime'),
            dataIndex: 'insertTime',
            xtype: 'datecolumn',
            format: tr('format.extjs.datetime'),
            hidden: true,
            width: 180
        },
        {
            stateId: 'lonlat',
            header: tr('pathgrid.columns.coords'),
            width: 200,
            renderer: function (val, metaData, rec) {
                metaData.tdAttr = 'title="' + tr('pathgrid.latitude') + ': ' + rec.get('lat') + ' \n' + tr('pathgrid.longitude') + ': ' + rec.get('lon') + ' \n(' + tr('mapobject.lastmessage.satellitecount') + ': ' + rec.get('satelliteNum') + ')"';
                return tr('mainmap.latitude.short') + ': ' + rec.get('lat') + '  ' + tr('mainmap.longitude.short') + ': ' + rec.get('lon') + '  (' + rec.get('satelliteNum') + ')';
            }
        },
        {
            stateId: 'speed',
            header: tr('pathgrid.columns.speed'),
            dataIndex: 'speed',
            width: 80
        },
        {
            stateId: 'course',
            header: tr('pathgrid.columns.course'),
            dataIndex: 'course',
            width: 80
        },
        {
            stateId: 'place',
            header: tr('pathgrid.columns.place'),
            dataIndex: 'regeo',
            width: 400
        },
        {
            stateId: 'data',
            header: tr('pathgrid.columns.data'),
            dataIndex: 'devdata',
            minWidth: 600,
            autoSizeColumn: true
        }
    ],
    loadMask: true,
    plugins: 'bufferedrenderer',
    selModel: {
        pruneRemoved: false
    },
    gridViewed: false,
    gridReloaded: false,
    refreshReport: function (settings) {
        this.reportSetting = settings;
        var store = this.getStore();
        Ext.apply(store.getProxy().extraParams, {
            from: settings.dates.from,
            to: settings.dates.to,
            selected: settings.selected
        });
        if (this.gridReloaded) {
            if (this.gridViewed) {
                this.getSelectionModel().deselectAll();
                this.gridViewed = false;
            }
            store.data.clear();
            this.reconfigure(store);
            store.reload();
        } else {
            store.loadPage(1);
        }
    },
    listeners: {
        activate: function (grid, eOpts) {
            grid.getSelectionModel().deselectAll();
            this.up('window').down('reportmap').refreshReport(grid.reportSetting );
        },
        select: function (model, rec) {
            this.up('window').down('seniel-mapwidget').addPositionMarker(rec.get('lon'), rec.get('lat'));
        },
        show: function (self) {
            if (!self.gridViewed) {
                self.gridViewed = true;
                if (self.gridReloaded) {
                    self.getView().refresh();
                    if (self.getStore().getTotalCount() > 0) {
                        self.selectRowInGrid(0);
                        self.getView().deselect(0);
                    }
//                    self.getView().refresh();
                }
            }
            if (self.getSelectionModel().hasSelection()) {
                var selection = self.getSelectionModel().getSelection()[0];
//                console.log("grid reselect ", selection, self.getStore().indexOf(selection))
                self.selectRowInGrid(self.getStore().indexOfTotal(selection));
//                console.log(self);
            }
        },
        viewready: function (grid) {
            grid.getView().getEl().on('scroll', function () {
                if (!grid.isColumnResizing) {
                    grid.isColumnResizing = true;
                    Ext.each(grid.columns, function (column) {
                        if (column.autoSizeColumn === true)
                            column.autoSize();
                    });
                    grid.isColumnResizing = false;
                }
            });
        }
    },
    //Показать строку с данными по выбранной на карте или графике скорости точке в гриде перемещений(pathgrid)
    selectRowInGrid: function (row) {
        this.getView().bufferedRenderer.scrollTo(row, true);
//        this.getView().scrollBy(0, row*21, true);
    }

});
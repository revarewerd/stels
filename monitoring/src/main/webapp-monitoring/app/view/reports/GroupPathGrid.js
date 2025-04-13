/**
 * Created by IVAN on 09.09.2015.
 */
Ext.define('Seniel.view.reports.GroupPathGrid', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.grouppathgrid',
    //stateId: 'repGrPGrid',
    //stateful: true,
    title: tr('main.reports.mesfromobj'),
    loadMask: true,
    loadBeforeActivated: false,
    manualLoad: true,
    dockedToolbar: [/*'refresh', 'search'*/],
    lbar: [
        {
            icon: 'images/ico16_zoomout.png',
            iconAlign: 'top',
            text: ' ',
            height: 22,
            width: 32,
            tooltip: tr('main.groupreport.collapseall'),
            tooltipType: 'title',
            scale: 'small',
            handler: function (btn) {
                var grid = btn.up('grouppathgrid');
                grid.collapseAllRows();
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
            handler: function (btn) {
                var grid = btn.up('grouppathgrid');
                Ext.ux.grid.Printer.print(grid);
            }
        }
    ],
    storeName: 'EDS.store.GroupPathReport',
    plugins: [
        {
            ptype: 'rowexpander',
            pluginId: 'rowExpander',
            rowBodyTpl: new Ext.XTemplate('<div class="nested-grid"></div>'),
            selectRowOnExpand: true
        }
    ],
    viewConfig: {
        listeners: {
            expandbody: function (rowNode, rec, row) {
                var self = this;
                var groupreportwnd = self.up('groupreportwnd');
                var dates = groupreportwnd.getWorkingDates();
                var defaultTimezone = Timezone.getDefaultTimezone();
                var tzone = timeZone
                if (defaultTimezone)
                    tzone = -moment.tz.zone(defaultTimezone.name).offset(dates.to)
                if (!rec.nestedGrid) {
                    groupPathReport.getObjectDayStatReport(rec.get('uid'), dates.from, dates.to,tzone, function (resp) {
                        console.log("resp",resp)
                        rec.nestedGrid = Ext.create('DayStatGrid', {
                            parentGridView: self
                        });
                        rec.nestedGrid.settings = {
                            dates: dates,
                            timezone: tzone,
                            selected:rec.get('uid'),
                            name:rec.get('name')
                        };
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
            stateId: 'name',
            header: tr("basereport.objectfield"),
            dataIndex: 'name',
            width: 120
        },
        {
            stateId: 'intervalstart',
            header: tr("statistics.intervalstart"),
            dataIndex: 'intervalstart',
            width: 120
        },
        {
            stateId: 'intervalfinish',
            header: tr("statistics.intervalfinish"),
            dataIndex: 'intervalfinish',
            width: 120
        },
        {
            stateId: 'messagescount',
            header: tr("statistics.messagescount"),
            dataIndex: 'messagescount',
            width: 130
        },
        {
            stateId: 'distance',
            header: tr("statistics.distance"),
            dataIndex: 'distance',
            width: 120
        },
        {
            stateId: 'maxspeed',
            header: tr("statistics.maxspeed"),
            dataIndex: 'maxspeed',
            width: 130
        },
        {
            stateId: 'avrspeed',
            header: tr("statistics.avrspeed"),
            dataIndex: 'avrspeed',
            width: 120
        },
        {
            stateId: 'lastmessagetime',
            header: tr("statistics.lastmessagetime"),
            dataIndex: 'lastmessagetime',
            width: 130
        },
        {
            stateId: 'lastpostiton',
            header: tr("statistics.lastpostiton"),
            dataIndex: 'lastpostiton',
            flex: 1,
            renderer: function (val, metaData, rec) {
                metaData.tdAttr = 'style="cursor: pointer !important;" title="' + val + '"';
                return val
            }
        }
    ],
    refreshReport: function (settings) {
        var store = this.getStore();

        this.collapseAllRows();
        this.removeAllNestedStories();
        store.each(function (rec) {
            if (rec.nestedGrid) {
                delete rec.nestedGrid;
            }
        });
        this.getSelectionModel().deselectAll();

        var defaultTimezone = Timezone.getDefaultTimezone();
        var tzone = timeZone
        if (defaultTimezone)
            tzone = -moment.tz.zone(defaultTimezone.name).offset(settings.dates.to)
        Ext.apply(store.getProxy().extraParams, {
            from: settings.dates.from,
            to: settings.dates.to,
            selected: settings.selected,
            timezone: tzone

        });
        store.load();
    },
    collapseAllRows: function () {
        var self = this;
        var expander = self.getPlugin('rowExpander');
        var store = self.getStore();
        for (var i = 0; i < store.getCount(); i++) {
            var record = store.getAt(i);
            if (expander.recordsExpanded[record.internalId]) {
                expander.toggleRow(i, record);
            }
        }
    },
    removeAllNestedStories: function () {
        var self = this;
        var store = self.getStore();
        for (var i = 0; i < store.getCount(); i++) {
            var record = store.getAt(i);
            if (record && record.nestedGrid) {
                delete record.nestedGrid;
            }
        }
    },
    selectRowInGrid: function (row) {
        this.getSelectionModel().selectRange(row, row);
        return this.getSelectionModel().getLastSelected();
        //    this.getView().bufferedRenderer.scrollTo(row, true);
        //      this.getView().scrollBy(0, row*21, true);
    },
    removeSelection: function () {
        this.getSelectionModel().deselectAll()
    },
    drawMovingPath:function(rec){
        var groupreportwnd = this.up('window');
        var dates = groupreportwnd.getWorkingDates();
        var mapWidget = this.up('window').down('seniel-mapwidget');
        //mapWidget.removeAllPopups();
        mapWidget.drawMovingPath(rec.get("uid"), dates.from, dates.to);
    },
    listeners: {
        select: function (view, rec) {
            this.up('window').down('grouppathgrid').drawMovingPath(rec);
            if (rec.nestedGrid) {
                rec.nestedGrid.getSelectionModel().deselectAll()
            }
        },
        sortchange: function (ct, column, direction) {
            var grid = ct.up('grid');
            grid.collapseAllRows();
            grid.removeAllNestedStories();
        }
    }
});

Ext.define('DayStatGrid', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.daystatgrid',
    //stateId: 'mgNestedGrid',
    //stateful: true,
    loadMask: true,
    store: {
        fields: ["intervalstart", "intervalfinish", /*"name",*/ "uid", "messagescount", "distance", "maxspeed",
            "avrspeed", "lastmessagetime", "lastpostiton"],
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
        //{
        //    stateId: 'name',
        //    header: tr("basereport.objectfield"),
        //    dataIndex: 'name',
        //    width: 120
        //},
        {
            stateId: 'intervalstart',
            header: tr("statistics.intervalstart"),
            dataIndex: 'intervalstart',
            width: 120,
            renderer: function (val, metaData, rec) {
                var res=val.split(" ")
                return res[0]
            }
        },
        //{
        //    stateId: 'intervalfinish',
        //    header: tr("statistics.intervalfinish"),
        //    dataIndex: 'intervalfinish',
        //    width: 120
        //},
        {
            stateId: 'messagescount',
            header: tr("statistics.messagescount"),
            dataIndex: 'messagescount',
            width: 130
        },
        {
            stateId: 'distance',
            header: tr("statistics.distance"),
            dataIndex: 'distance',
            width: 120
        },
        {
            stateId: 'maxspeed',
            header: tr("statistics.maxspeed"),
            dataIndex: 'maxspeed',
            width: 130
        },
        {
            stateId: 'avrspeed',
            header: tr("statistics.avrspeed"),
            dataIndex: 'avrspeed',
            width: 120
        },
        {
            stateId: 'lastmessagetime',
            header: tr("statistics.lastmessagetime"),
            dataIndex: 'lastmessagetime',
            width: 130
        },
        {
            stateId: 'lastpostiton',
            header: tr("statistics.lastpostiton"),
            dataIndex: 'lastpostiton',
            flex: 1,
            renderer: function (val, metaData, rec) {
                metaData.tdAttr = 'style="cursor: pointer !important;" title="' + val + '"';
                return val
            }
        },
        {
            xtype: 'actioncolumn',
            header: 'Детализация',
            width: 100,
            sortable: false,
            align: 'center',
            items: [
                {
                    icon: 'images/ico16_coldiag.png',
                    //iconCls: 'mousepointer',
                    tooltip: 'Детальный отчет',
                    handler: function (view, rowIndex, colIndex,item,e,record,row) {
                        var grid = this.up('grid');
                        var viewport = grid.parentGridView.up('viewport'),
                            wndid = ++viewport.wndcount;
                        var gridSettings=grid.settings;

                        console.log("record",record);

                        var uid=record.get("uid");
                        var dates= {
                            from :grid.createDateTime(record.get("intervalstart")),
                            to : grid.createDateTime(record.get("intervalfinish"))
                            };

                        var intervalStartDate=record.get("intervalstart").split(" ")[0]
                        var wnd = Ext.create('Seniel.view.reports.OneDayWindow', {
                            icon: 'images/ico16_coldiag.png',
                            title: tr('main.report') + ' ' + gridSettings.name + ' - '+intervalStartDate,
                            btnConfig: {
                                icon: 'images/ico24_coldiag.png',
                                text: tr('main.report') + ' ' + gridSettings.name + ' - '+intervalStartDate,
                            },
                            repConfig: {
                                settings:{
                                    dates: dates,
                                    timezone:gridSettings.timezone,
                                    selected:gridSettings.selected,
                                    name:gridSettings.name
                                },
                                dataPanels: [
                                    {
                                        type: 'Seniel.view.reports.PathGrid',
                                        title: tr('main.reports.mesfromobj'),
                                        itemId: 'movement'
                                    }
                                ],
                                hideToolbar: false
                            }
                        });

                        viewport.showNewWindow(wnd);
                    }
                }
            ]
        },
    ],
    initComponent:function() {
        var self = this;
        Ext.apply(this, {
            listeners: {
                select: function (view, rec) {
                    var settings = self.settings;
                    var mapWidget = self.parentGridView.up('groupreportwnd').down('seniel-mapwidget');
                    self.parentGridView.getSelectionModel().deselectAll();
                    mapWidget.drawMovingPath(settings.selected,  self.createDateTime(rec.get('intervalstart')), self.createDateTime(rec.get('intervalfinish')));
                }
            }
        })
        this.callParent()
    },
    createDateTime: function (dateString) {
        var defaultTimezone = Timezone.getDefaultTimezone()
        if (defaultTimezone) {
            return moment.tz(dateString, "DD.MM.YYYY HH:mm:ss:SSS", defaultTimezone.name).toDate()
        }
        return Ext.Date.parse(dateString,"d.m.Y H:i:s");
    }
});

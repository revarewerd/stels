function sumDateAndTime(date, time) {
    date.setHours(time.getHours(),time.getMinutes(),time.getSeconds(),time.getMilliseconds())
    var fdate=Ext.Date.format(date, "d.m.Y H:i:s:u")
    var defaultTimezone = Timezone.getDefaultTimezone()
    if (defaultTimezone) {
        return moment.tz(fdate, "DD.MM.YYYY HH:mm:ss:SSS", defaultTimezone.name).toDate()
    }
    return date;
};

function ruWeekDay(d) {
    var day = d.getDay();
    if (day === 0)
        return 6;
    else
        return day - 1;
}

Ext.define('Seniel.view.reports.BaseWindow', {
    extend: 'Seniel.view.WRWindow',
    requires: [
        'Seniel.view.reports.AddressesGrid',
        'Seniel.view.reports.ObjectEventsGrid',
        'Seniel.view.reports.MovingGrid',
        'Seniel.view.reports.ParkingGrid',
        'Seniel.view.reports.FuelingGrid',
        'Seniel.view.reports.MovingGroupGrid',
        'Seniel.view.reports.PathGrid',
        'Seniel.view.reports.SpeedReport',
        'Seniel.view.reports.SensorsReport',
        'Seniel.view.reports.FuelGraphReport',
        'Seniel.view.reports.ReportMap',
        'Seniel.view.DygraphPanel',
        'Seniel.view.reports.MovementStats',
        'Seniel.view.WRWindow'
    ],
    alias: 'widget.reportwnd',
    stateId: 'repWnd',
    stateful: true,
    minWidth: 640,
    minHeight: 480,
    layout: 'border',
    items: [
        {
            xtype: 'panel',
            region: 'west',
            title: tr('basereport.settingstitle'),
            minWidth: 200,
            width: 300,
            split: true,
            collapsible: true,
            titleCollapse: true,
            floatable: false,
            items: [
                {
                    xtype: 'container',
                    layout: {
                        type: 'vbox',
                        align: 'stretch'
                    },
                    minHeight: 256,
                    items: [

                        {
                            xtype: 'combobox',
                            itemId: 'selectedObject',
                            fieldLabel: tr('basereport.objectfield'),
                            emptyText: tr('basereport.objectfield.emptytext'),
                            margin: '12 20 6 20',
                            labelWidth: 50,
                            minChars: 0,
                            allowBlank: false,
                            queryMode: 'local',
                            valueField: 'uid',
                            displayField: 'name',
                            forceSelection: true,
                            anyMatch: true,
                            caseSensitive: false,
                            store: {
                                fields: ['uid', 'name'],
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
                            }
                        },
                        {
                            margin: '0 20 12 20',
                            padding: 0,
                            border: 0,
                            xtype: 'panel',
                            defaults: {
                                margin: '0 4 0 4',
                                padding: 0,
                                border: 0,
                                cls: 'report-fastdate-button'
                            },
                            cls: 'report-fastdate-panel',
                            items: [
                                {
                                    xtype: 'button',
                                    text: tr('basereport.pretime.today'),
                                    handler: function () {
                                        var d = new Date();
                                        this.up('reportwnd').down('datefield[itemId="fromDate"]').setValue(d);
                                        this.up('reportwnd').down('datefield[itemId="toDate"]').setValue(d);
                                    }
                                },
                                {
                                    xtype: 'button',
                                    text: tr('basereport.pretime.yesterday'),
                                    handler: function () {
                                        var d = new Date();
                                        d.setDate(d.getDate() - 1);
                                        this.up('reportwnd').down('datefield[itemId="fromDate"]').setValue(d);
                                        this.up('reportwnd').down('datefield[itemId="toDate"]').setValue(d);
                                    }
                                },
                                {
                                    xtype: 'button',
                                    text: tr('basereport.pretime.thisweek'),
                                    handler: function () {
                                        var d = new Date();
                                        this.up('reportwnd').down('datefield[itemId="toDate"]').setValue(d);
                                        d.setDate(d.getDate() - ruWeekDay(d));
                                        this.up('reportwnd').down('datefield[itemId="fromDate"]').setValue(d);
                                    }
                                },
                                {
                                    xtype: 'button',
                                    text: tr('basereport.pretime.prevweek'),
                                    handler: function () {
                                        var d = new Date();
                                        d.setDate(d.getDate() - ruWeekDay(d) - 1);
                                        this.up('reportwnd').down('datefield[itemId="toDate"]').setValue(d);
                                        d.setDate(d.getDate() - 6);
                                        this.up('reportwnd').down('datefield[itemId="fromDate"]').setValue(d);
                                    }
                                },
                                {
                                    xtype: 'button',
                                    text: tr('basereport.pretime.thismonth'),
                                    handler: function () {
                                        var d = new Date();
                                        this.up('reportwnd').down('datefield[itemId="toDate"]').setValue(d);
                                        d.setDate(1);
                                        this.up('reportwnd').down('datefield[itemId="fromDate"]').setValue(d);
                                    }
                                },
                                {
                                    xtype: 'button',
                                    text: tr('basereport.pretime.prevmonth'),
                                    handler: function () {
                                        var d = new Date();
                                        d.setDate(0);
                                        this.up('reportwnd').down('datefield[itemId="toDate"]').setValue(d);
                                        d.setDate(1);
                                        this.up('reportwnd').down('datefield[itemId="fromDate"]').setValue(d);
                                    }
                                }
                            ]
                        },
                        {
                            margin: '0 20 4 20',
                            xtype: 'datefield',
                            itemId: 'fromDate',
                            labelWidth: 50,
                            anchor: '100%',
                            fieldLabel: tr('basereport.fromdate'),
                            name: 'from_date',
                            format:tr('format.extjs.date'),
                            value: new Date()
                        },
                        {
                            margin: '0 20 4 20',
                            xtype: 'timefield',
                            itemId: 'fromTime',
                            labelWidth: 50,
                            anchor: '100%',
                            fieldLabel: tr('basereport.time'),
                            name: 'from_time',
                            format: tr('format.extjs.time'),
                            value: '00:00'
                        },
                        {
                            margin: '0 20 4 20',
                            itemId: 'toDate',
                            xtype: 'datefield',
                            labelWidth: 50,
                            anchor: '100%',
                            fieldLabel: tr('basereport.toDate'),
                            name: 'to_date',
                            format:tr('format.extjs.date'),
                            value: new Date()
                        },
                        {
                            margin: '0 20 12 20',
                            xtype: 'timefield',
                            itemId: 'toTime',
                            labelWidth: 50,
                            anchor: '100%',
                            fieldLabel: tr('basereport.time'),
                            name: 'to_time',
                            format: tr('format.extjs.time'),
                            value: '23:59'
                        },
                        {
                            margin: '0 20 0 20',
                            align: 'center',
                            xtype: 'button',
                            text: tr('basereport.execute'),
                            handler: function () {
                                var reportWnd = this.up('reportwnd'),
                                    selectedObject = reportWnd.getSelected(),
                                    dates = reportWnd.getWorkingDates();


                                if (!selectedObject) {
                                    Ext.MessageBox.show({
                                        title: tr('basereport.execute.noobject'),
                                        msg: tr('basereport.execute.noobject.msg'),
                                        buttons: Ext.MessageBox.OK,
                                        icon: Ext.Msg.WARNING
                                    });
                                } else if (!dates) {
                                    Ext.MessageBox.show({
                                        title: tr('basereport.execute.incorrectdate'),
                                        msg: tr('basereport.execute.incorrectdate.msg'),
                                        buttons: Ext.MessageBox.OK,
                                        icon: Ext.Msg.WARNING
                                    });
                                } else {
                                    var repWnd = this.up('window');
                                    var confPanels = repWnd.repConfig.dataPanels;
                                    var tr2 = tr('basereport.data');
                                    console.log("tr2:", tr2);
                                    var dataPanel = repWnd.down('panel[title="'+ tr2+'"]'),
                                        rdPanel = dataPanel.getComponent('repdataPanel'),
                                        rdItems = [];//rdPanel.getComponent('movstats'), rdPanel.getComponent('speed'), rdPanel.getComponent('movement'), rdPanel.getComponent('parking'), rdPanel.getComponent('moving')];
                                    
                                    for (var i = 0; i < confPanels.length; i++) {
                                        rdItems.push(rdPanel.getComponent(confPanels[i].itemId));
                                    }
                                    if (dataPanel.isHidden()) {
//                                        console.log('report height = ', (repWnd.getHeight() - 256));
                                        if(!Ext.util.Cookies.get('ext-reportWndData'))
                                            dataPanel.setHeight((repWnd.getHeight() - 296));
                                        dataPanel.show();
                                    } else {
                                        if (rdPanel.getComponent('movement')) {
                                            rdPanel.getComponent('movement').gridReloaded = true;
                                        }
                                    }

                                    var win = this.up('window'),
                                        objbox = win.down('combobox[itemId="selectedObject"]'),
                                        reports = [win.down('seniel-mapwidget')].concat(rdItems);
                                    win.setTitle(tr('basereport.reportfor')+' &laquo;' + objbox.getRawValue() + '&raquo;');
//                                    console.log('Кнопка окна=', win.ctrlbtn);
                                    win.ctrlbtn.setText(tr('basereport.reportfor')+' &laquo;' + objbox.getRawValue() + '&raquo;');

                                    var settings = {
                                        dates: reportWnd.getWorkingDates(),
                                        selected: reportWnd.getSelected()
                                    };
//                                    var buttons = win.down('toolbar').items;
//
//                                    for (var i = 0; i < buttons.length; i++) {
//                                        if (buttons.items[i].pressed) {
//                                            buttons.items[i].toggle();
//                                        }
//                                    }

                                    for (var i = 0; i < reports.length; i++) {
                                        reports[i].refreshReport(settings);
                                    }

                                    if(reportWnd.tabShown)
                                    {
                                        for(var k in reportWnd.tabShown)
                                            reportWnd.tabShown[k] = false;
                                        if(rdItems.length > 0)
                                        {
                                            reportWnd.postStats(rdItems[0].itemId);
                                            reportWnd.tabShown[rdItems[0].itemId] = true;
                                        }

                                    }
                                }
                            }
                        }
                    ]
                }
            ]
        },
        {
            region: 'center',
            title: tr('basereport.map'),
            xtype: 'reportmap',
            split: true,
            minHeight: 256
        },
        {
            stateful: true,
            stateId: 'reportWndData',
            region: 'south',
            title: tr('basereport.data'),
            header: {height: 0},
            width: 200,
            height: 180,
            minHeight: 120,
            hidden: true,
            collapsible: true,
            titleCollapse: true,
            floatable: false,
            split: true,
            layout: {
                type: 'hbox',
                align: 'stretch'
            },
            items: [
                
            ]
        }
    ],
    initComponent: function() {
        this.callParent(arguments);
        var self = this;
        this.on('boxready', function(wnd) {
            var viewport = wnd.up('viewport'),
                cb = wnd.down('combobox'),
                rm = wnd.down('reportmap');

            cb.getStore().add(viewport.down('mapobjectlist').roData);
            cb.select(cb.getStore().getAt(0));

            var curMap = Ext.util.Cookies.get('currentMapLayer');
            if (!curMap) {
                curMap = 'openstreetmaps';
            }
            rm.map.events.un({'changelayer': rm.onChangeLayer});
            rm.map.setBaseLayer(rm.map.getLayersBy('codeName', curMap)[0]);

            var mainPanel = wnd.down('panel[title="'+tr('basereport.data')+'"]');
            self.tabShown = {};
            if (wnd.repConfig && wnd.repConfig.dataPanels) {
                var widgets = wnd.repConfig.dataPanels;
                for (var i = 0; i < widgets.length; i++) {
                    widgets[i] = Ext.create(widgets[i].type, {
                        itemId: widgets[i].itemId,
                        title: widgets[i].title,
                        repCodeName: widgets[i].repCodeName
                    });
                    self.tabShown[widgets[i].itemId] = false;
                }
            } else {
                var widgets = [
                    {
                        type: 'Seniel.view.reports.MovementStats',
                        title: tr('basereport.statistics'),
                        itemId: 'movstats',
                        repCodeName: 'stat'
                    }
                ];
            }
            
            if (widgets.length > 1) {
                mainPanel.add(Ext.create('Ext.tab.Panel', {
                    flex: 1,
                    floatable: true,
                    title: tr('basereport.reportdata'),
                    itemId: 'repdataPanel',
                    header: false,
                    items: widgets,
                    listeners: {
                        tabchange: function( tabPanel, newCard)
                        {
                            if(!self.tabShown[newCard.itemId]) {
                                // console.log("Selected tab " + newCard.itemId);
                                self.tabShown[newCard.itemId] = true;
                                self.postStats(newCard.itemId);
                            }
                            // else
                            //     console.log("Tab " + newCard.itemId + " was already shown")
                        }
                    }
                }));
            } else {
                widgets[0].region = 'center';
                mainPanel.add(Ext.create('Ext.panel.Panel', {
                    flex: 1,
                    floatable: true,
                    title:  tr('basereport.reportdata'),
                    itemId: 'repdataPanel',
                    header: false,
                    layout: 'border',
                    items: [
                        widgets[0]
                    ]
                }));
            }
        });
    },

    //---------
    // Функциии
    //---------
    
    //Получить объект выбранный из списка
    getSelected: function() {
        //console.log("selectedObject=", this.down('combobox[itemId="selectedObject"]').getValue());
        return this.down('combobox[itemId="selectedObject"]').getValue();
    },
    setSelected: function(uid) {
        //console.log("setSelected uid = " + uid);
       this.down('combobox[itemId="selectedObject"]').setValue(uid);
    },
    //Получить указанные даты
    getWorkingDates: function() {
        //var fromdate = this.getReportsPanel().down('datefield[name="from_date"]').getValue();
        var fromdate = this.down('datefield[itemId="fromDate"]').getValue();
        //console.log('fromdate=', fromdate)
        //var fromtime = this.getReportsPanel().down('timefield[name="from_time"]').getValue();
        var fromtime = this.down('timefield[itemId="fromTime"]').getValue()// - new Date(2008,0,1).getTime();
        //console.log('fromtime=', fromtime)
        //var todate = this.getReportsPanel().down('datefield[name="to_date"]').getValue();
        var todate = this.down('datefield[itemId="toDate"]').getValue();
        //console.log('todate=', todate)
        //var totime = this.getReportsPanel().down('timefield[name="to_time"]').getValue();
        var totime = this.down('timefield[itemId="toTime"]').getValue()// - new Date(2008,0,1).getTime();
        //console.log('totime=', totime)
        var currnt = new Date(),
            compto = sumDateAndTime(todate, totime);

        if  (compto.getTime() > currnt.getTime()) {
            totime = currnt// - new Date().setHours(0,0,0,0);
            console.log("toTime = current")
        }

        if (fromdate > todate)
            return null;
        else {
            var result = {
                from: sumDateAndTime(fromdate, fromtime).getTime(),
                to: sumDateAndTime(todate, totime).getTime()
            }
            console.log("from",Ext.Date.format(new Date(result.from),"d.m.Y H:i"))
            console.log("to",Ext.Date.format(new Date(result.to),"d.m.Y H:i"))
            return result;
        }

    },

    postStats: function(reportType)
    {
        var dates = this.getWorkingDates();
        var target = this.getSelected();
        Ext.Ajax.request({
            url: 'EDS/ReportStats',
            callback: function() {},
            params:
            {
                reportType: reportType,
                from: dates.from,
                to: dates.to,
                target: target
            }
        })
    }
});
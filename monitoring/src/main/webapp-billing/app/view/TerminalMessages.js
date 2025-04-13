function sumDateAndTime(date, time) {
    var res = new Date(date);
    res.setHours(time.getHours(), time.getMinutes(), time.getSeconds(), time.getMilliseconds());
    return res;
}
Ext.require([
    'Ext.grid.plugin.BufferedRenderer'
]);

Ext.define('Billing.view.TerminalMessagesUploadWnd', {
    extend: 'Ext.window.Window',
    title: 'Сообщения от объекта',
    width: 500,
    height: 300,
    maximizable: true,
    layout: 'fit',
    items: [
        {
            xtype:'form',
            bodyPadding: '10 10 0',
            api: {
                submit: GPSDataExport.uploadFile
            },
            defaults: {
                anchor: '100%',
                msgTarget: 'side',
                labelWidth: 50
            },

            items: [
                {
                    xtype: 'hiddenfield',
                    name: 'uid',
                    value: ''
                },
                {
                    xtype: 'combobox',
                    fieldLabel: 'Формат',
                    name: 'datatype',
                    allowBlank: false,
                    forceSelection: true,
                    editable: false,
                    value: 'Mongodump',
                    //queryMode: 'local',
                    store: [
                        ['Mongodump', 'Mongo dump']
                    ]
                },
                {
                    xtype: 'filefield',
                    emptyText: 'Выберите файл',
                    fieldLabel: 'Файл',
                    name: 'datafile',
                    buttonText: 'Выбрать'
                }
            ],

            buttons: [
                {
                    text: 'Отправить',
                    handler: function () {
                        var form = this.up('form').getForm();
                        var self = this;
                        if (form.isValid()) {
                            form.submit({
                                //url: 'EDS/dataexport/upload',
                                waitMsg: 'Данные загружаются...',
                                success: function (fp, o) {
                                    Ext.Msg.show({
                                        title: "Загружено",
                                        msg: "Данные успешно загружены",
                                        minWidth: 200,
                                        modal: true,
                                        icon: Ext.Msg.INFO,
                                        buttons: Ext.Msg.OK
                                    });
                                    self.up('window').close();
                                },
                                failure: function (f, o) {
                                    console.log('Произошла ошибка', o.result.errors);
                                    var errMsg=""
                                    for(i in o.result.errors){
                                        errMsg+=o.result.errors[i][0]+"\n"
                                    }
                                    Ext.Msg.alert("Произошла ошибка",errMsg);
                                }
                            });
                        }
                    }
                }
            ]
        }
    ]});

Ext.define('Billing.view.TerminalMessagesPanel', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.terminalpathgrid',
    initComponent: function () {
        var self = this;
        Ext.apply(this, {
            store: Ext.create('EDS.store.TerminalMessagesService', {
                buffered: true,
                //pageSize: 200,
                autoLoad: true,
                autoSync: false,
                listeners: {
                    beforeload: function (store, op) {
                        console.log('grid beforeload');
                        //console.log('self rec',self.up('panel').getRecord().get("uid"))

                        store.getProxy().setExtraParam("uid", self.uid);
                        if (self.down('[name=dateFrom]').getValue() == null) {
                            var dateFrom = self.dateFrom;
                            dateFrom.setHours(0, 0);
                            self.down('[name=dateFrom]').setValue(dateFrom);
                            store.getProxy().setExtraParam("dateFrom", self.dateFrom);
                        }
                        if (self.down('[name=dateTo]').getValue() == null) {
                            var dateTo = new Date(dateFrom);
                            dateTo.setHours(23, 59);
                            self.down('[name=dateTo]').setValue(dateTo);
                            store.getProxy().setExtraParam("dateTo", dateTo);
                        }
                    }
                }
            }),
             viewConfig: {
                listeners: {
                    refresh: function(dataview) {
                        Ext.each(dataview.panel.columns, function(column) {
                            if (column.autoSizeColumn === true)
                                column.autoSize();
                        });
                    },
                   highlightitem: function(dataview){
                        Ext.each(dataview.panel.columns, function(column) {
                            if (column.autoSizeColumn === true)
                                column.autoSize();
                        });
                    }
                }
            },
            execute: function () {
                console.log("ExecuteNow");
                var store = self.getStore();
                var __ret = self.down('toolbar').getTimeInterval();
                var from = __ret.from;
                var to = __ret.to;
                console.log('from', from, 'to', to);
                if (from <= to) {
                    store.getProxy().setExtraParam("uid", self.uid);
                    store.getProxy().setExtraParam("dateFrom", from);
                    store.getProxy().setExtraParam("dateTo", to);

                    store.load();
                }
                else {
                    Ext.MessageBox.show({
                        title: 'Ошибка',
                        msg: 'Укажите корректный диапазон дат',
                        icon: Ext.MessageBox.ERROR,
                        buttons: Ext.Msg.OK
                    });
                }
            },
            dockedItems: [
                {
                    xtype: 'toolbar',
                    getTimeInterval: function () {
                        var dateFrom = self.down('[name=dateFrom]').getValue();
                        var timeFrom = self.down('[name=timeFrom]').getValue();
                        var dateTo = self.down('[name=dateTo]').getValue();
                        var timeTo = self.down('[name=timeTo]').getValue();
                        if (dateFrom != null && timeFrom != null) {
                            var from = sumDateAndTime(dateFrom, timeFrom);
                            var to = sumDateAndTime(dateTo, timeTo);
                        }
                        return {from: from, to: to};
                    }, items: [
                        '->',
                        {
                            xtype: 'label',
                            html: '<b>Период:</b>'
                        },
                        {
                            xtype: 'tbspacer'
                        },
                        {
                            xtype: 'datefield',
                            width: 110,
                            fieldLabel: 'с',
                            labelWidth: 10,
                            //value:new Date(),
                            name: 'dateFrom',
                            format: 'd.m.Y',
                            maxValue: new Date(),
                            altFormats: 'c|m/d/Y|n/j/Y|n/j/y|m/j/y|n/d/y|m/j/Y|n/d/Y|m-d-y|m-d-Y|m/d|m-d|md|mdy|mdY|d|Y-m-d|n-j|n/j'
                        },
                        {
                            xtype: 'timefield',
                            width: 60,
                            itemId: 'timeFrom',
                            //labelWidth: 50,
                            anchor: '100%',
                            //fieldLabel: 'Время',
                            name: 'timeFrom',
                            format: 'H:i',
                            value: '00:00'
                        },
                        {
                            xtype: 'datefield',
                            width: 110,
                            fieldLabel: 'по',
                            labelWidth: 15,
                            //value:new Date(),
                            name: 'dateTo',
                            format: 'd.m.Y',
                            maxValue: new Date(),
                            altFormats: 'c|m/d/Y|n/j/Y|n/j/y|m/j/y|n/d/y|m/j/Y|n/d/Y|m-d-y|m-d-Y|m/d|m-d|md|mdy|mdY|d|Y-m-d|n-j|n/j'
                        },
                        {
                            xtype: 'timefield',
                            width: 60,
                            itemId: 'timeTo',
                            //labelWidth: 50,
                            anchor: '100%',
                            //fieldLabel: 'Время',
                            name: 'timeTo',
                            format: 'H:i',
                            value: '23:59'
                        },
                        {
                            xtype: 'button',
                            text: 'Загрузить',
                            icon: 'images/ico16_okcrc.png',
                            handler: function () {
                                self.execute()
                            }
                        },
                        {
                            icon: 'extjs-4.2.1/examples/restful/images/delete.gif',
                            text: 'Удалить',
                            disabled: self.hideRule,
                            itemId: 'delete',
                            scope: self,
                            handler: function () {
                                var store = self.getStore();
                                var selection = self.getSelectionModel().getSelection();

                                if (selection) {
                                    var store = this.store;
                                    Ext.MessageBox.confirm('Удаление элемента', 'Вы уверены, что хотите удалить ' + selection.length + ' Записей?', function (button) {
                                        if (button === 'yes') {
                                            var sd = Ext.Array.map(selection, function(r){return r.data;});
                                            console.log("sd=",sd);
                                            terminalMessagesService.remove(self.uid, sd, function(e){
                                                store.load();
                                            });
                                        }
                                    });
                                }


                            }
                        },
                    {
                        icon: 'extjs-4.2.1/examples/restful/images/delete.gif',
                        disabled: self.hideRule,
                        text: 'Удалить в диапазоне',
                        //disabled: true,
                        itemId: 'deleteInInterval',
                        scope: self,
                        handler: function () {
                            var store = self.getStore();
                            var selection = self.getSelectionModel().getSelection();

                            if (selection) {
                                var store = this.store;
                                Ext.MessageBox.confirm('Удаление элемента', 'Вы уверены, что хотите удалить все записи в выбранном диапазоне?', function (button) {
                                    if (button === 'yes') {
                                        var __ret = self.down('toolbar').getTimeInterval();
                                        terminalMessagesService.removeInInterval(self.uid, __ret.from, __ret.to, function(e){
                                            store.load();
                                        });
                                    }
                                });
                            }
                        }
                    },
                    {
                        icon: 'images/ico16_edit_def.png',
                        disabled: self.hideRule,
                        text: 'Пересчитать показатели',
                        //disabled: true,
                        itemId: 'reaggregateFromInterval',
                        scope: self,
                        handler: function () {
                            var store = self.getStore();
                            var selection = self.getSelectionModel().getSelection();

                            if (selection) {
                                var store = this.store;
                                Ext.MessageBox.confirm('Удаление элемента', 'Вы уверены, что хотите пересчитать аггрегированные параметры начиная с выбанного интервала?', function (button) {
                                    if (button === 'yes') {
                                        var __ret = self.down('toolbar').getTimeInterval();
                                        var wnd = self.up('window');
                                        wnd.setLoading("Идет перезапись данных");
                                        terminalMessagesService.reaggregate(self.uid, __ret.from, function(e){
                                            wnd.setLoading(false);
                                            store.load();
                                        });
                                    }
                                });
                            }
                        }
                    },
                    {
                        xtype: 'button',
                        //icon: 'extjs-4.2.1/examples/restful/images/add.gif',
                        text: 'Экспортировать',
                        disabled: self.hideRule,
                        defaultType: 'button',
                        menu: [
                            {
                                text: 'Mongo dump',
                                scope: this,
                                handler: function () {
                                    var uid = self.uid;
                                    var __ret = self.down('toolbar').getTimeInterval();
                                    window.open("EDS/dataexport/download?uid=" + uid + "&from=" + __ret.from.getTime() + "&to=" + __ret.to.getTime());
                                }
                            }
                        ]
                    },
                    {
                        xtype: 'button',
                        //icon: 'extjs-4.2.1/examples/restful/images/add.gif',
                        text: 'Импортировать',
                        disabled: self.hideRule,
                        handler: function () {
                            var wnd = Ext.create('Billing.view.TerminalMessagesUploadWnd');
                            var uid = self.uid;
                            wnd.down('form').down('[name=uid]').setValue(uid);
                            wnd.show();
                            }
                        }
                    ]
                }
            ]
        });
        this.callParent(arguments);
    },
    sortableColumns: false,
    columns: [
        {
            header: '№',
            xtype: 'rownumberer',
            width: 40,
            resizable: true
        },
//        {
//            header: '№',
//            dataIndex: 'num',
//            width: 60
//        },
        {
            header: 'Время',
            dataIndex: 'time',
            xtype: 'datecolumn',
            format: "d.m.Y H:i:s",
            width: 150
        },
        {
            header: 'Время получения',
            dataIndex: 'insertTime',
            xtype: 'datecolumn',
            format: "d.m.Y H:i:s",
            hidden: true,
            width: 150
        },
        {
            header: 'Координаты',
            dataIndex: 'coordinates',
            width: 150
        },
//        {
//            header: 'Долгота',
//            dataIndex: 'lon',
//            flex:1
//        },
//        {
//            header: 'Широта',
//            dataIndex: 'lat',
//            flex:1
//        },
        {
            header: 'Скорость',
            dataIndex: 'speed',
            width: 60
        },
        {
            header: 'Направление',
            dataIndex: 'course',
            //flex:1,
            width: 80
        },
//        {
//            header: 'Спутники',
//            dataIndex:'satelliteNum',
//            flex:1
//        },
        {
            header: 'Положение',
            dataIndex: 'regeo',
            minWidth: 200,
            maxWidth:300,
            flex:1,
            renderer: function (val, metaData, rec) {
                metaData.tdAttr = 'style="cursor: pointer !important;" title="'+val+'"';
                return val
            }
        },
        {
            header: 'Данные',
            dataIndex: 'devdata',
            minWidth: 100,
            autoSizeColumn: true,
            renderer: function (val, metaData, rec) {
                metaData.tdAttr = 'style="cursor: pointer !important;" title="'+val+'"';
                return val
            }
        }
    ],
    loadMask: true,

    plugins: {
        ptype: 'bufferedrenderer',
        trailingBufferZone: 40, // Keep 40 rows rendered in the table behind scroll
        leadingBufferZone: 80 // Keep 80 rows rendered in the table ahead of scroll
    },
    selModel: {
        mode: 'MULTI',
        pruneRemoved: false
    }
});

// Ext.define('Billing.view.TerminalMessagesPanel', {
//     extend: 'Ext.grid.Panel',
//     alias: 'widget.terminalpathgrid',

Ext.define('Billing.view.TerminalGaps', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.terminalgaps',
    initComponent: function () {

        var self = this;

        Ext.apply(this, {

            store: Ext.create('EDS.store.TerminalMessagesGaps', {
                autoLoad: true,
                autoSync: false,
                listeners: {
                    beforeload: function (store, op) {
                        console.log('grid beforeload');
                        //console.log('self rec',self.up('panel').getRecord().get("uid"))
                        console.log("Gaps grid = ");
                        console.log(self);
                        store.getProxy().setExtraParam("uid", self.uid);
                        var dateFrom = self.dateFrom;
                        console.log(dateFrom);
                        if (self.down('[name=dateFrom]').getValue() == null) {
                            dateFrom.setHours(0, 0);
                            self.down('[name=dateFrom]').setValue(dateFrom);
                            store.getProxy().setExtraParam("dateFrom",dateFrom);
                        }
                        if (self.down('[name=dateTo]').getValue() == null) {
                            var dateTo = new Date(dateFrom);
                            dateTo.setHours(23, 59);
                            self.down('[name=dateTo]').setValue(dateTo);
                            store.getProxy().setExtraParam("dateTo", dateTo);
                        }
                    }
                }
            }),
            viewConfig: {
                listeners: {
                    refresh: function(dataview) {
                        Ext.each(dataview.panel.columns, function(column) {
                            if (column.autoSizeColumn === true)
                                column.autoSize();
                        });
                    },
                    highlightitem: function(dataview){
                        Ext.each(dataview.panel.columns, function(column) {
                            if (column.autoSizeColumn === true)
                                column.autoSize();
                        });
                    }
                }
            },
            dockedItems: [
                {
                    xtype: 'toolbar',
                    getTimeInterval: function () {
                        var dateFrom = self.down('[name=dateFrom]').getValue();
                        var timeFrom = self.down('[name=timeFrom]').getValue();
                        var dateTo = self.down('[name=dateTo]').getValue();
                        var timeTo = self.down('[name=timeTo]').getValue();
                        if (dateFrom != null && timeFrom != null) {
                            var from = sumDateAndTime(dateFrom, timeFrom);
                            var to = sumDateAndTime(dateTo, timeTo);
                        }
                        return {from: from, to: to};
                    }, items: [
                    '->',
                    {
                        xtype: 'label',
                        html: '<b>Период:</b>'
                    },
                    {
                        xtype: 'tbspacer'
                    },
                    {
                        xtype: 'datefield',
                        width: 110,
                        fieldLabel: 'с',
                        labelWidth: 10,
                        //value:new Date(),
                        name: 'dateFrom',
                        format: 'd.m.Y',
                        maxValue: new Date(),
                        altFormats: 'c|m/d/Y|n/j/Y|n/j/y|m/j/y|n/d/y|m/j/Y|n/d/Y|m-d-y|m-d-Y|m/d|m-d|md|mdy|mdY|d|Y-m-d|n-j|n/j'
                    },
                    {
                        xtype: 'timefield',
                        width: 60,
                        itemId: 'timeFrom',
                        //labelWidth: 50,
                        anchor: '100%',
                        //fieldLabel: 'Время',
                        name: 'timeFrom',
                        format: 'H:i',
                        value: '00:00'
                    },
                    {
                        xtype: 'datefield',
                        width: 110,
                        fieldLabel: 'по',
                        labelWidth: 15,
                        //value:new Date(),
                        name: 'dateTo',
                        format: 'd.m.Y',
                        maxValue: new Date(),
                        altFormats: 'c|m/d/Y|n/j/Y|n/j/y|m/j/y|n/d/y|m/j/Y|n/d/Y|m-d-y|m-d-Y|m/d|m-d|md|mdy|mdY|d|Y-m-d|n-j|n/j'
                    },
                    {
                        xtype: 'timefield',
                        width: 60,
                        itemId: 'timeTo',
                        //labelWidth: 50,
                        anchor: '100%',
                        //fieldLabel: 'Время',
                        name: 'timeTo',
                        format: 'H:i',
                        value: '23:59'
                    },
                    {
                        xtype: 'button',
                        text: 'Загрузить',
                        icon: 'images/ico16_okcrc.png',
                        handler: function () {
                            var store = self.getStore();
                            var __ret = self.down('toolbar').getTimeInterval();
                            var from = __ret.from;
                            var to = __ret.to;
                            console.log('from', from, 'to', to);
                            if (from <= to) {
                                store.getProxy().setExtraParam("uid", self.uid);
                                store.getProxy().setExtraParam("dateFrom", from);
                                store.getProxy().setExtraParam("dateTo", to);

                                store.load();
                            }
                            else {
                                Ext.MessageBox.show({
                                    title: 'Ошибка',
                                    msg: 'Укажите корректный диапазон дат',
                                    icon: Ext.MessageBox.ERROR,
                                    buttons: Ext.Msg.OK
                                });
                            }
                        }
                    }
                ]
                }
            ]
        });

        this.callParent(arguments);
    },

    listeners: {
        celldblclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
            var self=this;
            var tabPanel = self.up('tabpanel');
            if(tabPanel) {
                var messagesGrid = tabPanel.down('terminalpathgrid');
                console.log("MessagesGrid=");
                console.log(messagesGrid);
                if(messagesGrid) {

                    var __ret = self.down('toolbar').getTimeInterval();
                    var from = self.getStore().getProxy().extraParams.dateFrom;
                    var to = self.getStore().getProxy().extraParams.dateTo;
                    console.log("dateFrom is");
                    console.log(from);
                    var mesDates = messagesGrid.down('toolbar').getTimeInterval();
                    tabPanel.setActiveTab('pathGrid');
                    var rowNumber = record.get('_firstRow');

                    if(mesDates.from.getTime() != from.getTime() || mesDates.to.getTime() != to.getTime()) {

                        messagesGrid.down('[name=dateFrom]').setValue(from);
                        messagesGrid.down('[name=timeFrom]').setValue(from);
                        messagesGrid.down('[name=dateTo]').setValue(to);
                        messagesGrid.down('[name=timeTo]').setValue(to);

                        messagesGrid.getView().on('refresh', function (grid) {
                            messagesGrid.getView().bufferedRenderer.scrollTo(rowNumber, false);
                        }, messagesGrid, {single: true});
                        messagesGrid.execute();
                    }

                    else {
                        messagesGrid.getView().bufferedRenderer.scrollTo(rowNumber, false);
                    }

                }
            }
        }
    },

    columns: [
        {
            header: '№',
            xtype: 'rownumberer',
            width: 60,
            resizable: true
        },
        {
            header: 'Начало',
            dataIndex: 'firstTime',
            xtype: 'datecolumn',

            format: "d.m.Y H:i:s",
            width: 150
        },
        {
            header: 'Конец',
            dataIndex: 'secondTime',
            xtype: 'datecolumn',

            format: "d.m.Y H:i:s",
            width: 150
        },
        {
            header: 'Длительность',
            dataIndex: 'period',
            width: 100
        },
        {
            header: 'Координаты  начала',
            dataIndex: 'firstCoordinates',
            width: 200
        },
        {
            header: 'Координаты конца',
            dataIndex: 'secondCoordinates',
            width: 200
        },
        {
            header: 'Расстояние',
            dataIndex: 'distance',
            width: 100,
            renderer: function (val) { return Ext.util.Format.number(val, '0.00') + ' км';
            }
        }
    ]
});


Ext.define('Billing.view.TerminalMessages', {
    extend: 'Ext.window.Window',
    title: 'Сообщения от объекта',
    width: 1024,
    height: 768,
    maximizable: true,
    layout: 'fit',
    initComponent: function () {
        var self = this;
        Ext.apply(this, {
            items: [
                {
                    xtype: 'tabpanel',
                    items: [
                        {
                            xtype: 'terminalpathgrid',
                            uid: self.uid,
                            dateFrom: self.dateFrom,
                            hideRule: self.hideRule,
                            title: 'Сообщения от объекта',
                            itemId: 'pathGrid'
                        },
                        {
                            xtype: 'terminalgaps',
                            uid: self.uid,
                            dateFrom: self.dateFrom,
                            hideRule: self.hideRule,
                            title: 'Разрывы связи',
                            itemId: 'gapsGrid'
                        }
                    ]
                }

            ]
        });
        this.callParent();
    }
});

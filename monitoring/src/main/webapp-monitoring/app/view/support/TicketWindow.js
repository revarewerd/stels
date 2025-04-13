/**
 * Created by IVAN on 27.10.2015.
 */
Ext.define('Seniel.view.support.TicketWindow', {
    extend: 'Seniel.view.WRWindow',
    alias: 'widget.ticketwnd',
    //stateId: 'geozGrid',
    //stateful: true,
    title: tr('support.ticket'),
    icon: 'images/ico16_eventsmsgs.png',
    btnConfig: {
        icon: 'images/ico16_eventsmsgs.png',
        text: tr('support.ticket')
    },
    minWidth: 400,
    minHeight: 320,
    width: 800,
    height: 600,
    layout: 'border',
    items: [
        {
            region: 'north',
            xtype: 'ticketpanel',
            height: 250
        },
        {
            region: 'center',
            xtype: 'ticketgrid'
        }
    ],
    dockedItems: [
        {
            xtype: 'toolbar',
            dock: 'bottom',
            ui: 'footer',
            items: [
                {
                    itemId: 'readStatus',
                    text: tr("support.ticket.readStatus.markAs.read"),
                    readStatus: false,
                    handler: function (button) {
                        console.log("Mark as");
                        var ticketWindow = this.up('window');
                        userSupportRequestEDS.changeTicketReadStatus(ticketWindow.ticketId, !button.readStatus, function (readStatus) {
                            if (readStatus) {
                                button.readStatus = true;
                                button.setText(tr("support.ticket.readStatus.markAs.unread"))
                            }
                            else {
                                button.readStatus = false;
                                button.setText(tr("support.ticket.readStatus.markAs.read"))
                            }
                        })
                    }
                },
                '->',
                {
                    icon: 'images/ico16_okcrc.png',
                    //disabled:true,
                    itemId: 'reply',
                    text: tr('support.ticket.reply'),
                    handler: function () {
                        console.log("Add reply");
                        var ticketWindow = this.up('window');
                        //var rec=ticketWindow.down("#ticketpanel").getRecord()
                        this.up('window').showReplyWindow(ticketWindow.ticketId)
                    }
                },
                {
                    icon: 'images/ico16_lock.png',
                    //disabled:true,
                    itemId: 'closeTicket',
                    text: tr('support.ticket.closeTicket'),
                    handler: function () {
                        console.log("Close ticket");
                        var ticketWindow = this.up('window');
                        //var rec=ticketWindow.down("#ticketpanel").getRecord()
                        this.up('window').showCloseTicketWindow(ticketWindow.ticketId)
                    }
                },
                {
                    icon: 'images/ico16_cancel.png',
                    text: tr('support.ticket.cancel'),
                    handler: function () {
                        this.up('window').close();
                    }
                }]
        }
    ],
    onLoad: function (ticketId) {
        var self = this;
        //var ticketId=record.get('_id');
        console.log("ticketId", ticketId);
        userSupportRequestEDS.loadOne(ticketId, function (res, e) {
            if (e.type === "exception") {
                console.log("exception=", e);
                self.down("#closeTicket").setDisabled(true);
                self.down("#reply").setDisabled(true);
                Ext.MessageBox.show({
                    title: 'Произошла ошибка',
                    msg: e.message,
                    icon: Ext.MessageBox.ERROR,
                    buttons: Ext.Msg.OK
                });
            }
            else {
                console.log("data", res);
                var readStatus = res.userRead;
                console.log("readStatus", readStatus);
                if (readStatus == true) {
                    self.down("#readStatus").setText(tr("support.ticket.readStatus.markAs.unread"));
                    self.down("#readStatus").readStatus = true
                }
                else {
                    self.down("#readStatus").setText(tr("support.ticket.readStatus.markAs.read"));
                    self.down("#readStatus").readStatus = false
                }
                if (res.status == "close") {
                    self.down("#closeTicket").setDisabled(true);
                    self.down("#reply").setDisabled(true)
                }
                var rec = Ext.create('Ticket', res);
                self.down("#ticketpanel").loadRecord(rec);
                self.down("#ticketgrid").loadData(rec.get("dialog"))
            }
        })

    },
    showReplyWindow: function (ticketId, wndTitle, callbackFun) {
        var self = this;
        var title = tr('support.ticket.comments');
        if (wndTitle != undefined) title = wndTitle;
        Ext.Msg.show({
            title: title,
            msg: tr('support.ticket.addComment'),
            width: 600,
            buttons: Ext.Msg.OKCANCEL,
            multiline: true,
            fn: function (buttonId, text, opts) {
                if (buttonId == "ok") {
                    console.log("reply", text);
                    var defaultTimezone = Timezone.getDefaultTimezone();
                    var dateTime = new Date();
                    if (defaultTimezone) {
                        dateTime = moment.tz(Ext.Date.format(dateTime, "d.m.Y H:i:s:u"), "DD.MM.YYYY HH:mm:ss:SSS", defaultTimezone.name).toDate()
                    }
                    var data = {
                        ticketId: ticketId,
                        dateTime: dateTime.getTime(),
                        text: text
                    };
                    userSupportRequestEDS.updateTicketDialog(data,
                        function (res, e) {
                            if (e.type === "exception") {
                                console.log("exception=", e);
                                Ext.MessageBox.show({
                                    title: 'Произошла ошибка',
                                    msg: e.message,
                                    icon: Ext.MessageBox.ERROR,
                                    buttons: Ext.Msg.OK
                                });
                            }
                            else {
                                if (callbackFun != undefined) callbackFun();
                                self.down("#ticketgrid").refresh();
                            }
                        })
                }
            },
            animateTarget: 'addAddressBtn',
            icon: Ext.window.MessageBox.INFO
        });
    },
    showCloseTicketWindow: function (ticketId) {
        var self = this;
        var closeTicketFun = function () {
            userSupportRequestEDS.updateTicketStatus(ticketId, 'close', function (res) {
                self.down("#ticketgrid").refresh()
            })
        };
        this.showReplyWindow(ticketId, tr('support.ticket.closeTicket'), closeTicketFun)

    }
});
Ext.define('Seniel.view.support.TicketPanel', {
    extend: 'Ext.form.Panel',
    alias: 'widget.ticketpanel',
    //stateId: 'geozGrid',
    //stateful: true,
    //title: "Новое обращение",//tr('editgeozoneswnd.geozones'),
    //minWidth: 400,
    //minHeight: 320,
    //layout: 'border',
    itemId: "ticketpanel",
    fieldDefaults: {
        margin: '0 20 5 20'//,
        //vtype:'alphanum',
    },
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    items: [
        {
            margin: '20 20 5 20',
            xtype: 'combobox',
            itemId: 'category',
            name: 'category',
            fieldLabel: tr('support.request.category'),
            valueField: 'value',
            displayField: 'name',
            store: Ext.create('Ext.data.Store', {
                fields: ['value', 'name'],
                data: [
                    {"value": "equipment", "name": tr('support.request.category.equipment')},
                    {"value": "program", "name": tr('support.request.category.program')},
                    {"value": "finance", "name": tr('support.request.category.finance')}
                ]
            }),
            readOnly: true
        },
        {
            //xtype: 'combobox',
            xtype: 'textfield',
            itemId: 'question',
            name: 'question',
            fieldLabel: tr('support.request.question'),
            readOnly: true
        },
        {
            margin: '0 20 20 20',
            xtype: 'textareafield',
            itemId: 'description',
            name: 'description',
            fieldLabel: tr('support.ticket.description'),
            rows: 10,
            readOnly: true
        }
    ]
});
Ext.define('Seniel.view.reports.TicketGrid', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.ticketgrid',
    itemId: "ticketgrid",
    //stateId: 'repGrPGrid',
    //stateful: true,
    //title:tr('main.reports.intervals'),
    loadMask: true,
    loadBeforeActivated: false,
    manualLoad: true,
    dockedToolbar: ['refresh'],
    initComponent: function () {
        var self = this;
        Ext.apply(this, {
            store: Ext.create('Ext.data.Store', {
                storeId: 'ticketsStore',
                fields: ['_id', 'userName', 'dateTime', 'text'],
                proxy: {
                    type: 'memory',
                    reader: {
                        type: 'json',
                        root: 'items'
                    }
                }
            })
        });
        this.callParent();
        this.down('toolbar').insert(0, {
            icon: 'images/ico16_zoomout.png',
            //text: 'Свернуть все',
            scope: self,
            handler: self.collapseAllRows
        });
        this.down('toolbar').insert(1, {
            icon: 'images/ico16_zoomin.png',
            //text: 'Развернуть все',
            scope: self,
            handler: self.expandAllRows
        });
    },
    setStore: false,
    plugins: [
        {
            ptype: 'rowexpander',
            pluginId: 'rowExpander',
            rowBodyTpl: new Ext.XTemplate('<div class="nested-textarea"></div>')
        }
    ],
    viewConfig: {
        listeners: {
            expandbody: function (rowNode, rec, row) {
                var self = this;
                if (!rec.nestedGrid) {
                    rec.nestedGrid =
                        Ext.create('Ext.form.field.TextArea', {
                            width: "100%",
                            height: 100,
                            grow: true,
                            name: 'message',
                            //fieldLabel: 'Message',
                            //labelWidth:50,
                            parentGridView: self
                        });
                    rec.nestedGrid.setValue(rec.get('text'));
                    rec.nestedGrid.render(row.childNodes[0].childNodes[0].childNodes[0]);
                    rec.nestedGrid.getEl().swallowEvent(['mouseover', 'mouseout', 'mousedown', 'mouseup', 'click', 'dblclick', 'mousemove']);
                }
            }
        }
    },
    columns: [
        {
            stateId: 'userName',
            header: tr("support.ticket.user"),
            dataIndex: 'userName',
            width: 120
        },
        {
            stateId: 'dateTime',
            header: tr("support.ticket.date"),
            dataIndex: 'dateTime',
            width: 120,
            renderer: function (val) {
                var d = new Date(val);
                return Ext.Date.format(d, tr('format.extjs.datetime'));
            }
        },
        {
            stateId: 'text',
            header: tr("support.ticket.message"),
            dataIndex: 'text',
            flex: 1
        }
    ],
    refresh: function () {
        var self = this;
        var ticketWnd = self.up('window');
        var ticketId = ticketWnd.ticketId;
        console.log("ticketId", ticketId);
        var store = self.getStore();

        self.collapseAllRows();
        self.removeAllNestedStories();
        store.each(function (rec) {
            if (rec.nestedGrid) {
                delete rec.nestedGrid;
            }
        });
        this.getSelectionModel().deselectAll();
        ticketWnd.onLoad(ticketId)
    },
    collapseAllRows: function () {
        var self = this;
        var expander = self.getPlugin('rowExpander');
        console.log("expander", expander);
        var store = self.getStore();
        for (var i = 0; i < store.getCount(); i++) {
            var record = store.getAt(i);
            if (expander.recordsExpanded[record.internalId]) {
                expander.toggleRow(i, record);
            }
        }
    },
    expandAllRows: function () {
        var self = this;
        var expander = self.getPlugin('rowExpander');
        var store = self.getStore();
        for (var i = 0; i < store.getCount(); i++) {
            var record = store.getAt(i);
            if (!expander.recordsExpanded[record.internalId]) {
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
    removeSelection: function () {
        this.getSelectionModel().deselectAll()
    },
    loadData: function (data) {
        console.log("load data", data);
        this.getStore().loadData(data)
    }
    //listeners: {
    //    //select: function(view, rec) {
    //    //    var mapWidget = this.up('groupreportwnd').down('seniel-mapwidget');
    //    //    var groupreportwnd = this.up('groupreportwnd');
    //    //    var dates = groupreportwnd.getWorkingDates();
    //    //    mapWidget.drawMovingPath(rec.get("uid"), dates.from, dates.to);
    //    //},
    //    sortchange: function(ct, column, direction) {
    //        var grid = ct.up('grid');
    //        grid.collapseAllRows();
    //        grid.removeAllNestedStories();
    //    }
    //}
});

Ext.define('Ticket', {
    extend: 'Ext.data.Model',
    fields: [
        "_id", "question", "description", "dateTime", "userId", "category", "status", "userName", "userPhone", "dialog"
    ],
    idProperty: '_id'
});
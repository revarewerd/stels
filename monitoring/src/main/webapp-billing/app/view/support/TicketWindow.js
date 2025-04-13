/**
 * Created by IVAN on 27.10.2015.
 */
Ext.define('Billing.view.support.TicketWindow', {
    extend: 'Ext.window.Window',
    alias: 'ticketwnd',
    //stateId: 'geozGrid',
    //stateful: true,
    title: "Заявка",//tr('editgeozoneswnd.geozones'),
    icon: 'images/ico16_eventsmsgs.png',
    maximizable:true,
    minWidth: 400,
    minHeight: 320,
    width: 800,
    height: 600,
    layout: 'border',
    items:[
        {
            region:'north',
            xtype:'ticketpanel',
            height:250
        },
        {
            region:'center',
            xtype:'ticketgrid'
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
                    text: "Пометить как прочитанную",
                    readStatus: false,
                    handler: function(button){
                        console.log("Mark as");
                        var ticketWindow=this.up('window');
                        supportRequestEDS.changeTicketReadStatus(ticketWindow.ticketId,!button.readStatus,function(readStatus){
                            if(readStatus){
                                button.readStatus=true;
                                button.setText("Пометить как непрочитанную")
                            }
                            else {
                                button.readStatus=false;
                                button.setText("Пометить как прочитанную")
                            }
                        })
                    }
                },
                '->',
                {
                    icon: 'images/ico16_okcrc.png',
                    itemId: 'reply',
                    text: 'Добавить ответ',
                    handler: function(){
                        console.log("Add reply");
                        var ticketWindow=this.up('window');
                        this.up('window').showReplyWindow(ticketWindow.ticketId)
                    }
                },
                {
                    icon: 'images/ico16_lock.png',
                    itemId: 'closeTicket',
                    text: 'Закрыть заявку',
                    status:'open',
                    handler: function(btn){
                        var ticketWindow = btn.up('window');
                        if(btn.status=="close")
                            supportRequestEDS.updateTicketStatus(ticketWindow.ticketId,"open",function(res){
                                ticketWindow.down("#ticketgrid").refresh()
                            });
                        else
                            ticketWindow.showCloseTicketWindow(ticketWindow.ticketId);
                    }
                },
                {
                    icon: 'images/ico16_cancel.png',
                    text: 'Отменить',
                    handler: function(){
                        this.up('window').close();}
                }]
        }
    ],
    onLoad:function(ticketId){
        var self=this;
        console.log("ticketId",ticketId);
        supportRequestEDS.loadOne(ticketId,function(res,e){
                if(e.type === "exception") {
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
                    var readStatusBtn=self.down("#readStatus");
                    var closeTicketBtn=self.down("#closeTicket");
                    if(res.supportRead==true) {
                        readStatusBtn.setText("Пометить как непрочитанную");
                        readStatusBtn.readStatus=true;
                    }
                    if (res.status == "close") {
                        self.down("#reply").setDisabled(true);
                        closeTicketBtn.status="close";
                        closeTicketBtn.setText("Открыть повторно"); //.setDisabled(true);
                        closeTicketBtn.setIcon('images/ico16_unlock.png');
                    }
                    else {
                        self.down("#reply").setDisabled(false);
                        closeTicketBtn.status="open";
                        closeTicketBtn.setText("Закрыть заявку"); //.setDisabled(true);
                        closeTicketBtn.setIcon('images/ico16_lock.png');
                    }
                    var rec = Ext.create('Ticket', res);
                    self.down("#ticketpanel").loadRecord(rec);
                    self.down("#ticketgrid").loadData(rec.get("dialog"))
                }
        })

    },
    showReplyWindow:function(ticketId,wndTitle,callbackFun){
        var self=this;
        var title='Комментарии к заявке';
        if(wndTitle!=undefined) title=wndTitle;
        Ext.Msg.show({
            title: title,
            msg: 'Введите ваш комментарий:',
            width: 600,
            buttons: Ext.Msg.OKCANCEL,
            multiline: true,
            animateTarget: 'addAddressBtn',
            icon: Ext.window.MessageBox.INFO,
            fn: function(buttonId,text,opts){
                if(buttonId=="ok") {
                    console.log("reply", text);
                    var dateTime = new Date();
                    var data = {
                        ticketId: ticketId,
                        dateTime: dateTime.getTime(),
                        text: text
                    };
                    supportRequestEDS.updateTicketDialog(data,
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
                                if(callbackFun!=undefined) callbackFun();
                                else self.down("#ticketgrid").refresh();
                            }
                        }
                    )
                }
            }
        });
    },
    showCloseTicketWindow:function(ticketId){
        var self=this;
        var closeTicketFun=function(){
            supportRequestEDS.updateTicketStatus(ticketId,'close', function(res){
                self.down("#ticketgrid").refresh()
            })
        };
        this.showReplyWindow(ticketId,'Закрыть заявку',closeTicketFun)

    }
});
Ext.define('Seniel.view.support.TicketPanel', {
    extend: 'Ext.form.Panel',
    alias: 'widget.ticketpanel',
    itemId:"ticketpanel",
    fieldDefaults:{
        margin: '0 20 5 20'//,
        //vtype:'alphanum',
    },
    layout: {
        type:'vbox',
        align:'stretch'
    },
    items:[
        {
            margin: '20 20 5 20',
            xtype: 'combobox',
            itemId:'category',
            name:'category',
            fieldLabel:'Категория',
            valueField: 'value',
            displayField:'name',
            store: Ext.create('Ext.data.Store', {
                fields: ['value', 'name'],
                data : [
                    {"value":"equipment", "name":'Вопросы по оборудованию'},
                    {"value":"program", "name":'Вопросы по программе'},
                    {"value":"finance", "name":'Финансовые вопросы'}
                ]
            }),
            readOnly:true
        },
        {
            //xtype: 'combobox',
            xtype:'textfield',
            itemId:'question',
            name:'question',
            fieldLabel:"Вопрос",
            readOnly:true
        },
        {
            margin: '0 20 20 20',
            xtype: 'textareafield',
            itemId:'description',
            name:'description',
            fieldLabel: 'Опишите вашу проблему',
            rows:10,
            readOnly:true
        }
    ]
});
Ext.define('Seniel.view.reports.TicketGrid', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.ticketgrid',
    itemId:"ticketgrid",
    //stateId: 'repGrPGrid',
    //stateful: true,
    //title:tr('main.reports.intervals'),
    loadMask: true,
    loadBeforeActivated: false,
    manualLoad: true,
    dockedToolbar: ['refresh'],
    initComponent: function () {
        var self=this;
        Ext.apply(this,{
            store: Ext.create('Ext.data.Store', {
                storeId: 'ticketsStore'+self.ticketId,
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
    columns: [
        {
            stateId: 'userName',
            header: "Пользователь",//tr("basereport.objectfield"),
            dataIndex: 'userName',
            width: 120
        },
        {
            stateId: 'dateTime',
            header: "Дата",//tr("statistics.intervalstart"),
            dataIndex: 'dateTime',
            xtype:'datecolumn',
            format: "d.m.Y H:i:s",
            width: 120
        },
        {
            stateId: 'text',
            header: "Cообщение",//tr("statistics.intervalstart"),
            dataIndex: 'text',
            flex:1
        }
    ],
    setStore:false,
    plugins: [
        {
            ptype: 'rowexpander',
            pluginId: 'rowExpander',
            rowBodyTpl : new Ext.XTemplate('<div class="nested-textarea"></div>')
        }
    ],
    viewConfig: {
        listeners: {
            expandbody: function(rowNode, rec, row) {
                var    self = this;
                if (!rec.nestedGrid) {
                        rec.nestedGrid =
                            Ext.create('Ext.form.field.TextArea', {
                            width:"100%",
                            height:100,
                            grow      : true,
                            name      : 'message',
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
    refresh:function(){
        var self = this;
        var ticketWnd=self.up('window');
        var ticketId=ticketWnd.ticketId;
        var store = self.getStore();

        self.collapseAllRows();
        self.removeAllNestedStories();
        store.each(function(rec) {
            if (rec.nestedGrid) {
                delete rec.nestedGrid;
            }
        });
        this.getSelectionModel().deselectAll();
        ticketWnd.onLoad(ticketId)
    },
    collapseAllRows: function() {
        var self = this;
        var expander = self.getPlugin('rowExpander');
        console.log("expander",expander);
        var store=self.getStore();
        for(var i = 0; i < store.getCount(); i++) {
            var record = store.getAt(i);
            if(expander.recordsExpanded[record.internalId]){
                expander.toggleRow(i,record);
            }
        }
    },
    expandAllRows: function() {
        var self = this;
        var expander = self.getPlugin('rowExpander');
        var store=self.getStore();
        for(var i = 0; i < store.getCount(); i++) {
            var record = store.getAt(i);
            if(!expander.recordsExpanded[record.internalId]){
                expander.toggleRow(i,record);
            }
        }
    },
    removeAllNestedStories: function() {
        var self = this;
        var store=self.getStore();
        for(var i = 0; i < store.getCount(); i++) {
            var record = store.getAt(i);
            if (record && record.nestedGrid) {
                delete record.nestedGrid;
            }
        }
    },
    removeSelection:function(){
        this.getSelectionModel().deselectAll()
    },
    loadData:function(data){
        console.log("load data",data)
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
        "_id","question", "description","dateTime", "userId","category","status","userName","userPhone","dialog"
    ],
    idProperty: '_id'
});
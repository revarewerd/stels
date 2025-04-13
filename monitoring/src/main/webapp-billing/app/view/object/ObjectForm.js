Ext.define('Billing.view.object.ObjectForm', {
    extend: 'Ext.form.Panel',
    alias: 'widget.objform',
    requires:[
        'Billing.view.equipment.EquipmentStoreWindow',
        //'Billing.view.object.ObjectEventsPanel',
        'Billing.view.event.EventsPanel'
    ],
    initComponent: function () {
        this.addEvents('create');
        var self = this;
        Ext.apply(this,
            {
            itemId:'objectform',
            activeRecord: null,
            autoScroll: true,
            resizeable: true,
            title: '<font color="1a4780"><b>Параметры объекта</b></font>',
            layout:'column',
            defaults:
                {columnWidth: .60,
                    border:false,
                    margin: '10 10 10 10',
                    xtype:'form',
                    defaultType: 'textfield',
                    fieldDefaults: {
                        anchor: '100%',
                        labelAlign: 'right',
                        labelWidth:150
                    }
                },
            items: [
                    {items: [
                        {
                            fieldLabel: 'Наименование',
                            name: 'name',
                            allowBlank: false,
                            regex:/^[\S]+.*[\S]+.*[\S]+$/i,
                            readOnly:this.hideRule
                        },
                        {
                            fieldLabel: 'Пользователськое имя',
                            name: 'customName',
                            regex:/^[\S]+.*[\S]+.*[\S]+$/i,
                            readOnly:this.hideRule
                        },
                        {
                            fieldLabel: 'Комментарий',
                            name: 'comment',
                            hidden:this.hideRule
                            //allowBlank: false
                        },
                        {
                            xtype: 'hidden',
                            fieldLabel: 'Объект UID ',
                            name: 'uid',
                            allowBlank: true,
                            emptyText: "Будет сгенерирован",
                            readOnly: true
                        },
                        {
                            xtype: 'combobox',
                            fieldLabel: 'Учетная запись',
                            name: 'accountId',
                            store: Ext.create('EDS.store.AccountsDataShort', {
                                autoLoad: true
                            }),
                            valueField: '_id',
                            displayField: 'name',
//                            typeAhead: true,
                            forceSelection: true,
                            minChars: 0,
                            allowBlank: false,
                            readOnly:this.hideRule
                        },
                        { fieldLabel: 'Абонентская плата',
                            xtype:'displayfield',
                            width:50,
                            name:'cost',
                            readOnly:this.hideRule,
                            renderer: function (value) {
                                return accounting.formatMoney(parseFloat(value / 100));
                        }
                        },
                        {
                            xtype: 'checkbox',
                            labelWidth: 150,
                            fieldLabel: 'Отключен',
                            name:'disabled',
                            readOnly:this.hideRule
                        },
                        {  xtype: 'fieldcontainer',
                            layout: 'hbox',
                            items: [
                                {
                                    xtype: 'displayfield',
                                    labelWidth: 150,
                                    fieldLabel: 'Кол-во пользователей',
                                    name: 'userscount',
                                    value: 'Недоступно'
                                },
                                {xtype: 'button',
                                    margin: '0 0 0 40',
                                    text: 'Показать',
                                    handler: function () {
                                        console.log('click this=', this.up('form[itemId=objectform]'));
                                        this.up('form[itemId=objectform]').showUsersPanel();
                                    }
                                }
                            ]
                        },
                        {  xtype: 'fieldcontainer',
                            layout: 'hbox',
                            items: [
                                {
                                    xtype: 'checkbox',
                                    labelWidth: 150,
                                    fieldLabel: 'Блокировка бензонасоса',
                                    name: 'fuelPumpLock',
                                    readOnly: this.hideRule
                                },
                                {
                                    xtype: 'checkbox',
                                    margin: '0 0 0 20',
                                    labelWidth: 150,
                                    fieldLabel: 'Блокировка зажигания',
                                    name: 'ignitionLock',
                                    readOnly: this.hideRule
                                }
                            ]
                        }
                    ]},
                {   columnWidth: .40,
                    fieldDefaults: {
                        anchor: '100%',
                        labelAlign: 'right',
                        labelWidth: 80
                    },
                    items: [
                        {
                            fieldLabel: 'Тип',
                            name: 'type',
                            readOnly: this.hideRule
                        },
                        {
                            fieldLabel: 'Марка',
                            name: 'marka',
                            readOnly: this.hideRule
                        },
                        {
                            fieldLabel: 'Модель',
                            name: 'model',
                            readOnly: this.hideRule
                        },
                        {
                            fieldLabel: 'Госномер',
                            name: 'gosnumber',
                            readOnly: this.hideRule
                        },
                        {
                            fieldLabel: 'VIN',
                            name: 'VIN',
                            readOnly: this.hideRule
                        }
                    ]
                },
                {   margin: '0 10 0 10',
                    columnWidth: 1,
                    labelAlign: 'right',
                    labelWidth:135 ,
                    xtype: 'textareafield',
                    rows: 2,
                    fieldLabel: 'Примечание',
                    name: 'objnote',
                    hidden:this.hideRule
                },
                {
                    xtype: 'fieldset',
                    columnWidth: 1,
                    title: '<font color="1a4780"><b>Состояние объекта</b></font>',
                    collapsible: true,
                    hidden: this.hideRule,
                    defaultType: 'displayfield',
                    defaults: {
                        margin: '10 10 0 10',
                        border: 1,
                        style: {
                            borderColor: 'gray',
                            borderStyle: 'solid'
                        },
                        labelAlign: 'left',
                        labelWidth:150,
                        labelStyle: 'padding: 0 0 2px 4px;',
                        anchor: '100%'},
                    layout: 'column',
                    items: [
                        {
                            fieldLabel: 'Последнее сообщение',
                            columnWidth:.40,
                            name: 'latestmsg',
                            hidden: this.hideRule,
                            renderer: function (val) {
                                console.log("val",val)
                                var lmd = new Date(new Number(val)),
                                    lms = Ext.Date.format(lmd, "d.m.Y H:i:s"),
                                    res
                                console.log("lmd",lmd)
                                if (!val)
                                    res = "Сообщений не поступало";
                                else {
                                    var res = lms
                                }
                                return res;
                            }
                        },
                        {
                            columnWidth:.60,
                            fieldLabel: 'Местоположение',
                            name: 'placeName',
                            hidden: this.hideRule
                        },
                        {
                            fieldLabel: 'Зажигание',
                            columnWidth: .40,
                            name: 'ignition',
                            hidden: this.hideRule,
                            renderer: function (val) {
                                if (val != 'unknown' && val > 0) {
                                    return "Включено";
                                } else {
                                    return '';
                                }
                            }
                        },
                        {
                            fieldLabel: 'Скорость',
                            columnWidth: .30,
                            name: 'speed',
                            hidden: this.hideRule
                        },
                        {
                            fieldLabel: 'Кол-во спутников',
                            columnWidth: .30,
                            name: 'satelliteNum',
                            hidden: this.hideRule
                        }
                    ]
                }
            ]
        });
        this.callParent();
    },
    loadUsers: function (_id, comp) {
        if (!_id)
            return;
        usersPermissionsService.getPermittedUsersCount(_id, "object", function (result, e) {
            comp.setValue(result);
        });
    },
    showUsersPanel: function (){
        var self=this;
        var id=this.up('form').getRecord().get("_id");
        if (id) {
            var v = WRExtUtils.createOrFocus('UsrWnd' + id, 'Billing.view.user.UsersWindow', {
                oid: id,
                type:'object',
                title: "Пользователи объекта \"" + this.up('form').getRecord().get('name') + "\""
            });
            v.on('close',function(){
                self.loadUsers(id,self.down('[name=userscount]'));
            });
            v.show(this);
        }
        else {
            Ext.MessageBox.show({
                title: 'Объект еще не создан',
                msg: 'Невозможно просмотреть список пользователей владеющих несуществующим объектом',
                buttons: Ext.MessageBox.OK,
                icon: Ext.MessageBox.WARNING
            });
        }
    },
    setActiveRecord: function (record) {
        this.activeRecord = record;
        this.getForm().loadRecord(record);
    }
});
Ext.define('Billing.view.object.ObjectForm.EquipmentPanel', {
    extend: 'Ext.form.Panel',
    alias: 'widget.eqpanel',
    requires: ['Ext.form.field.Text',
               'Billing.view.equipment.EquipmentForm'],
    initComponent: function () {
        var self=this;
        this.addEvents('create');
        Ext.apply(this, {
            activeRecord: null,
            //autoScroll: true,
            resizeable: true,
            title: '<font color="1a4780"><b>Оборудование</b></font>',
            defaultType: 'textfield',
            fieldDefaults: {
                margin: '0 20 5 20',
                anchor: '100%',
                labelAlign: 'right',
                labelWidth: 120
            },
            collapsed: false,
            layout: 'fit',
            dockedItems: [
            {
                xtype: 'toolbar',
                dock: 'right',
                align: 'left',
                items: [
//                {
//                    xtype: 'button',
//                    icon: '/extjs-4.2.1/examples/restful/images/add.gif',
//                    tooltip: 'Добавить устройство',
//                    handler: function () {
//                        console.log("add eq")
//                    self.addEquipmentForm()
//                    }
//                },
                {
                    xtype: 'button',
                    icon: 'extjs-4.2.1/examples/shared/icons/fam/user_suit.gif',
                    tooltip: 'Добавить со склада учетной записи',
                    disabled:this.hideRule,
                    handler: function () {
                    self.up('panel').updateRecord();
                    var accountId=self.up('panel').getRecord().get("accountId");
                    if(accountId!=null && accountId!="")
                    self.showAccStoreWnd();
                    else Ext.MessageBox.show({
                                title: 'Произошла ошибка',
                                msg: "Не указана учетная запись",
                                icon: Ext.MessageBox.ERROR,
                                buttons: Ext.Msg.OK
                         });
                    }
                },
                {
                    xtype: 'button',
                    icon: 'images/home.png',
                    tooltip: 'Добавить с главного склада',
                    disabled:this.hideRule,
                    handler: function () {
                    self.up('panel').updateRecord();
                        var accountId=self.up('panel').getRecord().get("accountId");
                        if(accountId!=null && accountId!="")
                            self.showEqAddWnd();
                        else Ext.MessageBox.show({
                            title: 'Произошла ошибка',
                            msg: "Не указана учетная запись",
                            icon: Ext.MessageBox.ERROR,
                            buttons: Ext.Msg.OK
                        });
                    }
                },
                {
                xtype: 'button',
                tooltip: 'Удалить устройство',
                disabled:this.hideRule,
                icon: 'extjs-4.2.1/examples/restful/images/delete.gif',
                handler: function () {
                    console.log("remove eq");
                    self.removeEquipment();
                    }
                },
                {
                        xtype: 'button',
                        tooltip: 'Обновить',
                        icon: 'extjs-4.2.1/examples/page-analyzer/resources/images/refresh.gif',
                        handler: function () {
                            console.log("refresh");
                            var accountId=self.accountId;
                            EquipmentStoreUtils.storeRefresh(accountId)
                        }
                }
            ]
        }
        ],
        items: [
        {
            xtype: 'grid',
            layout:'fit',
            border: false,
            itemCount: 0,
            itemId: 'equipGrid',
            selModel:{
                mode: 'MULTI'
            },
            viewConfig: {
                        plugins: {
                            pluginId:'dndplug',
                            ptype: 'gridviewdragdrop',
                            dragGroup: 'eqStoreDND1'+self.accountId,
                            dropGroup: 'eqStoreDND2'+self.accountId

                        }
                    },
            store:Ext.create('EDS.store.ObjectsEquipmentService',{
                //autoLoad: true,
                autoSync:false,
                sorters: [{
                    sorterFn: function(o1, o2){
                        var getRank = function(o){
                            var name = o.get('eqtype');
                            if (name === 'Основной абонентский терминал') {
                                return 1;
                            } else {
                                return 2;
                            }
                        },
                        rank1 = getRank(o1),
                        rank2 = getRank(o2);

                        if (rank1 === rank2) {
                            return 0;
                        }

                        return rank1 < rank2 ? -1 : 1;
                    }
                }],
                listeners: {
                    beforeload: function (store, op) {
                        store.getProxy().setExtraParam("uid", self.up('panel').getRecord().get("uid"));
//                        var accountId=self.up('panel').getRecord().get("accountId")
//                        if(accountId!=null && accountId!=undefined)
//                        {
//                              console.log("extra param accountId", accountId)
//                            store.getProxy().setExtraParam("accountId", accountId);
//                        }
                        store.getProxy().setExtraParam("accountId", 'notrequired');
                        var eqgriddnd=self.down('grid').getView().getPlugin('dndplug')
                        eqgriddnd.dragZone.addToGroup('eqStoreDND1')
                        eqgriddnd.dropZone.addToGroup('eqStoreDND2')
                    }
                }
            }),
            columns: [
//            {
//                header: '№',
//                xtype: 'rownumberer',
//                width:40,
//                resizable:true
//            },
            {
                header: 'Тип устройства',
                flex: 3,
                sortable: true,
                dataIndex: "eqtype",
                filter: {
                    type: 'string'
                }
            },
            {
                header: 'Марка',
                flex: 1,
                sortable: true,
                dataIndex: "eqMark",
                filter: {
                    type: 'string'
                }
            },
            {
                header: 'Модель',
                flex: 1,
                sortable: true,
                dataIndex: "eqModel",
                filter: {
                    type: 'string'
                }
            },
            {
                header: 'Серийный номер',
                flex: 2,
                sortable: true,
                dataIndex: "eqSerNum",
                filter: {
                    type: 'string'
                }
            },
            {
                header: 'IMEI',
                flex: 2,
                sortable: true,
                dataIndex: "eqIMEI",
                filter: {
                    type: 'string'
                }
            },
            {
                header: 'Абонентский номер',
                hidden:self.hideRule,
                flex: 2,
                sortable: true,
                dataIndex: "simNumber",
                filter: {
                    type: 'string'
                }
            },
            {
                header: 'Прошивка',
                flex: 2,
                sortable: true,
                dataIndex: "eqFirmware",
                filter: {
                    type: 'string'
                }
            }],
        listeners: {
            itemdblclick: function (/*Ext.view.View */self0, /*Ext.data.Model*/ record, /*HTMLElement*/ item, /*Number*/ index, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
                this.up('eqpanel').showEquipmentForm(record);
            }
        },
        refresh: function () {
            this.store.load()
        }
        }
        ],
        listeners:{
                afterrender:function(panel,eopts){
                    panel.up('window').down('[name=accountId]').on('select',
                        function( combo, newRecord, eOpts ){
                            var record=combo.up('[itemId=objpanel]').getRecord();
                            var oldValue=record.get("accountId");
                            var newValue=newRecord[0].get("_id");
                            console.log('accountId changed from '+oldValue+'to '+newValue);
                            var grid=self.down('grid');
                            var eqgriddnd=grid.getView().getPlugin('dndplug');
                            var acceqstorewnd=Ext.ComponentQuery.query('[xtype=acceqstorewnd]');
                            for(i in acceqstorewnd)
                            {
                                if (acceqstorewnd[i].id=='accEqStoreWnd'+oldValue)
                                {
                                    acceqstorewnd[i].close();
                                    break;
                                }
                            }
                            eqgriddnd.dragZone.removeFromGroup('eqStoreDND1'+oldValue);
                            eqgriddnd.dropZone.removeFromGroup('eqStoreDND2'+oldValue);
                            combo.up('[itemId=objpanel]').updateRecord();
                            if(record.get("_id")!=null){ grid.getStore().load();}
                            else {grid.getStore().removeAll()}
                            eqgriddnd.dragZone.addToGroup('eqStoreDND1'+newValue);
                            eqgriddnd.dropZone.addToGroup('eqStoreDND2'+newValue);
                            console.log('eqgriddnd',eqgriddnd);
                        });
                }
            },
        showAccStoreWnd:function(){
        console.log("add from acc eqstore");
                    var accountId=this.up('window').down('[itemId=objpanel]').getRecord().get('accountId');
                    var accountName=this.up('window').down('[name=accountId]').getRawValue();
                    console.log("accountId",accountId);
                    var storewnd=Ext.getCmp('eqAddWnd');
                    console.log("storewnd",storewnd);
                    if(storewnd && storewnd.isVisible())
                        storewnd.close();
                    var existingWindow = WRExtUtils.createOrFocus('accEqStoreWnd'+accountId, 'Billing.view.equipment.AccEqStoreWindow', {
                        title: 'Склад "'+accountName+'"',
                        accountId: accountId
                    });
                    var eqAddStore=existingWindow.down('grid').getStore();
                    eqAddStore.getProxy().setExtraParam("accountId", accountId);
                    eqAddStore.load();
                    self.down('grid[itemId=equipGrid]').getStore().load();
                    existingWindow.show();
        },
        showEqAddWnd:function(){
        console.log("add from main eqstore")
                    var accountId=this.up('window').down('[itemId=objpanel]').getRecord().get('accountId');
                    var accountName=this.up('window').down('[name=accountId]').getRawValue();
                    console.log("accountId",accountId);
                    var storewnd=Ext.getCmp('accEqStoreWnd'+accountId);
                    console.log("storewnd",storewnd);
                    if(storewnd)
                        storewnd.close();
                    var existingWindow = WRExtUtils.createOrFocus('eqAddWnd', 'Billing.view.equipment.EquipmentStoreWindow', {
                        title: 'Главный склад'//+accountName+'"'
                    });
                    var eqgriddnd=self.down('grid').getView().getPlugin('dndplug');
                    console.log("object eq grid dnd",eqgriddnd);
                        var eqAddStore=existingWindow.down('grid').getStore();
                        eqAddStore.getProxy().setExtraParam("accountId", null);
                        eqAddStore.load();
                        self.down('grid[itemId=equipGrid]').getStore().load();
                        existingWindow.show();
        },
        showEquipmentForm: function(record){
                console.log('showEqForm');
                var self=this;
                var eqtype=record.get('eqtype');
                if(eqtype=="")
                    eqtype="Новое устройство";
                var rec=record.copy();
                var objname=self.up('[itemId=objpanel]').getRecord().get("name");
                if(objname)
                    rec.set("objectName",objname);
                var accname=self.up('[itemId=objpanel]').down("combobox").getRawValue();
                if(accname)
                    rec.set("accountName",objname);
                var existingWindow = WRExtUtils.createOrFocus('eqWnd' + rec.get('_id'), 'Billing.view.equipment.EquipmentWindow', {
                    title: eqtype+' "' + rec.get('eqIMEI') + '"',
                    hideRule:self.hideRule
                });
                if (existingWindow.justCreated)
                    {
                    existingWindow.down('[itemId=eqPanel]').loadRecord(rec);
                    existingWindow.down('[itemId=eqPanel]').on('save', function (args) {
                        var eqstorepanel=Ext.ComponentQuery.query('[xtype=eqstorepanel]');
                        self.down('grid').getStore().load();
                        eqstorepanel[0].refresh();
                        console.log('args',args);
                        });
                    }
                existingWindow.show();

                return existingWindow;
        },
        addEquipmentForm:function(){
        var rec=Ext.create('Equipment',{});
        var self=this;
        var eqWnd = this.showEquipmentForm(rec);
        eqWnd.down("[name=objectName]").setVisible(false);
        return eqWnd;
        },
        removeEquipment:function(){
        var self=this;
        var grid=self.down('[itemId=equipGrid]');
        var selection = grid.getView().getSelectionModel().getSelection();
        if (selection) {
            var store = grid.store;
            store.remove(selection);
            var acceqstorewnd=Ext.ComponentQuery.query('[xtype=acceqstorewnd]')
            for(i in acceqstorewnd)
            {
                if (acceqstorewnd[i].id=='accEqStoreWnd'+self.accountId)
                {
                 acceqstorewnd[i].down('grid').getStore().insert(0,selection)
                 break;
                }
            }
            }
        }
    });
    this.callParent();
}
});

Ext.define('Billing.view.object.ObjectForm.Window', {
    extend: 'Ext.window.Window',
    alias: 'widget.objwnd',
    title: 'Объект',
    width: 900,
    height: 750,
    maximizable: true,
    layout: 'fit',
    icon: 'images/car_002_blu_24.png',
    initComponent: function () {
            var self = this;
            console.log('ObjPanel self',self)
        this.on('close',function(wnd, eOpts){
            var accountId=self.down('[itemId=objpanel]').getRecord().get("accountId");
            EquipmentStoreUtils.storeRefresh(accountId)
        });

        Ext.apply(this,{
            items: [
                {   xtype: 'form',
                    itemId: 'objpanel',
                    layout: {
                        type: 'border'
                    },
                    items: [
                            {
                                xtype: 'objform',
                                flex:2,
                                //collapsible:true,
                                region:'center',
                                hideRule:self.hideRule
                            },
                            {
                                xtype: 'eqpanel',
                                split: true,
                                collapsible:true,
                                flex:1,
                                region:'south',
                                accountId: self.accountId,
                                hideRule: self.hideRule
                            }
                    ],
            dockedItems: [
                {
                    xtype: 'toolbar',
                    dock: 'bottom',
                    ui: 'footer',
                    items: [
                        {text: 'История',
                         disabled:self.hideRule,
                         handler: function () {
                            var uid=self.down('form').getRecord().get("uid");
                            var existingWindow = WRExtUtils.createOrFocus('ObjectEventsPanel' + uid, 'Billing.view.event.EventsWindow', {
                                aggregateId:uid,
                                aggregateType:"ObjectAggregate"
                            });
                            existingWindow.show();
                        }
                        },
                        {text: 'Датчики',
                         disabled:self.hideRule,
                         handler: function () {
                             var rec=self.down('form').getRecord();
                             var uid=rec.get("uid");
                             var existingWindow = WRExtUtils.createOrFocus('ObjectSensorsWindow' + uid, 'Billing.view.object.sensors.ObjectSensorsWindow', {
                                 objRecord: rec
                                 //aggregateId:uid,
                                 //aggregateType:"ObjectAggregate"
                             });
                             existingWindow.show();
                         }
                        },
                        '->',
                        {
                        icon: 'images/receipt_excel.png',
                        text: 'Из Excel',
                        disabled:self.hideRule,
                        handler: function () {
                            var self = this;
                            var accountId = self.up('panel').getRecord().get("accountId");
                            console.log("accountId",accountId)
                            if (accountId == null || accountId == "") {
                                Ext.MessageBox.show({
                                    title: 'Произошла ошибка',
                                    msg: "Не указана учетная запись",
                                    icon: Ext.MessageBox.ERROR,
                                    buttons: Ext.Msg.OK
                                });
                            }
                            else {
                                var wnd = Ext.create('Ext.window.Window', {
                                    title: self.up('window').title + ' -  вставьте строку из Excel',
                                    width: 600,
                                    height: 200,
                                    layout: 'fit',
                                    items: [
                                        {
                                            xtype: 'form',
                                            layout: 'fit',
                                            items: [
                                                {
                                                    xtype: 'textareafield',
                                                    name: 'excelString',
                                                    anchor: '100%',
                                                    emptyText: 'Вставьте данные',
                                                    maxWidth: 600,
                                                    listeners: {change: function (fld) {
                                                        console.log(fld.inputEl.getScroll());
                                                        fld.inputEl.scroll('l');
                                                        fld.inputEl.scroll('t');
                                                        //self.setCaretPosition(0)
                                                        //console.log('caret position',self.getCaretPosition())
                                                    }}
                                                }
                                            ],
                                            bbar: [
                                                '->',
                                                {xtype: 'button', text: 'Готово',
                                                    handler: function () {
                                                        console.log(this.up('form').getValues());
                                                        console.log('self.up', self.up('form'));
                                                        //self.up('form').parseExcelString(this.up('form').getValues())
                                                        console.log('self');
                                                        ObjectFromExcel.parseExcelString(this.up('form').getValues(), self.up('form'));
                                                        this.up('window').close();
                                                    }},
                                                {xtype: 'button', text: 'Очистить',
                                                    handler: function () {
                                                        this.up('form').getForm().reset();
                                                    }},
                                                {xtype: 'button', text: 'Отменить',
                                                    handler: function () {
                                                        this.up('window').close();

                                                    }}
                                            ]}
                                    ]});
                                wnd.show();
                            }
                        }
                        },
                        {
                            icon: 'images/ico16_checkall.png',
                            itemId: 'save',
                            disabled: self.hideRule,
                            text: 'Сохранить',
                            //scope:this,
                            handler: function () {
                                this.up('form').onSave();
                            }
                        },
                        {
                            icon: 'images/ico16_okcrc.png',
                            itemId: 'savenclose',
                            disabled: self.hideRule,
                            text: 'Сохранить и закрыть',
                            //scope:this,
                            handler: function () {
                                this.up('form').onSave(/*close*/true);
                            }
                        },
                        {
                            icon: 'images/ico16_cancel.png',
                            text: 'Отменить',
                            handler: function () {
                                this.up('window').close();
                            }
                        }
                    ]
                }
            ],

            listeners: {
                afterrender: function (form, eopts) {
                    var _id = form.getRecord().get("_id");
                    form.down('objform').loadUsers(_id,form.down('[name=userscount]'));
                    var name = form.getRecord().get("name");
                    var type = form.getRecord().get("type");
                    form.onLoad();
                }
            },
            onSave: function (close) {
                var self = this;
                console.log("On Save");
                var form = this.getForm();
                var eqGrid = this.down('[itemId=equipGrid]');
                if (form.isValid()) {
                    console.log("form =", form);
                    console.log("Form updateRec=", form.updateRecord());
                    var data = form.getRecord().getData();
                    data['equipment'] = new Array();
                    var eqGridStore=eqGrid.getStore();;
                    for (var j = 0; j < eqGridStore.getCount(); j++) {
                        data['equipment'].push(eqGridStore.getAt(j).getData());
                    }
                    console.log("data=", data);
                    objectData.updateData(data,/* eqCount,*/ function (submitResult, e) {
                        console.log("Результат onSave", submitResult);
                        if (!e.status) {
                            Ext.MessageBox.show({
                                title: 'Произошла ошибка',
                                msg: e.message,
                                icon: Ext.MessageBox.ERROR,
                                buttons: Ext.Msg.OK
                            });
                        }
                        else {
                            console.log('Объект успешно сохранен');
                            self.fireEvent('save',submitResult);
                            if(close)
                                {self.up('window').close();}
                            else{
                            self.onLoad(submitResult.uid);
                            }

                        }
                    });
                }
            },
            onReset: function () {
                this.getForm().reset();
            },
            onLoad: function (uid) {
                var self = this;
                var form = this.getForm();
                console.log("Onload form record=", form.getRecord());
                if(!uid)
                    {uid = this.getRecord().get("uid");}
                var accountId = this.getRecord().get("accountId");
                var eqGrid= this.down('[itemId=equipGrid]');
                if (uid != null && uid != "") {
                    objectData.loadData(uid, function (loadResult) {
//                        if (!loadResult.accountId) {
                            if(loadResult.account)
                                loadResult.accountId = loadResult.account;
                            else
                                loadResult.accountId = accountId;
//                        }
                        console.log('loadResult=', loadResult);
                        var currentObject = Ext.create('ObjectF', loadResult);
                        form.loadRecord(currentObject) ;
                        var accField=self.down('[name=accountId]');
                        accField.getStore().load();
                        accField.setValue(loadResult.accountId);
                        eqGrid.getStore().load();
                        self.up('window').setTitle('Объект "'+loadResult.name+'"');
                        console.log("Результат запроса", loadResult);
                        setTimeout(function () {
                            if (!loadResult)
                                Ext.Msg.alert('Запрос выполнен ', 'Произошла ошибка').toFront();
                        }, 0);
                    });
                }
            },
            updateData: function(data,uid){
                var self=this;
                var rec=self.getRecord()
                var uid=rec.get("uid")
                if(data.aggregate=='object' && data.itemId==uid){
                    for(i in data.data) {
                        rec.set(i,data.data[i])
                    }
                    self.loadRecord(rec)
                }
            }
        }
        ]
        });
        this.callParent();
        WRExtUtils.AtmosphereExtJSBroadcaster.on("dataChanged",function(data){
            if(self.down('form')!=null)
                self.down('form').updateData(data)
        })
}
});
Ext.define('ObjectF', {
        extend: 'Ext.data.Model',
        fields: [
            '_id',
            //Вкладка Параметры объекта
            'name','customName', 'comment', 'uid', 'type',/*'contract'*/'cost',/*'subscriptionfee',*/ 'marka', 'accountId',
            'model', 'gosnumber', 'VIN', /*'instplace',*/ 'objnote','fuelPumpLock','ignitionLock', "disabled"
        ],
        idProperty: '_id'
    });
 
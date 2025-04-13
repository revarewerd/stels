/**
 * Created by IVAN on 30.07.2014.
 */
Ext.define('Billing.view.equipment.AccountEquipmentsWindow', {
    extend: 'Ext.window.Window',
    alias: 'widget.eqswnd',
    width: 1024,
    height: 768,
    //constrainHeader: true,
    layout:'fit',
    maximizable:true,
//    initComponent: function () {
//
//        var self = this
//
//    Ext.apply(this, {
    initComponent: function () {
        this.on('close', function (wnd, eOpts) {
            console.log('obj win close');
            var eqaddwnd = Ext.ComponentQuery.query('[xtype=eqaddwnd]');
            eqaddwnd[0].down('grid').getSelectionModel().deselectAll();
            eqaddwnd[0].down('grid').getStore().load();
        })
        var self = this
        Ext.apply(this, {
            items: [
                {
                    xtype: 'equipmentsgrid',
                    hideRule: this.hideRule,
                    border: false,
                    itemCount: 0,
                    itemId: 'equipGrid'//,
//            layout:'fit',
//            multi:true,
                }
            ],
            dockedItems: [
                {
                    xtype: 'toolbar',
                    dock: 'bottom',
                    ui: 'footer',
                    items: [
                        '->',
                        {
                            icon: 'images/ico16_okcrc.png',
                            itemId: 'save',
                            text: 'Сохранить',
                            disabled: self.hideRule,
                            handler: function () {
                                this.up('window').onSave();
                            }
                        }, {
                            icon: 'images/ico16_cancel.png',
                            text: 'Отменить',
                            handler: function () {
                                this.up('window').close();
                            }
                        }]
                }
            ]
        })
        console.log('this.hideRule=', this.hideRule)
        this.callParent();
    },
    onSave:function(){
        //this.down('grid').getStore().sync()
        var store=this.down('grid').getStore();
        var removed=store.getRemovedRecords();
        var updated=store.getUpdatedRecords();
        console.log('to update',updated);
        console.log('to remove',removed);
        var dataToRemove=new Array();
        var dataToUpdate=new Array();
        for(i in removed) {
            dataToRemove.push(removed[i].data);
        }
        for(i in updated) {
            dataToUpdate.push(updated[i].data);
        }
        accountsEquipmentService.modify(this.accountId,dataToUpdate,dataToRemove,function(){
            store.load();
        });
        //accountsEquipmentService.remove(this.accountId,dataToRemove)
        this.close();
        console.log('ONSAVE!!!');
    }
});
Ext.define('Billing.view.equipment.AccountEquipmentsGrid', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.equipmentsgrid',
    selModel:{
        pruneRemoved:false,
        mode:'MULTI'
    },
    features: [
        {
            ftype: 'filters',
            encode: true,
            local: false
        }],
    dockedToolbar: ['add','remove','refresh','search'],
    setStore:false,
    //storeName:'EDS.store.ObjectsEquipmentService',
    loadBeforeActivated: true,
    initComponent: function () {
        var eqaddwnd=Ext.ComponentQuery.query('[xtype=eqaddwnd]');
        var self = this;
        console.log("AEG hideRule=",self.hideRule)
        var accountId=self.accountId;
        console.log('accountId   ',accountId);
        Ext.apply(this, {
            viewConfig: {
                plugins: {
                    pluginId:'dndplug',
                    ptype: 'gridviewdragdrop',
                    dragGroup: 'eqStoreDND1',//+accountId,
                    dropGroup: 'eqStoreDND2'//+accountId

                },
                listeners:{
                    beforedrop:function( node, data, overModel, dropPosition, dropHandlers, eOpts ){
                        console.log("beforedrop", data);
                    },
                    drop: function(node, data, overModel, dropPosition, eOpts ){
                        console.log("drop", data);
                        var store=self.getStore();
                        console.log("store", store);
                        for(var i in data.records){
                            data.records[i].setDirty();
                            for(var j in store.removed){
                                if(data.records[i]==store.removed[j])
                                {   console.log('not removed',store.removed.splice(j,1));
                                }
                            }
                        }
                        if (store.removed.length==0) store.removed=[];
                        console.log("drop", data.records[0].data.recordType);
                    }
                }
            },
            store:Ext.create('EDS.store.AccountsEquipmentService',{
                autoLoad: true,
                autoSync:false,
                listeners: {
                    beforeload: function (store, op) {
                        console.log('grid beforeload');
                        console.log('self rec',self.up('window').accountId);
                        store.getProxy().setExtraParam("accountId", self.up('window').accountId);
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
                    header: 'Объект',
                    flex: 2,
                    sortable: true,
                    dataIndex: "objectName",
                    filter: {
                        type: 'string'
                    }
                },
                {
                    header: 'Тип устройства',
                    //width:170,
                    flex: 3,
                    sortable: true,
                    dataIndex: "eqtype",
                    filter: {
                        type: 'string'
                    }
                },
                {
                    header: 'Марка',
                    //width:170,
                    flex: 1,
                    sortable: true,
                    dataIndex: "eqMark",
                    filter: {
                        type: 'string'
                    }
                },
                {
                    header: 'Модель',
//                width:170,
                    flex: 1,
                    sortable: true,
                    dataIndex: "eqModel",
                    filter: {
                        type: 'string'
                    }
                },
                {
                    header: 'Серийный номер',
                    //width:170,
                    flex: 2,
                    sortable: true,
                    dataIndex: "eqSerNum",
                    filter: {
                        type: 'string'
                    }
                },
                {
                    header: 'IMEI',
                    //width:170,
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
                    //width:170,
                    flex: 2,
                    sortable: true,
                    dataIndex: "simNumber",
                    filter: {
                        type: 'string'
                    }
                },
                {
                    header: 'Прошивка',
                    //width:170,
                    flex: 2,
                    sortable: true,
                    dataIndex: "eqFirmware",
                    filter: {
                        type: 'string'
                    }
                }],
            listeners: {
                itemdblclick: function (/*Ext.view.View */self0, /*Ext.data.Model*/ record, /*HTMLElement*/ item, /*Number*/ index, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
                    console.log("doubleclick");
                    console.log("record",record);
                    this.showEquipmentForm(record);
                }
            },
            showEquipmentForm: function(record){
                console.log('showEqForm');
                console.log('record=',record);
                var self=this;
                var eqtype=record.get('eqtype');
                if(eqtype=="") eqtype="Новое устройство";
                var existingWindow = WRExtUtils.createOrFocus('eqWnd' + record.get('_id'), 'Billing.view.equipment.EquipmentWindow', {
                    title: eqtype+' "' + record.get('eqIMEI') + '"',
                    hideRule:self.hideRule
                });
                if (existingWindow.justCreated)
                {//existingWindow.down('eqform').loadRecord(record);
                    existingWindow.down('[itemId=eqPanel]').loadRecord(record);
                    existingWindow.down('[itemId=eqPanel]').on('save', function (args) {
                        self.refresh();
                        console.log('args',args);
                    })
                }
                existingWindow.show();
                return existingWindow;//.down('[itemId=eqPanel]');
            },
            onSelectChange: function (selModel, selections) {
                if(self.hideRule)
                    this.down('#delete').setDisabled(true);
                else
                    this.down('#delete').setDisabled(selections.length === 0);
            },
            onAddClick: function(){
                console.log("add from eqstore");
                var existingWindow = WRExtUtils.createOrFocus('eqAddWnd', 'Billing.view.equipment.EquipmentStoreWindow', {});
                var eqAddStore=existingWindow.down('grid').getStore();
                eqAddStore.getProxy().setExtraParam("accountId", null);
                eqAddStore.load();
//                    self.refresh()
                existingWindow.show();
            },
            onDeleteClick: function () {
                var selection = this.getView().getSelectionModel().getSelection();//[0];

                if (selection) {
                    var store = this.store;
                    //Ext.MessageBox.confirm('Удаление элемента', 'Вы уверены, что хотите удалить ' + selection.length + ' учетных записей?', function (button) {
                    //    if (button === 'yes') {
                    store.remove(selection);
                    console.log('store.removed',store.removed);
                    var notremove=new Array();
                    for(var j in store.removed){
                        if(!store.removed[j].get('objectName'))
                            notremove.push(store.removed[j]);
                    }
                    for(var i in notremove){
                        for(var j in store.removed){
                            if(notremove[i]==store.removed[j])
                            {
                                console.log('not removed',store.removed.splice(j,1));
                                break;
                            }
                        }
                    }
                    console.log('eqaddwnd',eqaddwnd);
                    eqaddwnd[0].down('grid').getStore().insert(0,selection);
                    //    }
                    //});
                }
            }
        });
        this.callParent();
        this.down('#add').setDisabled(self.hideRule);
    },
    refresh:function(){
        var eqaddwnd=Ext.ComponentQuery.query('[xtype=eqaddwnd]');
        eqaddwnd[0].down('grid').getSelectionModel().deselectAll();
        eqaddwnd[0].down('grid').getStore().load();
        this.callParent();
    }
});
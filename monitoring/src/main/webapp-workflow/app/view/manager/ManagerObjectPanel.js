/**
 * Created by IVAN on 16.12.2014.
 */
Ext.require([
    'EDS.Ext5.store.ObjectsData'
])

Ext.define('Workflow.view.manager.ManagerObjectPanel', {
    extend: 'Ext.form.Panel',
    alias: 'widget.objpanel',
    layout: {
        type: 'border'
    },
    docked: ["add","remove"],
    initComponent: function () {
        var self = this;
        var dockedList= {
            add: {
                text:"Добавить объект",
                handler: function (btn) {
                    var form=btn.up("form")
                    form.reset()
                    form.loadRecord(Ext.create("ObjectShort",{accountId:self.accountId,uid:""}))
                }
            }
        }
        var docked=function(){
            var items=[]
            for(j in self.docked){
                console.log("action=",self.docked[j])
                items.push(dockedList[self.docked[j]])
            }
            console.log("items",items)
            return items
        }
        Ext.apply(this,{
            items: [
        {
            region: 'west',
            width: '30%',
            floatable: false,
            titleCollapse: true,
            //border:true,
            split: true,
            collapsible: true,
            xtype: 'grid',
            title: 'Объекты',
            dockedItems: [
                {
                    xtype: 'toolbar',
                    itemId:'topBar',
                    dock: 'top',
                    ui: 'footer',
                    items: [
                        {
                            text:"Обновить",
                            handler:function(){
                                self.refresh()
                        }
                        }
                    ]
                }
            ],
            store: Ext.create('EDS.Ext5.store.ObjectsData', {
                //autoLoad: true,
                listeners: {
                    beforeload: function (store, op) {
                        console.log("objects store load for accountId=",self.accountId)
                        store.getProxy().setExtraParam("accountId", self.accountId)
                        self.down('form').reset()
                    },
                    write: function (proxy, operation) {
                        console.log(operation.action + " " + operation.resultSet.message);
                    }
                }
            }),
            columns: [
                {text: 'UID', dataIndex: 'uid', flex: 2},
                {text: 'Наименование', dataIndex: 'name', flex: 3},
                {
                    xtype: 'actioncolumn',
                    width: 25,
                    icon: 'images/ico16_show.png',  // Use a URL in the icon config
                    tooltip: 'Просмотр',
                    handler: function (view, rowIndex, colIndex,item,e,record,row) {
                        view.getSelectionModel().select(record)
                        self.showObject(record)
                    }
                },
                {
                    xtype: 'actioncolumn',
                    width: 25,
                    icon: 'images/ico16_crossinc.png',  // Use a URL in the icon config
                    tooltip: 'Удалить',
                    handler: function (view, rowIndex, colIndex,item,e,record,row) {
                        view.getSelectionModel().select(record)
                        self.removeObject(record)
                    }
                }
            ],
            listeners:{
                rowdblclick: function( grid, record, tr, rowIndex, e, eOpts )  {
                    self.showObject(record)
                }
            }
        },
        {
            region: 'center',
            xtype: 'form',
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            fieldDefaults: {
                labelWidth: 150,
                margin: '0 20 5 20'
            },
            dockedItems: [
                {
                    xtype: 'toolbar',
                    itemId:'topBar',
                    dock: 'top',
                    ui: 'footer',
                    items: docked()
                },
                {
                    xtype: 'toolbar',
                    dock: 'bottom',
                    items: [
                        '->',
                        {
                            text: 'Сохранить',
                            handler:function(btn) {
                                var form= btn.up("form")
                                form.updateRecord()
                                var data=form.getRecord().getData()
                                //if(!data.uid) data['equipment'] = new Array();
                                console.log("data",data)
                                objectData.updateOnlyObjectData(data, function (submitResult, e) {
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
                                        self.up('#curTicketPanel').down("#objectSelection").getStore().load();
                                        self.refresh();
                                    }
                                })
                            }
                        },
                        {
                            text: 'Отменить',
                            handler:function(btn){
                                self.reset()
                            }
                        }
                    ]
                }
            ],
            defaultType: 'textfield',
            items: [
                {
                    margin: '10 20 5 20',
                    fieldLabel: 'Наименование',
                    name: 'name',
                    allowBlank: false,
                    regex: /^[\S]+.*[\S]+.*[\S]+$/i//,
                    //readOnly: true
                },
                {
                    xtype: 'checkbox',
                    fieldLabel: 'Отключен',
                    name:'disabled'//,
                    //readOnly:this.hideRule
                },
                {
                    fieldLabel: 'Тип',
                    name: 'type'//,
                    //readOnly: true
                },
                {
                    fieldLabel: 'Марка',
                    name: 'marka'//,
                    //readOnly: true
                },
                {
                    fieldLabel: 'Модель',
                    name: 'model'//,
                    //readOnly: true
                },
                {
                    fieldLabel: 'Госномер',
                    name: 'gosnumber'//,
                    //readOnly: true
                },
                {
                    fieldLabel: 'VIN',
                    name: 'VIN'//,
                    //readOnly: true
                },
                {
                    columnWidth: 1,
                    xtype: 'textareafield',
                    rows: 2,
                    fieldLabel: 'Примечание',
                    name: 'objnote'
                },
                {
                    xtype: 'fieldcontainer',
                    margin: '0 20 0 0',
                    layout: 'hbox',
                    items: [
                        {
                            xtype: 'checkbox',
                            labelWidth: 180,
                            fieldLabel: 'Блокировка бензонасоса',
                            name: 'fuelPumpLock'//,
                            //readOnly: true
                        },
                        {
                            xtype: 'checkbox',
                            margin: '0 0 0 20',
                            labelWidth: 150,
                            fieldLabel: 'Блокировка зажигания',
                            name: 'ignitionLock'//,
                            //readOnly: true
                        }
                    ]
                }
            ]
        }
    ],
    removeObject:function(record){
        var toRemove=new Array();
        toRemove.push(record.get("_id"));
        allObjectsService.remove(toRemove,function(result,e){
            if (!e.status) {
                Ext.MessageBox.show({
                    title: 'Произошла ошибка',
                    msg: e.message,
                    icon: Ext.MessageBox.ERROR,
                    buttons: Ext.Msg.OK
                });
            }
            else {
                console.log('object removed');
                self.refresh();
                }
        })
    },
    showObject:function(record){
        var objPanel=self
        var form=objPanel.down("form")
        var uid=record.get("uid")
        objectData.loadData(uid,function(data,e){
            if (!e.status) {
                Ext.MessageBox.show({
                    title: 'Произошла ошибка',
                    msg: e.message,
                    icon: Ext.MessageBox.ERROR,
                    buttons: Ext.Msg.OK
                });
            }
            else {
                console.log('Объект - ',data);
                if(data.accountId!=null)
                    data.account=data.accountId
                else data.account=self.accountId
                var rec = Ext.create('ObjectShort',data)
                form.loadRecord(rec)
            }
        })
    },
    refresh:function(){
        self.down('grid').getStore().load()
    }
        });
        this.callParent();
    },
    reset:function(){
        var form= this.down('form')
        var rec=form.getRecord()
        console.log("reset to base record=",rec)
        form.loadRecord(rec)
    }
})

Ext.define('ObjectShort', {
    extend: 'Ext.data.Model',
    fields: [
        '_id',
        //Вкладка Параметры объекта
        'name',/*'customName', 'comment',*/ 'uid', 'type',/*'contract',cost','subscriptionfee',*/ 'marka', 'accountId',
        'model', 'gosnumber', 'VIN', /*'instplace',*/ 'objnote','fuelPumpLock','ignitionLock', "disabled"
    ],
    idProperty: '_id'
});
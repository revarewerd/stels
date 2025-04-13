/**
 * Created by IVAN on 17.12.2014.
 */
Ext.require([
    'Workflow.view.manager.ManagerWorksGrid',
    'EDS.Ext5.store.ObjectsEquipmentService',
    'Workflow.view.WorkStatusStore',
    'Workflow.view.WorkTypeStore',
    'Workflow.view.manager.InstallEquipmentForm',
    'Workflow.view.manager.RemoveEquipmentForm'
])


Ext.define('Workflow.view.manager.ManagerWorksPanel', {
    extend: 'Ext.form.Panel',
    alias: 'widget.manworkspanel',
    itemId:"manWorksPanel",
    layout: {
        type: 'border'
    },
    initComponent: function () {
        var self = this;
        Ext.apply(this,{
    items: [
        {
            region: 'west',
            floatable: false,
            titleCollapse: true,
            //border:true,
            split: true,
            collapsible: true,
            width: '40%',
            xtype: 'manworksgrid'
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
                    dock: 'top',
                    ui: 'footer',
                    items: [
                        {
                            text: "Добавить работу",
                            handler: function(btn){
                                self.resetForm()
                            }
                        }
                    ]
                },
                {
                    xtype: 'toolbar',
                    dock: 'bottom',
                    items: [
                        '->',
                        {
                            text: 'Сохранить',
                            handler: function(btn){
                                //var form=self;
                                var instEqForm=self.down('#manInstEqForm');
                                var remEqForm=self.down('#manRemEqForm');
                                console.log("workForm",self)
                                self.updateRecord();
                                if(!self.isValid()){
                                    Ext.MessageBox.show({
                                        title: 'Произошла ошибка',
                                        msg: "Заполните все необходимые поля корректными данными",
                                        icon: Ext.MessageBox.ERROR,
                                        buttons: Ext.Msg.OK
                                    });
                                }
                                else {
                                    var record = self.getRecord();
                                    var workType = record.get("workType");
                                    var object = record.get("object");
                                    console.log("record", record);
                                    var objRec = self.down("#objectSelection").findRecordByValue(object);
                                    var objName = objRec.get("name");
                                    record.set("objectName", objName);
                                    switch (workType) {
                                        case 'install':
                                        {
                                            var instData = instEqForm.getData();
                                            record.set("install", instData);
                                            break;
                                        }
                                        case 'remove':
                                        {
                                            var remData = remEqForm.getData();
                                            record.set("remove", remData);
                                            break;
                                        }
                                        case 'replace':
                                        {
                                            var instData = instEqForm.getData();
                                            record.set("install", instData);
                                            var remData = remEqForm.getData();
                                            record.set("remove", remData);
                                            break;
                                        }
                                    }
                                    record.set("workStatus","notcomplete");
                                    var worksGrid = self.up("#curTicketPanel").down("#manWorksGrid");
                                    worksGrid.getStore().add(record);
                                    self.resetForm()
                                }
                        }
                        },
                        {
                            text: 'Отменить',
                            handler: function(btn){
                                self.resetForm()
                            }
                        }
                    ]
                }
            ],
            items: [
                {
                    margin: '10 20 5 20',
                    xtype: 'combobox',
                    fieldLabel: 'Объект',
                    itemId:'objectSelection',
                    name: 'object',
                    forceSelection:true,
                    allowBlank:false,
                    minChars: 0,
                    store:Ext.create('EDS.Ext5.store.ObjectsDataShort', {
                        listeners: {
                            beforeload: function (store, op) {
                                console.log("objects store load for accountId=",self.accountId)
                                store.getProxy().setExtraParam("accountId", self.accountId)
                                //self.down('form').reset()
                            },
                            write: function (proxy, operation) {
                                console.log(operation.action + " " + operation.resultSet.message);
                            }
                        }
                    }),
                    listeners:{
                        select:function ( combo, record, eOpts ){
                            console.log("record",record);
                            //var manRemEqForm=self.down("#manRemEqForm")
                            var existEqValue = self.down("#existEq");
                            var uid=record[0].get("uid");
                            var store=existEqValue.getStore();
                            store.removeAll();
                            store.getProxy().setExtraParam("uid", uid);
                            store.getProxy().setExtraParam("accountId", 'notrequired');
                            store.load();
                            //eqForm.enable();
                            //manRemEqForm.reset();
                        }
                    },
                    valueField: 'uid',
                    displayField: 'name'

                },
                {
                    xtype: 'combobox',
                    fieldLabel: 'Работа',
                    store:Ext.create('Workflow.view.WorkTypeStore'),
                    forceSelection:true,
                    allowBlank:false,
                    valueField: 'workType',
                    displayField: 'workDesc',
                    name: 'workType',
                    listeners: {
                        select: function (combo, record, eOpts) {
                            console.log("record",record)
                            var workType=record[0].get("workType");
                            self.loadEqForms(workType)
                        }
                    }
                },
                {
                    xtype: 'form',
                    itemId:'eqForm',
                    //disabled:true,
                    flex:1,
                    //title: 'Устройство',
                    layout: {
                        // layout-specific configs go here
                        type: 'accordion',
                        multi:true,
                        //titleCollapse: false,
                        animate: false
                        //activeOnTop: true
                    },
                    //layout:{
                    //    type:'vbox',
                    //    align:'stretch'
                    //},
                    items: [
                        {
                            xtype:'manremeqform',
                            title:'Удаление',
                            disabled:true,
                            hidden:true
                        },
                        {
                            xtype:'maninsteqform',
                            title:'Установка',
                            disabled:true,
                            hidden:true
                        }
                    ]
                }
            ]
        }
    ]
        });
        this.callParent();
    },
    loadData:function(record){
        var manWorksPanel=this
        var workType=record.get("workType");
        console.log("workType",workType);
        var uid=record.get('object');
        var store=manWorksPanel.down("#existEq").getStore();
        store.removeAll();
        store.getProxy().setExtraParam("uid", uid);
        store.getProxy().setExtraParam("accountId", 'notrequired');
        store.load();
        manWorksPanel.loadRecord(record);
        manWorksPanel.loadEqForms(workType,record)
    },
    loadEqForms:function(workType,record){
        var instRec=Ext.create('EquipmentShort',{})
        var remRec=Ext.create('EquipmentShort',{})
        if(record!= null && record!=undefined){
            var install=record.get("install");
            if(install!=null && install!=undefined) instRec=Ext.create('EquipmentShort',install)
            var remove=record.get("remove");
            if(remove!=null && remove!=undefined) remRec=Ext.create('EquipmentShort',remove);
        }
        var instForm=this.down("#manInstEqForm");
        var remForm=this.down("#manRemEqForm");
        instForm.reset();
        remForm.reset();
        switch(workType) {
            case "install" :
            {
                instForm.enable();
                instForm.setHidden(false);
                instForm.loadRecord(instRec);
                //instForm.expand();
                remForm.disable();
                remForm.setHidden(true);
                break;
            }
            case "remove" :
            {

                remForm.enable();
                remForm.setHidden(false);
                remForm.loadRecord(remRec);
                remForm.down("#existEq").setValue(remRec.get("eqIMEI"));
                //remForm.expand();
                instForm.disable();
                instForm.setHidden(true);
                break;
            }
            case "replace" :
            {
                instForm.enable();
                instForm.setHidden(false);
                instForm.loadRecord(instRec);
                //instForm.expand();
                remForm.enable();
                remForm.loadRecord(remRec);
                remForm.down("#existEq").setValue(remRec.get("eqIMEI"));
                remForm.setHidden(false);
                break;
            }
            default :
            {
                instForm.disable();
                instForm.setHidden(true);
                remForm.disable();
                remForm.setHidden(true);
            }
        }
    },
    resetForm:function(){
        var form=this
        form.reset();
        var rec=Ext.create('Work',{})
        var instForm=form.down("#manInstEqForm");
        var remForm=form.down("#manRemEqForm");
        instForm.disable();
        instForm.setHidden(true);
        remForm.disable();
        remForm.setHidden(true);
        form.loadRecord(rec);
        instForm.loadRecord(rec);
        remForm.loadRecord(rec);
    }
})

Ext.define('Work', {
    extend: 'Ext.data.Model',
    fields: ['_id','object','objectName','workType','remove','install','workStatus','workResult',/*'eqtype','eqMark','eqModel','eqStatus','eqIMEI',*/
        //{name:'contracts', persist:true},
    ],
    idProperty: '_id'
});
Ext.define('EquipmentShort', {
    extend: 'Ext.data.Model',
    fields: ['_id','eqtype','eqMark','eqModel',/*'eqStatus',*/'eqIMEI'
        //{name:'contracts', persist:true},
    ],
    idProperty: '_id'
});
/**
 * Created by IVAN on 26.02.2015.
 */
Ext.require([
    'Workflow.view.installer.EquipmentStoreGrid',
    'Workflow.view.installer.CurrentWorkForm'
])
Ext.define('Workflow.view.installer.RemoveEquipmentPanel', {
    extend: 'Ext.form.Panel',
    alias: 'widget.instremeqpanel',
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    xtype: 'panel',
    region: 'center',
    layout: 'border',
    items: [
        {
            itemId: "remWorkForm",
            xtype: 'currentworkform',
            region: 'center'
        },
        {
            xtype:'panel',
            title:'Удаление оборудования',
            region: 'east',
            width: '20%',
            titleCollapse: true,
            split: true,
            collapsible: true,
            layout: {
                type:'vbox',
                align:'stretch'//,
                //pack:'center'
            },
            items:[
                {
                    xtype:'panel',
                    layout:'fit',
                    items:[
                        {
                            xtype: 'radiogroup',
                            itemId:'eqRemoveTo',
                            margin:'10 10 10 10',
                            border: true,
                            fieldLabel: 'Поместить на',
                            columns: 1,
                            vertical: true,
                            items: [
                                {boxLabel: 'склад клиента', name: 'store', inputValue: 'accStore', checked: true},
                                {boxLabel: 'склад монтажника', name: 'store', inputValue: 'instStore'},
                                {boxLabel: 'главный склад', name: 'store', inputValue: 'mainStore'}
                            ]
                        }
                    ],
                    dockedItems:[
                        {
                            xtype: 'toolbar',
                            dock: 'bottom',
                            items: [
                                '->',
                                {
                                    text: 'Применить',
                                    itemId:'acceptEqRemoving',
                                    handler:function(){
                                        var storeVal=this.up("#remEqPanel").down("#eqRemoveTo").getValue().store
                                        console.log("storeVal", storeVal)
                                        var equipmentRec=this.up("#remEqPanel").down("#remWorkForm").getRecord()
                                        var eqModel=equipmentRec.get("eqMark")+" "+equipmentRec.get("eqModel")
                                        var eqRemoveResult=this.up("#remEqPanel").down("#eqRemoveResult")
                                        eqRemoveResult.down('[name=equipment]').setValue(eqModel)
                                        eqRemoveResult.down('[name=store]').setValue(storeVal)
                                    }
                                }
                            ]
                        }
                    ]
                }
                ,
                {
                    xtype:'form',
                    title:'Результат',
                    itemId:'eqRemoveResult',
                    layout: {
                        type: 'vbox',
                        align:'stretch'
                    },
                    defaults:{
                        xtype:'textfield',
                        margin:'10 10 10 10',
                        readOnly:true
                    },
                    items:[
                        {
                            fieldLabel:'Устройство',
                            name:"equipment"
                        },
                        {
                            fieldLabel:'Поместить на',
                            itemId:"remEqStoreType",
                            xtype:'combobox',
                            name:"store",
                            valueField: 'storeType',
                            displayField: 'storeDesc',
                            store:Ext.create('Workflow.view.EquipmentStoreTypes')
                        }
                    ]
                }
            ]
        }
        //{
        //    xtype: 'eqstoregrid',
        //    floatable: false,
        //    titleCollapse: true,
        //    //border:true,
        //    split: true,
        //    collapsible: true,
        //    region: 'east',
        //    width: '40%'
        //}
    ],
    loadData:function(record){
        console.log("remEqPanel load",record)
        var remove=record.get("remove")
        var remRec=Ext.create('EquipmentShort',remove);
        var uid=record.get("object");
        var imei=remove.eqIMEI;
        this.loadEquipmentData(remove,uid)
        this.loadObjectData(uid)
        var workResult=record.get("workResult")
        if(workResult!=null && workResult!=undefined){
            this.loadRemoveResult(workResult.remove)
        }
    },
    loadRemoveResult:function(removeResult){
        var storeType=removeResult.storeType
        var equipment=removeResult.data.eqMark+" "+removeResult.data.eqModel
        console.log("storeType",storeType)
        console.log("equipment",equipment)
        var eqRemoveResult=this.down("#eqRemoveResult")
        eqRemoveResult.down('[name=equipment]').setValue(equipment)
        eqRemoveResult.down('[name=store]').setValue(storeType)

    },
    loadEquipmentData:function(data,uid){
        var eqForm=this.down("#remWorkForm")
        var eqId=data._id;
        if(!eqId){
            eqForm.loadRecord(Ext.create('Equipment',data));
        }
        else {
            equipmentData.loadData(eqId,function(result,e){
                if (!e.status) {
                    Ext.MessageBox.show({
                        title: 'Произошла ошибка',
                        msg: e.message,
                        icon: Ext.MessageBox.ERROR,
                        buttons: Ext.Msg.OK
                    });
                }
                else {
                    eqForm.loadRecord(Ext.create('Equipment', result));
                }
            })
        }
        var readOnlyRule=this.up("#curTicketPanel").readOnlyRule
        eqForm.setReadOnly(readOnlyRule)
    },
    loadObjectData:function(uid){
        var objForm=this.down('[name=curObjectForm]')
        if(!uid){
            objForm.loadRecord(Ext.create('ObjectShort',{}))
        }
        else
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
                    objForm.loadRecord(Ext.create('ObjectShort',data))
                }
            })
    }
})
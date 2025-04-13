/**
 * Created by IVAN on 26.02.2015.
 */
Ext.require([
    'Workflow.view.installer.EquipmentStoreGrid',
    'Workflow.view.installer.CurrentWorkForm'
])
Ext.define('Workflow.view.installer.InstallEquipmentPanel', {
    extend: 'Ext.form.Panel',
    alias: 'widget.instinsteqpanel',
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    xtype: 'panel',
    region: 'center',
    layout: 'border',
    items: [
        {
            itemId: "instWorkForm",
            xtype: 'currentworkform',
            region: 'center'
        },
        {
            itemId:"eqStore",
            xtype: 'eqstoregrid',
            floatable: false,
            titleCollapse: true,
            //border:true,
            split: true,
            collapsible: true,
            region: 'east',
            width: '40%'
        }
    ],
    loadData:function(record){
        console.log("instEqPanel load",record);
        var install=record.get("install");
        var instRec=Ext.create('EquipmentShort',install);
        var uid=record.get("object");
        var imei=install.eqIMEI;
        this.loadObjectData(uid);
        var workResult=record.get("workResult")
        if(workResult!=null && workResult!=undefined){
            this.loadInstallResult(workResult.install)
        }
        else this.loadEquipmentData(install,uid)
        var eqForm=this.down("#instWorkForm");
        var eqStore=this.down("#eqStore");
        var readOnlyRule=this.up("#curTicketPanel").readOnlyRule
        eqForm.setReadOnly(readOnlyRule)
        eqStore.down("#selectEq").setDisabled(readOnlyRule)

    },
    loadInstallResult:function(installResult){
        var data=installResult.data
        var eqForm=this.down("#instWorkForm");
        eqForm.reset();
        console.log("inst res data",data)
        eqForm.loadRecord(Ext.create('Equipment', data));
    },
    loadEquipmentData:function(data,uid){
        var eqForm=this.down("#instWorkForm");
        var eqId=data._id;
        if(!eqId || eqId.search("Equipment")>-1){
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
    },
    loadObjectData:function(uid){
        var objForm=this.down('[name=curObjectForm]');
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

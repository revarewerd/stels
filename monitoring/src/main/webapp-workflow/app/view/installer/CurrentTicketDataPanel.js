/**
 * Created by IVAN on 26.11.2014.
 */
Ext.require([
        'Workflow.view.installer.InstallerAccountForm',
        'Workflow.view.installer.WorksGrid',
        'Workflow.view.installer.CurrentWorkDescription',
        'Workflow.view.installer.InstallEquipmentPanel',
        'Workflow.view.installer.RemoveEquipmentPanel'
])

Ext.define('Workflow.view.installer.CurrentTicketDataPanel', {
    extend: 'Ext.tab.Panel',
    alias: "widget.currentticketdatapanel",
    border:true,
    tabPosition: 'left',
    tabBar: {
        defaults: {
            flex: 1
        }
    },
    items: [
        {
            title: 'Учетная запись',
            minTabWidth:500,
            xtype:'instaccform',
            itemId:"instAccountForm"
            //icon: 'images/ico24_msghist.png',
        },
        {
            title: 'Работы',
            layout:'card',
            xtype:'panel',
            itemId:'instWorksPanel',
            //layout:'border',
            items:[
                {
                    xtype:'worksgrid',
                    itemId:'worksGrid',
                    region:'west'
                },
                {
                    xtype:'panel',
                    itemId:"curWorkPanel",
                    region:'center',
                    layout:'border',
                    items:[
                        {
                            xtype:'currentworkdesc',
                            itemId:"curWorkDesc",
                            region:'north',
                            floatable:false,
                            header:false,
                            titleCollapse:true,
                            //border:true,
                            split:true,
                            collapsible: true,
                            collapseMode:'mini',
                            height:'50'
                        },
                        {
                            xtype:'tabpanel',
                            region:'center',
                            itemId:'eqTabPanel',
                            //layout:'border',
                            items:[
                                {
                                    xtype:'instremeqpanel',
                                    itemId:'remEqPanel',
                                    title:'Оборудование к удалению',
                                    //hidden:true
                                    disabled:true
                                },
                                {
                                    xtype:'instinsteqpanel',
                                    itemId:'instEqPanel',
                                    title:'Оборудование к установке',
                                    //hidden:true
                                    disabled:true
                                }
                            ]
                        }
                    ],
                    dockedItems:[
                        {
                            xtype:'toolbar',
                            dock:'bottom',
                            ui:'footer',
                            items:[
                                '->',
                                {
                                    text:"Сохранить как выполненную",
                                    itemId:'saveAsComplete',
                                    disabled:'true',
                                    handler:function(btn){
                                        btn.up("#curWorkPanel").saveAsComplete();
                                    }
                                },
                                {
                                    text:"Сохранить как невыполненную",
                                    itemId:'saveAsNotComplete',
                                    disabled:'true',
                                    handler:function(btn){
                                        btn.up("#curWorkPanel").saveAsNotComplete();
                                    }
                                },
                                {
                                    text:"Отмена",
                                    handler:function(btn){
                                        btn.up("#curWorkPanel").cancel();
                                    }
                                }
                            ]
                        }
                    ],
                    saveAsComplete:function(){
                        console.log("Пометить как выполненную")
                        var worksPanel=this.up('#instWorksPanel')
                        var curWorkDesc=worksPanel.down("#curWorkDesc")
                        //var worksGrid=worksPanel.down("#worksgrid")
                        var workRecord=curWorkDesc.getRecord()
                        console.log("workRecord",workRecord)
                        var installData=null;
                        var removeData=null;
                        var storeType=null;
                        var uid=null;
                        if(workRecord.get("install")!=null){
                            var installForm=this.down("#instWorkForm")
                            var objectForm=installForm.down('[name=curObjectForm]')
                            //installForm.updateRecord()
                            var install=installForm.getRecord()
                            var objectRec=objectForm.getRecord()
                            uid=objectRec.getData().uid
                            console.log("install",install)
                            console.log("install object",objectRec)
                            if(install.get("_id").search("Equipment")==-1){
                                installData=install.getData()
                            }
                            else {
                                Ext.MessageBox.show({
                                    title: 'Произошла ошибка',
                                    msg: "Не выбрано новое оборудование для установки",
                                    icon: Ext.MessageBox.ERROR,
                                    buttons: Ext.Msg.OK
                                });
                                return;
                            }
                        }
                        if(workRecord.get("remove")!=null) {
                            var removeForm = this.down("#remWorkForm")
                            //removeForm.updateRecord()
                            var remove=removeForm.getRecord()
                            console.log("remove",remove)
                            storeType=this.down("#remEqStoreType").getValue()
                            if(storeType!=null && storeType!=""){
                                removeData=remove.getData();
                            }
                            else {
                                Ext.MessageBox.show({
                                    title: 'Произошла ошибка',
                                    msg: "Не указан склад для удаления оборудования",
                                    icon: Ext.MessageBox.ERROR,
                                    buttons: Ext.Msg.OK
                                });
                                return;
                            }
                        }
                        workRecord.set("workResult", {
                            "install": {
                                data:installData,
                                objectUID:uid
                            },
                            "remove": {
                                data: removeData,
                                storeType:storeType
                            }
                        })
                        workRecord.set("workStatus","complete")
                        curWorkDesc.loadRecord(workRecord)
                        console.log("newworkRecord",workRecord)
                        this.cancel()
                    },
                    saveAsNotComplete:function(){
                        console.log("Пометить как невыполненную")
                        var worksPanel=this.up('#instWorksPanel')
                        var curWorkDesc=worksPanel.down("#curWorkDesc")
                        var worksGrid=worksPanel.down("#worksgrid")
                        var workRecord=curWorkDesc.getRecord()
                        console.log("workRecord",workRecord)
                        workRecord.set("workStatus","notcomplete")
                        workRecord.set("workResult",null)
                        curWorkDesc.loadRecord(workRecord),
                        this.cancel()
                    },
                    cancel:function(){
                        console.log("Отмена")
                        var instWorkForm=this.down("#instWorkForm");
                        var remWorkForm=this.down("#remWorkForm");
                        var instEqPanel=this.down("#instEqPanel");
                        var remEqPanel=this.down("#remEqPanel");
                        instEqPanel.disable();
                        remEqPanel.disable();
                        instWorkForm.reset();
                        remWorkForm.reset();
                        remEqPanel.down("#eqRemoveResult").reset()
                        var worksPanel=this.up('#instWorksPanel')
                        var layout = worksPanel.getLayout();
                        layout.setActiveItem('worksGrid');
                    }
                }
            ]
        }
     ],
     dockedItems:[
         {
             xtype:'toolbar',
             dock:'bottom',
             ui:'footer',
             items:[
                 '->',
                 {
                     text:"Принять заявку",
                     itemId:"acceptTicket",
                     disabled:true,
                     handler:function(btn){
                         btn.up('#curTicketDataPanel').acceptTicket()
                     }
                 },
                 {
                     text:"Закрыть заявку",
                     itemId:"closeTicket",
                     disabled:true,
                     handler:function(btn){
                         btn.up('#curTicketDataPanel').closeTicket()
                     }
                 },
                 {
                     text:"Сохранить изменения",
                     itemId:"saveTicket",
                     disabled:true,
                     handler:function(btn){
                         btn.up('#curTicketDataPanel').saveTicket()
                     }
                 },
                 {
                     text:"Отменить изменения",
                     handler:function(btn){
                         btn.up('#curTicketDataPanel').cancelTicket()
                     }
                 }
             ]
         }
     ],
    acceptTicket:function(){
        console.log("Принять заявку")
        var installerPanel=this.up('#installerPanel')
        var currentTicketForm=installerPanel.down("#curTicketDesc")//некорректно брать рекорд из дескрипшна
        var record=currentTicketForm.getRecord()
        console.log("record",record)
        record.set("assignee","current",{silent:true})
        var data=record.getData()
        ticketsService.updateTicket(data,function(result){
            console.log("ticket updated")
            var layout = installerPanel.getLayout();
            layout.prev()
        })
    },
    saveTicket:function(){
        console.log("Сохранить изменения в заявке"),
            console.log("Принять заявку")
        var installerPanel=this.up('#installerPanel')
        var currentTicketForm=installerPanel.down("#curTicketDesc")//некорректно брать рекорд из дескрипшна
        var record=currentTicketForm.getRecord()
        console.log("record",record)
        record.set("assignee","current",{silent:true})
        var data=record.getData()
        ticketsService.updateTicket(data,function(result){
            console.log("ticket updated")
            var layout = installerPanel.getLayout();
            layout.prev()
        })
    },
    closeTicket:function(){
        console.log("Закрыть заявку")
        var installerPanel=this.up('#installerPanel')
        var currentTicketForm=installerPanel.down("#curTicketDesc") //некорректно брать рекорд из дескрипшна
        var record=currentTicketForm.getRecord()
        console.log("record",record)
        var works=record.get("works")
        var isCompleted=true
        for(i in works){
            if(works[i].workStatus!="complete"){
                isCompleted=false;
                break;
            }
        }
        if(isCompleted) {
            record.set("status", "close", {silent: true})
            var data = record.getData()
            ticketsService.updateTicket(data, function (result) {
                console.log("ticket updated")
                installerPanel.down("#ticketsPanel").refresh()
                var layout = installerPanel.getLayout();
                layout.prev()
            })
        }
        else {
            Ext.MessageBox.show({
                title: 'Произошла ошибка',
                msg: "Заявка не может быть закрыта, так-как остались невыполненные работы",
                icon: Ext.MessageBox.ERROR,
                buttons: Ext.Msg.OK
            });
        }
    },
    cancelTicket:function(){
        console.log("Изменения отменены")
        var currentTicketPanel=this.up("#curTicketPanel")
        currentTicketPanel.reset();
        var installerPanel=this.up('#installerPanel')
        var layout = installerPanel.getLayout();
        layout.prev()
    },
    reset:function(){
       var self=this
        self.setActiveItem("instAccountForm")
       self.down("#instAccountForm").reset()
       self.down("#curWorkDesc").reset()
       self.down("#curWorkPanel").cancel()
       self.down("#worksGrid").getStore().removeAll()
    },
    loadData:function(rec){
        var self=this
        var accId = rec.get("accountId")
        var accForm = self.down("#instAccountForm");
        var worksPanel = self.down("#instWorksPanel");
        if (!accId) {
            var aRec = Ext.create('Account', {});
            accForm.loadRecord(aRec);
        }
        else {
            accForm.loadData(accId);
        }
        worksPanel.down("grid").loadData(rec)
    }

});
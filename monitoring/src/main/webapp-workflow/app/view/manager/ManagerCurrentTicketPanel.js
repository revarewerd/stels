/**
 * Created by IVAN on 09.12.2014.
 */
Ext.require([
    'Workflow.view.manager.ManagerAccountPanel',
    'Workflow.view.manager.ManagerObjectPanel',
    'Workflow.view.manager.ManagerWorksPanel'
])

Ext.define('Workflow.view.manager.ManagerCurrentTicketPanel', {
    extend: 'Ext.tab.Panel',
    alias: "widget.managerticketpanel",
    itemId:'curTicketPanel',
    //title: 'Заявки',
    border:true,
    tabPosition: 'left',
//    plugins: 'responsive',
//    responsiveConfig: {
//        'width < 800': {
//            tabPosition: 'left'
//        },
//
//        'width >= 800': {
//            tabPosition: 'top'
//        }
//    },
    //tabStretchMax:true,
    tabBar: {
        defaults: {
            flex: 1
        }
    },
    listeners: {
        beforetabchange: function(tabs, newTab, oldTab) {
            var accId=tabs.down("#existAcc").getValue()
            var objForm=tabs.down("#manObjPanel");
            var worksForm=tabs.down("#manWorksPanel");
            if(!accId){
                return (newTab.title != 'Объекты') && (newTab.title != 'Работы')
            }
        }
    },
    items: [
        {
            title: 'Учетная запись',
            minTabWidth:500,
            xtype:'manaccpanel',
            itemId:'manAccPanel'
            //icon: 'images/ico24_msghist.png',
        },
        {
            title: 'Объекты',
            xtype:'objpanel',
            itemId:'manObjPanel',
            disabled:true
            //icon: 'images/ico24_msghist.png',
        },
        {
            title: 'Работы',
            xtype:'manworkspanel',
            itemId:'manWorksPanel',
            disabled:true
            //icon: 'images/ico24_msghist.png',
        }
//        ,{
//            title: 'Проверка',
//            layout:'fit'
//            //icon: 'images/ico24_msghist.png',
//        }
    ],
    dockedItems:[
        {
            xtype:'toolbar',
            dock:'bottom',
            ui:'footer',
            items:[
                {
                    text:"Повторно открыть заявку",
                    itemId:"openTicketButton",
                    hidden:true,
                    handler:function(btn){
                        btn.up("#curTicketPanel").openTicket()
                    }
                },
                {
                    text:"Закрыть заявку",
                    hidden:true,
                    itemId:"closeTicketButton",
                    handler:function(btn){
                        btn.up("#curTicketPanel").closeTicket()
                    }
                },
                '->',
                {
                    text:"Сохранить",
                    handler:function(btn){
                        btn.up("#curTicketPanel").saveTicket()
                    }
                },
                {
                    text:"Отмена",
                    handler:function(btn){
                        btn.up("#curTicketPanel").cancelTicket()
                    }
                }
            ]
        }
    ],
    openTicket:function(){
        console.log("Открыть заного заявку",record)
        var currentTicketPanel=this
        var record=currentTicketPanel.record
        console.log("record",record)
        if(record.get("status")!="close"){
            Ext.MessageBox.show({
                title: 'Произошла ошибка',
                msg: "Нельзя повторно открыть заявку, которая не была закрыта",
                icon: Ext.MessageBox.ERROR,
                buttons: Ext.Msg.OK
            });
        }
        else {
            record.set("status", "open", {silent: true})
            var data = record.getData()
            ticketsService.updateTicket(data, function (result) {
                console.log("ticket updated")
                var managerPanel = currentTicketPanel.up("#managerPanel");
                managerPanel.down("#ticketsPanel").refresh()
                var layout = managerPanel.getLayout();
                layout.prev()
            })
        }
    },
    closeTicket:function(){
        console.log("Закрыть заявку")
        var currentTicketPanel=this
        var record=currentTicketPanel.record
        console.log("record",record)
        if(record.get("status")!="open"){
            Ext.MessageBox.show({
                title: 'Произошла ошибка',
                msg: "Нельзя закрыть заявку, которая еще не создана",
                    icon: Ext.MessageBox.ERROR,
                buttons: Ext.Msg.OK
            });
        }
        else {
            record.set("status", "close", {silent: true})
            var data = record.getData()
            ticketsService.updateTicket(data, function (result) {
                console.log("ticket updated")
                var managerPanel = currentTicketPanel.up("#managerPanel");
                managerPanel.down("#ticketsPanel").refresh()
                var layout = managerPanel.getLayout();
                layout.prev()
            })
        }
    },
    saveTicket:function(){
        console.log("Сохранить изменения")
        var currentTicketPanel=this;
        var worksGrid=currentTicketPanel.down("#manWorksGrid")
        var accountId=currentTicketPanel.down("#existAcc").getValue()
        var worksData=worksGrid.getStore().getRange()
        console.log("accountId",accountId)
        console.log("worksData=",worksData)
        if(worksData.length==0){
            Ext.MessageBox.show({
                title: 'Произошла ошибка',
                msg: "Не задано ни одной работы",
                icon: Ext.MessageBox.ERROR,
                buttons: Ext.Msg.OK
            });
        }
        else {
            var data = {}
            if (currentTicketPanel.record != null && currentTicketPanel.record != undefined) data = currentTicketPanel.record.getData()
            if (data._id.search("Ticket") != -1) delete data._id
            data.accountId = accountId
            data.works = new Array()
            for (i in worksData) {
                var work = worksData[i].getData()
                delete work._id
                delete work.id
                data.works.push(work)
            }
            console.log("data", data)
            ticketsService.updateTicket(data, function () {
                console.log("ticket updated")
                currentTicketPanel.reset()
                var managerPanel = currentTicketPanel.up('#managerPanel')
                var layout = managerPanel.getLayout();
                layout.prev()
                managerPanel.down("#ticketsPanel").refresh()
            })
        }
    },
    cancelTicket:function(){
        console.log("Изменения отменены")
        var currentTicketPanel=this
        currentTicketPanel.reset()
        var managerPanel=currentTicketPanel.up('#managerPanel')
        var layout = managerPanel.getLayout();
        layout.prev()
    },

    loadData:function(rec){
        var currentTicketPanel=this
        console.log("record",rec)
        var status=rec.get("status")
        var openTicketButton=currentTicketPanel.down("#openTicketButton");
        var closeTicketButton=currentTicketPanel.down("#closeTicketButton");
        switch(status){
            case "open":{
                openTicketButton.setHidden(true);
                closeTicketButton.setHidden(false);
                break;
            }
            case "close":{
                openTicketButton.setHidden(false);
                closeTicketButton.setHidden(true);
                break;
            }
            default :{
                openTicketButton.setHidden(true);
                closeTicketButton.setHidden(true);
            }
        }
        var accId=rec.get("accountId")
        var accForm=currentTicketPanel.down("#accountForm");
        var objPanel=currentTicketPanel.down("#manObjPanel");
        var worksPanel=currentTicketPanel.down("#manWorksPanel");
        var oRec=Ext.create('ObjectShort',{});
        objPanel.down('form').loadRecord(oRec);
        var wRec=Ext.create('Work',{});
        worksPanel.down('grid').showRecord(wRec);
        currentTicketPanel.record=rec;
        if(!accId){
            var aRec=Ext.create('Account',{});
            accForm.loadRecord(aRec);
        }
        else {
            currentTicketPanel.down("#existAcc").setValue(accId)
            accForm.loadData(accId)
            objPanel.enable()
            objPanel.accountId=accId;
            objPanel.down("grid").getStore().load();
            worksPanel.enable()
            worksPanel.accountId=accId;
            worksPanel.down("#objectSelection").getStore().load();
            worksPanel.down("grid").loadData(rec)
        }
    },
    reset:function() {
        var currentTicketPanel = this
        currentTicketPanel.setActiveItem("manAccPanel")
        currentTicketPanel.down("#accountForm").reset()
        currentTicketPanel.down("#existAcc").reset()
        var manObjPanel=currentTicketPanel.down("#manObjPanel")
        delete manObjPanel.accountId
        manObjPanel.reset()
        manObjPanel.disable()
        var manWorksPanel=currentTicketPanel.down("#manWorksPanel")
        delete manWorksPanel.accountId
        manWorksPanel.reset()
        manWorksPanel.disable()
        currentTicketPanel.down("#manWorksGrid").getStore().removeAll()
    }
});
Ext.define('Ticket', {
    extend: 'Ext.data.Model',
    fields: ["_id", "accountId","accountName","status", "assignee","creator","creatorName",
        "openDate","closeDate","works","worksCount","userAssign"
    ],
    idProperty: '_id'
});
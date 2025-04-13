/**
 * Created by IVAN on 26.11.2014.
 */

Ext.define('Workflow.view.TicketsGrid', {
    extend: 'Ext.grid.Panel',
    alias: "widget.ticketsgrid",
    mainPanelItemId:undefined,
    readOnlyRule:false,
    actions: ["view","remove"],
    columns: [
        { text: '№',  dataIndex: '_id',flex:1,minWidth:30 },
        { text: 'Открыт', dataIndex: 'openDate',flex:3,minWidth:50, xtype: 'datecolumn', format: "d.m.Y H:i:s" },
        { text: 'Аккаунт', dataIndex: 'accountName',flex:3,minWidth:50  },
        { text: 'Автор', dataIndex: 'creatorName',flex:3,minWidth:50 },
        { text: 'Состояние', dataIndex: 'status',flex:2,minWidth:30,
            renderer: function (val, metaData, rec) {
                switch (val) {
                    case "close" :
                        return "Закрыта"
                    case "open":
                        return "Открыта"
                }
            }
        },

        { text: 'Число работ', dataIndex:'worksCount', flex:2, minWidth:30},
        { text: 'Назначен', dataIndex:'userAssign', flex:2, minWidth:30}
    ],
    initComponent: function () {
        var self = this;
        var actionColumns={
            view : {
                xtype:'actioncolumn',
                width:20,
                icon: 'images/ico16_show.png',  // Use a URL in the icon config
                tooltip: 'Просмотр',
                handler: function(view, rowIndex, colIndex,item,e,record,row) {
                    self.showTicket(record)
                }
            },
            cancel:{
                xtype:'actioncolumn',
                width:20,
                icon: 'images/ico16_cancel.png',
                tooltip: 'Отказаться от заявки',
                handler: function (grid, rowIndex, colIndex,item,e,record,row) {
                    self.cancelTicket(record)
                }
            },
            apply:  {
                xtype:'actioncolumn',
                width:20,
                icon: 'images/ico16_okcrc.png',
                tooltip: 'Принять заявку',
                handler: function (grid, rowIndex, colIndex,item,e,record,row) {
                    self.acceptTicket(record)
                }
            },
            remove: {
                xtype:'actioncolumn',
                width:20,
                icon: 'images/ico16_crossinc.png',
                tooltip: 'Удалить заявку',
                handler: function (grid, rowIndex, colIndex,item,e,record,row) {
                    self.removeTicket(record)
                }
            },
            reopen:{
                xtype:'actioncolumn',
                width:20,
                icon: 'images/ico16_loading.png',
                tooltip: 'Открыть заного',
                handler: function (grid, rowIndex, colIndex,item,e,record,row) {
                    self.reopenTicket(record)
                }
            },
            unassign:{
                xtype:'actioncolumn',
                width:20,
                icon: 'images/ico16_user.png',
                tooltip: 'Снять назначение',
                handler: function (grid, rowIndex, colIndex,item,e,record,row) {
                    self. unassignTicket(record)
                }
            }
        }
        var columns=function(){
            var items=[]
            for(j in self.actions){
                console.log("action=",self.actions[j])
                items.push(actionColumns[self.actions[j]])
            }
            console.log("items",items)
            return items
        }
        var storeProps = {
            autoLoad: true,
            listeners: {
                beforeload: function (store, op) {
                    console.log("TicketsGrid storeExtraParam=", self.storeExtraParam);
                    if (self.storeExtraParam) {
                        for (var key in self.storeExtraParam)
                            store.getProxy().setExtraParam(key, self.storeExtraParam[key]);
                    }
                }
            }
        }
        Ext.apply(this, {
            store: Ext.create('EDS.Ext5.store.TicketsService', storeProps),
            columns:self.columns.concat(columns())
        });
        this.callParent();
    },
    listeners:{
        rowdblclick: function( view, record, tr, rowIndex, e, eOpts )  {
            console.log("rowdblclk")
            view.up('grid').showTicket(rowIndex)
        }
    },
//    viewConfig: {
//        plugins: {
//            //pluginId:'dndplug',
//            ptype: 'gridviewdragdrop',
//            ddGroup: 'Tickets',
//            enableDrop:false
//        },
//        listeners:{
//            beforedrop:function( node, data, overModel, dropPosition, dropHandlers, eOpts ){
//                console.log("drop", data);
////                var dropableRecs=new Array();
////                for(i in data.records){
////                    if(data.records[i].data.recordType=='account'){
////                        dropableRecs.push(data.records[i]);
////                    }
////                    else console.log(data.records[i].data," not account");
////                }
////                console.log("dropableRecs",dropableRecs);
////                if(dropableRecs.length==0)
////                    dropHandlers.cancelDrop();
////                else
////                {
////                    data.records=dropableRecs;
////                    dropHandlers.processDrop();
////                }
//            },
//            drop: function(node, data, overModel, dropPosition, eOpts ){
//                console.log("drop", data);
//                console.log("drop",data.records[0].data.recordType);
//            }
//        }
//    },
    showTicket:function(record){
        var self=this
        var mainPanel=self.up(self.mainPanelItemId)
        var layout = mainPanel.getLayout();
        var currentTicketPanel=mainPanel.down('#curTicketPanel')
        currentTicketPanel.readOnlyRule=self.readOnlyRule
        currentTicketPanel.loadData(record,self.itemId)
        layout.next()
    },
    cancelTicket:function(record){
        var self=this
        console.log("Отменить заявку",record)
        record.set("assignee","nobody",{silent:true})
        var data=record.getData()
        ticketsService.updateTicket(data,function(result){
            console.log("ticket updated")
            self.up("#ticketsPanel").refresh()
        })
    },
    acceptTicket:function(record){
        var self=this
        console.log("Принять заявку",record)
        record.set("assignee","current",{silent:true})
        var data=record.getData()
        ticketsService.updateTicket(data,function(result){
            console.log("ticket updated")
            self.up("#ticketsPanel").refresh()
        })
    },
    unassignTicket:function(record){
        var self=this
        console.log("Снять назначение",record)
        record.set("assignee",null,{silent:true})
        var data=record.getData()
        ticketsService.updateTicket(data,function(result){
            console.log("ticket updated")
            self.up("#ticketsPanel").refresh()
        })
    },
    removeTicket:function(record){
        var self=this
        console.log("Удалить заявку",record)
        var id=record.get("_id")
        ticketsService.removeTicket(id,function(result){
            console.log("ticket updated")
            self.up("#ticketsPanel").refresh()
        })
    },
    reopenTicket:function(record){
        var self=this
        console.log("Открыть заного заявку",record)
        record.set("status","open",{silent:true})
        var data=record.getData()
        ticketsService.updateTicket(data,function(result){
            console.log("ticket updated")
            self.up("#ticketsPanel").refresh()
        })
    }
});
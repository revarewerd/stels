/**
 * Created by IVAN on 02.03.2015.
 */
Ext.require([
    'Workflow.view.installer.CurrentTicketDataPanel'
])

Ext.define('Workflow.view.installer.CurrentTicketPanel', {
    extend: 'Ext.form.Panel',
    alias: "widget.currentticketpanel",
    title:'Текущая заявка',
    titleAlign:'center',
    region:'center',
    layout: 'border',
    items:[
    {
        xtype:"currentticketdesc",
        itemId:'curTicketDesc',
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
        xtype:"currentticketdatapanel",
        itemId:'curTicketDataPanel',
        region:'center'
    }
],
    loadData:function(rec,panelId) {
        var self = this
        console.log("panelId",panelId)
        self.enableActions(panelId)
        console.log("record", rec)
        var ticketDesc = self.down("#curTicketDesc");
        ticketDesc.loadRecord(rec);
        var ticketDataPanel=self.down("#curTicketDataPanel")
        ticketDataPanel.loadData(rec)
},
    enableActions:function(panelId){
        var self = this
        var acceptTicket=self.down("#acceptTicket")
        var saveTicket=self.down("#saveTicket")
        var closeTicket=self.down("#closeTicket")
        var saveAsComplete=self.down('#saveAsComplete')
        var saveAsNotComplete=self.down('#saveAsNotComplete')
        var actions=[acceptTicket,closeTicket,saveAsComplete,saveAsNotComplete]
        for(i in actions){
            actions[i].disable()
        }
        switch(panelId)
        {
            case "availableTickets":{
                acceptTicket.enable();
                break;
            }
            case "assignedTickets":{
                saveTicket.enable();
                closeTicket.enable();
                saveAsComplete.enable();
                saveAsNotComplete.enable();
                break;
            }
            case "completedTickets":{
                break;
            }
            default: {
                break;
            }
        }
    },
    reset:function(){
        var self = this
        var ticketDesc = self.down("#curTicketDesc");
        ticketDesc.reset();
        var ticketDataPanel=self.down("#curTicketDataPanel")
        ticketDataPanel.reset();

    }
})
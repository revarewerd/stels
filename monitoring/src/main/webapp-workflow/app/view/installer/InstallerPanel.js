/**
 * Created by IVAN on 26.11.2014.
 */
Ext.require([
    'Workflow.view.TicketsGrid',
    'Workflow.view.installer.CurrentTicketPanel',
    'Workflow.view.installer.CurrentTicketDescription'
])



Ext.define("Workflow.view.installer.InstallerPanel", {
    extend: 'Ext.panel.Panel',
    alias: "widget.instpanel",
    layout:'card',
    itemId:'installerPanel',
    items: [
        {
            xtype:'panel',
            title:'Заявки',
            itemId:'ticketsPanel',
            titleAlign:'center',
            width:"30%",
            height:"30%",
            border:true,
            layout: {
                type:'border'
            },
            tools:[
                {
                    type:'help',
                    itemId:'helpButton',
                    tooltip:'Выберите интересующую вас заявку и нажмите кнопку "Просмотр заявки"'
//                    callback:function(){
//                        Ext.Msg.alert('Справка', 'Выберите интересующую вас заявку и нажмите кнопку "Просмотр заявки"');
//                    }
                }
            ],
            dockedItems:[
                {
                    xtype:'toolbar',
                    ui:'footer',
                    items:[
                        {
                            xtype:'button',
                            text:'Обновить',
                            handler:function(btn){
                                manPanel=btn.up('panel').refresh()
                            }
                        }
                    ]
                }
            ],
            items:[
                {
                    margin:'5 5 0 5',
                    headerPosition:'left',
                    titleAlign:'center',
                    title:'Открытые',
                    border:true,
                    split:true,
                    collapseDirection:'top',
                    collapsible: true,
                    floatable:false,
                    titleCollapse:true,
                    //collapseMode:'mini',
                    region:'north',
                    height:'30%',
                    xtype:'ticketsgrid',
                    itemId:"availableTickets",
                    mainPanelItemId:"#installerPanel",
                    storeExtraParam:{
                        "status":"open",
                        "assign":"notassign"
                    },
                    actions:["view","apply"],
                    readOnlyRule:true

                },
                {
                    margin:'0 5 0 5',
                    title:'Мои',
                    headerPosition:'left',
                    titleAlign:'center',
                    border:true,
                    region:'center',
                    height:'40%',
                    xtype:'ticketsgrid',
                    itemId:"assignedTickets",
                    mainPanelItemId:"#installerPanel",
                    storeExtraParam:{
                        "status":"open",
                        "assign":"my"
                    },
                    actions:["view","cancel"],
                    readOnlyRule:false

                },
                {
                    margin:'0 5 5 5',
                    title:'Закрытые',
                    headerPosition:'left',
                    titleAlign:'center',
                    titleCollapse:true,
                    border:true,
                    split:true,
                    collapseDirection:'top',
                    collapsible: true,
                    floatable:false,
                    titleCollapse:true,
                    region:'south',
                    height:'30%',
                    xtype:'ticketsgrid',
                    itemId:"completedTickets",
                    mainPanelItemId:"#installerPanel",
                    storeExtraParam:{
                        "status":"close",
                        "assign":"my"
                    },
                    actions:["view","reopen"],
                    readOnlyRule:true
                }

            ],
            refresh:function(){
                console.log("Обновить");
                this.down("#availableTickets").getStore().load()
                this.down("#assignedTickets").getStore().load()
                this.down("#completedTickets").getStore().load()
            }
        },
        {
            xtype:"currentticketpanel",
            itemId:"curTicketPanel"
        }
    ]
})


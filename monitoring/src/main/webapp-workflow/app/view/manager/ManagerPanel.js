/**
 * Created by IVAN on 09.12.2014.
 */
Ext.require([
    'Workflow.view.TicketsGrid',
    'Workflow.view.manager.ManagerCurrentTicketPanel'
])



Ext.define("Workflow.view.manager.ManagerPanel", {
    extend: 'Ext.panel.Panel',
    alias: "widget.managerpanel",
    //layout:'border',
    layout:'card',
    itemId:'managerPanel',
    items: [
        {
            xtype:'panel',
            title:'Заявки',
            itemId:'ticketsPanel',
            titleAlign:'center',
            border:true,
            layout: {
                type:'border'
            },
            tools:[
                {
                    type:'help',
                    itemId:'helpButton',
                    tooltip: 'Выберите интересующую вас заявку и нажмите кнопку "Просмотр заявки"'
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
                            text:'Новая заявка',
                            handler:function(btn){
                                btn.up('#managerPanel').showTicket()
                            }
                        },
                        {
                            xtype:'button',
                            text:'Обновить',
                            handler:function(btn){
                                btn.up('panel').refresh()
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
                    region:'center',
                    height:'60%',
                    xtype:'ticketsgrid',
                    itemId:"openTicketsGrid",
                    storeExtraParam:{
                        "status":"open",
                        "assign":"any"
                    },
                    actions:['view','unassign','remove'],
                    mainPanelItemId:"#managerPanel"
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
                    //collapseMode:'mini',
                    region:'south',
                    height:'40%',
                    xtype:'ticketsgrid',
                    itemId:"closedTicketsGrid",
                    storeExtraParam:{
                        "status":"close",
                        "assign":"any"
                    },
                    actions:['view','reopen'],
                    mainPanelItemId:"#managerPanel"
                }
            ],
            refresh:function(){
                console.log("Обновить");
                this.down("#openTicketsGrid").getStore().load()
                this.down("#closedTicketsGrid").getStore().load()
            }
        },
        {
            xtype:'panel',
            title:'Текущая заявка',
            titleAlign:'center',
            //region:'center',
            layout: 'border',
            items:[
                {
                    xtype:"managerticketpanel",
                    region:'center'
                }
            ]
        }
    ],
    showTicket:function(){
        console.log("Новая заявка");
        var managerPanel=this;
        var layout = managerPanel.getLayout();
        var currentTicketForm=managerPanel.down('#curTicketPanel');
        var record=Ext.create('Ticket',{});
        currentTicketForm.loadData(record);
        layout.next();
    }
})
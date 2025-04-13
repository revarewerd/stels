   
Ext.define('Billing.view.equipment.EqAddGrid', {
 extend: 'WRExtUtils.WRGrid',
 alias: 'widget.eqaddgrid',
 selModel:{
        pruneRemoved:false,        
        mode:'MULTI'
 },
features: [
        {
            ftype: 'filters',
            encode: true,
            local: false
        }],  
 dockedToolbar: ['search'],
 setStore:false,
 //storeName:'EDS.store.ObjectsEquipmentService',
 loadBeforeActivated: true,
 initComponent: function () {
    var self=this;
    var accountId=self.accountId;
    var dragGroup='eqStoreDND2';
    var dropGroup='eqStoreDND1'  ; 
    if(accountId!=undefined)
    {dragGroup='eqStoreDND2'+accountId;
     dropGroup='eqStoreDND1'+accountId;
    }
    
    console.log('eqaddgrid accountId',accountId);
    var eqaddwnd=Ext.ComponentQuery.query('[xtype=eqaddwnd]');
 Ext.apply(this, {
 viewConfig: {
                        plugins: {
                            pluginId:'dndplug',
                            ptype: 'gridviewdragdrop',
                            dragGroup: dragGroup,
                            dropGroup: dropGroup
                            
                        },
                        listeners:{
                            beforedrop:function( node, data, overModel, dropPosition, dropHandlers, eOpts ){
                            console.log("beforedrop", data);  
                           },
                            drop: function(node, data, overModel, dropPosition, eOpts ){
                                console.log("drop", data);                               
                            }
                        }
},
       store:Ext.create('EDS.store.ObjectsEquipmentStoreService',{
                autoLoad: true,
                autoSync:false,
                listeners: {
                    beforeload: function (store, op) {                          
                        console.log('EqAddGrid beforeload');  
                        //console.log('self rec',self.up('panel').getRecord().get("uid")) 
                        //console.log('accountId',self.up('window').accountId)  
                        //store.getProxy().setExtraParam("uid", null);
                        //store.getProxy().setExtraParam("accountId", self.up('window').accountId);
                    },
                    load:function (store, op){
                        console.log('EqAddGrid load');
                        var eqsgrids=Ext.ComponentQuery.query('[xtype=equipmentsgrid]');
                        //console.log("eqpanels",eqsgrids)
                        for(i in eqsgrids)
                        {   console.log('eqsgrids',eqsgrids[i]);
                            eqsgrids[i].getSelectionModel().deselectAll();
                            eqsgrids[i].getStore().load();
                        }                        
                    }
                }
            }),            
        columns: [
    {
        header: 'Тип устройства',
        //width:170,
        flex: 2,
        sortable: true,
        dataIndex: "eqtype",
        filter: {
            type: 'string'
        }
    },    
    {
        header: 'Марка',
        //width:170,
        flex: 1,
        sortable: true,
        dataIndex: "eqMark",
        filter: {
            type: 'string'
        }
    },
    {
        header: 'Модель',
        //width:170,
        flex: 1,
        sortable: true,
        dataIndex: "eqModel",
        filter: {
            type: 'string'
        }        
     },
    {
        header: 'Серийный номер',
        //width:170,
        flex: 1,
        sortable: true,
        dataIndex: "eqSerNum",
        filter: {
            type: 'string'
        }        
     },
     {
        header: 'IMEI',
        //width:170,
        flex: 1,
        sortable: true,
        dataIndex: "eqIMEI",
        filter: {
            type: 'string'
        }        
     },
     {
        header: 'Абонентский номер',
        //width:170,
        flex: 1,
        sortable: true,
        dataIndex: "simNumber",
        filter: {
            type: 'string'
        }        
     },
     {
        header: 'Прошивка',
        //width:170,
        flex: 1,
        sortable: true,
        dataIndex: "eqFirmware",
        filter: {
            type: 'string'
        }        
    }],
listeners: {
            itemdblclick: function (/*Ext.view.View */self0, /*Ext.data.Model*/ record, /*HTMLElement*/ item, /*Number*/ index, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
                console.log("doubleclick");
                console.log("record",record);
                this.showEquipmentForm(record);
            }
        },
        showEquipmentForm: function(record){
            console.log('showEqForm');  
            console.log('record=',record);        
            var self=this;
            var eqtype=record.get('eqtype');
            if(eqtype=="") eqtype="Новое устройство"
            var existingWindow = WRExtUtils.createOrFocus('eqWnd' + record.get('_id'), 'Billing.view.equipment.EquipmentWindow', {
                title: eqtype+' "' + record.get('eqIMEI') + '"'
            });
            if (existingWindow.justCreated)
                {//existingWindow.down('eqform').loadRecord(record);               
                existingWindow.down('[itemId=eqPanel]').loadRecord(record);
                existingWindow.down('[itemId=eqPanel]').on('save', function (args) { 
                //self.refresh();
                console.log('args',args);
                })
                }
            existingWindow.show();
            return existingWindow;//.down('[itemId=eqPanel]');
        }
    })
 this.callParent()  
},
onSelectChange: function (selModel, selections) {
        console.log('selection changed');
    } 
})
Ext.define('Billing.view.equipment.EquipmentStoreWindow', {
    extend: 'Ext.window.Window',
    alias: 'widget.eqaddwnd',
    title: 'Главный склад',
    width: 900,
    height: 400,
    //constrainHeader: true,
    maximizable: true,
    closeAction: 'hide',
    layout: 'fit',
    initComponent: function () {
        var self=this 
        this.addEvents('create');
        this.on('close',function(){
            console.log('EqAddWindow close');
            var eqsgrids=Ext.ComponentQuery.query('[xtype=equipmentsgrid]');
            console.log("eqpanels",eqsgrids);
            for(i in eqsgrids)
            {   console.log('eqsgrids',eqsgrids[i]);
                 eqsgrids[i].getSelectionModel().deselectAll();
                 eqsgrids[i].getStore().load();
            }
            var eqpanels=Ext.ComponentQuery.query('[xtype=eqpanel]');
            for(i in eqpanels)
            {
                console.log('eqpanels',eqpanels[i]);
                    eqpanels[i].down('grid').getSelectionModel().deselectAll();
                    eqpanels[i].down('grid').getStore().load();
            }
        });
        Ext.apply(this, {    
    items:[{                        
            xtype: 'eqaddgrid'//,
            //accountId:self.accountId
}]
       });
    this.callParent();
}
})
Ext.define('Billing.view.equipment.AccEqStoreWindow', {
    extend: 'Ext.window.Window',
    alias: 'widget.acceqstorewnd',
    //title: 'Добавить устройство со склада',
    width: 900,
    height: 400,
    //constrainHeader: true,
    maximizable: true,
    //closeAction: 'hide',
    layout: 'fit',
    initComponent: function () {
        var self=this; 
        this.addEvents('create');
        this.on('close',function(){
            console.log('AccEqStoreWindow close');  
            var eqpanels=Ext.ComponentQuery.query('[xtype=eqpanel]');
            console.log("eqpanels",eqpanels);
            for(i in eqpanels)
            {
                console.log('eqpanels',eqpanels[i]);
                if (eqpanels[i].accountId==self.accountId)
                {
                    eqpanels[i].down('grid').getSelectionModel().deselectAll();
                    eqpanels[i].down('grid').getStore().load();
                //acceqstorewnd[i].close()
                //break;
                }
            }            
        })
        Ext.apply(this, {    
            items:[{
            xtype: 'eqaddgrid',
            accountId:self.accountId
            }]
        });
        this.callParent();
    }
})


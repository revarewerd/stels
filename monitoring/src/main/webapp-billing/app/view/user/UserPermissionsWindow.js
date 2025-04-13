Ext.define('Billing.view.user.UserPermissionsSelectionGrid', {   
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.userpermselgrid',
    header: false,       
    dockedToolbar: ['search'],
    searchStringWidth:90,
    searchFieldWidth:60,    
    storeName:'EDS.store.UserPermissionSelectionService',
    storeAutoSync: false,
    invalidateScrollerOnRefresh: false,
    columns: [
                {
                    header: 'Наименование',
                    flex: 1,
                    sortable: true,
                    dataIndex: 'name',
                    filter: {
                        type: 'string'
                    }
                }
            ] 
});

Ext.define('Billing.view.user.UserPermissionsPanel', {
    
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.userpermgrid',
    header: false,
    dockedToolbar: ['refresh','remove','fill','search'],     
    storeName:'EDS.store.UserPermissionsService',
    storeAutoSync: false, 
    initComponent: function () {

        var self = this;
        console.log("self.record",self.record)
        this.editing = Ext.create('Ext.grid.plugin.CellEditing', {
            clicksToEdit: 1
        });       

        Ext.apply(this, {
            plugins: [this.editing],
            columns: [
                {
                    header: 'Тип',
                    flex: 1,
                    sortable: true,
                    dataIndex: 'recordType',
                    filter: {
                        type: 'string'
                    }                    
                },
                {                    
                    header: 'Имя',
                    flex: 1,
                    sortable: true,
                    dataIndex: 'name',
                    filter: {
                        type: 'string'
                    }
                },
                {   xtype : 'checkcolumn',
                    flex: 1,
                    header: 'Просмотр',
                    stopSelection:false,
                    dataIndex : 'view',
                    listeners:{
                        checkchange:function(chccol, rowIndex, checked, eOpts){
                             self.onGropCheckChange('view',rowIndex,checked);     
                        }
                    }                    
                },
                {   xtype : 'checkcolumn', 
                    flex: 1,
                    header: 'Просмотр "спящих"', 
                    dataIndex : 'sleepersView',
                    stopSelection:false,
                    listeners:{
                        checkchange:function(chccol, rowIndex, checked, eOpts){                            
                             self.onGropCheckChange('sleepersView',rowIndex,checked);     
                        }
                    }               
                    
                },
                //{   xtype : 'checkcolumn',
                //    flex: 1,
                //    header: 'Управление',
                //    dataIndex : 'control',
                //    stopSelection:false,
                //    listeners:{
                //        checkchange:function(chccol, rowIndex, checked, eOpts){
                //            self.onGropCheckChange('control',rowIndex,checked);
                //        }
                //    }
                //
                //},
                {
                    xtype : 'checkcolumn',
                    flex: 1,
                    header: 'Блокировка',
                    dataIndex : 'block',
                    stopSelection:false,
                    listeners:{
                        checkchange:function(chccol, rowIndex, checked, eOpts){
                            self.onGropCheckChange('control',rowIndex,checked);
                        }
                    }
                },
                {
                    xtype : 'checkcolumn',
                    flex: 1,
                    header: 'Координаты',
                    dataIndex : 'getCoords',
                    stopSelection:false,
                    listeners:{
                        checkchange:function(chccol, rowIndex, checked, eOpts){
                            self.onGropCheckChange('control',rowIndex,checked);
                        }
                    }
                },
                {
                    xtype : 'checkcolumn',
                    flex: 1,
                    header: 'Перезагрузка',
                    dataIndex : 'restartTerminal',
                    stopSelection:false,
                    listeners:{
                        checkchange:function(chccol, rowIndex, checked, eOpts){
                            self.onGropCheckChange('control',rowIndex,checked);
                        }
                    }
                },
                {   xtype : 'checkcolumn',
                    flex: 1,
                    header: 'Просмотр св-в',
                    dataIndex : 'paramsView',
                    stopSelection:false,
                    listeners:{
                        checkchange:function(chccol, rowIndex, checked, eOpts){
                            self.onGropCheckChange('paramsView',rowIndex,checked);
                        }
                    }

                },
                {   xtype : 'checkcolumn',
                    flex: 1,
                    header: 'Изменеие св-в',
                    dataIndex : 'paramsEdit',
                    stopSelection:false,
                    listeners:{
                        checkchange:function(chccol, rowIndex, checked, eOpts){
                            self.onGropCheckChange('paramsEdit',rowIndex,checked);
                        }
                    }

                },
                {   xtype : 'checkcolumn',
                    flex: 1,
                    header: 'Топливо',
                    dataIndex : 'fuelSettings',
                    stopSelection:false,
                    listeners:{
                        checkchange:function(chccol, rowIndex, checked, eOpts){
                            self.onGropCheckChange('fuelSettings',rowIndex,checked);
                        }
                    }

                },
                {   xtype : 'checkcolumn',
                    flex: 1,
                    header: 'Датчики',
                    dataIndex : 'sensorsSettings',
                    stopSelection:false,
                    listeners:{
                        checkchange:function(chccol, rowIndex, checked, eOpts){
                            self.onGropCheckChange('sensorsSettings',rowIndex,checked);
                        }
                    }

                }
            ]
        });
        this.callParent();
        this.getSelectionModel().on('selectionchange', this.onSelectChange, this);
    },
    listeners: {
        celldblclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
            var self = this;
            var dataIndex = self.columnManager.columns[cellIndex].dataIndex;
            //var id = record.get('_id')
            var name = record.get('name')
            var recordType = record.get('recordType')
            var itemId= record.get("item_id")
            record.setId(itemId)
            switch (dataIndex) {
                case "name":
                    console.log('recordType= ',recordType);
                    console.log('itemId= ',itemId);
                    if(recordType=="account")
                        this.showAccountForm(record);
                    else
                        this.showObjectForm(record);
                    break;
                case "recordType":
                    console.log('recordType= ',recordType);
                    console.log('itemId= ',itemId);
                    if(recordType=="account")
                        this.showAccountForm(record)
                    else
                        this.showObjectForm(record);
                    break;
            }
        }
    },
    onGropCheckChange:function(columnName,rowIndex,checked){
        var selection = this.getView().getSelectionModel().getSelection();
                        console.log('selection',selection);                        
                        var flag=false;
                        for (i in selection) 
                        {console.log('rowIndex',rowIndex,'selection[i].index',selection[i].index);
                            if(selection[i].index==rowIndex){flag=true; break;}
                        }
                        console.log('flag',flag);
                        if(flag)
                        for(i in selection){
                            selection[i].set(columnName,checked);
                        }
    },
    refresh: function () {
        this.callParent();
          this.up('window').down('userpermselgrid[title="Аккаунты"]').getStore().load();
          this.up('window').down('userpermselgrid[title="Объекты"]').getStore().load();
    },
    onSelectChange: function (selModel, selections) {
        this.down('#delete').setDisabled(selections.length === 0);
    },

    onSync: function () {
        this.store.sync();
    },

    onDeleteClick: function () {
        var selection = this.getView().getSelectionModel().getSelection();
        var self=this;
        if (selection) {
            var store = this.store;
            //Ext.MessageBox.confirm('Удаление элемента', 'Вы уверены, что хотите забрать у пользователя права на учетную запись ' + selection.get("name"), function (button) {
            //   if (button === 'yes') {
            store.remove(selection);
            for(i in selection)      
            {
                if(selection[i].get('recordType')=='account')  
                    {console.log(selection[i].get('recordType'))                        
                    self.up('window').down('userpermselgrid[title="Аккаунты"]').getStore().insert(0,selection[i]);
                    }
                else
                    {console.log(selection[i].get('recordType'))
                    self.up('window').down('userpermselgrid[title="Объекты"]').getStore().insert(0,selection[i]);}
            } 
        }
    },
    evalIfPermitted:function(fun,record){
            var ad=rolesService.checkAdminRole(function(res){
                console.log("res=",res)
                if(res) {
                    fun(record)
                }
                else
                    Ext.MessageBox.show({
                        title: 'Произошла ошибка',
                        msg: "Недостаточно прав для выполнения операции",
                        icon: Ext.MessageBox.ERROR,
                        buttons: Ext.Msg.OK
                    });
            })
        },
    showObjectForm: function (record) {
        var self=this
        var existingWindow = WRExtUtils.createOrFocus('ObjectForm' + record.get('_id'), 'Billing.view.object.ObjectForm.Window', {
            title: 'Объект "'+record.get('name')+'"',
            hideRule:self.hideRule
            //accountId:record.get('account')
        });
        if (existingWindow.justCreated) {
            var newobjpanel=existingWindow.down('[itemId=objpanel]')
            newobjpanel.on('save', function (args) {
                self.refresh()
            })
            newobjpanel.loadRecord(record)
        }
        existingWindow.show();
        return  existingWindow.down('[itemId=objpanel]')
    },
    showAccountForm: function (record) {
        var self = this;
        var existingWindow = WRExtUtils.createOrFocus('accWnd' + record.get('_id'), 'Billing.view.account.AccountForm.Window', {
            title: 'Учетная запись "' + record.get('name') + '"',
            id: 'accWnd' + record.get('_id'),
            hideRule:self.hideRule
        });
        if (existingWindow.justCreated) {
            var newaccform = existingWindow.down('accform');
            newaccform.setActiveRecord(record);
//            newaccform.on('save', function () {
//                self.refresh();
//            });
        }
        existingWindow.show();

        return existingWindow.down('accform');
    }
});


Ext.define('Billing.view.user.UserPermissionsWindow', {
    extend: 'Ext.window.Window',
    alias: "widget.userpermwindow",
    title: 'Права',
    width: 800,
    height: 600,
    maximized:true,
    maximizable:true,
    layout: 'border',    
    initComponent: function () {
        var self = this;
        var group1 = this.id + 'group1'
        var group2 = this.id + 'group2'
        
        Ext.apply(this, {
            title: 'Права пользователя "' + self.userName+'"',
            items: [
            {
                xtype: 'tabpanel',
                title:'Добавить элемент',                
                width:200,
                collapsible:true,
                region:'west',
                split: true,
                items:[                 
                {
                    xtype:'userpermselgrid',                    
                    title:'Аккаунты',
                    userId:self.userId,
                    storeExtraParam:{
                     "userId":self.userId
                    },
                    viewConfig: {
                        plugins: {
                            pluginId:'dndplug',
                            ptype: 'gridviewdragdrop',
                            dragGroup: group1,
                            dropGroup: group2
                            
                        },
                        listeners:{
                            beforedrop:function( node, data, overModel, dropPosition, dropHandlers, eOpts ){
                            console.log("drop", data);                            
                            var dropableRecs=new Array();                         
                            for(i in data.records){
                                if(data.records[i].data.recordType=='account'){
                                    dropableRecs.push(data.records[i]);                                  
                                }
                                else console.log(data.records[i].data," not account");  
                            }
                            console.log("dropableRecs",dropableRecs);
                            if(dropableRecs.length==0)
                                dropHandlers.cancelDrop();
                            else
                                {
                                 data.records=dropableRecs;
                                 dropHandlers.processDrop();
                                }                               
                           },
                            drop: function(node, data, overModel, dropPosition, eOpts ){
                                console.log("drop", data);
                                console.log("drop",data.records[0].data.recordType);
                            }
                        }
                    }
                },
                {
                    xtype:'userpermselgrid',
                    title:'Объекты',                                       
                     storeExtraParam:{
                     "userId":self.userId,
                     "ItemType":'object'
                    },                    
                    viewConfig: {
                        plugins: {
                            ptype: 'gridviewdragdrop',
                            dragGroup: group1,
                            dropGroup: group2
                        },
                        listeners:{
                            beforedrop:function( node, data, overModel, dropPosition, dropHandlers, eOpts ){
                            console.log("drop", data);                            
                            var dropableRecs=new Array();                         
                            for(i in data.records){
                                if(data.records[i].data.recordType=='object'){
                                    dropableRecs.push(data.records[i]);                                  
                                }
                                else console.log(data.records[i].data," not object");  
                            }
                            console.log("dropableRecs",dropableRecs);
                            if(dropableRecs.length==0)
                                dropHandlers.cancelDrop();
                            else
                                {
                                 data.records=dropableRecs;
                                 dropHandlers.processDrop();
                                }                               
                           },
                            drop: function(node, data, overModel, dropPosition, eOpts ){
                                console.log("drop", data);
                                console.log("drop",data.records[0].data.recordType);
                            }
                        }
                    }
                }
            ]
            },
            {
                xtype: 'userpermgrid',
                userId: self.userId,
                hideRule:self.hideRule,
                storeExtraParam:{
                     "userId":self.userId
                    },
                region: 'center',
                viewConfig: {
                    plugins: {
                        ptype: 'gridviewdragdrop',
                        dragGroup: group2,
                        dropGroup: group1                        
                    },
                    listeners:{
                        beforedrop:function( node, data, overModel, dropPosition, dropHandlers, eOpts ){                                
                                console.log("beforedrop");                                
                                for(var i in data.records){                                    
                                   data.records[i].beginEdit();
                                   var itemId=data.records[i].get('item_id');
                                   //console.log(data.records[i].data)
                                   if(itemId == '')  data.records[i].set('item_id',data.records[i].get('_id'));                                                                
                                   data.records[i].set('userId',self.userId);                                  
                                   data.records[i].endEdit(false);                                  
                                } 
                        },
                        drop: function(node, data, overModel, dropPosition, eOpts ){
                            console.log("drop", data);                           
                            var permStore=self.down('userpermgrid').getStore();
                            console.log("permStore", permStore);
                            for(var i in data.records){
                                data.records[i].setDirty();
                                for(var j in permStore.removed){
                                    if(data.records[i]==permStore.removed[j]);
                                        {   console.log('not removed',permStore.removed.splice(j,1));
                                            }                                            
                                }
                            }
                            if (permStore.removed.length==0) permStore.removed=[];                            
                            console.log("drop", data.records[0].data.recordType);
                        }
                    }
                }
            }
            ],
            dockedItems: [
                {
                    xtype: 'toolbar',
                    dock: 'bottom',
                    ui: 'footer',
                    items: ['->', {
                        icon: 'images/ico16_okcrc.png',
                        itemId: 'save',
                        text: 'Сохранить',
                        //scope: this,
                        handler: function(){
                            console.log('self',self.down('userpermgrid'));
                            console.log('data to remove',self.down('userpermgrid').getStore().getRemovedRecords( ));
                            console.log('data to update',self.down('userpermgrid').getStore().getUpdatedRecords( ));
                            console.log('modified data ',self.down('userpermgrid').getStore().getModifiedRecords( ));                           
                            self.down('userpermgrid').onSync();
                            self.close();
                        }
                    }, {
                        icon: 'images/ico16_cancel.png',
                        text: 'Отменить',
                        scope: this,
                        handler: function () {
                            self.close();
                        }
                    }
                    ]
                }
            ]
            
        });
        this.callParent();
    }
});

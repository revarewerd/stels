
Ext.define('Billing.view.user.PermissionsSelectionGrid', {     
     extend: 'WRExtUtils.WRGrid',
     alias: 'widget.permselgrid',  
     storeName:'EDS.store.PermittedItemsService',
     storeAutoSync: false,
     dockedToolbar: ['fill','search'],
     searchStringWidth:100,
     searchFieldWidth:85,     
     columns: [
    {
        header: 'Имя',
        flex: 1,
        sortable: true,
        dataIndex: 'name',
        filter: {
            type: 'string'
        }
    },
    {
        header: 'Комментарий',
        flex: 1,
        sortable: true,
        dataIndex: 'comment',
        filter: {
            type: 'string'
        }
    }
    ]
});
Ext.define('Billing.view.user.UsersWindow.PermissionsPanel', {    
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.permgrid',
    title: 'Пользователи',
    storeName:'EDS.store.PermittedItemsService',
    storeAutoSync: false,
    dockedToolbar: ['remove','refresh','fill','search'],    
    initComponent: function() {
    Ext.apply(this, {
        plugins: [{ptype: 'cellediting', clicksToEdit: 1}],
        columns: [
                {
                    header: 'Имя',
                    flex: 2,
                    sortable: true,
                    dataIndex: 'name',
                    filter: {
                        type: 'string'
                    }
                },
                //{
                //    header: 'Комментарий',
                //    flex: 2,
                //    sortable: true,
                //    dataIndex: 'comment',
                //    filter: {
                //        type: 'string'
                //    }
                //},
                {xtype: 'checkcolumn',
                    flex: 2,
                    header: 'Просмотр',
                    dataIndex: 'view'
                },
                {xtype: 'checkcolumn',
                    flex: 3,
                    header: 'Просмотр "спящих"',
                    dataIndex: 'sleepersView'
                },
            {
                xtype: 'checkcolumn',
                flex: 2,
                header: 'Блокировка',
                dataIndex: 'block'
            },
            {
                xtype: 'checkcolumn',
                flex: 2,
                header: 'Координаты',
                dataIndex: 'getCoords'
            },
            {
                xtype: 'checkcolumn',
                flex: 2,
                header: 'Перезагрузка',
                dataIndex: 'restartTerminal'
            },
            {   xtype : 'checkcolumn',
                flex: 3,
                header: 'Просмотр св-в',
                dataIndex : 'paramsView'
            },
            {   xtype : 'checkcolumn',
                flex: 3,
                header: 'Изменеие св-в',
                dataIndex : 'paramsEdit'
            },
            {   xtype : 'checkcolumn',
                flex: 2,
                header: 'Топливо',
                dataIndex : 'fuelSettings'
            },
            {   xtype: 'checkcolumn',
                flex: 2,
                header: 'Датчики',
                dataIndex: 'sensorsSettings'
            },
                {
                    header: 'Войти',
                    width: 140,
                    dataIndex: 'name',
                    sortable: false,
                    align: 'left',
                    renderer: function(value) {
                        return '<a href="EDS/monitoringbackdoor?login=' + value + '" target="slavemonitoring"> Войти как пользователь </a>';
                    }
                },
                {
                    xtype: 'actioncolumn',
                    width: 25,
                    dataIndex: 'name',
                    sortable: false,
                    align: 'center',
                    items: [
                        {icon: 'extjs-4.2.1/examples/shared/icons/fam/cog_edit.png',
                            tooltip: 'Редактировать права пользователя',
                            //iconCls: 'mousepointer',
                            handler: function(grid, rowIndex, colIndex) {
                                var rec = grid.getStore().getAt(rowIndex);
                                console.log("REC", rec);
                                if (rec.get("_id")) {
                                    WRExtUtils.createOrFocus('userPermWnd' + rec.get("_id"), 'Billing.view.user.UserPermissionsWindow', {
                                        userId: rec.get("_id"),
                                        userName: rec.get('name')
                                    }).show();
                                }
                                else {
                                    Ext.MessageBox.show({
                                        title: 'Пользователь ещё не создан',
                                        msg: 'Сохраните пользователя перед тем как назначать ему права',
                                        buttons: Ext.MessageBox.OK,
                                        icon: Ext.MessageBox.WARNING
                                    });
                                }
                            }
                        }
                    ]
                }
            ]
        })
        this.callParent();
        this.getSelectionModel().on('selectionchange', this.onSelectChange, this);
    },
    onSelectChange: function(selModel, selections) {
        var allInherited = true;
        for (i in selections) {
            if (!selections[i].get("inherited")) {
                console.log("inherited", selections[i].get("inherited"));
                allInherited = false;
            }
        }
        this.down('#delete').setDisabled(selections.length === 0 || allInherited);
    },
    onDeleteClick: function(comp) {
        var selection = this.getView().getSelectionModel().getSelection();
        var self = this;
        if (selection) {
            var store = this.store;
            store.remove(selection);
            self.up('window').down('permselgrid').getStore().insert(0, selection);
        }
    },
    refresh: function() {
        this.callParent();
        this.getSelectionModel().deselectAll()
        var permselgrid=this.up('window').down('permselgrid');        
        permselgrid.refresh();
        permselgrid.getSelectionModel().deselectAll()
    }
});

Ext.define('Billing.view.user.UsersWindow', {
    extend: 'Ext.window.Window',    
    alias: 'widget.userswnd',
    closable: true,    
    maximizable: true,
    maximized:true,
    width: 1200,
    height: 600,    
    layout: 'border',

    initComponent: function () {
        var self=this;
        var group1 = this.id + 'group1';
        var group2 = this.id + 'group2';
        Ext.tip.QuickTipManager.init();
        Ext.apply(this,
            {
                items: [
                    {
                    title:'Добавить пользователя',
                    region:'west',
                    collapsible:true,
                    split: true,
                    xtype: 'permselgrid',
                    storeExtraParam:{
                     "oid":this.oid,
                     "type":this.type,
                     "permitted":false
                    },
                    width:240, 
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
                                    if(!data.records[i].get('inherited')){                                        
                                        dropableRecs.push(data.records[i]);                                  
                                    }
                                    else console.log("can't remove inherired");  
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
                    {   region:'center',
                        xtype: 'permgrid',
                        storeExtraParam:{
                            "oid":this.oid,
                            "type":this.type,
                            "permitted":true
                        },
                        viewConfig: {
                            plugins: {
                                ptype: 'gridviewdragdrop',
                                dragGroup: group2,
                                dropGroup: group1                        
                            },
                            listeners:{
                                beforedrop:function( node, data, overModel, dropPosition, dropHandlers, eOpts ){                                
                                        console.log("beforedrop");                               
                                },
                                drop: function(node, data, overModel, dropPosition, eOpts ){
                                    console.log("drop", data);                            
                                    var permStore=self.down('permgrid').getStore();
                                    console.log("permStore", permStore);
                                    for(var i in data.records){
                                        data.records[i].setDirty();
                                        for(var j in permStore.removed){
                                            if(data.records[i]==permStore.removed[j])
                                                {   console.log('not removed',permStore.removed.splice(j,1));
                                                    }                                            
                                        }
                                    }
                                    if (permStore.removed.length==0) permStore.removed=[];                            
                                    console.log("drop", data.records[0].data.recordType);
                                }
                            },
                        getRowClass: function(record, rowIndex, rowParams, store){
                            console.log("GET ROW CLASS");
                            console.log("inherited", record.get("inherited"));
                            return record.get("inherited") == true ? 'grayedText' : ' ';
                        }
                        }
                    }
                ],                
                dockedItems: [
                    {
                        xtype: 'toolbar',
                        dock: 'bottom',
                        ui: 'footer',
                        items: ['->', 
                        {
                            icon: 'images/ico16_checkall.png',
                            itemId: 'save',
                            text: 'Сохранить',                            
                            handler: function(){
                                self.onSave();                                
                            }
                        },
                        {
                        icon: 'images/ico16_okcrc.png.png',
                        itemId: 'savenclose',
                        text: 'Сохранить и закрыть',                        
                        handler: function () {
                                self.onSave(/*close*/true);                             
                            }
                        },
                        , {
                            icon: 'images/ico16_cancel.png',
                            text: 'Отменить',
                            scope: this,
                            handler: function () {
                                self.close();
                            }
                        }
                        ]
                    }
                ],
                 onSave:function(close){                    
                    console.log('data',self.down('permgrid').getStore().data);
                    var recsToRemove=self.down('permgrid').getStore().getRemovedRecords( );
                    var recsToUpdate=self.down('permgrid').getStore().getUpdatedRecords( );
                    var dataToRemove=new Array(); 
                    var dataToUpdate=new Array(); 
                    for(i in recsToRemove) {
                        dataToRemove.push(recsToRemove[i].data);
                    }
                    for(i in recsToUpdate) {
                        dataToUpdate.push(recsToUpdate[i].data);
                    }
                    console.log('data to remove',dataToRemove);                                
                    console.log('data to update',dataToUpdate);                                
                    usersPermissionsService.providePermissions(dataToUpdate,dataToRemove,self.type,self.oid,function(){
                        console.log('Данные сохранены');
                        if(close)
                            {self.close();}
                        else
                            {self.down('permgrid').refresh();}            
                    }); 
                }
            });
        this.callParent();
    }
});

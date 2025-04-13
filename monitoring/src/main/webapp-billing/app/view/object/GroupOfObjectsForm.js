/**
 * Created by IVAN on 01.06.2015.
 */
Ext.define('Billing.view.object.GroupOfObjectsForm', {
    extend: 'Ext.form.Panel',
    alias: 'widget.objgroupform',
    requires: [
    ],
    initComponent: function () {
        this.addEvents('create');
        var self = this;
        var group1 = this.id + 'group1'
        var group2 = this.id + 'group2'
        Ext.apply(this, {
            layout:'border',
            items: [
                {
                    region: 'north',
                    xtype: 'form',
                    layout: {
                        type: 'hbox',
                        align: 'stretchmax'
                    },
                    defaultType: 'textfield',

                    fieldDefaults: {
                        margin: '10 15 10 15'
                    },
                    items: [
                        {
                            fieldLabel: 'Группа объектов',
                            name: 'name'
                            //readOnly: this.hideRule
                        },
                        {
                            xtype: 'combobox',
                            fieldLabel: 'Пользователь',
                            name: 'uid',
                            store: Ext.create('EDS.store.UsersService'),
                            valueField: '_id',
                            displayField: 'name',
//                            typeAhead: true,
                            forceSelection: true,
                            minChars: 0,
                            allowBlank: false,
                            readOnly:this.hideRule,
                            listeners:{
                                change:function( combo, newValue, oldValue, eOpts ){
                                    if(combo.getStore().indexOfId(newValue)!=-1){
                                        var objselgridStore=self.down('objselgrid').getStore()
                                        var objgroupgridStore=self.down('objgroupgrid').getStore()
                                        if(oldValue!=null)objgroupgridStore.removeAll();
                                        objselgridStore.getProxy().setExtraParam("uid",newValue)
                                        objselgridStore.load()
                                    }
                                }
                            }
                        },
                    ]
                },
                {
                    region:'center',
                    xtype:'objgroupgrid',
                    groupId:self.groupId,
                    viewConfig: {
                        plugins: {
                            ptype: 'gridviewdragdrop',
                            dragGroup: group1,
                            dropGroup: group2
                        },
                        listeners:{
                            beforedrop:function( node, data, overModel, dropPosition, dropHandlers, eOpts ){
                                console.log("beforedrop", data)
                                var store=self.down("objgroupgrid").getStore();
                                var dropableRecs=new Array();
                                for(i in data.records){
                                    var dropedId=data.records[i].getId()
                                    if(store.indexOfId(dropedId)==-1){
                                        dropableRecs.push(data.records[i]);
                                    }
                                    else console.log(data.records[i].data," already exists");
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
                                var store=self.down("objgroupgrid").getStore();
                                store.sort('name','ASC')
                            }
                        }
                    }

                },
                {
                    region:"east",
                    width:300,
                    xtype:'objselgrid',
                    viewConfig: {
                        plugins: {
                            ptype: 'gridviewdragdrop',
                            dragGroup: group2,
                            dropGroup: group1
                        },
                        listeners:{
                            drop: function(node, data, overModel, dropPosition, eOpts ){
                                var store=self.down("objselgrid").getStore();
                                store.sort('name','ASC')
                            }
                        }
                    }
                }
            ]
        });
        this.callParent();
    }
})
Ext.define('Billing.view.object.GroupOfObjectsForm.Window', {
    extend: 'Ext.window.Window',
    alias: 'widget.objwnd',
    title: 'Группа объектов',
    minWidth:600,
    minHeight:400,
    width: 800,
    height: 600,
    maximizable: true,
    layout: 'fit',
    initComponent: function () {
        var self = this;
        Ext.apply(this, {
            items: [
                {
                    xtype: 'objgroupform',
                    groupId:self.groupId
                }
            ],
            dockedItems: [
                {
                    xtype: 'toolbar',
                    dock: 'bottom',
                    ui: 'footer',
                    items: [
                        '->',
                        {
                            icon: 'images/ico16_okcrc.png',
                            itemId: 'save',
                            disabled: self.hideRule,
                            text: 'Сохранить',
                            handler: function () {
                                self.onSave();
                            }
                        },
                        {
                            icon: 'images/ico16_cancel.png',
                            text: 'Отменить',
                            handler: function () {
                                self.close();
                            }
                        }]
                }
            ]
        });
        this.callParent();
    },
    onSave:function(){
        console.log("Сохранить");
        var self=this;
        var name=self.down('[name=name]').getValue()
        var uid=self.down('[name=uid]').getValue()
        var recId= self.down('form').getRecord().getId()
        var updateData={}
        console.log("recId",recId)
        if(recId!=null) updateData._id=recId
        updateData.uid=uid;
        updateData.name=name;
        var updatedObjects=self.down('objgroupgrid').getStore().getRange()
        var objects=new Array();
        for(var i in updatedObjects){
            objects.push({"uid":updatedObjects[i].get("uid")})
        }
        updateData.objects=objects;
        console.log("updateData=",updateData);
        groupsOfObjects.update(updateData, function (r, e) {
            if (!e.status) {
                Ext.MessageBox.show({
                    title: 'Произошла ошибка',
                    msg: e.message,
                    icon: Ext.MessageBox.ERROR,
                    buttons: Ext.Msg.OK
                });
            }
            else {
                self.fireEvent("onSave");
                self.close();
            }
        })
    },
    onLoad:function(record){
        var objgroupform=this.down("objgroupform");
        //var objgroupgrid=this.down("objgroupgrid");
        //var store=objgroupgrid.getStore();
        //store.loadData(record);
        objgroupform.loadRecord(record);

        var combo=objgroupform.down('[name=uid]')
        combo.getStore().load()
        combo.setValue(record.get('uid'))
    }
})

Ext.define('Billing.view.object.ObjectsSelectionGrid', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.objselgrid',
    //header: false,
    title:"Добавить объект",
    collapsible:true,
    split: true,
    floatable:false,
    titleCollapse:true,
    //collapsed:true,
    dockedToolbar: ['search'],
    searchStringWidth:120,
    searchFieldWidth:120,
    plugins: 'bufferedrenderer',
    //storeName:'EDS.store.PermittedObjectsStore',
    storeAutoSync: false,
    invalidateScrollerOnRefresh: false,
    setStore:false,
    initComponent: function () {
        var self=this
        Ext.apply(this, {
            store:Ext.create('EDS.store.PermittedObjectsStore',{
                autoSync:false,
                listeners:{
                    'load':function( store, records, successful, eOpts ){
                        var objGroupStore=self.up("window").down('objgroupgrid').getStore()
                        var recordsToRemove=new Array();
                        for (var i in records){
                           if(objGroupStore.indexOf(records[i])>-1)
                               recordsToRemove.push(records[i])
                        }
                        if(recordsToRemove.length>0) store.remove(recordsToRemove)
                    }
                }
            })
        })
        this.callParent();

    },
    features: [
        {
            ftype: 'summary',
            dock: 'top'
        }
    ],
    columns: [
        {
            header: 'Наименование',
            flex: 1,
            sortable: true,
            dataIndex: 'name',
            filter: {
                type: 'string'
            }
        },
        {
            header: 'Пользовательское имя',
            flex: 1,
            sortable: true,
            dataIndex: 'customName',
            filter: {
                type: 'string'
            }
        },
        {
            xtype: 'actioncolumn',
            width: 20,
            menuText: 'Добавить объект',
            items: [
                {
                    icon: 'images/ico16_plus_def.png',
                    handler: function (view, rowIndex, colIndex, item, e, rec) {
                        var objSelStore=view.getStore();
                        var objGroupStore=view.up('window').down("objgroupgrid").getStore();
                        if(objGroupStore.indexOf(rec)==-1){
                            objGroupStore.add(rec);
                            objSelStore.remove(rec);
                            objGroupStore.sort('name','ASC');
                        }

                    }
                }
            ]
        }
    ]
});

Ext.define('Billing.view.object.ObjectsGroupGrid', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.objgroupgrid',
    //header: false,
    title:"Объекты в группе",
    dockedToolbar: ['search'],
    features: [
        {
            ftype: 'summary',
            dock: 'top'
        }
    ],
    searchStringWidth:120,
    searchFieldWidth:120,
    storeName:'EDS.store.ObjectsGroupStore',
    //plugins: 'bufferedrenderer',
    storeAutoSync: false,
    invalidateScrollerOnRefresh: false,
    initComponent: function () {
        var self=this
        console.log("groupId",self.groupId)
        Ext.apply(this, {
            storeExtraParam:{
                groupId:self.groupId
            }
        })
        this.callParent();

    },
    columns: [
        {
            header: '№',
            xtype: 'rownumberer',
            width: 40,
            resizable: true
        },
        {
            header: 'Имя',
            //width: 170,
            flex:1,
            sortable: true,
            dataIndex: 'name',
            filter: {
                type: 'string'
            },
            renderer: function (val, metaData, rec) {
                metaData.tdAttr = 'style="cursor: pointer !important;" title="' + val + '"';
                return val
            },
            summaryType: 'count',
            summaryRenderer: function (value, summaryData, dataIndex) {
                return Ext.String.format('<b>Всего позиций: {0} </b>', value);
            }
        },
        {
            header: 'Пользовательское имя',
            width: 170,
            sortable: true,
            dataIndex: 'customName',
            filter: {
                type: 'string'
            }
        },
        {
            xtype: 'actioncolumn',
            width: 20,
            menuText: 'Удалить объект',
            items: [
                {
                    icon: 'images/ico16_signno.png',
                    handler: function (view, rowIndex, colIndex, item, e, rec) {
                        var objGroupStore=view.getStore();
                        var objSelStore=view.up('window').down("objselgrid").getStore();
                        if(objSelStore.indexOf(rec)==-1){
                            objSelStore.add(rec);
                            objGroupStore.remove(rec);
                            objSelStore.sort('name','ASC');
                        }
                    }
                }
            ]
        }
    ]
});

Ext.define('ObjectsGroup', {
    extend: 'Ext.data.Model',
    fields: [
        "_id","name", "uid","userName", "objects","objectsNames"
    ],
    idProperty: '_id'
});
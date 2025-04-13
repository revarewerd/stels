Ext.define('Billing.view.equipment.EquipmentStorePanel', {
    //extend:'Ext.grid.Panel',
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.eqstorepanel',
    title: 'Оборудование',
    requires: [
        'Ext.grid.plugin.CellEditing',
        'Ext.form.field.Text',
        'Ext.toolbar.TextItem',
        'Billing.view.equipment.EquipmentForm'//,
    //   'Billing.view.equipment.EquipmentHistoryWindow'
    //    'Ext.ux.grid.FiltersFeature'
    ],
    viewPermit:false,
    itemType:'equipments',
    dockedToolbar: ['add', 'remove', 'refresh', 'search','fill','gridDataExport'],
    features: [
        {
            ftype: 'filters',
            encode: true,
            local: false
        },
        {
            ftype: 'summary',
            dock:'top'
        }
    ],
    plugins: 'bufferedrenderer',
    invalidateScrollerOnRefresh: false,
//    viewConfig: {
//        trackOver: false
//    },
     storeName: 'EDS.store.EquipmentStoreService',
    initComponent: function () {
        var self = this;
        var hideRule=!self.viewPermit;
        var eqaddwnd=Ext.ComponentQuery.query('[xtype=eqaddwnd]')
        if(eqaddwnd.length==0) {
            eqaddwnd[0]=WRExtUtils.createOrFocus('eqAddWnd', 'Billing.view.equipment.EquipmentStoreWindow', {});
        }
        Ext.apply(this, {
     columns: [
    {
        hideable: false,
        menuDisabled: true,
        sortable: false,
        flex:1
    },
    {
        header: '№',
        xtype: 'rownumberer',
        width:40,
        resizable:true
    },
         {
             menuDisabled: true,
             sortable: false,
             xtype: 'actioncolumn',
             itemId: 'eq',
             width: 20,
             resizable: false,
             items: [
                 {
                     icon: 'images/ico16_device_def.png',
                 }
             ]
         },
    {
        header: 'Тип устройства',
        width:220,
        //flex: 1,
        sortable: true,
        dataIndex: "eqtype",
        filter: {
            type: 'string'
        },
        renderer: function(val, metaData, rec){
                metaData.tdAttr = 'style="cursor: pointer !important;"';
                return val
        },
        summaryType: 'count',
             summaryRenderer: function(value, summaryData, dataIndex) {
                    return Ext.String.format('<b>Всего позиций: {0} </b>', value); 
             }
    },
    {
        header: 'Учетная запись',
        width:170,
        //flex: 2,
        sortable: true,
        dataIndex: "accountName",
        filter: {
            type: 'string'
        },
        renderer: function (val, metaData, rec) {
                        metaData.tdAttr='title="'+val+'"'
                    return val
                    }
    },
     {
        header: 'Объект',
        width:170,
        //flex: 2,
        sortable: true,
        dataIndex: "objectName",
        filter: {
            type: 'string'
        },
        renderer: function (val, metaData, rec) {
                        metaData.tdAttr='title="'+val+'"'
                    return val
                    }
    },
    {
        header: 'Марка',
        width:110,
        //flex: 1,
        sortable: true,
        dataIndex: "eqMark",
        filter: {
            type: 'string'
        }
    },
    {
        header: 'Модель',
        width:110,
        //flex: 1,
        sortable: true,
        dataIndex: "eqModel",
        filter: {
            type: 'string'
        }        
     },
    {
        header: 'Серийный номер',
        width:110,
        //flex: 1,
        sortable: true,
        dataIndex: "eqSerNum",
        filter: {
            type: 'string'
        }        
     },
     {
        header: 'IMEI',
        width:110,
        //flex: 1,
        sortable: true,
        dataIndex: "eqIMEI",
        filter: {
            type: 'string'
        }        
     },
     {
        header: 'Абонентский номер',
        hidden:hideRule,
        width:110,
        //flex: 1,
        sortable: true,
        dataIndex: "simNumber",
        filter: {
            type: 'string'
        }        
     },
     {
        header: 'Прошивка',
        width:170,
        //flex: 1,
        sortable: true,
        dataIndex: "eqFirmware",
        filter: {
            type: 'string'
        }        
     },
     {
        xtype:'actioncolumn',
        hidden:hideRule,
        header: 'История',
        width: 60,
        dataIndex: 'name',
        sortable:false,
        align: 'center',        
        items:[
            {icon: 'images/ico16_eventsmsgs.png',
                disabled:hideRule,
                tooltip: 'Посмотреть историю оборудования',
                handler: function (grid, rowIndex, colIndex) {
                    var id = grid.getStore().getAt(rowIndex).get("_id");
                    if (id) {
                        var existingWindow = WRExtUtils.createOrFocus('EquipmentEventsPanel' + id, 'Billing.view.event.EventsWindow', {
                            aggregateId: id,
                            aggregateType: "EquipmentAggregate"
                        });
                        existingWindow.show();
                        console.log("existingWindow", existingWindow)
                    }
                }
            }
        ]
    },
    {
        hideable: false,
        menuDisabled: true,
        sortable: false,
        flex:1
    }],
    onSelectChange: function (selModel, selections) {
        if(hideRule)
            this.down('#delete').setDisabled(true);
        else
            this.down('#delete').setDisabled(selections.length === 0);
    }

        });
        this.callParent();
        //this.getSelectionModel().on('selectionchange', this.onSelectChange, this);
        //this.down('#delete').setDisabled(hideRule)
        this.down('#add').setDisabled(hideRule)
    },
    listeners: {
        cellclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
            var self=this;
            var itemId=self.columnManager.columns[cellIndex].itemId;
            if (itemId == "eq") {
                this.showEquipmentForm(record)
            }
        },
        celldblclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
            var self=this
            var dataIndex=self.columnManager.columns[cellIndex].dataIndex
            console.log("celldblclick dataIndex=", dataIndex);            
            switch(dataIndex)
            {
                 case "eqtype":
                    this.showEquipmentForm(record)
                    break;
            }
        }
    },
    onSelectChange: function (selModel, selections) {
        this.down('#delete').setDisabled(selections.length === 0);
    },
     onAddClick: function(){
        var rec=Ext.create('Equipment',{});
        //rec.set("objectName","на складе")
        var eqWnd = this.showEquipmentForm(rec);        
        eqWnd.down("[name=objectName]").setVisible(false)
        //eqWnd.down("[name=accountName]").setVisible(false)
    },
     onDeleteClick: function () {
        var selection = this.getView().getSelectionModel().getSelection();//[0];

        if (selection) {
            var store = this.store
            Ext.MessageBox.confirm('Удаление элемента', 'Вы уверены, что хотите удалить ' + selection.length + ' элементов?', function (button) {
                if (button === 'yes') {
                    store.remove(selection);
                    var eqaddwnd=Ext.ComponentQuery.query('[xtype=eqaddwnd]')
                    eqaddwnd[0].down('grid').getStore().remove(selection)
                }
            });
        }
    },
    showEquipmentForm: function(record){
        console.log('showEqForm');  
        console.log('record=',record);        
        var self=this;
       var eqtype=record.get('eqtype')
       if(eqtype=="") eqtype="Новое устройство"
       var existingWindow = WRExtUtils.createOrFocus('eqWnd' + record.get('_id'), 'Billing.view.equipment.EquipmentWindow', {
            title: eqtype+' "' + record.get('eqIMEI') + '"',
            hideRule:!self.viewPermit
        });
        if (existingWindow.justCreated)
            {
             var eqPanel=existingWindow.down('[itemId=eqPanel]')
                eqPanel.loadRecord(record);
                eqPanel.on('save', function (args) {
                //self.refresh()
                self.getSelectionModel().deselectAll()
                console.log('args',args) 
                })
            }
        existingWindow.show();
        return existingWindow//.down('[itemId=eqPanel]');
    },
    updateData:function(data){
        this.changeData(data,"equipment")
    }
   })




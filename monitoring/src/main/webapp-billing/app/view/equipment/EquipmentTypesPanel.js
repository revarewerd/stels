/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
Ext.define('Billing.view.equipment.EquipmentTypesPanel', {
    //extend:'Ext.grid.Panel',
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.eqtypesgrid',
    title: 'Типы оборудования',
    requires: [
        'Ext.grid.plugin.CellEditing',
        'Ext.form.field.Text',
        'Ext.toolbar.TextItem',
        'Billing.view.equipment.EquipmentTypesForm'
    //    'Ext.ux.grid.FiltersFeature'
    ],
    features: [
        {
            ftype: 'filters',
            encode: true,
            local: false
        },
        {
            ftype: 'summary',dock:'top'
        }        
    ],
    invalidateScrollerOnRefresh: false,
    viewConfig: {
        trackOver: false
    },
     storeName: 'EDS.store.EquipmentTypesService',
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
        header: 'Тип устройства',
        //width:170,
        flex: 1,
        sortable: true,
        dataIndex: 'type',
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
        header: 'Марка',
        width:170,
        //flex: 1,
        sortable: true,
        dataIndex: 'mark',
        filter: {
            type: 'string'
        }
    },
    {
        header: 'Модель',
        width:170,
        //flex: 1,
        sortable: true,
        dataIndex: 'model',
        filter: {
            type: 'string'
        }        
     },
    {
        header: 'Сервер',
        width:170,
        //flex: 1,
        sortable: true,
        dataIndex: 'server',
        filter: {
            type: 'string'
        }        
     },
     {
        header: 'Порт',
        width:170,
        //flex: 1,
        sortable: true,
        dataIndex: 'port',
        filter: {
            type: 'string'
        }        
     },
    {
        hideable: false,
        menuDisabled: true,
        sortable: false,
        flex:1
    }],
    listeners: {
        celldblclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
             var self=this;
            var dataIndex=self.columnManager.columns[cellIndex].dataIndex;
            console.log("celldblclick dataIndex=", dataIndex);            
            switch(dataIndex)
            {
                case "type":                 
                this.showEquipmentTypesForm(record);
                break;
            }
        }
    },
    onSelectChange: function (selModel, selections) {
        this.down('#delete').setDisabled(selections.length === 0);
    },
     onAddClick: function(){
        var rec=Ext.create('EqType',{});               
        var form = this.showEquipmentTypesForm(rec);
    },
     onDeleteClick: function () {
        var selection = this.getView().getSelectionModel().getSelection();//[0];

        if (selection) {
            var store = this.store;
            Ext.MessageBox.confirm('Удаление элемента', 'Вы уверены, что хотите удалить ' + selection.length + ' элементов?', function (button) {
                if (button === 'yes') {
                    store.remove(selection);
                }
            });
        }
    },
    showEquipmentTypesForm: function(record){
        console.log('showEqTypesForm');  
        console.log('record=',record);        
        var self=this;
        if(record)
            {var id=record.get('_id');
             var type=record.get('type');
             var mark=record.get('mark');
            }
        var existingWindow = Ext.getCmp('EquipmentTypesForm' + id);
        if (!existingWindow) {
            console.log('!existingWindow');
            var wnd = Ext.create('Billing.view.equipment.EquipmentTypesForm.Window',{                
                        title: 'Тип оборудования "'+type+" "+mark+'"',
                        id: 'EquipmentTypesForm' + id                        
            });                      
            var newpanel=wnd.down('[itemId=eqtypes]');
            if(record) newpanel.loadRecord(Ext.create('EqType',record.getData()));
//            newpanel.on('save', function () {
//                self.refresh()
//                })
            //if(record) newpanel.loadRecord(record)            
            wnd.show();
//            return  newobjpanel
            }
        else
            {    
                console.log('existingWindow');
                existingWindow.focus();
//            return  existingWindow.down('[itemId=objpanel]')
            }
    },
    initComponent: function () {
        this.callParent();
        this.getSelectionModel().on('selectionchange', this.onSelectChange, this);
    },
    updateData:function(data){
        this.changeData(data,"equipmentType");
    }
   });


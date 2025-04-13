/**
 * Created by ilazarevsky on 6/29/15.
 */
Ext.define('Billing.view.trash.RecycleBinPanel', {
    extend:  'WRExtUtils.WRGrid',
    alias: 'widget.recyclebingrid',
    title: 'Корзина',
    storeName: 'EDS.store.RecycleBinData',
    dockedToolbar: [ 'restore',  'remove', 'refresh','search' , 'selectAll'],
    initComponent: function() {
        this.callParent();
        // this.getSelectionModel().on('selectionchange', this.onSelectChange, this);
    },
    selModel: {
        pruneRemoved: false,
        mode: 'MULTI'
    },
    columns: [
        {
            header: '№',
            xtype: 'rownumberer',
            width:40,
            resizable:true
        },
        //{
        //    header: "Время удаления",
        //    width: 120
        //},
        {
            header: "Время удаления",
            width:200,
            sortable: true,
            filter: {
                type: 'string'
            },
            //xtype: 'datecolumn',
            //format:"Y-m-d H:i:s",
            dataIndex: "removalTime"
        },
        {
            header: "Имя объекта",
            width: 400,
            filter: {
                type: 'string'
            },
            sortable: true,
            dataIndex: "entityName"
        },
        {
            header: "Учётная запись",
            width: 300,
            filter: {
                type: 'string'
            },
            sortable: true,
            dataIndex: "accountName"
        },
        //{
        //    header: "ID",
        //    dataIndex: "_id",
        //    width: 200
        //},
        {
            header: "Тип объекта",
            filter: {
                type: 'string'
            },
            dataIndex: "type",
            width: 200
        },
        {
            header: "Данные",
            dataIndex: "payload",
            flex: 1,
            renderer:  function (value, metaData, record, rowIdx, colIdx, store) {
                metaData.tdAttr = 'title="' + Ext.String.htmlEncode(value) + '"'
                return value;
            }
        }
    ],


    listeners: {
        celldblclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
            var self=this;
            this.showItemWindow(record);
        }
    },

    showItemWindow: function(record) {
        var existingWindow = WRExtUtils.createOrFocus('trashitemwnd' + record.get('_id'), 'Billing.view.trash.ItemWindow', {
            title: 'Данные'
        });
        if (existingWindow.justCreated)
            existingWindow.down('trashitemform').setActiveRecord(record);
        existingWindow.show();

        return existingWindow.down('trashitemform');
    },

    refresh: function() {
        this.callParent();
    },
    onSelectChange: function (selModel, selections) {
        this.down('#delete').setDisabled(selections.length === 0);
    },
    onRestoreClick: function() {
        var self = this;
        var selection = self.getView().getSelectionModel().getSelection();
        if(selection) {
            var store= this.store;
            console.log('Store = ');
            console.log(store);
            recycleBinStoreManager.restore(Ext.Array.map(selection, function(m){return {_id: m.get("_id"), type: m.get("type")}}), function(r,e) {
                if (!e.status) {
                    Ext.MessageBox.show({
                        title: 'Произошла ошибка',
                        msg: e.message,
                        icon: Ext.MessageBox.ERROR,
                        buttons: Ext.Msg.OK
                    });
                }
                //self.refresh();
            });
            //  store.revive(selection);
        }
        self.getView().getSelectionModel().deselectAll();
    },
    onDeleteClick: function() {
        var self =  this;
        var selection = self.getView().getSelectionModel().getSelection();
        if(selection) {
            Ext.MessageBox.confirm('Удаление элементов', 'Вы действительно хотите удалить безвозвратно' + selection.length + ' элементов', function(choice) {
                if(choice == 'yes') {
                    recycleBinStoreManager.delete(Ext.Array.map(selection, function(m){return {_id: m.get("_id"), type: m.get("type")}}), function(r,e) {
                        if (!e.status) {
                            Ext.MessageBox.show({
                                title: 'Произошла ошибка',
                                msg: e.message,
                                icon: Ext.MessageBox.ERROR,
                                buttons: Ext.Msg.OK
                            });
                        }
                      // self.refresh();
                    });
                }
            });
        }
        self.getView().getSelectionModel().deselectAll();
    },
    updateData:function(data){
        var self = this;
        var rec = null;
        var store=self.getStore();
        var recstore=store.snapshot;
        if(recstore==undefined) recstore=store.data;
        switch(data.action) {
            case "remove":
                store.insert(0, data.data);
                break;
            case "restore":
            {
                rec = recstore.findBy(function(item, id){
                    if(item.get("_id")==data.itemId)
                    {return true}
                    else {return false}
                })
                if(rec!=null) {
                    store.remove(rec)
                }
                console.log("restore handle");
               // self.refresh();
                break;
            }
            case "delete":
                self.refresh();
                break;
        }
    }
});

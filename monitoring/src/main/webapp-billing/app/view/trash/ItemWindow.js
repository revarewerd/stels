/**
 * Created by ivan on 27.12.15.
 */

Ext.define('Billing.view.trash.ItemForm', {
    extend: 'Ext.form.Panel',
    alias: 'widget.trashitemform',
    initComponent: function () {
        var self = this;
        Ext.apply(this, {
            layout:'fit',
            items: [
                {
                    xtype: 'grid',
                    store: {
                        fields: [
                            {name: 'key', type: 'string'},
                            {name: 'value', type: 'string'}
                        ]
                    },
                    columns  : [
                        {
                            text : 'Поле',
                            dataIndex : 'key',
                            flex: 1,
                            renderer:  function (value, metaData, record, rowIdx, colIdx, store) {
                                metaData.tdAttr = 'title="' + Ext.String.htmlEncode(value) + '"'
                                return value;
                            }
                        },
                        {
                            text : 'Значение',
                            dataIndex : 'value',
                            flex: 2,
                            renderer:  function (value, metaData, record, rowIdx, colIdx, store) {
                                metaData.tdAttr = 'title="' + Ext.String.htmlEncode(value) + '"'
                                return value;
                            }
                        }
                    ]
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
                            icon:  'extjs-4.2.1/examples/build/KitchenSink/ext-theme-access/resources/images/icons/fam/accept.gif',
                            text: 'Восстановить',
                            scope: this,
                            handler: this.onRestore
                        },
                        {
                            icon:  'extjs-4.2.1/examples/restful/images/delete.gif',
                            text: 'Удалить',
                            scope: this,
                            handler: this.onDelete
                        },
                        {
                            icon: 'images/ico16_cancel.png',
                            text: 'Отменить',
                            scope: this,
                            handler: this.closeAction
                        }]
                }
            ]
        });
        this.callParent();
    },

    onRestore: function() {
        var record = this.activeRecord;
        var self = this;
        recycleBinStoreManager.restore([{_id: record.get("_id"), type: record.get("type")}], function(r,e) {
            if (!e.status) {
                Ext.MessageBox.show({
                    title: 'Произошла ошибка',
                    msg: e.message,
                    icon: Ext.MessageBox.ERROR,
                    buttons: Ext.Msg.OK
                });
            }
            else {
                self.closeAction();
            }
        });
    },
    onDelete: function() {
        var record = this.activeRecord;
        Ext.MessageBox.confirm('Удаление элементов', 'Удалить элемент безвозвратно?', function(choice) {
            if (choice == 'yes') {
                recycleBinStoreManager.delete([{_id: record.get("_id"), type: record.get("type")}], function (r, e) {
                    if (!e.status) {
                        Ext.MessageBox.show({
                            title: 'Произошла ошибка',
                            msg: e.message,
                            icon: Ext.MessageBox.ERROR,
                            buttons: Ext.Msg.OK
                        });
                    }
                });
            }
        });
    },
    setActiveRecord: function (data) {
        var grid=this.down('grid');
        this.activeRecord = data;
        var store=grid.getStore();
        var array = [];
        array.push({key: 'Имя', value: data.get('entityName')});
        array.push({key: 'Тип элемента', value: data.get('type')});
        array.push({key: 'Учетная запись', value: data.get('accountName')});
        var payload = data.get("payloadData");
        for(var k in payload) {
            var val = payload[k];
            array.push({key: k, value: val});
        }
        console.log("data = " +   data);
        store.loadData(array);
    }
});

Ext.define('Billing.view.trash.ItemWindow', {
    extend: 'Ext.window.Window',
    alias: 'widget.trashitemwnd',
    closable: true,
    maximizable: true,
    width: 400,
    height: 400,
    layout: 'fit',
    initComponent: function () {
        Ext.apply(this, {
            items:[
                {
                    xtype:"trashitemform",
                    closeAction: function () {
                        this.up('window').close();
                    }
                }
            ]
        });
        this.callParent();
    }
});

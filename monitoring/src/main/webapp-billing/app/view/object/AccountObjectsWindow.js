//Ext.Loader.setPath('Ext.ux', 'extjs-4.2.1/examples/ux');
Ext.define('Billing.view.object.ObjectsSelectionPanel', {
    extend: 'WRExtUtils.WRGrid',
    header: false,
    //autoScroll : true,
    flex: 1,
    invalidateScrollerOnRefresh: false,
    viewConfig: {
        trackOver: false
    },
    loadMask: true,
    multiSelect: true,
    setStore: false,
    dockedToolbar: ['refresh', 'search'],
    initComponent: function () {
        var self = this;
        Ext.apply(this, {
            store: Ext.create('EDS.store.AllObjectsService', {
                autoLoad: true,
                autoSync: false,
                listeners: {
                    beforeload: function (store, op) {
                        store.getProxy().setExtraParam("nonAccount", self.up('window').accountId);
                    }
                }
            }),
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
                    header: 'Учетная запись',
                    flex: 1,
                    sortable: true,
                    dataIndex: 'accountName',
                    filter: {
                        type: 'string'
                    }
                }
            ]
        });
        this.callParent();
        console.log("dockedItems=",this.dockedItems);
        this.addDocked({
            xtype: 'toolbar',
            dock: 'bottom',
            ui: 'footer',
            items: ['->', {
                icon: 'images/ico16_okcrc.png',
                itemId: 'save',
                text: 'Добавить',
                handler: function () {
                    self.up('window').fireEvent('save', this.up('grid').getSelectionModel().getSelection())
                }
            }, {
                icon: 'images/ico16_cancel.png',
                text: 'Отменить',
                handler: function () {
                    self.up('window').close()
                }

            }]
        });
    }
});


Ext.define('Billing.view.object.AccountObjectsPanel', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.objectsgrid',
    animate: false,
    multiSelect: true,

    initComponent: function () {

        var self = this

        Ext.apply(this, {
            frame: true,
            store: Ext.create('EDS.store.ObjectsData', {
                autoLoad: true,
                listeners: {
                    beforeload: function (store, op) {
                        store.getProxy().setExtraParam("accountId", self.accountId)
                    },
                    write: function (proxy, operation) {

                        console.log(operation.action + " " + operation.resultSet.message);
                    }
                }
            }),
            dockedItems: [
                {
                    xtype: 'toolbar',
                    items: [
                        {
                            icon: 'extjs-4.2.1/examples/restful/images/add.gif',
                            text: 'Добавить новый',
                            disabled:self.hideRule,
                            scope: this,
                            handler: this.onAddClick
                        },
                        {
                            icon: 'extjs-4.2.1/examples/restful/images/add.gif',
                            text: 'Переместить существующий',
                            disabled:self.hideRule,
                            scope: self,
                            handler: this.onAddExistent
                        },
                        {
                            icon: 'extjs-4.2.1/examples/restful/images/delete.gif',
                            text: 'Удалить',
                            disabled: true,
                            itemId: 'delete',
                            scope: this,
                            handler: this.onDeleteClick
                        },
                        {
                            icon: 'extjs-4.2.1/examples/page-analyzer/resources/images/refresh.gif',
                            text: 'Обновить',
                            itemId: 'refresh',
                            scope: this,
                            handler:                
                                this.refresh
                            
                        }
                    ]
                }
            ],

            columns: [
                {
                    xtype: 'rownumberer',
                    width:40,
                    resizable:true
                },
                {
                    text: 'Наименование',
                    flex: 2,
                    sortable: true,
                    dataIndex: 'name'
                },
                {
                    text: 'Комментарий',
                    flex: 2,
                    sortable: true,
                    dataIndex: 'comment'
                },
                {
                    text: 'Уникальный идентификатор',
                    flex: 2,
                    dataIndex: 'uid',
                    sortable: true,
                    align: 'right'
                },
                {
                    text: 'Абонентская плата',
                    flex: 1,
                    dataIndex: 'cost',
                    sortable: true,
                    align: 'right',
                    renderer: function (value) {
                        return accounting.formatMoney(parseFloat(value / 100));
                    }
                },
                {   text: 'Марка',
                    flex: 1,
                    dataIndex: 'marka',
                    sortable: true,
                    align: 'right'
                },
                {   text: 'Модель',
                    flex: 1,
                    dataIndex: 'model',
                    sortable: true,
                    align: 'right'
                },
                {   text: 'Госномер',
                    flex: 1,
                    dataIndex: 'gosnumber',
                    sortable: true
                    //align:'right'                    
                },
                {  text: 'VIN',
                    flex: 1,
                    dataIndex: 'VIN',
                    sortable: true
                    //align:'right'                    
                }
            ],
            listeners: {
                itemdblclick: function (/*Ext.view.View */self, /*Ext.data.Model*/ record, /*HTMLElement*/ item, /*Number*/ index, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
                    this.showObjectForm(record)
                }
            },
            onSelectChange: function (selModel, selections) {
                if(self.hideRule)
                    this.down('#delete').setDisabled(true);
                else
                    this.down('#delete').setDisabled(selections.length === 0);
            }
        });
        this.callParent();
        this.getSelectionModel().on('selectionchange', this.onSelectChange, this);

    },
    refresh:function () {
       this.store.load(); 
    },
    showObjectForm: function (record) {
        var self=this
        console.log('record=', record)
        var existingWindow = Ext.getCmp('ObjectForm' + record.get('_id'))
        if (!existingWindow) {

            var objWnd = Ext.create('Billing.view.object.ObjectForm.Window', {
                title: 'Объект "' + record.get('name') + '"',
                id: 'ObjectForm' + record.get('_id'),
                type: record.get('type'),
                hideRule:self.hideRule
            })
            objWnd.down('[itemId=objpanel]').on('save',function(){
                console.log('REFRESH')
                self.refresh()})
            console.log('objpanel=', objWnd.down('[itemId=objpanel]').loadRecord(record))
            objWnd.show();
            return  objWnd.down('[itemId=objpanel]')
        }
        else {
            existingWindow.focus()
            return  existingWindow.down('[itemId=objpanel]')
        }
    },
    onDeleteClick: function () {
        var selection = this.getView().getSelectionModel().getSelection();//[0];
        // TODO по-хорошему лучше бы эта функция объекты с аккаунта снимала
        if (selection) {
            var store = this.store;
            Ext.MessageBox.show({
                title: 'Удаление элемента',
                buttons:  Ext.MessageBox.YESNO,
                msg: 'Вы уверены, что хотите удалить ' + selection.length + ' объектов? <br/><br/><input type="checkbox" id="deinstall_equipment_chk" /> Снять оборудование с объектов',

                fn: function(btn) {
                    if(btn == 'yes') {
                        var deinstall = Ext.getElementById('deinstall_equipment_chk').checked;
                        console.log("removing:", selection);
                        allObjectsService.remove(Ext.Array.map(selection, function (m) {
                            return m.get("_id");
                        }), deinstall, function (r, e) {
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
                }
            });
        }
    },

    onAddClick: function () {
        var self = this

        var rec = new EDS.model.ObjectsData({
            _id: null,
            accountId: self.accountId,
            name: '',
            uid: '',
            phone: '',
            type:'Автомобиль',
            leaf: false
        })

        this.showObjectForm(rec)
    },
    onAddExistent: function () {

        var self = this

        var obj = Ext.create('Ext.window.Window', {
            title: 'Объекты учетной записи',
            closable: true,
            //constrainHeader: true,
            maximizable: true,
            width: 800,
            minWidth: 350,
            height: 500,
            accountId: self.accountId,
            layout: {
                type: 'border'
            },
            items: [
                Ext.create('Billing.view.object.ObjectsSelectionPanel', {
                    region: 'center',
                    accountId: self.accountId
                })
            ]
        })

        obj.on('save', function (selected) {

            accountsStoreService.addToAccount(self.accountId,
                Ext.Array.map(selected, function (o) {return o.get('_id')}), function () {
                    self.getStore().load()
                    obj.close()
                })
        })

        obj.show()
    },
    flex: 1,
    region: 'center'
});

Ext.define('Billing.view.object.AccountObjectsWindow', {
    extend: 'Ext.window.Window',
    title: 'Объекты учетной записи',
    alias: 'widget.objectswnd',
    closable: true,
    //constrainHeader: true,
    maximizable: true,
    width: 1024,
    minWidth: 350,
    height: 500,
    accountId: null,
    layout: {
        type: 'border'
    },
    initComponent: function () {
        var self=this
        Ext.tip.QuickTipManager.init();
        Ext.apply(this,
            {
                items: [
                    Ext.create('Billing.view.object.AccountObjectsPanel', {
                        accountId: this.accountId,
                        hideRule:self.hideRule
                    })
                ]
            });
        this.callParent();
    }
})

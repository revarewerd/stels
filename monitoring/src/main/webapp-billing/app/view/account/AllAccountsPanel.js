Ext.define('Billing.view.account.AllAccountsPanel', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.allaccountgrid',
    title: 'Учетные записи',
    requires: [
        "Billing.view.account.AccountForm",
        'Billing.view.BalanceHistory'
    ],
    features: [
        {
            ftype: 'summary',
            dock:'top'
        }
    ],
    storeName: 'EDS.store.AccountsData',
    itemType:'accounts',
    dockedToolbar: ['add', 'remove', 'refresh', 'search','fill','gridDataExport'],
    viewConfig: {
         getRowClass: function(record, rowIndex, rowParams, store){                         
                            return  record.get("status") == false ? 'redText' : ' ';
                        }
    },
    initComponent: function () {
        var self = this;
        var hideRule=!self.viewPermit;
        Ext.apply(this, {
            columns: [
                {
                    hideable: false,
                    menuDisabled: true,
                    sortable: false,
                    flex: 1
                },
                {
                    header: '№',
                    xtype: 'rownumberer',
                    width: 40,
                    resizable: true
                },
                {
                    menuDisabled: true,
                    sortable: false,
                    xtype: 'actioncolumn',
                    itemId: 'acc',
                    width: 20,
                    resizable: false,
                    items: [
                        {
                            icon: 'images/account-icon16.png'
                        }
                    ]
                },
                {
                    header: 'Имя',
                    width: 200,
                    sortable: true,
                    dataIndex: 'name',
                    filter: {
                        type: 'string'
                    },
                    renderer: function (val, metaData, rec) {
                        metaData.tdAttr = 'style="cursor: pointer !important;"';
                        return val;
                    },
                    summaryType: 'count',
                    summaryRenderer: function (value, summaryData, dataIndex) {
                        return Ext.String.format('<b>Всего позиций: {0} </b>', value);
                    }
                },
                {
                    header: 'Комментарий',
                    width: 200,
                    sortable: true,
                    dataIndex: 'comment',
                    filter: {
                        type: 'string'
                    },
                    hidden:!this.viewPermit
                },
                {
                    header: 'Баланс',
                    sortable: true,
                    dataIndex: 'balance',
                    align: 'right',
                    renderer: function (value) {
                        return '<div class="ballink">' + accounting.formatMoney(parseFloat(value / 100)) + '</div>';
                    }
                },
                {
                    header: 'Аб. Плата',
                    sortable: true,
                    dataIndex: 'cost',
                    align: 'right',
                    renderer: function (value) {
                        return '<div class="ballink">' + accounting.formatMoney(parseFloat(value / 100)) + '</div>';
                    },
                    summaryType: 'sum',
                    summaryRenderer: function (value, summaryData, dataIndex) {
                        return Ext.String.format('<b>{0}</b>', accounting.formatMoney(parseFloat(value / 100)));
                    }
                },
                {
                    header: 'Тариф',
                    width: 130,
                    sortable: true,
                    dataIndex: 'plan',
                    filter: {
                        type: 'string'
                    }
                },
                {
                    header: 'Объекты',
                    dataIndex: 'objectsCount',
                    align: 'right',
                    width: 80,
                    filter: {
                        type: 'string'
                    },
                    renderer: function (value) {
                        return '<div class="ballink">' + value + '</div>';
                    },
                    summaryType: 'sum',
                    summaryRenderer: function (value, summaryData, dataIndex) {
                        return Ext.String.format('<b>{0}</b>', value);
                    }
                },
                {
                    header: 'Оборудование',
                    dataIndex: 'equipmentsCount',
                    align: 'right',
                    width: 80,
                    filter: {
                        type: 'string'
                    },
                    renderer: function (value) {
                        return '<div class="ballink">' + value + '</div>';
                    },
                    summaryType: 'sum',
                    summaryRenderer: function (value, summaryData, dataIndex) {
                        return Ext.String.format('<b>{0}</b>', value);
                    }
                },
                {
                    header: 'Пользователи',
                    dataIndex: 'usersCount',
                    align: 'right',
                    width: 100,
                    filter: {
                        type: 'string'
                    },
                    renderer: function (value) {
                        return '<div class="ballink">' + value + '</div>';
                    }
                },
                {
                    header: 'Статус',
                    dataIndex: 'status',
                    width: 100,
                    renderer: function (value) {
                        if (value) return "Включен";
                        else return "Заблокирован";
                    }
                },
                {
                    hideable: false,
                    menuDisabled: true,
                    sortable: false,
                    flex: 1
                }
            ],
            onSelectChange: function (selModel, selections) {
                if(hideRule)
                this.down('#delete').setDisabled(true);
                else
                this.down('#delete').setDisabled(selections.length === 0);
            }
        });
        this.callParent();
        this.down('#add').setDisabled(hideRule)
    },
    listeners: {
        cellclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
            var self=this;
            var itemId=self.columnManager.columns[cellIndex].itemId;
            if (itemId == "acc") {
               this.showAccountForm(record)
            }
        },
        celldblclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
            var self=this;
            var dataIndex=self.columnManager.columns[cellIndex].dataIndex;
            var id=record.get('_id')
            var name=record.get('name')
            switch(dataIndex)
            {
                case "name":
                    this.showAccountForm(record)
                    break;
                case "balance":
                    var bhw = WRExtUtils.createOrFocus('BhWWnd' + id, 'Billing.view.BalanceHistoryWindow', {
                        title: "Баланс учетной записи: \"" + record.get('name') + "\"",
                        accountId: record.get('_id'),
                        hideRule:!self.viewPermit
                    });
                    bhw.show();
                    break;
                case "cost":
                    var mpf = WRExtUtils.createOrFocus('MPFWnd' + id, 'Billing.view.MonthlyPaymentWindow', {
                        title: "Ежемесячные начисления: \"" + name + "\"",
                        accountId: id
                    });
                    mpf.show();
                    break;
                case "objectsCount":
                    var ow = WRExtUtils.createOrFocus('ObjWnd' + id, 'Billing.view.object.AccountObjectsWindow', {
                        accountId: id,
                        title: "Объекты  учетной записи \"" + name + "\"",
                        hideRule:!self.viewPermit
                    });
                    if (ow.justCreated) { ow.on('close', function () {console.log('ow on save');self.refresh();});}
                    ow.show();
                    break;
                case "equipmentsCount":
                    var ew = WRExtUtils.createOrFocus('EqWnd' + id, 'Billing.view.equipment.AccountEquipmentsWindow', {
                        hideRule:!self.viewPermit,
                        accountId: id,
                        title: "Оборудование учетной записи \"" + name + "\""
                    });
                    if (ew.justCreated) { ew.on('close', function () {console.log('ew on save');self.refresh();});}
                    ew.show();
                    break;
                case "usersCount":
                    var uw = WRExtUtils.createOrFocus('UsrWnd' + id, 'Billing.view.user.UsersWindow', {
                        oid: id,
                        type:'account',
                        title: "Пользователи учетной записи \"" + name + "\""
                    });
                    if (uw.justCreated) { uw.on('close', function () {console.log('uw on save');self.refresh();});}
                    uw.show();
                    break;
            }
        }
    },
    evalIfPermitted:function(fun,record){
        var self=this
            if(self.hideRule) {
                fun(record)
            }
            else
                Ext.MessageBox.show({
                    title: 'Произошла ошибка',
                    msg: "Недостаточно прав для выполнения операции",
                    icon: Ext.MessageBox.ERROR,
                    buttons: Ext.Msg.OK
                });
    },
    showAccountForm: function (record) {
        var self = this;
        var existingWindow = WRExtUtils.createOrFocus('accWnd' + record.get('_id'), 'Billing.view.account.AccountForm.Window', {
            title: 'Учетная запись "' + record.get('name') + '"',
            id: 'accWnd' + record.get('_id'),
            hideRule:!self.viewPermit
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
    },
    onDeleteClick: function () {
        var selection = this.getView().getSelectionModel().getSelection();//[0];

        if (selection) {
            var store = this.store;
            Ext.MessageBox.show({
                title: 'Удаление элемента',
                buttons: Ext.MessageBox.YESNO,
                msg: 'Вы уверены, что хотите удалить ' + selection.length + ' учетных записей?' +
                '<br/><br/><input type="checkbox" id="remove_objects_alongside" /> Удалить объекты и оборудование вместе с учетной записью' +
                '<br/><input type="checkbox" id="move_to_default_acc" /> Перенести объекты и оборудование на аккаунт по-умолчанию',
                fn: function (btn) {
                    if(btn == 'yes') {
                        var removalOptions = {};
                        ["remove_objects_alongside", "move_to_default_acc"].forEach(function(prop) {
                           removalOptions[prop] = Ext.getElementById(prop).checked;
                        });

                        accountsStoreService.remove(Ext.Array.map(selection, function (m) {
                            return m.get("_id");
                        }) ,removalOptions, function (r, e) {
                            if (!e.status) {
                                Ext.MessageBox.show({
                                    title: 'Произошла ошибка',
                                    msg: e.message,
                                    icon: Ext.MessageBox.ERROR,
                                    buttons: Ext.Msg.OK
                                });
                            }
                        });
                        store.initSortable();
                        store.sort();
                    }
                }
            });
        }
            //Ext.MessageBox.confirm('Удаление элемента', 'Вы уверены, что хотите безвозвратно удалить ' + selection.length + ' учетных записей?', function (button) {
            //    if (button === 'yes') {
            //        store.remove(selection);
            //        store.initSortable();
            //        store.sort();
            //    }
            //});
    },

    onAddClick: function (type) {
        var rec = Ext.create('Account', {accountType: type});
        var form = this.showAccountForm(rec);
    },
    getAdd: function () {
        var btn = Ext.create('Ext.Button', {
            icon: 'extjs-4.2.1/examples/restful/images/add.gif',
            text: 'Добавить',
            defaultType: 'button',
            itemId:"add",
            menu: [
                {text: 'Учетная запись юрид. лица',
                    scope: this,
                    handler: function () {
                        this.onAddClick('yur');
                    }
                },
                {text: 'Учетная запись физич. лица',
                    scope: this,
                    handler: function () {
                        this.onAddClick('fiz');
                    }
                }
            ]
        });
        return btn;
    },
    updateData:function(data){
        this.changeData(data,"account")
    }
});
Ext.require([
    'Ext.data.*',
    'Ext.tip.QuickTipManager',
    'Ext.window.MessageBox'
]);

Ext.define('Billing.view.BalanceHistory', {
    title: 'История баланса',
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.balancehistory',
    //autoScroll : true,
    selModel: {
        pruneRemoved: false,
        mode: 'MULTI'
    },
    storeName: 'EDS.store.BalanceHistory',
    initComponent: function(){

        //this.editing = Ext.create('Ext.grid.plugin.CellEditing');

        var self = this;

        Ext.apply(this, {
            dockedToolbar: ['balanceEntryTypes', 'period', 'fill', 'gridDataExport'],
            columns: [
                {
                    header: 'Тип',
                    width: 120,
                    sortable: true,
                    dataIndex: 'type'
                },
                {
                    header: 'Сумма',
                    width: 100,
                    sortable: true,
                    dataIndex: 'ammount',
                    align: 'right',
                    renderer: function(value){
                        return accounting.formatMoney(parseFloat(value / 100));
                    }
                },
                {
                    header: 'Баланс',
                    width: 100,
                    sortable: true,
                    dataIndex: 'newbalance',
                    align: 'right',
                    renderer: function(value){
                        return accounting.formatMoney(parseFloat(value / 100));
                    }
                },
                {
                    header: 'Время',
                    width: 120,
                    sortable: true,
                    dataIndex: 'timestamp',
                    align: 'right',
                    xtype: 'datecolumn',
                    format: "Y-m-d H:i:s"
                },
                {
                    header: 'Комментарий',
                    flex: 1,
                    sortable: true,
                    dataIndex: 'comment',
                    align: 'left',
                    renderer: function(val, metaData, rec){
                        metaData.tdAttr = 'title="' + val + '"';
                        return val
                    }
                }
            ]
        });
        this.callParent();
    }

});


Ext.define('Billing.view.BalanceHistoryWindow', {
    extend: 'Ext.window.Window',
    alias: "widget.balancehistorywindow",
    title: 'История изменения баланса',
    height: 450,
    width: 950,
    layout: 'border',
    maximizable: true,
    initComponent: function(){

        var self = this;


        Ext.applyIf(this, {
            formsubmitMethod: balanceChange.balanceChange
        });

        if(!this.itemType)
            this.itemType = 'balance';

        if(!this.operationsstore) {
            Ext.applyIf(this, {
                operationsstore: Ext.create('EDS.store.CommercialServices', {
                    autoLoad: true,
                    listeners: {
                        beforeload: function(store, op){
                            store.getProxy().setExtraParam("accountId", self.accountId)
                        }
                    }
                })
            });
        }

        Ext.apply(this, {
            items: [
                {
                    xtype: 'form',
                    title: 'Изменение баланса',
                    hidden: self.hideRule,
                    api: {
                        submit: self.formsubmitMethod
                    },

                    layout: 'anchor',
                    defaults: {
                        anchor: '100%'
                    },

                    // The fields
                    defaultType: 'textfield',
                    items: [
                        {
                            xtype: 'combobox',
                            fieldLabel: 'Тип',
                            name: 'type',
                            store: self.operationsstore,
                            valueField: 'name',
                            displayField: 'name',
                            typeAhead: true,
                            queryMode: 'local',
                            emptyText: 'Тип операции',
                            allowBlank: false,
                            listeners: {
                                'select': function(combo, records, eOpts){
                                    var record = records[0];
                                    this.up('form').down('field[name=amount]').setValue(record.get('cost'))
                                }
                            }
                        },
                        {
                            fieldLabel: 'Сумма',
                            name: 'amount',
                            xtype: 'numberfield',
                            emptyText: 'Сумма к зачислению/снятию',
                            allowBlank: false
                        },
                        {
                            fieldLabel: 'Комментарий',
                            name: 'comment',
                            xtype: 'textareafield',
                            rows: 2
                        }
                    ],
                    buttons: [
                        {
                            text: 'Исполнить операцию',
                            formBind: true, //only enabled once the form is valid
                            disabled: true,
                            handler: function(){

                                var self = this

                                var form = this.up('form').getForm();
                                if (form.isValid()) {

                                    var amount = form.getValues()['amount'];
                                    var msg, header
                                    if (amount >= 0) {
                                        msg = 'Вы уверены, что хотите <b>зачислить</b> на счет ' + amount + "?";
                                        header = 'Зачисление на счёт';
                                    }
                                    else {
                                        msg = 'Вы уверены, что хотите <b>снять</b> со счета ' + (-1) * amount + "?";
                                        header = 'Снятие со счета';
                                    }

                                    Ext.MessageBox.confirm(header, msg, function(button){
                                        if (button === 'yes') {
                                            form.submit({
                                                params: {
                                                    accountId: self.up('balancehistorywindow').accountId
                                                },
                                                success: function(form, action){
                                                    //Ext.Msg.alert('Success', action.result.message);
                                                    self.up('balancehistorywindow').down('balancehistory').store.load()
                                                    self.up('form').getForm().reset();
                                                },
                                                failure: function(form, action){
                                                    Ext.MessageBox.show({
                                                        title: 'Произошла ошибка',
                                                        msg: action.result.exception,
                                                        icon: Ext.MessageBox.ERROR,
                                                        buttons: Ext.Msg.OK
                                                    });
                                                }
                                            });
                                        }
                                    });
                                }
                            }
                        }
                    ],


                    region: 'north'
                },
                {
                    xtype: 'balancehistory',
                    itemType: self.itemType,
                    storeExtraParam:{ accountId: self.accountId, itemType: self.itemType},
                    border: false,
                    region: 'center'
                }
            ]
        });
        this.callParent();
    }
});


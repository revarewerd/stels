Ext.define('Billing.view.MonthlyPaymentWindow', {
    extend: 'Ext.window.Window',
    alias: 'widget.objwnd',
    title: 'Ежемесячные начисления',
    width: 1024,
    height: 768,
    //constrainHeader: true,
    maximizable: true,
    layout: 'fit',
    initComponent: function(){
        var self = this;

        console.log("MonthlyPaymentWindow.detailsStore1", self.detailsStore);

        if (!self.detailsStore)
            Ext.applyIf(self, {
                detailsStore: Ext.create('EDS.store.MonthlyPaymentService', {
                    autoLoad: true,
                    groupField: 'objectName',
                    listeners: {
                        beforeload: function(store, op){
                            console.log('accountId', self.accountId);
                            store.getProxy().setExtraParam("accountId", self.accountId);
                        }
                    }
                })
            });

        console.log("MonthlyPaymentWindow.detailsStore2", self.detailsStore);

        Ext.apply(this, {
            dockedItems: [
                {
                    xtype: 'toolbar',
                    dock: 'top',
                    items: [
                        {
                            xtype: 'button',
                            text: 'Разгруппировать',
                            handler: function(but){
                                var grouping = self.down('grid').getView().getFeature('groupingsummary');
                                if (grouping.disabled) {
                                    grouping.enable();
                                    self.down('[itemId=collapseButton]').enable();
                                    grouping.collapsed = true;
                                    grouping.collapseAll();
                                    but.setText('Разгрупировать');
                                }
                                else {
                                    grouping.disable();
                                    self.down('[itemId=collapseButton]').disable();
                                    but.setText('Групировать');
                                }
                            }
                        },
                        {
                            xtype: 'button',
                            itemId: 'collapseButton',
                            text: 'Развернуть',
                            handler: function(but){
                                var grouping = self.down('grid').getView().getFeature('groupingsummary');
                                if (grouping.collapsed) {
                                    grouping.collapsed = false;
                                    grouping.expandAll();
                                    but.setText('Свернуть');
                                }
                                else {
                                    grouping.collapsed = true;
                                    grouping.collapseAll();
                                    but.setText('Развернуть');
                                }
                            }
                        },
                        {
                            xtype: 'combobox',
                            itemId: 'monthCmbx',
                            text: 'Месяц',
                            editable: false,
                            forceSelection: true,
                            autoSelect: true,
                            allowBlank: false,
                            store: function(){
                                var data = [];
                                var now = new Date();
                                for (var i = 0; i < 12; i++) {
                                    var date = Ext.Date.subtract(now, Ext.Date.MONTH, i);
                                    data.push({'code': -i, 'name': Ext.Date.format(date, 'Y F')});
                                }
                                console.log('Combo box items = ', data);

                                return Ext.create('Ext.data.ArrayStore', {
                                    fields: ['code', 'name'],
                                    data: {
                                        'items': data
                                    },
                                    proxy: {
                                        type: 'memory',
                                        reader: {
                                            type: 'json',
                                            root: 'items'
                                        }
                                    }
                                });
                            }(),
                            listeners: {
                                afterrender: function(){
                                    var cmb = this;
                                    this.select(this.getStore().data.items[1]);
                                    this.on('change', function(){

                                        var store = self.down('grid').getStore();
                                        store.getProxy().setExtraParam("month", cmb.value);
                                        store.reload();
                                    });
                                }
                            },
                            //queryMode: 'local',
                            displayField: 'name',
                            valueField: 'code'
//                    handler:function(but){
//
//                    }
                        }
                    ]
                }
            ],
            items: [
                {
                    xtype: 'grid',
                    store: self.detailsStore,
                    features: [
                        {
                            ftype: 'summary',
                            dock: 'top'
                        },
                        {
                            groupHeaderTpl: 'Объект: {name}',
                            ftype: 'groupingsummary',
                            startCollapsed: true,
                            collapsed: true,
                            id: 'groupingsummary'
                        }
                    ],
                    columns: [
                        {
                            header: '№',
                            xtype: 'rownumberer',
                            width: 40,
                            resizable: true
                        },
                        {
                            header: 'Объект',
                            //width:170,
                            flex: 1,
                            sortable: true,
                            dataIndex: 'objectName',
                            summaryType: 'count',
                            summaryRenderer: function(value, summaryData, dataIndex){
                                return Ext.String.format('<b>Всего позиций: {0} </b>', value);
                            }
                        },
                        {
                            header: 'Тип услуги',
                            //width:170,
                            flex: 1,
                            sortable: true,
                            dataIndex: 'eqtype'
                        },
                        // // "firstDate", "lastDate", "withdraw",
                        {
                            header: 'Начало обслуживания',
                            //width:170,
                            flex: 1,
                            sortable: true,
                            dataIndex: 'firstDate',
                            xtype: 'datecolumn',
                            summaryType: 'min',
                            format: "d.m.Y"
                        },
                        {
                            header: 'Конец обслуживания',
                            //width:170,
                            flex: 1,
                            sortable: true,
                            dataIndex: 'lastDate',
                            xtype: 'datecolumn',
                            summaryType: 'max',
                            format: "d.m.Y"
                        },
                        {
                            header: 'Начислено',
                            //width:170,
                            flex: 1,
                            sortable: true,
                            dataIndex: 'withdraw',
                            summaryType: 'sum',
                            renderer: function(value){
                                return (value / 100.0).toFixed(2) + ' р.';
                            },
                            summaryRenderer: function(value, summaryData, dataIndex){
                                return Ext.String.format('<b>{0} р.</b>', (value / 100.0).toFixed(2));
                            }
                        },
                        {
                            header: 'Аб. плата',
                            //width:170,
                            flex: 1,
                            sortable: true,
                            dataIndex: 'cost',
                            summaryType: 'sum',
                            renderer: function(value){
                                return value + ' р.';
                            },
                            summaryRenderer: function(value, summaryData, dataIndex){
                                return Ext.String.format('<b>{0} р.</b>', value);
                            }
                        }
                    ]
                }
            ]
        })
        this.callParent();
    }
})


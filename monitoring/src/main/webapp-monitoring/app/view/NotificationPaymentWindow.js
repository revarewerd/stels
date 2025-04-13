/**
 * Created by ivan on 08.08.16.
 */

Ext.define('Seniel.view.NotificationPaymentWindow', {
    extend: 'Seniel.view.WRWindow',
    alias: 'widget.notifpaymentwnd',
    stateId: 'notifPaymentWnd',
    stateful: true,
    icon: 'images/ico16_coins.png',
    title: tr('notificationpay.title'),
    width: 800,
    height: 600,
    minWidth: 800,
    minHeight: 600,
    layout: 'fit',
    tbar: [
        {
            xtype: 'combobox',
            itemId: 'monthCmbx',
            fieldLabel: tr('subsriptionfee.period'),
            labelWidth: 220,
            flex: 1,
            padding: 8,
            editable: false,
            forceSelection: true,
            autoSelect: true,
            allowBlank: false,
            store: function() {
                var data = [];
                var now = new Date();
                for (var i = 0; i < 12; i++) {
                    var date = Ext.Date.subtract(now, Ext.Date.MONTH, i);
                    data.push({'code': -i, 'name': Ext.Date.format(date, 'Y F')});
                }

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
                afterrender: function() {
                    var cmb = this;
                    this.select(this.getStore().data.items[1]);
                    this.on('change', function() {
                        var store = cmb.up('window').down('grid').getStore();
                        store.getProxy().setExtraParam('month', cmb.value);
                        store.reload();
                    });
                }
            },
            displayField: 'name',
            valueField: 'code'
        },
        '-',
        {
            xtype: 'button',
            icon: 'images/ico16_printer.png',
            text: tr('notificationpay.print'),
            tooltip: tr('subsriptionfee.print.tooltip'),
            tooltipType: 'title',
            margin: '0 8',
            handler: function(btn) {
                var grid = btn.up('window').down('grid');
                Ext.ux.grid.Printer.print(grid);
            }
        }
        ],
    items: [

        {
            xtype: 'notifpaymentgrid'
        }
    ]

});


Ext.define('NotificationPaymentGrid', {
    extend:  'Ext.grid.Panel',
    alias: 'widget.notifpaymentgrid',

    store: Ext.create('EDS.store.notificationPaymentList', {
        autoLoad: true
    }),
    cls: 'subscription-fee-grid', // ??
    columns: [
        //Model("user", "phone", "fee", "time", "comment")
        {
            header: tr('notificationpay.user'),
            width: '20%',
            sortable: true,
            summaryType: 'count',
            dataIndex: 'user',
            renderer: function(val) {
                return '<b>' + val + '</b>';
            },
            summaryRenderer: function (val, summaryData, dataIndex) {
                return Ext.String.format('<b>'+tr('notificationpay.totalnotifications') +': '+' {0} </b>', val);
            }
        },
        {
            header: tr('notificationpay.phone'),
            width: '15%',
            sortable: true,
            dataIndex: 'phone'
        },
        {
            header: tr('notificationpay.fee'),
            width: '10%',
            sortable: true,
            dataIndex: 'fee',
            summaryType: 'sum',
            renderer: function(val) {
                return '<b>' + Ext.util.Format.currency(val) + '</b>';
            },
            summaryRenderer: function (val, summaryData, dataIndex) {
                return Ext.String.format('<b>{0}</b>', Ext.util.Format.currency(val));
            }
        },
        {
            header: tr('notificationpay.time'),
            width: '20%',
            sortable: true,
            dataIndex: 'time',
            renderer: function(val) {
                return Ext.util.Format.date(new Date(val), 'd.m.Y H:i:s withTZ');
            }
        },
        {
            header: tr('notificationpay.comment'),
            width: '35%',
            dataIndex: 'comment',
            renderer:  function (value, metaData, record, rowIdx, colIdx, store) {
                metaData.tdAttr = 'title="' + Ext.String.htmlEncode(value) + '"';
                return value;
            }
        }
        // {
        //     header: 'Дата',
        //     width: '30%',
        //     s
        // }
    ],
    features: [{
        ftype: 'summary',
        dock: 'bottom'
    }],
    initComponent: function() {
        console.log('Grid features = ', this.features);
        this.callParent(arguments);
    }


});
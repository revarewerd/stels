Ext.define('Seniel.view.SubscriptionFeeWindow', {
    extend: 'Seniel.view.WRWindow',
    requires: ['Ext.ux.grid.Printer'],
    alias: 'widget.subscrfeewnd',
    stateId: 'sbscrFeeWnd',
    stateful: true,
    icon: 'images/ico16_coins.png',
    title: tr('subsriptionfee.title'),
    btnConfig: {
        icon: 'images/ico24_coins.png',
        text: tr('subsriptionfee.title')
    },
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
            text: tr('subsriptionfee.print'),
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
            xtype: 'subscrfeegrid'
        }
    ]
});


Ext.define('SubscriptionFeeGrid', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.subscrfeegrid',
    stateId: 'subscriptionFeeGrid',
    stateful: true,
    store: Ext.create('EDS.store.SubscriptionFeeList', {
        autoLoad: true
    }),
    cls: 'subscription-fee-grid',
    plugins: [
        {
            ptype: 'rowexpander',
            pluginId: 'rowExpander',
            selectRowOnExpand: true,
            rowBodyTpl : new Ext.XTemplate(
                '<table class="subscription-fee-grid-nested" cellspacing="1"><tpl for="equipments">',
                    '<tr>' +
                    '<td width="46%">{eqType}</td><' +
                    'td width="20%">{[Ext.util.Format.date(new Date(values.firstDate), "d.m.Y")]}</td>' +
                    '<td width="20%">{[Ext.util.Format.date(new Date(values.lastDate), "d.m.Y")]}' +
                    '</td><td width="10%">{withdraw:currency}</td>' +
                    '</tr>',
                '</tpl></table>'
            )
        }
    ],
    columns: [
        {
            stateId: 'feeObjectName',
            header: tr('notifications.history.grid.object'),
            width: '34%',
            sortable: true,
            dataIndex: 'objectName',
            summaryType: 'count',
            renderer: function(val) {
                return '<b>' + val + '</b>';
            },
            summaryRenderer: function (val, summaryData, dataIndex) {
                return Ext.String.format('<b>'+tr('subsriptionfee.totalobjects')+' {0} </b>', val);
            }
        },
        {
            stateId: 'feeEquipment',
            header: tr('subsriptionfee.equipment'),
            width: '12%',
            sortable: true,
            dataIndex: 'eqCount',
            summaryType: 'sum',
            renderer: function(val) {
                return val + ' '+tr('subsriptionfee.units');
            },
            summaryRenderer: function (val, summaryData, dataIndex) {
                return Ext.String.format('<b>{0} '+tr('subsriptionfee.units')+'</b>', val);
            }
        },
        {
            stateId: 'feeServiceStarts',
            header: tr('subsriptionfee.servicestart'),
            width: '20%',
            sortable: true,
            dataIndex: 'firstDate',
            xtype: 'datecolumn',
            summaryType: 'min',
            renderer: function(val) {
                return Ext.util.Format.date(new Date(val), tr('format.extjs.date'));
            },
            summaryRenderer: function (val, summaryData, dataIndex) {
                return Ext.String.format('<b>{0}</b>', Ext.util.Format.date(val, 'd.m.Y'));
            }
        },
        {
            stateId: 'feeServiceEnds',
            header: tr('subsriptionfee.serviceend'),
            width: '20%',
            sortable: true,
            dataIndex: 'lastDate',
            xtype: 'datecolumn',
            summaryType: 'max',
            renderer: function(val) {
                return Ext.util.Format.date(new Date(val), tr('format.extjs.date'));
            },
            summaryRenderer: function (val, summaryData, dataIndex) {
                return Ext.String.format('<b>{0}</b>', Ext.util.Format.date(val, 'd.m.Y'));
            }
        },
        {
            stateId: 'feeFullCost',
            header: tr('subsriptionfee.charge'),
            width: '10%',
            sortable: true,
            dataIndex: 'fullFee',
            summaryType: 'sum',
            renderer: function(val) {
                return '<b>' + Ext.util.Format.currency(val) + '</b>';
            },
            summaryRenderer: function (val, summaryData, dataIndex) {
                return Ext.String.format('<b>{0}</b>', Ext.util.Format.currency(val));
            }
        }
    ],
    
    initComponent: function() {
        this.features.push({
            ftype: 'summary',
            dock: 'bottom'
        });
        console.log('Grid features = ', this.features);
        this.callParent(arguments);
    }
});
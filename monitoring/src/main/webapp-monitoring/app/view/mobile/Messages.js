Ext.define('Seniel.view.mobile.Messages', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.gridmessageslist',
    viewConfig: {
        stripeRows: false,
        markDirty: false,
        getRowClass: function(record) {
            return 'mobile-object-list-row';
        }
    },
    cls: 'mobile-grid-panel',
    initComponent: function() {
        Ext.apply(this, {
            store: Ext.create('EDS.store.EventsMessages', {
                autoLoad: true
            })
        });
        this.getStore().sort('time','DESC');
        this.callParent();
    },
    tbar: [
        {
            itemId: 'fromDate',
            xtype: 'datefield',
            cls: 'mobile-panel-toolbar',
            flex: 2,
            labelWidth: 38,
            labelPad: 2,
            fieldLabel: tr('basereport.fromdate').toLowerCase(),
            editable: false,
            hideTrigger: true,
            matchFieldWidth: true,
            name: 'from_date',
            format: tr('format.extjs.date'),
            value: new Date()
        },
        {
            xtype: 'tbspacer',
            width: 20
        },
        {
            itemId: 'toDate',
            xtype: 'datefield',
            cls: 'mobile-panel-toolbar',
            flex: 2,
            labelWidth: 38,
            labelPad: 2,
            fieldLabel: tr('basereport.toDate').toLowerCase(),
            editable: false,
            hideTrigger: true,
            matchFieldWidth: true,
            name: 'to_date',
            format: tr('format.extjs.date'),
            value: new Date()
        },
        {
            xtype: 'tbspacer',
            width: 20
        },
        {
            xtype: 'combo',
            itemId: 'selectedObjects',
            cls: 'mobile-panel-toolbar',
            flex: 4,
            labelWidth: 80,
            fieldLabel: tr('basereport.objectfield'),
            emptyText: tr('basereport.objectfield.emptytext'),
//            minChars: 0,
            editable: false,
            hideTrigger: true,
            queryMode: 'local',
            valueField: 'uid',
            displayField: 'name',
//            forceSelection: true,
            multiSelect: true,
//            caseSensitive: false,
            listConfig: {
                cls: 'mobile-toolbar-combo-picker'
            },
            store: {
                fields: ['uid', 'name'],
                data: {
                    'items': []
                },
                proxy: {
                    type: 'memory',
                    reader: {
                        type: 'json',
                        root: 'items'
                    }
                }
            },
            listeners: {
                boxready: function() {
                    var viewport = this.up('viewport');
                    this.getStore().add(viewport.down('gridobjectslist').objectsArray);
                }
            }
        },
        {
            xtype: 'tbspacer',
            width: 20
        },
        {
            align: 'center',
            xtype: 'button',
            text: tr('main.ok'),
            cls: 'mobile-toolbar-button',
            scale: 'large',
            flex: 1,
            handler: function() {
                var pnl = this.up('gridmessageslist');
                var objects = pnl.down('combo#selectedObjects').getValue();
                var fromdate = pnl.down('datefield#fromDate').getValue();
                var todate = pnl.down('datefield#toDate').getValue();

                var withTimezone=function(date){
                    var jdate=Ext.Date.format(date,"d.m.Y");
                    var defaultTimezone = Timezone.getDefaultTimezone()
                    res=date
                    if (defaultTimezone) {
                        var res = moment.tz(jdate, "DD.MM.YYYY", defaultTimezone.name).toDate()
                    }
                    return res
                }

                var params = null;
                if (fromdate <= todate) {
                    params = {
                        from: withTimezone(fromdate),
                        to: withTimezone(todate),
                        uids: objects
                    };
                }
                console.log('Store load parameters = ', params);
                if (params) {
                    Ext.apply(pnl.getStore().getProxy().extraParams, params);
                    pnl.getStore().reload();
                }
            }
        }
    ],
    rowLines: true,
    border: false,
    padding: false,
    hideCollapseTool: true,
    enableColumnHide: false,
    enableColumnMove: false,
    enableColumnResize: false,
    columns: [
        {
            text: tr('notifications.history.grid.time'),
            menuDisabled: true,
            dataIndex: 'time',
            width: 160,
            renderer: function(val) {
                var d = new Date(val);
                return Ext.Date.format(d, tr('format.extjs.datetime'));
            }
        },
        {
            text: tr('notifications.history.grid.message'),
            menuDisabled: true,
            dataIndex: 'type',
            flex: 1,
            renderer: function(val, metaData, rec) {
                return '<div style="white-space:normal !important;"><b>' + tr('notifications.history.grid.type') + ':</b> ' + tr(val) + ' &laquo;' + rec.get('name') + '&raquo;<br><b>' + tr('notifications.history.grid.message') + ':</b> ' + tr(rec.get('text')) + '</div>';
            }
        }
//        {text: 'Пользователь', dataIndex: 'user', minWidth: 120}
    ]
});
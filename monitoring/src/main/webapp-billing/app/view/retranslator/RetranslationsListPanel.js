Ext.define('Billing.view.retranslator.RetranslationsListPanel', {
    extend: 'WRExtUtils.WRGrid',
    //extend:'Ext.grid.Panel',
    alias: 'widget.retranslationsListgrid',
    title: 'Ретранслятор',
    requires: [
        'Ext.grid.*',
        'Ext.data.*',
        'Ext.form.field.Text',
        'Ext.toolbar.TextItem'
        //'Ext.ux.grid.FiltersFeature'
    ],
    features: [
        {
            ftype: 'filters',
            encode: true,
            local: false
        }
//        {
//            ftype: 'summary',
//            dock:'top'
//        }
    ],
    dockedToolbar: ['remove', 'refresh'],
    flex: 1,
    storeName: 'EDS.store.RetranslatorsTasks',
    storeAutoSync: false,
    plugins: 'bufferedrenderer',
    invalidateScrollerOnRefresh: false,
    viewConfig: {
        trackOver: false
    },
    selModel: {
        pruneRemoved: false,
        mode: 'MULTI'
    },
    listeners: {
        celldblclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
            var self = this;
            var dataIndex = self.columnManager.columns[cellIndex].dataIndex;
            console.log("celldblclick dataIndex=", dataIndex);
            switch (dataIndex) {
                case "name":
                    this.showRetranslatorForm(record);
                    break;
            }
        }
    },
    initComponent: function () {
        this.callParent();
        this.getSelectionModel().on('selectionchange', this.onSelectChange, this);
    },
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
            header: 'Имя',
            flex: 1,
            sortable: true,
            dataIndex: 'name',
            filter: {
                type: 'string'
            },
            renderer: function (val, metaData, rec) {
                metaData.tdAttr = 'style="cursor: pointer !important;"';
                return val;
            }//,
//        summaryType: 'count',
//             summaryRenderer: function(value, summaryData, dataIndex) {
//                    return Ext.String.format('<b>Всего позиций: {0} </b>', value); 
//             }
        },
        {
            header: 'Хост',
            dataIndex: 'host',
            //align: 'right',
            width: 100
        },
        {
            header: 'Порт',
            dataIndex: 'port',
            //align: 'right',
            width: 100
        },
        {
            header: 'Объекты',
            dataIndex: 'uids',
            //align: 'right',
            width: 100
        },
        {
            header: 'С',
            dataIndex: 'from',
            xtype: 'datecolumn', format: 'Y-m-d',
//align: 'right',
            width: 100
        },
        {
            header: 'По',
            dataIndex: 'to',
            xtype: 'datecolumn', format: 'Y-m-d',
            //align: 'right',
            width: 100
        },
        {
            header: 'Выполнено',
            dataIndex: 'processed',
            //align: 'right',
            width: 100
        },
        {
            header: 'Статус',
            dataIndex: 'status',
            //align: 'right',
            width: 100
        },
        {
            hideable: false,
            menuDisabled: true,
            sortable: false,
            flex: 1
        }
    ],

    onSelectChange: function (selModel, selections) {
        this.down('#delete').setDisabled(selections.length === 0);
    },

    onDeleteClick: function () {
        var selection = this.getView().getSelectionModel().getSelection();
        var self = this;
        Ext.MessageBox.confirm('Удаление элемента', 'Вы уверены, что хотите удалить ' + selection.length + ' ретрансляцию?', function (button) {
            if (button === 'yes') {
                console.log("removing:", selection);
                retranslationTasks.remove(Ext.Array.map(selection, function (m) {
                    return m.get("id");
                }), function (r, e) {
                    if (!e.status) {
                        Ext.MessageBox.show({
                            title: 'Произошла ошибка',
                            msg: e.message,
                            icon: Ext.MessageBox.ERROR,
                            buttons: Ext.Msg.OK
                        });
                    }
                    self.getStore().reload();
                });
            }
        });
    }

});

Ext.define('Billing.view.retranslator.RetranslationsWindow', {
    extend: 'Ext.window.Window',
    title: 'Активные ретрансляции',
    alias: 'widget.retranslationswindow',
    closable: true,
    //constrainHeader: true,
    maximizable: true,
    width: 1024,
    minWidth: 350,
    height: 500,
    layout: {
        type: 'border'
    },
    initComponent: function () {
        Ext.tip.QuickTipManager.init();
        Ext.apply(this,
            {
                items: [
                    Ext.create('Billing.view.retranslator.RetranslationsListPanel', {
                        region: 'center'
                    })
                ]
            });
        this.callParent();
    }
});

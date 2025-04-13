Ext.define('Billing.view.tariff.AllTariffsPanel', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.alltariffsgrid',
    title: 'Тарифы',
    requires: [
        'Billing.view.tariff.TariffForm'
    ],
    features: [
        {
            ftype: 'summary',
            dock: 'top'
        }
    ],
    storeName: 'EDS.store.Tariffs',
    invalidateScrollerOnRefresh: false,
    loadBeforeActivated: false,
    //presaveSelection: true,
    selModel: {
        pruneRemoved: false,
        mode: 'MULTI'
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
            menuDisabled: true,
            sortable: false,
            xtype: 'actioncolumn',
            width: 20,
            resizable: false,
            itemId: 'tariff',
            items: [
                {
                    icon: "extjs-4.2.1/examples/simple-tasks/resources/images/show_complete.png",
                    handler: function (grid, rowIndex, colIndex) {
                    }
                }
            ]
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
            },
            summaryType: 'count',
            summaryRenderer: function (value, summaryData, dataIndex) {
                return Ext.String.format('<b>Всего позиций: {0} </b>', value);
            }
        },
        {
            hideable: false,
            menuDisabled: true,
            sortable: false,
            flex: 1
        }
    ],
    listeners: {
        cellclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
            var self = this;
            var itemId = self.columnManager.columns[cellIndex].itemId;
            if (itemId == "tariff") {
                this.showTariffForm(record, 'edit');
            }
        },
        celldblclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
            var self = this;
            var dataIndex = self.columnManager.columns[cellIndex].dataIndex;
            switch (dataIndex) {
                case "name":
                    this.showTariffForm(record, 'edit');
                    break;
            }
        }
    },
    initComponent: function () {
        this.callParent();
        this.getSelectionModel().on('selectionchange', this.onSelectChange, this);
    },

    showTariffForm: function (record, rule) {
        var self = this;
        var existingWindow;
        var newtariffform;
        if (record) {
            existingWindow = WRExtUtils.createOrFocus('tarWnd' + record.get('_id'), 'Billing.view.tariff.TariffForm.Window', {
                title: 'Тариф "' + record.get('name') + '"',
                tariffid: 'tarWnd' + record.get('_id')
            });
            newtariffform = existingWindow.down('tariffform');
            newtariffform.setActiveRecord(record);
            newtariffform.setRule(rule);
        }
        else {
            existingWindow = Ext.create('Billing.view.tariff.TariffForm.Window', {
                title: 'Тариф '
            });
        }

//        existingWindow.on('save',function(){
//            self.refresh()
//        })

        existingWindow.show();

        return existingWindow.down('tariffform');
    },

    onSelectChange: function (selModel, selections) {
        this.down('#delete').setDisabled(selections.length === 0);
    },

    onAddClick: function () {
        this.showTariffForm();
    },

    onDeleteClick: function () {
        var selection = this.getView().getSelectionModel().getSelection();
        if (selection) {
            Ext.MessageBox.confirm('Удаление элемента', 'Вы уверены, что хотите удалить ' + selection.length + ' тарифов?', function (button) {
                if (button === 'yes') {
                    tariffEDS.remove(Ext.Array.map(selection, function (m) {
                        return m.get("_id");
                    }), function (res, e) {
                        if (!e.status) {
                            Ext.MessageBox.show({
                                title: 'Произошла ошибка',
                                msg: e.message,
                                icon: Ext.MessageBox.ERROR,
                                buttons: Ext.Msg.OK
                            });
                        }
                    })
                }
            });
        }
    }
    ,
    updateData: function (data) {
        this.changeData(data, "tariff");
    }

});
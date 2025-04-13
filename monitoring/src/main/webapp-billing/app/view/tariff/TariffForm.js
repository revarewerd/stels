Ext.define("Billing.view.tariff.TariffForm.Costgrid", {
    extend: "Ext.grid.Panel",
    columns: [
        {text: 'Название услуги', dataIndex: 'name', width: 270, field: {type: 'textfield'}},
        {text: 'Стоимость', dataIndex: 'cost', field: {type: 'textfield'}},
        {text: 'Комментарий', dataIndex: 'comment', flex: 2, field: {type: 'textfield'}}
    ],
    initComponent: function () {

        this.editing = Ext.create('Ext.grid.plugin.CellEditing', {pluginId: "editing"});

        var self = this;

        Ext.apply(this, {
            plugins: [this.editing],
            dockedItems: [
                {
                    xtype: 'toolbar',
                    items: [
                        {
                            icon: 'extjs-4.2.1/examples/restful/images/add.gif',
                            text: 'Добавить',
                            defaultType: 'button',
                            handler: function () {
                                var data = self.getData();
                                data.push({'name': '', "cost": "", "comment": ""});
                                self.setData(data)
                            }
                        },
                        {
                            icon: 'extjs-4.2.1/examples/restful/images/delete.gif',
                            text: 'Удалить',
                            disabled: true,
                            itemId: 'delete',
                            scope: self,
                            handler: function () {
                                var selection = this.getView().getSelectionModel().getSelection()[0];
                                if (selection) {
                                    this.getStore().remove(selection);
                                    self.saveChanges()
                                }
                            }
                        }
                    ]
                }
            ],
            store: Ext.create('Ext.data.Store', {
                fields: ['name', 'cost', 'comment'],
                data: this.data,
                autoSync: true,
                autoLoad: true,
                proxy: {
                    type: 'memory',
                    reader: {
                        type: 'json'
                    }
                }
            })
        });
        this.callParent();
        this.getSelectionModel().on('selectionchange', function (selModel, selections) {
            self.down('#delete').setDisabled(selections.length === 0);
        }, this);
    },

    getData: function () {
        var store = this.getStore();
        return Ext.pluck(store.data.items, 'data');
    },

    saveChanges: function () {
        this.setData(this.getData())
    },
    setData: function (jsonArray) {
        this.data.length = 0;
        Ext.Array.push(this.data, jsonArray);
        this.getStore().reload();
    }

});


Ext.define('Billing.view.tariff.TariffForm', {
    extend: 'Ext.form.Panel',
    alias: 'widget.tariffform',
    initComponent: function () {
        this.addEvents('create');
        Ext.apply(this, {
            activeRecord: null,
            autoScroll: true,
            resizeable: true,
            defaultType: 'textfield',
            fieldDefaults: {
                margin: '0 20 5 20',
                anchor: '100%',
                labelAlign: 'right'
            },
            items: [
                {
                    xtype: 'hiddenfield',
                    name: '_id'
                },
                {
                    margin: '10 20 5 20',
                    fieldLabel: 'Название',
                    name: 'name',
                    allowBlank: false
                },
                {
                    fieldLabel: 'Комментарий',
                    name: 'comment',
                    xtype: 'textareafield',
                    allowBlank: true
                },
                {
                    xtype: 'fieldcontainer',
                    fieldLabel: 'Длина истории сообщений',
                    layout: 'hbox',
                    defaults: {
                        margin: 0
                    },
                    items: [
                        {
                            xtype: 'combobox',
                            labelWidth: 0,
                            itemId: 'messageHistoryLengthType',
                            forceSelection: true,
                            minChars: 0,
                            selectOnTab: true,
                            value: 'unlimited',
                            store: [
                                ['custom', '- пользовательская -'],
                                ['fourHours', '4 часа'],
                                ['oneDay', '1 день'],
                                ['oneWeek', '1 неделя'],
                                ['oneMonth', '1 месяц'],
                                ['threeMonths', '3 месяца'],
                                ['sixMonths', '6 месяцев'],
                                ['oneYear', '1 год'],
                                ['unlimited', 'не ограничена']
                            ],
                            listeners: {
                                change: function (cmb, newValue, oldValue, eOpts) {
                                    var historyLengthField = cmb.up('panel').down('#messageHistoryLengthHours');
                                    historyLengthField.setDisabled(newValue != 'custom');
                                    switch (newValue) {
                                        case 'custom': {
                                            historyLengthField.setValue();
                                            break;
                                        }
                                        case 'fourHours': {
                                            historyLengthField.setValue(4);
                                            break;
                                        }
                                        case 'oneDay': {
                                            historyLengthField.setValue(24);
                                            break;
                                        }
                                        case 'oneWeek': {
                                            historyLengthField.setValue(168);
                                            break;
                                        }
                                        case 'oneMonth': {
                                            historyLengthField.setValue(744);
                                            break;
                                        }
                                        case 'threeMonths': {
                                            historyLengthField.setValue(2208);
                                            break;
                                        }
                                        case 'sixMonths': {
                                            historyLengthField.setValue(4416);
                                            break;
                                        }
                                        case 'oneYear': {
                                            historyLengthField.setValue(8784);
                                            break;
                                        }
                                        case 'unlimited': {
                                            historyLengthField.setValue(-1);
                                            break;
                                        }
                                    }

                                }
                            }
                        },
                        {
                            labelWidth: 50,
                            xtype: "textfield",
                            itemId: 'messageHistoryLengthHours',
                            fieldLabel: 'в часах',
                            value: -1,
                            flex: 1,
                            disabled: true,
                            allowBlank:false
                        }
                    ]
                },
                {
                    xtype: 'fieldcontainer',
                    fieldLabel: 'Тарифы абонентские',
                    items: [
                        Ext.create("Billing.view.tariff.TariffForm.Costgrid", {
                            itemId: 'abonentPrice',
                            data: [
                                {'name': 'Основной абонентский терминал', "cost": "100", "comment": ""},
                                {'name': 'Дополнительный абонетский терминал', "cost": "200", "comment": ""},
                                {'name': 'Спящий блок автономного типа GSM', "cost": "150", "comment": ""},
                                {'name': 'Спящий блок на постоянном питании типа Впайка', "cost": "10", "comment": ""},
                                {'name': 'Радиозакладка', "cost": "50", "comment": ""}
                            ]
                        })
                    ]
                },
                {
                    xtype: 'fieldcontainer',
                    fieldLabel: 'Ежемесячные услуги',
                    items: [
                        Ext.create("Billing.view.tariff.TariffForm.Costgrid", {
                            itemId: 'additionalAbonentPrice',
                            data: []
                        })
                    ]
                },
                {
                    xtype: 'fieldcontainer',
                    fieldLabel: 'Единовременные услуги',
                    items: [
                        Ext.create("Billing.view.tariff.TariffForm.Costgrid", {
                            itemId: 'servicePrice',
                            data: []
                        })
                    ]
                },
                {
                    xtype: 'fieldcontainer',
                    fieldLabel: 'Железо',
                    items: [
                        Ext.create("Billing.view.tariff.TariffForm.Costgrid", {
                            itemId: 'hardwarePrice',
                            data: []
                        })
                    ]
                }
            ],
            dockedItems: [
                {
                    xtype: 'toolbar',
                    dock: 'bottom',
                    ui: 'footer',
                    items: [
                        {
                            text: 'История',
                            itemId: 'history',
                            handler: function () {
                                var self = this;
                                var id = this.up('form').getRecord().get("_id");
                                var existingWindow = WRExtUtils.createOrFocus('TariffPlanEventsPanel' + id, 'Billing.view.event.EventsWindow', {
                                    aggregateId: id,
                                    aggregateType: "TariffPlanAggregate"
                                });
                                existingWindow.show();
                                console.log("existingWindow", existingWindow);
                            }
                        },
                        '->',
                        {
                            icon: 'images/ico16_checkall.png',
                            itemId: 'savenew',
                            text: 'Сохранить как новый',
                            hidden: true,
                            scope: this,
                            handler: function () {
                                this.onSave(true);
                            }
                        },
                        {
                            icon: 'images/ico16_okcrc.png',
                            itemId: 'save',
                            text: 'Сохранить',
                            scope: this,
                            handler: function () {
                                this.onSave(false);
                            }
                        },
                        {
                            icon: 'images/ico16_cancel.png',
                            text: 'Отменить',
                            scope: this,
                            handler: function () {
                                this.up('window').close();
                            }
                        }
                    ]
                }
            ]
        });
        this.callParent();
    },
    onSave: function (asnew) {
        console.log("On Save");
        var self = this;
        var form = self.getForm();
        var values = form.getValues();
        if (values.name instanceof Array)
            values.name = values.name[0];
        if (values.comment instanceof Array)
            values.comment = values.comment[0];
        console.log("form values:", values);
        if (form.isValid()) {
            values.abonentPrice = self.down('#abonentPrice').getData();
            values.servicePrice = self.down('#servicePrice').getData();
            values.hardwarePrice = self.down('#hardwarePrice').getData();
            values.additionalAbonentPrice = self.down('#additionalAbonentPrice').getData();
            values.messageHistoryLengthHours = self.down('#messageHistoryLengthHours').getValue();
            console.log("wnd.id=", self.up('window').tariffid);
            if (asnew || self.up('window').tariffid == null) {
                delete values._id;
                self.updateTarif(self, values);
            }
            else
                Ext.Msg.show({
                    title: 'Сохранить изменения?',
                    msg: 'Вы уверены что хотите поменять тарифный план <i><b>' + values.name + '</b></i>? Тарифный план будет изменен для всех пользователей!',
                    buttons: Ext.Msg.YESNO,
                    icon: Ext.Msg.QUESTION,
                    fn: function (btn, text) {
                        console.log('callback');
                        if (btn == 'yes') {
                            console.log('YES updateTarif');
                            self.updateTarif(self, values);
                        }
                        else {
                            console.log('form onLoad');
                            self.onLoad();
                        }
                    }
                })
        }
    },
    updateTarif: function (self, values) {
        tariffEDS.updateTariff(values, function (res, e) {
            console.log("tariffEDS.updateTariff=", res, e);
            if (!e.status) {
                Ext.MessageBox.show({
                    title: 'Произошла ошибка',
                    msg: e.message,
                    icon: Ext.MessageBox.ERROR,
                    buttons: Ext.Msg.OK
                })
            }
            else {
                self.up('window').fireEvent('save', res/*form.getValues()*/);
                self.up('window').close();
            }
        })
    },
    onLoad: function () {
        var self = this
        tariffEDS.loadTariff(this.getForm().getValues()._id, function (result) {
            console.log("result=", result);
            self.getForm().setValues(result);
            self.down('#abonentPrice').setData(result.abonentPrice);
            self.down('#servicePrice').setData(result.servicePrice);
            self.down('#hardwarePrice').setData(result.hardwarePrice);
            self.down('#additionalAbonentPrice').setData(result.additionalAbonentPrice);
            self.down('#messageHistoryLengthType').setValue(result.messageHistoryLengthType);
            self.down('#messageHistoryLengthHours').setValue(result.messageHistoryLengthHours);

        })
    },
    setActiveRecord: function (record) {
        var self = this
        this.getForm().loadRecord(record);
        this.onLoad();
    },
    setRule: function (rule) {
        var self = this
        if (rule == 'view') {
            self.down('#history').setVisible(false);
            self.down('#save').setVisible(false);
            var formItems = self.items.items;
            for (var i = 0; i < formItems.length; i++) {
                var xtype = formItems[i].getXType();
                console.log("xtype=" + xtype);
                if (xtype.indexOf("text") != -1)
                    formItems[i].setReadOnly(true);
                else if (xtype == "fieldcontainer") {
                    var cItems = formItems[i].down("panel");
                    if (cItems) {
                        var dockedComponent = cItems.getDockedComponent(1);
                        cItems.removeDocked(dockedComponent);
                        cItems.getSelectionModel().clearListeners();
                        cItems.getPlugin("editing").on("beforeedit", function (editor, e) {
                            e.cancel = true;
                        })
                    }
                    else {
                        cItems = formItems[i].items.items;
                        for (var j = 0; j < cItems.length; j++) {
                            cItems[j].setReadOnly(true);
                        }
                    }
                }
            }
        }
        if (rule == 'edit') {
            self.down('button[itemId=savenew]').setVisible(true);
        }
    }
});

Ext.define('Billing.view.tariff.TariffForm.Window', {
    extend: 'Ext.window.Window',
    width: 800,
    maximizable: true,
    layout: 'fit',
    icon: "extjs-4.2.1/examples/simple-tasks/resources/images/show_complete.png",
    items: [
        {
            xtype: 'tariffform',
            closeAction: function () {
                this.up('window').close();
            }
        }
    ]

});

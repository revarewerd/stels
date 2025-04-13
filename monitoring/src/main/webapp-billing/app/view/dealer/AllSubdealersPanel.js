Ext.define('Billing.view.dealers.DealerForm', {
    extend: 'Ext.form.Panel',
    alias: 'widget.dealerform',
    closeAction: Ext.emptyFn,
    header: false,
    initComponent: function(){
        this.addEvents('create');
        var self = this;
        console.log('hideRule', self.hideRule);

        Ext.apply(this, {
            activeRecord: null,
            layout: {
                type: 'accordion',
                multi: true,
                titleCollapse: true,
                hideCollapseTool: true
            },
            items: [
                {
                    xtype: 'panel',
                    title: '<font color="1a4780"><b>Основное</b></font>',
                    bodyPadding: '10px',
                    autoScroll: true,
                    layout: {
                        type: 'vbox',
                        align: 'stretch'
                    },
                    defaults: {
                        margin: '0 10 5 0',
                        labelWidth: 150
                    },
                    items: [
                        {
                            xtype: 'displayfield',
                            fieldLabel: 'Название',
                            itemId: 'accountField',
                            name: 'id',
                            allowBlank: false,
                            readOnly: self.hideRule
                        },
                        {
                            xtype: 'displayfield',
                            fieldLabel: 'Аккаунтов',
                            itemId: 'accounts',
                            name: 'accounts',
                            hidden: self.hideRule
                        },
                        {
                            xtype: 'displayfield',
                            fieldLabel: 'Объектов',
                            itemId: 'objects',
                            name: 'objects',
                            readOnly: self.hideRule
                        },
                        {
                            xtype: 'displayfield',
                            fieldLabel: 'Оборудования',
                            itemId: 'equipments',
                            name: 'equipments',
                            readOnly: self.hideRule
                        },
                        {
                            fieldLabel: 'Абонентская плата',
                            xtype: 'displayfield',
                            width: 50,
                            name: 'cost',
                            renderer: function(value){
                                return '<div class="ballink">' + accounting.formatMoney(parseFloat(value / 100)) + '</div>';
                            },
                            listeners: {
                                afterrender: function(component){
                                    component.getEl().on('click', function(){
                                        self.showMonthlyPayment();
                                    });
                                }
                            }
                        },
                        {
                            xtype: 'displayfield',
                            fieldLabel: 'Баланс',
                            name: 'balance',
                            listeners: {
                                afterrender: function(component){
                                    component.getEl().on('click', function(){
                                        self.showBalancePanel();
                                    });
                                }
                            },
                            renderer: function(value){
                                return '<div class="ballink">' + accounting.formatMoney(parseFloat(value / 100)) + '</div>';
                            }
                        },
                        {
                            xtype: 'textfield',
                            fieldLabel: 'Базовый тариф',
                            name: 'baseTariff'
                        }
                        //{
                        //    xtype: 'grid',
                        //    itemId: 'tariffication',
                        //    title: 'Тарификация',
                        //    columns: [
                        //        {text: 'Название услуги', dataIndex: 'name', width: 270, field: {type: 'textfield'}},
                        //        {text: 'Стоимость', dataIndex: 'cost', field: {type: 'textfield'}}//,
                        //        // {text: 'Комментарий', dataIndex: 'comment', flex: 2, field: {type: 'textfield'}}
                        //    ],
                        //    plugins: [Ext.create('Ext.grid.plugin.CellEditing', {pluginId: "editing"})],
                        //    store: Ext.create('Ext.data.Store', {
                        //        fields: ['name', 'cost'/*, 'comment'*/],
                        //        data: [
                        //            {'name': 'Терминал', "cost": "100"/*, "comment": ""*/},
                        //            {'name': 'Спящий блок', "cost": "200"/*, "comment": ""*/},
                        //            {'name': 'SMS', "cost": "150"/*, "comment": ""*/}
                        //        ],
                        //        autoSync: true,
                        //        autoLoad: true,
                        //        proxy: {
                        //            type: 'memory',
                        //            reader: {
                        //                type: 'json'
                        //            }
                        //        }
                        //    }),
                        //    getData: function(){
                        //        var store = this.getStore();
                        //        return Ext.pluck(store.data.items, 'data');
                        //    },
                        //    saveChanges: function(){
                        //        this.setData(this.getData());
                        //    },
                        //    setData: function(jsonArray){
                        //        this.data.length = 0;
                        //        Ext.Array.push(this.data, jsonArray);
                        //        this.getStore().reload();
                        //    }
                        //}
                    ]
                }
            ],
            dockedItems: [
                {
                    xtype: 'toolbar',
                    dock: 'bottom',
                    ui: 'footer',
                    items: [
                        '->', {
                            icon: 'images/ico16_okcrc.png',
                            itemId: 'save',
                            disabled: self.hideRule,
                            text: 'Сохранить',
                            scope: this,
                            handler: this.onSave
                        }, {
                            icon: 'images/ico16_cancel.png',
                            text: 'Отменить',
                            scope: this,
                            handler: function(){
                                self.up('window').close();
                            }
                        }
                    ]
                }
            ],
            listeners: {
                afterrender: function(form, eopts){
                    console.log("Form rendered ", form.getRecord());
                    //var _id = form.getRecord().get("_id");
                    //console.log("Record _id=", _id);
                    //var name = form.getRecord().get("name");
                    //console.log("Record name=", name);
                    //var accountType = form.getRecord().get("accountType");
                    //var currentAcc = Ext.create('Account', {_id: _id, name: name, accountType: accountType});
                    //console.log("Form loadRec=", form.getForm().loadRecord(currentAcc));
                    form.onLoad();
                }
            }

        });
        this.callParent();
    },
    showMonthlyPayment:function(){
        var self=this;
        var dealer=self.getRecord().get("id")
        if (dealer) {
            var mpf = WRExtUtils.createOrFocus('MPFWnd' + dealer, 'Billing.view.MonthlyPaymentWindow', {
                title: "Ежемесячные начисления: \"" + dealer + "\"",
                detailsStore: Ext.create('EDS.store.DealerMonthlyPaymentService', {
                    autoLoad: true,
                    groupField: 'uid',
                    listeners: {
                        beforeload: function (store, op) {
                            console.log('beforeload dealer', dealer);
                            store.getProxy().setExtraParam("dealer", dealer);
                        }
                    }
                })
            });
            mpf.show();
        }
        else {
            Ext.MessageBox.show({
                title: 'Учетная запись еще не создан',
                msg: 'Невозможно просмотреть абонентскую плату несуществующей учетной записи',
                buttons: Ext.MessageBox.OK,
                icon: Ext.MessageBox.WARNING
            });
        }
    },
    showBalancePanel: function(){
        var self=this;
        var dealerId = self.getRecord().get("id");
        if (dealerId) {
            var bhw = WRExtUtils.createOrFocus('BhWWnd' + dealerId, 'Billing.view.BalanceHistoryWindow', {
                title: "Баланс учетной записи: \"" + dealerId + "\"",
                accountId: dealerId,
                hideRule:self.hideRule,
                formsubmitMethod: dealersBalanceChange.dealerbalanceChange,
                itemType: 'dealerBalance',
                operationsstore: Ext.create("Ext.data.Store",{
                    fields:['name','cost'],
                    data: [
                        {name: 'Зачислить', cost: 100},
                        {name: 'Снять',  cost: -100}
                    ]
                })
            });
            bhw.show(self);
        }
        else {
            Ext.MessageBox.show({
                title: 'Учетная запись еще не создан',
                msg: 'Невозможно просмотреть баланс несуществующей учетной записи',
                buttons: Ext.MessageBox.OK,
                icon: Ext.MessageBox.WARNING
            });
        }
    },
    setActiveRecord: function(record){
        var self = this;
        this.getForm().loadRecord(record);
        dealersService.getDealerParams(record.get('id'), function(resp){
            console.log("getDealerParams resp:", resp);
            resp['baseTariff'] = resp['baseTariff'] / 100;
            self.getForm().setValues(resp);
        });

        //dealersService.getTariffication(record.get('id'), function(resp){
        //    console.log("getTariffication resp=", resp);
        //    var storeData = [];
        //    for (var rec in resp) {
        //        storeData.push({"name": rec, "cost": (resp[rec] / 100)});
        //    }
        //    console.log("getTariffication storeData=", storeData);
        //    var down = self.down("#tariffication");
        //    var store = down.getStore();
        //    store.loadData(storeData);
        //});
    },
    onSave: function(){
        console.log("On Save");
        var self = this;
        var form = self.getForm();
        //var cForm = self.down('[itemId=contractPanel]');
        console.log('form=', form);
        if (form.isValid()) {
            //var tariffication = self.down("#tariffication").getData();
            //console.log("tariffication=", tariffication);
            var rec = form.getRecord();
            console.log("rec=", rec);
            var recData = Ext.apply(rec.getData(),form.getValues());
            //recData.tariffication = tariffication;
            console.log("recData=", recData);
            recData['baseTariff'] = recData['baseTariff'] * 100;
            dealersService.updateDealerParams(recData, function(submitResult, e){
                if (e.type === "exception") {
                    console.log("exception=", e);
                    Ext.MessageBox.show({
                        title: 'Произошла ошибка',
                        msg: e.message,
                        icon: Ext.MessageBox.ERROR,
                        buttons: Ext.Msg.OK
                    });
                }
                else {
                    console.log("Результат запроса", submitResult, " e=", e);
                    if (submitResult['code'] == 2) {
                        Ext.Msg.alert('Запрос выполнен ', submitResult['msg']);
                    }
                    else {
                        self.fireEvent('save');
                        self.up('window').close();
                    }

                }
            });
        }
        else (Ext.Msg.alert('В запросе отказано', 'Заполните форму корректными данными'));
    },
    onLoad: function(){
        console.log("On load");
        var self = this;
        var form = this.getForm();
        //var accountId = this.getRecord().get("_id");
        //console.log("accountId=", accountId);
        //var cForm = this.down('[itemId=contractPanel]');

    }
});

Ext.define('Billing.view.dealer.DealerForm.Window', {
    extend: 'Ext.window.Window',
    alias: 'widget.dealerwnd',
    title: 'Дилер',
    icon: 'extjs-4.2.1/examples/shared/icons/fam/user_suit.gif',
    width: 800,
    height: 700,
    maximizable: true,
    layout: 'fit',
    initComponent: function(){
        var self = this;
        Ext.apply(this, {
            items: [
                {
                    xtype: 'dealerform',
                    hideRule: self.hideRule,
                    closeAction: function(){
                        this.up('window').close();
                    }
                }
            ]
        });
        this.callParent();
    }
});


Ext.define('Billing.view.dealer.AllSubdealersPanel', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.allsubdealersgrid',
    title: 'Дилеры',
    requires: [
        //'Billing.view.user.UserForm'
    ],
    features: [
        {
            ftype: 'summary',
            dock: 'top'
        }
    ],
    storeName: 'EDS.store.DealersService',
    invalidateScrollerOnRefresh: false,
    itemType: 'dealers',
    dockedToolbar: [/*'add', 'remove', */'refresh', 'search', 'fill'/*, 'gridDataExport'*/],
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
            dataIndex: 'id',
            summaryType: 'count',
            filter: {
                type: 'string'
            },
            renderer: function(val, metaData, rec){
                metaData.tdAttr = 'style="cursor: pointer !important;"';
                return val;
            }//,
        },
        // "name", "accounts", "objects", "equipments"
        {
            header: 'Аккаунтов',
            width: 150,
            sortable: false,
            dataIndex: "accounts",
            summaryType: 'sum',
            align: 'left'
        },
        {
            header: 'Объектов',
            width: 150,
            sortable: false,
            dataIndex: "objects",
            summaryType: 'sum',
            align: 'left'
        },
        {
            header: 'Оборудования',
            width: 150,
            sortable: false,
            dataIndex: "equipments",
            summaryType: 'sum',
            align: 'left'
        },
        {
            text: 'Блокировка',
            tooltip: 'Блокировка дилера',
            tooltipType: 'title',
            sortable: true,
            resizable: false,
            dataIndex: 'block',
            width: 70,
            renderer: function (val, metaData, rec) {
                metaData.tdAttr = 'style="cursor: pointer !important;"'
                if (val === true) return '<img src="images/ico16_lock.png" alt="" title="Разблокировать" />'
                else  return '<img src="images/ico16_unlock.png" alt="" title="Заблокировать" />'
            }
        },
        {
            header: 'Войти',
            width: 150,
            sortable:false,
            dataIndex: "id",
            align: 'left',
            renderer: function (value) {
                return '<a href="EDS/dealerbackdoor?dealer=' + encodeURIComponent(value) + '" target="slavedealer"> Войти </a>';
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
            var self=this
            var dataIndex=self.columnManager.columns[cellIndex].dataIndex
            if (dataIndex =="block") {
                var prevBlock=record.get("block")
                if(prevBlock == null || prevBlock == undefined) prevBlock=false
                this.dealerBlocking(record.get("id"),!prevBlock)
            }
        },
        celldblclick: function(/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts){
            var self = this;

            var existingWindow = WRExtUtils.createOrFocus('dealerWnd' + record.get('id'), 'Billing.view.dealer.DealerForm.Window', {
                title: 'Дилер "' + record.get('id') + '"',
                id: 'dealerWnd' + record.get('id'),
                hideRule: !self.viewPermit
            });
            if (existingWindow.justCreated) {
                var newaccform = existingWindow.down('dealerform');
                newaccform.setActiveRecord(record);
            }
            existingWindow.show();
        }
    },

    initComponent: function(){
        this.callParent();
        this.getSelectionModel().on('selectionchange', this.onSelectChange, this);
    },
    dealerBlocking: function(id,block){
        var self = this;
        var blockWord="разблокировать"
        if(block) blockWord = "заблокировать"
        Ext.MessageBox.show({
            title: 'Блокировка дилера',
            buttons:  Ext.MessageBox.YESNO,
            msg: 'Вы уверены, что хотите ' + blockWord+' дилера: '+id,
            fn: function(btn) {
                if(btn == 'yes') {
                    dealersService.dealerBlocking(id,block, function(resp,e){
                        console.log("dealerBlocking resp:", resp);
                        if (!e.status) {
                            Ext.MessageBox.show({
                                title: 'Произошла ошибка',
                                msg: e.message,
                                icon: Ext.MessageBox.ERROR,
                                buttons: Ext.Msg.OK
                            });
                        }
                        else {
                            self.refresh()
                        }
                    });
                }
            }
        });

    }
    //onSelectChange: function (selModel, selections) {
    //    this.down('#delete').setDisabled(selections.length === 0);
    //}

});

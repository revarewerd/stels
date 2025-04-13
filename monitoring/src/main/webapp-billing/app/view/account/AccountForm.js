Ext.define('Billing.view.account.AccountForm', {
    extend: 'Ext.form.Panel',
    alias: 'widget.accform',
    requires: [
        'Billing.view.BalanceHistory',
        'Billing.view.account.ContractULForm',
        'Billing.view.account.ContractFLForm',
        'WRExtUtils.WRRadioGroup'
    ],
    closeAction: Ext.emptyFn,
    header: false,
    initComponent: function () {
        this.addEvents('create');
        var self = this;
        console.log('hideRule',self.hideRule)

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
                    layout: {type: 'vbox',
                        align: 'stretch'},                    
                    defaults: { margin: '0 10 5 0', 
                                labelWidth: 150},
                    items: [
                        {
                            xtype: 'textfield',
                            fieldLabel: 'Учетная запись',
                            itemId: 'accountField',
                            name: 'name',
                            allowBlank: false,
                            readOnly:self.hideRule
                        },
                        {
                            xtype: 'textfield',
                            fieldLabel: 'Комментарий',
                            itemId: 'comment',
                            name: 'comment',
                            hidden:self.hideRule
                        },
                        {
                            xtype: 'textfield',
                            fieldLabel: 'Полное наименование',
                            itemId: 'fullClientName',
                            name: 'fullClientName',
                            readOnly:self.hideRule
                        },

                        {   xtype: 'fieldcontainer',
                            layout: 'hbox',
                            items: [
                                {
                                    fieldLabel: 'Тариф',
                                    readOnly:self.hideRule,
                                    xtype: 'combobox',
                                    labelWidth: 150,     
                                    width:400,
                                    name: 'plan',
                                    allowBlank: false,
                                    forceSelection: true,
                                    triggerAction: 'all',
                                    minChars: 0,
                                    selectOnTab: true,
                                    itemId: "tariffplan",
                                    store: Ext.create('EDS.store.Tariffs', {
                                        autoLoad: true,
                                        buffered:false
                                    }),
                                    displayField: 'name',
                                    valueField: '_id'
                                },
                                {xtype: 'splitter'},
                                {xtype: 'button',                                    
                                    text: 'Показать',
                                    handler: function (button) {
                                        self.showTariffPanel()
                                    }
                                }
                            ]},
                        {
                            fieldLabel: 'Способ оплаты',
                            readOnly:self.hideRule,
                            xtype: 'combobox',
                            labelWidth: 150,
                            width:400,
                            name: 'paymentWay',
                            forceSelection: true,
                            triggerAction: 'all',
                            minChars: 0,
                            selectOnTab: true,
                            store: [
                                ['requisite','по реквизитам'],
                                ['card','на карточку'],
                                ['yandexPayment','платежная система Yandex']
                            ],
                            displayField: 'name',
                            valueField: '_id'
                        },
                        { fieldLabel: 'Абонентская плата',
                            xtype:'displayfield',
                            width:50,
                            name:'cost',
                            renderer: function (value) {
                                return '<div class="ballink">' + accounting.formatMoney(parseFloat(value / 100)) + '</div>';
                            },
                            listeners: {
                                afterrender: function (component) {
                                    component.getEl().on('click', function () {
                                        self.showMonthlyPayment()
                                    });
                                }
                            }
                        },
                        {
                            xtype: 'fieldcontainer',
                            fieldLabel: 'Лимит задолженности',
                            itemId:'limit',
                            layout: 'column',
                            labelWidth: 150,
                            hidden:self.hideRule,
                            items: [
                                   {xtype: 'wrradiogroup',
                                       itemId:'limitType',
                                       columns: 1,
                                       defaults: {
                                          margin:'0 0 5 0'
                                       },
                                       initItemsConfig: [
                                           {boxLabel: 'на кол-во дней', inputValue: "daysLimit", padding: '0 12 0 0'},
                                           {boxLabel: 'на сумму до (руб.)', inputValue: 'currencyLimit', padding: '0 12 0 0'},
                                           {boxLabel: 'в % от абонентской платы', inputValue: 'percentMonthlyFeeLimit', padding: '0 12 0 0'}
                                       ],
                                       listeners: {
                                           change: function(radio, newVal, oldVal) {
                                               var value=radio.getRealValue()
                                               console.log("value",value)
                                               var limitContainer=radio.up("#limit")
                                               var dayslimitValue=limitContainer.down("#daysLimitValue")
                                               var currencylimitValue=limitContainer.down("#currencyLimitValue")
                                               var percentMonthlyFeelimitValue=limitContainer.down("#percentMonthlyFeeLimitValue")
                                               switch(value) {
                                                   case "daysLimit" : {
                                                       dayslimitValue.setDisabled(false)
                                                       currencylimitValue.setDisabled(true)
                                                       percentMonthlyFeelimitValue.setDisabled(true)
                                                       break;
                                                   }
                                                   case "currencyLimit" : {
                                                       dayslimitValue.setDisabled(true)
                                                       currencylimitValue.setDisabled(false)
                                                       percentMonthlyFeelimitValue.setDisabled(true)
                                                       break;
                                                   }
                                                   case "percentMonthlyFeeLimit" : {
                                                       dayslimitValue.setDisabled(true)
                                                       currencylimitValue.setDisabled(true)
                                                       percentMonthlyFeelimitValue.setDisabled(false)
                                                       break;
                                                   }
                                               }
                                           }
                                       }
                                   },
                                {xtype: 'fieldcontainer',
                                    itemId:'limitValue',
                                    margin: '0 0 0 40',
                                    layout: 'vbox',
                                    defaultType: 'textfield',
                                    items: [
                                        {
                                            itemId: "daysLimitValue",
                                            regex:/^\d+$/i,
                                            validateBlank:true,
                                            disabled: true
                                        },
                                        {
                                            itemId: "currencyLimitValue",
                                            regex:/^\-?\d+$/i,
                                            validateBlank:true,
                                            disabled: true
                                        },
                                        {
                                            itemId: "percentMonthlyFeeLimitValue",
                                            regex:/^\d+$/i,
                                            validateBlank:true,
                                            disabled: true
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            xtype: 'displayfield',
                            fieldLabel: 'Баланс',
                            name: 'balance',
                            listeners: {
                                afterrender: function (component) {
                                    component.getEl().on('click', function () {
                                        self.showBalancePanel()
                                    });
                                }
                            },
                            renderer: function (value) {
                                return '<div class="ballink">' + accounting.formatMoney(parseFloat(value / 100)) + '</div>';
                            }
                        },
                        {   
                            xtype: 'fieldcontainer',
                            fieldLabel: 'Включен',
                            layout: 'hbox',
                            labelWidth: 150,                            
                            items: [
                                  {
                                      xtype: 'checkbox',
                                      readOnly:self.hideRule,
                                      name:'status',
                                      inputValue:'status',
                                      listeners:{
                                          change:function(chbox, newValue, oldValue, eOpts){
                                              if(!newValue)
                                                  this.up('fieldcontainer').down('field[name=blockcause]').enable()
                                              else
                                                  this.up('fieldcontainer').down('field[name=blockcause]').disable()
                                          }
                                      }
                                  },
                                  {
                                      margin: '0 0 0 20',
                                      xtype: 'textfield',
                                      fieldLabel: 'Причина блокировки',
                                      labelWidth:130,
                                      flex:1,
                                      name:'blockcause',
                                      itemId:'blockcause'
                                  }
                            ]                            
                        },
                        {  xtype: 'fieldcontainer',
                            layout: 'hbox',
                            items: [
                                {
                                    xtype: 'displayfield',
                                    labelWidth: 150,
                                    width:150,
                                    fieldLabel: 'Пользователи',
                                    name: 'userscount',
                                    value: 'Недоступно',
                                    listeners: {
                                        afterrender: function (comp) {
                                            self.loadUsers(self,comp);
                                        }
                                    }
                                },
                                {xtype: 'button',
                                    margin: '0 0 0 40',
                                    text: 'Показать',
                                    handler: function () {
                                        self.showUsersPanel(); 
                                    }
                                }
                        ]},
                        {   xtype: 'fieldcontainer',
                            layout: 'hbox',
                            items: [
                                {
                                    xtype: 'displayfield',
                                    labelWidth: 150,
                                    width:150,
                                    fieldLabel: 'Объекты',
                                    name: 'objcount',
                                    value: 'Недоступно',
                                    listeners: {
                                        afterrender: function (comp) {
                                                self.loadObjects(self,comp);
                                        }
                                    }
                                },
                                {xtype: 'button',
                                    margin: '0 0 0 40',
                                    text: 'Показать',
                                    handler: function () {                                        
                                        self.showObjectsPanel();
                                    }
                                }
                            ]},
                        {   xtype: 'fieldcontainer',
                            layout: 'hbox',
                            items: [
                                {
                                    xtype: 'displayfield',
                                    labelWidth: 150,
                                    width:150,
                                    fieldLabel: 'Оборудование',
                                    name: 'eqcount',
                                    value: 'Недоступно',
                                    listeners: {
                                        afterrender: function (comp) {
                                                self.loadEquipments(self,comp)
                                        }
                                    }
                                },
                                {xtype: 'button',
                                    margin: '0 0 0 40',
                                    text: 'Показать',
                                    handler: function () {                                        
                                        self.showEquipmentsPanel();
                                    }
                                }
                            ]}
                    ]
                },
                {
                    xtype: 'form',
                    hidden:self.hideRule,
                    title: '<font color="1a4780"><b>Контакты</b></font>',
                    collapsed: true,
                    autoScroll: true,
                    layout: {type: 'vbox',
                        align: 'stretch'},
                    fieldDefaults: {
                        labelWidth: 150,
                        margin: '0 20 5 20'
                    },
                    defaultType: 'textfield',
                    items: [
                        {   margin: '10 20 5 20',
                            fieldLabel: 'Фамилия',
                            //vtype: 'rualpha',
                            name: 'cffam'
                        },
                        {
                            fieldLabel: 'Имя',
                            //vtype:'allalpha',
                            name: 'cfname'
                        },
                        {
                            fieldLabel: 'Отчество',
                            //vtype:'allalpha',
                            name: 'cffathername'
                        },
                        {
                            vtype: 'phone',
                            fieldLabel: 'Мобильный телефон 1',
                            name: 'cfmobphone1'},
                        {
                            vtype: 'phone',
                            fieldLabel: 'Мобильный телефон 2',
                            name: 'cfmobphone2'},
                        {
                            vtype: 'phone',
                            fieldLabel: 'Рабочий телефон',
                            name: 'cfworkphone1'},
                        {
                            fieldLabel: 'e-mail',
                            name: 'cfemail',
                            vtype: 'email' },
                        {
                            xtype: 'textareafield',
                            fieldLabel: 'Примечание',
                            name: 'cfnote'}
                    ]
                },
                {
                    xtype: 'form',
                    hidden:self.hideRule,
                    title: '<font color="1a4780"><b>Договора</b></font>',
                    collapsed: true,
                    autoScroll: true,
                    layout: {type: 'vbox',
                        align: 'stretch'},                    
                    fieldDefaults: {
                        labelWidth: 250
                    },
                    dockedItems: [
                        {
                            xtype: 'toolbar',
                            dock: 'right',
                            align: 'left',
                            items: [
                                {
                                    xtype: 'button',
                                    disabled:self.hideRule,
                                    icon: 'extjs-4.2.1/examples/restful/images/add.gif',
                                    tooltip: 'Добавить договор',
                                    handler: function () {
                                        self.addContract()
                                    }
                                },
                                {
                                    xtype: 'button',
                                    disabled:self.hideRule,
                                    tooltip: 'Удалить договор',
                                    icon: 'extjs-4.2.1/examples/restful/images/delete.gif',
                                    handler: function () {
                                        self.removeContract()
                                    }
                                }
                            ]
                        }
                    ],
                    items: [
                        {xtype: 'tabpanel',
                            border: false,
                            contractCount: 0,
                            itemId: 'contractPanel'
                        }
                    ]
                }
            ],
            dockedItems: [
                {
                    xtype: 'toolbar',
                    dock: 'bottom',
                    ui: 'footer',
                    items: [
                        {text: 'История',
                         disabled:self.hideRule,
                         handler: function () {
                            var self=this; 
                            var id=this.up('form').getRecord().get("_id");
                            var existingWindow = WRExtUtils.createOrFocus('AccountEventsPanel' + id, 'Billing.view.event.EventsWindow', {
                                aggregateId:id,
                                aggregateType:"AccountAggregate"
                            });
                            existingWindow.show();
                            console.log("existingWindow",existingWindow)
                        }
                        },                        
                        '->', {
                        icon: 'images/ico16_okcrc.png',
                        itemId: 'save',
                        disabled:self.hideRule,
                        text: 'Сохранить',
                        scope: this,
                        handler: this.onSave
                    }, {
                        icon: 'images/ico16_cancel.png',
                        text: 'Отменить',
                        scope: this,
                        handler: function () {
                            self.up('window').close();
                        }
                    }
                    ]
                }
            ],
            listeners: {
                afterrender: function (form, eopts) {
                    console.log("Form rendered ", form.getRecord());
                    var _id = form.getRecord().get("_id");
                    console.log("Record _id=", _id);
                    var name = form.getRecord().get("name");
                    console.log("Record name=", name);
                    var accountType = form.getRecord().get("accountType");
                    var currentAcc = Ext.create('Account', {_id: _id, name: name, accountType: accountType});
                    console.log("Form loadRec=", form.getForm().loadRecord(currentAcc));
                    form.onLoad();
                }
            }

        });
        this.callParent();
    },
    loadUsers:function (self,comp) {
      if (self.getRecord().get("_id")) {
        usersPermissionsService.getPermittedUsersCount(self.getRecord().get("_id"),"account", function (result, e) {
            comp.setValue(result);                                                
        });
      }
    },
    loadObjects:function (self,comp) {
     if (self.getRecord().get("_id")) {
            accountInfo.getObjectsStat(self.getRecord().get("_id"), function (result, e) {
                comp.setValue(result.objectsCount);
                console.log("result.objectsCount",result.objectsCount)
            });
        }   
    },
    loadEquipments:function (self,comp) {
     if (self.getRecord().get("_id")) {
            accountInfo.getEquiupmentsStat(self.getRecord().get("_id"), function (result, e) {
                comp.setValue(result.equipmentsCount); 
                console.log("result.equipmentsCount",result.equipmentsCount)
            });
        }   
    },
    showBalancePanel: function(){
        var self=this
        if (self.getRecord().get("_id")) {
            var bhw = WRExtUtils.createOrFocus('BhWWnd' + self.getRecord().get("_id"), 'Billing.view.BalanceHistoryWindow', {
                title: "Баланс учетной записи: \"" + self.getRecord().get("name") + "\"",
                accountId: self.getRecord().get("_id"),
                hideRule:self.hideRule
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
    showMonthlyPayment:function(){
        var self=this
        var id=self.getRecord().get("_id")
        if (id) {
            var mpf = WRExtUtils.createOrFocus('MPFWnd' + id, 'Billing.view.MonthlyPaymentWindow', {
                title: "Ежемесячные начисления: \"" + name + "\"",
                accountId: id
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
    showEquipmentsPanel: function () {
        var self=this
        if (self.getRecord().get("_id")) {
            var v = WRExtUtils.createOrFocus('EqWnd' + self.getRecord().get('_id'), 'Billing.view.equipment.AccountEquipmentsWindow', {
                hideRule:self.hideRule,
                accountId: self.getRecord().get('_id'),
                title: "Оборудование учетной записи \"" + self.getRecord().get('name') + "\""
            });
            v.on('close',function(){ self.loadEquipments(self,self.down('[name=eqcount]'))})
            v.show(self);
        }
        else {
            Ext.MessageBox.show({
                title: 'Учетная запись еще не создана',
                msg: 'Невозможно просмотреть список объектов несуществующей учетной записи',
                buttons: Ext.MessageBox.OK,
                icon: Ext.MessageBox.WARNING
            });
        }

    },
    showObjectsPanel: function () {
        var self=this
        if (self.getRecord().get("_id")) {
            var v = WRExtUtils.createOrFocus('ObjWnd' + self.getRecord().get('_id'), 'Billing.view.object.AccountObjectsWindow', {
                accountId: self.getRecord().get('_id'),
                title: "Объекты учетной записи \"" + self.getRecord().get('name') + "\"",
                hideRule:self.hideRule
            });
            v.on('close',function(){ 
                self.loadObjects(self,self.down('[name=objcount]'))
                self.loadEquipments(self,self.down('[name=eqcount]'))
            })
            v.show(self);
        }
        else {
            Ext.MessageBox.show({
                title: 'Учетная запись еще не создана',
                msg: 'Невозможно просмотреть список объектов несуществующей учетной записи',
                buttons: Ext.MessageBox.OK,
                icon: Ext.MessageBox.WARNING
            });
        }

    },
     showUsersPanel: function () {
        var self=this
        if (self.getRecord().get("_id")) {
            var v = WRExtUtils.createOrFocus('UsrWnd' + self.getRecord().get('_id'), 'Billing.view.user.UsersWindow', {
                oid: self.getRecord().get('_id'),                
                type:'account',
                title: "Пользователи учетной записи \"" + self.getRecord().get('name') + "\""
            });
            v.on('close',function(){ self.loadUsers(self,self.down('[name=userscount]'))})
            v.show(self);
        }
        else {
            Ext.MessageBox.show({
                title: 'Учетная запись еще не создана',
                msg: 'Невозможно просмотреть список пользователей владеющих несуществующей учетной записью',
                buttons: Ext.MessageBox.OK,
                icon: Ext.MessageBox.WARNING
            });
        }

    },
    showTariffPanel:function(){
        var self=this
        var tariffield = self.down('field[name=plan]');
        if (tariffield.getValue()) {
            var existingWindow = WRExtUtils.createOrFocus('TarWnd' + self.getRecord().get('_id'), 'Billing.view.tariff.TariffForm.Window', {
                title:'Тариф "'+tariffield.getRawValue()+'"'
            });
            var newtariffform = existingWindow.down('tariffform');
            newtariffform.setActiveRecord(tariffield.store.getById(tariffield.getValue()));                                        
            newtariffform.setRule('view'); 
            existingWindow.show(self);
        }
        else {
            Ext.MessageBox.show({
                title: 'Тариф неуказан',
                msg: 'Укажите тариф',
                buttons: Ext.MessageBox.OK,
                icon: Ext.MessageBox.WARNING
            });
        }
    },
    addContract:function(){      
        var form = this.getForm();
        var cForm = this.down('[itemId=contractPanel]');
        var contractType = form.getRecord().get('accountType');
        var clientWndType = 'ulform';
        var contractModel = 'ContractUL';
        if (contractType == 'fiz') {
            clientWndType = 'flform';
            contractModel = 'ContractFL';
        }        
        console.log("clientWndType=", clientWndType);
        if (!cForm.contractCount) {
            cForm.contractCount = 0;
        }
        console.log("cForm.contactCount=", cForm.contactCount);
        var tabId = ++cForm.contractCount;
        cForm.add([
        {
            xtype: clientWndType, 
            tabId: tabId, 
            title: 'Договор ' + tabId
            }
        ]);
        cForm.setActiveTab(tabId - 1);
        var currentDate = Ext.Date.format(new Date(), "КСБYmdHis.u");
        var currentContr = Ext.create(contractModel, {
            accountid: form.getRecord().get('_id'),
            conAccount: form.getRecord().get('name'),
            _id: currentDate,
            conNumber: currentDate,
            conDate: new Date()
            });
        console.log("Form loadRec=", cForm.items.getAt(tabId - 1).getForm().loadRecord(currentContr));
    },
    removeContract:function(){        
        var cForm = this.up('window').down('[itemId=contractPanel]');
        console.log('contactForm=', cForm);                                        
        if (cForm.remove(cForm.getActiveTab()) && cForm.contractCount > 0)
            cForm.contractCount--;
        var tab;
        for (var i = 0; i < cForm.contractCount; i++) {
            tab = cForm.items.getAt(i);
            tab.setTitle("Договор " + (i + 1));
            tab.tabId = i;
        }
    },
    setActiveRecord: function (record) {
        this.activeRecord = record;
        this.getForm().loadRecord(record);
    },
    onSave: function () {
        console.log("On Save");
        var self = this;
        var form = self.getForm();
        var cForm = self.down('[itemId=contractPanel]');
        console.log('cForm=', cForm);
        var contractCount = cForm.contractCount;
        if (form.isValid()) {
            var limitContainer=self.down("#limit");
            console.log("Form updateRec=", form.updateRecord());
            var rec=form.getRecord()
            var limitType=limitContainer.down("#limitType").getRealValue();
            console.log("limitType",limitType)
            if(limitType) {
                rec.set("limitType", limitType)
                var limitValue = limitContainer.down("#" + limitType + "Value").getValue()
                console.log("limitValue", limitValue)
                rec.set("limitValue", limitValue)
            }
            for (var i = 0; i < contractCount; i++) {
                console.log("Form updateRec=", cForm.items.getAt(i).getForm().updateRecord());                 
            }
            var data = form.getRecord().getData();
            data['contracts'] = new Array();
            for (var j = 0; j < contractCount; j++) {
                data['contracts'].push(cForm.items.getAt(j).getForm().getRecord().getData());
            }
            console.log("data=", data);            
            accountData.updateData(data, contractCount, function (submitResult, e) {                
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
    onLoad: function () {
        console.log("On load");
        var self=this
        var form = this.getForm();
        var accountId = this.getRecord().get("_id");
        console.log("accountId=", accountId);
        var cForm = this.down('[itemId=contractPanel]');
        if (accountId != null) {
            accountData.loadData(accountId, function (loadResult,e) {
                if (e.type === "exception") {
                    console.log("exception=", e);
                    Ext.MessageBox.show({
                        title: 'Произошла ошибка',
                        msg: e.message,
                        icon: Ext.MessageBox.ERROR,
                        buttons: Ext.Msg.OK
                    });
                    self.up('window').close()
                }
                else {
                    var contractCount = loadResult['contractCount'];
                    var accountType = loadResult['accountType'];
                    if (!contractCount)
                        cForm.contractCount = 0;
                    else
                        cForm.contractCount = contractCount;
                    var clientWndType = 'ulform';
                    var contractModel = 'ContractUL';
                    if (accountType == 'fiz') {
                        clientWndType = 'flform';
                        contractModel = 'ContractFL';
                    }
                    else accountType = 'yur';
                    console.log("contractCount=", cForm.contractCount);
                    console.log("clientWndTypet=", clientWndType);
                    cForm.removeAll();
                    console.log("loadResult=", loadResult);
                    var currentAccount = Ext.create('Account', loadResult);
                    var currentContr
                    for (var i = 0; i < contractCount; i++) {
                        currentContr = Ext.create(contractModel, loadResult['contracts'][i]);
                        var tabForm = cForm.add([
                            {xtype: clientWndType, tabId: i + 1, title: "Договор " + (i + 1)}
                        ])[0].getForm();
                        tabForm.loadRecord(currentContr);
                        cForm.setActiveTab(i);
                        console.log('tabForm ', i, '=', tabForm);
                    }
                    var limitType=self.down("#limitType");
                    var limitContainer=self.down("#limit");
                    console.log("limitContainer=",limitContainer)
                    switch(loadResult['limitType'])
                    {
                        case 'daysLimit' :{
                            limitType.setRealValue('daysLimit')
                            var item=limitContainer.down("#daysLimitValue");
                            item.enable();
                            item.setValue(loadResult['limitValue']);
                            break;
                        }
                        case 'currencyLimit' :{
                            limitType.setRealValue('currencyLimit')
                            var item=limitContainer.down("#currencyLimitValue");
                            item.enable();
                            item.setValue(loadResult['limitValue']);
                            break;
                        }
                        case 'percentMonthlyFeeLimit' :{
                            limitType.setRealValue('percentMonthlyFeeLimit')
                            var item=limitContainer.down("#percentMonthlyFeeLimitValue");
                            item.enable();
                            item.setValue(loadResult['limitValue']);
                            break;
                        }
                    }

                    var tarriffPlan = self.down("#tariffplan");
                    tarriffPlan.getStore().load(function(e){
                        tarriffPlan.setValue(loadResult.plan);
                    });

                    form.loadRecord(currentAccount);
                    setTimeout(function () {
                        if (!loadResult)
                            Ext.Msg.alert('Запрос выполнен ', 'Произошла ошибка').toFront();
                    }, 0);
                }
            });
        }
        else form.reset();
    },
    onReset: function () {        
        this.getForm().reset();
    }
});
Ext.define('Billing.view.account.AccountForm.Window', {
    extend: 'Ext.window.Window',
    alias: 'widget.accwnd',
    title: 'Учетная запись',
    icon: 'images/account-icon16.png',
    width: 800,
    height: 700,     
    maximizable: true,
    layout: 'fit', 
    initComponent: function () {
        var self = this;        
        Ext.apply(this,{
            items: [
                {
                    xtype: 'accform',
                    hideRule:self.hideRule,
                    closeAction: function () {
                        this.up('window').close();
                    }
                }
            ]            
            });
        this.callParent();
    }
});
Ext.define('Account', {
    extend: 'Ext.data.Model',
    fields: ['_id', 'name', 'comment', 'fullClientName', 'accountType',
        'limitType','limitValue', 'balance','cost','paymentWay',
        'plan', 'status','blockcause',/*'cfposition',*/'cffam', 'cfname',
        'cffathername', 'cfmobphone1', 'cfmobphone2',
        'cfworkphone1', 'cfemail', 'cfnote'
        //{name:'contracts', persist:true},
    ],
    idProperty: '_id'
});
Ext.apply(Ext.form.field.VTypes, {
    rualpha: function (val, field) {
        return /^[А-яЁё_\-\s]{1,50}$/.test(val);
    },
    rualphaText: 'Текст должен быть введен русскими символами',
    rualphaMask: /[0-9\W^A-aЁё]/i // /[А-яЁё_\-\s]/i
});
Ext.apply(Ext.form.field.VTypes, {
    rualphanum: function (val, field) {
        return /[А-яЁё0-9_\-\s]/.test(val);
    },
    rualphanumText: 'Текст может содержать только русские символы или цифры',
    rualphanumMask: /[0-9\W^A-aЁё]/i // /[А-яЁё0-9_\-\s]/i
});
Ext.apply(Ext.form.field.VTypes, {
    allalpha: function (val, field) {
        return /[A-zА-яЁё_\-\s]/.test(val);
    },
    allalphaText: 'Текст должен содержать только символы русского или латинского алфавита',
    allalphaMask: /[0-9\W^A-aЁё]/i // /[A-zА-яЁё_\-\s]/i
});
Ext.apply(Ext.form.field.VTypes, {
    phone: function (val, field) {
        return /^\+?[0-9]{1,5}\s?[\(\-]{0,1}[0-9]{1,5}[\)\-]{0,1}?\s?[0-9\s\-]{5,20}$/.test(val);
    },
    phoneText: 'Введите корректный номер телефона. Например +7(890)123-45-67 или 495 123 45 67',
    phoneMask: /[0-9\W^A-aЁё]/i
});

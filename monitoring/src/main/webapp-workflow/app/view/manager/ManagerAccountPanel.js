/**
 * Created by IVAN on 16.12.2014.
 */

Ext.define('Workflow.view.manager.ManagerAccountPanel', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.manaccpanel',
    itemId:"manAccPanel",
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    defaults: {
        margin: '0 20 5 20'
    },
    //fieldDefaults: {
    //    labelWidth: 150,
    //    margin: '0 20 5 20'
    //},
    items: [
        {
            xtype: 'fieldcontainer',
            fieldLabel: 'Учетная запись',
            itemId: 'accountName',
            layout: 'column',
            labelWidth: 150,
            hidden: self.hideRule,
            margin: '20 0 5 40',
            items: [
                {
                    xtype: 'radiogroup',
                    border: true,
                    fieldLabel: '',
                    columns: 1,
                    vertical: true,
                    items: [
                        {boxLabel: 'выбрать существующую', name: 'acc', inputValue: 'existAcc',checked: true},
                        {boxLabel: 'создать новую', name: 'acc', inputValue: 'newAcc'}
                    ],
                    listeners: {
                        change: function (radio, newVal, oldVal) {
                            var value = radio.getValue().acc
                            console.log("value", value)
                            var curTicketPanel=radio.up('#curTicketPanel');
                            var form=curTicketPanel.down("#accountForm")
                            var existAccValue = curTicketPanel.down("#existAcc")
                            var newAccValue = curTicketPanel.down("#newAcc")
                            var objForm=curTicketPanel.down("#manObjPanel");
                            var worksForm=curTicketPanel.down("#manWorksPanel");
                            switch (value) {
                                case "existAcc" :
                                {
                                    existAccValue.setDisabled(false);
                                    newAccValue.setDisabled(true);
                                    var account=existAccValue.getValue();
                                    if(account!="" && account!= null)
                                    {
                                        form.loadData(existAccValue.getValue());
                                        objForm.setDisabled(false);
                                        worksForm.setDisabled(false);
                                    }
                                    else  {
                                        form.getForm().reset(true);
                                    }
                                    break;
                                }
                                case "newAcc" :
                                {
                                    existAccValue.setDisabled(true)
                                    newAccValue.setDisabled(false)
                                    form.reset()
                                    var record = Ext.create("Account",{})
                                    form.loadRecord(record)
                                    objForm.setDisabled(true);
                                    worksForm.setDisabled(true);
                                    break;
                                }
                            }
                        }
                    }
                },
                {
                    xtype: 'fieldcontainer',
                    itemId: 'accValue',
                    margin: '0 0 0 40',
                    layout: 'vbox',
                    defaultType: 'textfield',
                    items: [
                        {
                            xtype: 'combobox',
                            itemId: "existAcc",
                            //regex:/^\d+$/i,
                            validateBlank: true,
                            //disabled: true,
                            store: Ext.create('EDS.Ext5.store.AccountsDataShort', {
                                autoLoad: true
                            }),
                            valueField: '_id',
                            displayField: 'name',
//                            typeAhead: true,
                            forceSelection: true,
                            minChars: 0,
                            allowBlank: false,
                            listeners:{
                                "select":function( combo, record, eOpts ){
                                    console.log("select")
                                    var curTicketPanel=combo.up("#curTicketPanel");
                                    if(!curTicketPanel.record){
                                        curTicketPanel.loadData(
                                            Ext.create("Ticket",
                                                {
                                                    accountId:combo.getValue(),
                                                    accountName:combo.getRawValue()
                                                }))
                                    }
                                    else {
                                        console.log("curTicketPanel.record",curTicketPanel.record)
                                        curTicketPanel.record.set('works',new Array(),{silent:true})
                                        curTicketPanel.record.set('worksCount',undefined,{silent:true})
                                        curTicketPanel.record.set('accountId',combo.getValue(),{silent:true})
                                        curTicketPanel.record.set('accountName',combo.getRawValue(),{silent:true})
                                        console.log("curTicketPanel.record",curTicketPanel.record)
                                        curTicketPanel.loadData(curTicketPanel.record)
                                    }
                        }
                            }
                        },
                        {
                            itemId: "newAcc",
                            //regex:/^\-?\d+$/i,
                            validateBlank: true,
                            disabled: true
                        }
                    ]
                }
            ]
        },
        {
            xtype: 'form',
            itemId:'accountForm',
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            defaultType: 'textfield',
            fieldDefaults: {
                labelWidth: 150,
                margin: '0 20 5 20'
            },
            items: [
                //{
                //    xtype:'hidden',
                //    name:'name'
                //},
                {
                    margin: '10 20 5 20',
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
                    name: 'cfmobphone1'
                },
                {
                    vtype: 'phone',
                    fieldLabel: 'Мобильный телефон 2',
                    name: 'cfmobphone2'
                },
                {
                    vtype: 'phone',
                    fieldLabel: 'Рабочий телефон',
                    name: 'cfworkphone1'
                },
                {
                    fieldLabel: 'e-mail',
                    name: 'cfemail',
                    vtype: 'email'
                },
                {
                    xtype: 'textareafield',
                    fieldLabel: 'Примечание',
                    name: 'cfnote'
                }
            ],
            dockedItems:[
                {
                    xtype: 'toolbar',
                    dock: 'bottom',
                    items:[
                        '->',
                        {
                            text:'Сохранить',
                            handler:function(btn){
                                var form=btn.up('form')
                                if(!form.getRecord()==true || !form.isValid()) {
                                    Ext.Msg.alert('Ошибка сохранения', 'Заполните форму корректными данными')
                                }
                                else {
                                    form.updateRecord()
                                    var rec = form.getRecord()
                                    var radio = btn.up('#manAccPanel').down('radiogroup')
                                    var accType = radio.getValue().acc
                                    if (accType == "newAcc") {
                                        var name = btn.up('#manAccPanel').down("#newAcc").getValue()
                                        if (name != null && name != "") {
                                            rec.set("name", name)
                                            rec.set("_id",null)
                                        }
                                        else {
                                            Ext.Msg.alert('Ошибка сохранения', 'Не указано имя аккаунта')
                                        }
                                    }
                                    var data=rec.getData()
                                    console.log(data)
                                    form.saveData(data)
                                }
                            }
                        },
                        {
                            text:'Отменить',
                            handler:function(btn){
                                var form=btn.up('form')
                                var rec=form.getRecord()
                                console.log("rec",rec)
                                if(rec.getId().match("Account"))
                                    form.reset()
                                else
                                    form.loadRecord(rec)
                            }
                        }
                    ]
                }
            ],
            loadData:function(accountId){
                var form=this;
                accountData.loadData(accountId,function(data,e){
                    if (!e.status) {
                        Ext.MessageBox.show({
                            title: 'Произошла ошибка',
                            msg: e.message,
                            icon: Ext.MessageBox.ERROR,
                            buttons: Ext.Msg.OK
                        });
                    }
                    else {
                        console.log('Учетная запись - ',data);
                        var rec = Ext.create('Account',data)
                        form.loadRecord(rec)
                    }
                })
            },
            saveData:function(data){
                var form=this;
                accountData.updateData(data,null,function (submitResult, e) {
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
                            Ext.Msg.alert('Запрос выполнен ', submitResult['msg'])
                            form.up("#manAccPanel").down("radiogroup").setValue({"acc":"existAcc"})
                            var combo=form.up("#manAccPanel").down("#existAcc")
                            combo.getStore().load()
                            combo.setValue(submitResult["accountId"])
                            form.reset()
                            form.loadData(submitResult["accountId"])
                        }
                })
            }
        }
    ]
})

Ext.apply(Ext.form.field.VTypes, {
    phone: function (val, field) {
        return /^\+?[0-9]{1,5}\s?[\(\-]{0,1}[0-9]{1,5}[\)\-]{0,1}?\s?[0-9\s\-]{5,20}$/.test(val);
    },
    phoneText: 'Введите корректный номер телефона. Например +7(890)123-45-67 или 495 123 45 67',
    phoneMask: /[0-9\W^A-aЁё]/i
})

Ext.define('Account', {
    extend: 'Ext.data.Model',
    fields: ['_id', 'name', 'cffam', 'cfname',
        'cffathername', 'cfmobphone1', 'cfmobphone2',
        'cfworkphone1', 'cfemail', 'cfnote'
        //{name:'contracts', persist:true},
    ],
    idProperty: '_id'
});
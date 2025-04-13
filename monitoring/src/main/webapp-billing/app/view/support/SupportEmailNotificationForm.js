/**
 * Created by IVAN on 11.01.2017.
 */
Ext.define('Billing.view.support.SupportEmailNotificationForm', {
    extend: 'Ext.form.Panel',
    alias: 'widget.supportemailntfform',
    header: false,
    defaultType: 'textfield',
    bodyPadding: 5,
    fieldDefaults: {
        anchor: '100%',
        labelAlign: 'right',
        labelWidth: 100
    },
    items: [
        {
            fieldLabel: 'Имя',
            name: 'name',
            allowBlank: false
        },
        {
            fieldLabel: 'E-mail',
            name: 'email',
            vtype: 'email',
            allowBlank: false
        },
        {
            xtype: 'fieldcontainer',
            fieldLabel: 'Категории',
            layout: 'vbox',
            items: [
                {
                    xtype: 'checkbox',
                    name: 'equipment',
                    fieldLabel: 'Вопросы по оборудованию',
                    labelWidth: 180,
                    inputValue: true,
                    uncheckedValue: false
                },
                {
                    xtype: 'checkbox',
                    name: 'program',
                    fieldLabel: 'Вопросы по программе',
                    labelWidth: 180,
                    inputValue: true,
                    uncheckedValue: false

                },
                {
                    xtype: 'checkbox',
                    name: 'finance',
                    fieldLabel: 'Финансовые вопросы',
                    labelWidth: 180,
                    inputValue: true,
                    uncheckedValue: false
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
                '->',
                {
                    icon: 'images/ico16_okcrc.png',
                    itemId: 'savenclose',
                    text: 'Сохранить',
                    handler: function (btn) {
                        var form = btn.up('form');
                        form.onSave(form)
                    }
                }, {
                    icon: 'images/ico16_cancel.png',
                    text: 'Отменить',
                    handler: function (btn) {
                        btn.up('window').close();
                    }
                }
            ]
        }
    ],
    onSave: function (form) {
        form.updateRecord();
        if (form.isValid()) {
            var data = form.getRecord().getData();
            if (data.equipment || data.program || data.finance)
                supportEmailNotificationEDS.addEmail(data, function (res, e) {
                    if (e.type === "exception") {
                        Ext.MessageBox.show({
                            title: 'Произошла ошибка',
                            msg: e.message,
                            icon: Ext.MessageBox.ERROR,
                            buttons: Ext.Msg.OK
                        });
                    }
                    else  form.up('window').close()
                });
            else
                Ext.MessageBox.show({
                    title: 'Ошибка добавления записи',
                    msg: 'Выберите как минимум одну категорию',
                    buttons: Ext.MessageBox.OK,
                    icon: Ext.MessageBox.WARNING
                });
        }
        else
            Ext.MessageBox.show({
                title: 'Ошибка добавления записи',
                msg: 'Заполните поля корректными данными',
                buttons: Ext.MessageBox.OK,
                icon: Ext.MessageBox.WARNING
            });
    },
    onLoad: function (id) {
        var self = this;
        if (id == "") {
            self.getForm().loadRecord(Ext.create('EmailNotification', {}))
        }
        else
            supportEmailNotificationEDS.loadOne(id, function (res, e) {
                if (e.type === "exception") {
                    Ext.MessageBox.show({
                        title: 'Произошла ошибка',
                        msg: e.message,
                        icon: Ext.MessageBox.ERROR,
                        buttons: Ext.Msg.OK
                    });
                    self.up('window').close()
                }
                else {
                    self.getForm().loadRecord(Ext.create('EmailNotification', res))
                }
            })
    }
    // initComponent: function () {
    //     this.addEvents('create');
    //     var self = this;
    //     Ext.apply(this, {})
    // }
});

Ext.define('Billing.view.support.SupportEmailNotificationFormWindow', {
    extend: 'Ext.window.Window',
    alias: 'widget.supportemailntfformwnd',
    title: "Новый подписчик",
    icon: 'images/ico16_eventsmsgs.png',
    maximizable: true,
    minWidth: 400,
    minHeight: 230,
    width: 400,
    height: 230,
    layout: 'fit',
    items: [
        {
            xtype: 'supportemailntfform'
        }
    ]
});

Ext.define('EmailNotification', {
        extend: 'Ext.data.Model',
        fields: [
            "_id", "name", "email", "equipment", "program", "finance"],
        idProperty: '_id'
    }
);
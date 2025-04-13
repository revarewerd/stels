Ext.define('Seniel.view.UserSettingsWindow', {
    extend: 'Seniel.view.WRWindow',
    alias: 'widget.usersetwnd',
    icon: 'images/ico16_user.png',
    title: tr('usersettings.title'),
    btnConfig: {
        icon: 'images/ico24_user.png',
        text: tr('usersettings.title')
    },
    width: 440,
    height: 260,
    minWidth: 440,
    minHeight: 260,
    layout: 'fit',
    items: [
        {
            itemId: 'objSettingsTab',
            xtype: 'tabpanel',
            padding: 0,
            defaults: {
                bodyPadding: 0,
                padding: 0
            },
            items: [
                {
                    title: tr('usersettings.common'),
                    itemId: 'usrSetCommon',
                    xtype: 'panel',
                    items: [
                        {
                            xtype: 'form',
                            defaults: {
                                anchor: '100%',
                                labelWidth: 120,
                                labelPad: 10,
                                xtype: 'textfield'
                            },
                            layout: 'anchor',
                            border: false,
                            padding: 8,
                            items: [
                                {
                                    name: 'timezoneId',
                                    fieldLabel: tr('usersettings.timezone'),
                                    xtype: 'combobox',
                                    allowBlank: false,
                                    forceSelection: true,
                                    triggerAction: 'all',
                                    minChars: 0,
                                    selectOnTab: true,
                                    valueField: 'id',
                                    displayField: 'name',
                                    store: Ext.create('EDS.store.Timezones', {
                                        autoLoad: false,
                                        buffered: false
                                    }),
                                    listeners: {
                                        afterrender: function (self) {
                                            timeZonesStore.getUserTimezone(function(tz) {
                                                if (!tz) {
                                                    var offset = new Date().getTimezoneOffset();
                                                    var store = self.getStore();
                                                    store.load({
                                                        scope: this,
                                                        callback: function () {
                                                            var f = store.find("offset", offset * -60 * 1000);
                                                            self.select(store.getAt(f));
                                                        }
                                                    });
                                                } else {
                                                    var store = self.getStore();
                                                    store.load({
                                                        scope: this,
                                                        callback: function () {
                                                            self.select(store.getById(tz));
                                                        }
                                                    });
                                                }
                                            });
                                        }
                                    }
                                },
                                {
                                    name: 'isClustering',
                                    boxLabel: tr('usersettings.clustering'),
                                    xtype: 'checkbox',
                                    uncheckedValue: false,
                                    inputValue: true
                                },
                                {
                                    name: 'isEquipGrouping',
                                    boxLabel: tr('usersettings.equipgrouping'),
                                    xtype: 'checkbox',
                                    uncheckedValue: false,
                                    checked: true,
                                    inputValue: true
                                }
                            ]
                        }
                    ]
                },
                {
                    title: tr('usersettings.authorization'),
                    itemId: 'usrSetAuth',
                    xtype: 'panel',
                    hidden: true,
                    layout: {
                        type: 'vbox',
                        align: 'stretch'
                    },
                    items: [
                        {
                            xtype: 'form',
                            defaults: {
                                anchor: '100%',
                                labelWidth: 120,
                                labelPad: 10,
                                xtype: 'textfield'
                            },
                            layout: 'anchor',
                            border: false,
                            padding: 8,
                            items: [
                                {
                                    name: 'usrPasw1',
                                    fieldLabel: tr('usersettings.newpassword'),
                                    inputType: 'password'
                                },
                                {
                                    name: 'usrPasw2',
                                    fieldLabel: tr('usersettings.repeatpassword'),
                                    inputType: 'password'
                                }
                            ]
                        }
                    ]
                },
                {
                    title: tr('usersettings.contacts'),
                    itemId: 'usrSetContacts',
                    xtype: 'panel',
                    items: [
                        {
                            xtype: 'form',
                            defaults: {
                                anchor: '100%',
                                labelWidth: 120,
                                labelPad: 10,
                                xtype: 'textfield'
                            },
                            layout: 'anchor',
                            border: false,
                            padding: 8,
                            items: [
                                {
                                    name: 'usrEmail',
                                    fieldLabel: tr('usersettings.email'),
                                    validator: function(val) {
                                        var arr = val.split(',');
                                        for (var i = 0; i < arr.length; i++) {
                                            if (!Ext.form.VTypes.email(arr[i])) {
                                                return tr('rules.email.valid');
                                            }
                                        }
                                        return true;
                                    }
                                },
                                {
                                    xtype: 'displayfield',
                                    value: tr('rules.invalidemail.msg'),
                                    padding: '-4 8 8 130',
                                    cls: 'field-hint-subtext'
                                },
                                {
                                    name: 'usrPhone',
                                    fieldLabel: tr('usersettings.mobile'),
                                    validator: function(val) {
                                        var arr = val.split(','),
                                            regex = /^\s*\+\d{11,}\s*$/;
                                        for (var i = 0; i < arr.length; i++) {
                                            if (!regex.test(arr[i])) {
                                                return tr('rules.phonanumber.valid');
                                            }
                                        }
                                        return true;
                                    }
                                },
                                {
                                    xtype: 'displayfield',
                                    value: tr('rules.invalidphone.msg'),
                                    padding: '-4 8 8 130',
                                    cls: 'field-hint-subtext'
                                }
                            ]
                        }
                    ]
                }
            ]
        }
    ],
    dockedItems: [
        {
            xtype: 'toolbar',
            dock: 'bottom',
            items: [
                '->',
                {
                    xtype: 'button',
                    text: tr('main.ok'),
                    itemId: 'btnOk',
                    icon: 'images/ico16_okcrc.png',
                    handler: function(btn) {
                        var wnd = btn.up('usersetwnd');
                        var authData = wnd.down('panel[itemId="usrSetAuth"] form').getValues(),
                            contData = wnd.down('panel[itemId="usrSetContacts"] form').getValues(),
                            comnData = wnd.down('panel[itemId="usrSetCommon"] form').getValues(),
                            email = wnd.down('textfield[name="usrEmail"]'),
                            phone = wnd.down('textfield[name="usrPhone"]');
                        
                        if (email.getValue() && !email.validate()) {
                            Ext.MessageBox.show({
                                title: tr('rules.invalidemail'),
                                msg: tr('rules.invalidemail.msg'),
                                icon: Ext.MessageBox.WARNING,
                                buttons: Ext.Msg.OK
                            });
                            return false;
                        }
                        if (phone.getValue() && !phone.validate()) {
                            Ext.MessageBox.show({
                                title: tr('rules.invalidphone'),
                                msg: tr('rules.invalidphone.msg'),
                                icon: Ext.MessageBox.WARNING,
                                buttons: Ext.Msg.OK
                            });
                            return false;
                        }
                        if (authData.usrPasw1 !== authData.usrPasw2) {
                            Ext.MessageBox.show({
                                title: tr('usersettings.newpassword'),
                                msg: tr('usersettings.identicalpassords'),
                                icon: Ext.MessageBox.WARNING,
                                buttons: Ext.Msg.OK
                            });
                            return false;
                        }
                        contData.usrPasw = authData.usrPasw1;
                        for (var i in comnData) {
                            contData[i] = comnData[i];
                        }

                        var msgPromt = Ext.MessageBox;

                        function updateSettings() {
                            userInfo.updateUserSettings(contData, function (resp) {
                                if (resp === 'SUCCESS') {
                                    var viewport = wnd.up('viewport');
                                    var mainmap = viewport.down('mainmap');
                                    viewport.userPhone = contData.usrPhone;
                                    viewport.userEmail = contData.usrEmail;
                                    if (contData.isClustering === true) {
                                        mainmap.enableClustering();
                                    } else {
                                        mainmap.disableClustering();
                                    }
//                                            if (wnd.userName !== contData.usrName) {
//                                                Ext.MessageBox.show({
//                                                    title: 'Имя пользователя изменено',
//                                                    msg: 'Необходимо заново пройти авторизацию. Вы будете перенаправлены на начальную страницу.',
//                                                    icon: Ext.MessageBox.INFO,
//                                                    buttons: Ext.Msg.OK,
//                                                    fn: function() {
//                                                        loginService.logout(function() {
//                                                            window.location = "/j_spring_security_logout";
//                                                        });
//                                                    }
//                                                });
//                                            }
                                    wnd.close();
                                    
                                    console.log('Settings = ', contData);
                                    location.reload()
                                }
                                if (resp === 'WRONG PASSWORD') {
                                    Ext.MessageBox.show({
                                        title: tr('usersettings.invalidpassword'),
                                        msg: tr('usersettings.invalidpassword.msg'),
                                        icon: Ext.MessageBox.ERROR,
                                        buttons: Ext.Msg.OK
                                    });
                                    return false;
                                }
                                if (resp === 'PASSWORD MODIFICATION PROHIBITED') {
                                    Ext.MessageBox.show({
                                        title: tr('usersettings.passwordchangeisforbidden'),
                                        msg: tr('usersettings.passwordchangeisforbidden.msg'),
                                        icon: Ext.MessageBox.ERROR,
                                        buttons: Ext.Msg.OK
                                    });
                                    return false;
                                }
//                                        if (resp === 'USERNAME EXISTS') {
//                                            Ext.MessageBox.show({
//                                                title: 'Неверное имя',
//                                                msg: 'Пользователь с таким именем уже существует, введите другое имя',
//                                                icon: Ext.MessageBox.ERROR,
//                                                buttons: Ext.Msg.OK
//                                            });
//                                            return false;
//                                        }
                            });
                        }

                        if (contData.usrPasw !== "") {
                            msgPromt.prompt(
                                tr('usersettings.changesettings'),
                                tr('usersettings.entercurrentpassword'),
                                function (btn, text) {
                                    msgPromt.textField.inputEl.dom.type = 'text';
                                    if (btn === 'ok') {
                                        contData.usrOldPasw = text;
//                                    console.log('Settings data = ', contData);
                                        updateSettings();
                                    }
                                }
                            );
                            msgPromt.textField.inputEl.dom.type = 'password';
                        }
                        else {
                            updateSettings();
                        }
                    }
                },
                {
                    xtype: 'button',
                    text: tr('main.cancel'),
                    itemId: 'btnCancel',
                    icon: 'images/ico16_cancel.png',
                    handler: function(btn) {
                        btn.up('usersetwnd').close();
                    }
                }
            ]
        }
    ],
    initComponent: function() {
        this.callParent(arguments);
        
        this.on('boxready', function(wnd) {
            var viewport = wnd.up('viewport');
            
            userInfo.canChangePassword(function(resp) {
                if (resp) {
                    wnd.down('tabpanel').setActiveTab(1);
                    wnd.down('tabpanel').getActiveTab().tab.show();
                    wnd.down('tabpanel').setActiveTab(0);
                }
            });
            wnd.down('textfield[name="usrEmail"]').setValue(viewport.userEmail);
            wnd.down('textfield[name="usrPhone"]').setValue(viewport.userPhone);
            wnd.down('checkbox[name="isClustering"]').setValue(viewport.down('mainmap').isClustering);
        });
    }

});
Ext.require(['Ext.form.*', 'Ext.util.KeyMap', 'Ext.util.Cookies']);

var mobiCookie = Ext.util.Cookies.get('isMobileVersion');
console.log('isMobileVersion = ', mobiCookie);
if (mobiCookie === null) {
    mobiCookie = navigator.userAgent.match(/mobi|tablet|iPad|iPhone|iPod|webOS|BlackBerry|Android/i) !== null || screen.width <= 480;
    Ext.util.Cookies.set('isMobileVersion', mobiCookie, Ext.Date.add(new Date(), Ext.Date.MONTH, 1));
}

Ext.define('AM.view.login.Form', {
    extend: 'Ext.form.Panel',
    alias: 'widget.loginform',

    name: 'loginform',
    frame: true,
    title: tr('login.enterthesystem'),
    bodyPadding: '5px 5px 0',
    style: 'margin: 5px auto 0 auto;',
    width: 350,
    height: 200,
    fieldDefaults: {
        labelWidth: 125,
        msgTarget: 'side',
        autoFitErrors: false
    },
    defaults: {
        width: 300
    },
    defaultType: 'textfield',

    initComponent: function () {
        var self = this;
        this.buttons = [
            {
                name: 'forgotButton',
                text: tr('login.forgotpassword'),
                handler: function () {
                    console.log("forgotButton: handler()");

                    Ext.MessageBox.show({
                        title: tr('recovery.title'),
                        msg: tr('recovery.message'),
                        buttons: Ext.MessageBox.OKCANCEL,
                        icon: Ext.MessageBox.QUESTION,
                        fn: function (buttonId) {
                            if (buttonId === "ok") {
                                self.recoverPassword();
                            }
                        }
                    });
                }
            },
            {
                name: 'loginButton',
                text: tr('login.enter'),
                handler: function () {
                    self.submitLogin()
                }
            }
        ];

        this.items = [
            {
                xtype:'combobox',
                fieldLabel: tr('login.lang'),
                forceSelection:true,
                editable: false,
                queryMode: 'local',
                store: [
                    ['ru', 'Русский'],
                    ['en', 'English'],
                    ['es', 'Español']
                ],
                value: Ext.util.Cookies.get('lang') ? Ext.util.Cookies.get('lang') : "ru",
                name: curlang,
                allowBlank: false,
                listeners: {
                    change: function(self, value){
                        console.log("lang: ",value);
                        Ext.util.Cookies.set('lang', value, Ext.Date.add(new Date(), Ext.Date.YEAR, 100));
                        window.location.reload();
                    }
                }
            },
            {
                fieldLabel: tr('login.name'),
                name: 'username',
                id: 'username',
                inputType: 'text'
            },
            {
                fieldLabel: tr('login.password'),
                inputType: 'password',
                name: 'password'
            },
            {
                boxLabel: tr('login.rememberme'),
                name: 'remember-me',
                xtype: 'checkboxfield',
                checked: false
            },
            {
                boxLabel: tr('login.mobileversion'),
                xtype: 'checkboxfield',
                listeners: {
                    change: function(cb, newVal, oldVal) {
                        if (Ext.util.Cookies.get('isMobileVersion') !== newVal.toString()) {
                            Ext.util.Cookies.set('isMobileVersion', newVal, Ext.Date.add(new Date(), Ext.Date.MONTH, 1));
                        }
                    },
                    boxready: function(cb) {
                        cb.setValue(mobiCookie);
                    }
                }
            }
        ];

        this.on('afterRender', function (el) {
            var map = new Ext.util.KeyMap(document, {
                    key: Ext.EventObject.ENTER,
                    fn: self.submitLogin,
                    scope: self
                }
            );
        });
        this.callParent(arguments);
    },
    blocked: false,
    submitLogin: function () {
        var form = this;
        if (!form.blocked) {

            var fv = form.getForm().getFieldValues();

            Ext.Ajax.request({
                url: 'j_spring_security_check',
                method: 'post',
                timeout: '3000',
                params: {
                    username: fv['username'],
                    password: fv['password'],
                    'remember-me': (fv['remember-me']) ? "on" : "off",
                    submit:	'Login'
                },
                defaultHeaders: {
                    'Content-Type': 'application/json; charset=utf-8'
                },
                success: function (response, opts) {
                    try {
                        console.log("response=", response);
                        var result = Ext.decode(response.responseText);
                        if (result !== null) {
                            console.log("result=", result);

                            if (result.success) {
                                window.location = "/";
                            }
                            else {
                                form.blocked = true;
                                Ext.MessageBox.show({
                                    title: tr('login.error'),
                                    msg: result.message,
                                    buttons: Ext.MessageBox.OK,
                                    icon: Ext.MessageBox.ERROR,
                                    fn: function () {
                                        form.blocked = false;
                                    }
                                });
                            }


                        } else {
                            console.log("failture=", e);
                        }
                    } catch (e) {
                        console.log("failture=", e);
                    }
                },
                failure: function (response, opts) {
                    console.log("respfailture=", response);
                }
            });
        }
    },
    recoverPassword: function () {
            var form = this;
            if (!form.blocked) {

                var fv = form.getForm().getFieldValues();

                Ext.Ajax.request({
                    url: '/EDS/recoverypassword',
                    method: 'post',
                    timeout: '3000',
                    params: {
                        username: fv['username']
                    },
                    defaultHeaders: {
                        'Content-Type': 'application/json; charset=utf-8'
                    },
                    success: function (response, opts) {
                        try {
                            console.log("response=", response);
                            var result = Ext.decode(response.responseText);
                            if (result !== null) {
                                console.log("result=", result);

                            form.blocked = true;
                            Ext.MessageBox.show({
                                msg: result.message,
                                buttons: Ext.MessageBox.OK,
                                icon: Ext.MessageBox.INFO,
                                fn: function () {
                                    form.blocked = false;
                                }
                            });

                            } else {
                                console.log("failture=", e);
                            }
                        } catch (e) {
                            console.log("failture=", e);
                        }
                    },
                    failure: function (response, opts) {
                        console.log("respfailture=", response);
                    }
                });
            }
        }
});


Ext.onReady(function() {
    Ext.widget('loginform', {
        renderTo: Ext.getBody()
    });
});
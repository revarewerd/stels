Ext.require(['Ext.form.*', 'Ext.util.KeyMap']);

Ext.define('AM.view.login.Form', {
    extend: 'Ext.form.Panel',
    alias: 'widget.loginform',

    name: 'loginform',
    frame: true,
    title: 'Вход в систему (Администрирование)',
    bodyPadding: '5px 5px 0',
    width: 350,
    height: 150,
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
                name: 'loginButton',
                text: 'Войти',
                handler: function () {
                    self.submitLogin()
                }
            }
        ];

        this.items = [
            {
                fieldLabel: 'Имя',
                name: 'username',
                id: 'username',
                inputType: 'text'
            },
            {
                fieldLabel: 'Пароль',
                inputType: 'password',
                name: 'password'
            },
            {
                boxLabel: 'Запомнить меня',
                name: 'remember-me',
                xtype: 'checkboxfield',
                checked: false
            }
        ];

        this.on('afterRender', function (el) {
            var map = new Ext.util.KeyMap(document, {
                    key: Ext.EventObject.ENTER,
                    fn: self.submitLogin,
                    scope: self
                }
            );
        })
        this.callParent(arguments);
    },
    blocked: false,
    submitLogin: function () {
        var form = this
        if (!form.blocked) {

            var fv = form.getForm().getFieldValues()

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
                        console.log("response=", response)
                        var result = Ext.decode(response.responseText);
                        if (result !== null) {
                            console.log("result=", result)

                            if (result.success) {
                                window.location = "/billing";
                            }
                            else {
                                form.blocked = true;
                                Ext.MessageBox.show({
                                    title: 'Ошибка авторизации',
                                    msg: result.message,
                                    buttons: Ext.MessageBox.OK,
                                    icon: Ext.MessageBox.ERROR,
                                    fn: function () {
                                        form.blocked = false
                                    }
                                });
                            }


                        } else {
                            console.log("failture=", e)
                        }
                    } catch (e) {
                        console.log("failture=", e)
                    }
                },
                failure: function (response, opts) {
                    console.log("respfailture=", response)
                }
            });
        }
    }
});


Ext.onReady(function () {
    Ext.widget('loginform', {
        renderTo: Ext.getBody()
    });
})
;


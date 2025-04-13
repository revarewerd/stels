/**
 * Created by IVAN on 24.05.2016.
 */
Ext.define('Seniel.view.PaymentWindow', {
    extend: 'Seniel.view.WRWindow',
    alias: 'widget.paymentwindow',
    icon: 'images/ico24_money.png',
    title: tr("payment.addfunds"),
    btnConfig: {
        icon: 'images/ico24_money.png',
        text: tr("payment.addfunds"),
    },
    width: 500,
    height: 450,
    minWidth: 500,
    minHeight: 450,
    layout: 'fit',
    initComponent: function () {
        var wnd = this;
        this.on('boxready', function (wnd) {
            var viewport = wnd.up('viewport');
            console.log('viewport', viewport);
            console.log(viewport.userEmail, viewport.userPhone);
            wnd.down('textfield[name="cps_email"]').setValue(viewport.userEmail);
            wnd.down('textfield[name="cps_phone"]').setValue(viewport.userPhone);
            paymentClientService.getPaymentParams(function(res){
                    console.log("res=",res)
                    if (res.type == "success") {
                        wnd.down('[name="shopId"]').setValue(res.shopId);
                        wnd.down('[name="scId"]').setValue(res.scId);
                        wnd.down('[name="customerNumber"]').setValue(res.customerNumber);
                        wnd.down("#paymentform").getForm().url=res.paymentServerUrl
                    }
                    else {
                        Ext.MessageBox.show({
                            title: tr('payment.error'),
                            msg: tr(res.result),
                            icon: Ext.MessageBox.WARNING,
                            buttons: Ext.Msg.OK
                        });
                        wnd.close()
                    }
                }
            )

        });
        Ext.apply(this, {
            dockedItems: [
                {
                    xtype: 'toolbar',
                    dock: 'bottom',
                    ui: 'footer',
                    items: [
                        '->',
                        {
                            icon: 'images/ico16_okcrc.png',
                            //disabled:true,
                            itemId: 'save',
                            text: tr("payment.proceed"),
                            handler: function () {
                                var email = wnd.down('textfield[name="cps_email"]');
                                var phone = wnd.down('textfield[name="cps_phone"]');
                                var sum = wnd.down('numberfield[name="sum"]');
                                if (!sum.getValue() || !sum.isValid()) {
                                    Ext.MessageBox.show({
                                        title: tr('payment.invalidAmount'),
                                        msg: tr('payment.correctAmount'),
                                        icon: Ext.MessageBox.WARNING,
                                        buttons: Ext.Msg.OK
                                    });
                                    return false;
                                }
                                if (email.getValue() && !email.validate()) {
                                    Ext.MessageBox.show({
                                        title: tr('rules.invalidemail'),
                                        msg: tr('payment.emailFormat'),
                                        icon: Ext.MessageBox.WARNING,
                                        buttons: Ext.Msg.OK
                                    });
                                    return false;
                                }
                                if (phone.getValue() && !phone.validate()) {
                                    Ext.MessageBox.show({
                                        title: tr('rules.invalidphone'),
                                        msg: tr('payment.phoneFormat'),
                                        icon: Ext.MessageBox.WARNING,
                                        buttons: Ext.Msg.OK
                                    });
                                    return false;
                                }
                                var form=wnd.down("#paymentform").getForm()
                                form.standardSubmit=true
                                form.submit()
                            }
                        },
                        {
                            icon: 'images/ico16_cancel.png',
                            text: tr('payment.cancel'),
                            handler: function () {
                                wnd.close();
                            }
                        }
                    ]
                }
            ],
            items: [
                {
                    xtype: 'paymentform',
                    wnd: wnd
                }
            ]
        });
        this.callParent();
    }
});

Ext.define('Seniel.view.PaymentWindow.PaymentForm', {
    extend: 'Ext.form.Panel',
    alias: 'widget.paymentform',
    itemId: 'paymentform',
    bodyPadding: 5,
    defaultType: 'textfield',
    fieldDefaults: {
        margin: '0 15 10 15',
        labelWidth: 150,
        //vtype:'alphanum',
    },
    layout: {
        type: 'vbox',
        align: 'stretch'
    },
    items: [
        {
            margin: '10 15 10 15',
            xtype: "numberfield",
            minValue:0.01,
            fieldLabel: tr('payment.amount'),
            name: "sum",
            allowExponential:false
        },
        {
            fieldLabel: tr('payment.method'),
            xtype: 'radiogroup',
            vertical: true,
            columns: 1,
            defaults:{
                margin:'0 5 5 5'
            },
            items: [
                {
                    margin:'5 5 5 5',
                    boxLabel: '<div class="payment-div"><img src="images/payment/visa.png"/><img src="images/payment/master.png"/><span>'+tr('payment.card')+'</span></div>',
                    name: "paymentType",
                    inputValue: 'AC',
                    checked: true
                },
                {
                    boxLabel: '<div class="payment-div"><img src="images/payment/yandex.png"/><span>'+tr('payment.yandexMoney')+'</span></div>',//tr
                    name: "paymentType",
                    inputValue: 'PC'
                },
                {
                    boxLabel: '<div class="payment-div"><img src="images/payment/webmoney.png"/><span>'+tr('payment.webMoney')+'</span></div>',//tr
                    name: "paymentType",
                    inputValue: 'WM'
                },
                {
                    boxLabel: '<div class="payment-div"><img src="images/payment/qiwi.png"/><span>'+tr('payment.QIWI')+'</span></div>',//tr
                    name: "paymentType",
                    inputValue: 'QW'
                },
                {
                    boxLabel: '<div class="payment-div"><img src="images/payment/sberbank.png"/><span>'+tr('payment.sberbank')+'</span></div>',//tr
                    name: "paymentType",
                    inputValue: 'SB',
                    margin:'0 5 0 5'
                },
            ]
        },
        {
            name: "cps_email",
            fieldLabel: tr('usersettings.email'),
            validator: function (val) {
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
            value: tr('payment.emailFormat'),
            margin: '-12 0 10 170',
            cls: 'field-hint-subtext'
        },
        {
            name: "cps_phone",
            fieldLabel: tr('usersettings.mobile'),
            validator: function (val) {
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
            value: tr('payment.phoneFormat'),
            margin: '-12 0 10 170',
            cls: 'field-hint-subtext'
        }
        ,
        {
            fieldLabel: tr('payment.comment'),
            name: "comment"
        },
        {
            xtype:'hidden',
            name:"shopId"
        },
        {
            xtype:'hidden',
            name:"scId"
        },
        {
            xtype:'hidden',
            name:"customerNumber"
        }
    ]
});
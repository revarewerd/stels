Ext.define('Seniel.view.reports.SendMailWindow', {
    extend: 'Ext.window.Window',
    alias: 'widget.sendmailwnd',
    minWidth: 480,
    minHeight: 120,
    layout: 'fit',
    modal: true,
    icon: 'images/ico16_msghist.png',
    title: tr('sendmailwindow.title'),
    items: [
        {
            xtype: 'form',
            layout: 'anchor',
            defaults: {
                xtype: 'textfield',
                allowBlank: false,
                labelPad: 12,
                labelWidth: 180,
                padding: 8,
                anchor: '100%',
                listeners: {
                    boxready: function() {
                        if (this.cookieParam && Ext.util.Cookies.get(this.cookieParam)) {
                            this.setValue(Ext.util.Cookies.get(this.cookieParam));
                        }
                    }
                }
            },
            items: [
                {
                    name: 'email',
                    fieldLabel: tr('sendmailwindow.mail'),
                    cookieParam: 'repMail',
                    vtype: 'email'
                },
                {
                    name: 'organization',
                    fieldLabel: tr('sendmailwindow.org'),
                    cookieParam: 'repOrgFrom'
                }
            ]
        }
    ],
     bbar: [
        '->',
        {
            xtype: 'button',
            text: tr('main.ok'),
            icon: 'images/ico16_okcrc.png',
            handler: function(btn) {
                var wnd = btn.up('window'),
                    form = wnd.down('form');
                if (form && !form.hasInvalidField() && wnd.urlPrefix) {
                    var values = form.getValues();
                    wnd.setLoading(tr('sendmailwindow.sending'));
                    Ext.util.Cookies.set('repMail', values.email, new Date(new Date().getTime()+(1000*60*60*24*365)));
                    Ext.util.Cookies.set('repOrgFrom', values.organization, new Date(new Date().getTime()+(1000*60*60*24*365)));
                    
                    Ext.Ajax.request({
                            url: '/EDS/addressReport/sendEmail',
                            params: (wnd.urlPrefix + '&action=sendemail&email=' + encodeURIComponent(values.email) + '&organization=' + encodeURIComponent(values.organization)),
                            method: 'post',
                            callback: function(opts, success, resp) {
                                wnd.setLoading(false);
                                if (success) {
                                    Ext.MessageBox.show({
                                        title: tr('sendmailwindow.success.title'),
                                        msg: tr('sendmailwindow.success') + ' ' + values.email,
                                        icon: Ext.MessageBox.INFO,
                                        buttons: Ext.Msg.OK
                                    });
                                    wnd.close();
                                } else {
                                    Ext.MessageBox.show({
                                        title: tr('sendmailwindow.error.title'),
                                        msg: resp.responseText,
                                        icon: Ext.MessageBox.ERROR,
                                        buttons: Ext.Msg.OK
                                    });
                                }
                            }
                        }
                    );
                } else {
                    Ext.MessageBox.show({
                        title: tr('sendmailwindow.emptyfields.title'),
                        msg: tr('sendmailwindow.emptyfields'),
                        icon: Ext.MessageBox.WARNING,
                        buttons: Ext.Msg.OK
                    });
                }
            }
        },
        {
            xtype: 'button',
            text: tr('main.cancel'),
            icon: 'images/ico16_cancel.png',
            handler: function(btn) {
                var wnd = btn.up('window');
                wnd.close();
            }
        }
    ]
});
Ext.define('Billing.view.SMSSendingWindow', {
    extend: 'Ext.window.Window',
    alias: 'widget.smssendwnd',
    width: 600,
    height: 200,
    maximizable: true,
    layout: 'fit',
    initComponent: function () {
        var self = this;
        Ext.apply(this,{
            items: [
                {
                    xtype:'form',
                    layout:'fit',
                    items:[{
                        xtype: 'textareafield',
                        name: 'SMSText',
                        anchor:'100%',
                        emptyText:'Введите текст сообщения',
                        maxWidth :600
                    }],
                    bbar: [
                        '->',
                        {
                            xtype: 'button',
                            text: 'Готово',
                            handler: function () {
                                self.sendSMS(this.up('form').getValues().SMSText)
                                this.up('window').close()
                            }
                        },

                        {
                            xtype: 'button',
                            text: 'Очистить',
                            handler: function () {
                                this.up('form').getForm().reset()
                            }
                        },

                        {
                            xtype: 'button',
                            text: 'Отменить',
                            handler: function () {
                                this.up('window').close()

                            }
                        }
                    ]
                }
            ]
        })
        this.callParent();
    },
    sendSMS:function(smstext){
        var self = this;
        console.log('phone', this.phone);
        console.log('smstext', smstext);
        trackerMesService.sendSMSToTracker(this.phone, smstext, function (res,e) {
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
                console.log("Результат запроса", result, " e=", e);
                self.onSmsSent(res);
            }
        });
    },
    onSmsSent: function (res) {

    }
})
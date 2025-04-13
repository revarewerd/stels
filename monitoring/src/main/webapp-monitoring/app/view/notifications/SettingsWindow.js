/**
 * Created by IVAN on 25.05.2015.
 */
Ext.define('Seniel.view.notifications.SettingsWindow', {
    extend: 'Seniel.view.WRWindow',
    alias: 'widget.editnotwnd',
    stateId: 'ntfListWnd',
    stateful: true,
    icon: 'images/ico16_options.png',
    title: tr('main.notification.settings'),
    btnConfig: {
        icon: 'images/ico24_options.png',
        text: tr('main.notification.settings')
    },
    minHeight: 200,
    minWidth: 400,
    maxHeight: 200,
    maxWidth: 400,
    layout: 'fit',
    resizable: false,
    maximizable: false,
    initComponent: function () {
        var self = this;
        userInfo.getUserSettings(function (resp) {
            self.down("[name=showEventMarker]").setValue(resp.showEventMarker);
            self.down("[name=showPopupNotificationsWindow]").setValue(resp.showPopupNotificationsWindow);
            self.down("[name=showUnreadNotificationsCount]").setValue(resp.showUnreadNotificationsCount);
        });
        this.callParent();
    },
    items: [
        {
            xtype: 'form',
            items: [
                {
                    margin: '20 20 10 20',
                    name: 'showEventMarker',
                    boxLabel: tr('main.notification.settings.showEventMarker'),
                    xtype: 'checkbox',
                    uncheckedValue: false,
                    inputValue: true
                },
                {
                    margin: '0 20 10 20',
                    name: 'showPopupNotificationsWindow',
                    boxLabel: tr('main.notification.settings.showPopupWindow'),
                    xtype: 'checkbox',
                    uncheckedValue: false,
                    inputValue: true
                },
                {
                    margin: '0 20 20 20',
                    name: 'showUnreadNotificationsCount',
                    boxLabel: tr('main.notification.settings.showUnreadNotificationsCount'),
                    xtype: 'checkbox',
                    uncheckedValue: false,
                    inputValue: true
                }
            ],
            dockedItems: [
                {
                    xtype: 'toolbar',
                    dock: 'bottom',
                    items: [
                        '->',
                        {
                            text: tr("main.apply"),
                            handler: function (btn, e) {
                                var data = btn.up('form').getValues()
                                console.log("Применить настройки отображения уведомлений, data=", data);
                                userInfo.updateUserSettings(data, function (resp) {
                                    console.log("result=", resp);
                                    var viewport = btn.up('viewport');
                                    var mainmap = viewport.down('mainmap');
                                    mainmap.showEventMarker = data.showEventMarker;
                                    mainmap.showPopupNotificationsWindow = data.showPopupNotificationsWindow;
                                    mainmap.showUnreadNotificationsCount = data.showUnreadNotificationsCount;
                                    btn.up('window').close();
                                    location.reload();
                                })
                            }
                        },
                        {
                            text: tr("main.cancel"),
                            handler: function (btn, e) {
                                btn.up('window').close();
                            }
                        }
                    ]
                }
            ]
        }
    ],
});
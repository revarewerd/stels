var timeout = 3 * 60 * 1000;
Ext.override(Ext.grid.View, { enableTextSelection: true });
Ext.override(Ext.view.AbstractView, { 
    loadingText:'Загрузка...'            
});
Ext.override(Ext.menu.Menu, { 
    hideMode: 'display'            
});
Ext.override(Ext.data.Connection, {
    timeout: timeout
});
Ext.override(Ext.data.proxy.Ajax, {
    timeout: timeout
});
Ext.override(Ext.form.field.Date, {
    startDay: 1
});
Ext.override(Ext.picker.Date, {
    startDay: 1
});
Ext.override(Ext.selection.Model, {
    storeHasSelected: function(record) {
        var store = this.store,
            records,
            len, id, i, m;

        if (record.hasId()) {
            return store.getById(record.getId());
        } else {
            if (store.buffered) {//on buffered stores the map holds the data items
                records = [];
                for (m in store.data.map) {
                    records = records.concat(store.data.map[m].value);
                }
            } else {
                records = store.data.items;
            }
            len = records.length;
            id = record.internalId;


            for (i = 0; i < len; ++i) {
                if (id === records[i].internalId) {
                    return true;
                }
            }
        }
        return false;
    }
});

Ext.Loader.setConfig({
    enabled: true
});

Ext.require('Ext.direct.*', function() {
    Ext.direct.Manager.addProvider(Ext.app.REMOTING_API);
});

Ext.application({
    name: 'Seniel',
    launch: function() {
        Ext.require('Seniel.view.mobile.*', function() {
            console.log('Messages, Objects, Map classes loaded');
        });
        
        Ext.require('Seniel.view.WRUtils');
        
        var objectsList = Ext.create('Seniel.view.mobile.Objects', {
            region: 'west',
            itemId: 'panelObjects'
        });
        var mapContainer = Ext.create('Seniel.view.mobile.Map', {
            region: 'center',
            itemId: 'panelMap',
            hidden: true
        });
        var messagesList = Ext.create('Seniel.view.mobile.Messages', {
            region: 'east',
            itemId: 'panelMessages',
            hidden: true
        });
        var messageBox = Ext.create('Seniel.view.mobile.MessageBox', {});

        Ext.create('Ext.container.Viewport', {
            messageBox: messageBox,
            layout: 'border',
            cls: 'mobile-version',
            padding: 0,
            margin: 0,
            border: 0,
            items: [
                {
                    region: 'north',
                    xtype: 'toolbar',
                    defaults: {
                        scale: 'large',
                        toggleGroup: 'mainMenu',
                        allowDepress: false,
                        toggleHandler: function(btn, pressed) {
                            var viewport = btn.up('viewport');
                            if (btn.defaultIcon && btn.icon !== btn.defaultIcon) {
                                btn.setIcon(btn.defaultIcon);
                            }
                            if (pressed) {
                                viewport.down('#' + btn.panelName).show();
                            } else {
                                viewport.down('#' + btn.panelName).hide();
                            }
                        }
                    },
                    items: [
                        '<a href="' + homesite + '" target="_blank" title="'+tr('main.homepage')+'"><img class="mobile-logo" src="EDS/logo/monitoringlogo.png" alt="" style="width: 240px;" /></a>',
                        {
                            icon: 'images/ico32_object.png',
                            defaultIcon: 'images/ico32_object.png',
                            text: tr('mobile.objects'),
                            textSnapshot: tr('mobile.objects'),
                            panelName: 'panelObjects',
                            itemId: 'btnObjectsPanel',
                            cls: 'mobile-toolbar-button',
                            flex: 1,
                            pressed: true
                        },
                        {
                            icon: 'images/ico32_geozone.png',
                            defaultIcon: 'images/ico32_geozone.png',
                            text: tr('mobile.map'),
                            textSnapshot: tr('mobile.map'),
                            panelName: 'panelMap',
                            itemId: 'btnMapPanel',
                            cls: 'mobile-toolbar-button',
                            flex: 1
                        },
                        {
                            icon: 'images/ico32_messages.png',
                            defaultIcon: 'images/ico32_messages.png',
                            text: tr('mobile.messages'),
                            textSnapshot: tr('mobile.messages'),
                            panelName: 'panelMessages',
                            itemId: 'btnMessagesPanel',
                            cls: 'mobile-toolbar-button',
                            flex: 1
                        },
                        {
                            xtype: 'tbspacer',
                            width: 20
                        },
                        {
                            text: (Ext.util.Cookies.get('lang') ? Ext.util.Cookies.get('lang') : 'ru').toUpperCase(),
                            tooltip: tr('main.selectlanguage.tooltip'),
                            tooltipType: 'title',
                            width: 40,
                            enableToggle: false,
                            toggleGroup: null,
                            toggleHandler: null,
                            menu: {
                                defaults: {
                                    xtype: 'menucheckitem',
                                    group: 'chosenLanguage',
                                    handler: function(item) {
                                        var value = (item.itemValue) ? (item.itemValue) : 'ru';
                                        Ext.util.Cookies.set('lang', value, Ext.Date.add(new Date(), Ext.Date.YEAR, 100));
                                        window.location.reload();
                                    }
                                },
                                listeners: {
                                    boxready: function(menu) {
                                        var lang = Ext.util.Cookies.get('lang') ? Ext.util.Cookies.get('lang') : 'ru';
                                        Ext.Array.each(menu.items.items, function(i) {
                                            if (i.itemValue === lang) {
                                                i.setChecked(true);
                                            }
                                        });
                                    }
                                },
                                items: [
                                    {
                                        text: 'Русский',
                                        itemValue: 'ru'
                                    },
                                    {
                                        text: 'English',
                                        itemValue: 'en'
                                    },
                                    {
                                        text: 'Español',
                                        itemValue: 'es'
                                    }
                                ]
                            }
                        },
                        {
                            xtype: 'tbspacer',
                            width: 20
                        },
                        {
                            icon: '../images/ico32_tv.png',
                            tooltip: tr('main.desktopversion'),
                            tooltipType: 'title',
                            width: 40,
                            enableToggle: false,
                            toggleGroup: null,
                            handler: function() {
                                Ext.util.Cookies.set('isMobileVersion', false, Ext.Date.add(new Date(), Ext.Date.MONTH, 1));
                                window.location = "/";
                            },
                            toggleHandler: null
                        },
                        {
                            icon: '../images/ico32_shutdown.png',
                            tooltip: tr('main.exit.tooltip'),
                            tooltipType: 'title',
                            width: 40,
                            enableToggle: false,
                            toggleGroup: null,
                            handler: function(btn) {
                                loginService.logout(function() {
                                    window.location = "/j_spring_security_logout";
                                });
                            },
                            toggleHandler: null
                        }
                    ]
                },
                {
                    region: 'center',
                    xtype: 'panel',
                    header: false,
                    layout: 'border',
                    defaults: {
                        width: '100%',
                        height: '100%'
                    },
                    items: [objectsList, mapContainer, messagesList]
                }
            ],
            listeners: {
                resize: function(viewport, width, height, oldWidth, oldHeight) {
                    var tb = viewport.down('toolbar[region="north"]');
                    var tbItems = tb.items.items;
                    if (width < 480) {
                        tbItems[0].setText('<a href="' + homesite + '" target="_blank" title="'+tr('main.homepage')+'"><img class="mobile-logo" src="EDS/logo/mobilelogo.png" alt="" style="width: 32px;" /></a>');
                        for (var i = 1; i < 4; i++) {
                            tbItems[i].setText('');
                            tbItems[i].setIconCls('mobile-toolbar-button-small');
                        }
                    } else if (width < 720) {
                        tbItems[0].setText('<a href="' + homesite + '" target="_blank" title="'+tr('main.homepage')+'"><img class="mobile-logo" src="EDS/logo/mobilelogo.png" alt="" style="width: 32px;" /></a>');
                    } else {
                        tbItems[0].setText('<a href="' + homesite + '" target="_blank" title="'+tr('main.homepage')+'"><img class="mobile-logo" src="EDS/logo/monitoringlogo.png" alt="" style="width: 240px;" /></a>');
                        for (var i = 1; i < 4; i++) {
                            tbItems[i].setText(tbItems[i].textSnapshot);
                            tbItems[i].setIconCls('');
                        }
                    }
                },
                beforerender: function(viewport) {
                    var tb = viewport.down('toolbar[region="north"]');
                    var tbItems = tb.items.items;
                    if (viewport.getWidth() < 480) {
                        tbItems[0].setText('<a href="' + homesite + '" target="_blank" title="'+tr('main.homepage')+'"><img class="mobile-logo" src="EDS/logo/mobilelogo.png" alt="" style="width: 32px;" /></a>');
                        for (var i = 1; i < 4; i++) {
                            tbItems[i].setText('');
                            tbItems[i].setIconCls('mobile-toolbar-button-small');
                        }
                    } else if (viewport.getWidth() < 720) {
                        tbItems[0].setText('<a href="' + homesite + '" target="_blank" title="'+tr('main.homepage')+'"><img class="mobile-logo" src="EDS/logo/mobilelogo.png" alt="" style="width: 32px;" /></a>');
                    }
                }
            }
        });
    }
});
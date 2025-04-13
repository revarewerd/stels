Ext.Loader.setConfig({
    enabled: true
});

Ext.require('Ext.direct.*', function () {
    Ext.direct.Manager.addProvider(Ext.app.REMOTING_API);
});

Ext.application(
    {
        name: 'Seniel',


        controllers: [
            'MapObjects'
        ],


        launch: function () {
            Timezone.setUserDefaultTimezone()
            Ext.require([
                'Seniel.view.WRUtils',
                'Seniel.view.GroupOfObjectsForm',
                'Seniel.view.GroupsOfObjectsPanel',
                'Seniel.view.reports.ReportsMenu',
                'Ext.state.*'
            ]);
            Ext.state.Manager.setProvider(Ext.create('Ext.state.CookieProvider',{
                expires: new Date(new Date().getTime()+(1000*60*60*24*365))
            }));
            
            //Ext.override(Ext.container.Viewport)
            Ext.create('Ext.container.Viewport', {


                layout: 'border',
                wndcount: 0,
                preactive: 0,
                active: 0,
                items: [
                    {
                        region: 'north',
                        xtype: 'toolbar',
                        defaults:{
                            scale: 'medium'
                        },
                        enableOverflow:true,
                        items: [
                            '<div id="homePageLink"><a href="'+homesite+'" target="_blank" title="'+tr('main.homepage')+'"><img src="EDS/logo/monitoringlogo.png" alt="" style="position:relative; top:2px; width:240px; height:32px;" /></a></div>',
                            '-',
                            {
                                text: tr('main.reports'),
                                itemId: 'reportsCtrl',
                                icon: 'images/ico24_report.png',
                                tooltip: tr('main.reports.tooltip'),
                                tooltipType: 'title',
                                disabled: true,
                                menu: Ext.create('Seniel.view.reports.ReportsMenu')
                            }, 
                            {
                                //text: tr('main.notification'),
                                text:'<div style="display:inline; padding-right:5px">'+ tr('main.notification')+
                                '</div><div id="unreadUserMessages" style="display:none; background-color:deepskyblue; color:white; padding-left:4px; padding-right:4px"></div>',
                                itemId: 'notificationsCtrl',
                                icon: 'images/ico24_bell.png',
                                tooltip: tr('main.notification.tooltip'),
                                tooltipType: 'title',
                                disabled: true,
                                menu: {
                                    items: [
                                        {
                                            text: tr('main.notificationhistory'),
                                            icon: 'images/ico16_eventsmsgs.png',
                                            tooltip: tr('main.notificationhistory.tooltip'),
                                            tooltipType: 'title',
                                            handler: function () {
                                                var viewport = this.up('viewport');
                                                if (!this.openedWnd) {
                                                    this.openedWnd = Ext.create('Seniel.view.notifications.HistoryWindow', {});
                                                    
                                                    var self = this;
                                                    this.openedWnd.on('close', function() {
                                                        delete self.openedWnd;
                                                    });
                                                    viewport.showNewWindow(this.openedWnd);
                                                } else {
                                                    this.openedWnd.setActive(true);
                                                }
                                            }
                                        },
                                        '-',
//                                        {
//                                            icon: 'images/ico16_plus_def.png',
//                                            text: 'Создать правило',
//                                            tooltip: 'Создать правило уведомления о событии',
//                                            tooltipType: 'title',
//                                            handler: function() {
//                                                var viewport = this.up('viewport');
//                                                var wnd = Ext.create('Seniel.view.notifications.RuleWindow', {});
//                                                viewport.showNewWindow(wnd);
//                                            }
//                                        },
                                        {
                                            icon: 'images/ico16_edit_def.png',
                                            text: tr('main.notificationrules'),
                                            tooltip: tr('main.notificationrules.tooltip'),
                                            tooltipType: 'title',
                                            handler: function() {
                                                var viewport = this.up('viewport');
                                                if (!this.openedWnd) {
                                                    this.openedWnd = Ext.create('Seniel.view.notifications.RulesListWindow', {});
                                                    
                                                    var self = this;
                                                    this.openedWnd.on('close', function() {
                                                        delete self.openedWnd;
                                                    });
                                                    viewport.showNewWindow(this.openedWnd);
                                                } else {
                                                    this.openedWnd.setActive(true);
                                                }
                                            }
                                        },
                                        {
                                            icon: 'images/ico16_options.png',
                                            text: tr('main.notification.settings'),
                                            tooltip: tr('main.notification.settings'),
                                            tooltipType: 'title',
                                            handler: function() {
                                                var viewport = this.up('viewport');
                                                if (!this.openedWnd) {
                                                    this.openedWnd = Ext.create('Seniel.view.notifications.SettingsWindow', {});

                                                    var self = this;
                                                    this.openedWnd.on('close', function() {
                                                        delete self.openedWnd;
                                                    });
                                                    viewport.showNewWindow(this.openedWnd);
                                                } else {
                                                    this.openedWnd.setActive(true);
                                                }
                                            }
                                        }
                                    ]
                                }
                            },
                            {
                                text: tr('main.geotools'),
                                itemId: 'geoTools',
                                icon: 'images/ico24_geozone.png',
                                tooltip: tr('main.geotools.tooltip'),
                                tooltipType: 'title',
                                menu: {
                                    items: [
//                                        {
//                                            icon: 'images/ico16_plus_def.png',
//                                            text: 'Создать геозону',
//                                            tooltip: 'Создать новую геозону',
//                                            tooltipType: 'title',
//                                            handler: function(btn) {
//                                                var viewport = btn.up('viewport'),
//                                                    mainmap = viewport.down('mainmap');
//                                                if (!mainmap.geozonetbr) {
//                                                    mainmap.geozonetbr = Ext.create('Seniel.view.GeoZoneToolbar', {
//                                                        y: 0,
//                                                        map: mainmap,
//                                                        menubtn: btn
//                                                    });
//                                                    mainmap.add([mainmap.geozonetbr]);
//                                                    mainmap.geozonetbr.on('beforehide', function(wnd) {
//                                                        wnd.map.drawGeozonesLayer.removeAllFeatures();
//                                                        wnd.down('button[itemId="geozPoly"]').toggle(false);
//                                                        wnd.down('button[itemId="geozRect"]').toggle(false);
//                                                        wnd.down('button[itemId="geozCrcl"]').toggle(false);
//                                                        wnd.down('button[itemId="geozResh"]').toggle(false);
//                                                        wnd.down('button[itemId="geozMove"]').toggle(false);
//                                                        wnd.map.drawGeozonesLayer.drawStarted = false;
//                                                        wnd.down('textfield[itemId="geozName"]').setValue('');
//                                                        wnd.menubtn.enable();
//                                                        wnd.curfeature = null;
//                                                    });
//                                                }
//                                                mainmap.geozonetbr.show();
//                                                btn.disable();
//                                            }
//                                        },
                                        {
                                            icon: 'images/ico16_geozone.png',
                                            text: tr('editgeozoneswnd.geozones'),
                                            tooltip: tr('editgeozoneswnd.title'),
                                            tooltipType: 'title',
                                            handler: function() {
                                                var viewport = this.up('viewport');
                                                if (!this.openedWnd) {
                                                    this.openedWnd = Ext.create('Seniel.view.EditGeozonesWindow', {});
                                                    
                                                    var self = this;
                                                    this.openedWnd.on('close', function() {
                                                        delete self.openedWnd;
                                                    });
                                                
                                                    viewport.showNewWindow(this.openedWnd);
                                                } else {
                                                    this.openedWnd.setActive(true);
                                                }
                                                    
//                                                viewport.addMask(wnd)
//                                                var maincontainer = viewport.down('container[itemId="maincontainer"]');
//                                                maincontainer.add([wnd]);
//                                                wnd.on('boxready', function() {
//                                                    var grid = wnd.down('editgeozgrid');
//                                                    grid.getStore().add(viewport.geozArray);
//                                                });
//                                                wnd.on('close', function() {
//                                                    btn.enable();
//                                                });
//                                                wnd.show();
                                            }
                                        }
                                    ]
                                }
                            },
                            {
                                text: tr('main.groupsofobjects'),
                                itemId: 'groupsOfObjects',
                                icon: 'images/cars/car_001_blu_24.png',
                                tooltip: tr('main.groupsofobjects'),
                                tooltipType: 'title',
                                handler: function() {
                                    var viewport = this.up('viewport');
                                    if (!this.openedWnd) {
                                        this.openedWnd = Ext.create('Seniel.view.GroupsOfObjectsPanel.Window', {});

                                        var self = this;
                                        this.openedWnd.on('close', function() {
                                            delete self.openedWnd;
                                        });
                                        viewport.showNewWindow(this.openedWnd);
                                    } else {
                                        this.openedWnd.setActive(true);
                                    }
                                }
                            },
                            {
                                itemId: 'supportCtrl',
                                text:'<div style="display:inline; padding-right:5px">'+tr('support')+
                                    '</div><div id="unreadUserTickets" style="display:none; background-color:deepskyblue; color:white; padding-left:4px; padding-right:4px"></div>',
                                icon: 'images/ico24_question_def.png',
                                tooltip:tr('support'),
                                tooltipType: 'title',
                                menu: {
                                    items: [
                                        {
                                            icon: 'images/ico16_edit_def.png',
                                            text: tr('support.request.new'),
                                            tooltip: tr('support.request.new'),
                                            tooltipType: 'title',
                                            handler: function() {
                                                var viewport = this.up('viewport');
                                                if (!this.openedWnd) {
                                                    this.openedWnd = Ext.create("Seniel.view.support.SupportRequestWindow",{})//Ext.create('Seniel.view.GroupsOfObjectsPanel.Window', {});

                                                    var self = this;
                                                    this.openedWnd.on('close', function() {
                                                        delete self.openedWnd;
                                                    });
                                                    viewport.showNewWindow(this.openedWnd);
                                                } else {
                                                    this.openedWnd.setActive(true);
                                                }
                                            }
                                        },
                                        {
                                            icon: 'images/ico16_eventsmsgs.png',
                                            text: tr('support.request.my'),
                                            tooltip: tr('support.request.my'),
                                            tooltipType: 'title',
                                            handler: function() {
                                                var viewport = this.up('viewport');
                                                if (!this.openedWnd) {
                                                    this.openedWnd = Ext.create("Seniel.view.support.UserTicketsWindow",{})//Ext.create('Seniel.view.GroupsOfObjectsPanel.Window', {});

                                                    var self = this;
                                                    this.openedWnd.on('close', function() {
                                                        delete self.openedWnd;
                                                    });
                                                    viewport.showNewWindow(this.openedWnd);
                                                } else {
                                                    this.openedWnd.setActive(true);
                                                }
                                            }
                                        },
                                    ]
                                }
                            },
                            '->',
                            {
                                xtype: 'tbspacer',
                                width: 12
                            },
                            tr('main.account') + ':',
                            {
                                xtype: 'label',
                                html: 'none',
                                initComponent: function () {
                                    var self = this;
                                    userInfo.getUserMainAcc(function (result) {
                                        self.setText('<b>' + result.name + '</b>', false);
                                    });
                                    this.callParent();
                                }
                            },
                            {
                                xtype: 'tbspacer',
                                width: 12
                            },
                            {
                                icon: 'images/ico24_user.png',
                                itemId: 'userSettings',
                                tooltip: tr('main.userSettings'),
                                tooltipType: 'title',
                                listeners: {
                                    boxready: function(btn) {
                                        loginService.getLogin(function(result) {
                                            btn.setText(result);
                                        });
                                    }
                                },
                                handler: function(btn) {
                                    var viewport = this.up('viewport');
                                    
                                    if (!btn.openedWnd) {
                                        btn.openedWnd = Ext.create('Seniel.view.UserSettingsWindow', {
                                            userName: btn.getText()
                                        });
                                        btn.openedWnd.on('close', function() {
                                            delete btn.openedWnd;
                                        });
                                        
                                        viewport.showNewWindow(btn.openedWnd);
                                    } else {
                                        btn.openedWnd.setActive(true);
                                    }
                                }
                            },
                            {
                                xtype: 'tbspacer',
                                width: 12
                            },
                            {
                                text: (Ext.util.Cookies.get('lang') ? Ext.util.Cookies.get('lang') : 'ru').toUpperCase(),
                                tooltip: tr('main.selectlanguage.tooltip'),
                                tooltipType: 'title',
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
                                width: 12
                            },
                            {
                                icon: '../images/ico24_mobile.png',
                                tooltip: tr('main.mobileversion'),
                                tooltipType: 'title',
                                handler: function() {
                                    Ext.util.Cookies.set('isMobileVersion', true, Ext.Date.add(new Date(), Ext.Date.MONTH, 1));
                                    window.location = "/mobile.html";
                                }
                            },
                            {
                                text: tr('main.exit'),
                                icon: 'images/ico24_shutdown.png',
                                tooltip: tr('main.exit.tooltip'),
                                tooltipType: 'title',
                                handler: function () {
                                    loginService.logout(function () {
                                        window.location = "/j_spring_security_logout";
                                    });
                                }
                            }
                        ],
                        listeners: {
                            resize: function (tb) {
                                var homePageLink = Ext.fly("homePageLink")
                                if (document.body.clientWidth < 1350)
                                    homePageLink.setHTML('<a href="' + homesite + '" target="_blank" title="' + tr('main.homepage') + '"><img src="EDS/logo/mobilelogo.png" alt="" style="position:relative; top:2px; width:32px; height:32px;" /></a>');
                                else
                                    homePageLink.setHTML('<a href="' + homesite + '" target="_blank" title="' + tr('main.homepage') + '"><img src="EDS/logo/monitoringlogo.png" alt="" style="position:relative; top:2px; width:240px; height:32px;" /></a>');
                                if (document.body.clientWidth < 1150) {
                                    tb.down('#reportsCtrl').setText('');
                                    tb.down('#notificationsCtrl').setText('<div style="display:inline; padding-right:5px">' +
                                        '</div><div id="unreadUserMessages" style="display:none; background-color:deepskyblue; color:white; padding-left:4px; padding-right:4px"></div>');
                                    tb.down('#geoTools').setText('');
                                    tb.down('#groupsOfObjects').setText('');
                                    tb.down('#supportCtrl').setText('<div style="display:inline; padding-right:5px">' +
                                        '</div><div id="unreadUserTickets" style="display:none; background-color:deepskyblue; color:white; padding-left:4px; padding-right:4px"></div>');

                                }
                                else {
                                    tb.down('#reportsCtrl').setText(tr('main.reports'));
                                    tb.down('#notificationsCtrl').setText('<div style="display:inline; padding-right:5px">' + tr('main.notification') +
                                        '</div><div id="unreadUserMessages" style="display:none; background-color:deepskyblue; color:white; padding-left:4px; padding-right:4px"></div>');
                                    tb.down('#geoTools').setText(tr('main.geotools'));
                                    tb.down('#groupsOfObjects').setText(tr('main.groupsofobjects'));
                                    tb.down('#supportCtrl').setText('<div style="display:inline; padding-right:5px">' + tr('support') +
                                        '</div><div id="unreadUserTickets" style="display:none; background-color:deepskyblue; color:white; padding-left:4px; padding-right:4px"></div>');
                                }

                            }
                        }
                    },
                    {
                        region: 'center',
                        xtype: 'container',
                        itemId: 'maincontainer',
                        layout: 'border',
                        items: [
                            {
                                region: 'center',
                                xtype: 'mainmap'
                            },
                            {
                                collapsible: true,
                                collapseMode: 'header',
                                split: true,
                                region: 'west',
                                width: 420,
                                xtype: 'leftpanel'
                            }
                        ]
                    },
                    {
                        xtype: 'toolbar',
                        region: 'south',
                        itemId: 'reporttoolbar',
                        defaults:{
                            scale: 'medium'
                        },
                        border: 'false',
                        items: [
                            {
                                icon: 'images/ico24_hideall2.png',
                                tooltip: tr('main.hideallwnds'),
                                tooltipType: 'title',
                                disabled: true,
                                handler: function () {
                                    Ext.WindowManager.each(function(comp){                                                       
                                        if (comp.xtype === 'window' || comp.getXTypes().search('window') !== -1) comp.hide();
                                    });
                                }
                            },
                            '-'
                        ]
                    }
                ],
                addMask:function(wnd){
                    var active=Ext.WindowManager.getActive();
                    if(active) active.setActive(false);
                    var myMask = new Ext.LoadMask(this, {
                        msg:"Загрузка..."
                    });
                    myMask.show();
                    wnd.on('show', function() {
                        console.log('ASFTERSHOW');
                        console.log(Ext.WindowManager.getActive().title);
                        myMask.hide();
                    });
                },
                showNewWindow: function(wnd) {
                    this.addMask(wnd);
                    var maincontainer = this.down('container[itemId="maincontainer"]');
                    maincontainer.add([wnd]);
                    wnd.show();
                },
                showNewWindowAt: function(wnd,x,y) {
                    this.addMask(wnd);
                    var maincontainer = this.down('container[itemId="maincontainer"]');
                    maincontainer.add([wnd]);
                    wnd.showAt(x,y);
                },
                createOnlyOneWnd:function (idname, type, bodyDescr) {

                    bodyDescr['id'] = idname;

                    var existingWindow = Ext.getCmp(idname);
                    if (!existingWindow) {
                        existingWindow = Ext.create(type, bodyDescr);
                    }
                    else
                    {
                        existingWindow.close();
                        existingWindow = Ext.create(type, bodyDescr);
                    }

                    return existingWindow
                },
                createOrFocus:function (idname, type, bodyDescr) {
                    var self=this;
                    bodyDescr['id'] = idname;

                    var existingWindow = Ext.getCmp(idname)
                    if (!existingWindow) {
                        existingWindow = Ext.create(type, bodyDescr)
                        existingWindow.justCreated = true
                    }
                    else
                    {
                        existingWindow.setActive(true);
                        //existingWindow.focus()
                        existingWindow.justCreated = false;
                    }

                    return existingWindow
                },
                listeners: {
                    beforerender: function () {
                        var self = this,
                            molg = self.down('mapobjectlist'),
                            mapc = self.down('mainmap');
                        self.geozStore = Ext.create('EDS.store.GeozonesData', {
                            autoLoad: true
                        });
                        self.geozStore.on('load', function (store, recs) {
                            var arr = new Array();
                            if (!self.geozOnMap) {
                                self.geozOnMap = new Array();
                            }
                            var onMap = Ext.util.Cookies.get('geozOnMap');
                            if (onMap) {
                                var geozones = onMap.split(',');
                                for (var i in geozones) {
                                    store.each(function (rec) {
                                        if (rec.get('id').toString() === geozones[i]) {
                                            rec.set('isOnMap', true);
                                            mapc.addGeozone(rec);
                                        }
                                    });
                                }
                            }
                            store.each(function (rec) {
                                arr.push({
                                    'id': rec.get('id'),
                                    'name': rec.get('name'),
                                    'ftColor': rec.get('ftColor'),
                                    'points': rec.get('points'),
                                    'isOnMap': (rec.get('isOnMap')) ? (true) : (false)
                                });
                            });
                            self.geozArray = arr;
                            var geozgrid = self.down('editgeozgrid');
                            if (geozgrid && geozgrid.updateStore) {
                                geozgrid.getStore().removeAll();
                                geozgrid.getStore().add(arr);
                                geozgrid.updateStore = false;
                            }
                        });

                        userInfo.getUserContacts(function (userinfo) {
                            console.log('User contacts = ', userinfo);
                            self.userPhone = userinfo.phone;
                            self.userEmail = userinfo.email;
                        });

                        userInfo.getDetailedBalanceRules(function (resp) {
                            if (resp && resp.showbalance) {
                                var balance = (resp.balance) ? (resp.balance) : (0),
                                    cmp;
                                var balanceItems = [
                                    {
                                        icon: 'images/ico16_coins.png',
                                        text: tr('main.abonentprice'),
                                        tooltip: tr('main.abonentprice.tooltip'),
                                        tooltipType: 'title',
                                        handler: function (btn) {
                                            var viewport = btn.up('viewport');
                                            if (!btn.openedWnd) {
                                                btn.openedWnd = Ext.create('Seniel.view.SubscriptionFeeWindow', {});

                                                btn.openedWnd.on('close', function () {
                                                    delete btn.openedWnd;
                                                });
                                                viewport.showNewWindow(btn.openedWnd);
                                            } else {
                                                btn.openedWnd.setActive(true);
                                            }
                                        }
                                    },
                                    {
                                        icon: 'images/ico16_coins.png',
                                        text: tr('notificationpay.title'), // TODO
                                        handler: function (btn) {
                                            var viewport = btn.up('viewport');
                                            if (!btn.openedWnd) {
                                                btn.openedWnd = Ext.create('Seniel.view.NotificationPaymentWindow', {});

                                                btn.openedWnd.on('close', function () {
                                                    delete btn.openedWnd;
                                                });
                                                viewport.showNewWindow(btn.openedWnd);
                                            } else {
                                                btn.openedWnd.setActive(true);
                                            }
                                        }
                                    }]
                                paymentClientService.canMakePayment(function (canMakePayment) {
                                    console.log("canMakePayment", canMakePayment)
                                    if (canMakePayment)
                                        balanceItems.push({
                                            icon: 'images/ico16_money.png',
                                            text: tr('payment.addfunds'),
                                            tooltip: tr('payment.addfunds.tooltip'),
                                            tooltipType: 'title',
                                            handler: function (btn) {
                                                var viewport = btn.up('viewport');
                                                if (!btn.openedWnd) {
                                                    btn.openedWnd = Ext.create('Seniel.view.PaymentWindow', {});

                                                    btn.openedWnd.on('close', function () {
                                                        delete btn.openedWnd;
                                                    });
                                                    viewport.showNewWindow(btn.openedWnd);
                                                } else {
                                                    btn.openedWnd.setActive(true);
                                                }
                                            }
                                        })
                                    console.log("balanceItems", balanceItems)
                                    if (resp.showfeedetails) {
                                        cmp = Ext.create('Ext.button.Button', {
                                            icon: 'images/ico24_coins.png',
                                            tooltip: tr('main.accountbalance'),
                                            tooltipType: 'title',
                                            scale: 'medium',
                                            itemId: 'balance',
                                            html: '<b>' + Ext.util.Format.currency(parseFloat(balance / 100)) + '</b>',
                                            menu: {
                                                items: balanceItems
                                            }
                                        });
                                    } else {
                                        cmp = Ext.create('Ext.form.Label', {
                                            itemId: 'balance',
                                            html: 'Баланс: <b>' + Ext.util.Format.currency(parseFloat(balance / 100)) + '</b>',
                                            padding: '0 12 0 0'
                                        });
                                    }
                                    var tbar = self.down('toolbar[region="north"]');
                                    var position = tbar.items.findIndex('itemId', 'userSettings');
                                    tbar.insert(position, cmp);
                                })
                            }
                        });
                    }
                }
            });

            userInfo.getWelcomeMessages(function (result) {
                if (result && result.length > 0) {
                    Ext.MessageBox.show({
                        title: tr('main.warning'),
                        msg: result.join("<br/>"),
                        icon: Ext.MessageBox.WARNING,
                        buttons: Ext.MessageBox.OK,
                        animateTarget: 'mb3'
                    });
                }
            });


        }
    }
);

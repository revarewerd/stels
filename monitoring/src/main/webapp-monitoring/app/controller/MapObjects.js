Ext.define('Seniel.controller.MapObjects', {
    extend: 'Ext.app.Controller',

    views: [
        'mapobject.List',
        'mapobject.GroupList',
        'mapobject.MainMap',
        'LeftPanel'
    ],

    refs: [
        {
            ref: 'openLayerMaps',
            selector: 'mainmap'
        },
        {
            ref: 'mapobjectlist',
            selector: 'mapobjectlist'
        },
        {
            ref: 'groupedobjectslist',
            selector: 'groupedobjectslist'
        }
    ],

    showWays: false,

    init: function () {
        this.control({
            'mapobjectlist': {
                check: function (grid, record) {
                    this.onCheckMapObject(grid, record, true);
                },
                uncheck: function (grid, record) {
                    this.onCheckMapObject(grid, record, false);
                },
                mapobjlock: function (record) {
                    this.getOpenLayerMaps().addLockMarker(record, this.getOpenLayerMaps().mapObjects[record.get('uid')]);
                },
                mapobjunlock: function (record) {
                    this.getOpenLayerMaps().removeLockMarker(this.getOpenLayerMaps().mapObjects[record.get('uid')]);
                },
                mapobjevent: function (record) {
                    this.getOpenLayerMaps().addEventMarker(record, this.getOpenLayerMaps().mapObjects[record.get('uid')]);
                },
                mapobjremoveevent: function (record) {
                    this.getOpenLayerMaps().removeEventMarker(this.getOpenLayerMaps().mapObjects[record.get('uid')]);
                },
                itemclick: function (view, node, item, index) {
                    var marker = this.getOpenLayerMaps().mapObjects[node.get('uid')];
                    if (marker) {
                        this.getOpenLayerMaps().setMapCenter(node.get('lon'), node.get('lat'));
                    }
                },
                resize: function (grid, width, height) {
                    var viewport = grid.up('viewport');
                    if (viewport.usettings && viewport.usettings.molcmpwdth && viewport.usettings.molcmpwdth !== width) {
                        viewport.usettings.molcmpwdth = width;
                        console.log('User settings = ', viewport.usettings);
                        mapObjects.setUserSettings(viewport.usettings);
                    }
                },
                traceobject: function (record, view, targeted) {
                    this.traceObject(record, view, targeted)
                },
                showhiddenobjects: function (pressed) {
                    this.showHiddenObjects(pressed)
                }

            },
            'groupedobjectslist': {
                check: function (grid, record) {
                    this.onCheckMapObject(grid, record, true);
                },
                uncheck: function (grid, record) {
                    this.onCheckMapObject(grid, record, false);
                },
                itemclick: function (view, node, item, index) {
                    var marker = this.getOpenLayerMaps().mapObjects[node.get('uid')];
                    if (marker) {
                        this.getOpenLayerMaps().setMapCenter(node.get('lon'), node.get('lat'));
                    }
                },
                traceobject: function (record, view, targeted) {
                    this.traceObject(record, view, targeted)
                },
                showhiddenobjects: function (pressed) {
                    this.showHiddenObjects(pressed)
                }
            }
        });

    },
    updateCounter: function (counterItemId, counterDivId, count) {
        var supportCtrl = this.getMapobjectlist().up('viewport').down("#" + counterItemId);

        var unreadUserTicketsDiv = Ext.fly(counterDivId);
        if (count > 0) {
            unreadUserTicketsDiv.setHTML(count);
            unreadUserTicketsDiv.setDisplayed("inline");
        }
        else {
            unreadUserTicketsDiv.setHTML(count);
            unreadUserTicketsDiv.setDisplayed("none");
        }
        supportCtrl.doComponentLayout();
    },
    serverRequest: function () {
        var self = this;
        self.requestUpdateFromServer.call(self);
        eventsMessages.getUpdatedAfter(self.getMapobjectlist().lastTimeUpdated, function (resp) {
            if (resp) {
                var mol = self.getMapobjectlist();
                var mainMap = self.getOpenLayerMaps();
                userSupportRequestEDS.getUnreadTicketsCount(function (unreadCount) {
                    self.updateCounter('supportCtrl', 'unreadUserTickets', unreadCount);
                })
                if (mainMap.showUnreadNotificationsCount)
                    eventsMessages.getUnreadUserMessagesCount(function (unreadCount) {
                        self.updateCounter('notificationsCtrl', 'unreadUserMessages', unreadCount);
                    })
                mol.lastTimeUpdated = resp.newTime;
                var store = mol.getStore();
                var golStore = self.getGroupedobjectslist().getStore();
                for (var i = 0; i < resp.data.length; i++) {

                    var rec = store.getById(resp.data[i].uid);
                    var golRec = golStore.getById(resp.data[i].uid);
                    console.log('target record = ', rec);
                    console.log('Response data = ', resp.data[i]);
                    if (rec) {
                        if (resp.data[i].blocked != undefined) {
                            rec.set('blocked', resp.data[i].blocked);
                        }
                        if (resp.data[i].requireMaintenance != undefined) {
                            rec.set('requireMaintenance', resp.data[i].requireMaintenance);
                        }
                    }
                    if (golRec) {
                        if (resp.data[i].blocked != undefined) {
                            golRec.set('blocked', resp.data[i].blocked);
                        }
                    }
                    if (resp.data[i].text && rec != null) {
                        resp.data[i].text = tr(resp.data[i].text);
                        mol.fireEvent('mapobjevent', rec);
                    }
                    if (resp.data[i].type == "Пополнение счета") {
                        var balanceField = mol.up('viewport').down("#balance");
                        userInfo.getDetailedBalanceRules(function (resp) {
                            balanceField.setText('<b>' + Ext.util.Format.currency(parseFloat(resp.balance / 100)) + '</b>')
                        })
                    }
                }
                if (resp.data.length > 0) {
                    var popupWnd = Ext.getCmp('notificationsPopupWindow');
                    var map = self.getOpenLayerMaps();
                    var showPopupWnd = map.showPopupNotificationsWindow;
                    if (!!showPopupWnd) {
                        if (!popupWnd) {
                            popupWnd = Ext.create('Seniel.view.notifications.PopupWindow', {});
                            var w = mol.up('viewport').getWidth();
                            popupWnd.showAt([w - 400, 44]);
                            popupWnd.map = map;
                        }
                        popupWnd.down('grid').getStore().add(resp.data);
                        popupWnd.down('grid').getStore().sort('time', 'DESC');
                    }
                }

                if (resp.reload) {
                    //IT is fucken worharound for preventing sending errorneus filters to server
                    var filters = store.lastOptions.filters.slice();
                    store.lastOptions.filters.length = 0;
                    store.reload({
                        callback: function (records, operation, success) {
                            store.filter(filters);
                        }
                    });
                }
            }

            setTimeout(function () {
                self.serverRequest();
            }, 2000);
        });
    },
    onLaunch: function () {
        this.serverRequest();
        var mol = this.getMapobjectlist();
        mol.getStore().on('load', function (store, records) {
            var rodata = new Array(),
                n = records.length,
                isSleepersData = false,
                isRadioUnits = false;
            for (var i = 0; i < n; i++) {
                rodata.push({'uid': records[i].get('uid'), 'name': records[i].get('name')});
                if (records[i].get('sleeper') !== null) {
                    isSleepersData = true;
                }
                if (records[i].get('radioUnit') && records[i].get('radioUnit').installed) {
                    isRadioUnits = true;
                }
            }

            if (!isSleepersData || !isRadioUnits) {
                for (var i = 0; i < mol.columns.length; i++) {
                    if (!isSleepersData && mol.columns[i].menuText === tr('mapobject.sleepermessages') && mol.columns[i].isVisible()) {
                        mol.columns[i].setVisible(false);
                    }
                    if (!isRadioUnits && mol.columns[i].menuText === tr('mapobject.radioblocks') && mol.columns[i].isVisible()) {
                        mol.columns[i].setVisible(false);
                    }
                }
                var us = mol.up('viewport').usettings;
                if (us && us.molcolumns) {
                    for (var i = 0; i < us.molcolumns.length;) {
                        if (!isSleepersData && us.molcolumns[i] === tr('mapobject.sleepermessages')) {
                            us.molcolumns.splice(i, 1);
                        }
                        if (!isRadioUnits && us.molcolumns[i] === tr('mapobject.radioblocks')) {
                            us.molcolumns.splice(i, 1);
                        }
                        i++;
                    }
                }
            }
            mol.roData = rodata;
            var mainToolbar = mol.up('viewport').down('toolbar[region="north"]');
            mainToolbar.down('#reportsCtrl').enable();
            var notificationsCtrl = mainToolbar.down('#notificationsCtrl');
            notificationsCtrl.enable();
            notificationsCtrl.doComponentLayout();
            var supportCtrl = mainToolbar.down('#supportCtrl');
            supportCtrl.enable();
            supportCtrl.doComponentLayout();
        });
    },

    getSelectedUids: function () {

        var selected = new Array(),
            i = this.getMapobjectlist().getStore().findExact('checked', true);
        for (; i !== -1;) {
            selected.push(this.getMapobjectlist().getStore().getAt(i).get('uid'));
            i = this.getMapobjectlist().getStore().findExact('checked', true, i + 1);
        }
        return selected;
    },

    requestUpdateFromServer: function (callback) {
        var self = this;

        mapObjects.getUpdatedAfter(this.lastupdate, function (result, request) {
            self.updateFromServer(result, request);
            if (callback) callback();
        });
    },

    updateFromServer: function (result, request) {
        if (request.xhr && request.xhr.status === 403) {
            Ext.MessageBox.show({
                title: tr('mapobjects.authtorizationerror'),
                msg: tr('mapobjects.authtorizationerror.msg'),
                buttons: Ext.Msg.OK,
                icon: Ext.Msg.ERROR,
                fn: function () {
                    window.location = '/login.html';
                }
            });
            return;
        }

        var self = this;

        var data = result;

        Ext.each(data, function (servUpdate, index) {
            var newTime = servUpdate.newTime;
            if (!self.lastupdate || self.lastupdate < newTime)
                self.lastupdate = newTime;
            var mapObjectsItem = self.getMapobjectlist().store.getById(servUpdate.uid);
            var groupObjectsQuery = self.getGroupedobjectslist().getStore().query("uid", servUpdate.uid).getRange();
            var node = [mapObjectsItem].concat(groupObjectsQuery);
            for (var i in node) {
                if (node[i]) {
                    node[i].beginEdit();
                    node[i].set('lon', servUpdate.lon);
                    node[i].set('lat', servUpdate.lat);
                    node[i].set('time', servUpdate.time);
                    node[i].set('latestmsg', servUpdate.time);
                    node[i].set('satelliteNum', servUpdate.satelliteNum);
                    node[i].set('speed', servUpdate.speed);
                    if (servUpdate.speed > 1) {
                        node[i].set('course', servUpdate.course);
                    }

                    var params = ['canFuelLevel', 'flsFuelLevel', 'extPower', 'canCoolantTemp', 'canRPM', 'canAccPedal', 'ignition'];
                    for (var j = 0; j < params.length; j++) {
                        if (servUpdate[params[j]]) {
                            node[i].set(params[j], servUpdate[params[j]]);
                        }
                    }

                    if (servUpdate.sleeper) {
                        node[i].set('sleeper', servUpdate.sleeper);
                    }

                    if (servUpdate.sensorsData && servUpdate.sensorsData.length > 0) {
                        node[i].set('sensorsData', servUpdate.sensorsData);
                    }

                    if (servUpdate.stateLatency) {
                        node[i].set('stateLatency', servUpdate.stateLatency);
                    }

                    node[i].endEdit(false);
                }
            }
            if (node[0]) {
                var marker = self.getOpenLayerMaps().mapObjects[node[0].get('uid')];
                if (marker) {
                    self.getOpenLayerMaps().moveMarker(node[0], servUpdate.lon, servUpdate.lat);
                    if (node[0].get('targeted') && node[0].get('speed') > 0) {
                        self.getOpenLayerMaps().setMapCenter(servUpdate.lon, servUpdate.lat);
                    }
                }
            }
        });
    },
    mapBounds: {
        lonmin: 1000,
        lonmax: 0,
        latmin: 1000,
        latmax: 0,
        set: function (lon, lat, self) {
            if (this.lonmin > lon) this.lonmin = lon;
            if (this.lonmax < lon) this.lonmax = lon;
            if (this.latmin > lat) this.latmin = lat;
            if (this.latmax < lat) this.latmax = lat;
            if (this.loadSelected < self.getMapobjectlist().loadSelected) {
                this.loadSelected++;
            }
            if (this.loadSelected === self.getMapobjectlist().loadSelected) {
                self.mapZoomExtent();
                this.loadSelected++;
            }
        },
        loadSelected: null
    },
    mapZoomExtent: function () {
        var map = this.getOpenLayerMaps(),
            bounds = new OpenLayers.Bounds(),
            min = map.transLonLat(this.mapBounds.lonmin, this.mapBounds.latmin),
            max = map.transLonLat(this.mapBounds.lonmax, this.mapBounds.latmax)
        bounds.extend(new OpenLayers.LonLat(min.lon, min.lat));
        bounds.extend(new OpenLayers.LonLat(max.lon, max.lat));
        map.map.zoomToExtent(bounds);
    },
    traceObject: function (record, view, targeted) {
        var lists = [
            this.getMapobjectlist(),
            this.getGroupedobjectslist()
        ];
        var checked = record.get('checked');
        if (targeted && !checked) {
            Ext.MessageBox.show({
                title: tr('mapobject.objecttracing.traceobject'),
                msg: tr('mapobject.objecttracing.traceobject.msg'),
                icon: Ext.MessageBox.QUESTION,
                buttons: Ext.Msg.YESNOCANCEL,
                fn: function (a) {
                    if (a === 'yes') {
                        record.set('checked', true);
                        lists[0].fireEvent('check', view, record);
                    }
                }
            });
        }
        for (var i in lists) {
            var list = lists[i];
            var recArray = list.getStore().query("uid", record.get("uid")).getRange();
            if (recArray.length > 0) {
                if (targeted) {
                    Ext.Array.each(recArray, function (rec) {
                        rec.set('targeted', true)
                    })
                }
                else {
                    Ext.Array.each(recArray, function (rec) {
                        rec.set('targeted', false)
                    })
                }
            }
        }
        var targetedArr = lists[0].targetedObjects
        if (targeted) {
            targetedArr.push(record.get("uid"))
        }
        else {
            for (var i = 0; i < targetedArr.length; i++) {
                if (record.get('uid') === targetedArr[i]) {
                    targetedArr.splice(i, 1);
                }
            }
        }
        mapObjects.updateTargetedUids(targetedArr);
    },
    showHiddenObjects: function (pressed) {
        var list = [
            this.getMapobjectlist(),
            this.getGroupedobjectslist()
        ];
        for (var i in list) {
            var liststore = list[i].getStore()
            var btn = list[i].down("#showHiddenObjects")
            var fname = liststore.filters.findBy(function (item) {
                return item.id === 'name';
            });

            if (pressed) {
                Ext.util.Cookies.set('showHiddenObjects', true, new Date(new Date().getTime() + (1000 * 60 * 60 * 24 * 365)));

                if (!fname) {
                    liststore.clearFilter();
                } else {
                    liststore.clearFilter(true);
                    liststore.filter(fname);
                }
                btn.setTooltip(tr('mapobject.hideinvisibleobjects'));
                btn.toggle(true, true);
            } else {
                Ext.util.Cookies.set('showHiddenObjects', false, new Date(new Date().getTime() + (1000 * 60 * 60 * 24 * 365)));

                var fhidn = {
                    id: 'hidn', filterFn: function (item) {
                        return item.get('hidden') === false;
                    }
                };
                if (!fname) {
                    liststore.filter(fhidn);
                } else {
                    liststore.filter([fhidn, fname]);
                }
                btn.setTooltip(tr('mapobject.showallobjects'));
                btn.toggle(false, true);
            }
        }
    },
    onCheckMapObject: function (grid, node, checked, eOpts) {
        var checkedRecords = [],
            fa = this.getOpenLayerMaps().featuresArray;
        if (node != undefined) {
            var mapObjectsItem = this.getMapobjectlist().getStore().getById(node.get("uid"))
            var groupObjectsQuery = this.getGroupedobjectslist().getStore().query("uid", node.get("uid"))
            var items = [mapObjectsItem].concat(groupObjectsQuery.getRange())
            //if(mapObjectsItem!= undefined)  mapObjectsItem.set("checked",checked)
            //if(groupObjectsQuery!= undefined) {//}
            Ext.Array.each(items, function (item) {
                item.set("checked", checked)
            })
            if (!checked) {
                this.traceObject(node, grid, false)
            }

        }
        if (fa === undefined) {
            grid.getStore().each(function (e) {
                if (e.get('checked'))
                    checkedRecords.push(e.get('uid'));
            });
        } else {
            var uid = node.get('uid');
            for (var f in fa) {
                if (uid !== fa[f].node.get('uid')) {
                    checkedRecords.push(fa[f].node.get('uid'));
                }
            }
            if (checked) {
                checkedRecords.push(uid);
            }
        }

        mapObjects.updateCheckedUids(checkedRecords);

        if (checked) {
            console.log("adding marker " + node.get('name'));
            var self = this;

            function addmarker() {
                self.getOpenLayerMaps().addMarker(node);
                self.getOpenLayerMaps().setMapCenter(node.get('lon'), node.get('lat'));
            }

            if (node.get('lon')) {
                addmarker();
                self.mapBounds.set(node.get('lon'), node.get('lat'), self);
            } else {
                mapObjects.getLonLat([node.get('uid')], function (result, request) {
                    self.updateFromServer(result, request);
                    addmarker();
                    self.mapBounds.set(node.get('lon'), node.get('lat'), self);
                });
            }
        } else {
            node.set('lon', null);
            node.set('lat', null);
            node.set('time', null);
            this.getOpenLayerMaps().removeMarker(node);
        }
    }

});
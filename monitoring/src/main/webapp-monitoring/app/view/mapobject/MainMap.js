Ext.define('Seniel.view.mapobject.MainMap', {
    extend: 'Seniel.view.OpenLayerMaps',
    alias: 'widget.mainmap',
    requires: [
        'Ext.window.MessageBox',
        'Seniel.view.mapobject.ToolTip'
    ],
    afterRender: function () {
        Seniel.view.mapobject.MainMap.superclass.afterRender.call(this);

        var self = this;

        userInfo.isObjectsClustering(function (resp) {
            console.log('Clustering enabled = ', resp);
            if (resp === true) {
                self.enableClustering();
            } else {
                self.disableClustering();
            }
        });
        userInfo.getUserSettings(function (resp) {
            self.showEventMarker = !!resp.showEventMarker;
            self.showPopupNotificationsWindow = !!resp.showPopupNotificationsWindow;
            self.showUnreadNotificationsCount = !!resp.showUnreadNotificationsCount;

        });

        var strategy = new OpenLayers.Strategy.Cluster({autoActivate: false, distance: 25, threshold: 3});

        this.vectorLayer = new OpenLayers.Layer.Vector("Vector Markers", {
            displayInLayerSwitcher: false,
            strategies: [strategy],
            styleMap: new OpenLayers.StyleMap(
                {
                    default: //{
//                            strokeColor: "#00FF00",
//                            strokeOpacity: 1,
//                            strokeWidth: 3,
//                            fillColor: "#FF5500",
//                            pointRadius: 10,
//                            pointerEvents: "visiblePainted"
//                        },
                        new OpenLayers.Style({
                            strokeColor: '#3399FF',
                            strokeOpacity: 1,
                            strokeWidth: 3,
                            fillColor: '#FFFFFF',
                            fillOpacity: 0.9,
                            pointRadius: '${radius}',
                            pointerEvents: 'visiblePainted',
                            fontColor: 'black',
                            fontSize: '11px',
                            fontWeight: 'bold',
                            label: '${label}'
                        }, {
                            context: {
                                radius: function (feature) {
                                    var pix = 2;
                                    if (feature.cluster) {
                                        pix = Math.min(feature.attributes.count, 8) * 2 + 6;
                                    }
                                    return pix;
                                },
                                label: function (feature) {
                                    return (feature.cluster) ? (feature.attributes.count) : (feature.style.label);
                                }
                            }
                        }),
                    select: {
                        strokeColor: "#FF0000"
                    }
                }
            )
        });

        this.vectorLayer.events.on({
                featureselected: function (feature) {
                    var p = self.map.getPixelFromLonLat(feature.feature.geometry.getBounds().getCenterLonLat());
                    var mp = self.getPosition();
                    if (feature.feature.cluster) {
                        var clusters = feature.feature.cluster;
                        var data = [];
                        for (var i in clusters) {
                            data.push(clusters[i].style.label);
                        }
                        data.sort();
                        feature.feature.tip = self.showClusterTip(data.join('<br>'), this, mp[0] + p.x + 15, mp[1] + p.y + 15);
                    } else {
                        feature.feature.tip = self.showTooltip(feature.feature.uid, this, mp[0] + p.x + 15, mp[1] + p.y + 15);
                    }
                },
                featureunselected: function (feature) {
                    if (feature.feature.tip) {
                        feature.feature.tip.hide();
                    }
                },
                click: function (e) {
                    if (e.ctrlKey && e.shiftKey) {
                        console.log("clicc", e, self);
                        var lonlat = self.map.getLonLatFromPixel(new OpenLayers.Pixel(e.x - self.x, e.y - self.y));
                        var lon = lonlat.lon;
                        var lat = lonlat.lat;
                        var speed = Math.random() * 40 + 40;
                        var pwr_ext = Math.random() * 30;
                        lonlat.transform(self.map.getProjectionObject(), self.proj);
                        console.log("clicc", lonlat);
                        geozonesData.testPoint(lon, lat, speed, pwr_ext, function (r) {
                            console.log("clicc calback", r);
                        });
                    }
                    var features = self.vectorLayer.features;
                    //var markers = self.markersLayer.markers;
                    //if (markers && markers.length) {
                    //    for (var i = 0; i < markers.length; i++) {
                    //        if(e.object.features[0].eventer==markers[i]){
                    //            eventsMessages.getLastEvent(markers[i].objdata.get('uid'),function(result){
                    //                self.showPopup(result,markers[i].objdata.get("lon"),markers[i].objdata.get("lat"));
                    //            })
                    //        }
                    //    }
                    //}
                    if (features && features.length) {
                        for (var i = 0; i < features.length; i++) {
                            if (e.target._featureId === features[i].id) {
                                var mapObjectsForUid = self.getMapObjects(features[i].uid);
                                var mol = self.up('viewport').down('mapobjectlist'),
                                    n = mol.getStore().findExact('uid', features[i].uid);
                                if (n > -1) {
                                    mol.getView().bufferedRenderer.scrollTo(n, true);
                                    var rec = mol.getStore().getAt(n);
                                    var commandPasswordNeeded = mol.commandPasswordNeeded;
                                    if (rec.get('latestmsg')) {
                                        mol.createCommandWindow(rec, commandPasswordNeeded, e.clientX, e.clientY);
                                        if (mapObjectsForUid && mapObjectsForUid.eventer) {
                                            console.log("mapObjectsForUid.eventer", mapObjectsForUid.eventer)
                                            eventsMessages.getLastEvent(features[i].uid, function (result) {
                                                self.showPopup(result, rec.get("lon"), rec.get("lat"));
                                            })
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                featuresremoved: function (e) {
                    for (var i in e.features) {
                        if (e.features[i].tip) {
                            e.features[i].tip.hide();
                            console.log('Feature with tip removed');
                        }
                    }
                }
            }
        );

        this.selectControl = new OpenLayers.Control.SelectFeature(this.vectorLayer, {
                hover: true,
                clickout: true,
                onSelect: function (o) {
//                console.log("selected=", o);
                }
            }
        );

        this.map.addLayer(this.vectorLayer);
        this.map.addControl(this.selectControl);

        this.selectControl.activate();

        this.map.raiseLayer(this.vectorLayer, -1);

    },
    mapObjects: [],
    getMapObjects: function (uid) {
        if (uid) {
            return this.mapObjects[uid];
        }
        else
            return this.mapObjects
    },
    popupsArray: [],
    getPopups: function (id) {
        if (id) {
            return this.popupsArray[id];
        }
        else
            return this.popupsArray
    },
    isClustering: false,
    showEventMarker: false,
    showPopupNotificationsWindow: false,
    enableClustering: function () {
        var self = this;
        if (self.vectorLayer.strategies && self.vectorLayer.strategies.length) {
            self.isClustering = true;
            self.vectorLayer.strategies[0].activate();
            self.resetClustering();
            self.hideArrowsIfClusters();
        }
        self.map.events.register('zoomend', self.map, function (e) {
            self.hideArrowsIfClusters();
        });
    },
    disableClustering: function () {
        var self = this;
        if (self.vectorLayer.strategies && self.vectorLayer.strategies.length) {
            self.isClustering = false;
            var features = self.vectorLayer.strategies[0].features;
            for (var i in features) {
                if (features[i].hiddenArrow && !features[i].arrow) {
                    features[i].arrow = features[i].hiddenArrow;
                    delete features[i].hiddenArrow;
                    self.markersLayer.addMarker(features[i].arrow);
                }
            }
            self.vectorLayer.strategies[0].deactivate();
            self.resetClustering();
        }
    },
    resetClustering: function () {
        var realFeatures = [];
        for (var i in this.mapObjects) {
            realFeatures.push(this.mapObjects[i]);
        }
        this.vectorLayer.removeAllFeatures();
        this.vectorLayer.addFeatures(realFeatures);
    },
    hideArrowsIfClusters: function () {
        if (this.isClustering === true) {
            var features = this.vectorLayer.features;
            for (var i in features) {
                if (features[i].cluster && features[i].cluster.length > 1) {
                    var cluster = features[i].cluster;
                    for (var j in cluster) {
                        if (cluster[j].arrow) {
                            cluster[j].hiddenArrow = cluster[j].arrow;
                            this.removeArrow(cluster[j]);
                        }
                    }
                } else if (features[i].hiddenArrow && !features[i].arrow) {
                    features[i].arrow = features[i].hiddenArrow;
                    delete features[i].hiddenArrow;
                    this.markersLayer.addMarker(features[i].arrow);
                }
            }
        }
    },
    showPopup: function (mes, lon, lat) {
        var time = Ext.Date.format(new Date(mes.time), "d.m.Y H:i:s withTZ");
        var text = '<div class="ol-popup-content"><b>' + tr('notifications.history.grid.object') + ': </b>' + mes.name
            + '<br/><b>' + tr('notifications.history.grid.time') + ': </b>' + time + '<br/><b>'
            + tr('notifications.history.grid.message') + ': </b>' + tr(mes.text) + '</div>';

        if (!mes.lon && !mes.lat) {
            mes.lon = lon;
            mes.lat = lat;
        }
        this.addPopup(text, mes);
    },
    addPopup: function (text, mes) {
        var lonlat = this.transLonLat(mes.lon, mes.lat);
        var id = mes.eid;
        var uid = mes.uid;
        var self = this;

        if (!this.popupsArray[id]) {
            var btn = Ext.create('Ext.panel.Tool', {
                iconCls: 'x-tool-close',
                handler: function () {
                    self.removePopup(id);
                    var eventsWnd = [self.up('viewport').down('eventswnd'), Ext.getCmp('notificationsPopupWindow')];
                    for (var i in eventsWnd) {
                        if (eventsWnd[i]) {
                            var rec = eventsWnd[i].down('grid').getStore().findRecord('eid', id);
                            if (rec) rec.set('onMap', false);
                        }
                    }
                    var mapobjectlist = Ext.ComponentQuery.query('mapobjectlist');
                    var coll = new Ext.util.MixedCollection();
                    coll.add('uid', uid);
                    mapobjectlist[0].fireEvent('mapobjremoveevent', coll);
                }
            });
            var popup = new OpenLayers.Popup.FramedCloud('olpopup-' + id, lonlat, null, '<span id="olpopup-close-' + id + '" style="float: right;"></span>' + text, null);
            popup.uid = uid;
            popup.autoSize = true;
            popup.closeBtn = btn;
            this.popupsArray[id] = popup;

            this.map.addPopup(popup);
            btn.render('olpopup-close-' + id);
            popup.updateSize();
            self.showPopupMarker(lonlat, self.getPopups(id))
        }
        this.map.setCenter(lonlat);
    },
    removePopup: function (id) {
        var self = this;
        if (this.popupsArray[id]) {
            var popup = this.popupsArray[id];
            delete popup.closeBtn;
            this.map.removePopup(popup);
            self.removePopupMarker(self.getPopups(id))
            delete this.popupsArray[id];
        }
    },
    showPopupMarker: function (lonlat, popup) {
        console.log("showPopupMarker");
        if (popup && !popup.popupMarker) {
            var size = new OpenLayers.Size(19, 24),
                offset = new OpenLayers.Pixel(-12, -24),
                icon = new OpenLayers.Icon('images/ico_marker.png', size, offset);
            popup.popupMarker = new OpenLayers.Marker(lonlat, icon);
            this.markersLayer.addMarker(popup.popupMarker);
        } else {
            if (!popup || (popup && popup.popupMarker.lonlat.equals(lonlat))) {
                return;
            } else {
                this.removePopupMarker(popup);
                this.showPopupMarker(lonlat, popup);
            }
        }
    },
    removePopupMarker: function (popup) {
        if (popup && popup.popupMarker) {
            this.markersLayer.removeMarker(popup.popupMarker);
            delete popup.popupMarker;
        }
    },
    addGeozone: function (node, centered) {
        var featureArray = this.up('viewport').geozOnMap;
        if (!featureArray[node.get('id')]) {
            var points = Ext.JSON.decode(node.get('points')),
                olpoints = new Array();
            for (var i = 0; i < points.length; i++) {
                olpoints.push(new OpenLayers.Geometry.Point(points[i].x, points[i].y));
            }
            var linearRing = new OpenLayers.Geometry.LinearRing(olpoints);
            var feature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Polygon([linearRing]), {'ftcolor': node.get('ftColor')});
            this.showGeozonesLayer.addFeatures([feature]);
            featureArray[node.get('id')] = feature;
            if (centered) {
                var bounds = feature.geometry.bounds;
                this.map.zoomToExtent(bounds);
            }
        }
    },
    removeGeozone: function (node) {
        var featureArray = this.up('viewport').geozOnMap;
        if (featureArray[node.get('id')]) {
            this.showGeozonesLayer.removeFeatures([featureArray[node.get('id')]]);
            delete featureArray[node.get('id')];
        }
    },
    centerOnGeozone: function (node, minlonlat, maxlonlat) {
        var bounds = new OpenLayers.Bounds();
        bounds.extend(minlonlat);
        bounds.extend(maxlonlat);
        this.map.zoomToExtent(bounds);
    },
    loadObjectImage: function (node, angle) {
        var img = new Image();
        objectSettings.loadObjectMapSettings(node.get('uid'), function (resp) {
            if (resp.imgSource) {
                for (var i in resp) {
                    node.set(i, resp[i]);
                }

                if (!resp.imgRotate && resp.imgRotate !== false) {
                    resp.imgRotate = (resp.imgSource.search(/rot/) > -1) ? (true) : (false);
                    node.set('imgRotate', resp.imgRotate);
                }

                if (!resp.imgArrow && resp.imgArrow !== false) {
                    node.set('imgArrow', true);
                    node.set('imgArrowSrc', 'arrow_sor_06.png');
                }
            } else {
                node.set('imgSource', 'images/cars/car_rot_swc_32.png');
                node.set('imgRotate', true);
                node.set('imgArrow', true);
                node.set('imgArrowSrc', 'arrow_sor_06.png');
            }
            img.src = (node.get('imgRotate')) ? ('rotate?img=' + node.get('imgSource').substring(7) + '&rot=' + angle) : (node.get('imgSource'));
        });
        return img;
    },
    addMarker: function (node) {
        var self = this;
        if (!self.mapObjects[node.get('uid')]) {
            var angle = node.get('course'),
                markstyle = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style['default']),
                img = new Image();
            markstyle.fontColor = 'red';
            markstyle.fontSize = '11px';
            markstyle.fontFamily = 'tahoma, arial, verdana, sans-serif';
            markstyle.fontWeight = 'bold';
            markstyle.labelAlign = 'cm';
            markstyle.labelXOffset = 0;
            markstyle.label = Ext.String.htmlDecode(node.get('name'));
            markstyle.labelOutlineWidth = 1;
            if (!node.get('imgSource')) {
                img = self.loadObjectImage(node, angle);
            } else {
                img.src = (node.get('imgRotate')) ? ('rotate?img=' + node.get('imgSource').substring(7) + '&rot=' + angle) : (node.get('imgSource'));
            }
            img.onerror = function () {
                node.set('imgSource', 'images/cars/car_rot_swc_32.png');
                node.set('imgRotate', true);
                node.set('imgArrow', true);
                node.set('imgArrowSrc', 'arrow_sor_06.png');
                img.src = 'images/cars/car_rot_swc_32.png';
            };
            img.onload = function () {
                var w = (img.width > 0) ? (img.width) : (32);
                var h = (img.height > 0) ? (img.height) : (32);
                markstyle.graphicWidth = w;
                markstyle.graphicHeight = h;
                markstyle.labelYOffset = -(h / 2 + 4);
                markstyle.externalGraphic = img.src;
                markstyle.graphicOpacity = 1;
                var lonlat = self.transLonLat(node.get('lon'), node.get('lat')),
                    point = new OpenLayers.Geometry.Point(lonlat.lon, lonlat.lat),
                    marker = new OpenLayers.Feature.Vector(point, null, markstyle);
                marker.uid = node.get('uid');
                self.mapObjects[node.get('uid')] = marker;
//                pointFeature.node = node;
                self.vectorLayer.addFeatures([marker]);

                if (node.get('imgArrow')) {
                    self.addArrow(node, lonlat, marker);
                }
                if (node.get('blocked') === true) {
                    self.addLockMarker(node, marker);
                }
                if (node.get('requireMaintenance') === true) {
                    self.addMaintenanceMarker(node, marker);
                }

                if (self.isClustering === true) {
                    self.resetClustering();
                }
            };
        }
    },
    moveMarker: function (node, lon, lat) {
        var self = this;
        var marker = this.mapObjects[node.get('uid')];
        if (marker) {
            var lonlat = self.transLonLat(node.get('lon'), node.get('lat')),
                angle = node.get('course'),
                img = new Image();
            if (!node.get('imgSource')) {
                img = self.loadObjectImage(node, angle);
            } else {
                img.src = (node.get('imgRotate')) ? ('rotate?img=' + node.get('imgSource').substring(7) + '&rot=' + angle) : (node.get('imgSource'));
            }

            img.onload = function () {
                marker.style.graphicWidth = img.width;
                marker.style.graphicHeight = img.height;
                marker.style.externalGraphic = img.src;
                marker.labelYOffset = -(img.height / 2 + 4);
                self.vectorLayer.drawFeature(marker);
                if (node.get('imgArrow')) {
                    self.addArrow(node, lonlat, marker);
                } else {
                    self.removeArrow(marker);
                }
                if (self.isClustering === true) {
                    self.resetClustering();
                }
            };
            marker.move(lonlat);
            if (node.get('blocked') === true) {
                self.addLockMarker(node, marker);
            }
            if (node.get('requireMaintenance') === true) {
                self.addMaintenanceMarker(node, marker);
            } else {
                self.removeMaintenanceMarker(marker);
            }
            var markers = self.markersLayer.markers;
            if (markers && markers.length) {
                for (var i = 0; i < markers.length; i++) {
                    if (markers[i].objdata) {
                        var uid = markers[i].objdata.get('uid');
                        if (node.get("uid") == uid) {
                            var mapobjectlist = Ext.ComponentQuery.query('mapobjectlist');
                            mapobjectlist[0].fireEvent('mapobjevent', node);
                        }
                    }
                }
            }
        }
    },
    removeMarker: function (node) {
        var marker = this.mapObjects[node.get('uid')];

        if (marker) {
            this.removeArrow(marker);
            this.removeLockMarker(marker);
            this.removeMaintenanceMarker(marker);
            this.vectorLayer.destroyFeatures([marker]);
            delete this.mapObjects[node.get('uid')];
            if (this.isClustering === true) {
                this.resetClustering();
            }
        }
    },
    addArrow: function (node, lonlat, marker) {
        var self = this;

        if (marker.arrow && marker.arrow.lonlat.equals(lonlat) && marker.arrow.icon.url.match(node.get('imgArrowSrc')) !== null) {
            return;
        }

        this.removeArrow(marker);

        if (node.get("speed") < 2) {
            return;
        }
        var angle = node.get("course");
        var img = new Image();
        if (node.get('imgArrowSrc')) {
            img.src = 'rotate?img=' + node.get('imgArrowSrc') + '&rot=' + angle;
        } else {
            img.src = 'rotate?img=arrow_sor_06.png&rot=' + angle;
        }

        img.onload = function () {
            var size = new OpenLayers.Size(img.width, img.height),
                R = 24,
                offset = new OpenLayers.Pixel(-(size.w / 2) + Math.sin(angle * Math.PI / 180) * R, -12 - R * Math.cos(angle * Math.PI / 180));
            var icon = new OpenLayers.Icon(img.src, size, offset);
            marker.arrow = new OpenLayers.Marker(lonlat, icon);
            self.markersLayer.addMarker(marker.arrow);
            self.hideArrowsIfClusters();
        };
    },
    removeArrow: function (marker) {
        if (marker && marker.arrow) {
            this.markersLayer.removeMarker(marker.arrow);
            delete marker.arrow;
        } else if (marker && marker.hiddenArrow) {
            delete marker.hiddenArrow;
        }
    },
    addMaintenanceMarker: function (node, marker) {
        var lonlat = this.transLonLat(node.get('lon'), node.get('lat'));
        if (marker && !marker.maintenance) {
            var size = new OpenLayers.Size(16, 16),
                offset = new OpenLayers.Pixel(10, -2),
                icon = new OpenLayers.Icon('images/Wrench16.png', size, offset);
            marker.maintenance = new OpenLayers.Marker(lonlat, icon);
            this.markersLayer.addMarker(marker.maintenance);
        } else {
            if (!marker || (marker && marker.maintenance.lonlat.equals(lonlat))) {
                return;
            } else {
                this.removeMaintenanceMarker(marker);
                this.addMaintenanceMarker(node, marker);
            }
        }
    },
    removeMaintenanceMarker: function (marker) {
        if (marker && marker.maintenance) {
            this.markersLayer.removeMarker(marker.maintenance);
            delete marker.maintenance;
        }
    },
    addLockMarker: function (node, marker) {
        var lonlat = this.transLonLat(node.get('lon'), node.get('lat'));
        if (marker && !marker.locker) {
            var size = new OpenLayers.Size(16, 16),
                offset = new OpenLayers.Pixel(4, -24),
                icon = new OpenLayers.Icon('images/ico16_lock.png', size, offset);
            marker.locker = new OpenLayers.Marker(lonlat, icon);
            this.markersLayer.addMarker(marker.locker);
        } else {
            if (!marker || (marker && marker.locker.lonlat.equals(lonlat))) {
                return;
            } else {
                this.removeLockMarker(marker);
                this.addLockMarker(node, marker);
            }
        }
    },
    removeLockMarker: function (marker) {
        if (marker && marker.locker) {
            this.markersLayer.removeMarker(marker.locker);
            delete marker.locker;
        }
    },
    addEventMarker: function (node, marker) {
        if (!this.showEventMarker) return;
        var lonlat = this.transLonLat(node.get('lon'), node.get('lat'));
        if (marker && !marker.eventer) {
            var size = new OpenLayers.Size(24, 24),
                offset = new OpenLayers.Pixel(-32, -32),
                icon = new OpenLayers.Icon('images/ico32_information.png', size, offset);
            marker.eventer = new OpenLayers.Marker(lonlat, icon);
            marker.eventer.objdata = node;
            this.markersLayer.addMarker(marker.eventer);
        } else {
            if (!marker || (marker && marker.eventer.lonlat.equals(lonlat))) {
                return;
            } else {
                this.removeEventMarker(marker);
                this.addEventMarker(node, marker);
            }
        }
    },
    removeEventMarker: function (marker) {
        if (marker && marker.eventer) {
            this.markersLayer.removeMarker(marker.eventer);
            delete marker.eventer;
        }
    },
    showTooltip: function (uid, target, posX, posY) {
        var store = this.up('viewport').down('mapobjectlist').getStore();
        var node = store.getById(uid);
        var tip = Ext.getCmp('maptooltip-' + uid);
        if (!tip) {
            tip = Ext.create('Seniel.view.mapobject.ToolTip', {
                id: 'maptooltip-' + uid
            });

            var grid = tip.down('grid');
            var lastMsgText = tr('mainmap.nolastmessage');
            var lastMsgColor = 'red';
            if (node.get('time')) {
                lastMsgText = Seniel.view.WRUtils.getLastMsgText(new Date(node.get('time')));
                lastMsgColor = Seniel.view.WRUtils.getLastMsgColor(new Date(node.get('time'))).name;
            }

            function css(color) {
                if (color === 'yellow') {
                    return "color: rgb(121, 115, 2);";
                }
                else
                    return "color: " + color + ";";
            }

            grid.getStore().add([
                {name: tr('mainmap.name') + ':', value: node.get('name')},
                {
                    name: tr('mainmap.coords') + ':',
                    value: tr('mainmap.latitude.short') + ': ' + node.get('lat') + ', ' + tr('mainmap.longitude.short') + ': ' + node.get('lon')
                },
                {
                    name: tr('mainmap.messatime') + ':',
                    value: ('<span style="' + css(lastMsgColor) + '">' + lastMsgText + '</span>')
                },
                {name: tr('mapobject.lastmessage.satellitecount') + ':', value: node.get('satelliteNum')},
                {name: tr('mainmap.speed') + ':', value: node.get('speed') + ' ' + tr('mapobject.laststate.kmsperhour')}
            ]);
            if (node.get('ignition')) {
                grid.getStore().add([{
                    name: tr('mainmap.ignition') + ':',
                    value: (node.get('ignition') > 0) ? (tr('mainmap.ignition.on')) : (tr('mainmap.ignition.off'))
                }]);
            }
            if (node.get('stateLatency') && typeof node.get('stateLatency') === 'object') {
                var state = node.get('stateLatency');
                var stateTitle = (state.isParking) ? (tr('mainmap.state.parking')) : (tr('mainmap.state.moving'));
                grid.getStore().add([{
                    name: stateTitle + ':',
                    value: Seniel.view.WRUtils.getLastMsgText(new Date(node.get('time') - state.currentInterval))
                }]);
            }

            var params = ['extPower', 'canFuelLevel', 'flsFuelLevel', 'canCoolantTemp', 'canRPM', 'canAccPedal'];
            var translations = ['extpower', 'canfuellevel', 'flsfuellevel', 'cancoolanttemp', 'canrpm', 'canaccpedal'];
            var units = ['voltage', 'fuellevel', 'fuellevel', 'temperature', 'rpm', 'percent'];
            for (var i = 0; i < params.length; i++) {
                if (node.get(params[i])) {
                    grid.getStore().add([{
                        name: tr('mainmap.' + translations[i]) + ':',
                        value: (node.get(params[i]) + ' ' + tr('units.' + units[i]))
                    }]);
                }
            }
            var sensorsData = node.get('sensorsData'),
                addData = [];
            if (sensorsData && sensorsData.length > 0) {
                for (var j = 0; j < sensorsData.length; j++) {
                    addData.push({
                        name: sensorsData[j].name + ':',
                        value: (Math.round(sensorsData[j].value * 100) / 100 + ' ' + sensorsData[j].unit)
                    });
                }
                grid.getStore().add(addData);
            }
            grid.view.trackOver = false;
            tip.setTarget(target);
            tip.showAt([posX, posY]);

            if (node.get('lon') && node.get('lat'))
                mapObjects.regeocode(node.get('lon'), node.get('lat'), function (name) {
                    grid.store.insert(1, {name: tr('mainmap.position') + ':', value: name});
                });
        }

        return tip;
    },
    showClusterTip: function (data, target, posX, posY) {
        var tip = Ext.create('Seniel.view.mapobject.ToolTip', {});
        var grid = tip.down('grid');
        grid.getStore().add([{name: tr('leftpanel.objects') + ':', value: data}]);
        grid.view.trackOver = false;
        tip.setTarget(target);
        tip.showAt([posX, posY]);

        return tip;
    }

});

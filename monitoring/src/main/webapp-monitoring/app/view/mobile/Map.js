Ext.define('Seniel.view.mobile.Map', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.mapcontainer',
    map: null,
    mapHolder: null,
    layout: 'fit',
    proj: null,
    pointLayer: null,
    layers: null,
    curLayer: null,
    trafficLayer: null,
    markersLayer: null,
    parkingLayer: null,
    mapObjects: [],
    tbar: [
        {
            icon: 'images/ico32_zoomin.png',
            itemId: 'btnMapZoomin',
            cls: 'mobile-toolbar-button',
            scale: 'large',
            handler: function(btn) {
                btn.up('mapcontainer').map.zoomIn();
            }
        },
        {
            icon: 'images/ico32_zoomout.png',
            itemId: 'btnMapZoomout',
            cls: 'mobile-toolbar-button',
            scale: 'large',
            handler: function(btn) {
                btn.up('mapcontainer').map.zoomOut();
            }
        },
        {
            xtype: 'tbspacer',
            width: 20
        },
        {
            xtype: 'combo',
            cls: 'mobile-panel-toolbar',
            allowBlank: true,
            fieldLabel: tr('mobile.maptype'),
            flex: 1,
            editable: false,
            hideTrigger: true,
            queryMode: 'local',
            valueField: 'id',
            displayField: 'name',
            listConfig: {
                cls: 'mobile-toolbar-combo-picker'
            },
            store: {
                fields: ['id', 'name'],
                data: {
                    'items': []
                },
                proxy: {
                    type: 'memory',
                    reader: {
                        type: 'json',
                        root: 'items'
                    }
                }
            },
            listeners: {
                boxready: function() {
                    var layers = this.up('mapcontainer').map.layers;
                    var items = new Array();
                    for (var i = 0; i < layers.length; i++) {
                        if (layers[i].isBaseLayer) {
                            items.push({'id': layers[i].codeName, 'name': layers[i].name});
                        }
                    }
                    
                    var curLayer = Ext.util.Cookies.get('mobileMapLayer');
                    this.getStore().add(items);
                    if (curLayer) {
                        this.setValue(curLayer)
                    } else {
                        Ext.util.Cookies.set('mobileMapLayer', items[0].id, new Date(new Date().getTime()+(1000*60*60*24*365)));
                        this.setValue(items[0].id);
                    }
                },
                change: function(field, newVal, oldVal) {
                    var map = field.up('mapcontainer').map;
                    var layer = map.getLayersBy('codeName', newVal)[0];
                    Ext.util.Cookies.set('mobileMapLayer', layer.codeName, new Date(new Date().getTime()+(1000*60*60*24*365)));
                    map.setBaseLayer(layer);
                }
            }
        }
    ],
    listeners: {
        'resize': function () {
            this.map.updateSize();
        }
    },

    transLonLat: function(lon, lat) {
        var lonlat = new OpenLayers.LonLat(lon, lat);
        lonlat.transform(this.proj, this.map.getProjectionObject());
        return lonlat;
    },

    setMapCenter: function(lon, lat) {
        var lonlat = this.transLonLat(lon, lat);
        this.map.setCenter(lonlat);
    },


    initComponent: function() {
        this.items = [
            this.mapHolder = new Ext.panel.Panel({
                flex: 1
            })
        ];
        this.callParent();
    },
    afterRender: function() {
        this.callParent();
        var maxExtent = new OpenLayers.Bounds(-20037508, -20037508, 20037508, 20037508),
            restrictedExtent = maxExtent.clone(),
            maxResolution = 156543.0339,
            options = {
                projection: new OpenLayers.Projection('EPSG:4326'),
                displayProjection: new OpenLayers.Projection('EPSG:4326'),
                units: 'deegrees',
                numZoomLevels: 18,
                maxResolution: maxResolution,
                maxExtent: maxExtent,
                restrictedExtent: restrictedExtent,
                controls: [
                    new OpenLayers.Control.TouchNavigation(), 
                    new OpenLayers.Control.ArgParser(),
                    new OpenLayers.Control.Attribution()
                ]
            };
        var divForMap = this.mapHolder.body.dom;
        while (divForMap.firstChild) {
            divForMap.removeChild(divForMap.firstChild);
        }
        this.map = new OpenLayers.Map(divForMap, options);
        var self = this;

        var OSM = new OpenLayers.Layer.OSM('Open Street Maps');
        OSM.codeName = 'openstreetmaps';

        var GoogleRoad = new OpenLayers.Layer.Google(tr('openlayers.googlemap'), {
            codeName: 'googlemap',
            type: google.maps.MapTypeId.ROADMAP,
            visibility: false
        });
        var GooglePhy = new OpenLayers.Layer.Google(tr('openlayers.googlemapterrain'), {
            codeName: 'googlemapterrain',
            type: google.maps.MapTypeId.TERRAIN,
            visibility: false
        });
        var GoogleHyb = new OpenLayers.Layer.Google(tr('openlayers.googlehybrid'), {
            codeName: 'googlehybrid',
            type: google.maps.MapTypeId.HYBRID,
            visibility: false
        });
        var GoogleSat = new OpenLayers.Layer.Google(tr('openlayers.googlesatellite'), {
            codeName: 'googlesatellite',
            type: google.maps.MapTypeId.SATELLITE,
            visibility: false
        });

        var Mailru = new OpenLayers.Layer.XYZ(tr('openlayers.mailrumap'), 'http://t3maps.mail.ru/tiles/scheme/${z}/${y}/${x}.png', {
            codeName: 'mailrumap',
            visibility: false,
            projection: new OpenLayers.Projection('EPSG:900913'),
            displayProjection: new OpenLayers.Projection('EPSG:4326')
        });

        this.markersLayer = new OpenLayers.Layer.Markers(tr('openlayers.markers'));
        this.markersLayer.codeName = 'markers';
        this.vectorLayer = new OpenLayers.Layer.Vector('Vector Markers', {
            displayInLayerSwitcher: false,
            styleMap: new OpenLayers.StyleMap(
                {
                    'default': {
                        strokeColor: '#00FF00',
                        strokeOpacity: 1,
                        strokeWidth: 3,
                        fillColor: '#FF5500',
                        fontSize: '1.4em',
                        pointRadius: 24,
                        pointerEvents: 'visiblePainted'
                    },
                    'select': {
                        strokeColor: '#FFFF00',
                        pointRadius: 10,
                        fontColor: 'red'
                    }

                }
            )
        });
        
//        this.vectorLayer.events.on({
//            click: function(e) {
//                var features = self.vectorLayer.features;
//                if (features && features.length) {
//                    for (var i = 0; i < features.length; i++) {
//                        if (e.target._featureId === features[i].id) {
//                            var ol = self.up('viewport').down('gridobjectslist'),
//                                n = ol.getStore().findExact('uid', features[i].uid);
//                            if (n > -1) {
//                                self.up('viewport').down('#btnObjectsPanel').toggle(true);
//                                ol.getView().bufferedRenderer.scrollTo(n, true);
//                            }
//                        }
//                    }
//                }
//            }
//        });

        this.map.addLayers([OSM, GoogleRoad, GooglePhy, GoogleHyb, GoogleSat, Mailru, this.vectorLayer, this.markersLayer]);
        this.map.raiseLayer(this.vectorLayer, -1);
        
        this.proj = new OpenLayers.Projection('EPSG:4326');
        var lonlat = new OpenLayers.LonLat(55, 37);
        lonlat.transform(this.proj, this.map.getProjectionObject());
        this.map.setCenter(lonlat, 3);
    },

    // Функции //

    loadObjectImage: function(node, angle) {
        var img = new Image();
        objectSettings.loadObjectMapSettings(node.get('uid'), function(resp) {
            if (resp.imgSource) {
                for (var i in resp) {
                    node.set(i, resp[i]);
                }

                if (!resp.imgRotate && resp.imgRotate !== false) {
                    resp.imgRotate = (resp.imgSource.search(/rot/) > -1)?(true):(false);
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
            img.src = (node.get('imgRotate'))?('rotate?img=' + node.get('imgSource').substring(7) + '&rot=' + angle):(node.get('imgSource'));
        });
        return img;
    },
    addMarker: function(node) {
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
            markstyle.label = node.get('name');
            markstyle.labelOutlineWidth = 1;
            if (!node.get('imgSource')) {
                img = self.loadObjectImage(node, angle);
            } else {
                img.src = (node.get('imgRotate'))?('rotate?img=' + node.get('imgSource').substring(7) + '&rot=' + angle):(node.get('imgSource'));
            }
            img.onerror = function() {
                node.set('imgSource', 'images/cars/car_rot_swc_32.png');
                node.set('imgRotate', true);
                node.set('imgArrow', true);
                node.set('imgArrowSrc', 'arrow_sor_06.png');
                img.src = 'images/cars/car_rot_swc_32.png';
            };
            img.onload = function() {
                var w = (img.width > 0)?(img.width):(32);
                var h = (img.height > 0)?(img.height):(32);
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
            };
        }
    },
    moveMarker: function(node, lon, lat) {
        var self = this;
        var marker = this.mapObjects[node.get('uid')];
        if (marker) {
            var lonlat = self.transLonLat(node.get('lon'), node.get('lat')),
                angle = node.get('course'),
                img = new Image();
            if (!node.get('imgSource')) {
                img = self.loadObjectImage(node, angle);
            } else {
                img.src = (node.get('imgRotate'))?('rotate?img=' + node.get('imgSource').substring(7) + '&rot=' + angle):(node.get('imgSource'));
            }
            
            img.onload = function() {
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
            };
            marker.move(lonlat);
            if (node.get('blocked') === true) {
                self.addLockMarker(node, marker);
            }
        }
    },
    removeMarker: function(node) {
        var marker = this.mapObjects[node.get('uid')];
        
        if (marker) {
            this.removeArrow(marker);
            this.removeLockMarker(marker);
            this.vectorLayer.destroyFeatures([marker]);
            delete this.mapObjects[node.get('uid')];
        }
    },
    addArrow: function(node, lonlat, marker) {
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
        
        img.onload = function() {
            var size = new OpenLayers.Size(img.width, img.height),
                R = 24,
                offset = new OpenLayers.Pixel(-(size.w / 2) + Math.sin(angle * Math.PI / 180) * R, -12 - R * Math.cos(angle * Math.PI / 180));
            var icon = new OpenLayers.Icon(img.src, size, offset);
            marker.arrow = new OpenLayers.Marker(lonlat, icon);
            self.markersLayer.addMarker(marker.arrow);
        };
    },
    removeArrow: function(marker) {
        if (marker && marker.arrow) {
            this.markersLayer.removeMarker(marker.arrow);
            delete marker.arrow;
        }
    },
    addLockMarker: function(node, marker) {
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
    removeLockMarker: function(marker) {
        if (marker && marker.locker) {
            this.markersLayer.removeMarker(marker.locker);
            delete marker.locker;
        }
    },
    addEventMarker: function(node, marker) {
        var lonlat = this.transLonLat(node.get('lon'), node.get('lat'));
        if (marker && !marker.eventer) {
            var size = new OpenLayers.Size(24, 24),
                offset = new OpenLayers.Pixel(-32, -32),
                icon = new OpenLayers.Icon('images/ico32_information.png', size, offset);
            marker.eventer = new OpenLayers.Marker(lonlat, icon);
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
    removeEventMarker: function(marker) {
        if (marker && marker.eventer) {
            this.markersLayer.removeMarker(marker.eventer);
            delete marker.eventer;
        }
    }
});
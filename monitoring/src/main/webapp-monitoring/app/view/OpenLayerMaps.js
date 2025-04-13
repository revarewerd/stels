OpenLayers.Control.Hover = OpenLayers.Class(OpenLayers.Control, {
    defaultHandlerOptions: {
        'delay': 500,
        'pixelTolerance': null,
        'stopMove': false
    },

    initialize: function (options) {
        this.handlerOptions = OpenLayers.Util.extend(
            {}, this.defaultHandlerOptions
        );
        OpenLayers.Control.prototype.initialize.apply(
            this, arguments
        );
        this.handler = new OpenLayers.Handler.Hover(
            this,
            {'pause': this.onPause, 'move': this.onMove},
            this.handlerOptions
        );
        OpenLayers.Lang.setCode('ru');
    }

});
OpenLayers.Control.Click = OpenLayers.Class(OpenLayers.Control, {
    defaultHandlerOptions: {
        'delay': 0,
        'pixelTolerance': null        
    },

    initialize: function (options) {
        this.handlerOptions = OpenLayers.Util.extend(
            {}, this.defaultHandlerOptions
        );
        OpenLayers.Control.prototype.initialize.apply(
            this, arguments
        );
        this.handler = new OpenLayers.Handler.Click(
            this,
            {'click': this.onClick},
            this.handlerOptions
        );
        OpenLayers.Lang.setCode('ru');
    }

});
Ext.define('Seniel.view.OpenLayerMaps', {
        extend: 'Ext.container.Container',
        title: 'Seniel maps widget',
        alias: 'widget.seniel-mapwidget',
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
        listeners: {
            resize: function() {
                this.map.updateSize();
            },
            boxready: function() {
                var curMap = Ext.util.Cookies.get('currentMapLayer');
                if (!curMap) {
                    curMap = 'openstreetmaps';
                    Ext.util.Cookies.set('currentMapLayer', curMap, new Date(new Date().getTime()+(1000*60*60*24*365)));
                }
                this.map.setBaseLayer(this.map.getLayersBy('codeName', curMap)[0]);
                var wnd = this.up('editgeozwnd');
                if (wnd) {
                    this.map.events.un({'changelayer': this.onChangeLayer});
                    this.drawGeozonesToolbar = wnd.down('#geozonesToolbar');
                }
            }
        },

        transLonLat: function (lon, lat) {
            var lonlat = new OpenLayers.LonLat(lon, lat);
            lonlat.transform(this.proj, this.map.getProjectionObject());
            return lonlat;
        },

        setMapCenter: function (lon, lat) {
            var lonlat = this.transLonLat(lon, lat);
            this.map.setCenter(lonlat);
        },


        initComponent: function () {
            this.items = [
                this.mapHolder = new Ext.panel.Panel({
                    flex: 1
                })
            ];
            this.callParent();
        },
        afterRender: function () {
            this.callParent();
            var maxExtent = new OpenLayers.Bounds(-20037508, -20037508, 20037508, 20037508),
                restrictedExtent = maxExtent.clone(),
                maxResolution = 156543.0339,
                options = {
                    projection: new OpenLayers.Projection("EPSG:4326"),
                    displayProjection: new OpenLayers.Projection("EPSG:4326"),
                    units: "deegrees",
                    numZoomLevels: 18,
                    maxResolution: maxResolution,
                    maxExtent: maxExtent,
                    restrictedExtent: restrictedExtent
                };
            var divForMap = this.mapHolder.body.dom;
            while (divForMap.firstChild) {
                divForMap.removeChild(divForMap.firstChild);
            }
            this.map = new OpenLayers.Map(divForMap, options);
            var self = this;
            var styleMap = new OpenLayers.StyleMap({
                'default': {
                    'strokeColor': '${ftcolor}',
                    'fillColor': '${ftcolor}',
                    'fillOpacity': 0.4,
                    'pointRadius': 8,
                    'strokeWidth': 2
                }
            });
            
            this.showGeozonesLayer = new OpenLayers.Layer.Vector(tr('openlayers.geozones'), {styleMap: styleMap});
            this.showGeozonesLayer.codeName = 'showgeozones';
            this.drawGeozonesLayer = new OpenLayers.Layer.Vector(tr('openlayers.geozones'), {styleMap: styleMap, displayInLayerSwitcher: false, renderers: OpenLayers.Layer.Vector.prototype.renderers});
            this.drawGeozonesLayer.codeName = 'drawgeozones';
            this.drawGeozonesLayer.events.on({
                sketchcomplete: function(e) {
                    if (self.drawGeozonesToolbar) {
                        var btnclr = self.drawGeozonesToolbar.down('#geozColor');
                        e.feature.attributes = {
                            ftcolor: btnclr.curColor
                        };
                        self.currentDrawing = e.feature;
                        self.drawGeozonesToolbar.down('#geozPoly').toggle(false);
                        self.drawGeozonesToolbar.down('#geozRect').toggle(false);
                        self.drawGeozonesToolbar.down('#geozCrcl').toggle(false);
                    }
                    console.log('Sketch drawing completed. ', e);
                }
            });
            
            this.drawControls = {
                poly: new OpenLayers.Control.DrawFeature(this.drawGeozonesLayer, OpenLayers.Handler.Polygon),
                modf: new OpenLayers.Control.ModifyFeature(this.drawGeozonesLayer),
				rect: new OpenLayers.Control.DrawFeature(this.drawGeozonesLayer, OpenLayers.Handler.RegularPolygon, {
                    handlerOptions: {irregular: true, sides: 4}
                }),
                crcl: new OpenLayers.Control.DrawFeature(this.drawGeozonesLayer, OpenLayers.Handler.RegularPolygon, {
                    handlerOptions: {irregular: false, sides: 40}
                })
            };
            this.confirmGeozoneSave = function(btn) {
                if(self.drawGeozonesLayer.features.length > 0) {
                    Ext.MessageBox.show({
                        title: tr('geozonetoolbar.geozonesaving'),
                        msg: tr('geozonetoolbar.geozonesaving.surenew'),
                        icon: Ext.MessageBox.QUESTION,
                        buttons: Ext.Msg.YESNOCANCEL,
                        fn: function(a) {
                            if (a === 'no') {
                                self.drawGeozonesLayer.removeAllFeatures();
                                self.currentDrawing = null;
                                if (btn) btn.btnEl.dom.click();
                            } else if (a === 'yes') {
                                self.drawGeozonesToolbar.down('#geozSave').btnEl.dom.click();
                            }
                        }
                    });
                    return false;
                } else return true;
            };
//            this.drawControls.poly.handler.callbacks.point = this.confirmGeozoneSave;
            
            this.map.addControl(new OpenLayers.Control.MousePosition());
            this.map.addControl(new OpenLayers.Control.LayerSwitcher());
            for (var key in this.drawControls) {
                    this.map.addControl(this.drawControls[key]);
            }
            
            var OSM = new OpenLayers.Layer.OSM('Open Street Maps');
            OSM.codeName = 'openstreetmaps';
            var OSMBlank = new OpenLayers.Layer.XYZ('Open Street Maps (WOL)', [
                    'http://a.tiles.wmflabs.org/osm-no-labels/${z}/${x}/${y}.png',
                    'http://b.tiles.wmflabs.org/osm-no-labels/${z}/${x}/${y}.png',
                    'http://c.tiles.wmflabs.org/osm-no-labels/${z}/${x}/${y}.png'
            ], {sphericalMercator: true, isBaseLayer: true});
            OSMBlank.codeName = 'openstreetmapswol'
            var OSMLocal = new OpenLayers.Layer.XYZ('OSM (' + tr('openlayers.osmlocal') + ')', [
                'http://c.tile.openstreetmap.de:8002/tiles/1.0.0/labels/' + tr('language') + '/${z}/${x}/${y}.png',
                'http://d.tile.openstreetmap.de:8002/tiles/1.0.0/labels/' + tr('language') + '/${z}/${x}/${y}.png'
            ], {sphericalMercator: true, visibility: false, isBaseLayer: false});
            OSMLocal.codeName = 'openstreetmapslocal';
//            var OSMlocal = new OpenLayers.Layer.OSM ('Open Street Maps (wrc)',
//                ["/EDS/osm/${z}/${x}/${y}.png"]);
//            OSMlocal.codeName = 'openstreetmapswayrecall';
            var GoogleRoad;
            var GooglePhy;
            var GoogleHyb;
            var GoogleSat;
            if (!window.forbidGoogleMaps) {
                GoogleRoad = new OpenLayers.Layer.Google(tr('openlayers.googlemap'), {
                    codeName: 'googlemap',
                    type: google.maps.MapTypeId.ROADMAP,
                    visibility: false
                });
                GooglePhy = new OpenLayers.Layer.Google(tr('openlayers.googlemapterrain'), {
                    codeName: 'googlemapterrain',
                    type: google.maps.MapTypeId.TERRAIN,
                    visibility: false
                });
                GoogleHyb = new OpenLayers.Layer.Google(tr('openlayers.googlehybrid'), {
                    codeName: 'googlehybrid',
                    type: google.maps.MapTypeId.HYBRID,
                    visibility: false
                });
                GoogleSat = new OpenLayers.Layer.Google(tr('openlayers.googlesatellite'), {
                    codeName: 'googlesatellite',
                    type: google.maps.MapTypeId.SATELLITE,
                    visibility: false
                });
            }
            //var GoogleTraffic = new google.maps.TrafficLayer();
//            var YahooSat = new OpenLayers.Layer.Yahoo("Yahoo Satellite", {
//                type: YAHOO_MAP_SAT,
//                sphericalMercator: true,
//                visibility: false
//            });
//            var YahooHyb = new OpenLayers.Layer.Yahoo("Yahoo Hybrid", {
//                type: YAHOO_MAP_HYB,
//                sphericalMercator: true,
//                visibility: false
//            });
//            var YahooReg = new OpenLayers.Layer.Yahoo("Yahoo Regional", {
//                type: YAHOO_MAP_REG,
//                sphericalMercator: true,
//                visibility: false
//            });
//            var BingAerial = new OpenLayers.Layer.Bing({
//                type: "Aerial",
//                name: "Bing Aerial Map",
//                key: "AmLdegWSMy9AurilVlZ0zYkout2JufhNmiXnHz59Z0gw79nb0Z92VSdGL6Dk-TmJ",
//                visibility: false
//            });
//            var BingRoad = new OpenLayers.Layer.Bing({
//                type: "Road",
//                name: "Bing Road Map",
//                key: "AmLdegWSMy9AurilVlZ0zYkout2JufhNmiXnHz59Z0gw79nb0Z92VSdGL6Dk-TmJ",
//                visibility: false
//            });
//            var BingLabels = new OpenLayers.Layer.Bing({
//                type: "AerialWithLabels",
//                name: "Bing Road Map",
//                key: "AmLdegWSMy9AurilVlZ0zYkout2JufhNmiXnHz59Z0gw79nb0Z92VSdGL6Dk-TmJ",
//                visibility: false
//            });
            var GoogleTraffic = new OpenLayers.Layer(tr('openlayers.googlejams'), {
                codeName: 'googlejams',
                visibility: false,
                isBaseLayer: false
            });

            GoogleTraffic.events.on({'visibilitychanged': function(){
                if(GoogleTraffic.visibility == true) {
                    GoogleTraffic.trafficLayer = new google.maps.TrafficLayer();
                    GoogleTraffic.trafficLayer.setMap(GoogleRoad.mapObject);
                }
                else if(GoogleTraffic.trafficLayer){
                    GoogleTraffic.trafficLayer.setMap(null);
                    delete GoogleTraffic.trafficLayer;
                }
            }});

            var yandexMap;
            var yandexSat;
            var yandexHyb;
            try {
                yandexMap = new OpenLayers.Layer.Yandex(tr('openlayers.yandexmap'), {type: YMaps.MapType.MAP, sphericalMercator: true});
                yandexMap.codeName = 'yandexmap';
                yandexSat = new OpenLayers.Layer.Yandex(tr('openlayers.yandexsatellite'), {type: YMaps.MapType.SATELLITE, sphericalMercator: true});
                yandexSat.codeName = 'yandexsatellite';
                yandexHyb = new OpenLayers.Layer.Yandex(tr('openlayers.yandexhybrid'), {type: YMaps.MapType.HYBRID, sphericalMercator: true});
                yandexHyb.codeName = 'yandexhybrid';
            }
            catch (e){

            }

            this.markersLayer = new OpenLayers.Layer.Markers(tr('openlayers.markers'));
            this.markersLayer.codeName = 'markers';

            /*   this.lineLayer = new OpenLayers.Layer.Vector("Line Layer", {
             styleMap: new OpenLayers.StyleMap({
             "default": new OpenLayers.Style({
             strokeColor: 'Transparent',
             fillColor: 'Transparent',
             pointRadius: 6
             }),
             "select": new OpenLayers.Style({
             strokeColor: 'Transparent',
             fillColor: 'Transparent',
             pointRadius: 6
             }),
             "temporary": new OpenLayers.Style({
             strokeColor: '#0000cc',
             fillColor: '#0000cc',
             strokeOpacity: .8,
             strokeWidth: 1,
             fillOpacity: .3,
             cursor: "default",
             pointRadius: 6
             })
             })})*/

//            this.lineLayer.events.on({
//                'featureselected': function(feature) {
//                    console.log("sdfg featureselected", feature )
//                },
//                'featureunselected': function(feature) {
//                    console.log("sdfg featureunselected", feature)
//                }
//            });

            //var self = this;

//            var report = function (e) {
//                //console.log("e=", e);
//                self.fireEvent('onMapSelection', self, e)
//            };

//            var highlightCtrl = new OpenLayers.Control.SelectFeature(this.lineLayer, {
//                hover: true,
//                highlightOnly: true,
//                renderIntent: "temporary"
////                eventListeners: {
////                    beforefeaturehighlighted: report,
////                    featurehighlighted: report,
////                    featureunhighlighted: report
////                }
//            });
//
//            var selectCtrl = new OpenLayers.Control.SelectFeature(this.lineLayer,
//                {
//                    clickout: false,
//                    onSelect: report
//                }
//            );
//
//            this.map.addControl(highlightCtrl);
//            this.map.addControl(selectCtrl);
//
//            highlightCtrl.activate();
//            selectCtrl.activate();

            var mapsList = [
                OSM, OSMBlank, OSMLocal,
                GoogleRoad, GooglePhy, GoogleHyb, GoogleSat,
                 /*YahooReg, YahooSat, YahooHyb, BingRoad, BingAerial, BingLabels,*/
                yandexMap, yandexHyb, yandexSat,
                GoogleTraffic/*, Yandex, YandexSat, YandexHyb*//*, this.lineLayer*/,
                this.showGeozonesLayer, this.drawGeozonesLayer, this.markersLayer
            ];
            this.map.addLayers(Ext.Array.filter(mapsList, function(a) {return a != undefined;}));
            this.proj = new OpenLayers.Projection("EPSG:4326");
            var lonlat = new OpenLayers.LonLat(55, 37);
            lonlat.transform(this.proj, this.map.getProjectionObject());
            this.map.setCenter(lonlat, 3);
            self.onChangeLayer = function(e) {
                if (e.property === 'visibility') {
                    if (e.layer.isBaseLayer && e.layer.visibility) {
                        Ext.util.Cookies.set('currentMapLayer', e.layer.codeName, new Date(new Date().getTime()+(1000*60*60*24*365)));
                    }
                }
            };
            this.map.events.register('changelayer', null, self.onChangeLayer);

//            var drawFeature = new OpenLayers.Control.DrawFeature(this.lineLayer, OpenLayers.Handler.Point);
//            this.map.addControl(drawFeature);
//
//            drawFeature.activate()


//            this.tempMarkersArray = new Array();
//            this.map.events.register('mousemove', this, function () {
//                if (this.tempMarkersArray.length>1) {                  
//                    var e 
//                    while ((e = this.tempMarkersArray.pop())) {
//                        e=this.tempMarkersArray.shift()
//                        this.markersLayer.removeMarker(e)
//                    }
//                }
//
//            })

            var wnd = this.up('window');
            if (wnd) {
                wnd.on('move', function (thiss, x, y, eOpts) {
                    console.log("window moved ");
                    //self.map.render(self.mapHolder.body.dom);
                    self.map.updateSize();
                });
                wnd.on('restore', function (thiss) {
                    console.log("window restore ");
                    self.map.updateSize();
                });
            }

        }

    }
);
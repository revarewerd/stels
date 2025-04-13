Ext.define('Seniel.view.reports.ReportMap', {
    extend: 'Seniel.view.OpenLayerMaps',
    alias: 'widget.reportmap',
    listeners: {
        boxready: function () {
            var mapWidget = this;
            console.log('map rendered');
            mapWidget.mapClick = new OpenLayers.Control.Click({
                onClick: function (evt) {
                    console.log('Map click');
                    mapWidget.up('window').setActive(true);
                }
            });
            mapWidget.map.addControl(mapWidget.mapClick);
            mapWidget.mapClick.activate();
        },
        resize: function () {
            this.map.updateSize();
        }
    },
    selectRowInGrid: function (obj, e) {
        if (e.n) {
            var win = obj.up('window');
            if (!!win.down('tabpanel')) win.down('tabpanel').setActiveTab('movement');
            win.down('pathgrid').selectRowInGrid(e.n - 1);
        }
    },
    refreshReport: function (settings) {
        var mapWidget = this;
        this.removeGeozones();        

        console.log("seniel-mapwidget refreshReport");
        mapWidget.on('onMapSelection', mapWidget.selectRowInGrid);

        var from = settings.dates.from;
        var to = settings.dates.to;
        var uid = settings.selected;
        mapWidget.repObjectId = uid;
        mapWidget.dateFrom = from;
        mapWidget.dateTo = to;

        mapWidget.drawMovingPath(uid, from, to);
    },
    createMovingPathLayer: function (uid, from, to) {
        var mapWidget = this;

        // //Очистка слоя WMS
        // if (mapWidget.pathWMSLayer) {
        //     if (mapWidget.pathWMSHover) {
        //         mapWidget.pathWMSHover.deactivate();
        //         mapWidget.pathWMSHover.destroy();
        //         delete mapWidget.pathWMSHover;
        //     }
        //     mapWidget.pathWMSLayer.destroy();
        //     delete mapWidget.pathWMSLayer;
        // }

        //Добавление WMS
        mapWidget.pathWMSLayer = new OpenLayers.Layer.WMS(tr('reportmap.objmovings'),
            'serv',
            {
                layers: 'PathReport',
                transparent: true,
                reportData: Ext.encode({
                    from: from,
                    to: to,
                    selected: uid
                })
            },
            {
                singleTile: true,
                ratio: 2,
                visibility: true
            }
        );
        mapWidget.map.addLayer(mapWidget.pathWMSLayer);

        //Добавление WMSHover
        if (mapWidget.up('window') && mapWidget.up('window').down('pathgrid')) {
            mapWidget.pathWMSHover = mapWidget.createMovingPathHover(uid, from, to);
            mapWidget.map.addControl(mapWidget.pathWMSHover);
            mapWidget.pathWMSHover.activate();
        }

        mapWidget.map.raiseLayer(mapWidget.pathWMSLayer, -2);
        mapWidget.map.resetLayersZIndex();
    },
    createMovingPathHover: function (uid, from, to) {
        var mapWidget = this;

        return new OpenLayers.Control.Hover({
            handlerOptions: {
                'delay': 500,
                'pixelTolerance': 6
            },
            onPause: function (evt) {
                var self = this;

                var ll = mapWidget.map.getLonLatFromPixel(evt.xy);
                ll.transform(mapWidget.map.getProjectionObject(), mapWidget.proj);

                positionService.getNearestPosition(
                    uid,
                    from,
                    to,
                    ll.lon, ll.lat,
                    0.0001 * mapWidget.map.getResolution(),
                    function (res) {
                        if (res.error !== 'noresult') {
                            var lonlat = mapWidget.transLonLat(res.lon, res.lat);
                            var markersLayer = mapWidget.markersLayer;

                            if (mapWidget.reportCursorMarker) {
                                self.removePathMarkers();
                            }

                            var icon;
                            if (res.speed === 0) {
                                var size = new OpenLayers.Size(12, 12);
                                icon = new OpenLayers.Icon('images/greenPoint.png', size);
                            }
                            else {
                                var size = new OpenLayers.Size(24, 24);
                                icon = new OpenLayers.Icon('rotate?img=arrow4.png&rot=' + res.course, size);
                            }

                            mapWidget.reportCursorMarker = new OpenLayers.Marker(lonlat, icon);
                            mapWidget.reportCursorMarker.events.register('click', mapWidget.reportCursorMarker, function (evt) {
                                positionService.getIndex(
                                    uid,
                                    mapWidget.dateFrom,
                                    res.time,
                                    function (res) {
                                        mapWidget.fireEvent('onMapSelection', mapWidget, res);
                                    }
                                );
                            });
                            markersLayer.addMarker(mapWidget.reportCursorMarker);
//                            console.log("reportCursorMarker drawn at first");
                        }
                    }
                );
            },
            removePathMarkers: function () {
                mapWidget.markersLayer.removeMarker(mapWidget.reportCursorMarker);
                delete mapWidget.reportCursorMarker;
            },
            onMove: function (evt) {
                if (mapWidget.reportCursorMarker) {
                    var dist = mapWidget.map.getPixelFromLonLat(mapWidget.reportCursorMarker.lonlat).distanceTo(evt.xy);
//                    console.log("dist=" + dist);
                    if (dist > 10) {
                        this.removePathMarkers();
                    }
                }
            }
        });
    },
    cleanMap: function () {
        var mapWidget = this;

        if (mapWidget.pathWMSLayer) {
            if (mapWidget.pathWMSHover) {
                mapWidget.pathWMSHover.deactivate();
                mapWidget.pathWMSHover.destroy();
                delete mapWidget.pathWMSHover;
            }
            mapWidget.pathWMSLayer.destroy();
            delete mapWidget.pathWMSLayer;
        }

        mapWidget.markersLayer.clearMarkers();
        delete mapWidget.temproraryMarker;
    },
    drawMovingPath: function (uid, from, to) {
        var mapWidget = this;

         this.cleanMap();
         // mapWidget.markersLayer.clearMarkers();
         // delete mapWidget.temproraryMarker;

        if (mapWidget.curposMarker) {
            mapWidget.markersLayer.addMarker(mapWidget.curposMarker);
        }

//        this.on('onMapSelection', this.selectRowInGrid);

        Ext.Ajax.request({
            method: 'GET',
            url: 'pathdata',
            params: {
                from: from,
                to: to,
                selected: uid,
                data: "path"
            },
            success: function (result, request) {
                var respJson = Ext.decode(result.responseText);
                console.log("result.responseText", result.responseText)

                if (respJson.first) {
                    mapWidget.createMovingPathLayer(uid, from, to);
                    mapWidget.addStartStopMarkers(respJson.first.lon, respJson.first.lat, respJson.last.lon, respJson.last.lat);
                }

                if (respJson.minlon !== 'Infinity' && respJson.minlat !== 'Infinity' && respJson.maxlon !== '-Infinity' && respJson.maxlat !== '-Infinity') {
                    var minlonlat = mapWidget.transLonLat(respJson.minlon, respJson.minlat),
                        maxlonlat = mapWidget.transLonLat(respJson.maxlon, respJson.maxlat),
                        bounds = new OpenLayers.Bounds();
                    bounds.extend(minlonlat);
                    bounds.extend(maxlonlat);
                    mapWidget.map.zoomToExtent(bounds);
                } else {
                    var clat = (respJson.maxlat - respJson.minlat) / 2 + respJson.minlat,
                        clon = (respJson.maxlon - respJson.minlon) / 2 + respJson.minlon;
                    mapWidget.map.setCenter(mapWidget.transLonLat(clon, clat), 8);
                }
            },
            failure: function () {
                console.log("MapObjectLonLatServlet failure");
            }
        });
    },
    drawParkingPoints: function (records) {
        var mapWidget = this;
        if (mapWidget.parkingLayer) {
            mapWidget.map.removeLayer(mapWidget.parkingLayer);
            delete mapWidget.parkingLayer;
            console.log('remove parking layer');
        }
        mapWidget.parkingLayer = new OpenLayers.Layer.Markers(tr("reportmap.parkingsandstoppings"));
        mapWidget.parkingLayer.setVisibility(false);
        mapWidget.map.addLayer(mapWidget.parkingLayer);
        mapWidget.map.setLayerIndex(mapWidget.parkingLayer, mapWidget.map.getLayerIndex(mapWidget.markersLayer));
        mapWidget.map.resetLayersZIndex();

        var parkingGrid = mapWidget.up('reportwnd').down('parkinggrid');
        Ext.Array.each(records, function (rec) {
            if (!rec.marker) {
                var isParking = (rec.get('intervalRaw') >= 5 * 60 * 1000) ? (true) : (false),
                    lonlat = mapWidget.transLonLat(rec.get('lon'), rec.get('lat')),
                    src = (isParking) ? ('images/ico24_parking.png') : ('images/ico24_parking.png'),
                    size = new OpenLayers.Size(24, 24),
                    offset = new OpenLayers.Pixel(-12, -12),
                    icon = new OpenLayers.Icon(src, size, offset);
                rec.marker = new OpenLayers.Marker(lonlat, icon);
                rec.marker.events.register('click', rec.marker, function (evt) {
                    parkingGrid.selectRowInParkingGrid(parkingGrid.getStore().indexOf(rec));
                });
                mapWidget.parkingLayer.addMarker(rec.marker);
            }
        });
    },
    addStartStopMarkers: function (flon, flat, llon, llat) {
        var mapWidget = this,
            flonlat = mapWidget.transLonLat(flon, flat),
            llonlat = mapWidget.transLonLat(llon, llat),
            markersLayer = mapWidget.markersLayer,
            size = new OpenLayers.Size(24, 24),
            offset = new OpenLayers.Pixel(-7, -23),
            iconStart = new OpenLayers.Icon('images/ico24_flag_str.png', size, offset),
            iconFinish = new OpenLayers.Icon('images/ico24_flag_fin.png', size, offset);
        mapWidget.startMarker = new OpenLayers.Marker(flonlat, iconStart);
        mapWidget.finishMarker = new OpenLayers.Marker(llonlat, iconFinish);
        markersLayer.addMarker(mapWidget.startMarker);
        markersLayer.addMarker(mapWidget.finishMarker);
    },
    addCurrentPosMarker: function (lon, lat, cur, uid) {
        var mapWidget = this,
            lonlat = mapWidget.transLonLat(lon, lat),
            markersLayer = mapWidget.markersLayer,
            img = new Image();
        mapWidget.map.setCenter(lonlat);
        objectSettings.loadObjectSettings(uid, function (resp) {
            if (resp && resp.imgSource) {
                if (resp.imgRotate || resp.imgSource.search(/rot/) > -1) {
                    img.src = 'rotate?img=' + resp.imgSource.substring(7) + '&rot=' + cur;
                } else {
                    img.src = resp.imgSource;
                }
            } else {
                img.src = 'rotate?img=cars/car_rot_swc_32.png&rot=' + cur;
            }
        });
        img.onload = function () {
            var size = new OpenLayers.Size(img.width, img.height),
                offset = new OpenLayers.Pixel(-size.w / 2, -size.h / 2),
                icon = new OpenLayers.Icon(img.src, size, offset);
            mapWidget.curposMarker = new OpenLayers.Marker(lonlat, icon);
            markersLayer.addMarker(mapWidget.curposMarker);
        };
    },
    addTemproraryMarker: function (lon, lat, angle) {
        var mapWidget = this,
            lonlat = mapWidget.transLonLat(lon, lat),
            markersLayer = mapWidget.markersLayer;
        mapWidget.map.setCenter(lonlat);
        if (mapWidget.temproraryMarker) {
            mapWidget.temproraryMarker.lonlat = lonlat;
            markersLayer.drawMarker(mapWidget.temproraryMarker);
            console.log("Marker drawn in new lonlat");
        } else {
            var size = new OpenLayers.Size(20, 25),
                offset = new OpenLayers.Pixel(-(size.w / 2), -size.h),
                icon = new OpenLayers.Icon('images/ico_marker.png', size, offset);
            mapWidget.temproraryMarker = new OpenLayers.Marker(lonlat, icon);
            markersLayer.addMarker(mapWidget.temproraryMarker);
            console.log("Marker drawn at first");
        }
    },
    addPositionMarker: function (lon, lat) {
        var mapWidget = this,
            lonlat = mapWidget.transLonLat(lon, lat),
            markersLayer = mapWidget.markersLayer;

        mapWidget.map.setCenter(lonlat);
        if (mapWidget.temproraryMarker && mapWidget.temproraryMarker.markerType === 'position' && lonlat.lat && lonlat.lon) {
            mapWidget.temproraryMarker.lonlat = lonlat;
            markersLayer.drawMarker(mapWidget.temproraryMarker);
        } else {
            if (mapWidget.temproraryMarker) {
                markersLayer.removeMarker(mapWidget.temproraryMarker);
                delete mapWidget.temproraryMarker;
            }
            if (lonlat.lat && lonlat.lon) {
                var size = new OpenLayers.Size(20, 25),
                    offset = new OpenLayers.Pixel(-(size.w / 2), -size.h),
                    icon = new OpenLayers.Icon('images/ico_marker.png', size, offset);
                mapWidget.temproraryMarker = new OpenLayers.Marker(lonlat, icon);
                mapWidget.temproraryMarker.markerType = 'position';
                markersLayer.addMarker(mapWidget.temproraryMarker);
            }
        }
    },
    addParkingMarker: function (lon, lat, isParking) {
        var mapWidget = this,
            lonlat = mapWidget.transLonLat(lon, lat),
            markersLayer = mapWidget.markersLayer;

        mapWidget.map.setCenter(lonlat);
        if (mapWidget.temproraryMarker && ((isParking && mapWidget.temproraryMarker.markerType === 'parking') || (!isParking && mapWidget.temproraryMarker.markerType === 'stopping'))) {
            mapWidget.temproraryMarker.lonlat = lonlat;
            markersLayer.drawMarker(mapWidget.temproraryMarker);
        } else {
            if (mapWidget.temproraryMarker) {
                markersLayer.removeMarker(mapWidget.temproraryMarker);
                delete mapWidget.temproraryMarker;
            }
            var src = (isParking) ? ('images/ico24_parking.png') : ('images/ico24_parking.png'),
                size = new OpenLayers.Size(24, 24),
                offset = new OpenLayers.Pixel(-12, -12),
                icon = new OpenLayers.Icon(src, size, offset);
            mapWidget.temproraryMarker = new OpenLayers.Marker(lonlat, icon);
            markersLayer.addMarker(mapWidget.temproraryMarker);
            mapWidget.temproraryMarker.markerType = (isParking) ? ('parking') : ('stopping');
        }
    },
    removeGeozones: function () {
        if (this.previousGeozone) {
            this.showGeozonesLayer.removeFeatures([this.previousGeozone]);
            delete this.previousGeozone;
        }
    },
    drawGeozone: function (rec) {
        this.removeGeozones();

        var points = Ext.JSON.decode(rec.points),
            olpoints = new Array();
        for (var i = 0; i < points.length; i++) {
            olpoints.push(new OpenLayers.Geometry.Point(points[i].x, points[i].y));
        }
        var linearRing = new OpenLayers.Geometry.LinearRing(olpoints);
        var feature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Polygon([linearRing]), {'ftcolor': rec.ftColor});
        this.showGeozonesLayer.addFeatures([feature]);

        // if (!this.isCreation && !this.isEditing) {
        //     var bounds = feature.geometry.bounds;
        //     this.map.zoomToExtent(bounds);
        // }
        this.previousGeozone = feature;
    }
});

/**
 * Created by IVAN on 22.09.2015.
 */
Ext.define('Seniel.view.reports.GroupReportMap', {
    extend: 'Seniel.view.reports.ReportMap',
    alias: 'widget.groupreportmap',
    listeners: {
        boxready: function() {
            var mapWidget = this;
            console.log('map rendered');
            mapWidget.mapClick = new OpenLayers.Control.Click({
                onClick: function(evt) {
                    console.log('GroupMap click');
                    mapWidget.up('window').setActive(true);
                }
            });
            mapWidget.map.addControl(mapWidget.mapClick);
            mapWidget.mapClick.activate();
        },
        resize: function() {
            this.map.updateSize();
        }
    },
    popupsArray: [],
    getPopups:function(id){
        if(id)
        {
            return this.popupsArray[id];
        }
        else
            return this.popupsArray
    },
    showPopup:function(mes){
        var time = Ext.Date.format(new Date(mes.time), "d.m.Y H:i:s withTZ");
        var gridStore=this.up('window').down('grouppathgrid').getStore();
        console.log("gridStore",gridStore);
        var name= gridStore.findRecord("uid",mes.uid).get("name")
        var text = '<div class="ol-popup-content"><b>' + tr('notifications.history.grid.object') + ': </b>' + name
            + '<br/><b>' + tr('notifications.history.grid.time') + ': </b>' + time + '<br/><b>'
            + tr('notifications.history.grid.speed') + ': </b>' + tr(mes.speed)+' '+ tr('units.speed') + '<br/><b>'
            + tr('notifications.history.grid.place') + ': </b>' + tr(mes.placeName) + '</div>';

        if(!mes.lon && !mes.lat){
            mes.lon=lon;
            mes.lat=lat;
        }
        this.addPopup(text, mes);
    },
    addPopup: function(text, mes) {
        console.log("mes",mes)
        var lonlat = this.transLonLat(mes.lon, mes.lat);
        var id = mes.n;
        var uid= mes.uid;
        var self = this;

        if (!this.popupsArray[id]) {
            var btn = Ext.create('Ext.panel.Tool', {
                iconCls: 'x-tool-close',
                handler: function() {
                    self.removePopup(id);
                    //var eventsWnd = [self.up('viewport').down('eventswnd'), Ext.getCmp('notificationsPopupWindow')];
                    //for (var i in eventsWnd) {
                    //    if (eventsWnd[i]) {
                    //        var rec = eventsWnd[i].down('grid').getStore().findRecord('eid', id);
                    //        if (rec) rec.set('onMap', false);
                    //    }
                    //}
                    //var rec=this.up('window').down('grid').getStore().findRecord('uid', uid);
                    //var mapobjectlist = Ext.ComponentQuery.query('mapobjectlist');
                    //var coll = new Ext.util.MixedCollection();
                    //coll.add('uid', uid);
                    //mapobjectlist[0].fireEvent('mapobjremoveevent', coll);
                }
            });
            var popup = new OpenLayers.Popup.FramedCloud('olpopup-' + id, lonlat, null, '<span id="olpopup-close-' + id + '" style="float: right;"></span>' + text, null);
            popup.uid=uid;
            popup.autoSize = true;
            popup.closeBtn = btn;
            this.popupsArray[id] = popup;

            this.map.addPopup(popup);
            btn.render('olpopup-close-' + id);
            popup.updateSize();
            //self.showPopupMarker(lonlat,self.getPopups(id))
        }
        this.map.setCenter(lonlat);
    },
    removePopup: function(id) {
        var self=this;
        if (this.popupsArray[id]) {
            var popup = this.popupsArray[id];
            delete popup.closeBtn;
            this.map.removePopup(popup);
            //self.removePopupMarker(self.getPopups(id))
            delete this.popupsArray[id];
        }
    },
    removeAllPopups: function() {
        var self=this;
        for(var i in this.popupsArray){
            var popup = this.popupsArray[i];
                delete popup.closeBtn;
                self.map.removePopup(popup);
                //self.removePopupMarker(self.getPopups(i))
                delete this.popupsArray[i];
        }

        //if (this.popupsArray[id]) {
        //    var popup = this.popupsArray[id];
        //    delete popup.closeBtn;
        //    this.map.removePopup(popup);
        //    self.removePopupMarker(self.getPopups(id))
        //    delete this.popupsArray[id];
        //}
    },
    selectRowInGrid: function(map, res) {
        this.showPopup(res)
    },
    refreshReport: function(settings) {
        var mapWidget = this;
        mapWidget.cleanMap();
        mapWidget.cleanMap();
        mapWidget.removeAllPopups();

        console.log("seniel-mapwidget refreshReport");
        mapWidget.on('onMapSelection', mapWidget.selectRowInGrid);

    },
    createMovingPathLayer: function(uid, from, to) {
        var mapWidget = this;

        //Очистка слоя WMS
        if (mapWidget.pathWMSLayer) {
            if (mapWidget.pathWMSHover) {
                mapWidget.pathWMSHover.deactivate();
                mapWidget.pathWMSHover.destroy();
                delete mapWidget.pathWMSHover;
            }
            mapWidget.pathWMSLayer.destroy();
            delete mapWidget.pathWMSLayer;
        }

        // Удаление всех всплывающих окон
        mapWidget.removeAllPopups();

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
        if (mapWidget.up('groupreportwnd') && mapWidget.up('groupreportwnd').down('grouppathgrid')) {
            mapWidget.pathWMSHover = mapWidget.createMovingPathHover(uid, from, to);
            mapWidget.map.addControl(mapWidget.pathWMSHover);
            mapWidget.pathWMSHover.activate();
        }

        mapWidget.map.raiseLayer(mapWidget.pathWMSLayer, -2);
        mapWidget.map.resetLayersZIndex();
    },
    createMovingPathHover: function(uid, from, to) {
        var mapWidget = this;

        return new OpenLayers.Control.Hover({
            handlerOptions: {
                'delay': 500,
                'pixelTolerance': 6
            },
            onPause: function(evt) {
                var self = this;

                var ll = mapWidget.map.getLonLatFromPixel(evt.xy);
                ll.transform(mapWidget.map.getProjectionObject(), mapWidget.proj);
                positionService.getNearestPosition(
                    uid,
                    from,
                    to,
                    ll.lon, ll.lat,
                    0.0001 * mapWidget.map.getResolution(),
                    function(res) {
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
                            mapWidget.reportCursorMarker.events.register('click', mapWidget.reportCursorMarker, function(evt) {
                                positionService.getIndex(
                                    uid,
                                    from,
                                    res.time,
                                    function(e) {
                                        res.uid=uid
                                        res.n= e.n
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
            removePathMarkers: function() {
                mapWidget.markersLayer.removeMarker(mapWidget.reportCursorMarker);
                delete mapWidget.reportCursorMarker;
            },
            onMove: function(evt) {
                if (mapWidget.reportCursorMarker) {
                    var dist = mapWidget.map.getPixelFromLonLat(mapWidget.reportCursorMarker.lonlat).distanceTo(evt.xy);
//                    console.log("dist=" + dist);
                    if (dist > 10) {
                        this.removePathMarkers();
                    }
                }
            }
        });
    }
})

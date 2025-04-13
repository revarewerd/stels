Ext.define('Seniel.view.GeoZoneToolbar', {
    extend: 'Ext.window.Window',
    alias: 'widget.geozonetbr',
    bodyBorder: false,
    bodyPadding: 0,
    padding: 1,
    border: false,
    header: false,
    minHeight: 0,
    minWidth: 438,
    resizable: false,
    onEsc: Ext.emptyFn,
    layout: 'fit',
    tbar: [
        {
            xtype: 'textfield',
            itemId: 'geozName',
            emptyText: tr('geozonetoolbar.geozonename'),
            height: 32,
            width: 120
        },
        {
            xtype: 'tbspacer',
            width: 2
        },
        {
            xtype: 'button',
            itemId: 'geozPoly',
            icon: 'images/ico24_draw_poly.png',
            tooltip: tr('geozonetoolbar.drawpolygon'),
            tooltipType: 'title',
            scale: 'medium',
            toggleGroup: 'GeoZoneDrawCtrls',
            toggleHandler: function(btn, pressed) {
                var wnd = btn.up('window'),
					map = wnd.map,
                    ctrl = map.drawControls.poly;
                if (pressed) {
					if (wnd.curfeature) {
						map.confirmGeozoneSave(btn);
						btn.toggle(false);
						return false;
					}
                    ctrl.activate();
                    map.drawGeozonesLayer.drawStarted = true;
                } else {
                    ctrl.deactivate();
                    map.drawGeozonesLayer.drawStarted = false;
                }
            }
        },
        {
            xtype: 'button',
            itemId: 'geozRect',
            icon: 'images/ico24_draw_rect.png',
            tooltip: tr('geozonetoolbar.drawrect'),
            tooltipType: 'title',
            scale: 'medium',
            toggleGroup: 'GeoZoneDrawCtrls',
            toggleHandler: function(btn, pressed) {
                var wnd = btn.up('window'),
					map = wnd.map,
                    ctrl = map.drawControls.rect;
                if (pressed) {
					if (wnd.curfeature) {
						map.confirmGeozoneSave(btn);
						btn.toggle(false);
						return false;
					}
                    ctrl.activate();
                    map.drawGeozonesLayer.drawStarted = true;
                } else {
                    ctrl.deactivate();
                    map.drawGeozonesLayer.drawStarted = false;
                }
            }
        },
        {
            xtype: 'button',
            itemId: 'geozCrcl',
            icon: 'images/ico24_draw_crcl.png',
            tooltip: tr('geozonetoolbar.drawcircle'),
            tooltipType: 'title',
            scale: 'medium',
            toggleGroup: 'GeoZoneDrawCtrls',
            toggleHandler: function(btn, pressed) {
                var wnd = btn.up('window'),
					map = wnd.map,
                    ctrl = map.drawControls.crcl;
                if (pressed) {
					if (wnd.curfeature) {
						map.confirmGeozoneSave(btn);
						btn.toggle(false);
						return false;
					}
                    ctrl.activate();
                    map.drawGeozonesLayer.drawStarted = true;
                } else {
                    ctrl.deactivate();
                    map.drawGeozonesLayer.drawStarted = false;
                }
            }
        },
        {
            xtype: 'button',
            itemId: 'geozResh',
            icon: 'images/ico24_draw_resh.png',
            tooltip: tr('geozonetoolbar.editvertices'),
            tooltipType: 'title',
            scale: 'medium',
            toggleGroup: 'GeoZoneDrawCtrls',
            toggleHandler: function(btn, pressed) {
                var wnd = btn.up('window'),
					map = wnd.map,
                    ctrl = map.drawControls.modf;
                if (pressed) {
					if (wnd.curfeature.geometry.components[0].components.length === 41) {
						ctrl.mode = OpenLayers.Control.ModifyFeature.RESIZE;
					} else {
						ctrl.mode = OpenLayers.Control.ModifyFeature.RESHAPE;
					}
                    map.selectControl.deactivate();
                    ctrl.activate();
                } else {
                    ctrl.deactivate();
					map.selectControl.activate();
                }
            }
        },
        {
            xtype: 'button',
            itemId: 'geozMove',
            icon: 'images/ico24_draw_move.png',
            tooltip: tr('geozonetoolbar.movegeozone'),
            tooltipType: 'title',
            scale: 'medium',
            toggleGroup: 'GeoZoneDrawCtrls',
            toggleHandler: function(btn, pressed) {
                var map = btn.up('window').map,
                    ctrl = map.drawControls.modf;
                if (pressed) {
                    ctrl.mode = OpenLayers.Control.ModifyFeature.DRAG;
                    map.selectControl.deactivate();
                    ctrl.activate();
                } else {
                    ctrl.deactivate();
                    map.selectControl.activate();
                }
            }
        },
//        {
//            xtype: 'button',
//            itemId: 'geozRota',
//            icon: 'images/ico24_draw_rota.png',
//            tooltip: 'Повернуть геозону',
//            tooltipType: 'title',
//            scale: 'medium',
//            toggleGroup: 'GeoZoneDrawCtrls',
//            toggleHandler: function(btn, pressed) {
//                var map = btn.up('window').map,
//                    ctrl = map.drawControls.modf;
//                if (pressed) {
//                    ctrl.mode = OpenLayers.Control.ModifyFeature.ROTATE;
//                    ctrl.activate();
//                    ctrl.selectFeature(map.drawGeozonesLayer.features[0]);
//                } else {
//                    ctrl.deactivate();
//                }
//            }
//        },
        {
            xtype: 'button',
            itemId: 'geozColor',
            icon: 'images/ico24_draw_colp.png',
            tooltip: tr('geozonetoolbar.chosecolor'),
            tooltipType: 'title',
            scale: 'medium',
            menu: Ext.create('Ext.menu.ColorPicker', {
                value: 'FF9900',
                listeners: {
                    select: function(pkr, color) {
                        var btn = pkr.up('button'),
                            map = btn.up('viewport').down('mainmap');
                        btn.el.dom.childNodes[0].childNodes[0].style.backgroundColor = '#' + color;
                        btn.curColor = '#' + color;
                        
                        if (map.geozonetbr.curfeature) {
                            map.geozonetbr.curfeature.attributes = {
                                ftcolor: btn.curColor
                            };
                        }
                        map.drawGeozonesLayer.redraw();
                    }
                }
            }),
            listeners: {
                afterrender: function(btn) {
                    btn.el.dom.childNodes[0].childNodes[0].style.backgroundColor = '#FF9900';
                    btn.curColor = '#FF9900';
                }
            }
        },
        {
            xtype: 'tbspacer',
            width: 20
        },
        {
            xtype: 'button',
            itemId: 'geozEdit',
            icon: 'images/ico24_okcrc_def.png',
            tooltip: tr('geozonetoolbar.savechanges'),
            tooltipType: 'title',
            scale: 'medium',
			hidden: true,
            handler: function(btn) {
                var wnd = btn.up('window'),
					name = wnd.down('textfield').getValue();
                
                if (name === '') {
                    name = wnd.editparams.name;
                }
                
                var geozone = {
                    id: wnd.editparams.id,
                    name: name,
                    ftColor: wnd.down('button[itemId="geozColor"]').curColor
                };
                if (wnd.map.drawGeozonesLayer.features.length === 0) {
                    geozone.points = Ext.JSON.decode(wnd.editparams.points);
                } else {
                    geozone.points = new Array();
                    var points = wnd.curfeature.geometry.components[0].components;
                    for (var i = 0; i < points.length; i++) {
                        geozone.points[i] = {
                            x: points[i].x,
                            y: points[i].y
                        };
                    }
                }
                
                console.log('Edited geozone = ', geozone);
                geozonesData.editGeozone(geozone, function(resp) {
                    if (resp) {
                        wnd.map.drawGeozonesLayer.removeAllFeatures();
                        wnd.down('textfield').setValue('');
						var viewport = wnd.up('viewport');
						viewport.geozStore.reload();
						wnd.down('button[itemId="geozEdit"]').hide();
						wnd.down('button[itemId="geozSave"]').show();
                        if (wnd.editparams.rec) {
                            wnd.editparams.rec.set('name', geozone.name);
                            wnd.editparams.rec.set('ftColor', geozone.ftColor);
                            wnd.editparams.rec.set('points', Ext.JSON.encode(geozone.points, null, 4));
                        }
                        if (wnd.editparams.hideAfterEdit) {
                            wnd.hide();
                        }
                        wnd.editparams = null;
                        wnd.curfeature = null;
                    }
                });
            }
        },
        {
            xtype: 'button',
            itemId: 'geozSave',
            icon: 'images/ico24_okcrc_def.png',
            tooltip: tr('geozonetoolbar.savegeozone'),
            tooltipType: 'title',
            scale: 'medium',
            handler: function(btn) {
                var wnd = btn.up('window');
                
                if (wnd.down('textfield').getValue() === '') {
                    Ext.MessageBox.show({
                        title: tr('geozonetoolbar.savegeozone.noname'),
                        msg: tr('geozonetoolbar.savegeozone.noname.msg'),
                        icon: Ext.MessageBox.WARNING,
                        buttons: Ext.Msg.OK
                    });
                    return false;
                }
                if (wnd.map.drawGeozonesLayer.features.length === 0) {
                    Ext.MessageBox.show({
                        title: tr('geozonetoolbar.savegeozone.nogeozone'),
                        msg: tr('geozonetoolbar.savegeozone.nogeozone'),
                        icon: Ext.MessageBox.WARNING,
                        buttons: Ext.Msg.OK
                    });
                    return false;
                }
                
                var geozone = new Object();
                geozone.name = wnd.down('textfield').getValue();
                geozone.ftColor = wnd.curfeature.attributes.ftcolor;
                geozone.points = new Array();
                var points = wnd.curfeature.geometry.components[0].components;
                for (var i = 0; i < points.length; i++) {
                    geozone.points[i] = {
                        x: points[i].x,
                        y: points[i].y
                    }
                }
                console.log('New geozone = ', geozone);
                geozonesData.addGeozone(geozone, function(resp) {
                    if (resp) {
                        wnd.map.drawGeozonesLayer.removeAllFeatures();
						wnd.curfeature = null;
                        wnd.down('textfield').setValue('');
						var viewport = wnd.up('viewport');
						viewport.geozStore.reload();
                    }
                });
            }
        },
        {
            xtype: 'button',
            itemId: 'geozHide',
            icon: 'images/ico24_cancel_def.png',
            tooltip: tr('geozonetoolbar.closepanel'),
            tooltipType: 'title',
            scale: 'medium',
            handler: function(btn) {
                var wnd = btn.up('window'),
                    editMode = false,
                    isEdited = false;
                
                wnd.down('button[itemId="geozResh"]').toggle(false);
                
                if (wnd.editparams) {
                    editMode = true;
                    
                    if (wnd.editparams.name !== wnd.down('textfield').getValue()) {
                        isEdited = true;
                    }
                    if (wnd.editparams.ftColor !== wnd.down('button[itemId="geozColor"]').curColor) {
                        isEdited = true;
                    }
                    
                    var points = wnd.curfeature.geometry.components[0].components,
                        loadedPoints = Ext.JSON.decode(wnd.editparams.points);
                    if (loadedPoints.length !== points.length) {
                        isEdited = true;
                    }
                    for (var i = 0; i < points.length; i++) {
                        if ((points[i].x !== loadedPoints[i].x) || (points[i].y !== loadedPoints[i].y)) {
                            isEdited = true;
                            break;
                        }
                    }
                    
                    if (isEdited) {
                        Ext.MessageBox.show({
                            title: tr('geozonetoolbar.geozonesaving'),
                            msg: tr('geozonetoolbar.geozonesaving.sure'),
                            icon: Ext.MessageBox.QUESTION,
                            buttons: Ext.Msg.YESNOCANCEL,
                            fn: function(a) {
                                if (a === 'no') {
                                    wnd.editparams = null;
                                    wnd.hide();
                                } else if (a === 'yes') {
                                    wnd.editparams.hideAfterEdit = true;
                                    wnd.down('button[itemId="geozEdit"]').btnEl.dom.click();
                                }
                            }
                        });
                        return false;
                    } else {
                        wnd.editparams = null;
                    }
                }
                
                if(wnd.map.drawGeozonesLayer.features.length > 0 && !editMode) {
                    Ext.MessageBox.show({
                        title: tr('geozonetoolbar.geozonesaving'),
                        msg: tr('geozonetoolbar.geozonesaving.surenew'),
                        icon: Ext.MessageBox.QUESTION,
                        buttons: Ext.Msg.YESNOCANCEL,
                        fn: function(a) {
                            if (a === 'no') {
                                wnd.hide();
                            } else if (a === 'yes') {
                                wnd.down('button[itemId="geozSave"]').btnEl.dom.click();
                            }
                        }
                    });
                    return false;
                }
                
                wnd.hide();
            }
        }
    ]
    
    //---------
    // Функциии
    //---------

});
Ext.define('Seniel.view.EditGeozonesGrid', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.editgeozgrid',
    stateId: 'geozGrid',
    stateful: true,
    title: tr('editgeozoneswnd.geozones'),
    tbar: [
        {
            icon: 'images/ico16_plus_def.png',
            itemId: 'btnAddRecord',
            text: tr('editgeozoneswnd.addgeozone'),
            tooltip: tr('editgeozoneswnd.addgeozone.tooltip'),
            tooltipType: 'title',
            handler: function(btn) {
                var wnd = btn.up('window');
                wnd.startCreating();
            }
        },
        {
            icon: 'images/ico16_edit_def.png',
            itemId: 'btnEditRecord',
            text: tr('editgeozoneswnd.geozinesediting.edit'),
            tooltip: tr('editgeozoneswnd.geozinesediting.editone'),
            tooltipType: 'title',
            disabled: true,
            handler: function(btn) {
                var wnd = btn.up('window');
                var grid = wnd.down('editgeozgrid');
                var rec = grid.getSelectionModel().getSelection()[0];
                wnd.startEditing(rec);
            }
        },
        {
            icon: 'images/ico16_signno.png',
            itemId: 'btnDeleteRecord',
            text: tr('editgeozoneswnd.geozinesremoving.remove'),
            tooltip: tr('editgeozoneswnd.geozinesremoving.remove.tooltip'),
            tooltipType: 'title',
            disabled: true,
            handler: function(btn) {
                var wnd = btn.up('window');
                var grid = wnd.down('grid');
                var rec = grid.getSelectionModel().getSelection()[0];
                wnd.deleteGeozone(rec);
            }
        }
    ],
    rowLines: true,
    border: false,
    padding: false,
    onEsc: Ext.emptyFn,
    layout: 'fit',
    viewConfig: {
        markDirty:false
    },
    store: {
        fields: ['id', 'name', 'ftColor', 'points', 'isOnMap'],
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
    columns: [
        {
            stateId: 'isOnMap',
            menuText: tr('editgeozoneswnd.showonmap.menuText'),
            xtype: 'actioncolumn',
            width: 20,
            menuDisabled: true,
            sealed: true,
            dataIndex: 'isOnMap',
            resizable: false,
            items: [
                {
                    icon: 'images/ico16_globe.png',
                    handler: function(view, rowIndex, colIndex, item, e, rec) {
                        rec.set('isOnMap', true);
                        var onMap = Ext.util.Cookies.get('geozOnMap');
                        if (onMap) {
                            onMap += ',' + rec.get('id');
                            Ext.util.Cookies.set('geozOnMap', onMap, new Date(new Date().getTime() + 1000*60*60*24*365));
                        } else {
                            Ext.util.Cookies.set('geozOnMap', rec.get('id'), new Date(new Date().getTime() + 1000*60*60*24*365));
                        }
                        
                        var viewport = view.up('viewport');
                        for (var i = 0; i < viewport.geozArray.length; i++) {
                            if (viewport.geozArray[i]['id'] === rec.get('id')) {
                                viewport.geozArray[i]['isOnMap'] = true;
                                break;
                            }
                        }
                        var mainmap = viewport.down('mainmap');
                        mainmap.addGeozone(rec, true);
                    },
                    getClass: function(val, metaData, rec){
                        if (rec.get('isOnMap')) {
                            metaData.tdAttr = 'title="'+tr('editgeozoneswnd.stopshowonmap')+'"';
                            return 'x-hide-display';
                        } else {
                            metaData.tdAttr = 'title="'+tr('editgeozoneswnd.doshowonmap')+'"';
                            return 'object-list-button';
                        }
                    }
                },
                {
                    icon: 'images/ico16_globeact.png',
                    handler: function(view, rowIndex, colIndex, item, e, rec) {
                        rec.set('isOnMap', false);
                        var onMap = Ext.util.Cookies.get('geozOnMap');
                        if (onMap) {
                            var arr = onMap.split(',');
                            onMap = '';
                            for (var i = 0; i < arr.length; i++) {
                                if (rec.get('id').toString() !== arr[i]) {
                                    onMap += arr[i] + ' ';
                                }
                            }
                            Ext.util.Cookies.set('geozOnMap', onMap.trim().replace(' ', ','), new Date(new Date().getTime() + 1000*60*60*24*365));
                        }
                        
                        var viewport = view.up('viewport');
                        for (var j = 0; j < viewport.geozArray.length; j++) {
                            if (viewport.geozArray[j]['id'] === rec.get('id')) {
                                viewport.geozArray[j]['isOnMap'] = false;
                                break;
                            }
                        }
                        var mainmap = viewport.down('mainmap');
                        mainmap.removeGeozone(rec);
                    },
                    getClass: function(val, metaData, rec){
                        if (rec.get('isOnMap')) {
                            return 'object-list-button';
                        } else {
                            return 'x-hide-display';
                        }
                    }
                }
            ]
        },
        {
            stateId: 'name',
            text: tr('editgeozoneswnd.name'),
            dataIndex: 'name',
            resizable: true,
            minWidth: 160,
            menuDisabled: true,
            flex: 1
        },
        {
            stateId: 'color',
            text: tr('editgeozoneswnd.color'),
            xtype: 'actioncolumn',
            width: 42,
            menuDisabled: true,
            sealed: true,
            dataIndex: 'ftColor',
            resizable: false,
            align: 'right',
            items: [
                {
                    handler: function(view, rowIndex, colIndex, item, e, rec) {
                        
                        var menu = new Ext.menu.ColorPicker({
                            listeners: {
                                select: function(pkr, color) {
                                    rec.set('ftColor',  ('#' + color));
                                    var geozone = {
                                        id: rec.get('id'),
                                        name: rec.get('name'),
                                        ftColor: rec.get('ftColor'),
                                        points: Ext.JSON.decode(rec.get('points'))
                                    };
                                    if (rec.get('isOnMap')) {
                                        var viewport = view.up('viewport');
                                        var featureArray = viewport.geozOnMap;
                                        if (featureArray[rec.get('id')]) {
                                            featureArray[rec.get('id')].attributes = {
                                                ftcolor: rec.get('ftColor')
                                            };
                                            viewport.down('mainmap').showGeozonesLayer.redraw();
                                        }
                                    }
                                    var map = view.up('window').down('#geozonesMap');
                                    if (map.showGeozonesLayer.features[0]) {
                                        map.showGeozonesLayer.features[0].attributes = {
                                            ftcolor: rec.get('ftColor')
                                        };
                                        map.showGeozonesLayer.redraw();
                                    }
                                    
                                    console.log('Edited geozone = ', geozone);
                                    geozonesData.editGeozone(geozone, function(resp) {
                                        if (resp) {
                                            var viewport = view.up('viewport');
                                            viewport.geozStore.reload();
                                        }
                                    });
                                }
                            }
                        });
                        menu.showAt(e.getXY());
                    },
                    getClass: function(val, metaData, rec){
                        metaData.tdAttr = 'title="'+tr('editgeozoneswnd.changecolor')+'" style="background: url(extjs-4.2.1/resources/ext-theme-gray/images/button/arrow.gif) no-repeat 28px 5px; background-color: '+val+';"';
                    }
                }
            ]
        },
        {
            stateId: 'edit',
            menuText: tr('editgeozoneswnd.geozinesediting'),
            xtype: 'actioncolumn',
            width: 20,
            menuDisabled: true,
            sealed: true,
            sortable: false,
            resizable: false,
            items: [
                {
                    icon: 'images/ico16_edit_def.png',
                    handler: function(view, rowIndex, colIndex, item, e, rec) {
                        var wnd = view.up('window');
                        wnd.startEditing(rec);
                    },
                    getClass: function(val, metaData) {
                        metaData.tdAttr = 'title="'+tr('editgeozoneswnd.geozinesediting.editone')+'"';
                        return 'object-list-button';
                    }
                }
            ]
        },
        {
            stateId: 'delete',
            menuText: tr('editgeozoneswnd.geozinesremoving'),
            xtype: 'actioncolumn',
            width: 20,
            menuDisabled: true,
            sealed: true,
            sortable: false,
            resizable: false,
            items: [
                {
                    icon: 'images/ico16_signno.png',
                    handler: function(view, rowIndex, colIndex, item, e, rec) {
                        var wnd = view.up('window');
                        wnd.deleteGeozone(rec);
                    },
                    getClass: function(val, metaData) {
                        metaData.tdAttr = 'title="'+tr('editgeozoneswnd.geozinesremoving.remove')+'"';
                        return 'object-list-button';
                    }
                }
            ]
        }
    ],
    listeners: {
        itemclick: function(view, rec) {
            var map = view.up('window').down('#geozonesMap');
            map.drawGeozone(rec);
        },
        select: function(selection, rec, index) {
            var tbr = selection.view.up('window').down('grid toolbar');
            tbr.down('#btnEditRecord').enable();
            tbr.down('#btnDeleteRecord').enable();
        },
        deselect: function(selection, rec, index) {
            var tbr = selection.view.up('window').down('grid toolbar');
            tbr.down('#btnEditRecord').disable();
            tbr.down('#btnDeleteRecord').disable();
        }
    }
});


Ext.define('Seniel.view.EditGeozonesWindow', {
    extend: 'Seniel.view.WRWindow',
    alias: 'widget.editgeozwnd',
    stateId: 'geozWnd',
    stateful: true,
    icon: 'images/ico16_geozone.png',
    title: tr('editgeozoneswnd.title'),
    btnConfig: {
        icon: 'images/ico24_geozone.png',
        text: tr('editgeozoneswnd.geozones')
    },
    minWidth: 400,
    minHeight: 320,
    layout: 'border',
    onEsc: function() {
        var tbr = this.down('#geozonesToolbar');
        var map = this.down('#geozonesMap');
        
        if (map.isCreation || map.isEditing) {
            this.isClosing = true;
            tbr.down('#geozHide').btnEl.dom.click();
            return false;
        }
        
        return true;
    },
    items: [
        {
            region: 'west',
            itemId: 'editGeozones',
            xtype: 'editgeozgrid',
            width: '40%',
            collapsible: true,
            split: true
        },
        {
            region: 'center',
            itemId: 'geozonesMap',
            xtype: 'seniel-mapwidget',
            width: '60%',
            drawGeozone: function(rec) {
                if (this.previousGeozone) {
                    this.showGeozonesLayer.removeFeatures([this.previousGeozone]);
                }

                var points = Ext.JSON.decode(rec.get('points')),
                    olpoints = new Array();
                for (var i = 0; i < points.length; i++) {
                    olpoints.push(new OpenLayers.Geometry.Point(points[i].x, points[i].y));
                }
                var linearRing = new OpenLayers.Geometry.LinearRing(olpoints);
                var feature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Polygon([linearRing]), {'ftcolor': rec.get('ftColor')});
                this.showGeozonesLayer.addFeatures([feature]);

                if (!this.isCreation && !this.isEditing) {
                    var bounds = feature.geometry.bounds;
                    this.map.zoomToExtent(bounds);
                }
                this.previousGeozone = feature;
            }
        },
        {
            region: 'north',
            itemId: 'geozonesToolbar',
            xtype: 'toolbar',
            hidden: true,
            width: 39,
            defaults: {
                tooltipType: 'title',
                scale: 'medium',
                width: 33
            },
            items: [
                {
                    xtype: 'textfield',
                    itemId: 'geozName',
                    width: 180,
                    height: 28,
                    margin: '0 0 0 12',
                    emptyText: tr('editgeozoneswnd.name'),
                    allowBlank: false
                },
                {
                    xtype: 'tbspacer',
                    width: 30
                },
                {
                    xtype: 'button',
                    itemId: 'geozPoly',
                    icon: 'images/ico24_draw_poly.png',
                    tooltip: tr('geozonetoolbar.drawpolygon'),
                    toggleGroup: 'GeoZoneDrawCtrls',
                    toggleHandler: function(btn, pressed) {
                        var map = btn.up('window').down('#geozonesMap'),
                            ctrl = map.drawControls.poly;
                        console.log('Polygon control = ', ctrl);
                        if (pressed) {
                            if (map.currentDrawing) {
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
                    toggleGroup: 'GeoZoneDrawCtrls',
                    toggleHandler: function(btn, pressed) {
                        var map = btn.up('window').down('#geozonesMap'),
                            ctrl = map.drawControls.rect;
                        if (pressed) {
                            if (map.currentDrawing) {
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
                    toggleGroup: 'GeoZoneDrawCtrls',
                    toggleHandler: function(btn, pressed) {
                        var map = btn.up('window').down('#geozonesMap'),
                            ctrl = map.drawControls.crcl;
                        if (pressed) {
                            if (map.currentDrawing) {
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
                    toggleGroup: 'GeoZoneDrawCtrls',
                    toggleHandler: function(btn, pressed) {
                        var map = btn.up('window').down('#geozonesMap'),
                            ctrl = map.drawControls.modf;
                        if (pressed) {
                            if (map.currentDrawing && map.currentDrawing.geometry.components[0].components.length === 41) {
                                ctrl.mode = OpenLayers.Control.ModifyFeature.RESIZE;
                            } else {
                                ctrl.mode = OpenLayers.Control.ModifyFeature.RESHAPE;
                            }
                            ctrl.activate();
                        } else {
                            ctrl.deactivate();
                        }
                    }
                },
                {
                    xtype: 'button',
                    itemId: 'geozMove',
                    icon: 'images/ico24_draw_move.png',
                    tooltip: tr('geozonetoolbar.movegeozone'),
                    toggleGroup: 'GeoZoneDrawCtrls',
                    toggleHandler: function(btn, pressed) {
                        var map = btn.up('window').down('#geozonesMap'),
                            ctrl = map.drawControls.modf;
                        if (pressed) {
                            ctrl.mode = OpenLayers.Control.ModifyFeature.DRAG;
                            ctrl.activate();
                        } else {
                            ctrl.deactivate();
                        }
                    }
                },
                {
                    xtype: 'button',
                    itemId: 'geozColor',
                    icon: 'images/ico24_draw_colp.png',
                    tooltip: tr('geozonetoolbar.chosecolor'),
                    width: 48,
                    menu: Ext.create('Ext.menu.ColorPicker', {
                        value: 'FF9900',
                        listeners: {
                            select: function(pkr, color) {
                                var btn = pkr.up('button'),
                                    map = btn.up('window').down('#geozonesMap');
                                btn.el.dom.childNodes[0].childNodes[0].style.backgroundColor = '#' + color;
                                btn.curColor = '#' + color;

                                if (map.currentDrawing) {
                                    map.currentDrawing.attributes = {
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
                    width: 30
                },
                {
                    xtype: 'button',
                    itemId: 'geozSave',
                    icon: 'images/ico24_okcrc_def.png',
                    tooltip: tr('geozonetoolbar.savegeozone'),
                    handler: function(btn) {
                        var wnd = btn.up('window');
                        var map = wnd.down('#geozonesMap');

                        if (map.isEditing) {
                            wnd.saveModifiedGeozone();
                        }
                        
                        if (map.isCreation) {
                            wnd.saveNewGeozone();
                        }
                    }
                },
                {
                    xtype: 'button',
                    itemId: 'geozHide',
                    icon: 'images/ico24_cancel_def.png',
                    tooltip: tr('geozonetoolbar.closepanel'),
                    handler: function(btn) {
                        var wnd = btn.up('window'),
                            map = btn.up('window').down('#geozonesMap');

                        if (map.isEditing && map.editRecord) {
                            wnd.cancelEditing();
                        }

                        if (map.isCreation) {
                            wnd.cancelCreating();
                        }
                    }
                }
            ]
        }
    ],
    initComponent: function() {
        this.callParent(arguments);
        
        this.on('boxready', function(wnd) {
            var viewport = wnd.up('viewport'),
                grid = wnd.down('editgeozgrid');
            
            grid.getStore().add(viewport.geozArray);
        });
        this.on('beforeclose', function(wnd) {
            var tbr = this.down('#geozonesToolbar');
            var map = this.down('#geozonesMap');

            if (map.isCreation || map.isEditing) {
                this.isClosing = true;
                tbr.down('#geozHide').btnEl.dom.click();
                return false;
            }

            return true;
        });
    },

    //---------
    // Функциии
    //---------
    startCreating: function() {
        var tbr = this.down('#geozonesToolbar');
        var map = this.down('#geozonesMap');
        var grid = this.down('editgeozgrid');
        
        if (!map.isEditing && !map.isCreation) {
            map.isCreation = true;
            map.showGeozonesLayer.setVisibility(false);
            grid.hide();
            tbr.show();
        } else {
            Ext.MessageBox.show({
                title: tr('editgeozoneswnd.addgeozone.error'),
                msg: tr('editgeozoneswnd.addgeozone.error.msg'),
                icon: Ext.MessageBox.WARNING,
                buttons: Ext.Msg.OK
            });
        }
    },
    cancelCreating: function() {
        var wnd = this;
        var tbr = wnd.down('#geozonesToolbar');
        var map = wnd.down('#geozonesMap');
        
        if(map.drawGeozonesLayer.features.length > 0) {
            Ext.MessageBox.show({
                title: tr('geozonetoolbar.geozonesaving'),
                msg: tr('geozonetoolbar.geozonesaving.surenew'),
                icon: Ext.MessageBox.QUESTION,
                buttons: Ext.Msg.YESNOCANCEL,
                fn: function(a) {
                    if (a === 'no') {
                        wnd.restoreAfterCreation();
                    } else if (a === 'yes') {
                        tbr.down('#geozSave').btnEl.dom.click();
                    }
                }
            });
        } else {
            wnd.restoreAfterCreation();
        }
    },
    restoreAfterCreation: function() {
        var tbr = this.down('#geozonesToolbar');
        var map = this.down('#geozonesMap');
        var grid = this.down('editgeozgrid');
        
        map.isCreation = false;
        map.currentDrawing = null;
        map.drawGeozonesLayer.removeAllFeatures();
        map.showGeozonesLayer.setVisibility(true);
        if (this.isClosing) {
            this.close();
            return false;
        }

        tbr.down('#geozName').setValue('');
        tbr.down('#geozPoly').toggle(false);
        tbr.down('#geozRect').toggle(false);
        tbr.down('#geozCrcl').toggle(false);
        tbr.down('#geozResh').toggle(false);
        tbr.down('#geozMove').toggle(false);
        tbr.hide();
        
        grid.show();
    },
    startEditing: function(rec) {
        var wnd = this;
        var tbr = wnd.down('#geozonesToolbar');
        var map = wnd.down('#geozonesMap');
        var grid = wnd.down('editgeozgrid');

        if (!map.isEditing && !map.isCreation) {
            map.isEditing = true;
            map.showGeozonesLayer.setVisibility(false);
            grid.hide();

            tbr.down('#geozName').setValue(rec.get('name'));
            tbr.down('#geozPoly').hide();
            tbr.down('#geozRect').hide();
            tbr.down('#geozCrcl').hide();
            tbr.show();

            map.editRecord = rec;
            var btncolor = tbr.down('#geozColor');
            if (btncolor) {
                btncolor.el.dom.childNodes[0].childNodes[0].style.backgroundColor = rec.get('ftColor');
                btncolor.curColor = rec.get('ftColor');
            }
            var points = Ext.JSON.decode(rec.get('points')),
                olpoints = [];
            for (var i = 0; i < points.length; i++) {
                olpoints.push(new OpenLayers.Geometry.Point(points[i].x, points[i].y));
            }
            var linearRing = new OpenLayers.Geometry.LinearRing(olpoints);
            map.currentDrawing = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Polygon([linearRing]), {'ftcolor': rec.get('ftColor')});
            map.drawGeozonesLayer.addFeatures([map.currentDrawing]);
            var bounds = map.currentDrawing.geometry.bounds;
            map.map.zoomToExtent(bounds);
        } else {
            var action = (map.isEditing)?
                tr('editgeozoneswnd.geozinesediting.completeeditcurrent'):
                tr('editgeozoneswnd.geozinesediting.completeaddnew');
            Ext.MessageBox.show({
                title: tr('editgeozoneswnd.geozinesediting.cantedit'),
                msg: action + ' «' + rec.get('name') + '».',
                icon: Ext.MessageBox.WARNING,
                buttons: Ext.Msg.OK
            });
        }
    },
    cancelEditing: function() {
        var wnd = this;
        var tbr = wnd.down('#geozonesToolbar');
        var map = wnd.down('#geozonesMap');
        var isModified = false;
        
        if (map.editRecord.get('ftColor') !== tbr.down('#geozColor').curColor) {
            isModified = true;
        }
        var points = map.currentDrawing.geometry.components[0].components,
            loadedPoints = Ext.JSON.decode(map.editRecord.get('points'));
        if (loadedPoints.length !== points.length) {
            isModified = true;
        } else {
            for (var i = 0; i < points.length; i++) {
                if ((points[i].x !== loadedPoints[i].x) || (points[i].y !== loadedPoints[i].y)) {
                    isModified = true;
                    break;
                }
            }
        }

        if (isModified) {
            Ext.MessageBox.show({
                title: tr('geozonetoolbar.geozonesaving'),
                msg: tr('geozonetoolbar.geozonesaving.sure'),
                icon: Ext.MessageBox.QUESTION,
                buttons: Ext.Msg.YESNOCANCEL,
                fn: function(a) {
                    if (a === 'no') {
                        wnd.restoreAfterEditing();
                    } else if (a === 'yes') {
                        tbr.down('#geozSave').btnEl.dom.click();
                    }
                }
            });
            return false;
        } else {
            wnd.restoreAfterEditing();
        }
    },
    restoreAfterEditing: function() {
        var tbr = this.down('#geozonesToolbar');
        var map = this.down('#geozonesMap');
        var grid = this.down('editgeozgrid');
        
        map.isEditing = false;
        map.currentDrawing = null;
        map.editRecord = null;
        map.drawGeozonesLayer.removeAllFeatures();
        map.showGeozonesLayer.setVisibility(true);
        if (this.isClosing) {
            this.close();
            return false;
        }

        tbr.down('#geozName').setValue('');
        tbr.down('#geozPoly').toggle(false);
        tbr.down('#geozPoly').show();
        tbr.down('#geozRect').toggle(false);
        tbr.down('#geozRect').show();
        tbr.down('#geozCrcl').toggle(false);
        tbr.down('#geozCrcl').show();
        tbr.down('#geozResh').toggle(false);
        tbr.down('#geozMove').toggle(false);
        tbr.hide();
        
        grid.show();
    },
    saveNewGeozone: function(geozone) {
        var wnd = this;
        var tbr = wnd.down('#geozonesToolbar');
        var map = wnd.down('#geozonesMap');
        
        if (map.drawGeozonesLayer.features.length === 0) {
            Ext.MessageBox.show({
                title: tr('geozonetoolbar.savegeozone.nogeozone'),
                msg: tr('geozonetoolbar.savegeozone.nogeozone.msg'),
                icon: Ext.MessageBox.WARNING,
                buttons: Ext.Msg.OK
            });
            return false;
        }
        
        if (!tbr.down('#geozName').getValue()) {
            Ext.MessageBox.show({
                title: tr('geozonetoolbar.savegeozone.noname'),
                msg: tr('geozonetoolbar.savegeozone.noname.msg'),
                icon: Ext.MessageBox.WARNING,
                buttons: Ext.Msg.OK
            });
            return false;
        }
        
        var geozone = {
            name: tbr.down('#geozName').getValue(),
            ftColor: map.currentDrawing.attributes.ftcolor,
            points: []
        };
        Ext.Array.each(map.currentDrawing.geometry.components[0].components, function(a) {
            geozone.points.push({x: a.x, y: a.y});
        });
        console.log('New geozone = ', geozone);
        geozonesData.addGeozone(geozone, function(resp) {
            if (resp) {
                var grid = wnd.down('editgeozgrid');
                grid.updateStore = true;
                var viewport = wnd.up('viewport');
                viewport.geozStore.reload();
                wnd.restoreAfterCreation();
            } else {
                Ext.MessageBox.show({
                    title: tr('geozonetoolbar.savegeozone.error'),
                    msg: tr('geozonetoolbar.savegeozone.error.msg'),
                    icon: Ext.MessageBox.ERROR,
                    buttons: Ext.Msg.OK
                });
            }
        });
    },
    saveModifiedGeozone: function() {
        var wnd = this;
        var tbr = wnd.down('#geozonesToolbar');
        var map = wnd.down('#geozonesMap');
        
        if (map.editRecord) {
            var geozone = {
                id: map.editRecord.get('id'),
                name: (tbr.down('#geozName').getValue())?(tbr.down('#geozName').getValue()):(map.editRecord.get('name')),
                ftColor: tbr.down('#geozColor').curColor,
                points: []
            };
            if (map.drawGeozonesLayer.features.length === 0) {
                geozone.points = Ext.JSON.decode(map.editRecord.get('points'));
            } else {
                Ext.Array.each(map.currentDrawing.geometry.components[0].components, function(a) {
                    geozone.points.push({x: a.x, y: a.y});
                });
            }

            console.log('Edited geozone = ', geozone);
            geozonesData.editGeozone(geozone, function(resp) {
                if (resp) {
                    var viewport = wnd.up('viewport');
                    viewport.geozStore.reload();
                    map.editRecord.set('name', geozone.name);
                    map.editRecord.set('ftColor', geozone.ftColor);
                    map.editRecord.set('points', Ext.JSON.encode(geozone.points, null, 4));
                    map.drawGeozone(map.editRecord);
                    if (map.editRecord.get('isOnMap')) {
                        var mainmap = viewport.down('mainmap');
                        mainmap.removeGeozone(map.editRecord);
                        mainmap.addGeozone(map.editRecord);
                    }
                    wnd.restoreAfterEditing();
                } else {
                    Ext.MessageBox.show({
                        title: tr('geozonetoolbar.savegeozone.error'),
                        msg: tr('geozonetoolbar.savegeozone.error.msg'),
                        icon: Ext.MessageBox.ERROR,
                        buttons: Ext.Msg.OK
                    });
                }
            });
        } else {
            Ext.MessageBox.show({
                title: tr('geozonetoolbar.editgeozone.error'),
                msg: tr('geozonetoolbar.editgeozone.error.msg'),
                icon: Ext.MessageBox.ERROR,
                buttons: Ext.Msg.OK
            });
            wnd.restoreAfterEditing();
        }
        return false;
    },
    deleteGeozone: function(rec) {
        var wnd = this;
        var grid = wnd.down('editgeozgrid');
        
        Ext.MessageBox.show({
            title: tr('editgeozoneswnd.geozinesremoving.removegeozone'),
            msg: tr('editgeozoneswnd.geozinesremoving.sure'),
            icon: Ext.MessageBox.QUESTION,
            buttons: Ext.Msg.YESNO,
            fn: function(a) {
                if (a === 'yes') {
                    var viewport = wnd.up('viewport');
                    if (rec.get('isOnMap')) {
                        var onMap = Ext.util.Cookies.get('geozOnMap');
                        if (onMap) {
                            var arr = onMap.split(',');
                            onMap = '';
                            for (var i = 0; i < arr.length; i++) {
                                if (rec.get('id').toString() !== arr[i]) {
                                    onMap += arr[i] + ' ';
                                }
                            }
                            Ext.util.Cookies.set('geozOnMap', onMap.trim().replace(' ', ','), new Date(new Date().getTime() + 1000*60*60*24*365));
                        }
                        var mainmap = viewport.down('mainmap');
                        mainmap.removeGeozone(rec);
                    }
                    
                    geozonesData.delGeozone(rec.get('id'));
                    var gzmap = wnd.down('#geozonesMap');
                    if (gzmap.previousGeozone) {
                        gzmap.showGeozonesLayer.removeFeatures([gzmap.previousGeozone]);
                        gzmap.previousGeozone = null;
                    }
                    grid.getSelectionModel().deselect(rec);
                    grid.getStore().remove(rec);
                    viewport.geozStore.reload();
                }
            }
        });
    }
});
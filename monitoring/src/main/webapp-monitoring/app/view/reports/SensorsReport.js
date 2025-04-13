Ext.define('Seniel.view.reports.SensorsReport', {
    extend:'Ext.panel.Panel',
    requires: [
        'Seniel.view.WRRadioGroup',
        'Seniel.view.LimitedCheckboxGroup',
        'Seniel.view.DygraphPanel'
    ],
    alias: 'widget.sensorsreport',
    lbar: [
        {
            icon: 'images/ico16_zoomout.png',
            iconAlign: 'top',
            text: ' ',
            height: 22,
            width: 32,
            tooltip: tr('fuelgraphreport.returnscale'),
            tooltipType: 'title',
            scale: 'small',
            handler: function(btn) {
                var sensorsGraph = btn.up('reportwnd').down('sensorsreport').down('dygraphpanel');
                if (sensorsGraph && sensorsGraph.dygraph) {
                    sensorsGraph.dygraph.resetZoom();
                }
            }
        },
        ' ',
        {
            icon: 'images/ico16_zoomin.png',
            iconAlign: 'top',
            text: ' ',
            height: 22,
            width: 32,
            tooltip: tr('fuelgraphreport.xzoom'),
            tooltipType: 'title',
            scale: 'small',
            pressed: true,
            enableToggle: true,
            toggleGroup: 'dygraphInteractionSensors',
            allowDepress: false,
            toggleHandler: function(btn, pressed) {
                var sensorsGraph = btn.up('reportwnd').down('sensorsreport').down('dygraphpanel');
                if (sensorsGraph && sensorsGraph.dygraph) {
                    if (pressed) {
                        sensorsGraph.dygraph.interactionMode = 'zoom';
                    }
                }
            }
        },
        {
            icon: 'images/ico16_draw_move.png',
            iconAlign: 'top',
            text: ' ',
            height: 22,
            width: 32,
            tooltip: tr('fuelgraphreport.xmoving'),
            tooltipType: 'title',
            scale: 'small',
            enableToggle: true,
            toggleGroup: 'dygraphInteractionSensors',
            allowDepress: false,
            toggleHandler: function(btn, pressed) {
                var sensorsGraph = btn.up('reportwnd').down('sensorsreport').down('dygraphpanel');
                if (sensorsGraph && sensorsGraph.dygraph) {
                    if (pressed) {
                        sensorsGraph.dygraph.interactionMode = 'pan';
                    }
                }
            }
        },
        {
            icon: 'images/ico16_printer.png',
            iconAlign: 'top',
            text: ' ',
            height: 22,
            width: 32,
            tooltip: tr('basereport.export.chart'),
            tooltipType: 'title',
            scale: 'small',
            handler: function(btn) {
                var rep = btn.up('sensorsreport');
                var grid = btn.up('window').down('movstatsgrid');
                var params = grid.getStore().getProxy().extraParams;
                var url = './EDS/xychart/sensor.png?uid=' + params.selected + '&from=' + encodeURIComponent(new Date(params.from)) + '&to='
                    + encodeURIComponent(new Date(params.to)) + '&sensorName=' + rep.curSensors.join('&sensorName=');
                window.open(url);
            }
        }
    ],
    layout: 'border',
    items: [
        {
            region: 'west',
            xtype: 'panel',
            width: '25%',
            border: false,
            split: true,
            overflowX: 'auto',
            overflowY: 'auto'
        },
        {
            region: 'center',
            xtype: 'dygraphpanel',
            width: '75%',
            border: false,
            header: false,
            axisLabels: ['Дата', 'Значение'], // TODO translate
            bodyBorder: false,
            listeners: {
                boxready: function() {
                    this.addEvents('graphClick');

                    this.on('graphClick', function (obj, x, row) {
                        if (!this.up('reportwnd').down('pathgrid').gridViewed) {
                            this.up('reportwnd').down('tabpanel').setActiveTab('movement');
                            this.up('reportwnd').down('tabpanel').setActiveTab('sensors');
                        } 
                        this.up('reportwnd').down('pathgrid').selectRowInGrid(row);
                    });
                }
            }
        }
    ],

    makeDygraphConfig: function (labels) {
        var c = new DygraphDataConfig(labels);
        if(labels.length < 2 || labels.length > 3)
            throw new Error("Invalid labels count " + labels.length);
        c.bindToAxis(labels[1], 'y');
        if(labels.length == 3) {
            c.bindToAxis(labels[2], 'y2');
        }
        return c;
    },

    refreshReport: function(settings) {
        console.log("sensors refreshReport");
        var self = this;
        var defSensor = 'pwr_ext',
            rg, cb, rgHidden;

        mapObjects.getSensorNames(settings.selected, function(resp) {
            if (resp && resp.length > 0) {
                var rgItems = new Array(); // основные сенсоры
                var rgAdditional = new Array(); // Дополнительные, не показываются по умолчанию
                var sensor = Ext.util.Cookies.get('reportSensor'); // TODO понять что за, сенсор который был включен ранее?
                console.log('Sensor cookie = ', sensor);
                
                var checked = false;
                for (var i = 0; i < resp.length; i++) {
                    if (resp[i].show) {
                        if (sensor && sensor === resp[i].code) {
                            checked = true;
                            defSensor = resp[i].code; // Сенсор из куки включается с галочкой
                            rgItems.push({'boxLabel': resp[i].name, 'inputValue': resp[i].code, 'checked': true});
                        } else {
                            rgItems.push({'boxLabel': resp[i].name, 'inputValue': resp[i].code});
                        }
                    } else {
                        rgAdditional.push({'boxLabel': resp[i].name, 'inputValue': resp[i].code});
                    }
                }
                
                if (!checked) {
                    rgItems[0].checked = true; // Или включаем первый (проверка границ? Если все сенсоры additional, будет нехорошо)
                    defSensor = resp[0].code; // defSensor хранит код выбранного сенсора (либо с куки, либо просто первого)
                }

                console.log("Before rg init");
                rg = Ext.create('Seniel.view.LimitedCheckboxGroup', {
                    columns: 1,
                    defaults: {
                        labelAlign: 'right',
                        labelPad: 10
                    },
                    initItemsConfig: rgItems,
                    padding: '4 0 4 4',
                    checkedLimit: 2,
                    listeners: {
                        change: function(rg, newVal) {
                            var sel = rg.getRealValues();
                            console.log("sel = " + sel);
                            if (rg.next() && rg.next().getValue()) { // Проверка чекбокса с additonalом? Видимо понадобится изменение и подгонка лимитов. А если пусто?
                                rg.next().next().setLimit(2 - sel.length); // Лезем в радиогруппу и зачищаем её
                            }
                            if(sel.length > 0) {
                                Ext.util.Cookies.set('reportSensor', sel[0], Ext.Date.add(new Date(), Ext.Date.MONTH, 1));
                            }

                            var selectedSensors = sel;
                            if(rg.next() && rg.next().next())
                                selectedSensors = Ext.Array.merge(sel, rg.next().next().getRealValues());
                            if(selectedSensors.length > 0) {
                                self.curSensors = selectedSensors;
                                // Выставляем куки (на месяц?)

                                // Показываем
                                var sensorParams = '';
                                var sep = '';
                                for(var i = 0; i < selectedSensors.length; ++i) {
                                    sensorParams += sep + 'sensor=' + selectedSensors[i];
                                    sep = '&';
                                }
                                var labels = Ext.Array.merge(['Дата'], selectedSensors);
                                self.down('dygraphpanel').setDataAndOptions(
                                    "pathdata?data=sensors&" + sensorParams  + "&selected=" +
                                    settings.selected + "&from=" + encodeURIComponent(settings.dates.from.toString()) + "&to=" + encodeURIComponent(settings.dates.to.toString())
                                    , self.makeDygraphConfig(labels));
                            }




                        }
                    }
                });

                rg.checkLimit();

                if (rgAdditional.length > 0) {
                    cb = Ext.create('Ext.form.field.Checkbox', {
                        boxLabel: 'Дополнительные датчики',
                        padding: '0 8 0 8',
                        listeners: {
                            change: function(cb, newVal) {
                                if (newVal) {
                                    cb.next().show();
                                    cb.next().setLimit(2 - cb.prev().getRealValues().length);
                                } else { // при скрытии обнуляем все галочки
                                    cb.next().hide();
                                    cb.next().setAllToFalse();
                                    //cb.prev().reset();
                                }
                            }
                        }
                    });
                    rgHidden = Ext.create('Seniel.view.LimitedCheckboxGroup', {
                        columns: 1,
                        defaults: {
                            labelAlign: 'right',
                            labelPad: 10
                        },
                        initItemsConfig: rgAdditional,
                        checkedLimit: 1,
                        hidden: true,
                        padding: '2 0 4 4',
                        listeners: {
                            change: function(rg, newVal) {
                                var sel = rg.getRealValues();
                                console.log("sel = " + sel);
                                rg.prev().prev().setLimit(2 - sel.length); // Лезем в радиогруппу и зачищаем её
                                var selectedSensors = Ext.Array.merge(sel, rg.prev().prev().getRealValues());
                                if (selectedSensors.length > 0) {
                                    self.curSensors = selectedSensors;

                                    // Показываем

                                    var sensorParams = '';
                                    var sep = '';
                                    for (var i = 0; i < selectedSensors.length; ++i) {
                                        sensorParams += sep + 'sensor=' + selectedSensors[i];
                                        sep = '&';
                                    }
                                    var labels = Ext.Array.merge(['Дата'], selectedSensors);
                                    self.down('dygraphpanel').setDataAndOptions(
                                        "pathdata?data=sensors&" + sensorParams + "&selected=" +
                                        settings.selected + "&from=" + encodeURIComponent(settings.dates.from.toString()) + "&to=" + encodeURIComponent(settings.dates.to.toString())
                                        , self.makeDygraphConfig(labels));
                                }
                            }
                            // },
                            // change2: function(rg, newVal) {
                            //     if (rg.getRealValue()) {
                            //         rg.prev().prev().setAllToFalse(); // Взаимодействие с верхней панелью, куки не заполняюттся
                            //         self.curSensor = rg.getRealValue();
                            //         self.down('dygraphpanel').setData("pathdata?data=sensors&sensor=" + encodeURI(rg.getRealValue()) + "&selected=" +
                            //             settings.selected + "&from=" + encodeURIComponent(settings.dates.from.toString()) + "&to=" + encodeURIComponent(settings.dates.to.toString()));
                            //     }
                            // }
                        }
                    });
                }
            } else {
                rg = Ext.create('Seniel.view.WRRadioGroup', { // При пустом запросе имеем единственный пункт
                    columns: 1,
                    defaults: {
                        labelAlign: 'right',
                        labelPad: 10
                    },
                    initItemsConfig: [
                        {boxLabel: 'pwr_ext', inputValue: 'pwr_ext', checked: true}
                    ],
                    padding: 4
                });
            }

            // Размещаем и показываем в первый раз (и при обновлении)

            var labels = ['Дата', defSensor];
            self.curSensors = [defSensor];
            self.down('dygraphpanel').setDataAndOptions("pathdata?data=sensors&sensor=" + encodeURI(defSensor) + "&selected=" +
                settings.selected + "&from=" + encodeURIComponent(settings.dates.from.toString()) + "&to=" + encodeURIComponent(settings.dates.to.toString()), self.makeDygraphConfig(labels));

            self.down('panel[region=west]').removeAll();
            if (rgHidden) {
                self.down('panel[region=west]').add([rg, cb, rgHidden]);
            } else {
                self.down('panel[region=west]').add([rg]);
            }
        });
    }
});
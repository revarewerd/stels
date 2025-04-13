/**
 * Created by IVAN on 16.10.2014.
 */
Ext.define('Billing.view.object.sensors.SensorParamsWindow', {
    extend: 'Ext.window.Window',
    requires: [
        //'Seniel.view.DygraphPanel'
    ],
    alias: 'widget.sensorparamswnd',
    stateId: 'sensorParamsWnd',
    stateful: true,
    icon: 'images/ico16_bell.png',
    title: 'Новый датчик',//tr('settingssensors.newsensor'),
    btnConfig: {
        icon: 'images/ico24_bell.png',
        text: 'Новый датчик'//tr('settingssensors.newsensor')
    },
    maximizable: false,
    minWidth: 600,
    minHeight: 400,
    width: 600,
    height: 420,
    layout: 'fit',
    items: [
        {
            xtype: 'tabpanel',
            padding: 0,
            defaults: {
                bodyPadding: 0,
                padding: 0
            },
            items: [
                {
                    xtype: 'form',
                    title:'Параметры датчика',//tr('settingssensors.sensorparams'),
                    defaults: {
                        xtype: 'textfield',
                        anchor: '100%',
                        labelWidth: 220,
                        labelPad: 10,
                        padding: '4 8 4 8'
                    },
                    layout: 'anchor',
                    border: false,
                    items: [
                        {
                            xtype: 'combo',
                            name: 'type',
                            fieldLabel: 'Тип датчика',//tr('settingssensors.sensortype'),
                            store: {
                                fields: ['id', 'type'],
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
                            allowBlank: false,
                            editable: false,
                            queryMode: 'local',
                            valueField: 'id',
                            displayField: 'type',
                            padding: '8 8 4 8',
                            listeners: {
                                boxready: function() {
                                    var wnd = this.up('window');
                                    var self = this;

                                    sensorsList.getCommonTypes(function(resp) {
                                        if (resp && resp.length > 0) {
                                            self.getStore().add(resp);

                                            if (wnd.editRecord) {
                                                self.setValue(wnd.editRecord.get('type'));
                                            }

                                            self.sensorsData = [];
                                            for (var i = 0; i < resp.length; i++) {
                                                self.sensorsData[resp[i].id] = {"unit": resp[i].unit, "type": resp[i].type};
                                            }
                                        }
                                    });
                                },
                                change: function(cb, newVal, oldVal) {
                                    if (newVal) {
                                        var nameField = cb.up('form').down('[name="name"]');
                                        var unitField = cb.up('form').down('[name="unit"]');

                                        if (cb.sensorsData && !nameField.getValue()) {
                                            nameField.setValue(cb.sensorsData[newVal].type);
                                        }
                                        if (cb.sensorsData) {
                                            unitField.setValue(cb.sensorsData[newVal].unit);
                                        }
                                    }
                                }
                            }
                        },
                        {
                            name: 'name',
                            fieldLabel: 'Название',//tr('settingssensors.grid.name'),
                            allowBlank: false
                        },
                        {
                            xtype: 'combo',
                            name: 'paramName',
                            fieldLabel: 'Параметр',//tr('settingssensors.grid.paramName'),
                            store: {
                                fields: ['param'],
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
                            allowBlank: false,
                            editable: false,
                            queryMode: 'local',
                            valueField: 'param',
                            displayField: 'param',
                            listeners: {
                                boxready: function() {
                                    var wnd = this.up('window');
                                    var uid = wnd.objUid;
                                    var self = this;

                                    sensorsList.getObjectSensorsCodenames(uid, function(resp) {
                                        if (resp && resp.length) {
                                            self.getStore().add(resp);
                                            if (wnd.editRecord) {
                                                self.setValue(wnd.editRecord.get('paramName'));
                                            }
                                        }
                                    });
                                }
                            }
                        },
                        {
                            name: 'unit',
                            fieldLabel:'Единицы измерения'// tr('settingssensors.grid.unit')
                        },
                        {
                            name: 'minValue',
                            xtype: 'numberfield',
                            hideTrigger: true,
                            fieldLabel:'Нижняя граница'//tr('settingssensors.grid.lowbound')
                        },
                        {
                            name: 'maxValue',
                            xtype: 'numberfield',
                            hideTrigger: true,
                            fieldLabel:'Верхняя граница'// tr('settingssensors.grid.upbound')
                        },
                        {
                            name: 'ratio',
                            xtype: 'numberfield',
                            fieldLabel:'Коэффициент', //tr('settingssensors.grid.ratio'),
                            value: 1
                        },
                        {
                            name: 'comment',
                            fieldLabel:'Комментарий'//tr('settingssensors.grid.comment')
                        },
                        {
                            xtype: 'checkbox',
                            name: 'dataTable',
                            fieldLabel:'Использовать таблицу значений',// tr('settingssensors.grid.datatable.use'),
                            inputValue: true,
                            padding: '2 8 4 8',
                            listeners: {
                                change: function(cb, newVal, oldVal) {
                                    if (newVal) {
                                        cb.up('tabpanel').down('sensorsvaluesgrid').enable();
                                        //cb.up('tabpanel').down('dygraphpanel').enable();
                                    } else {
                                        cb.up('tabpanel').down('sensorsvaluesgrid').disable();
                                        //cb.up('tabpanel').down('dygraphpanel').disable();
                                    }
                                }
                            }
                        },
                        {
                            xtype: 'checkbox',
                            name: 'showInInfo',
                            fieldLabel: 'Показывать в информации по объекту',//tr('settingssensors.grid.showininfo'),
                            inputValue: true,
                            checked: true,
                            padding: '2 8 4 8'
                        }
                    ]
                },
                {
                    xtype: 'sensorsvaluesgrid',
                    title: 'Таблица значений',//tr('settingssensors.datatable'),
                    disabled: true
                }//,
//                {
//                    xtype: 'dygraphpanel',
//                    title: tr('settingssensors.calcgraph'),
//                    disabled: true,
//                    layout: 'fit',
//                    axisLabels: ['X', 'Y'],
//                    units: {
//                        y: ''
//                    },
//                    lineColors: ['#008000'],
//                    listeners: {
//                        beforerender: function() {
//                            var grid = this.up('tabpanel').down('sensorsvaluesgrid');
//                            var data = [];
//                            grid.getStore().each(function(rec) {
//                                if (rec.get('x') && rec.get('y') && !isNaN(rec.get('x')) && !isNaN(rec.get('y'))) {
//                                    data.push([Number(rec.get('x')), Number(rec.get('y'))]);
//                                }
//                            });
//                            data.sort(function(a, b) {return a[0] - b[0];});
//                            console.log('Data array = ', data);
//                            this.setData(data);
//                        },
//                        activate: function() {
//                            if (this.dygraph && !this.xValueFormatUpdated) {
//                                this.dygraph.updateOptions({
//                                    xRangePad: 20,
//                                    yRangePad: 20,
//                                    axes: {
//                                        x: {
//                                            valueFormatter: function(val) {
//                                                return val;
//                                            },
//                                            axisLabelFormatter: function(val) {
//                                                return val;
//                                            }
//                                        },
//                                        y: {
//                                            valueFormatter: function(val) {
//                                                return val;
//                                            },
//                                            axisLabelFormatter: function(val) {
//                                                return val;
//                                            }
//                                        }
//                                    }
//                                });
//                                this.xValueFormatUpdated = true;
//                            }
//                            var grid = this.up('tabpanel').down('sensorsvaluesgrid');
//                            var data = [];
//                            grid.getStore().each(function(rec) {
//                                if (rec.get('x') && rec.get('y') && !isNaN(rec.get('x')) && !isNaN(rec.get('y'))) {
//                                    data.push([Number(rec.get('x')), Number(rec.get('y'))]);
//                                }
//                            });
//                            data.sort(function(a, b) {return a[0] - b[0];});
//                            console.log('Data array = ', data);
//                            this.setData(data);
//                        }
//                    }
//                }
            ]
        }
    ],
    bbar: [
        '->',
        {
            xtype: 'button',
            itemId: 'btnAddSensor',
            text: 'OK',//tr('main.ok'),
            icon: 'images/ico16_okcrc.png',
            handler: function(btn) {
                var wnd = btn.up('window'),
                    form = wnd.down('form');

                if (form.isValid()) {
                    var data = form.getValues();
                    console.log("wnd",wnd)
                    if (wnd.linkedGrid) {
                        if (!data.showInInfo) {
                            data.showInInfo = false;
                        }

                        if (data.dataTable) {
                            var sVGrid = wnd.down('sensorsvaluesgrid');
                            if (sVGrid && !sVGrid.getStore().getCount()) {
                                wnd.showMessage('Добавление датчика','Необходимо заполнить таблицу значений');
                                //tr('settingssensors.sensoradding'),//tr('settingssensors.sensoradding.msg'),
                                return false;
                            }

                            data.dataTable = [];
                            sVGrid.getStore().each(function(rec) {
                                if (rec.get('x') && rec.get('y') && !isNaN(rec.get('x')) && !isNaN(rec.get('y'))) {
                                    data.dataTable.push({x: Number(rec.get('x')), y: Number(rec.get('y'))});
                                }
                            });
                            data.dataTable.sort(function(a, b) {return a.x - b.x;});
                        } else {
                            data.dataTable = false;
                        }
                        wnd.linkedGrid.getStore().add(data);
                        console.log('New sensor data = ', data);

                        var btn = wnd.linkedGrid.up('window').down('toolbar #btnApplySettings');
                        if (btn && btn.isDisabled) {
                            btn.enable();
                        }
                    } else {
                        wnd.showMessage('Добавление датчика','Не удалось добавить новый датчик');
                        //tr('settingssensors.sensoradding'),//tr('settingssensors.sensoradding.failed'),
                    }
                } else {
                    wnd.showMessage('Добавление датчика','Заполнены не все обязательные поля формы');
                    //tr('settingssensors.sensoradding'),//tr('settingssensors.sensoradding.notallfields'),
                    return false;
                }

                wnd.close();
            }
        },
        {
            xtype: 'button',
            itemId: 'btnUpdSensor',
            text: 'OK',//tr('main.ok'),
            icon: 'images/ico16_okcrc.png',
            hidden: true,
            handler: function(btn) {
                var wnd = btn.up('window'),
                    form = wnd.down('form');

                if (form.isValid()) {
                    var data = form.getValues();
                    if (wnd.editRecord) {
                        if (!data.showInInfo) {
                            data.showInInfo = false;
                        }

                        if (data.dataTable) {
                            var sVGrid = wnd.down('sensorsvaluesgrid');
                            if (sVGrid && !sVGrid.getStore().getCount()) {
                                wnd.showMessage('Редактирование датчика','Необходимо заполнить таблицу значений');
                                //tr('settingssensors.sensorediting'),//tr('settingssensors.sensoradding.msg'),
                                return false;
                            }

                            data.dataTable = [];
                            sVGrid.getStore().each(function(rec) {
                                if (rec.get('x') && rec.get('y') && !isNaN(rec.get('x')) && !isNaN(rec.get('y'))) {
                                    data.dataTable.push({x: Number(rec.get('x')), y: Number(rec.get('y'))});
                                }
                            });
                            data.dataTable.sort(function(a, b) {return a.x - b.x;});
                        } else {
                            data.dataTable = false;
                        }

                        wnd.editRecord.set(data);

                        var btn = wnd.linkedGrid.up('window').down('toolbar #btnApplySettings');
                        if (btn && btn.isDisabled) {
                            btn.enable();
                        }
                    } else {
                        wnd.showMessage('Редактирование датчика','Не удалось обновить характеристики выбранного датчика');
                    }
                } else {
                    wnd.showMessage('Редактирование датчика','Заполнены не все обязательные поля формы');
                    return false;
                }

                wnd.close();
            }
        },
        {
            xtype: 'button',
            text:'Отмена',//tr('main.cancel'),
            icon: 'images/ico16_cancel.png',
            handler: function(btn) {
                var wnd = btn.up('window');
                wnd.close();
            }
        }
    ],
    initComponent: function() {
        this.callParent(arguments);

        this.on('boxready', function(wnd) {
            if (wnd.editRecord) {
                wnd.loadRecordData();
            }
        });
    },

    //---------
    // Функциии
    //---------
    loadRecordData: function() {
        var wnd = this,
            rec = wnd.editRecord;

        wnd.down('[name="name"]').setValue(rec.get('name'));
        wnd.down('[name="unit"]').setValue(rec.get('unit'));
        wnd.down('[name="ratio"]').setValue(rec.get('ratio'));
        wnd.down('[name="minValue"]').setValue(rec.get('minValue'));
        wnd.down('[name="maxValue"]').setValue(rec.get('maxValue'));
        wnd.down('[name="comment"]').setValue(rec.get('comment'));
        var dataTable = rec.get('dataTable');
        if (dataTable && dataTable.length > 0) {
            wnd.down('[name="dataTable"]').setValue(true);
            wnd.down('sensorsvaluesgrid').getStore().add(dataTable);
        }
        if (rec.get('showInInfo') !== true && rec.get('showInInfo') !== false) {
            wnd.down('[name="showInInfo"]').setValue(true);
        } else {
            wnd.down('[name="showInInfo"]').setValue(rec.get('showInInfo'));
        }

        wnd.down('#btnAddSensor').hide();
        wnd.down('#btnUpdSensor').show();
    },
    showMessage:function(title,msg){
        Ext.MessageBox.show({
            title: title,
            msg: msg,
            icon: Ext.MessageBox.WARNING,
            buttons: Ext.Msg.OK
        });
    }
});

Ext.define('SensorsValuesGrid', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.sensorsvaluesgrid',
    tbar: [
        {
            xtype: 'textfield',
            itemId: 'xValue',
            fieldLabel: 'X',
            labelWidth: 16,
            labelPad: 2,
            width: 64,
            padding: '0 4 0 4'
        },
        {
            xtype: 'textfield',
            itemId: 'yValue',
            fieldLabel: 'Y',
            labelWidth: 16,
            labelPad: 2,
            width: 64,
            padding: '0 4 0 4'
        },
        {
            icon: 'images/ico16_plus_def.png',
            itemId: 'btnAddRecord',
            text: 'Добавить',//tr('settingssensors.addrow'),
            tooltip:'Добавить строку',// tr('settingssensors.addrow.tip'),
            tooltipType: 'title',
            margin: '0 4 0 4',
            handler: function(btn) {
                var x = btn.up().down('#xValue').getValue();
                var y = btn.up().down('#yValue').getValue();
                var newRow = {x: (isNaN(x)?null:x), y: (isNaN(y)?null:y)};
                var grid = btn.up('grid');

                grid.getStore().add(newRow);
                btn.up().down('#xValue').setValue('');
                btn.up().down('#yValue').setValue('');
            }
        },
        '-',
        {
            icon: 'images/ico16_edit_def.png',
            itemId: 'btnEditRecord',
            text:'Редактировать' ,//tr('settingssensors.editrow'),
            tooltip:'Редактировать строку' ,//tr('settingssensors.editrow.tip'),
            tooltipType: 'title',
            margin: '0 0 0 4',
            disabled: true,
            handler: function(btn) {
                btn.up('grid').editRow();
            }
        },
        {
            icon: 'images/ico16_signno.png',
            itemId: 'btnDeleteRecord',
            text:'Удалить', //tr('settingssensors.removerow'),
            tooltip:'Удалить строку' ,//tr('settingssensors.removerow.tip'),
            tooltipType: 'title',
            margin: '0 4 0 4',
            disabled: true,
            handler: function(btn) {
                var grid = btn.up('grid');
                grid.removeRow();
            }
        },
        "-",
        {
            xtype: 'form',
            border: false,
            fileUpload: true,
            padding: '0 4 0 4',
            api: {
                submit: dataFileLoader.getUploadedFileData
            },
            items: [
                {
                    xtype: 'filefield',
                    name: 'dataFile',
                    buttonOnly: true,
                    hideLabel: true,
                    buttonText: 'Загрузить из файла',//tr('settingssensors.loadfromfile'),
                    tooltip: 'Загрузить строки из файла',//tr('settingssensors.loadrowsfromfile'),
                    tooltipType: 'title',
                    padding: 0,
                    margin: 0,
                    listeners: {
                        change: function(ff, newVal, oldVal) {
                            ff.up('form').submit({
                                waitMsg: 'Загрузка файла',//tr('settingssensors.fileloading')+'...',
                                success: function(form, action) {
                                    var res = action.result;

                                    if (res && res.data && res.data.length > 0) {
                                        var store = ff.up('grid').getStore();
                                        store.removeAll();
                                        store.add(res.data);
                                    }
                                    if (res && res.error) {
                                        Ext.MessageBox.show({
                                            title:'Ошибка загрузки файла',// tr('settingssensors.fileloading.error'),
                                            msg: res.error,
                                            icon: Ext.MessageBox.ERROR,
                                            buttons: Ext.Msg.OK
                                        });
                                    }
                                },
                                failure: function (form, action) {
                                    var res = action.result;
                                    console.log('Action = ', action);
                                    if (res && res.error) {
                                        Ext.MessageBox.show({
                                            title:'Ошибка загрузки файла',// tr('settingssensors.fileloading.error'),
                                            msg: res.error,
                                            icon: Ext.MessageBox.ERROR,
                                            buttons: Ext.Msg.OK
                                        });
                                    }
                                }
                            });
                        }
                    }
                }
            ]
        }
    ],
    plugins: [
        {
            ptype: 'rowediting',
            clicksToEdit: 2,
            pluginId: 'editplugin'
        }
    ],
    viewConfig: {
        markDirty: false
    },
    store: {
        fields: ['x', 'y'],
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
    rowLines: true,
    border: false,
    padding: false,
    layout: 'fit',
    columns: [
        {
            text: 'X',
            dataIndex: 'x',
            menuDisabled: true,
            resizable: false,
            flex: 1,
            editor: {
                xtype: 'textfield'
            }
        },
        {
            text: 'Y',
            dataIndex: 'y',
            menuDisabled: true,
            resizable: false,
            flex: 1,
            editor: {
                xtype: 'textfield'
            }
        },
        {
            menuText: 'Редактирование строк',//tr('settingssensors.rowsediting'),
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
                          view.getSelectionModel().select(rec);
                          view.up('grid').editRow();
                    },
                    getClass: function(val, metaData) {
                        metaData.tdAttr = 'title="'+'Редактировать'/*tr('settingssensors.editrow')*/+'"';
                        return 'object-list-button';
                    }
                }
            ]
        },
        {
            menuText:'Удаление строки',// tr('settingssensors.rowremoving'),
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
                        view.getSelectionModel().select(rec)
                        view.up('grid').removeRow();
                    },
                    getClass: function(val, metaData) {
                        metaData.tdAttr = 'title="'+'Удалить'/*tr('settingssensors.removerow')*/+'"';
                        return 'object-list-button';
                    }
                }
            ]
        }
    ],
    listeners: {
        select: function(selection, rec, index) {
            var tbar = selection.view.up('grid').down('toolbar');
            tbar.down('#btnEditRecord').enable();
            tbar.down('#btnDeleteRecord').enable();
        },
        deselect: function(selection, rec, index) {
            var tbar = selection.view.up('grid').down('toolbar');
            tbar.down('#btnEditRecord').disable();
            tbar.down('#btnDeleteRecord').disable();
        }
    },
    editRow:function(){
        var grid = this;
        var rec = grid.getSelectionModel().getSelection()[0];
        var editor = grid.getPlugin('editplugin');

        if (editor) {
            editor.startEdit(rec, 0);
        }
    },
    removeRow:function(){
        var grid=this
        var rec = grid.getSelectionModel().getSelection()[0];

        Ext.MessageBox.show({
            text:'Удалить', //tr('settingssensors.removerow'),
            msg: 'Вы действительно хотите удалить выбранную строку?',//tr('settingssensors.rowremoving.sure'),
            icon: Ext.MessageBox.QUESTION,
            buttons: Ext.Msg.YESNO,
            fn: function(a) {
                if (a === 'yes') {
                    grid.getSelectionModel().deselect(rec);
                    grid.getStore().remove(rec);
                }
            }
        });
    }
});
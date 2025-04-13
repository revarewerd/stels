/**
 * Created by IVAN on 10.10.2014.
 */
Ext.define('Billing.view.object.sensors.SettingsSensorsGrid', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.settingssensorsgrid',
    stateId: 'sensorsGrid',
    stateful: true,
    initComponent: function () {

        var self = this;
        Ext.apply(this,{
    tbar: [
        {
            icon: 'images/ico16_plus_def.png',
            itemId: 'btnAddRecord',
            text: "Добавить датчик",//tr('settingssensors.addsensor.tooltip'),
            tooltip:"Добавить датчик",//tr('settingssensors.addsensor'),
            tooltipType: 'title',
            handler: function(btn) {
                var uid = self.objRecord.get('uid');
                var grid = self//.down('settingssensorsgrid');
                var existingWindow = WRExtUtils.createOrFocus('SensorParamsWindow' + uid, 'Billing.view.object.sensors.SensorParamsWindow', {
                    objUid: uid,
                    linkedGrid: grid
                });
                if (existingWindow.justCreated) {
                    existingWindow.on('close', function () {
                        delete existingWindow;
                    });
                    existingWindow.show();
                }
            }
        },
        {
            icon: 'images/ico16_edit_def.png',
            itemId: 'btnEditRecord',
            text: "Редактировать датчик",//tr('settingssensors.editsensor'),
            tooltip: "Редактировать датчик",//tr('settingssensors.editsensor.tooltip'),
            tooltipType: 'title',
            disabled: true,
            handler: function(){self.editSensor()}
        },
        {
            icon: 'images/ico16_signno.png',
            itemId: 'btnDeleteRecord',
            text: "Удалить датчик",//tr('settingssensors.removesensor'),
            tooltip: "Удалить датчик",//tr('settingssensors.removesensor.tooltip'),
            tooltipType: 'title',
            disabled: true,
            handler: function() {self.removeSensor()}
        }
    ],
    viewConfig: {
        markDirty: false
    },
    store: {
        fields: ['name', 'type', 'paramName', 'unit', 'comment', 'ratio', 'dataTable', 'minValue', 'maxValue', {name: 'showInInfo', defaultValue: true}],
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
            stateId: 'name',
            text:"Название",// tr('settingssensors.grid.name'),
            dataIndex: 'name',
            flex: 1
        },
        {
            stateId: 'type',
            text:"Тип",//tr('settingssensors.grid.type'),
            dataIndex: 'type',
            flex: 1,
            renderer: function(val, metaData, rec) {
                switch (val) {
                    case 'sFuelL': val = "Датчик уровня топлива"/*tr('settingssensors.grid.type.fuellevel')*/;  break;
                    case 'sTmp': val = "Датчик температуры"/*tr('settingssensors.grid.type.temperature')*/;  break;
                    case 'sEngS': val = "Датчик оборотов двигателя"/* tr('settingssensors.grid.type.enginespeed')*/;  break;
                    case 'sIgn': val = "Датчик зажигания"/*tr('settingssensors.grid.type.ignition')*/;  break;
                    case 'sPwr': val = "Датчик напряжения"/*tr('settingssensors.grid.type.powervoltage')*/; break;
                    default: val = "Неизвестный"/*tr('settingssensors.grid.type.unknown')*/;
                }
                return val;
            }
        },
        {
            stateId: 'unit',
            text:"Размерность",//tr('settingssensors.grid.unit.dim'),
            dataIndex: 'unit',
            flex: 1
        },
        {
            stateId: 'param',
            text:"Параметр", //tr('settingssensors.grid.paramName'),
            dataIndex: 'paramName',
            flex: 1
        },
        {
            stateId: 'edit',
            menuText: "Редактирование датчиков",//tr('settingssensors.grid.sensorediting'),
            xtype: 'actioncolumn',
            width: 20,
            menuDisabled: true,
            sealed: true,
            sortable: false,
            resizable: false,
            items: [
                {
                    icon: 'images/ico16_edit_def.png',
                    handler: function(grid, rowIndex, colIndex, item, e, rec) {
                        grid.getSelectionModel().select(rec)
                        self.editSensor()
                    },
                    getClass: function(val, metaData) {
                        metaData.tdAttr = 'title="'+'Редактировать датчик'/*tr('settingssensors.editsensor')*/+'"';
                        return 'object-list-button';
                    }
                }
            ]
        },
        {
            stateId: 'delete',
            menuText: "Удаление датчиков",//tr('settingssensors.sensorsremoving'),
            xtype: 'actioncolumn',
            width: 20,
            menuDisabled: true,
            sealed: true,
            sortable: false,
            resizable: false,
            items: [
                {
                    icon: 'images/ico16_signno.png',
                    handler: function(grid, rowIndex, colIndex, item, e, rec) {
                          grid.getSelectionModel().select(rec)
                          self.removeSensor()
                    },
                    getClass: function(val, metaData) {
                        metaData.tdAttr = 'title="'+'Удалить датчик'/*tr('settingssensors.removesensor')*/+'"';
                        return 'object-list-button';
                    }
                }
            ]
        }
    ],
    editSensor:function(){
        var uid = self.objRecord.get('uid');
        var grid = self
        var rec = grid.getSelectionModel().getSelection()[0];
        console.log("rec",rec)
        var existingWindow = WRExtUtils.createOrFocus('SensorParamsWindow' + rec.id, 'Billing.view.object.sensors.SensorParamsWindow', {
            objUid: uid,
            linkedGrid: grid,
            editRecord: rec,
            title: /*tr('settingssensors.editing')*/'Редактировать'+' &laquo;'+rec.get('name')+'&raquo;',
            btnConfig: {
                icon: 'images/ico24_bell.png',
                text: /*tr('settingssensors.editing')*/'Редактировать'+' &laquo;'+rec.get('name')+'&raquo;'
            }
        });
        if (existingWindow.justCreated) {
            existingWindow.on('close', function () {
                delete existingWindow;
            });
            existingWindow.show();
        }
    },
    removeSensor:function(){
        var grid = self
        var rec = grid.getSelectionModel().getSelection()[0];
        Ext.MessageBox.show({
            title: 'Удаление датчика',//tr('settingssensors.sensorremoving'),
            msg: /*tr('settingssensors.sensorremoving.sure')*/'Вы действительно хотите удалить'+' &laquo;'+rec.get('name')+'&raquo;?',
            icon: Ext.MessageBox.QUESTION,
            buttons: Ext.Msg.YESNO,
            fn: function(a) {
                if (a === 'yes') {
                    grid.getSelectionModel().deselect(rec);
                    grid.getStore().remove(rec);
                    var btn = grid.up('window').down('toolbar #btnApplySettings');
                    if (btn && btn.isDisabled) {
                        btn.enable();
                    }
                }
            }
        });
    }
        });
        this.callParent(arguments);
    },
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
    }
});
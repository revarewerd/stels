Ext.define('Billing.view.object.AllObjectsPanel', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.allobjectsgrid',
    title: 'Объекты',
    requires: [
        'Billing.view.object.ObjectForm',
        'Billing.view.TrackerSMSWindow'
    ],
    features: [
        {
            ftype: 'summary',
            dock: 'top'
        }
    ],
    selModel: {
        pruneRemoved: false,
        mode: 'MULTI'
    },
    plugins: 'bufferedrenderer',
    invalidateScrollerOnRefresh: false,
    loadBeforeActivated: false,
    storeName: 'EDS.store.AllObjectsService',
    itemType:'objects',
    dockedToolbar: ['add', 'remove', 'refresh', 'search','fill','gridDataExport'],
    initComponent: function () {
        var self=this;
        var hideRule=!self.viewPermit;
    Ext.apply(this,{
    columns: [
        {
            hideable: false,
            menuDisabled: true,
            sortable: false,
            flex: 1
        },
        {
            header: '№',
            xtype: 'rownumberer',
            width: 40,
            resizable: true
        },
        {
            menuDisabled: true,
            sortable: false,
            xtype: 'actioncolumn',
            itemId: 'obj',
            width: 20,
            resizable: false,
            items: [
                {
                    icon: 'images/car_002_blu_24.png',
                }
            ]
        },
        {
            header: 'Имя',
            width: 170,
            sortable: true,
            dataIndex: 'name',
            filter: {
                type: 'string'
            },
            renderer: function (val, metaData, rec) {
                metaData.tdAttr = 'style="cursor: pointer !important;" title="' + val + '"';
                return val
            },
            summaryType: 'count',
            summaryRenderer: function (value, summaryData, dataIndex) {
                return Ext.String.format('<b>Всего позиций: {0} </b>', value);
            }
        },
        {
            header: 'Пользовательское имя',
            width: 170,
            sortable: true,
            dataIndex: 'customName',
            filter: {
                type: 'string'
            }
        },
        {
            header: 'Комментарий',
            width: 170,
            sortable: true,
            dataIndex: 'comment',
            filter: {
                type: 'string'
            },
            renderer: function (val, metaData, rec) {
                metaData.tdAttr = 'title="' + val + '"'
                return val
            },
            hidden:hideRule
        },
        {
            header: 'Учетная запись',
            width: 170,
            sortable: true,
            dataIndex: 'accountName',
            filter: {
                type: 'string'
            },
            renderer: function (val, metaData, rec) {
                metaData.tdAttr = 'title="' + val + '"'
                return val
            }
        },
        {
            text: 'УИД',
            width: 170,
            hidden: true,
            dataIndex: 'uid',
            sortable: true,
            align: 'right',
            filter: {
                type: 'string'
            }
        },
        {
            text: 'Трекер',
            width: 150,
            dataIndex: 'trackerModel',
            sortable: true,
            align: 'left',
            filter: {
                type: 'string'
            }
        },
        {
            text: 'IMEI',
            width: 110,
            dataIndex: 'eqIMEI',
            sortable: true,
            align: 'right',
            filter: {
                type: 'string'
            }
        },
        {
            text: 'Телефон',
            hidden:hideRule,
            width: 110,
            dataIndex: 'simNumber',
            sortable: true,
            align: 'right',
            filter: {
                type: 'string'
            }
        },
        {
            text: 'Абонентская плата',
            width: 110,
            dataIndex: 'cost',
            sortable: true,
            align: 'right',
            renderer: function (value) {
                return accounting.formatMoney(parseFloat(value / 100));
            },
            summaryType: 'sum',
            summaryRenderer: function (value, summaryData, dataIndex) {
                return Ext.String.format('<b>{0}</b>', accounting.formatMoney(parseFloat(value / 100)));
            }
        },
        {
            text: '',
            menuText:"Блокировка",
            tooltip: 'Блокировка',
            tooltipType: 'title',
            sortable: true,
            resizable: false,
            width: 32,
            dataIndex: 'blocked',
            renderer: function (val, metaData, rec) {
                if (val === true) {
                    return '<img src="images/ico16_lock.png" alt="" title="Автомобиль заблокирован" />';
                } else {
                    return '';
                }
            }
        },
        {
            text: '',
            menuText:"Зажигание",
            tooltip: 'Зажигание',
            tooltipType: 'title',
            sortable: true,
            resizable: false,
            width: 32,
            dataIndex: 'ignition',
            renderer: function (val, metaData, rec) {
                if (val != 'unknown' && val > 0) {
                    return '<img src="images/ignition_key16.png" alt="" title="Зажигание включено" />';
                } else {
                    return '';
                }
            }
        },
        {
            text: '',
            menuText:"Скорость",
            tooltip: 'Скорость',
            tooltipType: 'title',
            sortable: true,
            resizable: false,
            dataIndex: 'speed',
            width: 32,
            renderer: function (val, metaData) {
                var msg = 'title="Последнее состояние: автомобиль стоит"',
                    res = '<img src="images/ico16_car_stop.png" alt="" />';
                if (val > 0) {
                    msg = 'title="Последнее состояние: автомобиль едет (скорость ' + val + ' км/ч)"';
                    res = '<img src="images/ico16_car_move.png" alt="" />';
                }
                metaData.tdAttr = msg;
                return res;
            }
        },
        {
            text: '',
            menuText:'Радиозакладка',
            tooltip: 'Радиозакладка',
            tooltipType: 'title',
            sortable: true,
            resizable: false,
            dataIndex: 'radioUnit',
            width: 32,
            renderer: function(val, metaData) {
                if (val) {
                    if (val.installed) {
                        var workdate = new Date(val.workDate),
                            current = new Date(),
                            yearago = Ext.Date.subtract(current, Ext.Date.YEAR, 1),
                            isValid = Ext.Date.between(workdate, yearago, current);
                        var msg ='title="' + val.type + ' ' + val.model + '\nДата последнего проведения работ: ' + Ext.Date.format(workdate, "d.m.Y") + '"';
                        metaData.tdAttr =  msg
                        return (isValid)?('<img src="images/ico16_radio_ok.png" alt="" />'):('<img src="images/ico16_radio_err.png" alt="" />');
                    } else {
                        return '';
                    }
                } else {
                    return '';
                }
            }
        },
        {
            text: 'SMS',
            tooltip: 'SMS переписка с абонентским терминалом',
            tooltipType: 'title',
            sortable: true,
            resizable: false,
            dataIndex: 'sms',
            width: 32,
            renderer: function (val, metaData, rec) {
                if (rec.get('simNumber') != "") {
                    metaData.tdAttr = 'style="cursor: pointer !important;"'
                    if (val === true) return '<img src="images/ico16_msghist_yel.png" alt="" title="Показать сообщения" />'
                    else  return '<img src="images/ico16_msghist.png" alt="" title="Показать сообщения" />'
                } else {
                    return '';
                }
            }
        },
        {
            text: 'Последнее сообщение',
            tooltip: 'Последнее сообщение / Источник',
            tooltipType: 'title',
            sortable: true,
            dataIndex: 'latestmsg',
            width: 170,
            renderer: function (val, metaData, rec) {
                var lmd = new Date(val),
                    lms = Ext.Date.format(lmd, "d.m.Y H:i:s"),
                    cur = new Date(),
                    res,
                    timegrn = Ext.Date.subtract(cur, Ext.Date.MINUTE, 20),
                    timeyel = Ext.Date.subtract(cur, Ext.Date.HOUR, 3),
                    timered = Ext.Date.subtract(cur, Ext.Date.MONTH, 1),
                    sat = rec.get('satelliteNum'),
                    protocol = rec.get('latestmsgprotocol');
                if (!val)
                    res = "Сообщений не поступало";
                else {
                    var str = lms + " (" + protocol + ")";
                    if (lmd > cur) {
                        res = '<span style="color:green;">' + str + '</span>';
                    } else if (Ext.Date.between(lmd, timegrn, cur)) {
                        res = '<span style="color:green;">' + str + '</span>';
                    } else if (Ext.Date.between(lmd, timeyel, cur)) {
                        res = '<span style="color:gold;">' + str + '</span>';
                    } else if (Ext.Date.between(lmd, timered, cur)) {
                        res = '<span style="color:red;">' + str + '</span>';
                    } else
                        res = '<span style="color:brown;">' + str + '</span>';
                    metaData.tdAttr = 'title="' + (rec.get('placeName') ? 'Последнее положение: ' + rec.get('placeName') + '\n' : '') + 'Кол-во спутников: ' + sat + '"\n\
                                   style="cursor: pointer !important;"';
                }
                return res;
            }
        },
        {
            text: 'Сообщения от спящего блока',
            tooltip: 'Сообщения от спящего блока',
            tooltipType: 'title',
            resizable: true,
            sortable: true,
            dataIndex: 'sleepertime',
            width: 250,
            renderer: function (val, metaData, rec) {
                val = rec.get('sleeper');
                if (val != null) {
                    var lmd = new Date(val.time),
                        lms = val.time ? Ext.Date.format(lmd, "d.m.Y H:i:s") : "сообщений не поступало";
                    metaData.tdAttr = 'title="' + val.info+'" style="cursor: pointer !important;"';
                    return val.alert + " : " + lms + " : " + val.battery;
                }
                else return '';
            }

        },
        {
            text: 'Положение',
            hidden:true,
            width: 250,
            dataIndex: 'placeName',
            sortable: true,
            align: 'right',
            filter: {
                type: 'string'
            },
            renderer: function (val, metaData, rec) {
                metaData.tdAttr = 'title="' + val + '"'
                return val
            }
        },
        {
            hideable: false,
            menuDisabled: true,
            sortable: false,
            flex: 1
        }
    ],
        onSelectChange: function (selModel, selections) {
            if(hideRule)
                this.down('#delete').setDisabled(true);
            else
                this.down('#delete').setDisabled(selections.length === 0);
        }
    });
        this.callParent();
        //this.getSelectionModel().on('selectionchange', this.onSelectChange, this);
        //this.down('#delete').setDisabled(hideRule)
        this.down('#add').setDisabled(hideRule)

        var toolbar = this.dockedItems.findBy(function(e){
            return e.xtype == 'toolbar';
        });
        toolbar.insert(7, {
            xtype: 'tbfill'
        });
        toolbar.insert(8, {
            xtype: 'checkboxfield',
            margin: '0 10 0 0',
            boxLabel: 'Автообновление GPS-данных',
            checked: (Ext.util.Cookies.get('refreshGPS') == 'true'),
            listeners: {
                change: function(self, value){
                    Ext.util.Cookies.set('refreshGPS', value, Ext.Date.add(new Date(), Ext.Date.YEAR, 100));
                    servermesAtmosphere.disconnect();
                }
            }
        });
    },
    listeners: {
        cellclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
            var self=this
            var itemId=self.columnManager.columns[cellIndex].itemId;
            if (itemId == "obj") {
                this.showObjectForm(record)
            }
            var dataIndex=self.columnManager.columns[cellIndex].dataIndex
            if (dataIndex =="sms"  && record.get("simNumber")!="") {               
                this.showTrackerSMS(record)
            }
        },
        celldblclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
            var self=this
            var dataIndex=self.columnManager.columns[cellIndex].dataIndex
            switch(dataIndex)
            {
                case "name":
                    this.showObjectForm(record)
                    break;
                case "latestmsg":
                    this.showTerminalMessages(record)
                    break;
                case "sleepertime":
                    //if(record.get("sleeper") != null)
                    this.showSleepersMessages(record)
                    break;
        }
       }
    },
    onAddClick: function(){
        var rec=Ext.create('ObjectF',{type:'Автомобиль'});
        this.showObjectForm(rec);
    },
    onDeleteClick: function () {
        var selection = this.getView().getSelectionModel().getSelection();//[0];
        if (selection) {
            var store = this.store;
            Ext.MessageBox.show({
                title: 'Удаление элемента',
                buttons:  Ext.MessageBox.YESNO,
                msg: 'Вы уверены, что хотите удалить ' + selection.length + ' объектов? <br/><br/><input type="checkbox" id="deinstall_equipment_chk" /> Снять оборудование с объектов',

                fn: function(btn) {
                    if(btn == 'yes') {
                        var deinstall = Ext.getElementById('deinstall_equipment_chk').checked;
                        console.log("removing:", selection);
                        allObjectsService.remove(Ext.Array.map(selection, function (m) {
                            return m.get("_id");
                        }), deinstall, function (r, e) {
                            if (!e.status) {
                                Ext.MessageBox.show({
                                    title: 'Произошла ошибка',
                                    msg: e.message,
                                    icon: Ext.MessageBox.ERROR,
                                    buttons: Ext.Msg.OK
                                });
                            }
                        });
                    }
                }
            });
        }
    },
    showSleepersMessages: function (record) {
        var self=this;
        var existingWindow = WRExtUtils.createOrFocus('SleeperMesWindow' + record.get('_id'), 'Billing.view.SleeperMesWindow', {
            title: 'Сообщения от "спящих" "' + record.get('name') + '"',
            uid:record.get('uid'),
            hideRule:!self.viewPermit
        });
        existingWindow.show();
        return  existingWindow
    },
    showTrackerSMS: function (record) {
        var self=this;
        var existingWindow = WRExtUtils.createOrFocus('TrackerSMSWindow' + record.get('_id'), 'Billing.view.TrackerSMSWindow', {
            icon:"images/ico24_msghist.png",
            title: 'Сообщения от терминала "'+record.get('eqIMEI')+'"',            
            phone:record.get('simNumber'),
            eqMark:record.get('eqMark'),
            hideRule:!self.viewPermit
        });
        existingWindow.show();
        return  existingWindow           
    },
    showTerminalMessages: function (record) {
        var self=this;
        var existingWindow = WRExtUtils.createOrFocus('TerminalMessages' + record.get('_id'), 'Billing.view.TerminalMessages', {
            title: 'Сообщения от объекта "' + record.get('name') + '"',            
            uid: record.get('uid'),
            dateFrom:new Date(record.get('latestmsg')),
            hideRule:!self.viewPermit
        });
        existingWindow.show();
        return  existingWindow  
    },
    showObjectForm: function (record) {        
        var self=this
        var existingWindow = WRExtUtils.createOrFocus('ObjectForm' + record.get('_id'), 'Billing.view.object.ObjectForm.Window', {
            title: 'Объект "'+record.get('name')+'"',            
            accountId:record.get('account'),
            hideRule:!self.viewPermit
        });
        if (existingWindow.justCreated) {
            var newobjpanel=existingWindow.down('[itemId=objpanel]') 
//            newobjpanel.on('save', function (args) {
//                self.refresh()
//            })
            newobjpanel.loadRecord(record)
        }
            existingWindow.show();
            return  existingWindow.down('[itemId=objpanel]')
    },
    updateData:function(data){
        this.changeData(data,"object")
    },
    changeData:function(data,aggregate){
        if(data.aggregate==aggregate){
            var self=this
            var store=self.getStore()
            //console.log("data=",data)
            //console.log("store.snapshot",store.snapshot)
            var recstore=store.snapshot
            if(recstore==undefined) recstore=store.data

            if(data.action == 'batchUpdate'){
                store.suspendAutoSync();
                store.suspendEvents(false);
                Ext.Array.forEach(data.events, function(event) {
                    event.notrefilter = true;// (i != length - 1);
                    self.changeData(event, aggregate);
                });
                store.resumeAutoSync();
                store.resumeEvents();
                //store.commitChanges();
                if (data.refresh)
                    store.fireEvent("refresh");
                return;
            }

            var rec=recstore.findBy(function(item,uid){
                if(item.get("uid")==data.itemId)
                    {return true}
                else {return false}
            });
//            var rec = recstore.getById(data.itemId)
            //console.log("change rec=",rec)
            store.suspendAutoSync();
            switch(data.action){
                case "create" : {
                    console.log("create "+aggregate+" uid="+data.itemId);
                    if(rec==null) {
                        store.insert(0, data.data)
                    }
                    break;
                }
                //case "restore" : { // TODO производительность
                //    console.log("restore "+aggregate+" uid="+data.itemId);
                //    if(rec==null) {
                //        store.insert(0, data.data)
                //    }
                //    break;
                //}
                case "update" :
                    //console.log("update "+aggregate+" uid="+data.itemId);
                    if(rec!=null) {
                        rec.beginEdit()
                        for(fname in data.data){
                            //console.log("fname="+fname+" data[fname]="+data.data[fname]);
                            rec.set(fname,data.data[fname])
                        }
                        rec.endEdit(true);
                        rec.commit(false);
                    } break;
                case "equpdate" :
                    console.log("equpdate "+aggregate+" uid="+data.itemId);
                    if(rec!=null) {
                        rec.beginEdit()
                        for(fname in data.data){
                            //console.log("fname="+fname+" data[fname]="+data.data[fname]);
                            rec.set(fname,data.data[fname])
                        }
                        rec.endEdit()
                        rec.commit()
                    } break;
                case "delete" : {
                    console.log("delete "+aggregate+" uid="+data.itemId);
                    if(rec!=null) {
                        store.remove(rec)
                    }
                    break;
                }
                case "remove": {
                    console.log("remove "+aggregate+" uid="+data.itemId);
                    if(rec!=null) {
                        store.remove(rec);
                    }
                    break;
                }
            }
            if(!data.notrefilter) {
                store.initSortable();
                store.sort();
                store.filter();
            }
            store.resumeAutoSync();
        }
    }
});


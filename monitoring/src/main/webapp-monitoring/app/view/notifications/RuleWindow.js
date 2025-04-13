Ext.define('Seniel.view.notifications.RuleWindow', {
    extend: 'Seniel.view.WRWindow',
    requires: [
        'Seniel.view.WRRadioGroup',
        'Seniel.view.notifications.RulesTypes'
    ],
    alias: 'widget.addnotwnd',
    stateId: 'ntfRuleWnd',
    stateful: true,
    icon: 'images/ico16_bell.png',
    title: tr('rules.window.newrule'),
    btnConfig: {
        icon: 'images/ico24_bell.png',
        text: tr('rules.grid.type.newrule')
    },
    maximizable: false,
    minWidth: 720,
    minHeight: 440,
    layout: 'border',
    items: [{
        region: 'west',
        title: tr('rules.objectselection'),
        width: 240,
        split: true,
        collapsible: true,
        floatable: false,
        layout: 'border',
        items: [
            {
                region:'center',
                xtype: 'grid',
                itemId:'selGrid',
                selType: 'checkboxmodel',
                autoScroll: true,
                selModel: {
                    showHeaderCheckbox: true,
                    mode: 'SIMPLE'
                },
                store: {
                    fields: ['uid', 'name'],
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
                        text: tr('rules.grid.objectname'),
                        dataIndex: 'name',
                        flex: 1
                    }
                ],
                enableColumnHide: false,
                enableColumnMove: false,
                rowLines: true,
                border: false,
                padding: false,
                tbar: [
                    {
                        xtype: 'triggerfield',
                        itemId:'searchField',
                        triggerCls:'x-form-clear-trigger',
                        allowBlank: true,
                        fieldLabel: '&nbsp;'+tr('rules.grid.objectselection')+':',
                        width: 220,
                        onTriggerClick: function() {
                            this.setValue("")
                        },
                        listeners: {
                            change: function(field, val, oldval) {
                                var str = val.replace(/\\/,'\\\\').replace(/\[/, '\\[').replace(/\]/, '\\]').replace(/\+/, '\\+').replace(/\|/, '\\|').replace(/\./, '\\.').replace(/\(/, '\\(').replace(/\)/, '\\)').replace(/\?/, '\\?').replace(/\*/, '\\*').replace(/\^/, '\\^').replace(/\$/, '\\$');
                                var re = new RegExp(str, 'i');
                                var sensorPicker = field.up('addnotwnd').down('#ntfDataSensor');
                                var grid=field.up('grid');
                                var store=grid.getStore();
                                function clearFilter(){
                                    store.clearFilter();
                                    var selArray=[];
                                    for(var i in sensorPicker.hiddenSelected) {
                                        var rec=store.findRecord("uid",sensorPicker.hiddenSelected[i]);
                                        if(!!rec) selArray.push(rec);
                                    }
                                    grid.getSelectionModel().select(selArray,true);
                                    sensorPicker.hiddenSelected=[];
                                }
                                if (val === ''){
                                    clearFilter();
                                    return;
                                } else {
                                    clearFilter();
                                    var selBeforeFilter=sensorPicker.selectedUIDs;
                                    store.filter({id: 'name', property: 'name', value: re});
                                    for(var i in selBeforeFilter){
                                        if(sensorPicker.selectedUIDs.lastIndexOf(selBeforeFilter[i]==-1)){
                                            sensorPicker.hiddenSelected.push(selBeforeFilter[i])
                                        }
                                    }
                                    //console.log("hidden selected",sensorPicker.hiddenSelected)
                                }
                            },
                        }
                    }
                ],
                listeners: {
                    selectionchange: function(selModel, selected) {
                        var uids = [];
                        Ext.Array.each(selected, function(rec) {
                            uids.push(rec.get('uid'));
                        });
                        var sensorPicker = this.up('addnotwnd').down('#ntfDataSensor');
                        var gridStore=this.up('addnotwnd').down('#selGrid').getStore();
                        if (sensorPicker && sensorPicker.getStore()) {
                            console.log('Selected uids = ', uids);
                            if (sensorPicker.hiddenSelected && sensorPicker.hiddenSelected.length>0) {
                                sensorPicker.selectedUIDs=uids;
                                var updatedHidden=[];
                                for(var i in sensorPicker.hiddenSelected){
                                    var recId=gridStore.find("uid",sensorPicker.hiddenSelected[i]);
                                    if(recId==-1){
                                        updatedHidden.push(sensorPicker.hiddenSelected[i])
                                    }
                                }
                                sensorPicker.hiddenSelected=updatedHidden;
                                sensorPicker.selectedUIDs.concat(sensorPicker.hiddenSelected);
                            }
                            else {
                                sensorPicker.selectedUIDs = uids;
                            }
                            console.log("Selected uids =",sensorPicker.selectedUIDs)
                            if (sensorPicker.store.isDataLoaded) {
                                sensorPicker.getStore().reload();
                            }
                        }
                    }
                }
            }
        ]
    },
        {
        region: 'center',
        xtype: 'panel',
        border: false,
        autoScroll: true,
        items: [
            {
                xtype: 'panel',
                title: tr('rules.notification'),
                border: false,
                layout: 'anchor',
                defaults: {
                    anchor: '100%'
                },
                items: [
                    {
                        xtype: 'textfield',
                        itemId: 'ntfName',
                        name: 'ntfName',
                        fieldLabel: tr('rules.ntfname'),
                        labelWidth: 180,
                        emptyText: tr('rules.ntfname.new'),
                        padding: '8 8 0 8'
                    },
                    {
                        xtype: 'combo',
                        itemId: 'ntfType',
                        name: 'ntfType',
                        fieldLabel: tr('rules.ntftype'),
                        store: Ext.create('Ext.data.ArrayStore', {
                            fields: ['type', 'text'],
                            data: {
                                items: Seniel.view.notifications.RulesTypes.getTypesList()
                            },
                            proxy: {
                                type: 'memory',
                                reader: {
                                    type: 'json',
                                    root: 'items'
                                }
                            }
                        }),
                        allowBlank: false,
                        editable: false,
                        queryMode: 'local',
                        valueField: 'type',
                        displayField: 'text',
                        value: 'ntfVelo',
                        labelWidth: 180,
                        padding: '8 8 0 8',
                        maskArray: Seniel.view.notifications.RulesTypes.getMsgMasksList(),
                        listeners: {
                            change: function(cb, newVal, oldVal) {
                                var wnd = cb.up('window');
                            
                                wnd.down('#' + oldVal + 'Params').hide();
                                wnd.down('#' + newVal + 'Params').show();
                                
                                var mask = wnd.down('#ntfMessageMask');
                                if (mask.getValue()) {
                                    cb.maskArray[oldVal] = mask.getValue();
                                }
                                mask.setValue(cb.maskArray[newVal]);
                                if (newVal === 'ntfSlpr') {
                                    wnd.down('#ntfAddAction').hide();
                                } else {
                                    wnd.down('#ntfAddAction').show();
                                }
                                cb.up('panel[region="center"]').doComponentLayout();
                            }
                        }
                    },
                    {
                        xtype: 'checkboxfield',
                        itemId: 'ntfAllObjects',
                        boxLabel: tr('rules.notifyall'),
                        name: 'ntfAllObjects',
                        inputValue: true,
                        labelAlign: 'right',
                        labelWidth: 240,
                        padding: '0 8 0 8',
                        listeners: {
                            change: function(cb, newVal, oldVal) {
                                var objectsList = cb.up('window').down('panel[region="west"]');
                                if (newVal) {
                                    if (objectsList) {
                                        objectsList.hide();
                                        var sensorPicker = cb.up('addnotwnd').down('#ntfDataSensor');
                                        if (sensorPicker && sensorPicker.getStore()) {
//                                            sensorPicker.selectedUIDs = [];

                                            var uids = [];

                                            objectsList.down('grid').getStore().each(function(rec) {
                                                uids.push(rec.get('uid'));
                                            });

                                            sensorPicker.selectedUIDs = uids;

                                            if (sensorPicker.store.isDataLoaded) {
                                                sensorPicker.getStore().reload();
                                            }
                                        }
                                    }
                                } else {
                                    if (objectsList) {
                                        objectsList.show();
                                        var uids = [];
                                        Ext.Array.each(objectsList.down('grid').getSelectionModel().getSelection(), function(rec) {
                                            uids.push(rec.get('uid'));
                                        });
                                        var sensorPicker = cb.up('addnotwnd').down('#ntfDataSensor');
                                        if (sensorPicker && sensorPicker.getStore()) {
                                            console.log('Selected uids = ', uids);
                                            sensorPicker.selectedUIDs = uids;
                                            if (sensorPicker.store.isDataLoaded) {
                                                sensorPicker.getStore().reload();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                ]
            },
            {
                xtype: 'panel',
                title: tr('rules.params'),
                border: false,
                items: Seniel.view.notifications.RulesTypes.getFormPanels()
            },
            {
                xtype: 'panel',
                title: tr('rules.notificationandaction'),
                border: false,
                layout: 'anchor',
                defaults: {
                    anchor: '100%'
                },
                items: [
                    {
                        xtype: 'textareafield',
                        itemId: 'ntfMessageMask',
                        fieldLabel: tr('rules.messagemask'),
                        labelWidth: 180,
                        value: tr('rules.ntfmasks.velo'),
                        padding: '8 8 0 8'
                    },
                    {
                        xtype: 'fieldcontainer',
                        layout: 'column',
                        items: [
                            {
                                xtype: 'panel',
                                width: 191,
                                border: 0,
                                items: [
                                    {
                                        xtype: 'checkboxfield',
                                        itemId: 'ntfAlarmMessage',
                                        boxLabel: tr('rules.notifyinsystem'),
                                        name: 'ntfAlarm',
                                        inputValue: '1',
                                        labelAlign: 'right',
                                        padding: '4 0 4 8',
                                        checked: true
                                    },
                                    {
                                        xtype: 'checkboxfield',
                                        itemId: 'ntfAlarmEmail',
                                        boxLabel: tr('rules.notifybyemail'),
                                        name: 'ntfAlarm',
                                        inputValue: '2',
                                        labelAlign: 'right',
                                        padding: '0 0 4 8',
                                        listeners: {
                                            change: function(cb, newValue, oldValue) {
                                                if (newValue) {
                                                    cb.up('fieldcontainer').down('#ntfEmail').enable();
                                                } else {
                                                    cb.up('fieldcontainer').down('#ntfEmail').disable();
                                                }
                                            }
                                        }
                                    },
                                    {
                                        xtype: 'checkboxfield',
                                        itemId: 'ntfAlarmPhone',
                                        boxLabel: tr('rules.notifybysms'),
                                        name: 'ntfAlarm',
                                        inputValue: '3',
                                        labelAlign: 'right',
                                        padding: '0 0 0 8',
                                        listeners: {
                                            change: function(cb, newValue, oldValue) {
                                                if (newValue) {
                                                    Ext.MessageBox.alert(tr('rules.notifybysms'), tr('rules.notifybysms.costalarm'));
                                                    cb.up('fieldcontainer').down('#ntfPhone').enable();
                                                } else {
                                                    cb.up('fieldcontainer').down('#ntfPhone').disable();
                                                }
                                            }
                                        }
                                    }
                                ]
                            },
                            {
                                xtype: 'panel',
                                columnWidth: 1,
                                border: 0,
                                layout: 'anchor',
                                items: [
                                    {
                                        xtype: 'textfield',
                                        itemId: 'ntfEmail',
                                        anchor: '100%',
                                        emptyText: tr('rules.email'),
                                        padding: '34 8 4 2',
                                        disabled: true,
                                        validator: function(val) {
                                            var arr = val.split(',');
                                            for (var i = 0; i < arr.length; i++) {
                                                if (!Ext.form.VTypes.email(arr[i])) {
                                                    return tr('rules.email.valid');
                                                }
                                            }
                                            return true;
                                        },
                                        listeners: {
                                            afterrender: function(textbox) {
                                                var viewport = textbox.up('viewport');
                                                textbox.setValue(viewport.userEmail);
                                            }
                                        }
                                    },
                                    {
                                        xtype: 'textfield',
                                        itemId: 'ntfPhone',
                                        anchor: '100%',
                                        emptyText: tr('rules.phonanumber'),
                                        padding: '0 8 0 2',
                                        disabled: true,
                                        validator: function(val) {
                                            var arr = val.split(','),
                                                regex = /^\s*\+7\d{10,}\s*$/;
                                            for (var i = 0; i < arr.length; i++) {
                                                if (!regex.test(arr[i])) {
                                                    return tr('rules.phonanumber.valid');
                                                }
                                            }
                                            return true;
                                        },
                                        listeners: {
                                            afterrender: function(textbox) {
                                                var viewport = textbox.up('viewport');
                                                textbox.setValue(viewport.userPhone);
                                            }
                                        }
                                    }
                                ]
                            }
                        ]
                    },
                    {
                        xtype: 'combo',
                        itemId: 'ntfAddAction',
                        fieldLabel: tr('rules.additionalaction'),
                        store: Ext.create('Ext.data.ArrayStore', {
                            fields: ['type', 'text'],
                            data: {
                                'items': [
                                    {type: 'none', text: tr('rules.additionalaction.no')},
                                    {type: 'block', text: tr('rules.additionalaction.objectblocking')}
                                ]
                            },
                            proxy: {
                                type: 'memory',
                                reader: {
                                    type: 'json',
                                    root: 'items'
                                }
                            }
                        }),
                        allowBlank: false,
                        editable: false,
                        queryMode: 'local',
                        valueField: 'type',
                        displayField: 'text',
                        value: 'none',
                        labelWidth: 180,
                        padding: '-2 8 4 8'
                    }
                ]
            }
        ]
    }],
    bbar: [
        '->',
        {
            xtype: 'button',
            itemId: 'btnAddNtfRule',
            text: tr('main.ok'),
            icon: 'images/ico16_okcrc.png',
            handler: function(btn) {
                var wnd = btn.up('addnotwnd');
                wnd.down("#searchField").setValue("");
                var notificationRule = wnd.processInputData();
                if (notificationRule) {
                    console.log('New notification rule = ', notificationRule);
                    
                    if (notificationRule.action === 'block' && wnd.up('viewport').down('mapobjectlist').commandPasswordNeeded) {
                        var msgPromt = Ext.MessageBox;
                        msgPromt.prompt(
                            tr('rules.additionalaction.password'),
                            tr('rules.additionalaction.password.enter')+':',
                            function(btn, text) {
                                if (btn === 'ok') {
                                    notificationRule.blockpasw = text;
                                    notificationRules.addNotificationRule(notificationRule, function(resp){wnd.serverAddCallback.call(wnd, resp);});
                                    msgPromt.textField.inputEl.dom.type = 'text';
                                }
                            }
                        );
                        msgPromt.textField.inputEl.dom.type = 'password';
                    } else {
                        notificationRules.addNotificationRule(notificationRule, function(resp){wnd.serverAddCallback.call(wnd, resp);});
                    }
                }
            }
        },
        {
            xtype: 'button',
            itemId: 'btnUpdNtfRule',
            text: tr('main.ok'),
            icon: 'images/ico16_okcrc.png',
            hidden: true,
            handler: function(btn) {
                var wnd = btn.up('addnotwnd');
                wnd.down("#searchField").setValue("");
                var notificationRule = wnd.processInputData();

                if (notificationRule) {
                    console.log('Update notification rule = ', notificationRule);
                    
                    if (notificationRule.action === 'block' && wnd.up('viewport').down('mapobjectlist').commandPasswordNeeded) {
                        var msgPromt = Ext.MessageBox;
                        msgPromt.prompt(
                            tr('rules.additionalaction.password'),
                                tr('rules.additionalaction.password.enter')+':',
                            function(btn, text) {
                                if (btn === 'ok') {
                                    notificationRule.blockpasw = text;
                                    notificationRules.updNotificationRule(notificationRule);
                                    wnd.serverUpdCallback(notificationRule);
                                    msgPromt.textField.inputEl.dom.type = 'text';
                                }
                            }
                        );
                        msgPromt.textField.inputEl.dom.type = 'password';
                    } else {
                        notificationRules.updNotificationRule(notificationRule);
                        wnd.serverUpdCallback(notificationRule);
                    }
                }
            }
        },
        {
            xtype: 'button',
            text: tr('main.cancel'),
            icon: 'images/ico16_cancel.png',
            handler: function(btn) {
                var wnd = btn.up('addnotwnd');
                wnd.close();
            }
        }
    ],
    initComponent: function() {
        this.callParent(arguments);
        
        this.on('boxready', function(wnd) {
            var viewport = wnd.up('viewport');
            var objectsList = wnd.down('grid'),
                cbGeozones = wnd.down('#ntfGeoZCh');
            
            objectsList.getStore().add(viewport.down('mapobjectlist').roData);
            cbGeozones.getStore().add(viewport.geozArray);
            
            if (wnd.editRecord) {
                wnd.loadRecordData();
            }
        });
    },

    //---------
    // Функциии
    //---------
    serverAddCallback: function(resp) {
        console.log('Add notification response ', resp);
        console.log('Current component = ', this);
        if (resp && !resp.exists) {
            var wndGrid = this.up('viewport').down('editnotgrid');
            if (wndGrid) {
                wndGrid.getStore().loadRawData([resp], true);
            }
            this.close();
        } else {
            Ext.MessageBox.show({
                title: tr('rules.exists'),
                msg: tr('rules.exists.msg'),
                icon: Ext.MessageBox.ERROR,
                buttons: Ext.Msg.OK
            });
        }
    },
    serverUpdCallback: function(notificationRule) {
        if (this.editRecord) {
            this.editRecord.set(notificationRule);
            if (!notificationRule.allobjects) {
                this.editRecord.set('allobjects', false);
            }
        }
        this.close();
    },
    processInputData: function() {
        var wnd = this,
            type = wnd.down('#ntfType').getValue(),
            name = wnd.down('#ntfName').getValue(),
            isAllObjects = wnd.down('#ntfAllObjects').getValue(),
            isMessage = wnd.down('#ntfAlarmMessage').getValue(),
            isEmail = wnd.down('#ntfAlarmEmail').getValue(),
            isPhone = wnd.down('#ntfAlarmPhone').getValue(),
            message = wnd.down('#ntfMessageMask').getValue();

        if (wnd.down('grid').getSelectionModel().getCount() === 0 && !isAllObjects) {
            Ext.MessageBox.show({
                title: tr('rules.noobjects'),
                msg: tr('rules.noobjects.msg'),
                icon: Ext.MessageBox.WARNING,
                buttons: Ext.Msg.OK
            });
            return false;
        }

        if (!name) {
            Ext.MessageBox.show({
                title: tr('rules.nooname'),
                msg: tr('rules.nooname.msg'),
                icon: Ext.MessageBox.WARNING,
                buttons: Ext.Msg.OK
            });
            return false;
        }
        
        var validation = Seniel.view.notifications.RulesTypes.validateRuleParams(type, wnd);
        if (validation !== true) {
            Ext.MessageBox.show({
                title: validation.errorTitle,
                msg: validation.errorMsg,
                icon: Ext.MessageBox.WARNING,
                buttons: Ext.Msg.OK
            });
            return false;
        }

        if (!isMessage && !isEmail && !isPhone) {
            Ext.MessageBox.show({
                title: tr('rules.nonotifyway'),
                msg: tr('rules.nonotifyway.msg'),
                icon: Ext.MessageBox.WARNING,
                buttons: Ext.Msg.OK
            });
            return false;
        }

        if (isEmail && wnd.down('#ntfEmail').getValue() === '') {
            Ext.MessageBox.show({
                title: tr('rules.noemal'),
                msg: tr('rules.noemal.msg'),
                icon: Ext.MessageBox.WARNING,
                buttons: Ext.Msg.OK
            });
            return false;
        }

        if (isEmail && !wnd.down('#ntfEmail').validate()) {
            Ext.MessageBox.show({
                title: tr('rules.invalidemail'),
                msg: tr('rules.invalidemail.msg'),
                icon: Ext.MessageBox.WARNING,
                buttons: Ext.Msg.OK
            });
            return false;
        }

        if (isPhone && wnd.down('#ntfPhone').getValue() === '') {
            Ext.MessageBox.show({
                title: tr('rules.nophone'),
                msg: tr('rules.nophone.msg'),
                icon: Ext.MessageBox.WARNING,
                buttons: Ext.Msg.OK
            });
            return false;
        }

        if (isPhone && !wnd.down('#ntfPhone').validate()) {
            Ext.MessageBox.show({
                title: tr('rules.invalidphone'),
                msg: tr('rules.invalidphone.msg'),
                icon: Ext.MessageBox.WARNING,
                buttons: Ext.Msg.OK
            });
            return false;
        }
        
        var ntr = {
            name: name,
            type: type,
            messagemask: (message)?(message):(wnd.down('#ntfType').maskArray[type]),
            showmessage: isMessage,
            email: (isEmail)?(wnd.down('#ntfEmail').getValue()):(''),
            phone: (isPhone)?(wnd.down('#ntfPhone').getValue()):(''),
            action: (type === 'ntfSlpr')?('none'):(wnd.down('#ntfAddAction').getValue())
        };

        if (isAllObjects) {
            ntr.allobjects = true;
        } else {
            ntr.objects = new Array();
            Ext.Array.each(wnd.down('grid').getSelectionModel().getSelection(), function(rec) {
                ntr.objects.push(rec.get('uid'));
            });
        }
        
        ntr.params = Seniel.view.notifications.RulesTypes.getRuleParams(type, wnd);
        
        return ntr;
    },
    loadRecordData: function() {
        var wnd = this,
            rec = wnd.editRecord,
            objectsList = wnd.down('grid');

        wnd.down('#ntfName').setValue(rec.get('name'));
        wnd.down('#ntfName').disable();
        wnd.down('#ntfType').setValue(rec.get('type'));

        if (rec.get('allobjects')) {
            wnd.down('#ntfAllObjects').setValue(true);
        } else {
            wnd.down('#ntfAllObjects').setValue(false);

            var selObjs = rec.get('objects'),
                selRecs = new Array(),
                tmpRec;
            for (var i in selObjs) {
                tmpRec = objectsList.getStore().findRecord('uid', selObjs[i]);
                if (tmpRec) {
                    selRecs.push(tmpRec);
                }
            }
            objectsList.getSelectionModel().select(selRecs);
        }

        if (rec.get('showmessage')) {
            wnd.down('#ntfAlarmMessage').setValue(true);
        } else {
            wnd.down('#ntfAlarmMessage').setValue(false);
        }

        if (rec.get('email') === '') {
            wnd.down('#ntfAlarmEmail').setValue(false);
        } else {
            wnd.down('#ntfAlarmEmail').setValue(true);
            wnd.down('#ntfEmail').setValue(rec.get('email'));
        }

        if (rec.get('phone') === '') {
            wnd.down('#ntfAlarmPhone').setValue(false);
        } else {
            wnd.down('#ntfAlarmPhone').setValue(true);
            wnd.down('#ntfPhone').setValue(rec.get('phone'));
        }
        
        if (rec.get('action')) {
            wnd.down('#ntfAddAction').setValue(rec.get('action'));
        }
        
        Seniel.view.notifications.RulesTypes.setRuleParams(rec.get('type'), rec.get('params'), wnd);
    
        if (rec.get('messagemask')) {
            wnd.down('#ntfType').maskArray[rec.get('type')] = rec.get('messagemask');
            wnd.down('#ntfMessageMask').setValue(rec.get('messagemask'));
        } else {
            wnd.down('#ntfMessageMask').setValue(wnd.down('#ntfType').maskArray[rec.get('type')]);
        }

        wnd.down('#ntfVeloParams').hide();
        wnd.down('#'+rec.get('type')+'Params').show();

        wnd.down('#btnAddNtfRule').hide();
        wnd.down('#btnUpdNtfRule').show();
    }
});
/**
 * Created by IVAN on 21.10.2015.
 */
Ext.define('Billing.view.support.SupportPanel', {
    extend: 'WRExtUtils.WRGrid',
    alias: 'widget.supportpanel',
    //title:  'Техподдержка',
    title: '<div style="display:inline; padding-right:5px">Техподдержка</div><div id="unreadSupportMessages" style="display:none; background-color:deepskyblue; color:white; padding:5px"></div>',
    requires: [
        'Billing.view.support.SupportEmailNotificationWindow'
        //'Billing.view.object.GroupOfObjectsForm'
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
    storeName: 'EDS.store.SupportRequestEDS',
    itemType: 'objects',
    dockedToolbar: [/*'add',*/ 'remove', 'refresh', 'search', 'fill'/*,'gridDataExport'*/],
    initComponent: function () {
        var self = this;
        var hideRule = !self.viewPermit;
        var getSearchCriteriaList = function (searchString, criteriaList) {
            var searchCriteriaList = [];
            for (var i in criteriaList) {
                if (criteriaList[i].criteriaDisplay.indexOf(searchString.toString().toLowerCase()) > -1)
                    searchCriteriaList.push(criteriaList[i].criteriaValue)
            }
            return searchCriteriaList
        };
        Ext.apply(this, {
            specialFilters: {
                'category': {
                    'filterFn': function (record, searchString) {
                        var criteriaList = [
                            {criteriaDisplay: 'вопросы по оборудованию', criteriaValue: 'equipment'},
                            {criteriaDisplay: 'вопросы по программе', criteriaValue: 'program'},
                            {criteriaDisplay: 'финансовые вопросы', criteriaValue: 'finance'}
                        ];
                        return Ext.Array.contains(getSearchCriteriaList(searchString, criteriaList), record.get("category"))
                    }
                },
                'status': {
                    'filterFn': function (record, searchString) {
                        var criteriaList = [
                            {criteriaDisplay: 'новая', criteriaValue: 'new'},
                            {criteriaDisplay: 'открыта', criteriaValue: 'open'},
                            {criteriaDisplay: 'закрыта', criteriaValue: 'close'}
                        ];
                        return Ext.Array.contains(getSearchCriteriaList(searchString, criteriaList), record.get("status"))
                    }
                },
                'dateTime': {
                    'filterFn': function (record, searchString) {
                        var date = new Date(record.get("dateTime"));
                        var foramtedDateTime = Ext.Date.format(date, "d.m.Y H:i:s");
                        return foramtedDateTime.toString().toLowerCase().indexOf(searchString.toString().toLowerCase()) > -1
                    }
                }
            },
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
                    header: 'Код заявки',
                    dataIndex: '_id',
                    width: 160,
                    resizable: true,
                    filter: {
                        type: 'string'
                    }
                },
                {
                    header: 'Дата',
                    xtype: 'datecolumn',
                    format: "d.m.Y H:i:s",
                    width: 170,
                    sortable: true,
                    dataIndex: 'dateTime',
                    filter: {
                        type: 'special'
                    },
                    summaryType: 'count',
                    summaryRenderer: function (value, summaryData, dataIndex) {
                        return Ext.String.format('<b>Всего позиций: {0} </b>', value);
                    }
                },
                {
                    header: 'Пользователь',
                    width: 170,
                    sortable: true,
                    dataIndex: 'userName',
                    filter: {
                        type: 'string'
                    },
                    renderer: function (val, metaData, rec) {
                        metaData.tdAttr = 'style="cursor: pointer !important;" title="' + val + '"';
                        return val
                    }
                },
                {
                    header: 'Телефон',
                    width: 120,
                    sortable: true,
                    dataIndex: 'userPhone',
                    filter: {
                        type: 'string'
                    }
                },
                {
                    header: 'Категория',
                    width: 160,
                    sortable: true,
                    dataIndex: 'category',
                    filter: {
                        type: 'special'
                    },
                    renderer: function (val, metaData, rec) {
                        metaData.tdAttr = 'title="' + val + '"'
                        switch (val) {
                            case "equipment": {
                                val = 'Вопросы по оборудованию';
                                break;
                            }
                            case "program": {
                                val = 'Вопросы по программе';
                                break;
                            }
                            case "finance": {
                                val = 'Финансовые вопросы';
                                break;
                            }
                        }
                        return val
                    },
                },
                {
                    header: 'Вопрос',
                    width: 210,
                    sortable: true,
                    dataIndex: 'question',
                    filter: {
                        type: 'string'
                    }
                },
                {
                    header: 'Содержание',
                    flex: 2,
                    sortable: true,
                    dataIndex: 'description',
                    filter: {
                        type: 'string'
                    },
                    renderer: function (val, metaData, rec) {
                        metaData.tdAttr = 'title="' + val + '"'
                        return val
                    },
                    hidden: hideRule
                },
                {
                    header: 'Статус',
                    width: 70,
                    sortable: true,
                    dataIndex: 'status',
                    filter: {
                        type: 'special'
                    },
                    renderer: function (val, metaData, rec) {
                        var res = "";
                        switch (val) {
                            case "new" : {
                                res = '<span style="color:greenyellow;">' + "Новая" + '</span>';
                                break;
                            }
                                ;
                            case "open" : {
                                res = '<span style="color:green;">' + "Открыта" + '</span>';
                                break;
                            }
                                ;
                            case "close" : {
                                res = '<span style="color:red;">' + "Закрыта" + '</span>';
                                break;
                            }
                                ;
                            default : {
                                res = val
                                break;
                            }
                                ;
                        }
                        return res;
                    }
                },
                {
                    text: '<img src="images/ico16_msghist.png"/>',
                    menuText: 'Прочтение',
                    tooltip: 'Прочтение',
                    tooltipType: 'title',
                    sortable: true,
                    resizable: false,
                    dataIndex: 'supportRead',
                    width: 38,
                    renderer: function (val, metaData, rec) {
                        metaData.tdAttr = 'style="cursor: pointer !important;"'
                        if (val === true) return '<img src="images/ico16_msghist.png" alt="" title="Прочитано" />'
                        else  return '<img src="images/ico16_msghist_yel.png" alt="" title="Непрочитано" />'
                    }
                },
                {
                    xtype: 'actioncolumn',
                    menuText: 'Заявка',
                    hidden: hideRule,
                    header: 'Заявка',
                    width: 50,
                    dataIndex: 'name',
                    sortable: false,
                    align: 'center',
                    items: [
                        {
                            icon: 'images/ico16_eventsmsgs.png',
                            disabled: hideRule,
                            tooltip: 'Просмотр заявки',
                            handler: function (view, rowIndex, colIndex, item, e, rec) {
                                self.showTicketDataForm(rec)
                            }
                        }
                    ]
                },
                {
                    hideable: false,
                    menuDisabled: true,
                    sortable: false,
                    flex: 1
                }
            ],
            listeners: {
                cellclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
                    var dataIndex = self.columnManager.columns[cellIndex].dataIndex
                    switch (dataIndex) {
                        case  "supportRead": {
                            var readStatus = record.get('supportRead');
                            var ticketId = record.get("_id");
                            //var text = "Пометить прочитанной";
                            //if (readStatus) text = "Пометить непрочитанной";
                            //Ext.MessageBox.confirm('Изменить статус прочтения', text, function (button) {
                            //    if (button === 'yes') {
                            supportRequestEDS.changeTicketReadStatus(ticketId, !readStatus, function (readStatus, e) {
                                if (!e.status) {
                                    Ext.MessageBox.show({
                                        title: 'Произошла ошибка',
                                        msg: e.message,
                                        icon: Ext.MessageBox.ERROR,
                                        buttons: Ext.Msg.OK
                                    });
                                }
                                else self.refresh();
                            })
                            //    }
                            //});
                        }
                    }
                },
                celldblclick: function (/* Ext.view.Table */ self0, /*HTMLElement*/ td, /*Number*/ cellIndex, /*Ext.data.Model*/ record, /*HTMLElement*/ tr, /*Number*/ rowIndex, /*Ext.EventObject*/ e, /*Object*/ eOpts) {
                    var dataIndex = self.columnManager.columns[cellIndex].dataIndex
                    self.showTicketDataForm(record);
                }
            },
            onSelectChange: function (selModel, selections) {
                if (hideRule)
                    this.down('#delete').setDisabled(true);
                else
                    this.down('#delete').setDisabled(selections.length === 0);
            }
        });
        this.callParent();
        self.down('toolbar').insert(5, {
            icon: "images/ico16_eventsmsgs.png",
            text: 'Уведомления на E-mail',
            handler: function () {
                var existingWindow = WRExtUtils.createOrFocus('supportemmailnotifwnd', 'Billing.view.support.SupportEmailNotificationWindow', {});
                existingWindow.show();
                //return existingWindow;
            }
        });
        supportRequestEDS.getUnreadTicketsCount(function (res) {
            console.log("init unreadTicketsCount=", res);
            self.updateUnreadTicketsCount(res)
        })
    },
    onDeleteClick: function () {
        console.log("onDeleteClick");
        var self = this;
        var selection = this.getView().getSelectionModel().getSelection();//[0];

        if (selection) {
            var store = this.store;
            Ext.MessageBox.confirm('Удаление элемента', 'Вы уверены, что хотите удалить ' + selection.length + ' записей?', function (button) {
                if (button === 'yes') {
                    supportRequestEDS.remove(Ext.Array.map(selection, function (m) {
                        return m.get("_id");
                    }), function (r, e) {
                        if (!e.status) {
                            Ext.MessageBox.show({
                                title: 'Произошла ошибка',
                                msg: e.message,
                                icon: Ext.MessageBox.ERROR,
                                buttons: Ext.Msg.OK
                            });
                        }
                        else self.refresh();
                    })
                }
            });
        }
    },
    onAddClick: function (type) {
        console.log("onAddClick")
        //var rec = Ext.create('ObjectsGroup');
        //var form = this.showGroupOfObjectsForm(rec);
    },
    showTicketDataForm: function (rec) {
        console.log("show ticket data")
        var self = this
        var id = rec.get("_id");
        if (id) {
            var existingWindow = WRExtUtils.createOrFocus('TicketWindow' + id, 'Billing.view.support.TicketWindow', {
                title: 'Заявка № "' + id + '"',
                ticketId: id
                //btnConfig: {
                //    icon: 'images/cars/car_001_blu_24.png',
                //    text: tr('main.groupofobjects')+' "' + record.get('name') + '"'
                //}
            });
            if (existingWindow.justCreated) {
                existingWindow.show();
                existingWindow.onLoad(rec.get("_id"));
                existingWindow.on('close', function (args) {
                    self.refresh();
                });
            }
            console.log("Просмотр заявки")
        }
    },
    updateUnreadTicketsCount: function (unreadTicketsCount) {
        var unreadSupportMessagesDiv = Ext.fly('unreadSupportMessages')
        if (unreadTicketsCount > 0) {
            unreadSupportMessagesDiv.setHTML(unreadTicketsCount)
            unreadSupportMessagesDiv.setDisplayed("inline")
        }
        else {
            unreadSupportMessagesDiv.setHTML(unreadTicketsCount)
            unreadSupportMessagesDiv.setDisplayed("none")
        }
    },
    updateData: function (data) {
        if (data.eventType == "unreadSupportTickets") {
            console.log("unreadSupportTickets count=", data.unreadTicketsCount)
            var unreadTicketsCount = data.unreadTicketsCount
            this.updateUnreadTicketsCount(unreadTicketsCount)
            this.refresh()
        }
    },
})
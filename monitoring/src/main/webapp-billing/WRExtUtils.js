WRExtUtils = {

    createOrFocus:function (idname, type, bodyDescr) {

        bodyDescr['id'] = idname

        var existingWindow = Ext.getCmp(idname)
        if (!existingWindow) {
            existingWindow = Ext.create(type, bodyDescr)
            existingWindow.justCreated = true
        }
        else
        {
            existingWindow.focus()
            existingWindow.justCreated = false
        }

        return existingWindow
    },

    makeBalanceEntryTypePicker: function (me, entity) {
        return {
            selectedRecords : [],
            xtype: 'combobox',
            store: Ext.create('EDS.store.BalanceEntryTypes', {
                listeners: {
                    'beforeload': function (store) {
                        store.getProxy().setExtraParam('itemType', entity)
                    }
                }
            }),
            valueField: 'type',
            displayField: 'type',
            editable: false,
            multiSelect: true,
            listeners: {
                beforeselect: function (combo, record) {
                    var recs = combo.selectedRecords;
                    recs.push(record.get("type"));
                   // console.log("typefilter is ");
                   // console.log(recs);
                    me.getStore().getProxy().setExtraParam('typeFilter', recs);
                    me.getStore().load();
                },
                beforedeselect: function (combo, record) {
                    var recs = combo.selectedRecords;
                   // console.log("Record is ");
                   // console.log(record);
                   // console.log("recs are");
                   // console.log(recs);
                    var index = recs.indexOf(record.get("type"));
                 //   console.log('index of deselected record ' + index);
                    recs.splice(index, 1);
                 //   console.log("typefilter is ");
                //    console.log(recs);
                    me.getStore().getProxy().setExtraParam('typeFilter', recs);
                    me.getStore().load();
                }
            }
        }
    }



};

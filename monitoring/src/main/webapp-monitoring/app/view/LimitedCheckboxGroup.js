/**
 * Created by ivan on 19.10.16.
 */

// TODO - параметризовать, выделить общего предка с WWRRadioGroup
Ext.define('Seniel.view.LimitedCheckboxGroup', {
    extend: 'Ext.form.CheckboxGroup',
    alias: 'widget.limitedchkgroup',
    cbgName:'',
    initItemsConfig: [],
    initComponent: function() {
        console.log("Init limited checkbox group");
        this.callParent(arguments);
        var self = this;
        if (this.itemId) {
            this.cbgName = this.itemId + '-' + this.id;
        } else {
            this.cbgName = this.id;
        }

        if (this.initItemsConfig && this.initItemsConfig.length > 0) {
            console.log("This.initItemsConfig = " + this.initItemsConfig);
            for (var i = 0; i < this.initItemsConfig.length; i++) {
                this.initItemsConfig[i].name = this.cbgName;

                var chk = Ext.create('Ext.form.field.Checkbox', this.initItemsConfig[i]);
                console.log("Add ");
                console.log(chk);
                this.add([
                    chk
                ]);
            }
        }
        this.full = false; // Если только изначально не чекнутые

        Ext.apply(this, {
            listeners: {
                change: function (cBox, newValue, oldValue) {
                    self.checkLimit();
                }
            }
        });

    },

    checkLimit: function () {
        var self = this;
        var limit = this.checkedLimit;
        var boxes = this.getChecked();
        if(boxes.length == limit) {
            Ext.Array.forEach(self.getUnchecked(), function(cb) {
                cb.disable();
            });
            self.full = true;
        }
        else {
            if(self.full)
                Ext.Array.forEach(self.items.items, function(cb) {
                    cb.enable();
                });
        }
    },

    setLimit: function(limit) {
        this.checkedLimit = limit;
        this.checkLimit();
    },
    getRealValues:  function() {
        if (this.cbgName) {
            return Ext.Array.map(this.getChecked(), function (el) {
                return el.getSubmitValue();
            });
        } else {
            return undefined;
        }
    },
    setRealValue: function(data) {
        var val = this.getValue();
        val[this.cbgName] = data;
        this.setValue(val);
    },
    setAllToFalse: function() {
        var items = this.items.items;
        for (var i = 0; i < items.length; i++) {
            items[i].setValue(false);
        }
    },

    getUnchecked: function() {
        var items = this.items.items;
        var checked = this.getChecked();
        return Ext.Array.filter(items, function(cb) {
            for(var i = 0; i < checked.length; ++i) {
                if(checked[i] === cb)
                    return false;
            }
            return true;
        });
    }

});
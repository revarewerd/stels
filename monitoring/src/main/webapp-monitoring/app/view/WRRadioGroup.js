Ext.define('Seniel.view.WRRadioGroup', {
    extend: 'Ext.form.RadioGroup',
    alias: 'widget.wrradiogroup',
    rgName: '',
    initItemsConfig: [],
    initComponent: function() {
        this.callParent(arguments);
        
        if (this.itemId) {
            this.rgName = this.itemId + '-' + this.id;
        } else {
            this.rgName = this.id;
        }
        
        if (this.initItemsConfig && this.initItemsConfig.length > 0) {
            for (var i = 0; i < this.initItemsConfig.length; i++) {
                this.initItemsConfig[i].name = this.rgName;
                this.add([
                    Ext.create('Ext.form.field.Radio', this.initItemsConfig[i])
                ]);
            }
        }
    },

    //---------
    // Функциии
    //---------
    getRealValue:  function() {
        if (this.rgName) {
            return this.getValue()[this.rgName];
        } else {
            return undefined;
        }
    },
    setRealValue: function(data) {
        var val = this.getValue();
        val[this.rgName] = data;
        this.setValue(val);
    },
    setAllToFalse: function() {
        var items = this.items.items;
        for (var i = 0; i < items.length; i++) {
            items[i].setValue(false);
        }
    }
});
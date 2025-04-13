Ext.define('Seniel.view.mapobject.ImagesView', {
    extend: 'Ext.view.View',
    requires: [
        'EDS.store.ObjectImages'
    ],
    alias: 'widget.objimgview',
    tpl: [
        '<tpl for=".">',
            '<div class="object-images-thumb"><img width="{size}" height="{size}" src="{src}" alt="{name}" /></div>',
        '</tpl>'
    ],
    trackOver: true,
    overItemCls: 'x-item-over',
    itemSelector: 'div.object-images-thumb',
    emptyText: tr('imagesview.noimages'),
    filterView: function(size, isMarker, isRotate) {
        this.getStore().clearFilter();
        
        if (size === undefined) {
            size = 32;
        }
        
        if (isMarker) {
            this.getStore().filter([
                {property: 'size', value: size},
                {property: 'name', value: /mrk/}
            ]);
        } else if (isRotate) {
            this.getStore().filter([
                {property: 'size', value: size},
                {property: 'name', value: /rot/}
            ]);
        } else {
            this.getStore().filter([
                {property: 'size', value: size},
                {property: 'name', value: /\d\d\d_..._\d\d/}
            ]);
        }
        
        if (this.selectedItem && this.getStore().getById(this.selectedItem.get('name'))) {
            this.getSelectionModel().select([this.selectedItem]);
        }
//        this.doComponentLayout();
    },
    listeners: {
        itemclick: function(view, rec) {
            view.up('objsetwnd').down('toolbar[dock="bottom"] button[itemId="btnApplySettings"]').enable();
            view.selectedItem = rec;
            var form = view.up('panel[itemId="objMapImage"]').down('form');
            form.down('displayfield').setValue(rec.get('src'));
            form.updateRecord();
            var rec = form.getRecord(),
                rg = form.down('radiogroup');
            rec.set('imgSize', rg.getValue()[rg.rgName]);
            console.log('Form record = ', form.getRecord().data);
        }
    },
    selectedItem: null,
    initComponent: function() {
        var imgView = this;
        var imgStore = new EDS.store.ObjectImages({
            autoLoad: true,
            listeners: {
                load: function(store, records) {
                    var item = imgView.initItem;
                    if (item) {
                        var name = item.src.substring(12),
                            rec = store.getById(name);
                        if (rec) {
                            imgView.selectedItem = rec;
                        }
                        imgView.filterView(item.size, item.isMarker, item.isRotate);
                    } else {
                        var rec = store.getById('car_rot_swc_32.png');
                        if (rec) {
                            imgView.selectedItem = rec;
                        }
                        imgView.filterView(32, false, true);
                    }
                }
            }
        });
        Ext.apply(this, {
            store: imgStore
        });
        this.callParent();
    }
});
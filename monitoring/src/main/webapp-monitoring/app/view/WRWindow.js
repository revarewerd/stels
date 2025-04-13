Ext.define('WindowControlButton', {
    extend: 'Ext.button.Button',
    alias: 'widget.wndctrlbtn',
    enableToggle: true,
    scale: 'medium',
    toggleHandler: function (btn, pressed) {                                       
        if (pressed) {
            this.window.show();
        } else if (!pressed && this.window.status === 'active') {
            this.window.hide();
        }
    }
});

Ext.define('Seniel.view.WRWindow', {
    extend: 'Ext.window.Window',
    icon: 'images/ico16_blank.png',
    title: 'Заголовок окна',
    closable: true,
    minimizable: true,
    maximizable: true,
    bodyStyle: 'padding: 0px;',
    btnConfig: {
        icon: 'images/ico24_blank.png',
        text: 'Заголовок окна'
    },
    constrain: true,
    width: 800,
    height: 600,
    listeners: {
        afterrender: function(wnd) {
            console.log('AFTERRENDER event');
            wnd.createToolbarButton();
            
            var viewport = wnd.up('viewport'),
                maincontainer = viewport.down('container[itemId="maincontainer"]'),
                maxW = viewport.getWidth(),
                maxH = maincontainer.getHeight();
            var wndW = (maxW > wnd.getWidth())?(wnd.getWidth()):(maxW);
            var wndH = (maxH > wnd.getHeight())?(wnd.getHeight()):(maxH);
            wnd.setWidth(wndW);
            wnd.setHeight(wndH);
        },
        activate: function(wnd) {
            if (!wnd.animateTarget) {
                wnd.animateTarget = wnd.ctrlbtn;
            }
            wnd.ctrlbtn.toggle(true);
            wnd.status = 'active';
        },
        deactivate: function(wnd) {
            wnd.status = 'inactive';
            wnd.ctrlbtn.toggle(false);
        },
        minimize: function(wnd) {
            wnd.ctrlbtn.toggle(false);
        },
        hide: function(wnd) {
            wnd.ghostPanel.toBack();
        },
        beforeclose: function(wnd) {
            wnd.animateTarget = null;
        },
        close: function(wnd) {
            var habtn = wnd.toolbar.items.first();
            wnd.toolbar.remove(wnd.ctrlbtn);
            if (wnd.toolbar.items.getCount() === 2) {
                habtn.disable();
            }
            
            var maincontainer = wnd.up('container[itemId="maincontainer"]');
            if (maincontainer.floatingItems.items.length) {
                for (var i = 0; i < maincontainer.floatingItems.items.length; i++) {
                    if (wnd === maincontainer.floatingItems.items[i]) {
                        delete maincontainer.floatingItems.items.splice(i, 1);
                    }
                }
            }
        }
    },

    //---------
    // Функциии
    //---------
    createToolbarButton:  function() {
        var viewport = Ext.ComponentQuery.query('viewport')[0],
            toolbar = viewport.down('toolbar[itemId="reporttoolbar]'),
            habtn = toolbar.items.first();
        
        this.ctrlbtn = Ext.create('WindowControlButton', this.btnConfig);
        this.ctrlbtn.window = this;
        this.toolbar = toolbar;                          
        toolbar.add([this.ctrlbtn]);
        if (habtn.disabled) {
            habtn.enable();
        }
    }
});
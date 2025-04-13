Ext.define('Seniel.view.mobile.MessageBox', {
    extend: 'Ext.window.Window',
    alias: 'widget.mapcontainer',
    cls: 'mobile-message-box',
    width: '60%',
    minHeight: 100,
    border: false,
    header: false,
    modal: true,
    closable: false,
    maximizable: false,
    minimizable: false,
    resizable: false,
    layout: 'anchor',
    items: [
        {
            xtype: 'displayfield',
            itemId: 'displayMsgText',
            padding: '4 8 0 8',
            anchor: '100%'
        },
        {
            xtype: 'textfield',
            itemId: 'inputMsgPasw',
            cls: 'mobile-panel-toolbar',
            inputType: 'password',
            padding: '0 8 8 16',
            anchor: '100%',
            hidden: true
        },
        {
            xtype: 'textfield',
            itemId: 'inputMsgText',
            cls: 'mobile-panel-toolbar',
            padding: 8,
            anchor: '100%',
            hidden: true
        }
    ],
    bbar: [
        '->',
        {
            icon: 'images/ico32_okcrc.png',
            text: tr('main.ok'),
            itemId: 'btnMsgOk',
            cls: 'mobile-toolbar-button',
            scale: 'large',
            handler: function(btn) {
                var wnd = btn.up('window');
                var paswEl = wnd.down('#inputMsgPasw');
                var textEl = wnd.down('#inputMsgText');
                var text = null;
                if (wnd.callbackFunc) {
                    if (paswEl.isVisible()) {
                        text = (paswEl.getValue())?(paswEl.getValue()):(null);
                    } else if (textEl.isVisible()) {
                        text = (textEl.getValue())?(textEl.getValue()):(null);
                    }
                    wnd.callbackFunc('Ok', text);
                }
                
                if (!paswEl.isHidden()) {
                    paswEl.hide();
                    paswEl.setValue('');
                }
                if (!textEl.isHidden()) {
                    textEl.hide();
                    textEl.setValue('');
                }
                wnd.hide();
            }
        },
        {
            icon: 'images/ico32_cancel.png',
            text: tr('main.cancel'),
            itemId: 'btnMsgCancel',
            cls: 'mobile-toolbar-button',
            scale: 'large',
            handler: function(btn) {
                var wnd = btn.up('window');
                var paswEl = wnd.down('#inputMsgPasw');
                var textEl = wnd.down('#inputMsgText');
                if (wnd.callbackFunc) {
                    wnd.callbackFunc('Cancel', null);
                }
                
                if (!paswEl.isHidden()) {
                    paswEl.hide();
                    paswEl.setValue('');
                }
                if (!textEl.isHidden()) {
                    textEl.hide();
                    textEl.setValue('');
                }
                wnd.hide();
            }
        }
    ],
    callbackFunc: null,
    
    showMessage: function(html, icon, callback) {
        this.down('#displayMsgText').setValue(html);
        this.callbackFunc = (callback)?(callback):(null);
        this.down('#btnMsgCancel').hide();
        this.show();
    },
    showConfirm: function(html, icon, callback) {
        this.down('#displayMsgText').setValue(html);
        this.callbackFunc = (callback)?(callback):(null);
        this.down('#btnMsgCancel').show();
        this.show();
    },
    showPasswordPromt: function(html, icon, callback) {
        this.down('#displayMsgText').setValue(html);
        this.down('#inputMsgPasw').show();
        this.callbackFunc = (callback)?(callback):(null);
        this.down('#btnMsgCancel').show();
        this.show();
    },
    showTextPromt: function(html, icon, callback) {
        this.down('#displayMsgText').setValue(html);
        this.down('#inputMsgText').show();
        this.callbackFunc = (callback)?(callback):(null);
        this.down('#btnMsgCancel').show();
        this.show();
    }
});
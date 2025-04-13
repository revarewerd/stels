Ext.define('Seniel.view.WRUtils', {
    singleton: true,
    
    daysText: [tr('timeperiods.days.fform'), tr('timeperiods.days.sform'), tr('timeperiods.days.tform')],
    hoursText: [tr('timeperiods.hours.fform'), tr('timeperiods.hours.sform'), tr('timeperiods.hours.tform')],
    minutesText: [tr('timeperiods.minutes.fform'), tr('timeperiods.minutes.sform'), tr('timeperiods.minutes.tform')],
    getLastMsgText: function(lastmsg) {
        var text = Ext.Date.format(lastmsg, tr('format.extjs.datetime')) + ' (';
        var elapsed = Ext.Date.getElapsed(lastmsg);
        
        var days = Math.floor(elapsed / (24 * 3600000));
        var daysText = this.daysText[this.getWordForm(days)];
        text += (days)?(days + ' ' + daysText + ' '):('');
        
        var hours = Math.floor(elapsed / 3600000 - days * 24);
        var hoursText = this.hoursText[this.getWordForm(hours)];
        text += (hours)?(hours + ' ' + hoursText + ' '):('');
        
        var minutes = Math.ceil(elapsed / 60000 - days * 24 * 60 - hours * 60);
        var minutesText = this.minutesText[this.getWordForm(minutes)];
        text += (minutes)?(minutes + ' ' + minutesText + ' '):('');
        
        return text + tr('timeperiods.ago') + ')';
    },
    getLastMsgColor: function(lastmsg) {
        var current = new Date(),
            timegrn = Ext.Date.subtract(current, Ext.Date.MINUTE, 20),
            timeyel = Ext.Date.subtract(current, Ext.Date.HOUR, 3);

        if (lastmsg > current) {
            return {code: 'grn', name: 'green'};
        }

        if (Ext.Date.between(lastmsg, timegrn, current)) {
            return {code: 'grn', name: 'green'};
        } else if (Ext.Date.between(lastmsg, timeyel, current)) {
            return {code: 'yel', name: 'yellow'};
        } else {
            return {code: 'red', name: 'red'};
        }
    },
    getWordForm: function(n) {
        var mod = n % 100;
        if (tr('language') === 'ru') {
            if (mod > 4 && mod < 21) {
                return 2;
            } else {
                var dmod = mod % 10;
                if (dmod === 1) {
                    return 0;
                } else if (dmod > 1 && dmod < 5) {
                    return 1;
                } else {
                    return 2;
                }
            }
        } else {
            if (mod === 1) {
                return 0;
            } else {
                return 1;
            }
        }
    }
});
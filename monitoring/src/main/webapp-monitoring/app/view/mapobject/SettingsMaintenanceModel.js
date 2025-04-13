Ext.define('Seniel.view.mapobject.SettingsMaintenanceModel', {
    extend: 'Ext.data.Model',
    fields: function () {
        function allFieldsFor(prefix) {
            return [
                {name: prefix + 'Enabled', type: 'boolean', defaultValue: true},
                {name: prefix + 'RuleEnabled', type: 'boolean', defaultValue: true},
                {name: prefix + 'Until', type: 'int'},
                {name: prefix + 'Interval', type: 'int'},
                {name: prefix + 'IntervalDefault', type: 'int'}
            ]
        }

        return [].concat(
            allFieldsFor('distance'),
            allFieldsFor('motohours'),
            allFieldsFor('hours')
        )
    }()
});
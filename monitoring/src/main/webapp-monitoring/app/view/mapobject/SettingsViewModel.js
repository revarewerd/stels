Ext.define('Seniel.view.mapobject.SettingsViewModel', {
    extend: 'Ext.data.Model',
    fields: [
        {name: 'imgSource',  type: 'string', defaultValue: 'images/cars/car_rot_swc_32.png'},
        {name: 'imgMarker', type: 'boolean', defaultValue: false},
        {name: 'imgRotate', type: 'boolean', defaultValue: true},
        {name: 'imgArrow', type: 'boolean', defaultValue: true},
        {name: 'imgArrowSrc', type: 'string', defaultValue: 'arrow_sor_06.png'},
        {name: 'imgSize', type: 'int', defaultValue: 32}
    ]
});
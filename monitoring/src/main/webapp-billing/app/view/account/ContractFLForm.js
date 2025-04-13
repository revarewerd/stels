Ext.define('Billing.view.account.ContractFLForm', {
    extend: 'Ext.form.Panel',
    alias: 'widget.flform',   
    title: 'Договор ?',     
    layout: {
          type:'vbox',
          align:'stretch'
      },
      border:false,
      items:[          
           {xtype: 'tabpanel',
               border:false,
                defaults:
                    {border:false},                       
             items: [
                    {title: 'Договор',                        
                        xtype: 'form',                        
                            layout: {
                                type:'vbox',
                                align:'stretch'
                            },
                            defaultType:'textfield',
                            fieldDefaults:{
                                //vtype:'alphanum',
                                margin:'0 20 5 20',
                                labelWidth:150
                            }, 
                            items: [
                              {                                    
                                  margin: '20 20 5 20',  
                                  fieldLabel: '№ договора',
                                  //vtype:'allalpha',
                                  name:'conNumber'
                                 },
                              {   
                                  xtype: 'datefield',
                                  fieldLabel: 'Дата заключения',
                                  value:new Date(),
                                  name:'conDate',
                                  format:'d.m.Y',
                                  altFormats:'c|m/d/Y|n/j/Y|n/j/y|m/j/y|n/d/y|m/j/Y|n/d/Y|m-d-y|m-d-Y|m/d|m-d|md|mdy|mdY|d|Y-m-d|n-j|n/j'
                              },
                              {   
                                  //vtype:'allalpha
                                  fieldLabel: 'Кем заключен',                                  
                                  name:'conPerson'}, 
                               {   
                                  fieldLabel: 'Кодовое слово',
                                  name:'conCodeWord'},
                              {   margin: '0 20 20 20',
                                  xtype: 'textareafield',                                    
                                  fieldLabel: 'Примечание',
                                  name: 'conNote'}
                            ]                    
                   },
                   {title: 'Пасспортные данные',
                        xtype: 'form',                        
                            layout: {
                                type:'vbox',
                                align:'stretch'
                            },
                            defaultType:'textfield',
                            fieldDefaults:{  
                                margin: '0 20 5 20',
                                //vtype:'alphanum',
                                labelWidth:150
                            }, 
                            items: [
                                {   margin: '20 20 5 20',                                    
                                    fieldLabel: 'Дата рождения',
                                    name:'borndate'},
                                {                                        
                                    fieldLabel: 'Место рождения',
                                    name:'bornplace'},
                                {   
                                    margin: '20 20 5 20',
                                    xtype:'label',                                                                      
                                    html: '<b>Паспорт:</b>'
                                    }, 
                                {    
                                    fieldLabel: 'серия номер',
                                    name:'passportn'}, 
                                {   
                                    fieldLabel: 'выдан',
                                    name:'passpissuedby'},
                                {   margin: '0 20 20 20',
                                    fieldLabel: 'дата выдачи',
                                    name:'passpissueddate'}                               
                            ]                    
                   }, 
                    {title: 'Адрес прописки',
                        xtype: 'form',                        
                            layout: {
                                type:'vbox',
                                align:'stretch'
                            },
                            defaultType:'textfield',
                            fieldDefaults:{
                                margin: '0 20 5 20',
                                //vtype:'alphanum',
                                labelWidth:150
                            }, 
                            items: [
                                {   margin: '20 20 5 20',                                    
                                    fieldLabel: 'Страна',
                                    name:'propcountry'},
                                {                                        
                                    fieldLabel: 'Индекс',
                                    name:'propindex'},
                                {                                        
                                    fieldLabel: 'Территориальный субъект',
                                    name:'proptersubj'}, 
                                {   
                                    fieldLabel: 'Район',
                                    name:'propzone'}, 
                                {   
                                    fieldLabel: 'Горд, село, деревня',
                                    name:'propcity'},
                                {   
                                    fieldLabel: 'Улица',
                                    name:'propstreet'},
                                {   
                                    fieldLabel: 'Дом',
                                    name:'prophouse'},
                                {   margin: '0 20 20 20',
                                    fieldLabel: 'Квартира',
                                    name:'propflat'}
                            ]                    
                   }, 
                    {title: 'Адрес проживания',
                        xtype: 'form', 
                            layout: {
                                type:'vbox',
                                align:'stretch'
                            },
                            defaultType:'textfield',
                            fieldDefaults:{
                                margin: '0 20 5 20',
                                labelWidth:150
                            }, 
                            items: [
                                {   margin: '20 20 5 20',                                    
                                    fieldLabel: 'Страна',
                                    name:'prozhcountry'},
                                {                                       
                                    fieldLabel: 'Индекс',
                                    name:'prozhindex'},
                                {                                       
                                    fieldLabel: 'Территориальный субъект',
                                    name:'prozhtersubj'}, 
                                {    
                                    fieldLabel: 'Район',
                                    name:'prozhzone'}, 
                                {   
                                    fieldLabel: 'Горд, село, деревня',
                                    name:'prozhcity'},
                                {   
                                    fieldLabel: 'Улица',
                                    name:'prozhstreet'},
                                {   
                                    fieldLabel: 'Дом',
                                    name:'prozhhouse'},
                                {   margin: '0 20 20 20',
                                    fieldLabel: 'Квартира, офис',
                                    name:'prozhflat'}]
                        },                    
                   ]}
    ]   
})
Ext.define('ContractFL', {
    extend: 'Ext.data.Model',
    fields: [            
            '_id', //'accountid',
            //Вкладка Договор
            'conNumber','conDate','conPerson','conCodeWord','conNote',
            //'conPlan','conPaymetWay','conPaymentRegular','conAccount'
            //Вкладка Паспортные данные
            'borndate','bornplace','passportn','passpissuedby','passpissueddate',
            //Вкладка Адрес прописки
            'propcountry','propindex','proptersubj','propzone','propcity','propstreet',
            'prophouse','propflat',
            //Вкладка Адрес проживания
            'prozhcountry','prozhindex','prozhtersubj','prozhzone','prozhcity','prozhstreet',
            'prozhhouse','prozhflat'           
            ],
    idProperty: '_id'  
});


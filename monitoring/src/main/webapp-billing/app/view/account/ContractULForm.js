Ext.define('Billing.view.account.ContractULForm', {
    extend: 'Ext.form.Panel',
    alias: 'widget.ulform',   
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
                                margin: '0 20 5 20',
                                //vtype:'alphanum',
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
                    {title: 'Юр. адрес',
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
                                    name:'ucountry'},
                                {                                       
                                    fieldLabel: 'Индекс',
                                    name:'uindex'},
                                {                                       
                                    fieldLabel: 'Территориальный субъект',
                                    name:'utersubj'}, 
                                {    
                                    fieldLabel: 'Район',
                                    name:'uzone'}, 
                                {   
                                    fieldLabel: 'Горд, село, деревня',
                                    name:'ucity'},
                                {   
                                    fieldLabel: 'Улица',
                                    name:'ustreet'},
                                {   
                                    fieldLabel: 'Дом',
                                    name:'uhouse'},
                                {   margin: '0 20 20 20',
                                    fieldLabel: 'Квартира, офис',
                                    name:'uoffice'}
                            ]                    
                   }, 
                    {title: 'Факт. адрес',
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
                                    name:'fcountry'},
                                {                                       
                                    fieldLabel: 'Индекс',
                                    name:'findex'},
                                {                                     
                                    fieldLabel: 'Территориальный субъект',
                                    name:'ftersubj'}, 
                                {    
                                    fieldLabel: 'Район',
                                    name:'fzone'}, 
                                {   
                                    fieldLabel: 'Горд, село, деревня',
                                    name:'fcity'},
                                {   
                                    fieldLabel: 'Улица',
                                    name:'fstreet'},
                                {   
                                    fieldLabel: 'Дом',
                                    name:'fhouse'},
                                {   margin: '0 20 20 20',
                                    fieldLabel: 'Квартира, офис',
                                    name:'foffice'}]
                        },                    
                    {title: 'Реквизиты банка',
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
                                    fieldLabel: 'Наименование банка',
                                    name:'bankname'},
                                {                                      
                                    fieldLabel: 'Расчетный счет',
                                    name:'currentacc'},
                                {                                     
                                    fieldLabel: 'БИК',
                                    name:'bik'}, 
                                {   
                                    fieldLabel: 'Корреспондентский счет',
                                    name:'correspondacc'}, 
                                {   
                                    fieldLabel: 'ИНН',
                                    name:'inn'},
                                {   
                                    fieldLabel: 'КПП',
                                    name:'kpp'},
                                {   
                                    fieldLabel: 'ОКПО',
                                    name:'okpo'},
                                {   margin: '0 20 20 20',
                                    fieldLabel: 'ОКВЭД',
                                    name:'okved'},
                                {   margin: '0 20 20 20',
                                    xtype: 'textareafield',                                    
                                    fieldLabel: 'Примечание',
                                    name:'banknote'}
                                    ]  
                    },
                    {title: 'Генеральный дир.',
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
                                    fieldLabel: 'Фамилия',
                                    name:'gdfam'},
                                {                                      
                                    fieldLabel: 'Имя',
                                    name:'gdname'},
                                {                                      
                                    fieldLabel: 'Отчество',
                                    name:'gdfathername'}, 
                                {    
                                    fieldLabel: 'Телефон 1',
                                    name:'gdphone1'}, 
                                {   
                                    fieldLabel: 'Телефон 2',
                                    name:'gdphone2'},
                                {   
                                    fieldLabel: 'Улица',
                                    name:'gdstreet'},
                                {   
                                    fieldLabel: 'Дом',
                                    name:'gdhouse'},
                                {   margin: '0 20 20 20',
                                    fieldLabel: 'Квартира, офис',
                                    name:'gdoffice'}
                                    ]                    
                    },                    
                    {title: 'Главбух',
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
                                    fieldLabel: 'Фамилия',
                                    name:'gbfam'},
                                {                                      
                                    fieldLabel: 'Имя',
                                    name:'gbname'},
                                {                                     
                                    fieldLabel: 'Отчество',
                                    name:'gbfathername'}, 
                                {   
                                    fieldLabel: 'Телефон',
                                    name:'gbphone'}, 
                                {   
                                    fieldLabel: 'Улица',
                                    name:'gbstreet'},
                                {   
                                    fieldLabel: 'Дом',
                                    name:'gbhouse'},
                                {   margin: '0 20 20 20',
                                    fieldLabel: 'Квартира, офис',
                                    name:'gboffice'}
                                    ]          }
                        ]}
    ]   
})
Ext.define('ContractUL', {
    extend: 'Ext.data.Model',
    fields: [            
            '_id', //'accountid',
            //Вкладка Договор
            'conNumber','conDate','conPerson','conCodeWord','conNote',
            //'conPlan','conPaymetWay','conPaymentRegular','conAccount'            
            //Вкладка Юр. aдрес
            'ucountry','uindex','utersubj','uzone','ucity','ustreet',
            'uhouse','uoffice',
            //Вкладка Физ. адрес
            'fcountry','findex','ftersubj','fzone','fcity','fstreet',
            'fhouse','foffice',
            //Вкладка Реквизиты банка
            'bankname','currentacc','bik','correspondacc','inn','kpp',
            'okpo','okved','banknote',
            //Вкладка Генеральный дир.
            'gdfam','gdname','gdfathername','gdphone1','gdphone2',
            'gdstreet','gdhouse','gdoffice',
            //Вкладка Главбух
            'gbfam','gbname','gbfathername','gbphone','gbstreet',
            'gbhouse','gboffice'
            ],
    idProperty: '_id'    
});

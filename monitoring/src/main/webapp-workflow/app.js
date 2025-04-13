/**
 * Created by IVAN on 06.11.2014.
 */
Ext.require('Ext.direct.*', function () {
    Ext.direct.Manager.addProvider(Ext.app.REMOTING_API);
});
//function fun(){
//    console.log("roles", window.userRoles)
//    var panels=new Array();
//    panels['Installer']='Workflow.view.installer.InstallerPanel'
//    var allowedPanels=new Array();
//    for (i in window.userRoles) {
//        console.log(window.userRoles[i],panels[window.userRoles[i]])
//        if(panels[window.userRoles[i]]!= null && panels[window.userRoles[i]]!=undefined) allowedPanels.push(panels[window.userRoles[i]])
//    }
//    console.log("allowedPanels", allowedPanels)
//    Ext.require(allowedPanels)
//}
//fun()

Ext.application({
    name: 'Workflow',
    launch: function () {
//          Ext.require('Workflow.view.installer.InstallerPanel')
//        console.log("roles", window.userRoles)
//        var panels=new Array();
//        panels['Installer']='Workflow.view.installer.InstallerPanel'
//        var allowedPanels=new Array();
//        for (i in window.userRoles) {
//            console.log(window.userRoles[i],panels[window.userRoles[i]])
//            if(panels[window.userRoles[i]]!= null && panels[window.userRoles[i]]!=undefined) allowedPanels.push(panels[window.userRoles[i]])
//        }
//        console.log("allowedPanels", allowedPanels)
//        Ext.require(allowedPanels)
        var viewport = Ext.create({
            xtype: 'viewport',
            layout: {
                type: 'fit'
            },
            //overflowY:'scroll',
            items: [
                {
                    //style:'margin-top: 15%; margin-left: 40%',
                    xtype: "wptabpanel"
                }
            ]//,
//            listeners: {
//                beforerender: function (viewport, eOpts) {
//                    console.log("roles", window.userRoles)
//                }//,
//                resize:function(viewport, width, height, oldWidth, oldHeight, eOpts ) {
//                    var bg = viewport.down('#workplacesbg')
//                    var itemsCount=bg.items.getCount();
//                    var setPosition=function(){
//                        var newXPos = width / 2 - bg.width/2
//                        if(newXPos<0 || width <=bg.defaultItemWidth)  newXPos=0
//                        var newYPos = height / 2 - bg.height/2
//                        if(newYPos<0 || height<=bg.defaultItemHeight )  newYPos=0
//                        bg.setX(newXPos)
//                        bg.setY(newYPos)
//                    }
//                    bg.setWidth(bg.defaultWidth)
//                    bg.setHeight(bg.defaultHeight)
//                    setPosition();
//
//                    var newWidth,newHeight;
//                    var itemsOnRow,itemsOnCol;
//                    if (width <= bg.defaultWidth /*|| height <= bg.defaultHeight*/){
//                        itemsOnRow=Math.floor(width/bg.defaultItemWidth)
//                        itemsOnCol=Math.ceil(itemsCount/itemsOnRow)
//                        newWidth=itemsOnRow*bg.defaultItemWidth+itemsOnRow*4
//                        newHeight=itemsOnCol*bg.defaultItemHeight+itemsOnCol*4
//                        bg.setHeight(newHeight)
//                        bg.setWidth(newWidth)
//                        setPosition();
//                    } else
//                    if (height <= bg.defaultHeight)  {
//                        itemsOnCol=Math.floor(height/bg.defaultItemHeight)
//                        itemsOnRow=Math.ceil(itemsCount/itemsOnCol)
//                        newWidth=itemsOnRow*bg.defaultItemWidth+itemsOnRow*4
//                        newHeight=itemsOnCol*bg.defaultItemHeight+itemsOnCol*4
//                        bg.setHeight(newHeight)
//                        bg.setWidth(newWidth)
//                        setPosition();
//
//                    }
//                    /*Уменьшение размера кнопок в случае если ширина или высота viewport меньше параметров самой кнопки
//                    или меньше новой ширины или высоты контейнера.*/
//                    if ( height<=bg.defaultItemHeight || width <=bg.defaultItemWidth || height< newHeight || width < newWidth)  {
//                        for(i in bg.items.items){
//                            bg.items.items[i].setWidth(180)
//                            bg.items.items[i].setHeight(45)
//                        }
//                        bg.setY(0)
//                        bg.setX(0)
//                    }
//                    else
//                    {
//                        for(i in bg.items.items){
//                            bg.items.items[i].setWidth(bg.defaultItemWidth)
//                            bg.items.items[i].setHeight(bg.defaultItemHeight)
//                        }
//                    }
//
//                }
//            }
        });
    }
});


(function() {
    console.log("roles", window.userRoles)
    var panels=new Array();
    panels['Installer']='Workflow.view.installer.InstallerPanel'
    panels['Manager']="Workflow.view.manager.ManagerPanel"
    panels['Accountant']="Workflow.view.accountant.AccountantPanel"
    panels['Storekeeper']="Workflow.view.storekeeper.StorekeeperPanel"
    var allowedPanels=new Array();
    for (i in window.userRoles) {
        console.log(window.userRoles[i],panels[window.userRoles[i]])
        if(panels[window.userRoles[i]]!= null && panels[window.userRoles[i]]!=undefined) allowedPanels.push(panels[window.userRoles[i]])
    }
    console.log("allowedPanels", allowedPanels)
    Ext.require(allowedPanels)
}())

Ext.define("Workflow.WorkplacesTabPanel", {
    //require:['Workflow.view.installer.InstallerPanel'],
    extend: 'Ext.tab.Panel',
    alias: "widget.wptabpanel",
    plugins: 'responsive',
    responsiveConfig: {
        'width < height': {
            tabPosition: 'right'
        },

        'width >= height': {
            tabPosition: 'top'
        }
    },
    initComponent: function () {
        var self = this;
        Ext.apply(this, {
            items:self.setAllowedPanels()
        });
        this.callParent();
    },
    tabBar: {
        defaults: {
            closable: false
        },
        items: [
            {
                xtype: 'tbfill'
            }
            ,
            {
                text: 'Выход',
                tooltip: 'Выйход из системы',
                handler: function () {
                    Ext.MessageBox.confirm('Выход из системы', 'Вы уверены, что хотите выйти из системы?', function (button) {
                        if (button === 'yes') {
                            loginService.logout(function () {
                                window.location = "/workflow/login.html";
                            });
                        }
                    })
                }
            }
        ]
    },
    listeners:{
        afterrender:function(tabpanel,eopts){
           var items= tabpanel.getTabBar().items.items
            console.log("items",items)
            for(var i in items){
                if(items[i].hidden!=true) {
                    var active=tabpanel.setActiveTab(parseInt(i));
                    console.log("active=",active);
                    break;
                }
            }
            console.log("tabpanel.getActiveTab();",tabpanel.getActiveTab());
        }
    },
    setAllowedPanels:function(){
        var panels = new Array();
        panels['Installer']=
            {
                title: 'Монтажник',
                layout:'fit',
                //icon: 'images/ico24_msghist.png',
                items: [
                    {
                        xtype: 'instpanel'
                    }
                ]
            };
        panels['Manager']=
            {
                    title: 'Менеджер',
                    layout:'fit',
                    //icon: 'images/ico24_msghist.png',
                    items: [
                        {
                            xtype: 'managerpanel'
                        }
                    ]
            };
        panels['Storekeeper']=
            {
                title: 'Кладовщик',
                layout:'fit',
                    //icon: 'images/ico24_msghist.png',
                items: [
                    {
                        xtype: 'storekeeperpanel'
                    }
                ]
            };
        panels['Accountant']=
            {
                title: 'Бухгалтер',
                layout:'fit',
                //icon: 'images/ico24_msghist.png',
                items: [
                    {
                        xtype: 'accountantpanel'
                    }
                ]
            };
        var allowedPanels=new Array();
        for (i in window.userRoles) {
            if(panels[window.userRoles[i]]!= null && panels[window.userRoles[i]]!=undefined) allowedPanels.push(panels[window.userRoles[i]])
        }
        return allowedPanels
    },
    hasAuthority: function (auth) {
        console.log("ups", auth, window.userRoles)
        var flag = false
        for (i in window.userRoles) {
            if (window.userRoles[i] == auth) flag = true
        }
        return flag
    }

});
package ru.sosgps.wayrecall.e2e;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.internal.FindsById;
import org.openqa.selenium.internal.FindsByName;
//import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

/**
 * Created by nickl on 22.11.14.
 */
public class WRC {

    public static void main(String[] args) {
        //HtmlUnitDriver driver = new HtmlUnitDriver(true);
        //PhantomJSDriver driver = new PhantomJSDriver();
        //ChromeDriver driver = new ChromeDriver();
        FirefoxDriver driver = new FirefoxDriver();
        try {

            driver.get("http://localhost:5193");

            WebElement username = driver.findElementByName("username");
            username.sendKeys("1234");
            WebElement password = driver.findElementByName("password");
            password.sendKeys("Ahthee3");

            WebElement entbtn = findElementByComponentQuery(driver, "button[name=loginButton]");

            entbtn.click();

            (new WebDriverWait(driver, 30)).until(new ExpectedCondition<Boolean>() {
                public Boolean apply(WebDriver d) {
                    return d.getTitle().toLowerCase().contains("мониторинг");
                }
            });


            WebElement filterobjs = driver.findElementByName("filterobjs");
            filterobjs.sendKeys("sms");

            WebElement n = findElementByComponentQuery(driver, "#notificationsCtrl");
            n.click();
            WebElement n2 = findElementByComponentQuery(driver, "[text=\"Правила уведомлений\"]");
            n2.click();

//            WebElement row = (WebElement) driver.executeScript("" +
//                            "var g = Ext.ComponentQuery.query('grid')[0];" +
//                            "var i =g.getStore().getAt(0);" +
//                            "var r =g.getView().getNode(i);" +
//                            "console.log('r=',r);" +
//                            "return r.children[8].firstChild.firstChild;"
//
//            );
//
//            row.click();

//
//            WebElement commandCell = row.findElement(By.xpath("./td[@title=\"Отправить команду объекту\"]"));
//            System.out.println("commandCell="+commandCell/*+" "+commandCell.getAttribute("id")*/);
//            WebElement button = commandCell.findElement(By.xpath(".//*[@role='button']"));
//            button.click();


        } finally {
//            driver.close();
//            driver.quit();
        }




    }

    private static <T extends WebDriver & JavascriptExecutor & FindsById> WebElement findElementByComponentQuery(T driver, String cc) {


        String btnId =
                (String) driver.executeScript("" +
                                "var r = Ext.ComponentQuery.query('" + cc + "')[0].getEl().dom.id;" +
                                "console.log('r=',r);"+
                                "return r;"
                );

        return driver.findElementById(btnId);
    }

}

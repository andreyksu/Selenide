package ru.andreyksu.annikonenkov.selenide;

import ru.andreyksu.annikonenkov.selenide.pagewidgets.*;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.CollectionCondition.*;
import static com.codeborne.selenide.Selenide.*;

import com.codeborne.selenide.WebDriverRunner;

/**
 * Simple Selenide Test with PageObjects
 */
public class GoogleTestTest

{

    public static void main(String[] args) {
        System.setProperty("selenide.browser", WebDriverRunner.CHROME);
        System.setProperty("webdriver.chrome.driver", "/opt/webDrivers/chromedriver");
        // Arrange
        open("https://google.com/ncr");
        // Act
        new GoogleSearchTest().searchFor("selenide");

        // Assert
        SearchResultsTest results = new SearchResultsTest();
        results.found.shouldHave(sizeGreaterThan(1));
        results.getResult(0).shouldHave(text("Selenide: concise UI tests in Java"));
    }
}

package com.yatra.automation;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class YatraAutomationScript {

    public static void main(String[] args) {
        EdgeOptions edgeOptions = new EdgeOptions();
        edgeOptions.addArguments("--disable-notifications");
        System.setProperty("webdriver.edge.driver", "C:\\edgedriver_win64\\msedgedriver.exe");

        WebDriver wd = new EdgeDriver(edgeOptions);
        WebDriverWait wait = new WebDriverWait(wd, Duration.ofSeconds(20));

     
        try {
            wd.get("https://www.yatra.com");
            wd.manage().window().maximize();


            closePopUp(wait);
            clickOnDepartureDate(wait);

            WebElement currentMonthWebElement = selectTheMonthFromCalendar(wait, 0); // current Month
            WebElement nextMonthWebElement = selectTheMonthFromCalendar(wait, 1);    // Next Month

            System.out.println("Calendar Loaded: " + 2 + " months visible.\n");

            // Wait explicitly for prices to be visible (on next month calendar)
            wait.until(ExpectedConditions.visibilityOfNestedElementsLocatedBy(
                nextMonthWebElement, By.xpath(".//span[contains(@class,'custom-day-content')]")));

            String lowestPriceForCurrentMonth = getMeLowestPrice(currentMonthWebElement);
            String lowestPriceForNextMonth = getMeLowestPrice(nextMonthWebElement);

            System.out.println("Lowest Price Details:");
            System.out.println("- Current Month: " + formatPriceOutput(lowestPriceForCurrentMonth));
            System.out.println("- Next Month: " + formatPriceOutput(lowestPriceForNextMonth));
            System.out.println();

            compareTwoMonthsPrices(lowestPriceForCurrentMonth, lowestPriceForNextMonth);

        } catch (Exception e) {
            System.err.println("Automation error: " + e.getMessage());
        } finally {
            wd.quit();
        }
    }

   

    private static void clickOnDepartureDate(WebDriverWait wait) {
        By departureDateButtonLocator = By.xpath("//div[@aria-label=\"Departure Date inputbox\" and @role='button']");
        WebElement departureDateButton = wait.until(ExpectedConditions.elementToBeClickable(departureDateButtonLocator));
        departureDateButton.click();
    }

    private static void closePopUp(WebDriverWait wait) {
        By popUpLocator = By.xpath("//div[contains(@class,\"style_popup\")][1]");
        try {
            WebElement popUpElement = wait.until(ExpectedConditions.visibilityOfElementLocated(popUpLocator));
            WebElement crossButton = popUpElement.findElement(By.xpath(".//img[@alt=\"cross\"]"));
            crossButton.click();
        } catch (TimeoutException e) {
            System.out.println("Pop-up not shown on the screen");
        }
    }

    private static String getMeLowestPrice(WebElement monthWebElement) {
        By priceLocator = By.xpath(".//span[contains(@class,\"custom-day-content\")]");
        List<WebElement> priceList = monthWebElement.findElements(priceLocator);

        int lowestPrice = Integer.MAX_VALUE;
        WebElement priceElement = null;

        for (WebElement price : priceList) {
            int priceInt = parsePriceString(price.getText());
            if (priceInt < lowestPrice) {
                lowestPrice = priceInt;
                priceElement = price;
            }
        }

        if (priceElement == null) {
            return "No price data found";
        }

        WebElement dateElement = priceElement.findElement(By.xpath(".//../.."));
        String result = dateElement.getAttribute("aria-label") + "---Price is Rs" + lowestPrice;
        return result;
    }

    private static int parsePriceString(String priceString) {
        if (priceString == null || priceString.isEmpty()) return Integer.MAX_VALUE;
        try {
            return Integer.parseInt(priceString.replace("₹", "").replace(",", "").trim());
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    public static WebElement selectTheMonthFromCalendar(WebDriverWait wait, int index) {
        By calendarMonthLocator = By.xpath("//div[@class=\"react-datepicker__month-container\"]");
        List<WebElement> calendarMonthsList = wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(calendarMonthLocator));
        if (index < 0 || index >= calendarMonthsList.size()) {
            throw new IllegalArgumentException("Invalid month index: " + index);
        }
        return calendarMonthsList.get(index);
    }

    public static void compareTwoMonthsPrices(String currentMonthPrice, String nextMonthPrice) {
        int currentMonthRSIndex = currentMonthPrice.indexOf("Rs");
        int nextMonthRSIndex = nextMonthPrice.indexOf("Rs");

        if (currentMonthRSIndex == -1 || nextMonthRSIndex == -1) {
            System.out.println("Unable to extract price values for comparison.");
            return;
        }

        String currentPrice = currentMonthPrice.substring(currentMonthRSIndex + 2).trim();
        String nextPrice = nextMonthPrice.substring(nextMonthRSIndex + 2).trim();

        int current = Integer.parseInt(currentPrice);
        int next = Integer.parseInt(nextPrice);

        System.out.println("Price Comparison Result:");
        if (current < next) {
            System.out.println("The lowest price between the two months is Rs " + current + " (Current Month).");
        } else if (current == next) {
            System.out.println("Price is same for both months! Choose whichever you prefer.");
        } else {
            System.out.println("The lowest price between the two months is Rs " + next + " (Next Month).");
        }
    }

    private static String formatPriceOutput(String rawPriceString) {
       
        if (rawPriceString == null || rawPriceString.isEmpty() || rawPriceString.equals("No price data found")) {
            return "No price data found";
        }
        String[] parts = rawPriceString.split("---");
        if (parts.length < 2) return rawPriceString;
        String datePart = parts[0].replace("Choose ", "").trim();
        String pricePart = parts[1].replace("Price is ", "").trim();
        return datePart + " — " + pricePart;
    }
}
